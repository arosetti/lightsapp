package com.lightsapp.camera;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.lightsapp.core.MyHandler;
import com.lightsapp.core.MyRunnable;
import com.lightsapp.morse.MorseConverter;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class FrameAnalyzer extends MyRunnable {
    private final String TAG = FrameAnalyzer.class.getSimpleName();

    private MyHandler myHandler;

    private MorseConverter mMorse;
    private List<Frame> lframes;
    private long timestamp;
    private int start_frame = 0;
    private int last_frame_analyzed = 0;

    private long d_max = Long.MIN_VALUE, d_min = Long.MAX_VALUE, d_avg, d_sum = 0;
    private long l_max = Long.MIN_VALUE, l_min = Long.MAX_VALUE, l_avg, l_sum = 0;

    public FrameAnalyzer(Handler handler, int speed) {
        super(true);
        lframes = new ArrayList<Frame>();
        myHandler = new MyHandler(handler);
        mMorse = new MorseConverter(speed);
    }

    @Override
    public void loop() {

        try {
            Frame frm;
            for (int i = last_frame_analyzed; i < lframes.size(); i++) {
                frm = lframes.get(i);
                if (frm.luminance < 0) {
                    frm.analyze();
                    last_frame_analyzed = i;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error analyzing frames: " + e.getMessage());
        }

        try {
            getFrameStats();
            analyze();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        myHandler.signalStr("update", "");
    }

    public final List<Frame> getFrames() {
        return lframes;
    }

    public final void reset() {
        myHandler.signalStr("data_message", "***");
        lframes.clear();
        start_frame = 0;
        last_frame_analyzed = 0;
    }

    private void getFrameStats() { // TODO do it incrementally
        if (lframes.isEmpty())
            return;

        l_sum = 0;
        d_sum = 0;

        for (int i = start_frame; i < lframes.size(); i++) {

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

    private void analyze() {
        // exit we do not have enough frames
        if ((lframes.size() - start_frame) < 2)
            return;

        // search for a possible start of the transmission
        if (start_frame == 0) {
            for (int i = start_frame; i < (lframes.size() - 1); i++) {
                if ((lframes.get(i).luminance * 2) < lframes.get(i + 1).luminance) {
                    start_frame = i;
                    myHandler.signalStr("data_message", "start_frame: " + start_frame);
                }
            }
            return;
        }

        int dsum = 0;
        List<Long> ldata = new ArrayList<Long>();

        for (int i = start_frame; i < (lframes.size() - 1); i++) {
            long lcur = lframes.get(i).luminance;
            long lnext = lframes.get(i + 1).luminance;
            long ldiff = Math.abs(lcur - lnext);

            // add to counter if signal does not change too much
            // add new element list on change and reset counter.
            if (ldiff < (lcur / 3)) {
                dsum += lframes.get(i).delta;
            } else { /*else if (ldiff > lcur) {*/
                dsum += lframes.get(i).delta;
                ldata.add(new Long(dsum));
                dsum = 0;
            }
            /*else {
                // ABORT: not a morse signal?
                //start_frame = 0;
            }*/

        }

        // approximate to morse values and generate long[]
        // TODO should be a morse function.
        long base = mMorse.get("GAP");
        long dbase, dlong, dvlong;

        for (int i = 0; i < ldata.size(); i++) {

            // remove wrong small values ( short glitches )
            if (ldata.get(i) < base / 2)
                ldata.remove(i);

            // abort if values are too high
            if (ldata.get(i) > (8*base)) {
                reset();
                return;
            }

            dbase = Math.abs(ldata.get(i) - base);
            dlong = Math.abs(ldata.get(i) - 3 * base);
            dvlong = Math.abs(ldata.get(i) - 7 * base);

            // approximate to the closest value
            long dd = Math.min(Math.min(dbase, dlong), dvlong);

            if (dd == dbase) {
                ldata.set(i, base);
            } else if (dd == dlong) {
                ldata.set(i, 3 * base);
            } else if (dd == dvlong) {
                ldata.set(i, 7 * base);
            }
        }

        // translate morse array to string
        long data[] = new long[ldata.size()];
        for (int i = 0; i < ldata.size(); i++)
            data[i] = ldata.get(i);
        String str = mMorse.getText(data);
        myHandler.signalStr("data_message", "str: " + str + "\nmorse : " + ldata.toString());
    }

    private void logFrame() {
        try {
            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            FileWriter writer = new FileWriter(baseDir + File.separator + "data.txt", true);
            if (lframes != null && !lframes.isEmpty()) {
                writer.write(lframes.get(lframes.size() - 1).toString() + "\n");
            }
            writer.close();

        } catch (Exception e) {
            Log.e(TAG, "Error saving frames");
            return;
        }
    }

    // TODO use a buffer and process in the loop, except for timestamp
    public void addFrame(byte[] data, int width, int height) {
        long delta;

        if (timestamp != 0)
            delta = (System.currentTimeMillis() - timestamp);
        else
            delta = 0;

        Frame frame = new Frame(data, width, height, delta);
        lframes.add(frame);

        timestamp = System.currentTimeMillis();
        myHandler.signalStr("info_message", "frames: " + lframes.size() +
                "\ncur / min / max / avg" +
                "\ndelta: (" + delta + " / " +
                d_min + " / " + d_max + " / " + d_avg + ") ms " +
                "\nluminance: (" + lframes.get(last_frame_analyzed).luminance / 1000 +
                " / " + l_min / 1000 + " / " + l_max / 1000 + " / " + l_avg / 1000 + ") K");
    }
}