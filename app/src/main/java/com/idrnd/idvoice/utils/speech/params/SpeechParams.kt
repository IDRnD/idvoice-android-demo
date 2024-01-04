package com.idrnd.idvoice.utils.speech.params

import net.idrnd.voicesdk.liveness.LivenessResult
import net.idrnd.voicesdk.media.QualityCheckEngineResult

data class SpeechParams(
    val speechQualityStatus: SpeechQualityStatus,
    val qualityCheckEngineResult: QualityCheckEngineResult,
    val livenessCheckStatus: LivenessCheckStatus,
    val livenessResult: LivenessResult? = null,
)
