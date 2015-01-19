package com.lightsapp.core.analyzer.sound;

import android.content.Context;
import android.util.Log;

import com.lightsapp.core.analyzer.BaseAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundAnalyzer extends BaseAnalyzer {
    protected final String TAG = SoundAnalyzer.class.getSimpleName();

    protected int THRESHOLD = 10000;

    private int blockSize = 1024;
    private short[] buffer;

    private long time;
    private boolean signal_up;

    private BlockingQueue<Spectrum> bQueueSpectrum;
    private BlockingQueue<Frame> bQueueFrameIn;
    private BlockingQueue<Frame> bQueueFrameElaborated;
    private List<Long> ldata;

    protected List<double[]> lfreqblocks;

    public SoundAnalyzer(Context context) {
        super(context);

        bQueueSpectrum = new LinkedBlockingQueue<Spectrum>();
        bQueueFrameIn = new LinkedBlockingQueue<Frame>();
        bQueueFrameElaborated = new LinkedBlockingQueue<Frame>();
        lfreqblocks = new ArrayList<double[]>();
        buffer = new short[blockSize];

        ldata = new ArrayList<Long>();

        signal_up = false;
        sleep_time = 30;
        time = 0;
    }

    protected void analyze()
    {
        // Se la coda è vuota (non dovrebbe capitare)
        if (bQueueFrameIn.isEmpty())
            return;

        try {
            Frame new_frame = bQueueFrameIn.take();

            // Per il grafico
            Spectrum spectrum = new Spectrum(new_frame.getSpectrum().getCopy());
            bQueueSpectrum.put(spectrum);

            new_frame.cutSpectrum(10, 400);

            if (signal_up){ // valuta condizione di discesa
                if (new_frame.getAverageMax(2) < (THRESHOLD * sensitivity)){
                    signal_up = false;
                    ldata.add(time);
                    time = 0;
                }
                else{
                    time += new_frame.delta;
                }
                Log.v(TAG, "Is Up, average: "+new_frame.avg);
            }
            else { // valuta condizione di salita
                if (new_frame.getAverageMax(2) > (THRESHOLD * sensitivity))
                {
                    signal_up = true;
                    ldata.add(time);
                    time = 0;
                }
                else{
                    time -= new_frame.delta;
                }
                Log.v(TAG, "Is Down, average: "+new_frame.avg);
            }

            new_frame.clean();
            bQueueFrameElaborated.put(new_frame);

            if (enable_analyze.get() && ldata.size() > 0)
                mContext.mMorseA.analyze(ldata);
        }
        catch (InterruptedException e) {
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    public double[] getFrames()
    {
        try {
            if (!bQueueSpectrum.isEmpty()) {
                Spectrum spectrum = bQueueSpectrum.peek();
                bQueueSpectrum.clear();
                return spectrum.getData();
            }
        }
        catch (Exception e){
            Log.d(TAG, "queue error: " + e.getMessage());
        }

        return null;
    }

    @Override
    public final void loop() {
        try {
            Thread.sleep(sleep_time);
            SoundDataBlock data = null;
            if (mContext.mSoundController != null) {
                int ret = mContext.mSoundController.mAudioRec.read(buffer, 0, blockSize);
                if (ret > 0)
                {
                    data = new SoundDataBlock(buffer, blockSize, ret);

                    long timestamp_now = System.currentTimeMillis();
                    Frame fdata = new Frame(data, timestamp_now, timestamp_now - timestamp_last);
                    bQueueFrameIn.add(fdata);
                    timestamp_last = timestamp_now;

                    analyze();
                }
                else
                    Log.v(TAG, "audio recording ret is 0");
            }
        }
        catch (InterruptedException e) {
        }
        catch (Exception e) {
            Log.e(TAG, "error analyzing audio frames: " + e.getMessage());
            e.printStackTrace();
        }
    }
}