package com.lightsapp.camera;

public class LFrame {
    public long delta;
    public long luminance;

    public LFrame(long delta, long luminance) {
        this.delta = delta;
        this.luminance = luminance;
    }

    @Override
    public String toString() {
        return "[" + delta + "," + luminance + "] ";
    }
}
