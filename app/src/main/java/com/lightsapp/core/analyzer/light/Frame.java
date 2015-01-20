package com.lightsapp.core.analyzer.light;

import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.util.Log;


public class Frame {
    private final String TAG = Frame.class.getSimpleName();

    private byte[] data_raw = null;
    private final int width, height, size, width_half, height_half;

    public long delta, timestamp, luminance = -1;

    private boolean enable_crop;

    public Frame(byte [] data, int width, int height, long timestamp, long delta, boolean crop) {
        this.data_raw = data;
        this.width = width;
        this.width_half = width/2;
        this.height = height;
        this.height_half = height/2;
        size = width * height;
        this.timestamp = timestamp;
        this.delta = delta;
        this.enable_crop = crop;
    }

    public Frame analyze() {
        if (data_raw != null) {
            YuvImage yuvimage;
            try {
                yuvimage = new YuvImage(data_raw, ImageFormat.NV21, width, height, null);
                luminance = getLuminance(yuvimage.getYuvData(), width, height);
            }
            catch (Exception e) {
            }
            data_raw = null;
            yuvimage = null;
        }
        return this;
    }

    public void setLuminance(long l) {
        luminance = (l >= -1) ? l : -1;
    }

    private int dist(int x, int y) {
        return (int) Math.sqrt((width_half - x) * (width_half - x) + (height_half - y) * (height_half - y));
    }

    private int dist_quadratic(int x, int y) {
        return (int) ((width_half - x) * (width_half - x) + (height_half - y) * (height_half - y));
    }

    private long getLuminance(byte[] data, int width, int height) {
        long ysum = 0;

        int max_dist = dist(0, 0);
        int max_dist_quadratic = (max_dist / 8)*(max_dist / 8);

        try {
            for (int j = 0, yp = 0; j < height; j++) {
                int uvp = size + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & ((int) data[yp])) - 16;
                    if (y < 0)
                        y = 0;
                    if ((i & 1) == 0) {
                        //v = (0xff & data[uvp++]) - 128;
                        //u = (0xff & data[uvp++]) - 128;
                        uvp += 2;
                    }

                    if (!enable_crop || (enable_crop && (dist_quadratic(i, j) < max_dist_quadratic)))
                        ysum += (long) y;
                }
            }
        }
        catch (Exception e) {
            Log.v(TAG, "error getting luminance");
            return -1;
        }

        if (enable_crop)
            return (int) ((float)ysum / (Math.PI * max_dist * max_dist / 64));
        else
            return (int) ((float)ysum / (float)size);
    }

    @Override
    public String toString() {
        return "[" + delta + "," + luminance + "]";
    }
}
