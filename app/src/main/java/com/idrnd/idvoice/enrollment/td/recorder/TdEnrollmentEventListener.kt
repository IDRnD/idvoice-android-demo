package com.idrnd.idvoice.enrollment.td.recorder

import androidx.annotation.IntRange
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import java.io.File

/**
 * Listener of events from TD enrollment recorder.
 */
interface TdEnrollmentEventListener {

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
     * @param audioFiles Files with recordings for voice template making.
     */
    fun onComplete(audioFiles: List<File>)

    /**
     * Called when a recorder has started a new record.
     * @param index Index of record.
     */
    fun onStartRecordIndex(@IntRange(from = 0, to = Int.MAX_VALUE.toLong()) index: Int)

    /**
     * Called when it detects speech.
     */
    fun onSpeechPartRecorded()
}
