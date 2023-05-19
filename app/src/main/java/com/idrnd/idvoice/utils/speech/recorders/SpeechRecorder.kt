package com.idrnd.idvoice.utils.speech.recorders

import android.content.Context
import com.idrnd.idvoice.utils.audioRecorder.FileAudioRecorder
import com.idrnd.idvoice.utils.audioRecorder.MicAudioRecorder
import com.idrnd.idvoice.utils.extensions.readInChunksWhile
import com.idrnd.idvoice.utils.speech.CheckLivenessType
import com.idrnd.idvoice.utils.speech.DecisionToStopRecording
import com.idrnd.idvoice.utils.speech.collector.SpeechCollector
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.params.SpeechParams
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import kotlinx.coroutines.runBlocking
import net.idrnd.voicesdk.android.media.AssetsExtractor
import net.idrnd.voicesdk.liveness.LivenessEngine
import net.idrnd.voicesdk.liveness.LivenessResult
import net.idrnd.voicesdk.media.SNRComputer
import net.idrnd.voicesdk.media.SpeechEndpointDetector
import net.idrnd.voicesdk.media.SpeechSummaryEngine
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * File audio recorder with speech counter, liveness checker and speech endpoint detector.
 */
class SpeechRecorder(
    context: Context,
    sampleRate: Int,
    val speechRecorderParams: SpeechRecorderParams,
    private val livenessThreshold: Float = LIVENESS_THRESHOLD,
    var speechRecorderListener: SpeechRecorderListener? = null,
) : AutoCloseable {

    private val audioRecorder: FileAudioRecorder

    private var pauseLatch = CountDownLatch(0)

    val outputFile: File?
        get() = audioRecorder.outputFile

    val sampleRate: Int
        get() = audioRecorder.sampleRate

    val bufferSizeForRecord: Int
        get() = audioRecorder.bufferSize

    /**
     * Returns true if audio recorder is paused and false otherwise.
     */
    val isPaused: Boolean
        get() = audioRecorder.isPaused

    /**
     * Returns true if audio recorder is stopped and false otherwise.
     */
    val isStopped: Boolean
        get() = audioRecorder.isStopped

    private lateinit var futureAssetsDir: Future<File>
    private val assetsDir by lazy { runBlocking { futureAssetsDir.get() } }
    private val executor: ExecutorService
    private var isExternalLivenessEngine = false

    private var livenessEngine: LivenessEngine? = null
    private lateinit var speechSummaryEngine: SpeechSummaryEngine
    private var speechEndpointDetector: SpeechEndpointDetector? = null
    private lateinit var speechCollector: SpeechCollector
    private lateinit var snrComputer: SNRComputer
    private var lastProgress = -1f

    var isPreparing = false
        private set

    var isPrepared = false
        private set

    init {
        audioRecorder = FileAudioRecorder(MicAudioRecorder(sampleRate))
        executor = Executors.newSingleThreadExecutor()
        futureAssetsDir = executor.submit<File> { AssetsExtractor(context).extractAssets() }
    }

    /**
     * Prepare recorder. Should be called as soon as possible.
     * If it has not been called, then it will be called in start() function.
     */
    fun prepare() {
        if (isPreparing || isPrepared) return

        isPreparing = true
        isExternalLivenessEngine = false
        executor.submit {
            if (speechRecorderParams.checkLivenessType == CheckLivenessType.CheckLiveness) {
                livenessEngine =
                    LivenessEngine(File(assetsDir, AssetsExtractor.LIVENESS_INIT_DATA_SUBPATH).absolutePath)
            }

            prepareWithoutLivenessEngine()

            isPrepared = true
            isPreparing = false
        }
    }

    /**
     * Prepare recorder. Should be called as soon as possible.
     * If it has not been called, then it will be called in start() function.
     *
     * @param livenessEngine Liveness engine can be passed to reduce preparation time.
     */
    fun prepare(livenessEngine: LivenessEngine) {
        if (isPreparing || isPrepared) return

        isPreparing = true
        isExternalLivenessEngine = true
        this.livenessEngine = livenessEngine
        executor.submit {
            prepareWithoutLivenessEngine()
            isPrepared = true
            isPreparing = false
        }
    }

    private fun prepareWithoutLivenessEngine() {
        speechSummaryEngine =
            SpeechSummaryEngine(File(assetsDir, AssetsExtractor.SPEECH_SUMMARY_INIT_DATA_SUBPATH).absolutePath)

        if (speechRecorderParams.decisionToStopRecording == DecisionToStopRecording.WaitingForEndSpeech) {
            // We use configuration with minSpeechLengthMs = 0 because the speech we calculated with SpeechSummaryStream
            // and we need SpeechEndpointDetector only for detection of end speech.
            speechEndpointDetector = SpeechEndpointDetector(0, MAX_SILENCE_LENGTH_MS, sampleRate)
        }

        speechCollector = SpeechCollector(
            speechSummaryEngine.createStream(sampleRate),
            speechRecorderParams.minSpeechLengthInMs,
        )

        snrComputer = SNRComputer(File(assetsDir, AssetsExtractor.SNR_COMPUTER_INIT_DATA_SUBPATH).absolutePath)
    }

    /**
     * Start record.
     *
     * @param outputFile Speech will be recorded tn this file.
     */
    fun startRecord(outputFile: File) {
        if (!isPreparing && !isPrepared) {
            prepare()
        }

        // Stop record if that has been start
        stopRecord()

        audioRecorder.startRecord(outputFile)

        executor.submit {
            val unpreparedChunkSize = MicAudioRecorder.getAudioSize(MIN_AUDIO_LENGTH_FOR_ANALYSIS_IN_MS, sampleRate)

            // VoiceSdk engine can process only even byte arrays
            val chunkSize = if (unpreparedChunkSize % 2 != 0) unpreparedChunkSize + 1 else unpreparedChunkSize

            audioRecorder.outputFile!!.inputStream().use { stream ->
                stream.readInChunksWhile(chunkSize) { !audioRecorder.isStopped }.forEach { chunk ->
                    // This is implementation pause functionality.
                    // To wait until an user calls resume() method if the pauseLatch count more then 0.
                    pauseLatch.await()

                    speechCollector.addSamples(chunk)

                    val progress = speechCollector.getSpeechLengthInMs() / speechRecorderParams.minSpeechLengthInMs
                    if (progress != lastProgress) {
                        speechRecorderListener?.onProgress(progress)
                        lastProgress = progress
                    }

                    if (!speechCollector.isThereSpeechEnough()) return@forEach

                    if (speechRecorderParams.decisionToStopRecording == DecisionToStopRecording.WaitingForEndSpeech) {
                        speechEndpointDetector!!.addSamples(chunk)
                        if (!speechEndpointDetector!!.isSpeechEnded) return@forEach
                        speechEndpointDetector!!.reset()
                    }

                    // To prevent record more speech than necessary
                    pauseRecord()

                    val speech = speechCollector.getSpeech()
                    val speechLengthInMs = speechCollector.getSpeechLengthInMs()
                    speechCollector.reset()
                    val snr = snrComputer.compute(speech, sampleRate)
                    if (snr < speechRecorderParams.minSnrInDb) {
                        stopRecord()
                        speechRecorderListener?.onComplete(
                            outputFile,
                            SpeechParams(
                                speechLengthInMs,
                                snr,
                                SpeechQualityStatus.TooNoisy,
                                LivenessCheckStatus.Unknown,
                            ),
                        )
                        return@submit
                    }

                    var livenessCheckStatus = LivenessCheckStatus.Unknown
                    var livenessResult: LivenessResult? = null
                    if (speechRecorderParams.checkLivenessType == CheckLivenessType.CheckLiveness) {
                        speechRecorderListener?.onLivenessCheckStarted()
                        livenessResult = livenessEngine!!.checkLiveness(speech, sampleRate)
                        livenessCheckStatus = if (livenessResult.value.probability >= livenessThreshold) {
                            LivenessCheckStatus.LiveDetected
                        } else {
                            LivenessCheckStatus.SpoofDetected
                        }
                    }

                    stopRecord()
                    speechRecorderListener?.onComplete(
                        outputFile,
                        SpeechParams(
                            speechLengthInMs,
                            snr,
                            SpeechQualityStatus.Ok,
                            livenessCheckStatus,
                            livenessResult,
                        ),
                    )
                    return@submit
                }
            }
        }
    }

    fun pauseRecord() {
        if (pauseLatch.count > 0) return
        audioRecorder.pauseRecord()
        pauseLatch = CountDownLatch(1)
    }

    fun resumeRecord() {
        audioRecorder.resumeRecord()
        pauseLatch.countDown()
    }

    fun stopRecord() {
        // Stop audio record
        audioRecorder.stopRecord()
        pauseLatch.countDown()
    }

    override fun close() {
        executor.shutdown()
        // We don't have responsibility to close external resources.
        if (!isExternalLivenessEngine) {
            livenessEngine?.close()
        }
        speechSummaryEngine.close()
        speechCollector.close()
        snrComputer.close()
        speechEndpointDetector?.close()
    }

    companion object {
        private val TAG = SpeechRecorder::class.simpleName
        private const val MIN_AUDIO_LENGTH_FOR_ANALYSIS_IN_MS = 64
        private const val LIVENESS_THRESHOLD = 0.5f
        private const val MAX_SILENCE_LENGTH_MS = 350
    }
}
