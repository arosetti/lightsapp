package com.lightsapp.core.analyzer.sound;

import com.lightsapp.utils.math.LinearFilterD;

import static com.lightsapp.utils.math.DFT.HANN;
import static com.lightsapp.utils.math.DFT.window;

public class Spectrum {
    protected final String TAG = SoundAnalyzer.class.getSimpleName();

    private double[] spectrum;
    private int length;

    public Spectrum(double[] spectrum)
    {
        this.spectrum = spectrum;
        this.length = spectrum.length;
    }

    public double[] diff(Spectrum spec)
    {
        if (this.length() != spec.length())
            return null;

        double[] block = new double[this.length()];
        for (int i=0; i<length; ++i) {
            block[i] = spectrum[i] - spec.get(i);
        }

        return block;
    }

    public double[] getCopy()
    {
        double[] block = new double[this.length()];
        for (int i=0; i<length; ++i) {
            block[i] = spectrum[i];
        }

        return block;
    }

    public void spectrumSmoothing() {
        LinearFilterD lf = LinearFilterD.get(LinearFilterD.Filter.KERNEL_SAVITZKY_GOLAY_5);
        lf.apply(spectrum);
    }

    public void spectrumHann() {
        spectrum = window(spectrum, HANN);
    }

    public void normalize()
    {
        double maxValue = 0.0;

        for (int i=0;i<length; ++i)
            if (maxValue < spectrum[i])
                maxValue = spectrum[i];

        if (maxValue != 0)
            for (int i=0;i<length; ++i)
                spectrum[i] /= maxValue;
    }

    public double[] getData() { return spectrum; }

    public double get(int index)
    {
        return spectrum[index];
    }

    public int length()
    {
        return length;
    }
}