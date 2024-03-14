package com.idrnd.idvoice.utils.speech.recorders

import com.idrnd.idvoice.preferences.BiometricsType
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.utils.speech.params.SpeechQualityStatus
import net.idrnd.voicesdk.media.QualityCheckEngine
import net.idrnd.voicesdk.media.QualityCheckEngineResult
import net.idrnd.voicesdk.media.QualityCheckMetricsThresholds
import net.idrnd.voicesdk.media.QualityCheckScenario
import net.idrnd.voicesdk.media.QualityCheckShortDescription

/**
 * Handy methods to get SDK recommended thresholds.
 */
object SpeechQualityHelper {

    fun getSpeechQualityStatus(qualityCheckEngineResult: QualityCheckEngineResult) =
        when (qualityCheckEngineResult.qualityCheckShortDescription) {
            QualityCheckShortDescription.TOO_NOISY -> SpeechQualityStatus.TooNoisy
            QualityCheckShortDescription.TOO_SMALL_SPEECH_TOTAL_LENGTH -> SpeechQualityStatus.TooSmallSpeechTotalLength
            QualityCheckShortDescription.TOO_SMALL_SPEECH_RELATIVE_LENGTH ->
                SpeechQualityStatus.TooSmallSpeechRelativeLength
            QualityCheckShortDescription.MULTIPLE_SPEAKERS_DETECTED -> SpeechQualityStatus.MultipleSpeakersDetected
            QualityCheckShortDescription.OK -> SpeechQualityStatus.Ok
            null -> throw IllegalStateException("QualityCheckShortDescription is null!")
        }

    fun getVerificationThresholds(qualityCheckEngine: QualityCheckEngine): QualityCheckMetricsThresholds =
        when (GlobalPrefs.biometricsType) {
            BiometricsType.TextDependent ->
                qualityCheckEngine.getRecommendedThresholds(
                    QualityCheckScenario.VERIFY_TD_VERIFICATION
                )

            BiometricsType.TextIndependent ->
                qualityCheckEngine.getRecommendedThresholds(
                    QualityCheckScenario.VERIFY_TI_VERIFICATION
                )
        }

    fun getTDEnrollmentThresholds(voiceVerifyEngine: QualityCheckEngine): QualityCheckMetricsThresholds =
        voiceVerifyEngine.getRecommendedThresholds(
            QualityCheckScenario.VERIFY_TD_ENROLLMENT
        )

    fun getTIEnrollmentThresholds(voiceVerifyEngine: QualityCheckEngine): QualityCheckMetricsThresholds =
        voiceVerifyEngine.getRecommendedThresholds(
            QualityCheckScenario.VERIFY_TI_ENROLLMENT
        )
}
