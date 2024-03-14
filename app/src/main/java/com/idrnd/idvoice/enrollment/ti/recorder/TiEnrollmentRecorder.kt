package com.idrnd.idvoice.enrollment.ti.recorder

import android.content.Context
import com.idrnd.idvoice.utils.speech.params.LivenessCheckStatus
import com.idrnd.idvoice.utils.speech.recorders.LivenessStatusHelper
import com.idrnd.idvoice.utils.speech.recorders.SpeechQualityHelper
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorder
import com.idrnd.idvoice.utils.speech.recorders.SpeechRecorderListener
import java.io.ByteArrayOutputStream
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
 * Collects quality and liveness speech chunks until needed speech length is reached, at that point
 * completes the recording.
 * Each chunk has to pass quality and liveness checks, else the chunk is not taken into account.
 */
class TiEnrollmentRecorder(
    context: Context,
    private val sampleRate: Int,
    private val livenessEngine: LivenessEngine,
    private var eventListener: TiEnrollmentEventListener? = null,
    private val qualityCheckEngine: QualityCheckEngine
) : AutoCloseable {

    /**
     * We use recommended IDVoice SDK thresholds.
     */
    val qualityThresholds = SpeechQualityHelper.getTIEnrollmentThresholds(qualityCheckEngine).apply {
        // Manually set minimumSpeechLengthMs as we want chunks of 3 seconds or more for this case.
        minimumSpeechLengthMs = MINIMUM_SPEECH_LENGTH_FOR_CHUNKS_IN_MS
        // This way we disable relative speech length check for our use case.
        minimumSpeechRelativeLength = MINIMUM_RELATIVE_SPEECH_LENGTH_FOR_CHUNKS_IN_MS
    }

    /**
     * Scope for our coroutines.
     */
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * A list of speech chunks being processed.
     */
    private var onSpeechChunkJobs = mutableListOf<Job>()

    /**
     * The speech recorder.
     */
    private val speechRecorder = SpeechRecorder(context = context, sampleRate = sampleRate).apply {
        // We want chunks of 3 seconds.
        minimumSpeechLengthToDetectSpeechEnd = MINIMUM_SPEECH_LENGTH_FOR_CHUNKS_IN_MS
    }

    /**
     * Container for total speech amount in bytes.
     */
    private var outputStream = ByteArrayOutputStream()

    /**
     * total speech amount length in milliseconds.
     */
    private var outputSpeechLengthInMs = 0f

    /**
     * Tells the progress.
     */
    val progress
        get() = outputSpeechLengthInMs / MIN_SPEECH_LENGTH_FOR_ENROLLMENT_IN_MS

    /**
     * Tells if the process is completed
     */
    val isCompleted
        get() = progress >= 1.0

    /**
     * Start the enrollment recording.
     * The prepared audio for voice template creating will be returned to the event listener.
     */
    @Synchronized
    fun start() {
        speechRecorder.speechRecorderListener = object : SpeechRecorderListener {
            override fun onSpeechChunk(speechBytes: ByteArray, speechSummary: SpeechSummary) {
                val speechChunkJob = scope.launch {
                    // Cancel speech chunk jobs if process is already completed, no further actions.
                    if (isCompleted) {
                        onSpeechChunkJobs.forEach { it.cancelAndJoin() }
                        return@launch
                    }
                    // Resume recording if recording was paused due to speech chunk obtained,
                    // otherwise does nothing.
                    speechRecorder.resumeRecord()

                    // Checks speech quality.
                    val qualityCheckEngineResult = qualityCheckEngine.checkQuality(
                        speechBytes,
                        sampleRate,
                        qualityThresholds
                    )

                    // Gets speech quality status.
                    val speechQualityStatus = SpeechQualityHelper.getSpeechQualityStatus(qualityCheckEngineResult)

                    // Notify quality of speech status if it is not Ok. No further actions.
                    if (qualityCheckEngineResult.qualityCheckShortDescription != QualityCheckShortDescription.OK) {
                        eventListener?.onSpeechQualityStatus(speechQualityStatus)
                        return@launch
                    }

                    // Checks speech liveness.
                    val livenessCheckStatusResult = LivenessStatusHelper.checkLiveness(
                        speechBytes,
                        sampleRate,
                        livenessEngine
                    )

                    // Notify speech liveness if it's spoof. No further actions.
                    if (livenessCheckStatusResult != LivenessCheckStatus.LiveDetected) {
                        eventListener?.onLivenessCheckStatus(livenessCheckStatusResult)
                        return@launch
                    }

                    // At this point both, quality and liveness checks have passed.

                    // Notify OK quality status.
                    eventListener?.onSpeechQualityStatus(speechQualityStatus)

                    // Accumulate speech bytes.
                    outputStream.write(speechBytes)

                    // Increase total speech length in Ms with chunk's speech length.
                    outputSpeechLengthInMs += speechSummary.speechInfo.speechLengthMs

                    // Notify new progress.
                    eventListener?.onProgress(progress)

                    // We still need more speech so no more actions.
                    if (!isCompleted) {
                        return@launch
                    }

                    // At this point we have enough speech length so we can complete the process.
                    // So we pause recording.
                    speechRecorder.pauseRecord()
                    // and notify completion with total speech bytes.
                    eventListener?.onComplete(speechBytes)
                }
                onSpeechChunkJobs.add(speechChunkJob)
            }

            override fun onSpeechSummaryUpdate(speechSummary: SpeechSummary) {
                if (isCompleted) return
                eventListener?.onSpeechPartRecorded()
            }
        }
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
            onSpeechChunkJobs.forEach { it.cancelAndJoin() }
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
        private const val MIN_SPEECH_LENGTH_FOR_ENROLLMENT_IN_MS = 10_000f
        private const val MINIMUM_SPEECH_LENGTH_FOR_CHUNKS_IN_MS = 3000f

        // Set to 0 as we ignore this check for TI enrollment.
        private const val MINIMUM_RELATIVE_SPEECH_LENGTH_FOR_CHUNKS_IN_MS = 0f
    }
}
