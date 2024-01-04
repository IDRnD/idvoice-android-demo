package com.idrnd.idvoice.utils.speech.params

enum class SpeechQualityStatus {
    Ok,
    TooNoisy,
    TooSmallSpeechTotalLength,
    TooSmallSpeechRelativeLength,
    MultipleSpeakersDetected,
}
