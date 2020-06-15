package com.idrnd.idvoice;

import android.app.Application;

import com.idrnd.idvoice.utils.runners.SingleTaskRunner;
import com.idrnd.idvoice.utils.verification.VoiceVerifyEngine.Accuracy;

public class IDVoiceApplication extends Application {

    /**
     * Global runner instance for execution in a single background thread
     */
    public static SingleTaskRunner singleTaskRunner = new SingleTaskRunner();

    /**
     * Sample rate in Hz for audio recording
     */
    public static int RECORDING_SAMPLE_RATE = 48000;

    /**
     * Minimal signal length in ms for processing in VoiceSDK
     */
    public static final float MIN_AUDIO_SIGNAL_LENGTH_IN_MS = 32f;

    /**
     * Global accuracy level for verification
     */
    public static final Accuracy TD_VERIFY_ACCURACY = Accuracy.HIGH;
    public static final Accuracy TI_VERIFY_ACCURACY = Accuracy.MEDIUM;
}