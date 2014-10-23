package com.lightsapp.camera.FrameAnalyzer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.lightsapp.core.MyHandler;
import com.lightsapp.core.MyRunnable;
import com.lightsapp.lightsapp.MainActivity;
import com.lightsapp.morse.MorseConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FrameAnalyzer extends MyRunnable {
    protected final String TAG = FrameAnalyzer.class.getSimpleName();

    protected MyHandler myHandler;
    protected MainActivity mCtx;

    protected MorseConverter mMorse;
    protected List<Frame> lframes;
    protected List<Frame> ltmp_frames;
    private long timestamp;

    protected int start_frame = 0,
                  last_frame_analyzed = 0,
                  sensitivity = 0;

    protected final Lock lock_tmp_frames;

    protected String str = "";

    protected long d_max = Long.MIN_VALUE, d_min = Long.MAX_VALUE, d_avg, d_sum = 0;
    protected long l_max = Long.MIN_VALUE, l_min = Long.MAX_VALUE, l_avg, l_sum = 0;

    protected FrameAnalyzer(Context context) {
        super(true);

        mCtx = (MainActivity) context;

        lock_tmp_frames = new ReentrantLock(true);
        lframes = new ArrayList<Frame>();
        ltmp_frames = new ArrayList<Frame>();
        myHandler = new MyHandler(mCtx.mHandlerRecv, TAG);

        mMorse = new MorseConverter(Integer.parseInt(mCtx.mPrefs.getString("interval", "500")));
    }

    @Override
    public final void loop() {
        try {

            if (myHandler.isHandlerNull()) {
                myHandler.setHandler(mCtx.mHandlerRecv);
            }

            update();
            analyze();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (Exception e){
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
        lframes.clear();
        lock_tmp_frames.lock();
        ltmp_frames.clear();
        lock_tmp_frames.unlock();

        start_frame = 0;
        last_frame_analyzed = 0;
        str = "";
    }

    protected void update() {
        List<Frame> lswap_frames;
        lock_tmp_frames.lock();
        try {
            lswap_frames = ltmp_frames;
            ltmp_frames = new ArrayList<Frame>();
        }
        finally {
            lock_tmp_frames.unlock();
        }

        for (int i = 0; i < lswap_frames.size(); i++) {
            lswap_frames.get(i).analyze();
            lframes.add(lswap_frames.get(i));
        }
        lswap_frames = null;
        last_frame_analyzed = lframes.size() - 1;
        frameStats();
    }

    // TODO mutex on lframes
    protected final void frameStats() {
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

    // TODO make it global.
    private long[] ListToPrimitiveArray(List<Long> input) {
        long output[] = new long[input.size()];
        int index = 0;
        for(Long val : input) {
            output[index] =  val;
            index++;
        }
        return output;
    }

    protected void analyze() {

    }

    protected final void endAnalyze(List<Long> ldata) {
        // approximate to morse values and generate long[]
        long base = mMorse.get("SPEED_BASE");
        long dbase, dlong, dvlong;

        if (ldata.size() == 0)
            return;

        for (int i = 0; i < ldata.size(); i++) {
            dbase = Math.abs(Math.abs(ldata.get(i)) - base);
            dlong = Math.abs(Math.abs(ldata.get(i)) - 3 * base);
            dvlong = Math.abs(Math.abs(ldata.get(i)) - 7 * base);

            // approximate to the closest value
            long closest_val = Math.min(Math.min(dbase, dlong), dvlong);
            long sign = (ldata.get(i) > 0)? 1:-1;

            if (closest_val == dbase) {
                ldata.set(i, sign * base);
            } else if (closest_val == dlong) {
                ldata.set(i, 3 * sign * base);
            } else if (closest_val == dvlong) {
                ldata.set(i, 7 * sign * base);
            }
        }

        signalToGui(ldata);
    }

    protected final void signalToGui(List<Long> ldata) {
        myHandler.signalStr("data_message", mMorse.getText(ListToPrimitiveArray(ldata)) +
                                            "\nmorse : " + ldata.toString());
    }

    public final void setSensitivity(int sensitivity) {
        Log.v(TAG, "Sensitivity set to " + sensitivity);
        this.sensitivity=sensitivity;
    }

    public final void addFrame(byte[] data, int width, int height) {
        long delta = (timestamp == 0)? 0 : (System.currentTimeMillis() - timestamp);

        lock_tmp_frames.lock();
        try {
            ltmp_frames.add(new Frame(data, width, height, delta));
        }
        finally {
            lock_tmp_frames.unlock();
        }

        timestamp = System.currentTimeMillis();
    }
}