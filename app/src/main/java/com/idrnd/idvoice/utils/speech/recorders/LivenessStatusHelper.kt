package com.idrnd.idvoice.utils.speech.recorders

import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import net.idrnd.voicesdk.liveness.LivenessEngine

object LivenessStatusHelper {

    private const val LIVENESS_THRESHOLD = 0.5f

    fun checkLiveness(speechBytes: ByteArray, sampleRate: Int, livenessEngine: LivenessEngine): LivenessCheckStatus {
        val livenessResult = livenessEngine.checkLiveness(speechBytes, sampleRate)

        return if (livenessResult.value.probability >= LIVENESS_THRESHOLD) {
            LivenessCheckStatus.LiveDetected
        } else {
            LivenessCheckStatus.SpoofDetected
        }
    }
}
