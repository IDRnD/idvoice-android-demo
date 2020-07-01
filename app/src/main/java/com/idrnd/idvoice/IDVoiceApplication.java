package com.idrnd.idvoice;

import android.app.Application;

import com.idrnd.idvoice.utils.runners.SingleTaskRunner;

public class IDVoiceApplication extends Application {

    /**
     * Global runner instance for execution in a single background thread
     */
    public static SingleTaskRunner singleTaskRunner = new SingleTaskRunner();

    /**
     * Sample rate in Hz for audio recording
     */
    public static int RECORDING_SAMPLE_RATE = 48000;
}
