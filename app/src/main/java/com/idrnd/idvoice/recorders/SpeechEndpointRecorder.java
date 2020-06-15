package com.idrnd.idvoice.recorders;

import android.util.Log;

import com.idrnd.idvoice.ui.dialogs.interfaces.OnStopRecordingListener;

import net.idrnd.voicesdk.common.VoiceSdkEngineException;
import net.idrnd.voicesdk.media.SpeechEndpointDetector;

import static com.idrnd.idvoice.IDVoiceApplication.MIN_AUDIO_SIGNAL_LENGTH_IN_MS;
import static com.idrnd.idvoice.IDVoiceApplication.singleTaskRunner;

/**
 * Class for recording and processing audio using SpeechEndpointDetector
 */
public class SpeechEndpointRecorder extends AudioRecorder {

    private static final String TAG = SpeechEndpointRecorder.class.getSimpleName();
    private static final int MIN_SPEECH_LENGTH_IN_MS = 800;
    private static final int MAX_SILENCE_LENGTH_IN_MS = 300;
    private final SpeechEndpointDetector speechEndpointDetector;

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
        synchronized (speechEndpointDetector) {
            speechEndpointDetector.reset();
        }
    }

    @Override
    protected void onNextAudioChunk(byte[] audioChunk) {
        super.onNextAudioChunk(audioChunk);

        if(getAudioDurationInMs(audioChunk.length, recorder.getSampleRate()) < MIN_AUDIO_SIGNAL_LENGTH_IN_MS) {
            Log.i(TAG, "Audio signal is too short, at least " + MIN_AUDIO_SIGNAL_LENGTH_IN_MS + " ms are required");
            return;
        }

        singleTaskRunner.execute(() -> {
                    // Add audioChunk for processing
                    synchronized (speechEndpointDetector) {
                        speechEndpointDetector.addSamples(audioChunk);

                        // Check if there is the end speech
                        if(speechEndpointDetector.isSpeechEnded()) {
                            speechEndpointDetector.reset();
                            stopRecording();
                        }
                    }
                }
        );
    }
}