package com.idrnd.idvoice.enrollment.ti

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hadilq.liveevent.LiveEvent
import com.idrnd.idvoice.MainApplication
import com.idrnd.idvoice.R
import com.idrnd.idvoice.enrollment.ti.recorder.TiEnrollmentEventListener
import com.idrnd.idvoice.enrollment.ti.recorder.TiEnrollmentRecorder
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.utils.TemplateFileCreator
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import com.idrnd.idvoice.utils.views.EnrollerView.State
import com.idrnd.idvoice.utils.views.EnrollerView.State.Process
import com.idrnd.idvoice.utils.views.EnrollerView.State.ProcessIsFinished
import net.idrnd.voicesdk.liveness.LivenessEngine
import net.idrnd.voicesdk.media.QualityCheckEngine
import net.idrnd.voicesdk.verify.VoiceTemplateFactory

class EnrollerViewModel(
    context: Context,
    templateFactory: VoiceTemplateFactory,
    templateFileCreator: TemplateFileCreator,
    livenessEngine: LivenessEngine,
    qualityCheckEngine: QualityCheckEngine
) : ViewModel() {

    val enrollmentProgress = MutableLiveData(0)
    val isSpeechRecorded = MutableLiveData<Unit>()
    val messageId = LiveEvent<Int?>()
    val state = MutableLiveData(State.Record)

    private val tiEnrollmentRecorder: TiEnrollmentRecorder
    private val templateFactory: VoiceTemplateFactory
    private val templateFileCreator: TemplateFileCreator

    init {
        this.templateFactory = templateFactory
        this.templateFileCreator = templateFileCreator

        this.tiEnrollmentRecorder = TiEnrollmentRecorder(
            context,
            GlobalPrefs.sampleRate,
            livenessEngine,
            object : TiEnrollmentEventListener {

                override fun onSpeechQualityStatus(status: SpeechQualityStatus) {
                    when (status) {
                        SpeechQualityStatus.TooNoisy ->
                            messageId.postValue(R.string.speech_is_too_noisy)

                        SpeechQualityStatus.TooSmallSpeechTotalLength ->
                            messageId.postValue(R.string.total_speech_length_is_not_enough)
                        // We will never get this value due to 0f value from MIN_RELATIVE_SPEECH_LENGTH_FOR_CHUNKS_IN_MS.
                        // Anyways we are defining it here for consistency.
                        SpeechQualityStatus.TooSmallSpeechRelativeLength ->
                            messageId.postValue(R.string.relative_speech_length_is_not_enough)

                        SpeechQualityStatus.MultipleSpeakersDetected ->
                            messageId.postValue(R.string.multi_speaker_detected_try_again)

                        SpeechQualityStatus.Ok ->
                            messageId.postValue(R.string.please_continue_talking)
                    }
                }

                override fun onLivenessCheckStatus(status: LivenessCheckStatus) {
                    messageId.postValue(R.string.speech_is_not_live_enroll_again)
                }

                override fun onLivenessCheckStarted() {
                    state.postValue(Process)
                }

                override fun onComplete(speechBytes: ByteArray) {
                    // Signal UI as processing
                    state.postValue(Process)

                    // Set message for user as null so that it doesn't appear when a screen is rotated.
                    messageId.postValue(null)

                    // Make template from record
                    val template =
                        templateFactory.createVoiceTemplate(speechBytes, GlobalPrefs.sampleRate)

                    // Save template in file system
                    val templateFile =
                        templateFileCreator.createTemplateFile(GlobalPrefs.templateFilename, true)
                    template.saveToFile(templateFile.absolutePath)

                    // Save template path in prefs
                    GlobalPrefs.templateFilepath = templateFile.absolutePath

                    // Clear resources
                    template.close()

                    // Signal UI that processing is finished
                    state.postValue(ProcessIsFinished)
                }

                override fun onProgress(progress: Float) {
                    enrollmentProgress.postValue((progress * 100).toInt())
                }

                override fun onSpeechPartRecorded() {
                    isSpeechRecorded.postValue(Unit)
                }
            },
            qualityCheckEngine
        )
    }

    fun startRecord() {
        // Process must continue
        if (state.value == Process || state.value == ProcessIsFinished) return
        state.postValue(State.Record)
        tiEnrollmentRecorder.start()
    }

    fun stopRecord() {
        // Process must continue
        if (state.value == Process || state.value == ProcessIsFinished) return
        tiEnrollmentRecorder.stop()
    }

    override fun onCleared() {
        super.onCleared()
        tiEnrollmentRecorder.close()
    }

    companion object {
        val EnrollerViewModelFactory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication
                EnrollerViewModel(
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
