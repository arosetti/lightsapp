package com.lightsapp.core.light;

import android.content.Context;
import android.util.Log;

import com.lightsapp.utils.MyRunnable;
import com.lightsapp.lightsapp.MainActivity;
import com.lightsapp.core.morse.MorseConverter;
import static com.lightsapp.utils.Utils.*;
import static com.lightsapp.utils.HandlerUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LightAnalyzer extends MyRunnable {
    protected final String TAG = LightAnalyzer.class.getSimpleName();
    protected String NAME = "???";

    protected MainActivity mCtx;

    protected List<Frame> lframes;
    protected List<Frame> lframes_tmp;

    protected final Lock lock_frames_tmp;
    protected final Lock lock_frames;

    protected final int SLEEP_TIME = 100;

    protected int last_frame_analyzed = 0,
                  sensitivity = -1;

    protected final MorseConverter mMorse;
    protected long speed_base;

    private long timestamp_last;
    protected long d_max = Long.MIN_VALUE, d_min = Long.MAX_VALUE, d_avg, d_sum = 0;
    protected long l_max = Long.MIN_VALUE, l_min = Long.MAX_VALUE, l_avg, l_sum = 0;

    protected AtomicReference<Boolean> enable_analyze;

    protected LightAnalyzer(Context context) {
        super(true);

        mCtx = (MainActivity) context;

        lock_frames_tmp = new ReentrantLock(true);
        lock_frames = new ReentrantLock(true);

        lframes = new ArrayList<Frame>();
        lframes_tmp = new ArrayList<Frame>();

        mMorse = new MorseConverter(Integer.parseInt(mCtx.mPrefs.getString("interval", "500")));
        speed_base = mMorse.get("SPEED_BASE");

        enable_analyze = new AtomicReference<Boolean>(false);
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

                //error analyzing data, maybe error allocating memory for the frame.
                // we use the previous frame's luminance.
                if (lframes_swap.get(i).luminance < 0) {
                    Log.v(TAG, "couldn't allocate memory for image frame, using prev value if available");
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

        signalData(ldata);
    }

    protected final void signalData(List<Long> ldata) {
        String str = mMorse.getText(ListToPrimitiveArray(ldata));
        signalStr(mCtx.mHandlerRecv, "data_message_text", str);
        signalStr(mCtx.mHandlerRecv, "data_message_morse", mMorse.getMorse(str) +
                                                           "\n" + ldata.toString());
    }

    private void signalReset() {
        signalStr(mCtx.mHandlerRecv, "data_message_text", "");
        signalStr(mCtx.mHandlerRecv, "data_message_morse", "");
    }

    private void signalGraph() {
        lock_frames.lock();
        try {
            if(lframes.size() > 0) {
                signalStr(mCtx.mHandlerInfo ,"info_message", "frames: " + lframes.size() +
                        "\ncur, min, max, avg" +
                        "\ndelay: (" + lframes.get(last_frame_analyzed).delta + ", " +
                        d_min + ", " + d_max + ", " + d_avg + ") ms " +
                        "\nlum: (" + lframes.get(last_frame_analyzed).luminance +
                        ", " + l_min + ", " + l_max + ", " + l_avg + ")");
                signalStr(mCtx.mHandlerInfo, "update", "");
            }
        }
        finally {
            lock_frames.unlock();
        }
    }

    public final void setSensitivity(int sensitivity) {
        Log.v(TAG, "Sensitivity set to " + sensitivity);
        this.sensitivity = sensitivity;
    }

    public final void addFrame(byte[] data, int width, int height) {
        Frame frame = null;
        long timestamp_now = System.currentTimeMillis() ;
        long delta = (timestamp_last == 0)? 0 : (timestamp_now - timestamp_last);

        lock_frames_tmp.lock();
        try {
            frame = new Frame(data, width, height, timestamp_now, delta);
            if (frame != null) {
                lframes_tmp.add(frame);
                timestamp_last = System.currentTimeMillis();
                //Log.i(TAG, "FRAME SIZE: " + data.length / 1000 + " KByte");
            }
            else {
                Log.w(TAG, "can't allocate frame!!");
            }
        }
        finally {
            lock_frames_tmp.unlock();
        }
    }
}