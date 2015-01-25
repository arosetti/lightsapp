package com.lightsapp.core.analyzer.sound;

import android.util.Log;

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
        sf = null;
    }

    public Spectrum getSpectrum() {
        if (spec == null && data != null) {
            spec = data.FFT();
            //spec.spectrumHann();
        }
        return spec;
    }

    public double[] diffSpectrum(Frame mFrame) {
        return this.getSpectrum().diff(mFrame.getSpectrum());
    }

    public Frame cutSpectrum(int min, int max) {
        if (data != null) {
            sf = new SpectrumFragment(min, max, spec);
        }
        return this;
    }

    public double getMax() {
        if (sf == null)
            sf = new SpectrumFragment(0, spec.length(), spec);

        maxX = sf.getMaxX();
        maxY = sf.getMaxY();
        //Log.v(TAG, "MaxY: " + maxY);
        return maxY;
    }

    public double getAverageMax(int deltaX) {
        if (sf == null)
            sf = new SpectrumFragment(0, spec.length(), spec);

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