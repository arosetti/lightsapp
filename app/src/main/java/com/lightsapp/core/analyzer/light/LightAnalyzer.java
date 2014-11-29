package com.lightsapp.core.analyzer.light;

import android.content.Context;
import android.util.Log;

import com.lightsapp.core.analyzer.morse.MorseAnalyzer;
import com.lightsapp.ui.MainActivity;
import com.lightsapp.utils.MyRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.lightsapp.utils.HandlerUtils.signalStr;

public class LightAnalyzer extends MyRunnable {
    protected final String TAG = LightAnalyzer.class.getSimpleName();
    protected String NAME = "???";

    protected MainActivity mContext;

    protected List<Frame> lframes;
    protected List<Frame> lframes_tmp;

    protected final Lock lock_frames_tmp;
    protected final Lock lock_frames;

    protected AtomicReference<Boolean> enable_analyze;
    protected final int SLEEP_TIME = 200;

    protected int last_frame_analyzed = 0,
                  sensitivity = -1;

    private long timestamp_last;
    protected long d_max = Long.MIN_VALUE, d_min = Long.MAX_VALUE, d_avg, d_sum = 0;
    protected long l_max = Long.MIN_VALUE, l_min = Long.MAX_VALUE, l_avg, l_sum = 0;
    protected String statusInfo;

    protected LightAnalyzer(Context context) {
        super(true);

        mContext = (MainActivity) context;

        lock_frames_tmp = new ReentrantLock(true);
        lock_frames = new ReentrantLock(true);

        lframes = new ArrayList<Frame>();
        lframes_tmp = new ArrayList<Frame>();

        enable_analyze = new AtomicReference<Boolean>(false);

        mContext.mMorseA = new MorseAnalyzer(context);
    }

    public final String getName() {
        return NAME;
    }

    public void setAnalyzer(boolean val) {
        enable_analyze.getAndSet(val);
    }


    public boolean getAnalyzer() {
        return enable_analyze.get();
    }

    @Override
    public final void loop() {
        try {
            Thread.sleep(SLEEP_TIME);
            update();

            Thread.sleep(SLEEP_TIME);
            if (enable_analyze.get())
                analyze();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            Log.e(TAG, "error analyzing image frames: " + e.getMessage());
        }
        finally {
            signalGraph();
        }
    }

    public final List<Frame> getFrames() {
        return lframes;
    }

    public void reset() {
        lock_frames_tmp.lock();
        try {
            lframes_tmp.clear();
        }
        finally {
            lock_frames_tmp.unlock();
        }

        lock_frames.lock();
        try {
            lframes.clear();
        }
        finally {
            lock_frames.unlock();
        }

        last_frame_analyzed = 0;
    }

    protected void update() {
        int size;
        List<Frame> lframes_swap = null;

        lock_frames_tmp.lock();
        try {
            size = lframes_tmp.size();
            if (size != 0) {
                lframes_swap = lframes_tmp;
                lframes_tmp = new ArrayList<Frame>();
            }
        }
        finally {
            lock_frames_tmp.unlock();
        }

        if (size == 0)
            return;

        lock_frames.lock();
        try {
            Log.v(TAG, "Swapping " + lframes_swap.size() +
                    " image frames to lframes which is big " + lframes.size() + " frames.");
            for (int i = 0; i < lframes_swap.size(); i++) {
                lframes.add(lframes_swap.get(i).analyze());

                //error analyzing data, maybe error allocating memory.
                // we use the previous frame's luminance.
                if (lframes_swap.get(i).luminance < 0) {
                    Log.e(TAG, "error analyzing image frame, using prev value if available");
                    if (i > 0)
                        lframes_swap.get(i).setLuminance(lframes_swap.get(i - 1).luminance);
                    else
                        lframes_swap.get(i).setLuminance(0);
                }
            }
            lframes_swap = null;
        }
        finally {
            lock_frames.unlock();
        }

        frameStats();
    }

    protected final void frameStats() {
        lock_frames.lock();
        try {
            if (lframes.isEmpty())
                return;

            for (int i = last_frame_analyzed; i < lframes.size(); i++) {

                l_sum += lframes.get(i).luminance;
                d_sum += lframes.get(i).delta;

                if (lframes.get(i).luminance > l_max)
                    l_max = lframes.get(i).luminance;
                if (lframes.get(i).luminance < l_min)
                    l_min = lframes.get(i).luminance;

                if (lframes.get(i).delta > d_max)
                    d_max = lframes.get(i).delta;
                if ((lframes.get(i).delta > 0) && (lframes.get(i).delta < d_min))
                    d_min = lframes.get(i).delta;
            }

            l_avg = l_sum / lframes.size();
            d_avg = d_sum / lframes.size();

            last_frame_analyzed = lframes.size() - 1;
        }
        finally {
            lock_frames.unlock();
        }
    }

    // to be overridden
    protected void analyze() {

    }

    private void signalGraph() {
        lock_frames.lock();
        try {
            if(lframes.size() > 0) {
                setStatusInfo("frames: " + lframes.size() +
                              "\ncur, min, max, avg" +
                              "\ndelay: (" + lframes.get(last_frame_analyzed).delta + ", " +
                              d_min + ", " + d_max + ", " + d_avg + ") ms " +
                              "\nlum: (" + lframes.get(last_frame_analyzed).luminance +
                              ", " + l_min + ", " + l_max + ", " + l_avg + ")");
                signalStr(mContext.mHandlerInfo, "update", "");
            }
        }
        finally {
            lock_frames.unlock();
        }
    }

    public synchronized  void setStatusInfo(String str) {
        statusInfo = str;
    }

    public synchronized String getStatusInfo() {
        return statusInfo;
    }

    public final void setSensitivity(int sensitivity) {
        Log.v(TAG, "Sensitivity set to " + sensitivity);
        this.sensitivity = sensitivity;
    }

    public final void addFrame(byte[] data, int width, int height) {
        Frame frame = null;
        long timestamp_now;
        long delta;

        lock_frames_tmp.lock();
        try {
            timestamp_now = System.currentTimeMillis();
            delta = (timestamp_last == 0)? 0 : (timestamp_now - timestamp_last);
            frame = new Frame(data, width, height, timestamp_now, delta);
            lframes_tmp.add(frame);
            timestamp_last = System.currentTimeMillis();
            //Log.v(TAG, "FRAME SIZE: " + data.length / 1000 + " KByte");
        }
        catch (Exception e) {
             Log.e(TAG, "error inserting image frame: " + e.getMessage());
        }
        lock_frames_tmp.unlock();
    }
}