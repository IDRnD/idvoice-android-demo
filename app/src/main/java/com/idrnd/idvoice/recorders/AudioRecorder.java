package com.idrnd.idvoice.recorders;

import android.media.AudioFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.idrnd.idvoice.BuildConfig;
import com.idrnd.idvoice.models.AudioRecord;
import com.idrnd.idvoice.ui.dialogs.interfaces.OnStopRecordingListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaRecorder.AudioSource.UNPROCESSED;
import static android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION;

/**
 * Class for audio recording. Recording is performed in a separate thread.
 * stopRecording() method should be called in order to stop recording and retrieve audio record
 */
public class AudioRecorder {

    public enum Status {
        IDLE,
        RECORDING
    }

    private static final String TAG = AudioRecorder.class.getSimpleName();
    private static final int AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    protected OnStopRecordingListener onStopRecordingListener;
    protected android.media.AudioRecord recorder;
    protected int minBufferSize;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int sampleRate;
    private Thread recordingThread;
    private final ByteArrayOutputStream audioDataOutputStream;
    private final byte[] buffer;
    private Status status = Status.IDLE;

    /**
     * Get audio duration in seconds of number bytes (PCM16 only)
     *
     * @param numberBytes
     * @param sampleRate
     * @return audio duration in ms
     */
    public long getAudioDurationInMs(int numberBytes, int sampleRate) {
        int bytePerSecond = sampleRate * 2;
        return (long)(((float)numberBytes / bytePerSecond) * 1000);
    }

    public AudioRecorder(int sampleRate) {
        this.sampleRate = sampleRate;

        // 1) Set buffer size
        minBufferSize = android.media.AudioRecord.getMinBufferSize(
                sampleRate,
                AUDIO_CHANNELS,
                AUDIO_ENCODING
        ) * 3;

        // 2) Check if desired buffer size is too big for hardware and decrease it if necessary
        if (minBufferSize < 0) {
            minBufferSize = 4000;
        }

        // 3) Allocate memory for buffer
        buffer = new byte[minBufferSize];

        // 4) Init Android audio recorder
        int audioSource;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioSource = UNPROCESSED;
        } else {
            audioSource = VOICE_RECOGNITION;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AUDIO_ENCODING)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AUDIO_CHANNELS)
                    .build();

            android.media.AudioRecord.Builder builder = new android.media.AudioRecord.Builder()
                    .setAudioSource(audioSource)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(minBufferSize);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Mark audio source as privacy-sensitive
                builder.setPrivacySensitive(true);
            }

            recorder = builder.build();
        } else {
            recorder = new android.media.AudioRecord(
                    audioSource,
                    sampleRate,
                    AUDIO_CHANNELS,
                    AUDIO_ENCODING,
                    minBufferSize
            );
        }

        audioDataOutputStream = new ByteArrayOutputStream(minBufferSize);
    }

    public void setOnStopRecordingListener(OnStopRecordingListener onStopRecordingListener) {
        this.onStopRecordingListener = onStopRecordingListener;
    }

    protected void onStartRecording() {
        // Nothing
    }

    public void startRecording() {
        if (status == Status.IDLE) {
            recorder.startRecording();

            status = Status.RECORDING;

            // Run buffer callback in background thread
            recordingThread = new Thread(
                    this::bufferCallback,
                    AudioRecorder.class.getSimpleName() + " Thread"
            );

            recordingThread.start();
            onStartRecording();
        }
    }

    synchronized public AudioRecord stopRecording() throws IllegalStateException {
        if (BuildConfig.DEBUG && !(status == Status.RECORDING)) {
            throw new AssertionError("Stop recording must be called while is recording");
        }

        status = Status.IDLE;

        try {
            recorder.stop();
        } catch (IllegalStateException ex) {
            Log.e(TAG, "Error while audio recording", ex);
            throw ex;
        }

        recordingThread.interrupt();
        recordingThread = null;

        AudioRecord audioRecord = new AudioRecord(
                audioDataOutputStream.toByteArray(),
                sampleRate
        );

        audioDataOutputStream.reset();

        if (onStopRecordingListener != null) {
            handler.post(() -> onStopRecordingListener.onStopRecording(audioRecord));
        }

        return audioRecord;
    }

    public Status getRecordingStatus() {
        return status;
    }

    protected void onNextAudioChunk(byte[] audioChunk) {
        // Nothing
    }

    protected void onBufferCallbackException(IOException exception) {
        // Nothing
    }

    private void bufferCallback() {
        while (status == Status.RECORDING && !recordingThread.isInterrupted()) {

            // 1) Read audio buffer from recorder
            int numReadBytes = recorder.read(buffer, 0, minBufferSize);

            if (numReadBytes > 0) {

                ByteBuffer recordedBytes = ByteBuffer.allocate(numReadBytes);
                recordedBytes.put(buffer, 0, numReadBytes);

                // 2) Collect audio samples
                try {
                    audioDataOutputStream.write(recordedBytes.array());
                    onNextAudioChunk(recordedBytes.array());
                } catch (IOException e) {
                    onBufferCallbackException(e);
                }
            }
        }
    }
}