package com.lightsapp.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lightsapp.core.light.*;
import com.lightsapp.lightsapp.MainActivity;

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
        width = 320;
        height = 240;
        mCtx = (MainActivity) context;
    }

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

    public String getInfo() {
        return width + "x" + height + "@[" + fps_min /1000 + "-" + fps_max/1000 + "]fps";
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
        try {
            setWillNotDraw(false);
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
        }
        catch (Exception e) {
        }

        try {
            mCamera.setDisplayOrientation(90);
            Camera.Parameters params = mCamera.getParameters();

            this.format = params.getPreviewFormat();

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            width = params.getPreviewSize().width;
            height = params.getPreviewSize().height;

            params.setPictureSize(width, height);
            params.setPreviewFormat(ImageFormat.NV21);
            params.setRecordingHint(true);
            params.setAutoExposureLock(true);
            params.setAutoWhiteBalanceLock(true);

            List<int[]>  fpsRange = params.getSupportedPreviewFpsRange();
            if (fpsRange != null && !fpsRange.isEmpty()) {
                fps_min = fpsRange.get(fpsRange.size() - 1)[params.PREVIEW_FPS_MIN_INDEX];
                fps_max = fpsRange.get(fpsRange.size() - 1)[params.PREVIEW_FPS_MAX_INDEX];

            }
            else {
                fps_min = 29000;
                fps_max = 29000;
            }
            params.setPreviewFpsRange(fps_min, fps_max);
            params.setPreviewSize(width, height);
            mCamera.setParameters(params);
        }
        catch (Exception e) {
            Log.d(TAG, "Error starting camera parameters: " + e.getMessage());
        }

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

            canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 20, paint);

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
            Log.e(TAG, "DRAW ERROR: " + e.getMessage());
        }
    }
}