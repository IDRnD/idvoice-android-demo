package com.idrnd.idvoice.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import com.idrnd.idvoice.utils.Prefs;

import static com.idrnd.idvoice.utils.Prefs.LIVENESS_CHECK_ENABLED;
import static com.idrnd.idvoice.utils.Prefs.QUALITY_CHECK_ENABLED;
import static com.idrnd.idvoice.utils.Prefs.VERIFICATION_THRESHOLD;

public class PreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Set information about global SharedPreference for binding it with PreferenceFragmentCompat
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(Prefs.getInstance().getSharedPreferenceName());
        preferenceManager.setSharedPreferencesMode(Prefs.getInstance().getSharedPreferenceMode());

        // Screen where Preferences will be appeared
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(requireContext());

        // List of Preferences
        SeekBarPreference verificationThreshold = new SeekBarPreference(preferenceScreen.getContext());
        verificationThreshold.setTitle("Verification threshold %");
        verificationThreshold.setMax(100);
        verificationThreshold.setShowSeekBarValue(true);
        verificationThreshold.setSelectable(false);
        verificationThreshold.setKey(null);
        verificationThreshold.setValue((int)(Prefs.getInstance().getVerificationThreshold() * 100));
        verificationThreshold.setOnPreferenceChangeListener((preference, newValue) -> {
                    Prefs.getInstance().setVerificationThreshold((int) newValue / 100f);
                    return true;
                }
        );
        preferenceScreen.addPreference(verificationThreshold);

        CheckBoxPreference livenessCheckEnabled = new CheckBoxPreference(preferenceScreen.getContext());
        livenessCheckEnabled.setTitle("Liveness check");
        livenessCheckEnabled.setKey(LIVENESS_CHECK_ENABLED);
        livenessCheckEnabled.setChecked(Prefs.getInstance().getLivenessCheckEnabled());
        preferenceScreen.addPreference(livenessCheckEnabled);

        CheckBoxPreference qualityCheckEnabled = new CheckBoxPreference(preferenceScreen.getContext());
        qualityCheckEnabled.setTitle("Quality check");
        qualityCheckEnabled.setKey(QUALITY_CHECK_ENABLED);
        qualityCheckEnabled.setChecked(Prefs.getInstance().getQualityCheckEnabled());
        preferenceScreen.addPreference(qualityCheckEnabled);

        setPreferenceScreen(preferenceScreen);
    }
}