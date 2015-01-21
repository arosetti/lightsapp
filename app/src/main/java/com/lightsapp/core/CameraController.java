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

import com.lightsapp.core.analyzer.light.BasicLightAnalyzer;
import com.lightsapp.core.analyzer.light.DerivativeLightAnalyzer;
import com.lightsapp.core.analyzer.light.ThresholdLightAnalyzer;
import com.lightsapp.ui.MainActivity;

import java.util.List;


public class CameraController extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = CameraController.class.getSimpleName();

    private MainActivity mContext;
    private SurfaceHolder mHolder = null;
    private Camera mCamera = null;

    private int format, width = 0, height = 0, fps_min, fps_max;
    private boolean done = false;

    public CameraController(Context context) {
        super(context);

        mContext = (MainActivity) context;

        setup();
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public Camera setup() {
        boolean err = false;
        boolean hasCamera = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        boolean hasFrontCamera = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

        Log.d(TAG, "setup camera ...");
        mCamera = null;
        try {
            if (hasCamera) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            }
            else if (hasFrontCamera && !hasCamera) {
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
            Log.d(TAG, "camera is ok");
            int algorithm = Integer.parseInt(mContext.mPrefs.getString("algorithm", "2"));
            switch (algorithm) {
                case 1:
                    mContext.mLightA = new ThresholdLightAnalyzer(mContext);
                    break;
                case 2:
                    mContext.mLightA = new DerivativeLightAnalyzer(mContext);
                    break;
                default:
                    mContext.mLightA = new BasicLightAnalyzer(mContext);
            }
        }

        return mCamera;
    }

    public Camera getCamera() { return mCamera; }

    public boolean isCameraNull() { return mCamera == null; }

    public boolean isDone() { return done; }

    public String getInfo() {
        return width + "x" + height + "@" + fps_min / 1000 + "/" + fps_max / 1000 + "fps";
    }

    public float getRatio() {
        return (float) width / (float) height;
    }

    private void setCameraParameters() {
        if (mCamera == null)
            return;

        try {
            mCamera.setDisplayOrientation(90);
            Camera.Parameters params = mCamera.getParameters();

            /* size */
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size current_size = null;

            for(int i=0; i<sizes.size(); i++) {
                Camera.Size size = sizes.get(i);
                Log.d(TAG, "Supported size: " + size.width + ", " + size.height);
                if( current_size == null || size.width < current_size.width ||
                   (size.width == current_size.width && size.height < current_size.height) ) {
                    current_size = size;
                }
            }
            if( current_size != null ) {
                Log.d(TAG, "Current size: " + current_size.width + ", " + current_size.height);
                params.setPreviewSize(current_size.width, current_size.height);
            }
            width = params.getPreviewSize().width;
            height = params.getPreviewSize().height;
            Log.d(TAG, "Preview size is -> width: " + width + " height: " + height);

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
            e.printStackTrace();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (format == ImageFormat.NV21) {
            mContext.mLightA.addFrame(data, width, height);
        }
        else {
            Log.e(TAG, "wrong image format");
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");

        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
            else {
                Log.d(TAG, "mCamera is null");
            }
        }
        catch (Exception e) {
            Log.d(TAG, "Error creating surface: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            setCameraParameters();
            setWillNotDraw(false);
            done = true;
        }
    }

    public void stopPreviewAndFreeCamera() {
        try {
            if(mCamera != null) {
                mCamera.stopPreview();
                mHolder.removeCallback(this);
                //mCamera.setPreviewCallback(null);
                //mCamera.lock();
                mCamera.release();
                mCamera = null;
            }
        }
        catch (Exception e) {
            Log.d(TAG, "Error destroying surface: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed()");
        stopPreviewAndFreeCamera();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged() " + w + ", " + h);

        if (mHolder == null || mHolder.getSurface() == null || mCamera == null) {
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
            Log.d(TAG, "Error surface changed: " + e.getMessage());
            e.printStackTrace();
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
            paint.setStrokeWidth(3f);
            paint.setAlpha(200);
            paint.setAntiAlias(true);
            paint.setColor(Color.RED);

            canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 15, paint);

            paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(13);

            if (mContext.mLightA != null) {
                String lines[] = mContext.mLightA.getStatusInfo().split("\\r?\\n");
                int i = 0;
                for (String line : lines) {
                    canvas.drawText(line, canvas.getWidth() / 2, 12 + i * 12, paint);
                    i++;
                }
            }

            if (mContext.mMorseA != null) {
                paint.setColor(Color.RED);
                canvas.drawText(mContext.mMorseA.getCurrentMorse(),
                        canvas.getWidth() / 2, canvas.getHeight() - 16, paint);
                paint.setTextSize(18);
                canvas.drawText(mContext.mMorseA.getCurrentText(),
                        canvas.getWidth() / 2, canvas.getHeight() - 32, paint);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Draw Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}