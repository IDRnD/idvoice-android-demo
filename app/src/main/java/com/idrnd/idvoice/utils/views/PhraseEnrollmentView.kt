package com.idrnd.idvoice.utils.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
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

    init {
        inflate(context, R.layout.phrase_enrollment_view, this)
    }

    var state = Record
        set(newState) {

            if (field == newState) {
                return
            }

            val statusIndicators = getStatusIndicators()
            val recordAndProcessPhraseView = getRecordAndProcessPhraseView()

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

    var messageAboutProcess
        set(value) {
            getRecordAndProcessPhraseView().messageAboutProcess = value
        }
        get() = getRecordAndProcessPhraseView().messageAboutProcess

    var messageAboutPhrase
        set(value) {
            getRecordAndProcessPhraseView().messageAboutPhrase = value
        }
        get() = getRecordAndProcessPhraseView().messageAboutPhrase

    var phraseForPronouncing
        set(value) {
            getRecordAndProcessPhraseView().phraseForPronouncing = value
        }
        get() = getRecordAndProcessPhraseView().phraseForPronouncing

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

    fun getLifecycleObserver() = getRecordAndProcessPhraseView().getLifecycleObserver()

    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
        // Set background color inner view
        getRecordAndProcessPhraseView()?.apply {
            this.background = background
        } ?: Log.w(
            TAG,
            "Try to set background before finish of view inflating. This is expected behaviour."
        )
    }

    fun setCheckedRecordIndicatorByIndex(index: Int, checked: Boolean) {
        getStatusIndicators()[index].isChecked = checked
    }

    fun visualize() {
        getRecordAndProcessPhraseView().visualize()
    }

    fun stopVisualization() {
        getRecordAndProcessPhraseView().stopVisualization()
    }

    private fun getStatusIndicators() = findViewById<Group>(R.id.statusIndicators)
        .referencedIds
        .map { findViewById<CheckBox>(it) }
        .toTypedArray()

    private fun getRecordAndProcessPhraseView() =
        findViewById<RecordAndProcessPhraseView>(R.id.recordAndProcessPhraseView)

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
