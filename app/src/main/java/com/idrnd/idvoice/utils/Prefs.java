package com.idrnd.idvoice.utils;

import android.annotation.SuppressLint;
import android.content.Context;
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

    public static final String VERIFICATION_THRESHOLD = "VERIFICATION_THRESHOLD";
    public static final String LIVENESS_CHECK_ENABLED = "LIVENESS_CHECK_ENABLED";
    public static final String QUALITY_CHECK_ENABLED = "QUALITY_CHECK_ENABLED";
    private static final String VOICE_TEMPLATE_TD = "VOICE_TEMPLATE_TD";
    private static final String VOICE_TEMPLATE_TI = "VOICE_TEMPLATE_TI";

    private static Map<VoiceTemplateType, String> mapEnumPrefsKey = new HashMap<VoiceTemplateType, String>() {{
        put(VoiceTemplateType.TextIndependent, VOICE_TEMPLATE_TI);
        put(VoiceTemplateType.TextDependent, VOICE_TEMPLATE_TD);
    }
    };

    private SharedPreferences sharedPreferences;
    private String sharedPreferenceName;
    private int sharedPreferenceMode;

    private static Prefs instance;

    private Prefs() { }

    public static Prefs getInstance() {
        if (instance == null) {
            instance = new Prefs();
        }
        return instance;
    }

    public void init(Context context, String sharedPreferenceName, int sharedPreferenceMode) {
        this.sharedPreferences = context.getSharedPreferences(sharedPreferenceName, sharedPreferenceMode);;
        this.sharedPreferenceName = sharedPreferenceName;
        this.sharedPreferenceMode = sharedPreferenceMode;
    }

    public Prefs(Context context, String sharedPreferenceName, int sharedPreferenceMode) {
        init(context, sharedPreferenceName, sharedPreferenceMode);
    }

    public String getSharedPreferenceName() {
        return sharedPreferenceName;
    }

    public int getSharedPreferenceMode() {
        return sharedPreferenceMode;
    }

    /**
     * Gets voice template from SharedPreferences by voice template type
     * @param voiceTemplateType voice template type
     * @return serialized voice template
     */
    public byte[] getVoiceTemplate(VoiceTemplateType voiceTemplateType) {
        String encodedBytes = sharedPreferences.getString(mapEnumPrefsKey.get(voiceTemplateType), null);

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
        sharedPreferences
            .edit()
            .putString(
                mapEnumPrefsKey.get(voiceTemplateType),
                Base64.encodeToString(voiceTemplate, Base64.NO_WRAP)
            ).commit();
    }

    public float getVerificationThreshold() {
        return sharedPreferences.getFloat(VERIFICATION_THRESHOLD, 0.5f);
    }

    public void setVerificationThreshold(float verificationThreshold) {
        sharedPreferences
            .edit()
            .putFloat(VERIFICATION_THRESHOLD, verificationThreshold)
            .apply();
    }

    public boolean getLivenessCheckEnabled() {
        return sharedPreferences.getBoolean(LIVENESS_CHECK_ENABLED, true);
    }

    public void setLivenessCheckEnabled(boolean livenessCheckEnabled) {
        sharedPreferences
            .edit()
            .putBoolean(LIVENESS_CHECK_ENABLED, livenessCheckEnabled)
            .apply();
    }

    public boolean getQualityCheckEnabled() {
        return sharedPreferences.getBoolean(QUALITY_CHECK_ENABLED, true);
    }

    public void setQualityCheckEnabled(boolean qualityCheckEnabled) {
        sharedPreferences
                .edit()
                .putBoolean(QUALITY_CHECK_ENABLED, qualityCheckEnabled)
                .apply();
    }
}
