package com.idrnd.idvoice.recorders;

import com.idrnd.idvoice.ui.dialogs.interfaces.StopRecordingListener;
import net.idrnd.voicesdk.media.SpeechEndpointDetector;
import static com.idrnd.idvoice.IDVoiceApplication.singleTaskRunner;

/**
 * Class for recording and processing audio using SpeechEndpointDetector
 */
public class SpeechEndpointRecorder extends AudioRecorder {

    private SpeechEndpointDetector speechEndpointDetector;

    /**
     * Class for recording and processing audio using SpeechEndpointDetector
     * @param minSpeechLengthInSeconds amount of net speech length that is required
     * @param maxSilenceLengthInSeconds max silence length used to detect the end of speech
     */
    public SpeechEndpointRecorder(float minSpeechLengthInSeconds, float maxSilenceLengthInSeconds, int recorderSampleRate, StopRecordingListener stopRecordingListener) {
        super(recorderSampleRate);
        this.stopRecordingListener = stopRecordingListener;
        speechEndpointDetector = new SpeechEndpointDetector((int) (minSpeechLengthInSeconds * 1000), (int) (maxSilenceLengthInSeconds * 1000), recorderSampleRate);
    }

    public SpeechEndpointRecorder(float minSpeechLengthInSeconds, float maxSilenceLengthInSeconds, int recorderSampleRate) {
        this(minSpeechLengthInSeconds, maxSilenceLengthInSeconds, recorderSampleRate, null);
    }

    public SpeechEndpointRecorder(int recorderSampleRate) {
        this(0.8f, 0.3f, recorderSampleRate, null);
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
            // Add audioChuynk for processing
            speechEndpointDetector.addSamples(audioChunk);

            // Check if there is the end speech
            if(speechEndpointDetector.isSpeechEnded()) {
                speechEndpointDetector.reset();
                stopRecording();
            }
        });
    }
}
