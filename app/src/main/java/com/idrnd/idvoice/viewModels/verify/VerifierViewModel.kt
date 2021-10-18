package com.idrnd.idvoice.viewModels.verify

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hadilq.liveevent.LiveEvent
import com.idrnd.idvoice.R
import com.idrnd.idvoice.fragments.verify.results.BiometricsResultFragment
import com.idrnd.idvoice.fragments.verify.results.BiometricsResultFragment.Companion.BUNDLE_LIVENESS_SCORE
import com.idrnd.idvoice.fragments.verify.results.BiometricsResultFragment.Companion.BUNDLE_VERIFICATION_PROBABILITY
import com.idrnd.idvoice.model.BiometricsType.TextDependent
import com.idrnd.idvoice.model.BiometricsType.TextIndependent
import com.idrnd.idvoice.model.GlobalPrefs
import com.idrnd.idvoice.utils.audioRecorder.FileAudioRecorder
import com.idrnd.idvoice.utils.audioRecorder.MicAudioRecorder
import com.idrnd.idvoice.utils.audioRecorder.MicAudioRecorder.Companion.normAverageAmplitudeEncodedPcm16
import com.idrnd.idvoice.utils.speech.analysers.InputSpeechStreamAnalyser
import com.idrnd.idvoice.utils.speech.analysers.SpeechAnalyser
import com.idrnd.idvoice.utils.speech.recorders.FileRecorderWithSpeechAnalyzer
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.*
import kotlinx.coroutines.*
import net.idrnd.voicesdk.antispoof2.AntispoofEngine
import net.idrnd.voicesdk.antispoof2.AntispoofResult
import net.idrnd.voicesdk.core.common.VoiceTemplate
import net.idrnd.voicesdk.media.SpeechEndpointDetector
import net.idrnd.voicesdk.media.SpeechSummaryEngine
import net.idrnd.voicesdk.media.SpeechSummaryStream
import net.idrnd.voicesdk.verify.VerifyResult
import net.idrnd.voicesdk.verify.VoiceTemplateFactory
import net.idrnd.voicesdk.verify.VoiceTemplateMatcher
import java.io.File
import java.util.*
import kotlin.concurrent.thread

class VerifierViewModel : ViewModel() {

    /**
     * Indicates whether this view model is initialized or not.
     */
    var isInitialized = false
        private set

    var onResultFragment = LiveEvent<Fragment>()

    val dataForVisualization = MutableLiveData<Float>()
    val needResetVisualization = MutableLiveData<Boolean>()
    val phraseForPronouncing = MutableLiveData<String>()
    val messageAboutPhraseForPronouncing = MutableLiveData<String>()
    val state = MutableLiveData(Record)

    private lateinit var cacheDir: File
    private lateinit var speechRecorder: FileRecorderWithSpeechAnalyzer
    private lateinit var templateMatcher: VoiceTemplateMatcher
    private lateinit var templateFactory: VoiceTemplateFactory
    private lateinit var antispoofEngine: AntispoofEngine

    private lateinit var speechSummaryStream: SpeechSummaryStream
    private lateinit var speechEndpointDetector: SpeechEndpointDetector

    private lateinit var messageAboutTooNoisyEnvironment: String
    private lateinit var messageAboutSpeechIsNotEnough: String
    private lateinit var messageAboutLongReverberation: String

    private var recordThread: Thread? = null
    private var outputFile: File? = null

    // Flag to stop audio recording job
    @Volatile
    private var stopRecordingFlag: Boolean = false

    fun init(
        context: Context,
        speechSummaryEngine: SpeechSummaryEngine,
        templateMatcher: VoiceTemplateMatcher,
        templateFactory: VoiceTemplateFactory,
        antispoofEngine: AntispoofEngine,
    ) {

        // Init a phrase for pronouncing
        val phraseForPronouncing = when (GlobalPrefs.biometricsType) {
            TextDependent -> GlobalPrefs.passwordPhrase
            TextIndependent -> ""
        }

        // Notify about an user phrase
        this.phraseForPronouncing.postValue(phraseForPronouncing)

        // Init a user notification
        val messageAboutPhraseForPronouncing = when (GlobalPrefs.biometricsType) {
            TextDependent -> context.getString(R.string.please_pronounce_phrase)
            TextIndependent -> context.getString(R.string.please_start_talking)
        }

        // Notify about a message for user
        this.messageAboutPhraseForPronouncing.postValue(messageAboutPhraseForPronouncing)

        // Init voice engines
        this.templateMatcher = templateMatcher
        this.templateFactory = templateFactory

        // Init liveness engine
        this.antispoofEngine = antispoofEngine

        // Get sample rate from prefs
        val sampleRate = GlobalPrefs.sampleRate

        // Init speech recorder
        speechSummaryStream = speechSummaryEngine.createStream(sampleRate)
        speechEndpointDetector = SpeechEndpointDetector(350, 350, sampleRate)

        this.speechRecorder = FileRecorderWithSpeechAnalyzer(
            FileAudioRecorder(MicAudioRecorder(sampleRate)),
            InputSpeechStreamAnalyser(SpeechAnalyser(speechSummaryStream, speechEndpointDetector), sampleRate)
        )

        // Init cache dir for output files
        cacheDir = context.cacheDir

        // Init messages
        messageAboutTooNoisyEnvironment = context.getString(R.string.speech_is_too_noisy)
        messageAboutSpeechIsNotEnough = context.getString(R.string.speech_is_not_enough)
        messageAboutLongReverberation = context.getString(R.string.speech_has_too_long_reverberation)

        // Set that view model is initialized
        isInitialized = true
    }

    fun startRecord() {

        if (state.value == Process || state.value == ProcessIsFinished) {
            // Process must continue
            return
        }

        recordThread = thread(true) {

            // Set false to stop recording flag
            stopRecordingFlag = false

            // New output file
            outputFile = File(cacheDir, UUID.randomUUID().toString()).apply { createNewFile() }

            // Set record state
            state.postValue(Record)

            // Start recording
            speechRecorder.startRecordAndSpeechAnalysis(outputFile!!)

            while (!speechRecorder.isStopped) {

                if (!speechRecorder.hasNext()) {
                    continue
                }

                val resultAnalysis = speechRecorder.next()

                if (stopRecordingFlag) {
                    speechRecorder.stopRecordAndSpeechAnalysis()
                }

                val speechSummary = resultAnalysis.speechSummary

                if (speechSummary.speechInfo.speechLengthMs > 0) {
                    dataForVisualization.postValue(normAverageAmplitudeEncodedPcm16(resultAnalysis.bytes))
                }

                if (resultAnalysis.isSpeechEnded) {
                    speechRecorder.stopRecordAndSpeechAnalysis()
                }
            }

            // Exit point from coroutine
            if (stopRecordingFlag) {
                return@thread
            }

            // Reset visualization
            needResetVisualization.postValue(true)

            // Get audio file
            val outputFile = speechRecorder.outputFile!!
            val sampleRate = speechRecorder.sampleRate

            // Send signal to UI that we are processing records
            state.postValue(Process)

            // Read bytes from output file
            val readBytes = outputFile.readBytes()

            // Make template from record
            val template = templateFactory.createVoiceTemplate(readBytes, sampleRate)

            // Get user's template
            val enrolledTemplate = VoiceTemplate.loadFromFile(GlobalPrefs.templateFilepath)

            // Match enrolled templates with new template
            val deferredVerifyResult = viewModelScope.async(Dispatchers.Default) {
                templateMatcher.matchVoiceTemplates(template, enrolledTemplate)
            }

            val deferredLivenessResult = viewModelScope.async(Dispatchers.Default) {
                antispoofEngine.isSpoof(readBytes, sampleRate)
            }

            // Wait biometrics results
            val verifyResult: VerifyResult
            val livenessResult: AntispoofResult

            try {
                runBlocking {
                    verifyResult = deferredVerifyResult.await()
                    livenessResult = deferredLivenessResult.await()
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Try to await biometrics results", e)

                // Clear resources and return
                template.close()
                enrolledTemplate.close()

                return@thread
            }

            // Clear resources
            template.close()
            enrolledTemplate.close()

            // Add args to result fragment
            val resultFragment = BiometricsResultFragment()

            resultFragment.arguments = Bundle().apply {
                putFloat(BUNDLE_VERIFICATION_PROBABILITY, verifyResult.probability)
                putFloat(BUNDLE_LIVENESS_SCORE, livenessResult.score)
            }

            // Send fragment to UI
            this.onResultFragment.postValue(resultFragment)

            // Signal UI that processing is finished
            state.postValue(ProcessIsFinished)
        }
    }

    fun stopRecord() {

        if (state.value == Process || state.value == ProcessIsFinished) {
            // Process must continue
            return
        }

        // Stop record
        internalStopRecord()
    }

    private fun internalStopRecord() {
        // Set a stop recording flag to true
        stopRecordingFlag = true

        // Stop recording
        speechRecorder.stopRecordAndSpeechAnalysis()
        recordThread?.interrupt()
        recordThread?.join()

        // Reset resources
        speechSummaryStream.reset()
        speechEndpointDetector.reset()
    }

    override fun onCleared() {
        super.onCleared()

        // Stop record
        internalStopRecord()

        // Clear resources
        speechSummaryStream.close()
        speechEndpointDetector.close()
    }

    companion object {
        private val TAG = VerifierViewModel::class.simpleName
    }
}
