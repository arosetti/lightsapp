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
    private Camera mCamera;
    private MorseConverter mMorse;
    private List<Frame> lframes;
    private long timestamp;
    private MyHandler myHandler;

    public FrameAnalyzer(Handler handler) {
        super(true);
        lframes = new ArrayList<Frame>();
        myHandler = new MyHandler(handler);
     }

    @Override
    public void main() {
        try {
            Thread.sleep(100);
            getFrameStats();
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

    private void getFrameStats() {
        /* calc max,min,avg */
        long max_lum = Long.MIN_VALUE;
        long min_lum = Long.MAX_VALUE;
        long sum = 0;

        if (lframes.isEmpty())
            return;

        for(int i = 0; i < lframes.size(); i++) {
            if (lframes.get(i).luminance > max_lum)
                max_lum = lframes.get(i).luminance;
            if (lframes.get(i).luminance < min_lum)
                min_lum = lframes.get(i).luminance;
            sum += lframes.get(i).luminance;
        }

        Log.v("CameraTest", "Luminance -> max " + max_lum + " | min = " + min_lum + " | avg " + sum / lframes.size());
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
            Log.e("CameraTest", "Error saving frames");
            return;
        }
    }

    public void addFrame(byte[] data, int width, int height) {
        long luminance = 0;
        long delta;

        luminance = getFrameLuminance(data, width, height);

        if (timestamp != 0)
            delta = (System.currentTimeMillis() - timestamp);
        else
            delta = 0;

        Frame frame = new Frame(delta, luminance);
        lframes.add(frame);

        timestamp = System.currentTimeMillis();
        Log.v("CameraTest", "Frame collected -> Lum = " + luminance + " | TimeDelta = " + delta );
        Log.v("CameraTest", "Frames collected -> " + lframes.size() );
        logFrame();
    }
}