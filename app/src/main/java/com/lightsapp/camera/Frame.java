package com.lightsapp.camera;

public class Frame {
    private final String TAG = "Frame";
    public long delta;
    public long luminance;

    public Frame(long delta, long luminance) {
        this.delta = delta;
        this.luminance = luminance;
    }

    @Override
    public String toString() {
        return "[" + delta + "," + luminance + "]";
    }
}
