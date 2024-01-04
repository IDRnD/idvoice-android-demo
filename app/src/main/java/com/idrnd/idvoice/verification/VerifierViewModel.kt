package com.idrnd.idvoice.verification

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hadilq.liveevent.LiveEvent
import com.idrnd.idvoice.MainApplication
import com.idrnd.idvoice.R
import com.idrnd.idvoice.preferences.BiometricsType.TextDependent
import com.idrnd.idvoice.preferences.BiometricsType.TextIndependent
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.utils.speech.CheckLivenessType
import com.idrnd.idvoice.utils.speech.DecisionToStopRecording.AsSoonAsPossible
import com.idrnd.idvoice.utils.speech.DecisionToStopRecording.WaitingForEndSpeech
import com.idrnd.idvoice.utils.speech.params.SpeechParams
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorder
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorderListener
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorderParams
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.Process
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.Record
import com.idrnd.idvoice.verification.results.BiometricsResultFragment
import com.idrnd.idvoice.verification.results.BiometricsResultFragment.Companion.BUNDLE_LIVENESS_PROBABILITY
import com.idrnd.idvoice.verification.results.BiometricsResultFragment.Companion.BUNDLE_VERIFICATION_PROBABILITY
import net.idrnd.voicesdk.core.common.VoiceTemplate
import net.idrnd.voicesdk.liveness.LivenessEngine
import net.idrnd.voicesdk.media.QualityCheckEngine
import net.idrnd.voicesdk.media.QualityCheckScenario
import net.idrnd.voicesdk.verify.VoiceTemplateFactory
import net.idrnd.voicesdk.verify.VoiceTemplateMatcher
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

class VerifierViewModel(
    context: Context,
    private val templateMatcher: VoiceTemplateMatcher,
    private val templateFactory: VoiceTemplateFactory,
    livenessEngine: LivenessEngine,
    qualityCheckEngine: QualityCheckEngine,
) : ViewModel() {

    val onResultFragment = LiveEvent<Fragment>()
    val isSpeechRecorded = MutableLiveData<Unit>()
    val phraseForPronouncing = MutableLiveData<String>()
    val title = MutableLiveData<String>()
    val state = MutableLiveData(Record)
    val messageId = LiveEvent<Int?>()
    val progress = MutableLiveData<Int>()

    private val minSpeechLengthMs: Float
        get() = if (GlobalPrefs.biometricsType == TextDependent) 700f else 5000f

    private val minSnrDb: Float
        get() = if (GlobalPrefs.biometricsType == TextDependent) 3f else 8f

    private val cacheDir = context.cacheDir

    // We use recommended IDVoice SDK thresholds for verifications based on biometrics type.
    private val qualityCheckMetricsThresholds = when (GlobalPrefs.biometricsType) {
        TextDependent -> qualityCheckEngine.getRecommendedThresholds(QualityCheckScenario.VERIFY_TD_VERIFICATION)
        TextIndependent -> qualityCheckEngine.getRecommendedThresholds(QualityCheckScenario.VERIFY_TI_VERIFICATION)
    }
    private val speechRecorder = SpeechRecorder(
        context,
        GlobalPrefs.sampleRate,
        // We use values from IDVoice & IDLive Voice best practices guideline
        SpeechRecorderParams(
            qualityCheckMetricsThresholds,
            CheckLivenessType.CheckLiveness,
            if (GlobalPrefs.biometricsType == TextDependent) WaitingForEndSpeech else AsSoonAsPossible,
        ),
        GlobalPrefs.livenessThreshold,
        qualityCheckEngine = qualityCheckEngine
    )

    init {
        speechRecorder.prepare(livenessEngine)

        // Init a phrase for pronouncing
        val phraseForPronouncing = when (GlobalPrefs.biometricsType) {
            TextDependent -> GlobalPrefs.passwordPhrase
            TextIndependent -> ""
        }

        // Notify about an user phrase
        this.phraseForPronouncing.postValue(phraseForPronouncing)

        // Init a user notification
        val title = when (GlobalPrefs.biometricsType) {
            TextDependent -> context.getString(R.string.please_pronounce_phrase)
            TextIndependent -> context.getString(R.string.please_start_talking)
        }

        // Notify about a message for user
        this.title.postValue(title)
    }

    fun startRecord() {
        if (state.value == Process || state.value == ProcessIsFinished) {
            // Process must continue
            return
        }

        speechRecorder.speechRecorderListener = object : SpeechRecorderListener {

            override fun onLivenessCheckStarted() {
                state.postValue(Process)
            }

            override fun onComplete(audioFile: File, speechParams: SpeechParams) {
                // Get quality check error message.
                val qualityErrorMessage = when (speechParams.speechQualityStatus) {
                    SpeechQualityStatus.TooNoisy -> R.string.speech_is_too_noisy
                    SpeechQualityStatus.TooSmallSpeechTotalLength -> R.string.total_speech_length_is_not_enough
                    SpeechQualityStatus.TooSmallSpeechRelativeLength -> R.string.relative_speech_length_is_not_enough
                    SpeechQualityStatus.MultipleSpeakersDetected -> R.string.multi_speaker_detected
                    SpeechQualityStatus.Ok -> null // No error message needed
                }
                // Show quality check error message if any and continue recording.
                if (qualityErrorMessage != null) {
                    messageId.postValue(qualityErrorMessage)
                    state.postValue(Record)
                    speechRecorder.startRecord(getNewCacheFile())
                    return
                }

                // Read bytes from output file
                val readBytes = audioFile.readBytes()

                // Make template from record
                val template = templateFactory.createVoiceTemplate(readBytes, GlobalPrefs.sampleRate)

                // Get user's template
                val enrolledTemplate = VoiceTemplate.loadFromFile(GlobalPrefs.templateFilepath)

                // Match enrolled templates with new template
                val verifyResult = templateMatcher.matchVoiceTemplates(template, enrolledTemplate)

                // Clear resources
                template.close()
                enrolledTemplate.close()

                // Add args to result fragment
                val resultFragment = BiometricsResultFragment()

                resultFragment.arguments = Bundle().apply {
                    putFloat(BUNDLE_VERIFICATION_PROBABILITY, verifyResult.probability)
                    putFloat(BUNDLE_LIVENESS_PROBABILITY, speechParams.livenessResult!!.value.probability)
                }

                // Send fragment to UI
                onResultFragment.postValue(resultFragment)

                // Signal UI that processing is finished
                state.postValue(ProcessIsFinished)

                stopRecord()
            }

            override fun onProgress(progress: Float) {
                isSpeechRecorded.postValue(Unit)
                this@VerifierViewModel.progress.postValue((progress * 100).roundToInt())
            }
        }

        speechRecorder.startRecord(getNewCacheFile())
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
        speechRecorder.close()
    }

    private fun getNewCacheFile(): File {
        return File(cacheDir, "${UUID.randomUUID()}.bin")
    }

    companion object {
        private val TAG = VerifierViewModel::class.simpleName
        val VerifierViewModelFactory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication
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
