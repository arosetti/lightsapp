package com.lightsapp.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lightsapp.core.MyHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraController extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = "CameraController";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private MyHandler myHandler;
    private FrameAnalyzer mFrameA;

    private int PreviewSizeWidth;
    private int PreviewSizeHeight;

    private long timestamp;

    public CameraController(Context context, Camera camera, Handler handler) {
        super(context);
        mCamera = camera;
        myHandler = new MyHandler(handler);

        mFrameA = new FrameAnalyzer(handler);
        mFrameA.start();
        mFrameA.activate();

        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        timestamp = 0;

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters p = camera.getParameters();
        int width = p.getPreviewSize().width;
        int height = p.getPreviewSize().height;

        //TODO ckeck image format
        ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, width, height);
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        //mFrameA.addFrame(yuvimage.getYuvData(), yuvimage.getWidth(), yuvimage.getHeight());
    }

    public void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(this);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            //mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
        catch (Exception e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e) {

        }

        try {
            mCamera.setDisplayOrientation(90);
            Camera.Parameters params = mCamera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            params.setPreviewFrameRate(30);
            params.setPreviewFpsRange(15000, 30000);
            //params.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);
            mCamera.setParameters(params);
        } catch (Exception e){
            Log.d(TAG, "Error starting camera parameters: " + e.getMessage());
        }

        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    private boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private boolean hasFlash(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
}