package com.idrnd.idvoice.utils;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

/**
 * SharedPreferences wrapper
 */
public class Prefs {

    public enum VoiceTemplateType {
        TextIndependent,
        TextDependent
    }

    private static final String VERIFICATION_THRESHOLD = "VERIFICATION_THRESHOLD";
    private static final String LIVENESS_CHECK_ENABLED = "LIVENESS_CHECK_ENABLED";
    private static final String VOICE_TEMPLATE_TD      = "VOICE_TEMPLATE_TD";
    private static final String VOICE_TEMPLATE_TI      = "VOICE_TEMPLATE_TI";

    private static Map<VoiceTemplateType, String> mapEnumPrefsKey = new HashMap<VoiceTemplateType, String>() {{
        put(VoiceTemplateType.TextIndependent, VOICE_TEMPLATE_TI);
        put(VoiceTemplateType.TextDependent, VOICE_TEMPLATE_TD);
    }};

    private SharedPreferences preferenceManager;

    private static Prefs instance;

    private Prefs() { }

    public static Prefs getInstance() {
        if (instance == null) {
            instance = new Prefs();
        }
        return instance;
    }

    public void init(SharedPreferences preferenceManager) { this.preferenceManager = preferenceManager; }

    public Prefs(SharedPreferences manager) { preferenceManager = manager; }

    /**
     * Gets voice template from SharedPreferences by voice template type
     * @param voiceTemplateType voice template type
     * @return serialized voice template
     */
    public byte[] getVoiceTemplate(VoiceTemplateType voiceTemplateType) {
        String encodedBytes = preferenceManager.getString(mapEnumPrefsKey.get(voiceTemplateType), null);

        if (encodedBytes != null) {
            return Base64.decode(encodedBytes, Base64.NO_WRAP);
        } else {
            return null;
        }
    }

    /**
     * Saves serialized voice template in SharedPreferences.
     * This is a synchronous method, it is necessary to know when the template is saved in SharedPreferences to disable the progress indicator.
     * @param voiceTemplate raw voice template
     * @param voiceTemplateType voice template type
     */
    @SuppressLint("ApplySharedPref")
    public void setVoiceTemplateSync(byte[] voiceTemplate, VoiceTemplateType voiceTemplateType) {
        preferenceManager
            .edit()
            .putString(
                mapEnumPrefsKey.get(voiceTemplateType),
                Base64.encodeToString(voiceTemplate, Base64.NO_WRAP)
            ).commit();
    }

    public float getVerificationThreshold() {
        return preferenceManager.getFloat(VERIFICATION_THRESHOLD, 0.5f);
    }

    public void setVerificationThreshold(float verificationThreshold) {
        preferenceManager
            .edit()
            .putFloat(VERIFICATION_THRESHOLD, verificationThreshold)
            .apply();
    }

    public boolean getLivenessCheckEnabled() {
        return preferenceManager.getBoolean(LIVENESS_CHECK_ENABLED, true);
    }

    public void setLivenessCheckEnabled(boolean checkLivenessEnable) {
        preferenceManager
            .edit()
            .putBoolean(LIVENESS_CHECK_ENABLED, checkLivenessEnable)
            .apply();
    }
}
