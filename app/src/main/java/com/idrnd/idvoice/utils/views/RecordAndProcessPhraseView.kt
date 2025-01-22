package com.idrnd.idvoice.utils.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.LifecycleObserver
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.Record

/**
 * View for phrase recording.
 */
class RecordAndProcessPhraseView : ConstraintLayout {

    init {
        inflate(context, R.layout.record_and_process_phrase_view, this)
    }

    var state = Record
        set(newState) {

            if (field == newState) {
                return
            }

            val messageAboutPhraseView = getMessageAboutPhraseView()
            val phraseForPronouncingView = getPhraseForPronouncingView()
            val recordAndProcessView = getRecordAndProcessView()

            when (field to newState) {
                Record to State.Process, ProcessIsFinished to State.Process -> {
                    messageAboutPhraseView.visibility = GONE
                    phraseForPronouncingView.visibility = GONE
                    recordAndProcessView.state =
                        RecordAndProcessView.State.Process
                }

                Record to ProcessIsFinished, State.Process to ProcessIsFinished -> {
                    messageAboutPhraseView.visibility = GONE
                    phraseForPronouncingView.visibility = GONE
                    recordAndProcessView.state =
                        RecordAndProcessView.State.ProcessIsFinished
                }

                State.Process to Record, ProcessIsFinished to Record -> {
                    messageAboutPhraseView.visibility = VISIBLE
                    phraseForPronouncingView.visibility = VISIBLE
                    recordAndProcessView.state =
                        RecordAndProcessView.State.Record
                }
            }

            field = newState
        }

    var messageAboutProcess
        set(value) {
            getRecordAndProcessView().messageAboutProcess = value
        }
        get() = getRecordAndProcessView().messageAboutProcess

    var messageAboutPhrase
        set(value) {
            getMessageAboutPhraseView().text = value
        }
        get() = getMessageAboutPhraseView().text.toString()

    var phraseForPronouncing
        set(value) {
            getPhraseForPronouncingView().text = value
        }
        get() = getPhraseForPronouncingView().text.toString()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {

        context.withStyledAttributes(attrs, R.styleable.RecordAndProcessPhraseView, defStyleAttr) {

            messageAboutProcess = getString(
                R.styleable.RecordAndProcessPhraseView_message_about_process
            )
                ?: DEFAULT_MESSAGE_ABOUT_PROCESS

            messageAboutPhrase = getString(
                R.styleable.RecordAndProcessPhraseView_message_about_phrase
            )
                ?: DEFAULT_MESSAGE_ABOUT_PHRASE

            phraseForPronouncing = getString(
                R.styleable.RecordAndProcessPhraseView_phrase_for_pronouncing
            )
                ?: DEFAULT_PHRASE_FOR_PRONOUNCING

            // Call is here because method has specific logic to set background of inner views and must to call after
            // a view inflating.
            setBackground(background)
        }
    }

    fun getLifecycleObserver(): LifecycleObserver = getRecordAndProcessView()

    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
        // Set background color inner view

        // Background not set message
        val backgroundNotSetMessage =
            "Try to set background before finish of view inflating. This is expected behaviour."

        findViewById<ViewGroup>(R.id.rootRecordAndProcessPhraseView)?.apply {
            this.background = background
        } ?: Log.w(TAG, backgroundNotSetMessage)

        getRecordAndProcessView()?.apply {
            this.background = background
        } ?: Log.w(TAG, backgroundNotSetMessage)
    }

    fun visualize() {
        getRecordAndProcessView().visualize()
    }

    fun stopVisualization() {
        getRecordAndProcessView().stopVisualization()
    }

    private fun getRecordAndProcessView() = findViewById<RecordAndProcessView>(R.id.recordAndProcessView)
    private fun getMessageAboutPhraseView() = findViewById<TextView>(R.id.messageAboutPhraseView)
    private fun getPhraseForPronouncingView() = findViewById<TextView>(R.id.phraseForPronouncingView)

    companion object {
        private val TAG = RecordAndProcessPhraseView::class.simpleName
        private const val DEFAULT_MESSAGE_ABOUT_PROCESS = "Processing"
        private const val DEFAULT_MESSAGE_ABOUT_PHRASE = "Please to pronounce a phrase"
        private const val DEFAULT_PHRASE_FOR_PRONOUNCING = "Awesome phrase"
    }

    enum class State {
        Record,
        Process,
        ProcessIsFinished
    }
}
