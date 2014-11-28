package com.lightsapp.core.sound;

import android.content.Context;
import android.util.Log;

import com.lightsapp.lightsapp.MainActivity;
import com.lightsapp.utils.MyRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class SoundAnalyzer extends MyRunnable {
    protected final String TAG = SoundAnalyzer.class.getSimpleName();
    protected String NAME = "SoundAnalyzer";

    protected MainActivity mCtx;

    protected final int SLEEP_TIME = 100;

    private RealDoubleFFT transformer;
    private int blockSize = 256;
    private short[] buffer;
    //private double[] toTransform;

    BlockingQueue<SoundDataBlock> blockingQueueSound;

    protected List<double[]> lfreqblocks;

    public SoundAnalyzer(Context context) {
        super(true);

        mCtx = (MainActivity) context;
        transformer = new RealDoubleFFT(blockSize);

        blockingQueueSound = new LinkedBlockingQueue<SoundDataBlock>();
        //toTransform = new double[blockSize];
        lfreqblocks = new ArrayList<double[]>();
    }

    public double[] getFrames()
    {
        try {
            // Questa parte va spostata nell'analisi e creato una copia del vettore per il grafico
            SoundDataBlock dataBlock_2 = blockingQueueSound.take();
            Spectrum spectrum = dataBlock_2.FFT();

            //spectrum.normalize();

            double[] toReturn = new double[spectrum.length()];

            for (int i = 0; i < spectrum.length(); ++i)
            {
                toReturn[i] = spectrum.get(i);
            }
            return toReturn;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public final void loop() {
        try {
            Thread.sleep(SLEEP_TIME);

            buffer = new short[blockSize];

            int bufferReadResult = mCtx.mSoundController.mAudioRec.read(buffer, 0, blockSize);

            SoundDataBlock dataBlock = new SoundDataBlock(buffer, blockSize, bufferReadResult);

            blockingQueueSound.clear();
            blockingQueueSound.put(dataBlock);

            /*toTransform = new double[blockSize];
            for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                toTransform[i] = (double) buffer[i] / 32768.0; // signed   16bit
            }
            transformer.ft(toTransform);*/

            // Calculate the Real and imaginary and Magnitude
            /*for(int i = 0; i < blockSize; i++){
                // real is stored in first part of array
                re[i] = toTransform[i*2];
                // imaginary is stored in the sequential part
                im[i] = toTransform[(i*2)+1];
                // magnitude is calculated by the square root of (imaginary^2 + real^2)
                magnitude[i] = Math.sqrt((re[i] * re[i]) + (im[i]*im[i]));
            }*/

            //lfreqblocks.add(toTransform);

            Thread.sleep(SLEEP_TIME);
            //if (enable_analyze.get())
                //analyze();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            Log.e(TAG, "error analyzing audio frames: " + e.getMessage());
        }
        finally {
            //signalGraph();
        }
    }

    public void reset() {
        /*lock_frames_tmp.lock();
        try {
            sframes_tmp.clear();
        }
        finally {
            lock_frames_tmp.unlock();
        }

        lock_frames.lock();
        try {
            sframes.clear();
        }
        finally {
            lock_frames.unlock();
        }*/
    }

}