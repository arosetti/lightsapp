package com.lightsapp.core.sound;

public class Frame {
    private final String TAG = Frame.class.getSimpleName();

    public long delta, timestamp;
    public long amplitude = -1, frequence = -1;
    public byte[] data_raw = null;

    public Frame(byte[] data, int a, int f) {
        this.data_raw = data;
        this.timestamp = timestamp;
        this.delta = delta;
    }

    public Frame analyze() {
        return null;
    }

    @Override
    public String toString() {
        return "";
    }
}
