package com.idrnd.idvoice.utils.views.lightCircle

import kotlin.concurrent.thread

/**
 * Callback which will invoke if it is not waiting.
 */
class WaitingCallback(val waitDurationInMs: Long, private val callback: () -> (Unit)) {

    private var thread: Thread? = null
    private var lastTimeWhenItIsWaited = 0L

    /**
     * Wait for the passed duration, after which the callback will be called.
     */
    @Synchronized
    fun waitFurther() {
        lastTimeWhenItIsWaited = System.currentTimeMillis()
        if (thread?.isAlive == true) return
        thread = thread {
            while (lastTimeWhenItIsWaited + waitDurationInMs >= System.currentTimeMillis()) {
                val result = runCatching { Thread.sleep(waitDurationInMs) }
                if (result.isFailure) return@thread
            }
            callback.invoke()
        }
    }
}
