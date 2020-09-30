package com.idrnd.idvoice.ui.dialogs.speechrecord;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.recorders.SpeechRecorder;

import static com.idrnd.idvoice.IDVoiceApplication.RECORDING_SAMPLE_RATE;

/**
 * Speech recording dialog using SpeechRecorder
 */
public class SpeechRecordDialog extends AbstractSpeechRecordDialog {

    private Handler handler = new Handler(Looper.getMainLooper());

    /**
     * @param messageForUser message for user that contains useful information about what they need to do
     */
    public SpeechRecordDialog(Context context, String messageForUser) {
        super(context, messageForUser);
        recorder = new SpeechRecorder(RECORDING_SAMPLE_RATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final TextView speechLengthView = findViewById(R.id.speechLength);

        ((SpeechRecorder) recorder).setOnSpeechLengthUpdateListener(speechLengthInMs ->
            handler.post(() -> speechLengthView.setText(speechLengthInMs / 1000f + "s"))
        );
    }
}