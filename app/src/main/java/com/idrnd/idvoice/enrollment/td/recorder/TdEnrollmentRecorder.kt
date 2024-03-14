package com.idrnd.idvoice.enrollment.td.recorder

import android.content.Context
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.recorders.LivenessStatusHelper
import com.idrnd.idvoice.utils.speech.recorders.SpeechQualityHelper
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorder
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorderListener
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import net.idrnd.voicesdk.liveness.LivenessEngine
import net.idrnd.voicesdk.media.QualityCheckEngine
import net.idrnd.voicesdk.media.QualityCheckShortDescription
import net.idrnd.voicesdk.media.SpeechSummary

/**
 * Collects quality and liveness speech chunks up to 3, at that point completes the recording.
 * Each chunk has to pass quality and liveness checks, else the chunk is not taken into account.
 */
class TdEnrollmentRecorder(
    context: Context,
    val sampleRate: Int,
    val livenessEngine: LivenessEngine,
    var eventListener: TdEnrollmentEventListener? = null,
    val qualityCheckEngine: QualityCheckEngine
) : AutoCloseable {

    /**
     * We use recommended IDVoice SDK thresholds.
     */
    val qualityThresholds = SpeechQualityHelper.getTDEnrollmentThresholds(qualityCheckEngine)

    /**
     * Scope for recoding process.
     */
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Job for recording process.
     */
    private var recordJob: Job? = null

    /**
     * The speech recorder.
     */
    private val speechRecorder = SpeechRecorder(context = context, sampleRate = sampleRate)

    /**
     * Container for good speech chunks.
     */
    private var speechBytesList = mutableListOf<ByteArray>()

    /**
     * Start the enrollment recording.
     * The prepared speech bytes for voice template will be returned through the event listener.
     */
    @Synchronized
    fun start() {
        speechRecorder.speechRecorderListener = object : SpeechRecorderListener {
            override fun onSpeechChunk(speechBytes: ByteArray, speechSummary: SpeechSummary) {
                recordJob = scope.launch {
                    eventListener?.onLivenessCheckStarted()

                    // Gets speech quality status.
                    val qualityResults = qualityCheckEngine.checkQuality(speechBytes, sampleRate, qualityThresholds)
                    val speechQualityStatus = SpeechQualityHelper.getSpeechQualityStatus(qualityResults)

                    // Notify quality of speech status if it is not Ok and resumes recording.
                    if (qualityResults.qualityCheckShortDescription != QualityCheckShortDescription.OK) {
                        eventListener?.onSpeechQualityStatus(speechQualityStatus)
                        eventListener?.onStartRecordIndex(speechBytesList.size)
                        speechRecorder.resumeRecord()
                        return@launch
                    }

                    // Checks speech liveness.
                    val livenessCheckStatus =
                        LivenessStatusHelper.checkLiveness(speechBytes, sampleRate, livenessEngine)

                    // Notify speech liveness if it's spoof and resumes recording.
                    if (livenessCheckStatus != LivenessCheckStatus.LiveDetected) {
                        eventListener?.onLivenessCheckStatus(livenessCheckStatus)
                        eventListener?.onStartRecordIndex(speechBytesList.size)
                        speechRecorder.resumeRecord()
                        return@launch
                    }

                    // At this point both, quality and liveness checks have passed.

                    // Notify OK quality status.
                    eventListener?.onSpeechQualityStatus(speechQualityStatus)

                    // Add speech bytes chunk to passed speech bytes chunk list.
                    speechBytesList.add(speechBytes)

                    // Update recording index with the number of passed speech bytes chunks.
                    eventListener?.onStartRecordIndex(speechBytesList.size)

                    // If speech bytes chunk list size is not yet equal to the number of phrases needed for enrollment
                    // then resume recording (to continue getting speech chunks).
                    if (speechBytesList.size != NUMBER_PHRASES_FOR_ENROLLMENT) {
                        speechRecorder.resumeRecord()
                        return@launch
                    }

                    // At this point we have enough speech length so we can complete the process.
                    eventListener?.onComplete(speechBytesList)
                    stop()
                }
            }

            override fun onSpeechSummaryUpdate(speechSummary: SpeechSummary) {
                eventListener?.onSpeechPartRecorded()
            }
        }
        eventListener?.onStartRecordIndex(speechBytesList.size)
        speechRecorder.startRecord()
    }

    /**
     * Stop record.
     *
     * @exception IOException  if an I/O error occurs.
     */
    @Synchronized
    fun stop() {
        scope.launch {
            speechRecorder.stopRecord()
            recordJob?.cancelAndJoin()
        }
    }

    /**
     * Close opened resources. After calling this function, you should no longer use the object.
     */
    override fun close() {
        stop()
        eventListener = null
    }

    companion object {
        private const val NUMBER_PHRASES_FOR_ENROLLMENT = 3
    }
}
