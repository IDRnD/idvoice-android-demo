package com.idrnd.idvoice.utils.speech.recorders

import com.idrnd.idvoice.utils.speech.CheckLivenessType
import com.idrnd.idvoice.utils.speech.DecisionToStopRecording

data class SpeechRecorderParams(
    val minSpeechLengthInMs: Float,
    val minSnrInDb: Float,
    val checkLivenessType: CheckLivenessType,
    val decisionToStopRecording: DecisionToStopRecording,
)
