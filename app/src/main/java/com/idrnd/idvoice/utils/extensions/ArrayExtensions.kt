package com.idrnd.idvoice.utils.extensions

import kotlinx.coroutines.Deferred

/**
 * Awaits for completion of given deferred values.
 */
suspend inline fun <reified T> Array<Deferred<T>?>.awaitAll(): Array<T?> {
    return map { it?.await() }.toTypedArray()
}
