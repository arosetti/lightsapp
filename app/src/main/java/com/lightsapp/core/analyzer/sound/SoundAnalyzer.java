package com.lightsapp.core.analyzer.sound;

import android.content.Context;
import android.util.Log;

import com.lightsapp.ui.MainActivity;
import com.lightsapp.utils.MyRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundAnalyzer extends MyRunnable {
    protected final String TAG = SoundAnalyzer.class.getSimpleName();

    protected MainActivity mContext;

    protected final int SLEEP_TIME = 30;
    protected final int SOGLIA = 100000;

    private int blockSize = 1024;
    private short[] buffer;
    private long timestamp_last;

    private Frame last_frame;
    private long time;

    private BlockingQueue<SoundDataBlock> bQueueSound;
    private BlockingQueue<Spectrum> bQueueSpectrum;
    private BlockingQueue<Frame> bQueueFrame;
    private List<Long> ldata;

    protected List<double[]> lfreqblocks;

    public SoundAnalyzer(Context context) {
        super(true);

        mContext = (MainActivity) context;

        bQueueSound = new LinkedBlockingQueue<SoundDataBlock>();
        bQueueSpectrum = new LinkedBlockingQueue<Spectrum>();
        bQueueFrame = new LinkedBlockingQueue<Frame>();
        lfreqblocks = new ArrayList<double[]>();
        buffer = new short[blockSize];

        ldata = new ArrayList<Long>();

        last_frame = null;
        time = 0;
    }

    protected void analyze()
    {
        // Se la coda è vuota (non dovrebbe capitare)
        if (bQueueFrame.isEmpty())
            return;

        // Se si analizza il primo frame
        if (last_frame == null) {
            try {
                last_frame = bQueueFrame.take();
            }
            catch (InterruptedException e) {
            }
            return;
        }

        try {
            Frame new_frame = bQueueFrame.take();

            // Spettro di differenza
            double[] double_diff = last_frame.diffSpectrum(new_frame);
            Spectrum spec_diff = new Spectrum(double_diff);
            SpectrumFragment sf = new SpectrumFragment(0, 400, spec_diff); // Valori a caso

            Spectrum spectrum = new Spectrum(new_frame.getSpectrum().getCopy());
            //spectrum.normalize();
            bQueueSpectrum.put(spec_diff);

            if (time >= 0){ // valuta condizione di discesa
                if (sf.getAverage() < -SOGLIA){
                    ldata.add(time);
                    time = 0;
                }
                else{
                    time += new_frame.delta;
                }
            }
            else { // valuta condizione di salita
                if (sf.getAverage() > SOGLIA){
                    ldata.add(time);
                    time = 0;
                }
                else{
                    time -= new_frame.delta;
                }
            }

            last_frame = new_frame;

            //if (ldata.size() > 0)
            //    mContext.mMorseA.analyze(ldata);
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
            Thread.sleep(SLEEP_TIME);
            SoundDataBlock data = null;
            if (mContext.mSoundController != null) {
                int ret = mContext.mSoundController.mAudioRec.read(buffer, 0, blockSize);
                if (ret > 0)
                {
                    data = new SoundDataBlock(buffer, blockSize, ret);
                    //bQueueSound.put(data);

                    long timestamp_now = System.currentTimeMillis();
                    Frame fdata = new Frame(data, timestamp_now, timestamp_now - timestamp_last);
                    bQueueFrame.add(fdata);
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

    public void reset() {

    }
}