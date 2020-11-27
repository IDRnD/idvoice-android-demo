package com.idrnd.idvoice.recorders;

import android.media.AudioFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

    private enum Status {
        IDLE,
        WORKING
    }

    private static final String TAG = AudioRecorder.class.getSimpleName();
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    protected OnStopRecordingListener onStopRecordingListener;
    protected android.media.AudioRecord recorder;
    protected int minBufferSize;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int recorderSampleRate;
    private Thread recordingThread;
    private ByteArrayOutputStream audioDataOutputStream;
    private byte[] buffer;
    private Status status = Status.IDLE;

    public AudioRecorder(int recorderSampleRate) {
        this.recorderSampleRate = recorderSampleRate;

        // 1) Set buffer size
        minBufferSize = android.media.AudioRecord.getMinBufferSize(
                recorderSampleRate,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING
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
        recorder = new android.media.AudioRecord(
                audioSource,
                recorderSampleRate,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                minBufferSize
        );

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

            status = Status.WORKING;

            // Run buffer callback in background thread
            recordingThread = new Thread(
                    this::bufferCallback,
                    AudioRecorder.class.getSimpleName() + " Thread"
            );

            recordingThread.start();
        }
        onStartRecording();
    }

    protected void onNextAudioChunk(byte[] audioChunk) {
        // Nothing
    }

    protected void onBufferCallbackException(IOException exception) {
        // Nothing
    }

    private void bufferCallback() {
        while (status == Status.WORKING) {
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

    synchronized public AudioRecord stopRecording() throws IllegalStateException {
        if (status == Status.WORKING) {

            status = Status.IDLE;

            try {
                recorder.stop();
            } catch (IllegalStateException ex) {
                Log.e(TAG, "Error while audio recording", ex);
                throw ex;
            }

            recordingThread = null;

            AudioRecord audioRecord = new AudioRecord(
                    audioDataOutputStream.toByteArray(),
                    recorderSampleRate
            );

            audioDataOutputStream.reset();

            if (onStopRecordingListener != null) {
                handler.post(() -> onStopRecordingListener.onStopRecording(audioRecord));
            }
            return audioRecord;
        } else {
            if (onStopRecordingListener != null) {
                handler.post(() -> onStopRecordingListener.onStopRecording(null));
            }
            return null;
        }
    }
}