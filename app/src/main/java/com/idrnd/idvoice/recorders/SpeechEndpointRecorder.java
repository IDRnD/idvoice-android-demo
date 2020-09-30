package com.idrnd.idvoice.recorders;

import android.util.Log;

import com.idrnd.idvoice.ui.dialogs.interfaces.OnStopRecordingListener;

import net.idrnd.voicesdk.common.VoiceSdkEngineException;
import net.idrnd.voicesdk.media.SpeechEndpointDetector;

import static com.idrnd.idvoice.IDVoiceApplication.singleTaskRunner;

/**
 * Class for recording and processing audio using SpeechEndpointDetector
 */
public class SpeechEndpointRecorder extends AudioRecorder {

    private static final String TAG = SpeechEndpointRecorder.class.getSimpleName();
    private static final int MIN_SPEECH_LENGTH_IN_MS = 800;
    private static final int MAX_SILENCE_LENGTH_IN_MS = 300;
    private SpeechEndpointDetector speechEndpointDetector;

    /**
     * Class for recording and processing audio using SpeechEndpointDetector
     * @param minSpeechLengthInMs amount of speech length that is required
     * @param maxSilenceLengthInMs max silence length used to detect the end of speech
     */
    public SpeechEndpointRecorder(
            int minSpeechLengthInMs,
            int maxSilenceLengthInMs,
            int recorderSampleRate,
            OnStopRecordingListener onStopRecordingListener
    ) {
        super(recorderSampleRate);
        this.onStopRecordingListener = onStopRecordingListener;
        speechEndpointDetector = new SpeechEndpointDetector(minSpeechLengthInMs, maxSilenceLengthInMs, recorderSampleRate);
    }

    public SpeechEndpointRecorder(int minSpeechLengthInMs, int maxSilenceLengthInMs, int recorderSampleRate) {
        this(minSpeechLengthInMs, maxSilenceLengthInMs, recorderSampleRate, null);
    }

    public SpeechEndpointRecorder(int recorderSampleRate) {
        this(MIN_SPEECH_LENGTH_IN_MS, MAX_SILENCE_LENGTH_IN_MS, recorderSampleRate, null);
    }

    @Override
    protected void onStartRecording() {
        super.onStartRecording();
        speechEndpointDetector.reset();
    }

    @Override
    protected void onNextAudioChunk(byte[] audioChunk) {
        super.onNextAudioChunk(audioChunk);

        singleTaskRunner.execute(() -> {
            // Add audioChunk for processing
            try {
                speechEndpointDetector.addSamples(audioChunk);
            } catch (VoiceSdkEngineException exception) {
                if(exception.getMessage().contains("input audio signal is too short, at least 32 ms are required")) {
                    Log.d(TAG, exception.getMessage());
                } else {
                    throw exception;
                }
            }

            // Check if there is the end speech
            if(speechEndpointDetector.isSpeechEnded()) {
                speechEndpointDetector.reset();
                stopRecording();
            }
        });
    }
}
