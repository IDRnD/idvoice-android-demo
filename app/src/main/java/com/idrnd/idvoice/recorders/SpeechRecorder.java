package com.idrnd.idvoice.recorders;

import android.util.Log;

import com.idrnd.idvoice.ui.dialogs.interfaces.OnSpeechLengthUpdateListener;
import com.idrnd.idvoice.utils.verification.EngineManager;

import net.idrnd.voicesdk.media.SpeechSummaryStream;

import static com.idrnd.idvoice.IDVoiceApplication.MIN_AUDIO_SIGNAL_LENGTH_IN_MS;

/**
 * Class for recording and processing audio using SpeechSummaryStream
 */
public class SpeechRecorder extends AudioRecorder {

    private static final float MIN_SPEECH_LENGTH_IN_MS = 10000f;
    private static final float MAX_SILENCE_LENGTH_IN_MS = 300f;

    private OnSpeechLengthUpdateListener onSpeechLengthUpdateListener;
    private final float minSpeechLengthInMs;
    private final float maxSilenceLengthInMs;
    private final String TAG = SpeechRecorder.class.getSimpleName();

    private final SpeechSummaryStream speechSummaryStream;

    /**
     * Class for recording and processing audio using SpeechSummaryStream
     * @param minSpeechLengthInMs amount of net speech length that is required
     * @param maxSilenceLengthInMs max silence length used to detect the end of speech
     */
    public SpeechRecorder(
            float minSpeechLengthInMs,
            float maxSilenceLengthInMs,
            int recordingSampleRate,
            OnSpeechLengthUpdateListener onSpeechLengthUpdateListener
    ) {
        super(recordingSampleRate);
        this.minSpeechLengthInMs = minSpeechLengthInMs;
        this.maxSilenceLengthInMs = maxSilenceLengthInMs;
        this.onSpeechLengthUpdateListener = onSpeechLengthUpdateListener;

        speechSummaryStream = EngineManager.getInstance().getSpeechSummaryEngine().createStream(recordingSampleRate);
    }

    public SpeechRecorder(float minSpeechLengthInMs, float maxSilenceLengthInMs, int recordingSampleRate) {
        this(minSpeechLengthInMs, maxSilenceLengthInMs, recordingSampleRate, null);
    }

    public SpeechRecorder(int recordingSampleRate) {
        this(MIN_SPEECH_LENGTH_IN_MS, MAX_SILENCE_LENGTH_IN_MS, recordingSampleRate, null);
    }

    public void setOnSpeechLengthUpdateListener(OnSpeechLengthUpdateListener onSpeechLengthUpdateListener) {
        this.onSpeechLengthUpdateListener = onSpeechLengthUpdateListener;
    }

    @Override
    protected void onStartRecording() {
        super.onStartRecording();
        // Reset stateful speech summary stream
        synchronized (speechSummaryStream) {
            speechSummaryStream.reset();
        }
    }

    @Override
    protected void onNextAudioChunk(byte[] audioChunk) {
        super.onNextAudioChunk(audioChunk);

        if(getAudioDurationInMs(audioChunk.length, recorder.getSampleRate()) < MIN_AUDIO_SIGNAL_LENGTH_IN_MS) {
            Log.i(TAG, "Audio signal is too short, at least " + MIN_AUDIO_SIGNAL_LENGTH_IN_MS + " ms are required");
            return;
        }

        // Add audio samples to speech summary stream
        synchronized (speechSummaryStream) {
            speechSummaryStream.addSamples(audioChunk);

            // Retrieve speech summary
            float speechLengthInMs = speechSummaryStream.getTotalSpeechInfo().getSpeechLengthMs();
            float currentBackgroundLength = speechSummaryStream.getCurrentBackgroundLength();

            // Invoke speech length update listener (it updates UI)
            if (onSpeechLengthUpdateListener != null) {
                onSpeechLengthUpdateListener.onSpeechLengthUpdate(speechLengthInMs);
            }

            // Check if user stopped talking
            if ((speechLengthInMs > minSpeechLengthInMs) && (currentBackgroundLength > maxSilenceLengthInMs)) {
                stopRecording();
            }
        }
    }
}