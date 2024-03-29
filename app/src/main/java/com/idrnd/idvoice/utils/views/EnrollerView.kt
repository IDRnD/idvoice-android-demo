package com.idrnd.idvoice.utils.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleObserver
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.views.EnrollerView.State.Process
import com.idrnd.idvoice.utils.views.EnrollerView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.EnrollerView.State.Record
import com.idrnd.idvoice.utils.views.lightCircle.LightCircle
import com.idrnd.idvoice.utils.views.lightCircle.WaitingCallback

/**
 * View for text independent voice enrollment.
 */
class EnrollerView : ConstraintLayout, LifecycleObserver {

    private val view = LayoutInflater.from(context).inflate(
        R.layout.ti_enroller_view,
        this,
        true
    )

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
                Record to Process, ProcessIsFinished to Process -> {
                    titleTiTipsViewView.visibility = GONE
                    tipsTextView.visibility = GONE
                    progressBar.visibility = GONE
                    visualizer.visibility = GONE

                    processingImage.visibility = VISIBLE
                    messageAboutProcessView.visibility = VISIBLE
                }

                Record to ProcessIsFinished, Process to ProcessIsFinished -> {
                    titleTiTipsViewView.visibility = GONE
                    tipsTextView.visibility = GONE
                    progressBar.visibility = GONE
                    visualizer.visibility = GONE

                    processingImage.visibility = GONE
                    messageAboutProcessView.visibility = GONE
                }

                Process to Record, ProcessIsFinished to Record -> {
                    titleTiTipsViewView.visibility = VISIBLE
                    tipsTextView.visibility = VISIBLE
                    progressBar.visibility = VISIBLE
                    visualizer.visibility = VISIBLE

                    processingImage.visibility = GONE
                    messageAboutProcessView.visibility = GONE
                }
            }

            field = newState
        }

    private val tipsTextView: TextView by lazy {
        view.findViewById(R.id.headerTiEnrollmentTipsTextView)
    }
    private val titleTiTipsViewView: TextView by lazy {
        view.findViewById(R.id.tiEnrollmentTipsTextView)
    }
    private val progressBar: ProgressBar by lazy { view.findViewById(R.id.speechProgressBar) }
    private val visualizer: LightCircle by lazy { view.findViewById(R.id.visualizer) }

    private val messageAboutProcessView: TextView by lazy {
        view.findViewById(R.id.messageAboutProcess2)
    }
    private val processingImage: ImageView by lazy { view.findViewById(R.id.processingImage2) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    fun setProgress(progress: Int) {
        progressBar.progress = progress
    }

    private val waitingCallback = WaitingCallback(350L) {
        if (!isAttachedToWindow) return@WaitingCallback
        handler.post { visualizer.lightOff() }
    }

    fun visualize() {
        waitingCallback.waitFurther()
        if (!visualizer.isLightOn) visualizer.lightOn()
    }

    companion object {
        private val TAG = EnrollerView::class.simpleName
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
