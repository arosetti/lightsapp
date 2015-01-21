package com.lightsapp.core.analyzer.light;

import android.content.Context;
import android.util.Log;

import com.lightsapp.core.analyzer.BaseAnalyzer;
import com.lightsapp.core.analyzer.morse.MorseAnalyzer;
import com.lightsapp.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.lightsapp.utils.HandlerUtils.signalStr;


public class LightAnalyzer extends BaseAnalyzer {
    protected final String TAG = LightAnalyzer.class.getSimpleName();

    protected List<Frame> lframes;
    protected List<Frame> lframes_tmp;

    protected final Lock lock_frames_tmp;
    protected final Lock lock_frames;

    protected boolean enable_crop;
    protected int last_frame_analyzed = 0;

    protected long d_max = Long.MIN_VALUE, d_min = Long.MAX_VALUE, d_avg, d_sum = 0;
    protected long l_max = Long.MIN_VALUE, l_min = Long.MAX_VALUE, l_avg, l_sum = 0;
    protected String statusInfo = "";

    protected LightAnalyzer(Context context) {
        super(context);

        mContext = (MainActivity) context;

        lock_frames_tmp = new ReentrantLock(true);
        lock_frames = new ReentrantLock(true);

        lframes = new ArrayList<Frame>();
        lframes_tmp = new ArrayList<Frame>();

        enable_crop = mContext.mPrefs.getBoolean("enable_crop", true);

        mContext.mMorseA = new MorseAnalyzer(context);
    }

    @Override
    public final void loop() {
        try {
            Thread.sleep(sleep_time);
            update();

            if (enable_analyze.get())
                analyze();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            Log.e(TAG, "error analyzing image frames: " + e.getMessage());
            e.printStackTrace();
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
            last_frame_analyzed = 0;
            d_max = Long.MIN_VALUE;
            d_min = Long.MAX_VALUE;
            d_avg = d_sum = 0;
            l_max = Long.MIN_VALUE;
            l_min = Long.MAX_VALUE;
            l_avg = l_sum = 0;
            lock_frames.unlock();
        }

        mContext.mMorseA.reset();
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
                    " image frames to lframes which is " + lframes.size() + " frames big.");
            Frame frame;
            for (int i = 0; i < lframes_swap.size(); i++) {
                frame = lframes_swap.get(i);
                frame.analyze();

                //error analyzing data, maybe error allocating memory.
                // we use the previous frame's luminance.
                if (frame.luminance < 0) {
                    Log.e(TAG, "error analyzing image frame, using prev value if available");
                    if (!lframes.isEmpty())
                        frame.setLuminance(lframes.get(lframes.size() - 1).luminance);
                    else
                        frame.setLuminance(0);
                }
                lframes.add(frame);
            }
            lframes_swap = null;
        }
        catch (Exception e) {
            Log.d(TAG, "error updating image frames: " + e.getMessage());
            e.printStackTrace();
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
                if ((lframes.get(i).delta > 5) && (lframes.get(i).delta < d_min))
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

    private void signalGraph() {
        lock_frames.lock();
        try {
            if(lframes.size() > 0) {
                try {
                    long fps = (d_avg == 0) ? 0 : (1000 / d_avg);
                    setStatusInfo("frames: " + lframes.size() +
                                  "\nfps: " + fps +
                                  "\nlum: " + lframes.get(last_frame_analyzed).luminance);
                }
                catch (Exception e) {
                    Log.d(TAG, "error logging statistics: " + e.getMessage());
                    e.printStackTrace();
                }
                signalStr(mContext.mHandlerInfo, "update", "");
            }
        }
        finally {
            lock_frames.unlock();
        }
    }

    public synchronized void setStatusInfo(String str) {
        statusInfo = str;
    }

    public synchronized String getStatusInfo() {
        return statusInfo;
    }

    public final void addFrame(byte[] data, int width, int height) {
        Frame frame = null;
        long timestamp_now;
        long delta;

        if (!this.getStatus())
            return;

        lock_frames_tmp.lock();
        try {
            timestamp_now = System.currentTimeMillis();
            delta = (timestamp_last == 0)? 0 : (timestamp_now - timestamp_last);
            frame = new Frame(data, width, height, timestamp_now, delta, enable_crop);
            lframes_tmp.add(frame);
            timestamp_last = System.currentTimeMillis();
        }
        catch (Exception e) {
             Log.e(TAG, "error inserting image frame: " + e.getMessage());
             e.printStackTrace();
        }
        lock_frames_tmp.unlock();
    }
}