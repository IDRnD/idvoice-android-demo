package com.idrnd.idvoice.recorders;

import com.idrnd.idvoice.ui.dialogs.interfaces.SpeechLengthUpdateListener;
import com.idrnd.idvoice.utils.EngineManager;

import net.idrnd.voicesdk.media.SpeechSummaryStream;

/**
 * Class for recording and processing audio using SpeechSummaryStream
 */
public class SpeechRecorder extends AudioRecorder {

    private SpeechLengthUpdateListener speechLengthUpdateListener;

    private float minSpeechLengthInSeconds;
    private float maxSilenceLengthInSeconds;

    private SpeechSummaryStream speechSummaryStream;

    /**
     * Class for recording and processing audio using SpeechSummaryStream
     * @param minSpeechLengthInSeconds amount of net speech length that is required
     * @param maxSilenceLengthInSeconds max silence length used to detect the end of speech
     */
    public SpeechRecorder(float minSpeechLengthInSeconds, float maxSilenceLengthInSeconds, int recordingSampleRate, SpeechLengthUpdateListener speechLengthUpdateListener) {
        super(recordingSampleRate);
        this.minSpeechLengthInSeconds = minSpeechLengthInSeconds;
        this.maxSilenceLengthInSeconds = maxSilenceLengthInSeconds;
        this.speechLengthUpdateListener = speechLengthUpdateListener;

        speechSummaryStream = EngineManager.getInstance().getSpeechSummaryEngine().createStream(recordingSampleRate);
    }

    public SpeechRecorder(float minSpeechLengthInSeconds, float maxSilenceLengthInSeconds, int recordingSampleRate) {
        this(minSpeechLengthInSeconds, maxSilenceLengthInSeconds, recordingSampleRate, null);
    }

    public SpeechRecorder(int recordingSampleRate) {
        this(10f, 0.3f, recordingSampleRate, null);
    }

    public void setOnSpeechLengthUpdateListener(SpeechLengthUpdateListener speechLengthUpdateListener) {
        this.speechLengthUpdateListener = speechLengthUpdateListener;
    }

    @Override
    protected void onStartRecording() {
        super.onStartRecording();
        // Reset stateful speech summary stream
        speechSummaryStream.reset();
    }

    @Override
    protected void onNextAudioChunk(byte[] audioChunk) {
        super.onNextAudioChunk(audioChunk);

        // Add audio samples to speech summary stream
        speechSummaryStream.addSamples(audioChunk);

        // Retrieve speech summary
        float speechLength = speechSummaryStream.getSpeechLength();
        float currentBackgroundLength = speechSummaryStream.getCurrentBackgroundLength();

        // Invoke speech length update listener (it updates UI)
        if (speechLengthUpdateListener != null) {
            speechLengthUpdateListener.onSpeechLengthUpdate(speechLength);
        }

        // Check if user stopped talking
        if (speechLength > minSpeechLengthInSeconds && currentBackgroundLength > maxSilenceLengthInSeconds) {
            stopRecording();
        }
    }
}