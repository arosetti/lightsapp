package com.lightsapp.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CameraController extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Handler mHandler;

    private int PreviewSizeWidth;
    private int PreviewSizeHeight;

    private long timestamp;
    private List<Frame> lframes;

    public CameraController(Context context, Camera camera, Handler handler) {
        super(context);
        mCamera = camera;
        mHandler = handler;
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        timestamp = 0;
        lframes = new ArrayList<Frame>();
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

    private long totalLuminance(byte[] data, int width, int height)
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

    private void signalUI(String str) {
        if (!str.equals(null) && !str.equals("")) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString("message", str);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    private void frameStats() {
        /* calc max,min,avg */
        long max_lum = Long.MIN_VALUE;
        long min_lum = Long.MAX_VALUE;
        long sum = 0;

        for(int i = 0; i < lframes.size(); i++) {
            if (lframes.get(i).luminance > max_lum)
                max_lum = lframes.get(i).luminance;
            if (lframes.get(i).luminance < min_lum)
                min_lum = lframes.get(i).luminance;
            sum += lframes.get(i).luminance;
        }

        if (min_lum == 0)
            signalUI("lum is zero");

        Log.v("CameraTest", "Luminance -> max " + max_lum + " | min = " + min_lum + " | avg " + sum / lframes.size());
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters p = camera.getParameters();
        int width = p.getPreviewSize().width;
        int height = p.getPreviewSize().height;
        long luminance = 0;
        long delta;

        ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, width, height);
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);

        luminance = totalLuminance(yuvimage.getYuvData(), yuvimage.getWidth(), yuvimage.getHeight());

        if (timestamp != 0)
            delta = (System.currentTimeMillis() - timestamp);
        else
            delta = 0;

        Frame frame = new Frame(delta, luminance);
        lframes.add(frame);

        timestamp = System.currentTimeMillis();
        Log.v("CameraTest", "Frame collected -> Lum = " + luminance + " | TimeDelta = " + delta );
        Log.v("CameraTest", "Frames collected -> " + lframes.size() );
        frameStats();
        logFrame();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            //mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            //mCamera.release();
            //mCamera = null;
            Log.d("CameraPreview", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        //mCamera.setPreviewCallback(null);
        //mCamera.stopPreview();
        //mCamera.release();
        //mCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e){

        }

        mCamera.setDisplayOrientation(90);
        Camera.Parameters params = mCamera.getParameters();
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
        {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        params.setPreviewFrameRate(30);
        params.setPreviewFpsRange(15000,30000);
        //params.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);
        mCamera.setParameters(params);

        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d("CameraPreview", "Error starting camera preview: " + e.getMessage());
        }
    }

    // TODO move to checking class
    private boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // TODO move to checking class
    private boolean hasFlash(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
}