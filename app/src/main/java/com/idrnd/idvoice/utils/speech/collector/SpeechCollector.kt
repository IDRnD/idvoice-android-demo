package com.idrnd.idvoice.utils.speech.collector

import android.content.Context
import net.idrnd.voicesdk.android.media.AssetsExtractor
import net.idrnd.voicesdk.media.SpeechSummaryEngine
import net.idrnd.voicesdk.media.SpeechSummaryStream
import java.io.ByteArrayOutputStream
import java.io.File

class SpeechCollector : AutoCloseable {

    private val buffer = ByteArrayOutputStream()
    private var speechSummaryEngine: SpeechSummaryEngine? = null
    private val speechSummaryStream: SpeechSummaryStream
    private val minSpeechLengthMs: Float

    constructor(context: Context, minSpeechLengthMs: Float, sampleRate: Int) {
        this.minSpeechLengthMs = minSpeechLengthMs
        speechSummaryEngine = SpeechSummaryEngine(
            File(
                AssetsExtractor(context).extractAssets(),
                AssetsExtractor.SPEECH_SUMMARY_INIT_DATA_SUBPATH,
            ).absolutePath,
        )
        speechSummaryStream = speechSummaryEngine!!.createStream(sampleRate)
    }

    constructor(assetsFolder: File, minSpeechLengthMs: Float, sampleRate: Int) {
        this.minSpeechLengthMs = minSpeechLengthMs
        speechSummaryEngine = SpeechSummaryEngine(
            File(assetsFolder, AssetsExtractor.SPEECH_SUMMARY_INIT_DATA_SUBPATH).absolutePath,
        )
        speechSummaryStream = speechSummaryEngine!!.createStream(sampleRate)
    }

    constructor(speechSummaryStream: SpeechSummaryStream, minSpeechLengthMs: Float) {
        this.minSpeechLengthMs = minSpeechLengthMs
        this.speechSummaryStream = speechSummaryStream
    }

    fun addSamples(speech: ByteArray) {
        buffer.write(speech)
        speechSummaryStream.addSamples(speech)
    }

    fun isThereSpeechEnough(): Boolean {
        return speechSummaryStream.totalSpeechInfo.speechLengthMs >= minSpeechLengthMs
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

    fun isSpeechLengthIncresedLastTime(): Boolean {
        val currentSpeechLength = getSpeechLengthInMs()
        if (currentSpeechLength > speechLengthLastTime) {
            speechLengthLastTime = getSpeechLengthInMs()
            speechLengthLastTime = currentSpeechLength
            return true
        }
        speechLengthLastTime = getSpeechLengthInMs()
        return false
    }

    override fun close() {
        speechSummaryEngine?.close()
    }
}
