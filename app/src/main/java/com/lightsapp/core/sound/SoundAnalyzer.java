package com.lightsapp.core.sound;

import android.content.Context;
import android.util.Log;

import com.lightsapp.lightsapp.MainActivity;
import com.lightsapp.utils.MyRunnable;

import java.util.ArrayList;
import java.util.List;

public class SoundAnalyzer extends MyRunnable {
    protected final String TAG = SoundAnalyzer.class.getSimpleName();
    protected String NAME = "SoundAnalyzer";

    protected MainActivity mCtx;

    protected List<Frame> sframes;
    protected List<Frame> sframes_tmp;

    protected final int SLEEP_TIME = 100;

    public SoundAnalyzer(Context context) {
        super(true);

        mCtx = (MainActivity) context;

        sframes = new ArrayList<Frame>();
        sframes_tmp = new ArrayList<Frame>();
    }

    @Override
    public final void loop() {
        try {
            Thread.sleep(SLEEP_TIME);
            //update();

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

    public final List<Frame> getFrames() {
        return sframes;
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