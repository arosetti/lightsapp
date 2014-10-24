package com.lightsapp.camera.FrameAnalyzer;

import android.content.Context;
import android.util.Log;

import com.lightsapp.core.MyHandler;
import com.lightsapp.core.MyRunnable;
import com.lightsapp.lightsapp.MainActivity;
import com.lightsapp.morse.MorseConverter;
import static com.lightsapp.utils.Utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FrameAnalyzer extends MyRunnable {
    protected final String TAG = FrameAnalyzer.class.getSimpleName();

    protected MainActivity mCtx;
    protected MyHandler myHandler;

    protected List<Frame> lframes;
    protected List<Frame> lframes_tmp;

    protected final Lock lock_frames_tmp;
    protected final Lock lock_frames;

    protected int start_frame = 0,
                  last_frame_analyzed = 0,
                  sensitivity = 0;

    protected final MorseConverter mMorse;
    protected long speed_base;

    private long timestamp;
    protected long d_max = Long.MIN_VALUE, d_min = Long.MAX_VALUE, d_avg, d_sum = 0;
    protected long l_max = Long.MIN_VALUE, l_min = Long.MAX_VALUE, l_avg, l_sum = 0;

    protected FrameAnalyzer(Context context) {
        super(true);

        mCtx = (MainActivity) context;

        lock_frames_tmp = new ReentrantLock(true);
        lock_frames = new ReentrantLock(true);

        lframes = new ArrayList<Frame>();
        lframes_tmp = new ArrayList<Frame>();
        myHandler = new MyHandler(mCtx.mHandlerRecv, TAG);

        mMorse = new MorseConverter(Integer.parseInt(mCtx.mPrefs.getString("interval", "500")));
        speed_base = mMorse.get("SPEED_BASE");
    }

    @Override
    public final void loop() {
        try {
            if (myHandler.isHandlerNull()) {
                myHandler.setHandler(mCtx.mHandlerRecv);
            }

            Thread.sleep(100);
            update();

            Thread.sleep(100);
            analyze();

            Thread.sleep(200);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            Log.e(TAG, "error analyzing frames: " + e.getMessage());
        }
        finally {
            myHandler.signalStr(mCtx.mHandlerGraph ,"info_message", "frames: " + lframes.size() +
                    "\ncur / min / max / avg" +
                    "\ndelta: (" + lframes.get(last_frame_analyzed).delta + " / " +
                    d_min + " / " + d_max + " / " + d_avg + ") ms " +
                    "\nluminance: (" + lframes.get(last_frame_analyzed).luminance +
                    " / " + l_min + " / " + l_max + " / " + l_avg + ")");
            myHandler.signalStr(mCtx.mHandlerGraph, "update", "");
        }
    }

    public final List<Frame> getFrames() {
        return lframes;
    }

    public final void reset() {
        myHandler.signalStr("data_message", "***");

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

        start_frame = 0;
        last_frame_analyzed = 0;
    }

    protected void update() {
        List<Frame> lframes_swap;

        lock_frames_tmp.lock();
        try {
            lframes_swap = lframes_tmp;
            lframes_tmp = new ArrayList<Frame>();
        }
        finally {
            lock_frames_tmp.unlock();
        }

        lock_frames.lock();
        try {
            for (int i = 0; i < lframes_swap.size(); i++) {
                lframes_swap.get(i).analyze();
                lframes.add(lframes_swap.get(i));
            }
            lframes_swap = null;
            last_frame_analyzed = lframes.size() - 1;
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

            l_sum = 0;
            d_sum = 0;

            for (int i = last_frame_analyzed; i < lframes.size(); i++) {

                l_sum += lframes.get(i).luminance;
                d_sum += lframes.get(i).delta;

                if (i < start_frame)
                    return;

                if (lframes.get(i).luminance > l_max)
                    l_max = lframes.get(i).luminance;
                if (lframes.get(i).luminance < l_min)
                    l_min = lframes.get(i).luminance;

                if (lframes.get(i).delta > d_max)
                    d_max = lframes.get(i).delta;
                if (lframes.get(i).delta < d_min)
                    d_min = lframes.get(i).delta;
            }

            l_avg = l_sum / lframes.size();
            d_avg = d_sum / lframes.size();
        }
        finally {
            lock_frames.unlock();
        }
    }

    protected void analyze() {

    }

    // approximate to morse values and generate long[]
    protected final void endAnalyze(List<Long> ldata) {
        long dbase, dlong, dvlong, sign, closest;

        if (ldata.size() == 0)
            return;

        for (int i = 0; i < ldata.size(); i++) {
            dbase = Math.abs(Math.abs(ldata.get(i)) - speed_base);
            dlong = Math.abs(Math.abs(ldata.get(i)) - 3 * speed_base);
            dvlong = Math.abs(Math.abs(ldata.get(i)) - 7 * speed_base);

            // approximate to the closest value
            closest = Math.min(Math.min(dbase, dlong), dvlong);
            sign = (ldata.get(i) > 0)? 1:-1;

            if (closest == dbase) {
                ldata.set(i, sign * speed_base);
            } else if (closest == dlong) {
                ldata.set(i, 3 * sign * speed_base);
            } else if (closest == dvlong) {
                ldata.set(i, 7 * sign * speed_base);
            }
        }

        signalToGui(ldata);
    }

    protected final void signalToGui(List<Long> ldata) {
        myHandler.signalStr("data_message", mMorse.getText(ListToPrimitiveArray(ldata)) +
                                            "\n" + ldata.toString());
    }

    public final void setSensitivity(int sensitivity) {
        Log.v(TAG, "Sensitivity set to " + sensitivity);
        this.sensitivity=sensitivity;
    }

    public final void addFrame(byte[] data, int width, int height) {
        long timestamp_now = System.currentTimeMillis() ;
        long delta = (timestamp == 0)? 0 : (timestamp_now - timestamp);

        lock_frames_tmp.lock();
        try {
            lframes_tmp.add(new Frame(data, width, height, timestamp_now, delta));
        }
        finally {
            lock_frames_tmp.unlock();
        }

        timestamp = System.currentTimeMillis();
    }
}