package com.idrnd.idvoice.enrollment.td.recorder

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
import net.idrnd.voicesdk.liveness.LivenessEngine
import java.io.File
import java.util.UUID

class TdEnrollmentRecorder(
    context: Context,
    sampleRate: Int,
    livenessEngine: LivenessEngine,
    var eventListener: TdEnrollmentEventListener? = null,
) : AutoCloseable {

    private var outputFiles = mutableListOf<File>()
    private val speechRecorder = SpeechRecorder(
        context,
        sampleRate,
        // We use values from IDVoice & IDLive Voice best practices guideline
        SpeechRecorderParams(
            MIN_SPEECH_LENGTH_FOR_TD_ENROLLMENT_IN_MS,
            MIN_SNR_FOR_TD_ENROLLMENT_IN_DB,
            CheckLivenessType.CheckLiveness,
            DecisionToStopRecording.WaitingForEndSpeech,
        ),
        GlobalPrefs.livenessThreshold,
    )
    private val cacheDir = context.cacheDir

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
                eventListener?.onLivenessCheckStarted()
            }

            override fun onComplete(audioFile: File, speechParams: SpeechParams) {
                eventListener?.onSpeechQualityStatus(speechParams.speechQualityStatus)

                if (speechParams.speechQualityStatus != SpeechQualityStatus.Ok) {
                    speechRecorder.startRecord(getNewCacheFile())
                    return
                }

                eventListener?.onLivenessCheckStatus(speechParams.livenessCheckStatus)

                if (speechParams.livenessCheckStatus != LivenessCheckStatus.LiveDetected) {
                    speechRecorder.startRecord(getNewCacheFile())
                    return
                }

                outputFiles.add(audioFile)

                if (outputFiles.size != NUMBER_PHRASES_FOR_ENROLLMENT) {
                    eventListener?.onStartRecordIndex(outputFiles.size)
                    speechRecorder.startRecord(getNewCacheFile())
                    return
                }

                eventListener?.onComplete(outputFiles)
                stop()
            }

            override fun onProgress(progress: Float) {
                eventListener?.onSpeechPartRecorded()
            }
        }

        eventListener?.onStartRecordIndex(outputFiles.size)
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
        outputFiles.clear()
    }

    private fun getNewCacheFile(): File {
        return File(cacheDir, "${UUID.randomUUID()}.bin")
    }

    /**
     * Close opened resources. After calling this function, you should no longer use the object.
     */
    override fun close() {
        stop()
        speechRecorder.close()
    }

    companion object {
        private const val MIN_SPEECH_LENGTH_FOR_TD_ENROLLMENT_IN_MS = 700f
        private const val MIN_SNR_FOR_TD_ENROLLMENT_IN_DB = 8f
        private const val NUMBER_PHRASES_FOR_ENROLLMENT = 3
    }
}
