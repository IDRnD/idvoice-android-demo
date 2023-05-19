package com.idrnd.idvoice.enrollment.ti.recorder

import androidx.annotation.FloatRange
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import java.io.File

/**
 * Listener of events from TI enrollment recorder.
 */
interface TiEnrollmentEventListener {
    /**
     * Called when a recorder detects a problem with speech quality.
     * @param status Status of speech quality.
     */
    fun onSpeechQualityStatus(status: SpeechQualityStatus)

    /**
     * Called when a recorder has started speech liveness.
     */
    fun onLivenessCheckStarted()

    /**
     * Called when a recorder has completed recording.
     * @param audioFile File with recording for voice template making.
     * @param speechLengthInMs Collected speech length in ms.
     * @param snrInDb SNR of speech in ms.
     * @param speechQualityStatus Quality of speech.
     * @param livenessCheckStatus Liveness state of speech.
     */
    fun onComplete(
        audioFile: File,
        speechLengthInMs: Float,
        speechQualityStatus: SpeechQualityStatus,
        livenessCheckStatus: LivenessCheckStatus,
    )

    /**
     * Called when a recorder has a new progress value.
     * @param progress Progress value from 0 to 1.
     */
    fun onProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float)

    /**
     * Called when it detects speech.
     */
    fun onSpeechPartRecorded()
}
