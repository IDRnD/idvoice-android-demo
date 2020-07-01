package com.idrnd.idvoice.recorders;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.idrnd.idvoice.models.AudioRecord;
import com.idrnd.idvoice.ui.dialogs.interfaces.NextVerifyResultListener;
import com.idrnd.idvoice.utils.EngineManager;
import com.idrnd.idvoice.utils.Prefs;

import net.idrnd.voicesdk.verify.VoiceTemplate;
import net.idrnd.voicesdk.verify.VoiceVerifyEngine;
import net.idrnd.voicesdk.verify.VoiceVerifyStream;

import static com.idrnd.idvoice.utils.Prefs.VoiceTemplateType.TextIndependent;

/**
 * Class for audio recording combined with continuous voice verification. It does not employ anti-spoofing check.
 */
public class ContinuousVerifyRecorder extends AudioRecorder {

    private final String TAG = ContinuousVerifyRecorder.class.getSimpleName();
    private VoiceVerifyStream voiceVerifyStream;
    private VoiceVerifyEngine verifyEngine;
    private NextVerifyResultListener nextVerifyResultListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Class for audio recording combined with continuous voice verification. It does not employ anti-spoofing check
     * @param recorderSampleRate audio record sampling rate
     * @param nextVerifyResultListener callback for processing produced verify results
     */
    public ContinuousVerifyRecorder(int recorderSampleRate, NextVerifyResultListener nextVerifyResultListener) {
        super(recorderSampleRate);
        this.nextVerifyResultListener = nextVerifyResultListener;

        // Get text independent voice template
        VoiceTemplate enrollTemplate = VoiceTemplate.deserialize(Prefs.getInstance().getVoiceTemplate(TextIndependent));

        // For a detailed explanation of this parameter please refer to https://docs.idrnd.net/voice/#idvoice-speaker-verification
        // ('Continuous speaker verification' section)
        final int windowLengthSeconds = 4;

        // It is important to keep this object alive the whole time VoiceVerifyStream exists
        // For continuous verification, text-independent verification should be used
        verifyEngine = EngineManager.getInstance().getVerifyEngine(TextIndependent);

        voiceVerifyStream = verifyEngine.createVerifyStream(enrollTemplate, recorderSampleRate, windowLengthSeconds);
    }

    @Override
    public synchronized AudioRecord stopRecording() {
        voiceVerifyStream.reset();
        return super.stopRecording();
    }

    @Override
    protected void onNextAudioChunk(byte[] audioChunk) {
        super.onNextAudioChunk(audioChunk);
        try {
            // Process new byte array with audio chunk
            voiceVerifyStream.addSamples(audioChunk);

            // Check the produced verify result
            if (voiceVerifyStream.hasVerifyResults()) {
                // Return verify probability
                float newProbability = voiceVerifyStream.getVerifyResult().getVerifyResult().getProbability();

                handler.post(() -> nextVerifyResultListener.onNextVerifyResult(newProbability));
            }
        } catch (Exception e) {
            Log.e(TAG, "Something wrong when process audio chunk using VoiceVerifyStream", e);
        }
    }

    public void setOnNextVerifyResultListener(NextVerifyResultListener nextVerifyResultListener) {
        this.nextVerifyResultListener = nextVerifyResultListener;
    }
}







