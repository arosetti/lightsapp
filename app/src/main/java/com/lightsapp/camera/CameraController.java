package com.lightsapp.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lightsapp.camera.FrameAnalyzer.*;
import com.lightsapp.lightsapp.MainActivity;

import java.io.IOException;
import java.util.List;

public class CameraController extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = CameraController.class.getSimpleName();

    private MainActivity mCtx;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private FrameAnalyzer mFrameA;

    private int format, width, height;

    public CameraController(Context context) {
        super(context);

        mCtx = (MainActivity) context;
    }

    public final List<Frame> getFrames() {
        return mFrameA.getFrames();
    }

    public void reset() {
        mFrameA.reset();
    }

    public void startAnalyzer() {
        mFrameA.setLoop(true);
        mFrameA.start();
        mFrameA.activate();
    }

    public void stopAnalyzer() {
        mFrameA.stop();
    }

    public void setSensitivity(int sensitivity) { mFrameA.setSensitivity(sensitivity); }

    public Camera setup() {
        boolean err = false;
        boolean hasCamera = mCtx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        boolean hasFrontCamera = mCtx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

        try {
            if (hasCamera) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            }
            else if (hasFrontCamera) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
        }
        catch(Exception e) {
            err = true;
        }

        if(err) {
            try {
                mCamera = Camera.open(0);
            }
            catch(Exception e) {
            }
        }

        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        int algorithm = Integer.parseInt(mCtx.mPrefs.getString("algorithm", "2"));
        switch (algorithm) {
            case 1:
                mFrameA = new ThresholdFrameAnalyzer(mCtx);
                break;
            case 2:
                mFrameA = new DerivativeFrameAnalyzer(mCtx);
                break;
            default:
                mFrameA = new BasicFrameAnalyzer(mCtx);
        }

        return mCamera;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (format == ImageFormat.NV21) {
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
            this.format = params.getPreviewFormat();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            params.setRecordingHint(true);
            params.setAutoExposureLock(false);
            params.setAutoWhiteBalanceLock(true);
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