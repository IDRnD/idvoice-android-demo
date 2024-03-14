package com.idrnd.idvoice.utils.speech.recorders

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.idrnd.idvoice.utils.audioRecorder.MicAudioRecorder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import net.idrnd.voicesdk.android.media.AssetsExtractor
import net.idrnd.voicesdk.media.SpeechSummaryEngine

/**
 * Records speech and accumulates speech bytes in speechChunkDeque. The speech bytes chunk is
 * send to the listener when speech end is detected.
 *
 * Keeps speechSummaryStream updated once speech start is detected and sends it to listener until
 * speech end is detected.
 * To do so it detects when speech starts and ends.
 * After speech starts, when it detects speech ended it pauses itself. You need to resume it to
 * continue recording.
 */
class SpeechRecorder(
    context: Context,
    var speechRecorderListener: SpeechRecorderListener? = null,
    sampleRate: Int
) {
    /**
     * Microphone recorder.
     */
    private val micAudioRecorder: MicAudioRecorder by lazy { MicAudioRecorder(sampleRate) }

    /**
     * SpeechSummaryEngine to allow us get an instance of SpeechSummaryStream.
     */
    private val speechSummaryEngine =
        SpeechSummaryEngine(
            File(
                AssetsExtractor(context).extractAssets(),
                AssetsExtractor.SPEECH_SUMMARY_INIT_DATA_SUBPATH
            ).absolutePath
        )

    /**
     * SpeechSummaryStream to keep speech information updated.
     */
    private val speechSummaryStream = speechSummaryEngine.createStream(micAudioRecorder.sampleRate)

    /**
     * Container for bytes that contain speech.
     **/
    private val speechChunkInputStream = ByteArrayOutputStream()

    /**
     * Gets speech summary updated information.
     */
    private val totalSpeechSummary
        get() = speechSummaryStream.totalSpeechSummary

    /**
     * Gets updated total speech information.
     */
    private val totalSpeechInfo
        get() = speechSummaryStream.totalSpeechInfo

    /**
     * To know whether we must continue looking for speech beginning or not.
     */
    private var shouldDetectSpeechBeginning = true

    /**
     *  Executor service where to process the audio.
     */
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     *  Handler to post listener calls to UI thread.
     */
    private val handler: Handler = Handler(Looper.getMainLooper())

    /**
     * The minimum speech length recorded to start detecting end of speech.
     */
    var minimumSpeechLengthToDetectSpeechEnd = DEFAULT_MIN_SPEECH_LENGTH_TO_DETECT_SPEECH_END

    /**
     * The maximum silence length recorded to detect end of speech.
     */
    var maximumSilenceLengthToDetectSpeechEnd = DEFAULT_SILENCE_LENGTH_TO_DETECT_SPEECH_END

    /**
     * Tells if speech end is detected.
     */
    private val isSpeechEnded: Boolean
        @Synchronized
        get() {
            val speechSummary = totalSpeechSummary
            if (totalSpeechInfo.speechLengthMs < minimumSpeechLengthToDetectSpeechEnd) {
                return false
            }

            var silenceLengthMs = 0F
            for (speechEvent in speechSummary.speechEvents.reversedArray()) {
                if (speechEvent.isVoice) {
                    break
                }
                silenceLengthMs += speechEvent.audioInterval.endTime - speechEvent.audioInterval.startTime
            }
            return silenceLengthMs >= maximumSilenceLengthToDetectSpeechEnd
        }

    /**
     * Tells whether recording is stopped or not.
     */
    val isStopped: Boolean
        get() = micAudioRecorder.isStopped()

    /**
     * Tells whether recording is paused or not.
     */
    val isPaused: Boolean
        get() = micAudioRecorder.isPaused

    /**
     * Gets the sample rate.
     */
    val sampleRate
        get() = micAudioRecorder.sampleRate

    /**
     * Gets the recording encoding.
     */
    val encoding
        get() = micAudioRecorder.encoding

    /**
     * Start record.
     * @exception IllegalStateException  can't start audio recording cause an unknown reason.
     */
    @Synchronized
    @Throws(IllegalStateException::class, NoSuchElementException::class)
    fun startRecord() {
        // Stop a record.
        if (!isStopped) {
            stopRecord()
        }
        shouldDetectSpeechBeginning = true

        // Start audio recording.
        micAudioRecorder.startAudioRecording()

        if (executor.isTerminated || executor.isShutdown) executor = Executors.newSingleThreadExecutor()
        executor.submit {
            val byteArrayStreamToDetectSpeechBeginning = ByteArrayOutputStream()

            while (!micAudioRecorder.isStopped()) {
                if (!micAudioRecorder.hasNext()) {
                    continue
                }

                val audioBytes = micAudioRecorder.next()

                if (shouldDetectSpeechBeginning) {
                    byteArrayStreamToDetectSpeechBeginning.write(audioBytes)

                    // If 0.5 seconds of audio collected.
                    if (byteArrayStreamToDetectSpeechBeginning.size() >= sampleRate / 2) {
                        val bytesToAnalyzeSpeechBeginning = byteArrayStreamToDetectSpeechBeginning.toByteArray()

                        val speechInfo = speechSummaryEngine.getSpeechSummary(
                            bytesToAnalyzeSpeechBeginning,
                            sampleRate
                        ).speechInfo

                        val hasSpeech = speechInfo.speechLengthMs > 0

                        if (hasSpeech) {
                            shouldDetectSpeechBeginning = false
                            // Collect those initial speech bytes in speech deque.
                            speechChunkInputStream.write(bytesToAnalyzeSpeechBeginning)
                            // Collect the same bytes on speechSummaryStream to keep it updated.
                            speechSummaryStream.addSamples(bytesToAnalyzeSpeechBeginning)
                            // Send totalSpeechSummary to listener.
                            val totalSpeechSummaryToPost = totalSpeechSummary
                            handler.post { speechRecorderListener?.onSpeechSummaryUpdate(totalSpeechSummaryToPost) }
                            // Clear bytes to analyze speech beginning.
                            byteArrayStreamToDetectSpeechBeginning.reset()
                        } else {
                            // Just clear it as we need to collect new 0.5 seconds of fresh audio bytes for
                            // analysis of speech beginning.
                            byteArrayStreamToDetectSpeechBeginning.reset()
                        }
                    }
                } else { // speech beginning is already detected.

                    // If speech end is detected then pause recording, send speech chunk to listener
                    // and clear all.
                    if (isSpeechEnded) {
                        pauseRecord()
                        val speechBytesToPost = speechChunkInputStream.toByteArray()
                        val speechSummaryToPost = speechSummaryStream.totalSpeechSummary
                        handler.post {
                            speechRecorderListener?.onSpeechChunk(
                                speechBytesToPost,
                                speechSummaryToPost
                            )
                        }
                        speechChunkInputStream.reset()
                        speechSummaryStream.reset()
                        shouldDetectSpeechBeginning = true
                    } else {
                        // If speech is not yet ended we continue adding bytes to speech chunk,
                        // speech endpoint detector and to speech summary stream. Also we continue
                        // updating listener with totalSpeechSummary.
                        speechChunkInputStream.write(audioBytes)
                        val previousSpeechLength = totalSpeechSummary.speechInfo.speechLengthMs
                        speechSummaryStream.addSamples(audioBytes)
                        if (totalSpeechSummary.speechInfo.speechLengthMs > previousSpeechLength) {
                            val totalSpeechSummaryToPost = totalSpeechSummary
                            handler.post { speechRecorderListener?.onSpeechSummaryUpdate(totalSpeechSummaryToPost) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Stop record.
     *
     * @exception IOException  if an I/O error occurs.
     */
    @Synchronized
    fun stopRecord() {
        micAudioRecorder.stopAudioRecording()
        speechChunkInputStream.reset()
        executor.shutdown()
    }

    /**
     * Pauses recording.
     */
    fun pauseRecord() {
        micAudioRecorder.pauseAudioRecording()
    }

    /**
     * Resume recording.
     */
    fun resumeRecord() {
        micAudioRecorder.resumeAudioRecording()
    }

    companion object {
        const val DEFAULT_MIN_SPEECH_LENGTH_TO_DETECT_SPEECH_END = 700f
        const val DEFAULT_SILENCE_LENGTH_TO_DETECT_SPEECH_END = 150f
    }
}
