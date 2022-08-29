package com.idrnd.idvoice.viewModels.enroll.textDependent

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idrnd.idvoice.R
import com.idrnd.idvoice.model.GlobalPrefs
import com.idrnd.idvoice.utils.TemplateFileCreator
import com.idrnd.idvoice.utils.audioRecorder.FileAudioRecorder
import com.idrnd.idvoice.utils.audioRecorder.MicAudioRecorder
import com.idrnd.idvoice.utils.audioRecorder.MicAudioRecorder.Companion.normAverageAmplitudeEncodedPcm16
import com.idrnd.idvoice.utils.extensions.awaitAll
import com.idrnd.idvoice.utils.speech.analysers.InputSpeechStreamAnalyser
import com.idrnd.idvoice.utils.speech.analysers.SpeechAnalyser
import com.idrnd.idvoice.utils.speech.recorders.FileRecorderWithSpeechAnalyzer
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.Process
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.ProcessIsFinished
import kotlinx.coroutines.*
import net.idrnd.voicesdk.core.common.VoiceTemplate
import net.idrnd.voicesdk.media.SpeechEndpointDetector
import net.idrnd.voicesdk.media.SpeechSummaryEngine
import net.idrnd.voicesdk.media.SpeechSummaryStream
import net.idrnd.voicesdk.verify.QualityShortDescription.*
import net.idrnd.voicesdk.verify.VoiceTemplateFactory
import java.io.File
import java.util.*
import kotlin.concurrent.thread

class PhraseEnrollerViewModel : ViewModel() {

    /**
     * Indicates whether this view model is initialized or not.
     */
    var isInitialized = false
        private set

    val dataForVisualization = MutableLiveData<Float>()
    val needResetVisualization = MutableLiveData<Boolean>()
    val warningMessageForUser = MutableLiveData<String?>()
    val numberRecordedPhrases = MutableLiveData(0)
    val state = MutableLiveData<State>()
    val phraseForPronouncing = MutableLiveData<String>()

    private lateinit var templateFactory: VoiceTemplateFactory
    private lateinit var phrases: Array<String>
    private lateinit var speechRecorder: FileRecorderWithSpeechAnalyzer
    private lateinit var templateFileCreator: TemplateFileCreator

    private lateinit var messageAboutTooNoisyEnvironment: String
    private lateinit var messageAboutSpeechIsNotEnough: String
    private lateinit var messageAboutLongReverberation: String

    private lateinit var speechSummaryStream: SpeechSummaryStream
    private lateinit var speechEndpointDetector: SpeechEndpointDetector

    private lateinit var cacheDir: File

    private val deferredTemplateList = Array<Deferred<VoiceTemplate>?>(NUM_AUDIO_RECORDS) { null }
    private var recordThread: Thread? = null
    @Volatile
    private var stopRecordingFlag = false

    fun init(
        context: Context,
        templateFactory: VoiceTemplateFactory,
        speechSummaryEngine: SpeechSummaryEngine,
        templateFileCreator: TemplateFileCreator
    ) {
        this.templateFactory = templateFactory
        this.templateFileCreator = templateFileCreator

        // Get sample rate from prefs
        val sampleRate = GlobalPrefs.sampleRate

        // Init speech recorder
        speechSummaryStream = speechSummaryEngine.createStream(sampleRate)
        speechEndpointDetector = SpeechEndpointDetector(500, 350, sampleRate)

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

        // Init phrases
        phrases = Array(NUM_AUDIO_RECORDS) { GlobalPrefs.passwordPhrase }

        // Send first phrase
        phraseForPronouncing.postValue(phrases[0])

        // Set that view model is initialized
        isInitialized = true
    }

    fun startRecord() {

        if (state.value == Process || state.value == ProcessIsFinished) {
            // All recording methods are ignored when it is processing
            return
        }

        // Set a stop recording flag to false
        stopRecordingFlag = false

        // Start recording
        recordThread = thread(true) {

            // Start phrase recording in cycle until all records will be recorded
            while (numberRecordedPhrases.value != NUM_AUDIO_RECORDS) {

                // Check a stop recording flag
                if (stopRecordingFlag) {
                    return@thread
                }

                // New output file
                val outputFile = File(cacheDir, UUID.randomUUID().toString()).apply { createNewFile() }

                // Set record state
                state.postValue(State.Record)

                // Start phrase recording
                speechRecorder.startRecordAndSpeechAnalysis(outputFile)

                while (!speechRecorder.isStopped) {

                    if (!speechRecorder.hasNext()) {
                        continue
                    }

                    val resultAnalysis = speechRecorder.next()

                    // Check a stop recording flag
                    if (stopRecordingFlag) {
                        // Stop recording and exit from cycle
                        speechRecorder.stopRecordAndSpeechAnalysis()
                        break
                    }

                    val speechSummary = resultAnalysis.speechSummary

                    if (speechSummary.speechInfo.speechLengthMs > 0) {
                        dataForVisualization.postValue(normAverageAmplitudeEncodedPcm16(resultAnalysis.bytes))
                    }

                    if (resultAnalysis.isSpeechEnded) {
                        speechRecorder.stopRecordAndSpeechAnalysis()
                        break
                    }
                }

                // Check a stop recording flag
                if (stopRecordingFlag) {
                    return@thread
                }

                // Reset visualization
                needResetVisualization.postValue(true)

                // Read bytes from output file
                val recordedBytes = outputFile.readBytes()

                // Check record quality
                val quality = templateFactory.checkQuality(recordedBytes, speechRecorder.sampleRate)

                when (quality.qualityShortDescription) {
                    TOO_NOISY -> {
                        warningMessageForUser.postValue(messageAboutTooNoisyEnvironment)
                        continue
                    }
                    TOO_SMALL_SPEECH_TOTAL_LENGTH -> {
                        warningMessageForUser.postValue(messageAboutSpeechIsNotEnough)
                        continue
                    }
                    OK -> {
                        // Nothing
                    }
                    else -> {
                        throw IllegalStateException("Unexpected quality value ($quality)!")
                    }
                }

                val indexRecordedPhrases = numberRecordedPhrases.value!!

                // Make template from record
                deferredTemplateList[indexRecordedPhrases] = viewModelScope.async(Dispatchers.Default) {
                    templateFactory.createVoiceTemplate(recordedBytes, speechRecorder.sampleRate)
                }

                // Send signal to UI that record is valid and we can to start new record
                runBlocking(Dispatchers.Main) {
                    // Need to update this variable instantly therefore to use Dispatchers.Main and forwarded call a
                    // value
                    numberRecordedPhrases.value = (indexRecordedPhrases + 1)
                }

                // Send new phrase for pronouncing
                phraseForPronouncing.postValue(phrases[indexRecordedPhrases])
            }

            // Check a stop recording flag
            if (stopRecordingFlag) {
                return@thread
            }

            // Send signal to UI that we are processing records
            state.postValue(Process)

            // Set message for user as null so that it doesn't appear when a screen is rotated.
            warningMessageForUser.postValue(null)

            // Wait until templates are created
            val templates = try {
                runBlocking { deferredTemplateList.awaitAll() }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Try to await templates", e)
                return@thread
            }

            // Merge them
            val mergedTemplate = templateFactory.mergeVoiceTemplates(templates)

            // Save template in file system
            val templateFile = templateFileCreator.createTemplateFile(GlobalPrefs.templateFilename, true)

            mergedTemplate.saveToFile(templateFile.absolutePath)

            // Save template path in prefs
            GlobalPrefs.templateFilepath = templateFile.absolutePath

            // Close resources
            templates.forEach { it?.close() }
            mergedTemplate.close()

            // Signal UI that processing is finished
            state.postValue(ProcessIsFinished)
        }
    }

    fun stopRecord() {

        if (state.value == Process || state.value == ProcessIsFinished) {
            // All recording methods are ignored when it is processing
            return
        }

        // Set message for user as null so that it doesn't appear when a screen is rotated.
        warningMessageForUser.postValue(null)

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
        private val TAG = PhraseEnrollerViewModel::class.simpleName
        private const val NUM_AUDIO_RECORDS = 3
    }
}
