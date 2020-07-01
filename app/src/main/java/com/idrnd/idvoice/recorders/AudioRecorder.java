package com.idrnd.idvoice.recorders;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import com.idrnd.idvoice.models.AudioRecord;
import com.idrnd.idvoice.ui.dialogs.interfaces.StopRecordingListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for audio recording. Recording is performed in a separate thread.
 * stopRecording() method should be called in order to stop recording and retrieve audio record
 */
public class AudioRecorder {
    private enum Status {
        IDLE,
        WORKING
    }

    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    protected StopRecordingListener stopRecordingListener;
    protected android.media.AudioRecord recorder;
    protected int bufferSize;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int recorderSampleRate;
    private Thread recordingThread;
    private List<byte[]> audioData;
    private byte[] buffer;
    private Status status = Status.IDLE;

    public AudioRecorder(int recorderSampleRate) {
        this.recorderSampleRate = recorderSampleRate;

        // 1) Set buffer size
        bufferSize = android.media.AudioRecord.getMinBufferSize(recorderSampleRate, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 3;

        // 2) Check if desired buffer size is too big for hardware and decrease it if necessary
        if (bufferSize < 0) {
            bufferSize = 4000;
        }

        // 3) Allocate memory for buffer
        buffer = new byte[bufferSize];

        // 4) Init Android audio recorder
        recorder = new android.media.AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            recorderSampleRate, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, bufferSize
        );

        audioData = new ArrayList<>();
    }

    public void setStopRecordingListener(StopRecordingListener stopRecordingListener) {
        this.stopRecordingListener = stopRecordingListener;
    }

    protected void onStartRecording() {}

    public void startRecording() {
        if (status == Status.IDLE) {
            recorder.startRecording();

            status = Status.WORKING;

            // Run buffer callback in background thread
            recordingThread = new Thread(this::bufferCallback, "AudioRecorder Thread");
            recordingThread.start();
        }
        onStartRecording();
    }

    protected void onNextAudioChunk(byte[] audioChunk) {}

    private void bufferCallback() {
        int read;

        while (status == Status.WORKING) {
            // 1) Read audio buffer from recorder
            read = recorder.read(buffer, 0, bufferSize);

            if (read != android.media.AudioRecord.ERROR_INVALID_OPERATION) {
                // 2) Collect audio samples
                audioData.add(buffer.clone());
                onNextAudioChunk(buffer.clone());
            }
        }
    }

    synchronized public AudioRecord stopRecording() {
        if (status == Status.WORKING) {

            status = Status.IDLE;

            try {
                recorder.stop();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }

            recordingThread = null;

            // Convert ArrayList of audio buffers to raw array
            byte[] audioDataArray = new byte[audioData.size() * bufferSize];

            for (int i = 0; i < audioData.size(); i++) {
                System.arraycopy(
                    audioData.get(i),
                    0,
                    audioDataArray,
                    i * bufferSize,
                    bufferSize
                );
            }

            AudioRecord audioRecord = new AudioRecord(audioDataArray, recorderSampleRate);

            if (stopRecordingListener != null) {
                handler.post(() -> stopRecordingListener.onStopRecording(audioRecord));
            }

            return audioRecord;
        } else {
            if (stopRecordingListener != null) {
                handler.post(() -> stopRecordingListener.onStopRecording(null));
            }

            return null;
        }
    }
}