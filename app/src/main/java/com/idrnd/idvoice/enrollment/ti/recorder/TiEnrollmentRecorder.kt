package com.idrnd.idvoice.enrollment.ti.recorder

import android.content.Context
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.utils.speech.CheckLivenessType
import com.idrnd.idvoice.utils.speech.DecisionToStopRecording
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.params.SpeechParams
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorder
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorderListener
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorderParams
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.idrnd.voicesdk.android.media.AssetsExtractor
import net.idrnd.voicesdk.liveness.LivenessEngine
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class TiEnrollmentRecorder(
    context: Context,
    private val sampleRate: Int,
    livenessEngine: LivenessEngine,
    var eventListener: TiEnrollmentEventListener? = null,
) : AutoCloseable {

    private var outputStream = ByteArrayOutputStream()
    private var outputSpeechLength = 0f
    private val speechRecorder = SpeechRecorder(
        context,
        sampleRate,
        // We use values from IDVoice & IDLive Voice best practices guideline
        SpeechRecorderParams(
            MIN_SPEECH_LENGTH_FOR_CHUNKS_IN_MS,
            MIN_SNR_IN_DB,
            CheckLivenessType.DoesntCheckLiveness,
            DecisionToStopRecording.AsSoonAsPossible,
        ),
        GlobalPrefs.livenessThreshold,
    )
    private val cacheDir = context.cacheDir
    private val defLivenessEngine = GlobalScope.async {
        val assetsDir = AssetsExtractor(context).extractAssets()
        // Initialization of LivenessEnginge requires a lot of time so we initializes this in the separated thread.
        LivenessEngine(File(assetsDir, AssetsExtractor.LIVENESS_INIT_DATA_SUBPATH).absolutePath)
    }

    val isPaused: Boolean
        get() = speechRecorder.isPaused

    val isRecording: Boolean
        get() = !speechRecorder.isStopped && !speechRecorder.isPaused

    init {
        speechRecorder.prepare(livenessEngine)
    }

    /**
     * Start the enrollment recording.
     * The prepared audio for voice template creating will be returned to the event listener.
     */
    @Synchronized
    fun start() {
        stop()

        speechRecorder.speechRecorderListener = object : SpeechRecorderListener {

            override fun onLivenessCheckStarted() {
                // Nothing
            }

            override fun onComplete(audioFile: File, speechParams: SpeechParams) {
                eventListener?.onSpeechQualityStatus(speechParams.speechQualityStatus)

                if (speechParams.speechQualityStatus != SpeechQualityStatus.Ok) {
                    speechRecorder.startRecord(getNewCacheFile())
                    return
                }

                outputStream.write(audioFile.readBytes())
                outputSpeechLength += speechParams.speechLengthInMs
                val progress = outputSpeechLength / MIN_SPEECH_LENGTH_FOR_ENROLLMENT_IN_MS
                eventListener?.onProgress(progress)

                if (progress < 1f) {
                    speechRecorder.startRecord(getNewCacheFile())
                    return
                }

                eventListener?.onLivenessCheckStarted()
                val outputSpeech = outputStream.toByteArray()
                val livenessResult =
                    runBlocking { defLivenessEngine.await() }.checkLiveness(outputSpeech, sampleRate)
                val livenessCheckStatus = if (livenessResult.value.probability >= LIVENESS_THRESHOLD) {
                    LivenessCheckStatus.LiveDetected
                } else {
                    LivenessCheckStatus.SpoofDetected
                }

                eventListener?.onComplete(
                    getNewCacheFile().apply { writeBytes(outputSpeech) },
                    outputSpeechLength,
                    SpeechQualityStatus.Ok,
                    livenessCheckStatus,
                )

                stop()
            }

            override fun onProgress(progress: Float) {
                eventListener?.onSpeechPartRecorded()
            }
        }

        speechRecorder.startRecord(getNewCacheFile())
    }

    @Synchronized
    fun pause() {
        speechRecorder.pauseRecord()
    }

    @Synchronized
    fun resume() {
        speechRecorder.resumeRecord()
    }

    @Synchronized
    fun stop() {
        speechRecorder.stopRecord()
        outputStream.reset()
        outputSpeechLength = 0f
    }

    private fun getNewCacheFile(): File {
        return File(cacheDir, "${UUID.randomUUID()}.bin")
    }

    /**
     * Close opened resources. After calling this function, you should no longer use the object.
     */
    override fun close() {
        stop()
        runBlocking { defLivenessEngine.await() }.close()
        speechRecorder.close()
    }

    companion object {
        private const val MIN_SPEECH_LENGTH_FOR_ENROLLMENT_IN_MS = 10_000f
        private const val MIN_SPEECH_LENGTH_FOR_CHUNKS_IN_MS = 3000f
        private const val MIN_SNR_IN_DB = 10f
        private const val LIVENESS_THRESHOLD = 0.5f
    }
}
