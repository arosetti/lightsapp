package com.lightsapp.camera;

import android.graphics.ImageFormat;
import android.graphics.YuvImage;

public class Frame {
    private final String TAG = Frame.class.getSimpleName();
    public long delta;
    public long luminance = -1;
    private int width,height;
    public byte[] data_raw = null;

    public Frame(byte [] data, int width, int height, long delta) {
        this.data_raw = data;
        this.width = width;
        this.height = height;
        this.delta = delta;
    }

    public void analyze() {
        YuvImage yuvimage = new YuvImage(data_raw, ImageFormat.NV21, width, height, null);
        luminance = getLuminance(yuvimage.getYuvData(), width, height);
        data_raw = null;
        yuvimage = null;
    }

    private long getLuminance(byte[] data, int width, int height) {
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
    @Override
    public String toString() {
        return "[" + delta + "," + luminance + "]";
    }
}
