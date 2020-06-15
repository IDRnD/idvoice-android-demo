package com.idrnd.idvoice.models;

public class AudioRecord {

    public byte[] samples;
    public int sampleRate;

    public AudioRecord(byte[] samples, int sampleRate) {
        this.samples = samples;
        this.sampleRate = sampleRate;
    }
}