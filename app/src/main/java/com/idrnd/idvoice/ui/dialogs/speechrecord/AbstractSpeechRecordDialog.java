package com.idrnd.idvoice.ui.dialogs.speechrecord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.idrnd.idvoice.R;
import com.idrnd.idvoice.recorders.AudioRecorder;
import com.idrnd.idvoice.ui.dialogs.interfaces.OnStopRecordingListener;
import com.skyfishjy.library.RippleBackground;

/**
 * Class for reusing one layout in different speech dialogs.
 * Dialog starts audio recording when it's been showed and returns audio record when disappears
 */
public abstract class AbstractSpeechRecordDialog extends AlertDialog {

    protected AudioRecorder recorder;
    private RippleBackground rippleBackground;
    private OnStopRecordingListener stopRecordingListener;

    /**
     * @param messageForUser message for user that contains useful information about what they need to do
     */
    public AbstractSpeechRecordDialog(Context context, String messageForUser, OnStopRecordingListener stopRecordingListener) {
        super(context, R.style.CustomThemeOverlayAlertDialog);
        this.stopRecordingListener = stopRecordingListener;

        View rootView = ((LayoutInflater)
            (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)))
            .inflate(R.layout.dialog_record, null, false);

        ((TextView) rootView.findViewById(R.id.messageForUser)).setText(messageForUser);

        rippleBackground = rootView.findViewById(R.id.rippleBackground);

        rootView.findViewById(R.id.stopBtn).setOnClickListener(view ->
            recorder.stopRecording()
        );

        setView(rootView);
    }

    /**
     * @param messageForUser message for user that contains useful information about what they need to do
     */
    public AbstractSpeechRecordDialog(Context context, String messageForUser) {
        this(context, messageForUser, null);
    }

    public void setOnStopRecordingListener(OnStopRecordingListener stopRecordingListener) {
        this.stopRecordingListener = stopRecordingListener;
    }

    @Override
    protected void onStart() {
        super.onStart();
        recorder.setOnStopRecordingListener(stopRecordingListener);
        recorder.startRecording();
        rippleBackground.startRippleAnimation();
    }
}
