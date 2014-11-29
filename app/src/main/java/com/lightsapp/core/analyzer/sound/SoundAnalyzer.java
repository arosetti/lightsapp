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
    private int blockSize = 1024;
    private short[] buffer;

    BlockingQueue<SoundDataBlock> bQueueSound;
    protected List<double[]> lfreqblocks;

    public SoundAnalyzer(Context context) {
        super(true);

        mContext = (MainActivity) context;

        bQueueSound = new LinkedBlockingQueue<SoundDataBlock>();
        lfreqblocks = new ArrayList<double[]>();
        buffer = new short[blockSize];
    }

    public double[] getFrames()
    {
        SoundDataBlock data = null;

        try {
            if(!bQueueSound.isEmpty())
                data = bQueueSound.peek();
            bQueueSound.clear();
        }
        catch (Exception e){
            Log.d(TAG, "queue error: " + e.getMessage());
        }

        if (data != null) {
            Spectrum spectrum = data.FFT();
            //spectrum.normalize();
            return spectrum.getData();
        }

        return null;
    }

    @Override
    public final void loop() {
        try {
            Thread.sleep(SLEEP_TIME);

            int ret = mContext.mSoundController.mAudioRec.read(buffer, 0, blockSize);

            if (ret > 0) {
                SoundDataBlock data = new SoundDataBlock(buffer, blockSize, ret);
                bQueueSound.put(data);
            }
            else
                Log.v(TAG, "audio recording ret is 0");
        }
        catch (InterruptedException e) {

        }
        catch (Exception e) {
            Log.e(TAG, "error analyzing audio frames: " + e.getMessage());
        }
    }

    public void reset() {

    }
}