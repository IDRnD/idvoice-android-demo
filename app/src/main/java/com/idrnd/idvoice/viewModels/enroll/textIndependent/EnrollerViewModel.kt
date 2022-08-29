package com.idrnd.idvoice.viewModels.enroll.textIndependent

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.idrnd.idvoice.R
import com.idrnd.idvoice.model.GlobalPrefs
import com.idrnd.idvoice.utils.TemplateFileCreator
import com.idrnd.idvoice.utils.audioRecorder.FileAudioRecorder
import com.idrnd.idvoice.utils.audioRecorder.MicAudioRecorder
import com.idrnd.idvoice.utils.speech.analysers.InputSpeechStreamAnalyser
import com.idrnd.idvoice.utils.speech.analysers.SpeechAnalyser
import com.idrnd.idvoice.utils.speech.recorders.FileRecorderWithSpeechAnalyzer
import com.idrnd.idvoice.utils.views.EnrollerView.State
import com.idrnd.idvoice.utils.views.EnrollerView.State.Process
import com.idrnd.idvoice.utils.views.EnrollerView.State.ProcessIsFinished
import kotlinx.coroutines.*
import net.idrnd.voicesdk.media.SpeechEndpointDetector
import net.idrnd.voicesdk.media.SpeechSummaryEngine
import net.idrnd.voicesdk.media.SpeechSummaryStream
import net.idrnd.voicesdk.verify.QualityShortDescription.*
import net.idrnd.voicesdk.verify.VoiceTemplateFactory
import java.io.File
import java.util.*
import kotlin.concurrent.thread

class EnrollerViewModel : ViewModel() {

    /**
     * Indicates whether this view model is initialized or not.
     */
    var isInitialized = false
        private set

    val enrollmentProgress = MutableLiveData(0)
    val warningMessageForUser = MutableLiveData<String?>()
    val state = MutableLiveData(State.Record)
    val audioRecordIsPaused: Boolean
        get() = speechRecorder.isPaused

    private lateinit var templateFactory: VoiceTemplateFactory
    private lateinit var speechRecorder: FileRecorderWithSpeechAnalyzer

    private lateinit var templateFileCreator: TemplateFileCreator

    private lateinit var messageAboutTooNoisyEnvironment: String
    private lateinit var messageAboutSpeechIsNotEnough: String
    private lateinit var messageAboutLongReverberation: String

    private lateinit var speechSummaryStream: SpeechSummaryStream
    private lateinit var speechEndpointDetector: SpeechEndpointDetector

    private lateinit var cacheDir: File
    private lateinit var outputFile: File

    private var recordThread: Thread? = null

    fun init(
        context: Context,
        templateFactory: VoiceTemplateFactory,
        templateFileCreator: TemplateFileCreator,
        speechSummaryEngine: SpeechSummaryEngine,
    ) {
        this.templateFactory = templateFactory
        this.templateFileCreator = templateFileCreator

        // Init cache dir
        cacheDir = context.cacheDir

        // Init output file
        outputFile = File(cacheDir, UUID.randomUUID().toString()).apply { createNewFile() }

        // Get sample rate from prefs
        val sampleRate = GlobalPrefs.sampleRate

        // Init speech recorder
        speechSummaryStream = speechSummaryEngine.createStream(sampleRate)
        speechEndpointDetector = SpeechEndpointDetector(350, 350, sampleRate)

        this.speechRecorder = FileRecorderWithSpeechAnalyzer(
            FileAudioRecorder(MicAudioRecorder(sampleRate)),
            InputSpeechStreamAnalyser(SpeechAnalyser(speechSummaryStream, speechEndpointDetector), sampleRate)
        )

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
            // Cycle because user can several times record of audio due to low quality of record.
            while (!Thread.interrupted()) {
                // Start speech recording if speech is not enough
                if (enrollmentProgress.value!! < MAX_ENROLLMENT_PROGRESS) {

                    // Set record state
                    state.postValue(State.Record)

                    // Start speech recording
                    speechRecorder.startRecordAndSpeechAnalysis(outputFile)

                    while (!speechRecorder.isStopped) {

                        // Check that VoiceSDK has information about speech
                        if (!speechRecorder.hasNext()) {
                            continue
                        }

                        // Grab the analysis results
                        val resultAnalysis = speechRecorder.next()

                        // Get the total speech length
                        val speechSummary = resultAnalysis.speechSummary
                        val speechLength = speechSummary.speechInfo.speechLengthMs

                        // Calculate progress of enrollment
                        val progress = ((speechLength / GlobalPrefs.minSpeechLengthForTiEnrollmentInMs) * 100).toInt()

                        if (progress >= MAX_ENROLLMENT_PROGRESS) {
                            // Post data for visualization progress
                            enrollmentProgress.postValue(MAX_ENROLLMENT_PROGRESS)

                            // Stop recording
                            speechRecorder.stopRecordAndSpeechAnalysis()
                        } else {
                            // Post data for visualization progress
                            enrollmentProgress.postValue(progress)
                        }
                    }
                }

                if (Thread.interrupted()) {
                    return@thread
                }

                // Set processing state
                state.postValue(Process)

                // Set message for user as null so that it doesn't appear when a screen is rotated.
                warningMessageForUser.postValue(null)

                // Read bytes from output file
                val recordedBytes = outputFile.readBytes()

                // Check record quality
                val quality = templateFactory.checkQuality(recordedBytes, speechRecorder.sampleRate)

                when (quality.qualityShortDescription) {
                    TOO_NOISY -> {
                        warningMessageForUser.postValue(messageAboutTooNoisyEnvironment)
                        resetProgressAndUpdateOutputFile()
                        continue
                    }
                    TOO_SMALL_SPEECH_TOTAL_LENGTH -> {
                        warningMessageForUser.postValue(messageAboutSpeechIsNotEnough)
                        resetProgressAndUpdateOutputFile()
                        continue
                    }
                    OK -> {
                        // Nothing
                    }
                    else -> {
                        throw IllegalStateException("Unexpected quality value ($quality)!")
                    }
                }

                // Make template from record
                val template = templateFactory.createVoiceTemplate(recordedBytes, speechRecorder.sampleRate)

                // Save template in file system
                val templateFile = templateFileCreator.createTemplateFile(GlobalPrefs.templateFilename, true)
                template.saveToFile(templateFile.absolutePath)

                // Save template path in prefs
                GlobalPrefs.templateFilepath = templateFile.absolutePath

                // Clear resources
                template.close()

                // Signal UI that processing is finished
                state.postValue(ProcessIsFinished)

                // Finish job
                return@thread
            }
        }
    }

    fun pauseRecord() {

        if (state.value == Process || state.value == ProcessIsFinished) {
            // Process must continue
            return
        }

        // Set message for user as null so that it doesn't appear when a screen is rotated.
        warningMessageForUser.postValue(null)

        speechRecorder.pauseRecordAndSpeechAnalysis()
    }

    fun resumeRecord() {

        if (state.value == Process || state.value == ProcessIsFinished) {
            // Process must continue
            return
        }

        speechRecorder.resumeRecordAndSpeechAnalysis()
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
        recordThread?.interrupt()
        speechRecorder.stopRecordAndSpeechAnalysis()
        recordThread?.join()
    }

    private fun resetProgressAndUpdateOutputFile() {
        // Reset progress
        runBlocking(Dispatchers.Main) {
            enrollmentProgress.value = 0
        }

        // Update output file
        outputFile = File(cacheDir, UUID.randomUUID().toString()).apply { createNewFile() }
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
        private const val MAX_ENROLLMENT_PROGRESS = 100
    }
}
