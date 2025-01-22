package com.idrnd.idvoice.verification

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hadilq.liveevent.LiveEvent
import com.idrnd.idvoice.MainApplication
import com.idrnd.idvoice.R
import com.idrnd.idvoice.preferences.BiometricsType.TextDependent
import com.idrnd.idvoice.preferences.BiometricsType.TextIndependent
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import com.idrnd.idvoice.utils.speech.recorders.SpeechQualityHelper
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorder
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorderListener
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.Process
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.Record
import com.idrnd.idvoice.verification.results.BiometricsResultFragment
import com.idrnd.idvoice.verification.results.BiometricsResultFragment.Companion.BUNDLE_LIVENESS_PROBABILITY
import com.idrnd.idvoice.verification.results.BiometricsResultFragment.Companion.BUNDLE_MULTIPLE_SPEAKERS_DETECTED_WARNING
import com.idrnd.idvoice.verification.results.BiometricsResultFragment.Companion.BUNDLE_VERIFICATION_PROBABILITY
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.idrnd.voicesdk.core.common.VoiceTemplate
import net.idrnd.voicesdk.liveness.LivenessEngine
import net.idrnd.voicesdk.media.QualityCheckEngine
import net.idrnd.voicesdk.media.SpeechSummary
import net.idrnd.voicesdk.verify.VoiceTemplateFactory
import net.idrnd.voicesdk.verify.VoiceTemplateMatcher

class VerifierViewModel(
    context: Context,
    private val templateMatcher: VoiceTemplateMatcher,
    private val templateFactory: VoiceTemplateFactory,
    private val livenessEngine: LivenessEngine,
    private val qualityCheckEngine: QualityCheckEngine
) : ViewModel() {

    val onResultFragment = LiveEvent<Pair<Class<out Fragment>, Bundle?>>()
    val isSpeechRecorded = MutableLiveData<Unit>()
    val phraseForPronouncing = MutableLiveData<String>()
    val title = MutableLiveData<String>()
    val state = MutableLiveData(Record)
    val messageId = LiveEvent<Int?>()
    val progress = MutableLiveData<Int>()

    private val sampleRate = GlobalPrefs.sampleRate

    /**
     * Tells the minimum speech length by biometrics type.
     */
    val minimumSpeechLengthByBiometricType =
        if (GlobalPrefs.biometricsType == TextDependent) 700f else 5000f

    /**
     * The speech recorder.
     */
    private val speechRecorder = SpeechRecorder(
        context,
        sampleRate = GlobalPrefs.sampleRate
    ).apply {
        minimumSpeechLengthToDetectSpeechEnd = minimumSpeechLengthByBiometricType
    }

    // We use recommended IDVoice SDK thresholds for verifications based on biometrics type.
    private val qualityThresholds =
        SpeechQualityHelper.getVerificationThresholds(qualityCheckEngine)

    /**
     * Job for recording process.
     */
    private var recordJob: Job? = null

    init {
        // Init a phrase for pronouncing.
        val phraseForPronouncing = when (GlobalPrefs.biometricsType) {
            TextDependent -> GlobalPrefs.passwordPhrase
            TextIndependent -> ""
        }

        // Notify about an user phrase.
        this.phraseForPronouncing.postValue(phraseForPronouncing)

        // Init a user notification.
        val title = when (GlobalPrefs.biometricsType) {
            TextDependent -> context.getString(R.string.please_pronounce_phrase)
            TextIndependent -> context.getString(R.string.please_start_talking)
        }

        // Notify about a message for user.
        this.title.postValue(title)
    }

    fun startRecord() {
        if (state.value == Process || state.value == ProcessIsFinished) {
            // Process must continue.
            return
        }

        recordJob = viewModelScope.launch {
            speechRecorder.speechRecorderListener = object : SpeechRecorderListener {

                override fun onSpeechChunk(speechBytes: ByteArray, speechSummary: SpeechSummary) {
                    // Tells the user we are processing the speech.
                    state.postValue(Process)

                    // Gets speech quality status.
                    val qualityResults = qualityCheckEngine.checkQuality(speechBytes, sampleRate, qualityThresholds)
                    val speechQualityStatus = SpeechQualityHelper.getSpeechQualityStatus(qualityResults)

                    // Get error message if any. We ignore MultipleSpeakersDetected case as we don't want to block
                    // the process in this case, instead we will show a warning to the user once the process is ended.
                    val qualityErrorMessage = when (speechQualityStatus) {
                        SpeechQualityStatus.TooNoisy -> R.string.speech_is_too_noisy
                        SpeechQualityStatus.TooSmallSpeechTotalLength -> R.string.total_speech_length_is_not_enough
                        SpeechQualityStatus.TooSmallSpeechRelativeLength ->
                            R.string.relative_speech_length_is_not_enough
                        SpeechQualityStatus.MultipleSpeakersDetected -> null // We show it as a warning later.
                        SpeechQualityStatus.Ok -> null // No error message needed.
                    }

                    // Show quality check error message if any and continue recording.
                    if (qualityErrorMessage != null) {
                        messageId.postValue(qualityErrorMessage)
                        state.postValue(Record)
                        speechRecorder.resumeRecord()
                        return
                    }

                    // Make template from record.
                    val template = templateFactory.createVoiceTemplate(speechBytes, sampleRate)

                    // Get user's template.
                    val enrolledTemplate = VoiceTemplate.loadFromFile(GlobalPrefs.templateFilepath)

                    // Match enrolled templates with new template.
                    val verifyResult = templateMatcher.matchVoiceTemplates(template, enrolledTemplate)

                    // Clear resources
                    template.close()
                    enrolledTemplate.close()

                    // Checks speech liveness.
                    val livenessResult = livenessEngine.checkLiveness(speechBytes, sampleRate)

                    // Get fragment class to show results.
                    val resultFragmentClass = BiometricsResultFragment::class.java

                    // Add results in bundle.
                    val arguments = Bundle().apply {
                        putFloat(BUNDLE_VERIFICATION_PROBABILITY, verifyResult.probability)
                        putFloat(BUNDLE_LIVENESS_PROBABILITY, livenessResult.value.probability)
                        // Define whether multiple speakers detected warning will be visible or not
                        val showMultipleSpeakersWarning =
                            speechQualityStatus == SpeechQualityStatus.MultipleSpeakersDetected
                        putBoolean(BUNDLE_MULTIPLE_SPEAKERS_DETECTED_WARNING, showMultipleSpeakersWarning)
                    }

                    // Send fragment to UI.
                    onResultFragment.postValue(Pair(resultFragmentClass, arguments))

                    // Signal UI that processing is finished.
                    state.postValue(ProcessIsFinished)

                    // Stop recording.
                    stopRecord()
                }

                override fun onSpeechSummaryUpdate(speechSummary: SpeechSummary) {
                    // Signal UI speech is happening.
                    isSpeechRecorded.postValue(Unit)

                    // Gets the recorded speech progress for current biometric type.
                    val recordedProgress = speechSummary.speechInfo.speechLengthMs / minimumSpeechLengthByBiometricType

                    // Posts the progress as percentage to UI.
                    progress.postValue((recordedProgress * 100).roundToInt())
                }
            }
        }
        speechRecorder.startRecord()
    }

    fun stopRecord() {
        if (state.value == Process || state.value == ProcessIsFinished) {
            // Process must continue
            return
        }

        // Stop record
        speechRecorder.stopRecord()
    }

    override fun onCleared() {
        super.onCleared()
        stopRecord()
    }

    companion object {
        private val TAG = VerifierViewModel::class.simpleName
        val VerifierViewModelFactory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication
                VerifierViewModel(
                    app.applicationContext,
                    app.voiceTemplateMatcher,
                    app.voiceTemplateFactory,
                    app.livenessEngine,
                    app.qualityCheckEngine
                )
            }
        }
    }
}
