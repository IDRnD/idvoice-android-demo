package com.idrnd.idvoice.utils.speech.recorders

import net.idrnd.voicesdk.media.SpeechSummary

/**
 * Listener of events from a speech recorder.
 */
interface SpeechRecorderListener {

    /**
     * Called when a speech chunk is recorded from beginning to end of speech.
     */
    fun onSpeechChunk(speechBytes: ByteArray, speechSummary: SpeechSummary)

    /**
     * Called when speech samples are added to current speech chunk recording.
     */
    fun onSpeechSummaryUpdate(speechSummary: SpeechSummary)
}
