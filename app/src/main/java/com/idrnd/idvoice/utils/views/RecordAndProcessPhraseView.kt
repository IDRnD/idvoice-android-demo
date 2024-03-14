package com.idrnd.idvoice.utils.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.Record

/**
 * View for phrase recording.
 */
class RecordAndProcessPhraseView : ConstraintLayout {

    private val view = LayoutInflater.from(context).inflate(
        R.layout.record_and_process_phrase_view,
        this,
        true
    )

    private val rootRecordAndProcessPhraseView: ViewGroup by lazy {
        view.findViewById(
            R.id.rootRecordAndProcessPhraseView
        )
    }
    private val recordAndProcessView: RecordAndProcessView by lazy {
        view.findViewById(R.id.recordAndProcessView)
    }
    private val messageAboutPhraseView: TextView by lazy {
        view.findViewById(R.id.messageAboutPhraseView)
    }
    private val phraseForPronouncingView: TextView by lazy {
        view.findViewById(R.id.phraseForPronouncingView)
    }

    var state = Record
        set(newState) {

            if (field == newState) {
                return
            }

            when (field to newState) {
                Record to State.Process, ProcessIsFinished to State.Process -> {
                    messageAboutPhraseView.visibility = GONE
                    phraseForPronouncingView.visibility = GONE
                    recordAndProcessView.state = RecordAndProcessView.State.Process
                }
                Record to ProcessIsFinished, State.Process to ProcessIsFinished -> {
                    messageAboutPhraseView.visibility = GONE
                    phraseForPronouncingView.visibility = GONE
                    recordAndProcessView.state = RecordAndProcessView.State.ProcessIsFinished
                }
                State.Process to Record, ProcessIsFinished to Record -> {
                    messageAboutPhraseView.visibility = VISIBLE
                    phraseForPronouncingView.visibility = VISIBLE
                    recordAndProcessView.state = RecordAndProcessView.State.Record
                }
            }

            field = newState
        }

    var lifecycle
        set(value) {
            recordAndProcessView.lifecycle = value
        }
        get() = recordAndProcessView.lifecycle

    var messageAboutProcess
        set(value) {
            recordAndProcessView.messageAboutProcess = value
        }
        get() = recordAndProcessView.messageAboutProcess

    var messageAboutPhrase
        set(value) {
            messageAboutPhraseView.text = value
        }
        get() = messageAboutPhraseView.text

    var phraseForPronouncing
        set(value) {
            phraseForPronouncingView.text = value
        }
        get() = phraseForPronouncingView.text

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

    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
        // Set background color inner view
        try {
            rootRecordAndProcessPhraseView.background = background
            recordAndProcessView.background = background
        } catch (e: NullPointerException) {
            Log.d(
                TAG,
                "Try to set background before finish of view inflating. This is expected behaviour.",
                e
            )
        }
    }

    fun visualize() {
        recordAndProcessView.visualize()
    }

    fun stopVisualization() {
        recordAndProcessView.stopVisualization()
    }

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
