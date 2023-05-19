package com.idrnd.idvoice.utils.speech.params

import net.idrnd.voicesdk.liveness.LivenessResult

data class SpeechParams(
    val speechLengthInMs: Float,
    val snrInDb: Float,
    val speechQualityStatus: SpeechQualityStatus,
    val livenessCheckStatus: LivenessCheckStatus,
    val livenessResult: LivenessResult? = null,
)
