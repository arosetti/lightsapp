package com.lightsapp.core.analyzer.sound;

import com.lightsapp.utils.math.FFT;

public class SoundDataBlock
{
    private double[] block;

    public SoundDataBlock(short[] buffer, int blockSize, int bufferReadSize)
    {
        block = new double[blockSize];

        for (int i = 0; i < blockSize && i < bufferReadSize; i++) {
            block[i] = (double) buffer[i];
        }
    }

    public SoundDataBlock(double[] buffer)
    {
        block = buffer;
    }


    public SoundDataBlock()
    {

    }

    public void setBlock(double[] block)
    {
        this.block = block;
    }

    public double[] getBlock()
    {
        return block;
    }

    public Spectrum FFT()
    {
        return new Spectrum(FFT.magnitudeSpectrum(block));
    }
}