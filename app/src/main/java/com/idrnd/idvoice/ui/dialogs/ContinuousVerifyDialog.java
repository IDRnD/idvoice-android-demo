package com.idrnd.idvoice.ui.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.recorders.ContinuousVerifyRecorder;
import com.idrnd.idvoice.ui.dialogs.interfaces.NextVerifyResultListener;
import com.idrnd.idvoice.utils.IntermediateColorCalculator;

import java.util.Locale;

import static com.idrnd.idvoice.IDVoiceApplication.RECORDING_SAMPLE_RATE;

/**
 * Dialog for continuous voice verification using ContinuousVerifyRecorder
 */
public class ContinuousVerifyDialog extends AlertDialog implements NextVerifyResultListener {

    private ContinuousVerifyRecorder recorder;
    private TextView textViewVerifyPercent;
    private IntermediateColorCalculator intermediateColorCalculator;

    public ContinuousVerifyDialog(Context context) {
        super(context, R.style.CustomThemeOverlayAlertDialog);

        View rootView = ((LayoutInflater)
            (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)))
            .inflate(R.layout.dialog_continous_verify, null, false);

        ((TextView) rootView.findViewById(R.id.messageForUser)).setText(context.getString(R.string.message_for_continuous_verify));
        rootView.findViewById(R.id.stopBtn).setOnClickListener(view -> dismiss());
        textViewVerifyPercent = rootView.findViewById(R.id.textViewVerifyPercent);

        recorder = new ContinuousVerifyRecorder(RECORDING_SAMPLE_RATE, this);

        intermediateColorCalculator = new IntermediateColorCalculator(
            0.5f,
            ContextCompat.getColor(context, R.color.red),
            ContextCompat.getColor(context, R.color.green)
        );

        setView(rootView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        recorder.startRecording();
    }

    @Override
    protected void onStop() {
        super.onStop();
        recorder.stopRecording();
    }

    @Override
    public void onNextVerifyResult(float probability) {
        // Show new verification probability
        textViewVerifyPercent.setText(String.format(Locale.US, "%.0f %%", probability * 100));
        textViewVerifyPercent.setTextColor(intermediateColorCalculator.calculateIntermediateColor(probability));
    }
}