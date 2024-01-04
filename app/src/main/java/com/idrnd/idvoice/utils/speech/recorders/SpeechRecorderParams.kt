package com.idrnd.idvoice.utils.speech.recorders

import com.idrnd.idvoice.utils.speech.CheckLivenessType
import com.idrnd.idvoice.utils.speech.DecisionToStopRecording
import net.idrnd.voicesdk.media.QualityCheckMetricsThresholds

data class SpeechRecorderParams(
    val qualityCheckMetricsThresholds: QualityCheckMetricsThresholds,
    val checkLivenessType: CheckLivenessType,
    val decisionToStopRecording: DecisionToStopRecording,
)
