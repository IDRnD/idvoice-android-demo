package com.idrnd.idvoice.utils.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.views.RecordAndProcessView.State.Process
import com.idrnd.idvoice.utils.views.RecordAndProcessView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.RecordAndProcessView.State.Record
import com.idrnd.idvoice.utils.views.lightCircle.LightCircle
import com.idrnd.idvoice.utils.views.lightCircle.WaitingCallback

/**
 * View that must record and process something.
 */
class RecordAndProcessView : ConstraintLayout, DefaultLifecycleObserver {

    init {
        inflate(context, R.layout.record_and_process_view, this)
    }

    /**
     * [State] of view. You can change it for change an appearance state.
     */
    var state: State = Record
        set(newState) {

            if (field == newState) {
                return
            }

            val visualizer = findViewById<LightCircle>(R.id.visualizer)
            val processingImage = findViewById<ImageView>(R.id.processingImage)
            val messageAboutProcess = findViewById<TextView>(R.id.messageAboutProcess)

            Log.d(TAG, "Change state from $field to $newState")

            when (field to newState) {
                Record to Process, ProcessIsFinished to Process -> {
                    visualizer.visibility = GONE
                    processingImage.visibility = VISIBLE
                    messageAboutProcess.visibility = VISIBLE
                }

                Record to ProcessIsFinished, Process to ProcessIsFinished -> {
                    visualizer.visibility = GONE
                    processingImage.visibility = GONE
                    messageAboutProcess.visibility = GONE
                }

                Process to Record, ProcessIsFinished to Record -> {
                    visualizer.visibility = VISIBLE
                    processingImage.visibility = GONE
                    messageAboutProcess.visibility = GONE
                }
            }

            field = newState
        }

    var messageAboutProcess
        set(value) {
            findViewById<TextView>(R.id.messageAboutProcess).text = value
        }
        get() = findViewById<TextView>(R.id.messageAboutProcess).text.toString()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {

        context.withStyledAttributes(attrs, R.styleable.RecordAndProcessView, defStyleAttr) {
            messageAboutProcess = getString(
                R.styleable.RecordAndProcessView_message_about_process
            ) ?: DEFAULT_MESSAGE_ABOUT_PROCESS

            // Call is here because method has specific logic to set background of inner views and must to call after
            // a view inflating.
            setBackground(background)
        }
    }

    private val waitingCallback = WaitingCallback(350L) {
        if (handler == null) return@WaitingCallback
        handler.post { findViewById<LightCircle>(R.id.visualizer).lightOff() }
    }

    /**
     * Visualize data. For example audio data from recorder.
     */
    fun visualize() {
        waitingCallback.waitFurther()
        val visualizer = findViewById<LightCircle>(R.id.visualizer)
        if (!visualizer.isLightOn) visualizer.lightOn()
    }

    fun stopVisualization() {
        findViewById<LightCircle>(R.id.visualizer).lightOff()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        stopVisualization()
    }

    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
        // Set background color inner view
        findViewById<ViewGroup>(R.id.recordAndProcessViewContainer)?.apply {
            this.background = background
        } ?: Log.w(
            TAG,
            "Try to set background before finish of view inflating. This is expected behaviour."
        )
    }

    companion object {
        private val TAG = RecordAndProcessView::class.simpleName
        private val DEFAULT_MESSAGE_ABOUT_PROCESS = "Please wait until processing is complete"
    }

    /**
     * States of view. It can be:
     *
     * * [Record]: bars are show and visualize a passed data
     * * [Process]: show a progress bar and message about process
     * * [ProcessIsFinished]: disappear all views. It is needed for smooth UI.
     *
     */
    enum class State {
        Record,
        Process,
        ProcessIsFinished
    }
}
