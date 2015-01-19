package com.lightsapp.core.analyzer.sound;

import android.content.Context;
import android.util.Log;

import com.lightsapp.core.analyzer.BaseAnalyzer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundAnalyzer extends BaseAnalyzer {
    protected final String TAG = SoundAnalyzer.class.getSimpleName();

    protected int THRESHOLD = 10000;

    private int sampleRate = 16000;
    private int blockSize = 512;
    private short[] buffer;

    private int beepFreq;
    private int beepFreqval;
    private int min_beepFreqval;
    private int max_beepFreqval;
    private int bandwith;

    private long time;
    private boolean signal_up;
    private boolean threshold_changed;

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
        bandwith = 50;

        threshold_changed = false;

        beepFreq = Integer.valueOf(mContext.mPrefs.getString("beep_freq", "850"));
        beepFreqval = beepFreq * blockSize / sampleRate;
        min_beepFreqval = 0;
        max_beepFreqval = 511;
        if (beepFreqval > bandwith)
            min_beepFreqval = beepFreqval-bandwith;
        if (beepFreqval < 511-bandwith)
            max_beepFreqval = beepFreqval+bandwith;
    }

    public void reset(){
        bQueueFrameElaborated.clear();
        ldata.clear();
        signal_up = false;
        time = 0;
    }

    protected void reanalyze()
    {
        ldata.clear();
        signal_up = false;
        time = 0;

        for (Frame f: bQueueFrameElaborated) {
            if (signal_up){ // valuta condizione di discesa
                if (f.avg < (THRESHOLD * sensitivity)){
                    signal_up = false;
                    ldata.add(time);
                    time = 0;
                }
                else{
                    time += f.delta;
                }
            }
            else { // valuta condizione di salita
                if (f.avg > (THRESHOLD * sensitivity))
                {
                    signal_up = true;
                    ldata.add(time);
                    time = 0;
                }
                else{
                    time -= f.delta;
                }
            }
        }
    }

    protected void analyze() {
        // Se la coda è vuota (non dovrebbe capitare)
        if (bQueueFrameIn.isEmpty())
            return;

        // controlla che non c'è un cambio di frequenza di ricezione
        if (beepFreq != Integer.valueOf(mContext.mPrefs.getString("beep_freq", "850"))){
            reset();
            beepFreq = Integer.valueOf(mContext.mPrefs.getString("beep_freq", "850"));
            beepFreqval = beepFreq * blockSize / sampleRate;
            Log.v(TAG, "beepFreqval: "+beepFreqval);
            min_beepFreqval = 0;
            max_beepFreqval = 511;
            if (beepFreqval > bandwith)
                min_beepFreqval = beepFreqval-bandwith;
            if (beepFreqval < 511-bandwith)
                max_beepFreqval = beepFreqval+bandwith;
        }

        try {

            if (threshold_changed){
                reanalyze();
                threshold_changed = false;
            }


            Frame new_frame = bQueueFrameIn.take();

            // Per il grafico
            Spectrum spectrum = new Spectrum(new_frame.getSpectrum().getCopy());
            bQueueSpectrum.put(spectrum);

            new_frame.cutSpectrum(min_beepFreqval, max_beepFreqval);

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

            Log.v(TAG, "Delta: "+new_frame.delta);
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

    @Override
    public void setSensitivity(int sensitivity) {
        super.setSensitivity(sensitivity);
        mContext.graphView_snd.setManualYAxisBounds(sensitivity * THRESHOLD, 0);
    }

}