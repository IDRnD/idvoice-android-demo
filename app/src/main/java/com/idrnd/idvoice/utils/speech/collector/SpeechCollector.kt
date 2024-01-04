package com.idrnd.idvoice.utils.speech.collector

import net.idrnd.voicesdk.media.SpeechSummaryEngine
import java.io.ByteArrayOutputStream

class SpeechCollector(
    private val speechSummaryEngine: SpeechSummaryEngine,
    private val minSpeechLengthMs: Float,
    private val sampleRate: Int
) : AutoCloseable {

    private val speechSummaryStream = speechSummaryEngine.createStream(sampleRate)
    private val buffer = ByteArrayOutputStream()

    fun addSamples(speech: ByteArray) {
        buffer.write(speech)
        speechSummaryStream.addSamples(speech)
    }

    fun isThereSpeechEnough(): Boolean {
        return speechSummaryStream.totalSpeechInfo.speechLengthMs >= minSpeechLengthMs
    }

    /**
     * Tells whether recorded bytes contain speech since beginning or not.
     * Use case:
     * I've a recorded audio byte array with no speech in it, then those bytes can be discarded.
     *
     * It's false when array is empty.
     * It's false when less than 0.5 seconds (in other words sampleRate/2) of samples were added.
     * It's true when background length equals total length
     */
    fun hasNoSpeechSinceBeginning(): Boolean {
        val recordedBytes = buffer.toByteArray()
        if (recordedBytes.isEmpty()) return false
        if (recordedBytes.size < sampleRate / 2) return false
        val speechInfo = speechSummaryEngine.getSpeechSummary(recordedBytes, sampleRate).speechInfo
        return speechInfo.backgroundLengthMs == speechInfo.totalLengthMs
    }

    fun getSpeech(): ByteArray {
        return buffer.toByteArray()
    }

    fun reset() {
        speechSummaryStream.reset()
        buffer.reset()
        speechLengthLastTime = 0f
    }

    fun getSpeechLengthInMs(): Float {
        return speechSummaryStream.totalSpeechInfo.speechLengthMs
    }

    private var speechLengthLastTime = 0f

    override fun close() {
        speechSummaryEngine.close()
    }
}
