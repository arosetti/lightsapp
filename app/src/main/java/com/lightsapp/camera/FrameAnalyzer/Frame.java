package com.lightsapp.camera.FrameAnalyzer;

import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.util.Log;

public class Frame {
    private final String TAG = Frame.class.getSimpleName();
    public long delta, timestamp;
    public long luminance = -1;
    private int width,height;
    public byte[] data_raw = null;

    public Frame(byte [] data, int width, int height, long timestamp, long delta) {
        this.data_raw = data;
        this.width = width;
        this.height = height;
        this.timestamp = timestamp;
        this.delta = delta;
    }

    public Frame analyze() {
        if (data_raw != null) {
            YuvImage yuvimage = new YuvImage(data_raw, ImageFormat.NV21, width, height, null);
            luminance = getLuminance(yuvimage.getYuvData(), width, height);
            data_raw = null;
            yuvimage = null;
        }
        return this;
    }

    private long getLuminance(byte[] data, int width, int height) {
        final int frameSize = width * height;
        long ysum = 0;

        try {
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

                    ysum += (long) y;
                }
            }
        }
        catch (Exception e) {
            Log.v(TAG, "error getting luminance");
        }

        return (int) ((float)ysum / (float)frameSize);
    }

    @Override
    public String toString() {
        return "[" + delta + "," + luminance + "]";
    }
}
