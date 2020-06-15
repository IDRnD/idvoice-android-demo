package com.idrnd.idvoice.ui.dialogs;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.utils.Prefs;

import java.util.Objects;

import static com.idrnd.idvoice.ui.fragments.StartFragment.LIVENESS_SCORE;
import static com.idrnd.idvoice.ui.fragments.StartFragment.VERIFICATION_SCORE;

/**
 * Statistic dialog for showing statistic for verify and liveness check
 */
public class StatisticsDialog extends DialogFragment {

    private TextView verificationTitle;
    private TextView livenessTitle;
    private TextView verificationResult;
    private TextView livenessResult;

    // Liveness check threshold does not have that much variety as verification one,
    // so we just hard-code it
    private static final int LIVENESS_THRESHOLD = 50;

    public static StatisticsDialog newInstance(Bundle bundle) {
        StatisticsDialog statisticsDialog = new StatisticsDialog();
        statisticsDialog.setArguments(bundle);
        return statisticsDialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.statistics_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        verificationTitle = view.findViewById(R.id.verifyResultTitle);
        livenessTitle = view.findViewById(R.id.livenessResultTitle);

        verificationResult = view.findViewById(R.id.verifyResultView);
        livenessResult = view.findViewById(R.id.livenessResultView);

        // Get verification and liveness probabilities
        Bundle arguments = getArguments();

        float verificationScore = arguments.getFloat(VERIFICATION_SCORE) * 100;

        // Get verify threshold from shared preferences
        int verificationThreshold = (int) (Prefs.getInstance().getVerificationThreshold() * 100);
        updateVerifyStatisticViews(verificationScore, verificationThreshold);

        if (arguments.containsKey(LIVENESS_SCORE)) {
            float antispoofingScore = arguments.getFloat(LIVENESS_SCORE) * 100;
            updateLivenessStatisticViews(antispoofingScore, LIVENESS_THRESHOLD);
        } else {
            view.findViewById(R.id.livenessResultContainer).setVisibility(View.GONE);
            livenessTitle.setText("");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Objects.requireNonNull(getDialog().getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @SuppressLint("DefaultLocale")
    private void updateVerifyStatisticViews(float verificationProbability, int verificationThreshold) {
        verificationResult.setText(String.format("%.1f %%", verificationProbability));

        int color = ContextCompat.getColor(
                requireContext(),
                verificationProbability > verificationThreshold ? R.color.green : R.color.red
        );

        verificationResult.setTextColor(color);
        verificationTitle.setTextColor(color);
    }

    @SuppressLint("DefaultLocale")
    private void updateLivenessStatisticViews(float livenessProbability, int livenessThreshold) {
        livenessResult.setText(String.format("%.1f %%", livenessProbability));

        int color = ContextCompat.getColor(
                requireContext(),
                livenessProbability > livenessThreshold ? R.color.green : R.color.red
        );

        livenessResult.setTextColor(color);
        livenessTitle.setTextColor(color);
    }
}