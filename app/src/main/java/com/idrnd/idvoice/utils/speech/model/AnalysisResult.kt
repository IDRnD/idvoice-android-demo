package com.idrnd.idvoice.utils.speech.model

import net.idrnd.voicesdk.media.SpeechSummary

/**
 * Class that keeps speech analysis result.
 */
data class AnalysisResult(val bytes: ByteArray, val speechSummary: SpeechSummary, val isSpeechEnded: Boolean)
