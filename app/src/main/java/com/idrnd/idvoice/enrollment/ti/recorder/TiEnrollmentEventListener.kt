package com.idrnd.idvoice.enrollment.ti.recorder

import androidx.annotation.FloatRange
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus

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
     * Called when a recorder checks speech liveness.
     * @param status Status of liveness check.
     */
    fun onLivenessCheckStatus(status: LivenessCheckStatus)

    /**
     * Called when a recorder has started speech liveness.
     */
    fun onLivenessCheckStarted()

    /**
     * Called when a recorder has completed recording.
     * @param speechBytes the total speech bytes.
     */
    fun onComplete(speechBytes: ByteArray)

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
