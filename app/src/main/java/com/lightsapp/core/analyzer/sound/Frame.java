package com.lightsapp.core.analyzer.sound;

public class Frame {
    private final String TAG = Frame.class.getSimpleName();

    private SoundDataBlock data;
    private Spectrum spec;

    public long delta, timestamp;
    public double max;
    public double avg;

    public Frame(SoundDataBlock data, long timestamp, long delta) {
        this.data = data;
        this.timestamp = timestamp;
        this.delta = delta;
        spec = null;
    }

    public Spectrum getSpectrum() {
        if (spec == null) {
            spec = data.FFT();
            //spec.spectrumSmoothing();
        }
        return spec;
    }

    public double[] diffSpectrum(Frame mFrame) {
        return this.getSpectrum().diff(mFrame.getSpectrum());
    }

    public Frame analyze() {
        if (data != null) {
            SpectrumFragment sf = new SpectrumFragment(80, 200, this.getSpectrum());
            max = sf.getMaxY();
            avg = sf.getAverage();
        }
        return this;
    }

    @Override
    public String toString() {
        return "[" + delta + ",  ]";
    }
}