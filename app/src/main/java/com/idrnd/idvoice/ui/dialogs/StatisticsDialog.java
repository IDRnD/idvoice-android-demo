package com.idrnd.idvoice.ui.dialogs;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.utils.Prefs;

import java.util.Objects;

/**
 * Statistic dialog for showing statistic for verify and liveness check
 */
public class StatisticsDialog extends DialogFragment {

    private TextView verificationStatistics;
    private TextView antispoofingStatistics;

    public static String VERIFICATION_SCORE = "VERIFICATION_SCORE";
    public static String ANTISPOOFING_SCORE = "ANTISPOOFING_SCORE";

    public static StatisticsDialog newInstance(Bundle bundle) {
        StatisticsDialog statisticsDialog = new StatisticsDialog();
        statisticsDialog.setArguments(bundle);
        return statisticsDialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        verificationStatistics = view.findViewById(R.id.verificationStatistics);
        antispoofingStatistics = view.findViewById(R.id.antispoofingStatistics);

        // Get verification and liveness probabilities
        Bundle arguments = getArguments();

        float verificationScore = arguments.getFloat(VERIFICATION_SCORE) * 100;

        // Get verify threshold from shared preferences
        int verificationThreshold = (int) (Prefs.getInstance().getVerificationThreshold() * 100);
        updateVerifyStatisticViews(verificationScore, verificationThreshold);

        if (arguments.containsKey(ANTISPOOFING_SCORE)) {
            float antispoofingScore = arguments.getFloat(ANTISPOOFING_SCORE) * 100;

            // Liveness check threshold does not have that much variety as verification one,
            // so we just hard-code it
            final int antispoofingThreshold = 50;
            updateLivenessStatisticViews(antispoofingScore, antispoofingThreshold);
        } else {
            antispoofingStatistics.setText("");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Objects.requireNonNull(getDialog().getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void updateVerifyStatisticViews(float verificationProbability, int verificationThreshold) {
        verificationStatistics.setText(
            String.format(
                getString(
                    verificationProbability > verificationThreshold ?
                    R.string.verification_successfull :
                    R.string.verification_failed
                ), verificationProbability
            )
        );

        verificationStatistics.setTextColor(getContext().getResources().getColor(
            verificationProbability > verificationThreshold ? R.color.green : R.color.red));
    }

    private void updateLivenessStatisticViews(float livenessProbability, int livenessThreshold) {
        antispoofingStatistics.setText(
            String.format(
                getString(
                    livenessProbability > livenessThreshold ?
                    R.string.antispoofing_successfull :
                    R.string.antispoofing_failed
                ), livenessProbability
            )
        );

        antispoofingStatistics.setTextColor(getContext().getResources().getColor(
            livenessProbability > livenessThreshold ? R.color.green : R.color.red));
    }
}
