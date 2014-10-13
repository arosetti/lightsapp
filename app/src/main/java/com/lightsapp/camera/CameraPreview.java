package com.lightsapp.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private long timestamp;
    private List<LFrame> lframes;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        timestamp = System.currentTimeMillis();
        lframes = new ArrayList<LFrame>();
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
        delta = (System.currentTimeMillis() - timestamp);
        LFrame lf = new LFrame(delta, luminance);
        lframes.add(lf);

        timestamp = System.currentTimeMillis();
        Log.v("CameraTest", "Frame collected -> Lum = " + luminance + " | Delta = " + delta );
        Log.v("CameraTest", "Frames collected -> " + lframes.size() );

        /*
        FileOutputStream out;
        try {
            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            out = new FileOutputStream(baseDir + File.separator + "data.png");
            yuvimage.compressToJpeg(rect, 90, out);
        } catch (Exception e) {
            Log.e("CameraTest", "Error saving image");
            return;
        }
        */
        saveLFrames();
    }

    private void saveLFrames(){
        try {
            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            FileWriter writer = new FileWriter(baseDir + File.separator + "data.txt");
            writer.write(lframes.toString());
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