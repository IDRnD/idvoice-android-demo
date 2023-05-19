package com.idrnd.idvoice.utils.speech.recorders

import androidx.annotation.FloatRange
import com.idrnd.idvoice.utils.speech.params.SpeechParams
import java.io.File

/**
 * Listener of events from a speech recorder.
 */
interface SpeechRecorderListener {

    /**
     * Called when a recorder has started speech liveness.
     */
    fun onLivenessCheckStarted()

    /**
     * Called when a recorder has completed recording.
     * @param audioFile File with recording for voice template making.

     */
    fun onComplete(audioFile: File, speechParams: SpeechParams)

    /**
     * Called when a recorder has a new progress value.
     * @param progress Progress value from 0 to 1.
     */
    fun onProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float)
}
