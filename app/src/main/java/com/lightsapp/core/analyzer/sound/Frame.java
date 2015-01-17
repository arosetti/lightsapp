package com.lightsapp.core.analyzer.sound;

public class Frame {
    private final String TAG = Frame.class.getSimpleName();

    private SoundDataBlock data;
    private Spectrum spec;
    private SpectrumFragment sf;

    public long delta, timestamp;
    public int maxX;
    public double maxY;
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

    public Frame cutSpectrum(int min, int max) {
        if (data != null) {
            sf = new SpectrumFragment(min, max, this.getSpectrum());
        }
        return this;
    }

    public double getAverageMax(int deltaX) {
        if (sf != null) {
            maxX = sf.getMaxX();

            int minMargin = maxX - deltaX;
            int maxMargin = maxX + deltaX;
            if (maxX - deltaX < sf.getMarginMin())
                minMargin = sf.getMarginMin();
            if (maxX + deltaX > sf.getMarginMax())
                maxMargin = sf.getMarginMax();

            sf.setMargins(minMargin, maxMargin);
            avg = sf.getAverage();
            return avg;
        }
        return 0.0;
    }

    public void clean() {
        sf = null;
        spec = null;
        data = null;
    }

    @Override
    public String toString() {
        return "[" + delta + ",  ]";
    }
}