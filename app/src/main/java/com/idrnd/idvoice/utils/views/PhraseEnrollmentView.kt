package com.idrnd.idvoice.utils.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.withStyledAttributes
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.Record

/**
 * View for text dependent voice enrollment.
 */
class PhraseEnrollmentView : ConstraintLayout {

    private val view = LayoutInflater.from(context).inflate(
        R.layout.phrase_enrollment_view,
        this,
        true
    )

    private val recordAndProcessPhraseView: RecordAndProcessPhraseView by lazy {
        view.findViewById(R.id.recordAndProcessPhraseView)
    }

    private val statusIndicators: Array<CheckBox> by lazy {
        view.findViewById<Group>(R.id.statusIndicators)
            .referencedIds
            .map { view.findViewById<CheckBox>(it) }
            .toTypedArray()
    }

    var state = Record
        set(newState) {

            if (field == newState) {
                return
            }

            when (field to newState) {
                Record to State.Process, ProcessIsFinished to State.Process -> {
                    statusIndicators.forEach { it.visibility = GONE }
                    recordAndProcessPhraseView.state = RecordAndProcessPhraseView.State.Process
                }
                Record to ProcessIsFinished, State.Process to ProcessIsFinished -> {
                    statusIndicators.forEach { it.visibility = GONE }
                    recordAndProcessPhraseView.state = RecordAndProcessPhraseView.State.ProcessIsFinished
                }
                State.Process to Record, ProcessIsFinished to Record -> {
                    statusIndicators.forEach { it.visibility = VISIBLE }
                    recordAndProcessPhraseView.state = RecordAndProcessPhraseView.State.Record
                }
            }

            field = newState
        }

    var lifecycle
        set(value) {
            recordAndProcessPhraseView.lifecycle = value
        }
        get() = recordAndProcessPhraseView.lifecycle

    var messageAboutProcess
        set(value) {
            recordAndProcessPhraseView.messageAboutProcess = value
        }
        get() = recordAndProcessPhraseView.messageAboutProcess

    var messageAboutPhrase
        set(value) {
            recordAndProcessPhraseView.messageAboutPhrase = value
        }
        get() = recordAndProcessPhraseView.messageAboutPhrase

    var phraseForPronouncing
        set(value) {
            recordAndProcessPhraseView.phraseForPronouncing = value
        }
        get() = recordAndProcessPhraseView.phraseForPronouncing

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {

        context.withStyledAttributes(attrs, R.styleable.PhraseEnrollmentView, defStyleAttr) {

            messageAboutProcess = getString(R.styleable.PhraseEnrollmentView_message_about_process)
                ?: DEFAULT_MESSAGE_ABOUT_PROCESS

            messageAboutPhrase = getString(R.styleable.PhraseEnrollmentView_message_about_phrase)
                ?: DEFAULT_MESSAGE_ABOUT_PHRASE

            phraseForPronouncing = getString(
                R.styleable.PhraseEnrollmentView_phrase_for_pronouncing
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
            recordAndProcessPhraseView.background = background
        } catch (e: NullPointerException) {
            Log.d(
                TAG,
                "Try to set background before finish of view inflating. This is expected behaviour.",
                e
            )
        }
    }

    fun setCheckedRecordIndicatorByIndex(index: Int, checked: Boolean) {
        statusIndicators[index].isChecked = checked
    }

    fun visualize() {
        recordAndProcessPhraseView.visualize()
    }

    fun stopVisualization() {
        recordAndProcessPhraseView.stopVisualization()
    }

    companion object {
        private val TAG = PhraseEnrollmentView::class.simpleName
        private const val DEFAULT_MESSAGE_ABOUT_PROCESS = "Please wait until processing is complete"
        private const val DEFAULT_MESSAGE_ABOUT_PHRASE = "Please to pronounce a phrase"
        private const val DEFAULT_PHRASE_FOR_PRONOUNCING = "Awesome phrase"
    }

    enum class State {
        Record,
        LivenessChecking,
        Process,
        ProcessIsFinished
    }
}
