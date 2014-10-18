package com.lightsapp.camera;

import android.hardware.Camera;
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
    private final String TAG = "FrameAnalyzer";
    private Camera mCamera;
    private MorseConverter mMorse;
    private List<Frame> lframes;
    private long timestamp;
    private MyHandler myHandler;

    private long d_max = Long.MIN_VALUE, d_min = Long.MAX_VALUE, d_avg, d_sum = 0;
    private long l_max = Long.MIN_VALUE, l_min = Long.MAX_VALUE, l_avg, l_sum = 0;

    public FrameAnalyzer(Handler handler) {
        super(true);
        lframes = new ArrayList<Frame>();
        myHandler = new MyHandler(handler);
     }

    @Override
    public void loop() {
        d_max = Long.MIN_VALUE;
        d_min = Long.MAX_VALUE;
        d_avg = 0;
        d_sum = 0;
        l_max = Long.MIN_VALUE;
        l_min = Long.MAX_VALUE;
        l_avg = 0;
        l_sum = 0;
        try {
            getFrameStats();
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long getFrameLuminance(byte[] data, int width, int height)
    {
        final int frameSize = width * height;
        long luminance = 0;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) data[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & data[uvp++]) - 128;
                    u = (0xff & data[uvp++]) - 128;
                }

                luminance += (long) y;
            }
        }

        return luminance;
    }

    private void getFrameStats() { // TODO do it incrementally
        if (lframes.isEmpty())
            return;

        l_sum = 0;
        d_sum = 0;

        for(int i = 0; i < lframes.size(); i++) {
            if (lframes.get(i).luminance > l_max)
                l_max = lframes.get(i).luminance;
            if (lframes.get(i).luminance < l_min)
                l_min = lframes.get(i).luminance;
            l_sum += lframes.get(i).luminance;

            if (lframes.get(i).delta > d_max)
                d_max = lframes.get(i).delta;
            if (lframes.get(i).delta < d_min)
                d_min = lframes.get(i).delta;
            d_sum += lframes.get(i).delta;
        }

        l_avg = l_sum / lframes.size();
        d_avg = d_sum / lframes.size();
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

    public void addFrame(byte[] data, int width, int height) {
        long luminance = 0;
        long delta;
        // TODO use a buffer and process in the loop
        luminance = getFrameLuminance(data, width, height);

        if (timestamp != 0)
            delta = (System.currentTimeMillis() - timestamp);
        else
            delta = 0;

        Frame frame = new Frame(delta, luminance);
        lframes.add(frame);

        timestamp = System.currentTimeMillis();
        myHandler.signalStr("message", "frames: " + lframes.size() +
                             "\ncur / min / max / avg" +
                             "\ndelta: (" + delta + " / " +
                             d_min + " / " + d_max + " / " + d_avg + ") ms " +
                             "\nluminance: (" + luminance / 1000 +
                             " / " + l_min/1000 + " / " + l_max/1000 + " / " + l_avg/1000 +") K");

        //logFrame();
    }
}