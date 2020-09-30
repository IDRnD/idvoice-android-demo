package com.idrnd.idvoice.ui.dialogs.speechrecord;

import android.content.Context;
import android.view.View;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.recorders.SpeechEndpointRecorder;

import static com.idrnd.idvoice.IDVoiceApplication.RECORDING_SAMPLE_RATE;

/**
 * Speech recording dialog using SpeechEndpointRecorder
 */
public class SpeechEndpointRecordDialog extends AbstractSpeechRecordDialog {

    /**
     * @param messageForUser message for user that contains useful information about what they need to do
     */
    public SpeechEndpointRecordDialog(Context context, String messageForUser) {
        super(context, messageForUser);

        recorder = new SpeechEndpointRecorder(RECORDING_SAMPLE_RATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Hide speech counter from layout, it's not needed when working with SpeechEndpointRecorder
        findViewById(R.id.speechLength).setVisibility(View.INVISIBLE);
        findViewById(R.id.speechLengthLabel).setVisibility(View.INVISIBLE);
    }
}