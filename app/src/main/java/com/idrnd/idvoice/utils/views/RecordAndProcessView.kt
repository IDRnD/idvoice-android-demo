package com.idrnd.idvoice.utils.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.views.RecordAndProcessView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.RecordAndProcessView.State.Record

/**
 * View that must record and process something.
 */
class RecordAndProcessView : ConstraintLayout, LifecycleObserver {

    private val view = LayoutInflater.from(context).inflate(
        R.layout.record_and_process_view,
        this,
        true
    )

    private val container by lazy { view.findViewById<ViewGroup>(R.id.recordAndProcessViewContainer) }

    /**
     * [State] of view. You can change it for change an appearance state.
     */
    var state: State = Record
        set(newState) {

            if (field == newState) {
                return
            }

            Log.d(TAG, "Change state from $field to $newState")

            when (field to newState) {
                Record to State.Process, ProcessIsFinished to State.Process -> {
                    visualizer.visibility = GONE
                    processingImage.visibility = VISIBLE
                    messageAboutProcessView.visibility = VISIBLE
                }

                Record to ProcessIsFinished, State.Process to ProcessIsFinished -> {
                    visualizer.visibility = GONE
                    processingImage.visibility = GONE
                    messageAboutProcessView.visibility = GONE
                }

                State.Process to Record, ProcessIsFinished to Record -> {
                    visualizer.visibility = VISIBLE
                    processingImage.visibility = GONE
                    messageAboutProcessView.visibility = GONE
                }
            }

            field = newState
        }

    /**
     * Lifecycle for view. You need add it, that view will be auto-changing by lifecycle.
     */
    var lifecycle: Lifecycle? = null
        set(value) {
            // Update an observer on lifecycle
            field?.removeObserver(this)
            field = value
            field?.addObserver(this)
        }

    private val processingImage: ImageView by lazy { view.findViewById(R.id.processingImage) }
    private val messageAboutProcessView: TextView by lazy { view.findViewById(R.id.messageAboutProcess) }
    private val visualizer: SimpleCircle by lazy { view.findViewById(R.id.visualizer) }

    var messageAboutProcess
        set(value) { messageAboutProcessView.text = value }
        get() = messageAboutProcessView.text

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        context.withStyledAttributes(attrs, R.styleable.RecordAndProcessView, defStyleAttr) {
            messageAboutProcess = getString(R.styleable.RecordAndProcessView_message_about_process) ?: DEFAULT_MESSAGE_ABOUT_PROCESS

            // Call is here because method has specific logic to set background of inner views and must to call after
            // a view inflating.
            setBackground(background)
        }
    }

    /**
     * Visualize data. For example audio data from recorder.
     */
    fun visualizeData(normalizedValue: Float) {
        visualizer.setNormalizedRadius(normalizedValue)
    }

    fun resetVisualization() {
        visualizer.setNormalizedRadius(0f)
    }

    @OnLifecycleEvent(ON_RESUME)
    fun onResume() {
        resetVisualization()
    }

    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
        // Set background color inner view
        try {
            container.background = background
        } catch (e: NullPointerException) {
            Log.d(TAG, "Try to set background before finish of view inflating. This is expected behaviour.", e)
        }
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
