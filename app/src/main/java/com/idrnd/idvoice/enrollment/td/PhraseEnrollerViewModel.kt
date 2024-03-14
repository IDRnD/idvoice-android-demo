package com.idrnd.idvoice.enrollment.td

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hadilq.liveevent.LiveEvent
import com.idrnd.idvoice.MainApplication
import com.idrnd.idvoice.R
import com.idrnd.idvoice.enrollment.td.recorder.TdEnrollmentEventListener
import com.idrnd.idvoice.enrollment.td.recorder.TdEnrollmentRecorder
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.utils.TemplateFileCreator
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.Process
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.ProcessIsFinished
import java.io.File
import net.idrnd.voicesdk.liveness.LivenessEngine
import net.idrnd.voicesdk.media.QualityCheckEngine
import net.idrnd.voicesdk.verify.VoiceTemplateFactory

class PhraseEnrollerViewModel(
    context: Context,
    templateFactory: VoiceTemplateFactory,
    templateFileCreator: TemplateFileCreator,
    livenessEngine: LivenessEngine,
    qualityCheckEngine: QualityCheckEngine
) : ViewModel() {

    val isSpeechRecorded = MutableLiveData<Unit>()
    val needResetVisualization = MutableLiveData<Boolean>()
    val messageId = LiveEvent<Int?>()
    val recordRecordingIndex = MutableLiveData(0)
    val state = MutableLiveData<State>()
    val phraseForPronouncing = MutableLiveData<String>()

    private val tdEnrollmentRecorder: TdEnrollmentRecorder
    private val templateFactory: VoiceTemplateFactory
    private val templateFileCreator: TemplateFileCreator

    private val cacheDir: File

    init {
        this.templateFactory = templateFactory
        this.templateFileCreator = templateFileCreator

        tdEnrollmentRecorder = TdEnrollmentRecorder(
            context,
            GlobalPrefs.sampleRate,
            livenessEngine,
            object : TdEnrollmentEventListener {

                override fun onSpeechQualityStatus(status: SpeechQualityStatus) {
                    when (status) {
                        SpeechQualityStatus.TooNoisy -> {
                            messageId.postValue(R.string.speech_is_too_noisy)
                        }

                        SpeechQualityStatus.TooSmallSpeechTotalLength -> {
                            messageId.postValue(R.string.total_speech_length_is_not_enough)
                        }

                        SpeechQualityStatus.TooSmallSpeechRelativeLength -> {
                            messageId.postValue(R.string.relative_speech_length_is_not_enough)
                        }

                        SpeechQualityStatus.MultipleSpeakersDetected -> {
                            messageId.postValue(R.string.multi_speaker_detected_try_again)
                        }

                        SpeechQualityStatus.Ok -> {
                            messageId.postValue(R.string.please_continue_talking)
                        }
                    }
                }

                override fun onLivenessCheckStarted() {
                    state.postValue(State.LivenessChecking)
                }

                override fun onComplete(speechBytesList: List<ByteArray>) {
                    // Set processing state.
                    state.postValue(Process)

                    // Set message for user as null so that it doesn't appear when a screen is rotated.
                    messageId.postValue(null)

                    val templates = speechBytesList.map {
                        // Make template from records.
                        templateFactory.createVoiceTemplate(it, GlobalPrefs.sampleRate)
                    }

                    // Our complete voice template.
                    val mergedTemplate = templateFactory.mergeVoiceTemplates(templates.toTypedArray())

                    // Save template in file system.
                    val templateFile =
                        templateFileCreator.createTemplateFile(GlobalPrefs.templateFilename, true)
                    mergedTemplate.saveToFile(templateFile.absolutePath)

                    // Save template path in prefs.
                    GlobalPrefs.templateFilepath = templateFile.absolutePath

                    // Clear resources.
                    templates.forEach { it.close() }
                    mergedTemplate.close()

                    // Signal UI that processing is finished.
                    state.postValue(ProcessIsFinished)
                }

                override fun onStartRecordIndex(index: Int) {
                    recordRecordingIndex.postValue(index)
                    state.postValue(State.Record)
                }

                override fun onSpeechPartRecorded() {
                    isSpeechRecorded.postValue(Unit)
                }

                override fun onLivenessCheckStatus(status: LivenessCheckStatus) {
                    if (status == LivenessCheckStatus.SpoofDetected) {
                        messageId.postValue(R.string.speech_is_not_live_enroll_again)
                    }
                    state.postValue(State.Record)
                }
            },
            qualityCheckEngine
        )

        // Init cache dir for output files.
        cacheDir = context.cacheDir

        // Send first phrase.
        phraseForPronouncing.postValue(GlobalPrefs.passwordPhrase)
    }

    fun startRecord() {
        if (state.value == Process || state.value == ProcessIsFinished) {
            // All recording methods are ignored when it is processing.
            return
        }

        recordRecordingIndex.postValue(0)
        tdEnrollmentRecorder.start()
    }

    fun stopRecord() {
        if (state.value == Process || state.value == ProcessIsFinished) {
            // All recording methods are ignored when it is processing.
            return
        }

        // Set message for user as null so that it doesn't appear when a screen is rotated.
        messageId.postValue(null)
        tdEnrollmentRecorder.stop()
    }

    override fun onCleared() {
        super.onCleared()
        tdEnrollmentRecorder.close()
    }

    companion object {
        private val TAG = PhraseEnrollerViewModel::class.simpleName

        val PhraseEnrollerViewModelFactory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication
                PhraseEnrollerViewModel(
                    app.applicationContext,
                    app.voiceTemplateFactory,
                    app.templateFileCreator,
                    app.livenessEngine,
                    app.qualityCheckEngine
                )
            }
        }
    }
}
