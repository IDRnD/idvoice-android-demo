package com.idrnd.idvoice.utils.speech.analysers

import net.idrnd.voicesdk.media.SpeechEndpointDetector
import net.idrnd.voicesdk.media.SpeechSummary
import net.idrnd.voicesdk.media.SpeechSummaryStream

/**
 * Speech analyser that make various types of analysis of speech.
 *
 * @param speechSummaryStream for speech counting. **It is the caller's responsibility to close** [speechSummaryStream].
 * @param speechEndpointDetector for speech endpoint detection. **It is the caller's responsibility to close**
 * [speechEndpointDetector].
 */
class SpeechAnalyser(
    private val speechSummaryStream: SpeechSummaryStream,
    private val speechEndpointDetector: SpeechEndpointDetector
) {

    /**
     * Total speech summary of whole record.
     */
    val speechSummary: SpeechSummary
        get() {
            return speechSummaryStream.totalSpeechSummary
        }

    /**
     * Returns has speech ended. After returning true it makes reset and start analysis again with next bytes.
     */
    val isSpeechEnded: Boolean
        get() {
            val isSpeechEnded = speechEndpointDetector.isSpeechEnded

            if (isSpeechEnded) {
                speechEndpointDetector.reset()
            }

            return isSpeechEnded
        }

    fun addSamples(byteArray: ByteArray) {
        speechSummaryStream.addSamples(byteArray)
        speechEndpointDetector.addSamples(byteArray)
    }

    fun reset() {
        speechSummaryStream.reset()
        speechEndpointDetector.reset()
    }
}
