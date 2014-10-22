package com.lightsapp.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lightsapp.core.MyHandler;

import java.io.IOException;
import java.util.List;

public class CameraController extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = "CameraController";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private MyHandler myHandler;
    private FrameAnalyzer mFrameA;

    private int imageFormat;
    private int width;
    private int height;

    public CameraController(Context context, Camera camera, Handler handler, int speed) {
        super(context);

        mCamera = camera;
        myHandler = new MyHandler(handler);

        mFrameA = new FrameAnalyzer(handler, speed);
        startAnalyzer();

        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void startAnalyzer() {
        mFrameA.start();
        mFrameA.activate();
    }

    public final List<Frame> getFrames() {
        return mFrameA.getFrames();
    }

    public void reset() {
        mFrameA.reset();
    }

    public void stopAnalyzer() {
        mFrameA.stop();
    }

    public void setSensitivity(int sensitivity) { mFrameA.setSensitivity(sensitivity); }

    // TODO send data to mFrame more efficently
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (imageFormat == ImageFormat.NV21) {
            mFrameA.addFrame(data, width, height);
        }
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
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
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
            width = params.getPreviewSize().width;
            height = params.getPreviewSize().height;
            params.setPreviewFormat(ImageFormat.NV21);
            imageFormat = params.getPreviewFormat();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            params.setRecordingHint(true);
            params.setAutoExposureLock(false);
            params.setAutoWhiteBalanceLock(false);

            params.setPreviewFrameRate(30);
            params.setPreviewFpsRange(15000, 30000);
            //params.setPreviewSize(width, height);
            mCamera.setParameters(params);
        } catch (Exception e) {
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
}