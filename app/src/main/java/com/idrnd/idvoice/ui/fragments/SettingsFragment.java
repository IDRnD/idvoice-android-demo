package com.idrnd.idvoice.ui.fragments;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.utils.Prefs;
import com.idrnd.idvoice.utils.logs.FileUtils;
import com.idrnd.idvoice.utils.verification.EngineManager;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static String TAG = SettingsFragment.class.getSimpleName();
    private View progressLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressLayout = view.findViewById(R.id.progerssLayout);

        view.findViewById(R.id.backBtn).setOnClickListener(v ->
            getFragmentManager().popBackStack()
        );

        TextView textVerifyThreshold = view.findViewById(R.id.textVerifyThreshold);
        textVerifyThreshold.setText(
            String.format(
                Locale.US,
                "%d %%",
                (int) (Prefs.getInstance().getVerificationThreshold() * 100)
            )
        );

        Switch switchLiveness = view.findViewById(R.id.switchLiveness);
        switchLiveness.setChecked(Prefs.getInstance().getLivenessCheckEnabled());
        switchLiveness.setOnCheckedChangeListener(
            (buttonView, isChecked) -> {
                Log.d(TAG, "Liveness check enabled: " + isChecked);
                Prefs.getInstance().setLivenessCheckEnabled(isChecked);

                // If no anti-spoofing checks are planned, clean up the resources associated with it
                // to decrease RAM consumption
                if (!isChecked) EngineManager.getInstance().releaseAntispoofEngine();
            }
        );

        SeekBar seekBarVerificationThreshold = view.findViewById(R.id.seekBarVerificationThreshold);
        seekBarVerificationThreshold.setProgress((int) (Prefs.getInstance().getVerificationThreshold() * 100));
        seekBarVerificationThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "Verification threshold was changed: " + progress + " %%");
                Prefs.getInstance().setVerificationThreshold(progress / 100f);
                textVerifyThreshold.setText(String.format(Locale.US, "%d %%", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        view.findViewById(R.id.buttonSendLogs).setOnClickListener((v) -> {
            showLoading();
            FileUtils.getInstance().zipLogs((zipLogs) -> {
                hideLoading();
                try {
                    Uri zipUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() +  ".fileprovider",
                        zipLogs
                    );

                    FileUtils.getInstance().sendFileByEmail(requireActivity(),  zipUri, "Logs from IDVoice");
                } catch (PackageManager.NameNotFoundException e) {
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Send-message application was not found", e);
                }
            });
        });
    }

    public void showLoading() { progressLayout.setVisibility(View.VISIBLE); }

    public void hideLoading() { progressLayout.setVisibility(View.GONE); }
}

















