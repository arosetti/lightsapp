package com.lightsapp.core.analyzer.sound;

public class SpectrumFragment
{
    private int start;
    private int end;
    private Spectrum spectrum;

    private static double DISTINCT_FACTOR = 2;

    public SpectrumFragment(int start, int end, Spectrum spectrum)
    {
        this.start = start;
        this.end = end;
        this.spectrum = spectrum;
    }

    public void setMargins(int start, int end)
    {
        this.start = start;
        this.end = end;
    }

    public double getAverage()
    {
        double sum = 0;

        for(int i = start; i <= end; ++i)
            sum+=spectrum.get(i);

        return sum/((double)(end - start));
    }

    public Spectrum getSpectrum()
    {
        return spectrum;
    }

    public boolean[] getDistincts()
    {
        double average = getAverage();

        boolean[] ret = new boolean[spectrum.length()];

        for (boolean b : ret)
            b = false;

        for (int i = start; i <= end; ++i)
        {
            if (spectrum.get(i) > (average * DISTINCT_FACTOR))
            {
                ret[i] = true;
            }
        }

        return ret;
    }

    public double getMaxY()
    {
        double maxValue = 0;

        for(int i = start; i <= end; ++i)
            if(maxValue < spectrum.get(i))
            {
                maxValue = spectrum.get(i);
            }

        return maxValue;
    }

    public int getMaxX()
    {
        int max = 0;
        double maxValue = 0;

        for(int i = start; i <= end; ++i)
            if(maxValue < spectrum.get(i))
            {
                maxValue = spectrum.get(i);
                max = i;
            }

        return max;
    }

    public double getMinY()
    {
        double minValue = 0;

        for(int i = start; i <= end; ++i)
            if(minValue > spectrum.get(i))
            {
                minValue = spectrum.get(i);
            }

        return minValue;
    }

    public int getMinX()
    {
        int min = 0;
        double minValue = 0;

        for(int i = start; i <= end; ++i)
            if(minValue > spectrum.get(i))
            {
                minValue = spectrum.get(i);
                min = i;
            }

        return min;
    }
}