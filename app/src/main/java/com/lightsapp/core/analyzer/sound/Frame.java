package com.lightsapp.core.analyzer.sound;

public class Frame {
    private final String TAG = Frame.class.getSimpleName();

    SoundDataBlock data;

    public long delta, timestamp;
    public int max;
    public double avg;


    public Frame(SoundDataBlock data, long timestamp, long delta) {
        this.data = data;
        this.timestamp = timestamp;
        this.delta = delta;
    }

    public Frame analyze() {
        if (data != null) {
            Spectrum s = data.FFT();
            SpectrumFragment sf = new SpectrumFragment(80, 200, s);
            max = sf.getMax();
            avg = sf.getAverage();
        }
        return this;
    }

    @Override
    public String toString() {
        return "[" + delta + ",  ]";
    }
}