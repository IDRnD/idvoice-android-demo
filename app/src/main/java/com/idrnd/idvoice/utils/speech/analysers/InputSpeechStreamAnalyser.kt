package com.idrnd.idvoice.utils.speech.analysers

import android.util.Log
import com.idrnd.idvoice.utils.audioRecorder.MicAudioRecorder.Companion.getAudioSize
import com.idrnd.idvoice.utils.speech.analysers.InputSpeechStreamAnalyser.AnalysingStatus.Idle
import com.idrnd.idvoice.utils.speech.model.AnalysisResult
import net.idrnd.voicesdk.common.VoiceSdkEngineException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.concurrent.thread

/**
 * Input speech stream analyser that make various types of analysis of speech.
 *
 * @param speechAnalyser analyser of speech.
 */
class InputSpeechStreamAnalyser(
    private val speechAnalyser: SpeechAnalyser,
    sampleRate: Int
) : Iterator<AnalysisResult> {

    /**
     * Required buffer size for a speech analysis.
     */
    val bufferSize = getAudioSize(MIN_AUDIO_LENGTH_FOR_ANALYSIS_IN_MS, sampleRate)

    @Volatile
    var analysingStatus = Idle
        private set

    private var analyserThread: Thread? = null
    private val resultAnalysisDeque = ConcurrentLinkedDeque<AnalysisResult>()

    /**
     * Read and analysis input stream. Note that after input stream has been full read analysis not will be finished
     * and will be waiting new data in input stream.
     *
     * @param inputStream audio input stream. **It is the caller's responsibility to close [inputStream]**.
     */
    fun startAnalysis(inputStream: InputStream) {

        // Stop analysis
        stopAnalysis()

        // Set status to analysis
        analysingStatus = AnalysingStatus.Analysis

        // Start input stream reading
        analyserThread = thread(true) {

            val bufferForRead = ByteArray(bufferSize)
            val byteBufferForAnalysis = ByteBuffer.allocate(bufferSize * 2)

            while (analysingStatus == AnalysingStatus.Analysis) {

                val numReadBytes = inputStream.read(bufferForRead)

                if (numReadBytes == -1) {
                    continue
                }

                if (numReadBytes == 0) {
                    Log.e(TAG, "Can't read bytes from input stream")
                    continue
                }

                byteBufferForAnalysis.put(bufferForRead.copyOfRange(0, numReadBytes))
                var bufferPosition = byteBufferForAnalysis.position()

                // Find a closet even position in buffer. It is necessary because analyser can analysis only byte
                // array with even number of bytes.
                if (bufferPosition.isOdd()) {
                    bufferPosition -= 1
                }

                if (bufferPosition >= bufferSize) {

                    // * Get bytes for analysis
                    val bytesForAnalysis = ByteArray(bufferPosition)

                    // Move position to start buffer
                    byteBufferForAnalysis.position(0)

                    // Copy bytes
                    byteBufferForAnalysis.get(bytesForAnalysis)

                    // Clear buffer
                    byteBufferForAnalysis.clear()

                    try {
                        speechAnalyser.addSamples(bytesForAnalysis)
                    } catch (e: VoiceSdkEngineException) {
                        Log.e(TAG, "Problem with speech analysis", e)
                        throw e
                    }

                    val analysisResult = AnalysisResult(
                        bytesForAnalysis,
                        speechAnalyser.speechSummary,
                        speechAnalyser.isSpeechEnded
                    )

                    // Check stop flag before send information
                    if (analysingStatus == AnalysingStatus.Analysis) {
                        resultAnalysisDeque.addLast(analysisResult)
                    } else {
                        break
                    }
                }
            }
        }
    }

    /**
     * Stop analysing.
     */
    fun stopAnalysis() {
        // Stop input stream reading
        analysingStatus = Idle

        // Wait when thread is dead
        analyserThread?.join()
        analyserThread = null

        // Reset analyser
        speechAnalyser.reset()
    }

    override fun hasNext(): Boolean {
        return resultAnalysisDeque.isNotEmpty()
    }

    override fun next(): AnalysisResult {
        return resultAnalysisDeque.removeFirst()
    }

    private fun Int.isEven(): Boolean {

        if (this == 0) {
            return false
        }

        return (this % 2 == 0)
    }

    private fun Int.isOdd(): Boolean {

        if (this == 0) {
            return false
        }

        return !isEven()
    }

    companion object {
        private val TAG = InputSpeechStreamAnalyser::class.simpleName
        private const val MIN_AUDIO_LENGTH_FOR_ANALYSIS_IN_MS = 64L
    }

    enum class AnalysingStatus {
        Analysis,
        Idle
    }
}
