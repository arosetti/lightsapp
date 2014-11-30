package com.lightsapp.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lightsapp.core.analyzer.light.*;
import com.lightsapp.ui.MainActivity;

import java.io.IOException;
import java.util.List;

public class CameraController extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = CameraController.class.getSimpleName();

    private MainActivity mCtx;
    private SurfaceHolder mHolder;
    private Camera mCamera = null;

    private int format, width, height, fps_min, fps_max;

    public CameraController(Context context) {
        super(context);
        width = 0;
        height = 0;
        mCtx = (MainActivity) context;
    }

    public Camera setup() {
        boolean err = false;
        boolean hasCamera = mCtx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        boolean hasFrontCamera = mCtx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

        mCamera = null;
        try {
            if (hasCamera) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            }
            else if (hasFrontCamera) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
        }
        catch(Exception e) {
            Log.e(TAG, "camera open failed: " + e.getMessage());
            err = true;
        }

        if (err) {
            try {
                mCamera = Camera.open(0);
            }
            catch(Exception e) {
                Log.e(TAG, "camera open failed: " + e.getMessage());
            }
        }

        if (mCamera != null) {
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            int algorithm = Integer.parseInt(mCtx.mPrefs.getString("algorithm", "2"));
            switch (algorithm) {
                case 1:
                    mCtx.mLightA = new ThresholdLightAnalyzer(mCtx);
                    break;
                case 2:
                    mCtx.mLightA = new DerivativeLightAnalyzer(mCtx);
                    break;
                default:
                    mCtx.mLightA = new BasicLightAnalyzer(mCtx);
            }
        }

        return mCamera;
    }

    public Camera getCamera() { return mCamera; }

    public boolean isCameraNull() { return mCamera == null; }

    public String getInfo() {
        return width + "x" + height + "@[" + fps_min /1000 + "-" + fps_max/1000 + "]fps";
    }

    private void setCameraParameters() {
        try {
            mCamera.setDisplayOrientation(90);
            Camera.Parameters params = mCamera.getParameters();

            /* size */
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            Camera.Size current_size = null;

            for(int i=0; i<sizes.size(); i++) {
                Camera.Size size = sizes.get(i);
                Log.d(TAG, "Supported size: " + size.width + ", " + size.height);
                if( current_size == null || size.width < current_size.width || (size.width == current_size.width && size.height < current_size.height) ) {
                    current_size = size;
                }
            }
            if( current_size != null ) {
                Log.d(TAG, "Current size: " + current_size.width + ", " + current_size.height);
                params.setPictureSize(current_size.width, current_size.height);
                mCamera.setParameters(params);
            }
            width = params.getPreviewSize().width;
            height = params.getPreviewSize().height;

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            params.setRecordingHint(true);
            params.setAutoExposureLock(true);
            params.setAutoWhiteBalanceLock(true);

            params.setPreviewFormat(ImageFormat.NV21);
            this.format = params.getPreviewFormat();

            /* fps */
            List<int[]>  fpsRange = params.getSupportedPreviewFpsRange();
            if (fpsRange != null && !fpsRange.isEmpty()) {
                fps_min = fpsRange.get(fpsRange.size() - 1)[params.PREVIEW_FPS_MIN_INDEX];
                fps_max = fpsRange.get(fpsRange.size() - 1)[params.PREVIEW_FPS_MAX_INDEX];
                Log.d(TAG, "Supported fps: " + fps_min/1000 + "-" + fps_max/1000 + "fps");
            }
            else {
                fps_min = 29000;
                fps_max = 29000;
            }
            params.setPreviewFpsRange(fps_min, fps_max);

            mCamera.setParameters(params);
        }
        catch (Exception e) {
            Log.d(TAG, "Error starting camera parameters: " + e.getMessage());
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (format == ImageFormat.NV21) {
            mCtx.mLightA.addFrame(data, width, height);
        }
        else {
            Log.e(TAG, "wrong image format");
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
        Log.d(TAG, "surfaceCreated()");
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        }
        catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            e.printStackTrace();
        }

        setCameraParameters();
        setWillNotDraw(false);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed()");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mCamera.setPreviewCallback(null);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged() " + w + ", " + h);

        if (mHolder.getSurface() == null || mCamera == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        }
        catch (Exception e) {
        }

        setCameraParameters();

        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        }
        catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        Log.i(TAG, getInfo());
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawUI(canvas);
    }

    public void update()
    {
        postInvalidate();
    }

    public void drawUI(Canvas canvas) {
        try {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setAlpha(200);
            paint.setAntiAlias(true);
            paint.setColor(Color.GREEN);

            canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 28, paint);

            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(13);

            String lines[] = mCtx.mLightA.getStatusInfo().split("\\r?\\n");
            int i = 0;
            for ( String line: lines ) {
                canvas.drawText(line, canvas.getWidth()/2, 12 + i * 12, paint);
                i++;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Draw Error: " + e.getMessage());
        }
    }
}