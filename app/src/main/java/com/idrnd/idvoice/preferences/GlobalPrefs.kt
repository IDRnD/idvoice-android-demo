package com.idrnd.idvoice.preferences

import android.content.SharedPreferences
import com.idrnd.idvoice.preferences.BiometricsType.TextDependent
import com.idrnd.idvoice.preferences.BiometricsType.TextIndependent

/**
 * Global class with app shared preferences.
 */
object GlobalPrefs {

    private lateinit var preferencesManager: SharedPreferences

    /**
     * Initialization of object.
     */
    fun init(preferencesManager: SharedPreferences) {
        GlobalPrefs.preferencesManager = preferencesManager
    }

    /**
     * Normalized verification threshold.
     */
    var verifyThreshold: Float
        get() = preferencesManager.getFloat(VERIFY_THRESHOLD, 0.5f)
        set(verifyThreshold) {
            preferencesManager.edit().putFloat(VERIFY_THRESHOLD, verifyThreshold).apply()
        }

    /**
     * Normalized liveness threshold.
     */
    var livenessThreshold: Float
        get() = preferencesManager.getFloat(LIVENESS_THRESHOLD, 0.5f)
        set(livenessThreshold) {
            preferencesManager.edit().putFloat(LIVENESS_THRESHOLD, livenessThreshold).apply()
        }

    /**
     * Type of biometrics analysis.
     */
    var biometricsType: BiometricsType
        get() = BiometricsType.values()[preferencesManager.getInt(BIOMETRICS_TYPE, TextDependent.ordinal)]
        set(biometricsType) {
            preferencesManager.edit().putInt(BIOMETRICS_TYPE, biometricsType.ordinal).commit()
        }

    /**
     * Template filepath by a biometrics type.
     */
    var templateFilepath: String?
        get() {
            return when (biometricsType) {
                TextDependent -> preferencesManager.getString(TD_TEMPLATE_FILE, null)
                TextIndependent -> preferencesManager.getString(TI_TEMPLATE_FILE, null)
            }
        }
        set(value) {
            when (biometricsType) {
                TextDependent -> preferencesManager.edit().putString(TD_TEMPLATE_FILE, value).apply()
                TextIndependent -> preferencesManager.edit().putString(TI_TEMPLATE_FILE, value).apply()
            }
        }

    /**
     * Default template filename by a biometrics type.
     */
    val templateFilename: String
        get() {
            return when (biometricsType) {
                TextDependent -> "td_template"
                TextIndependent -> "ti_template"
            }
        }

    /**
     * Password phrase.
     */
    val passwordPhrase = "My voice is my password"

    /**
     * Sample rate for audio recording.
     */
    val sampleRate = 48_000

    /**
     * Min speech length for TI enrollment in ms.
     */
    val minSpeechLengthForTiEnrollmentInMs = 10_000

    // Preferences keys
    private const val VERIFY_THRESHOLD = "VERIFY_THRESHOLD"
    private const val LIVENESS_THRESHOLD = "LIVENESS_THRESHOLD"
    private const val TD_TEMPLATE_FILE = "TD_TEMPLATE_FILE"
    private const val TI_TEMPLATE_FILE = "TI_TEMPLATE_FILE"
    private const val BIOMETRICS_TYPE = "BIOMETRICS_TYPE"
}
