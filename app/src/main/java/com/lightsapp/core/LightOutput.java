package com.lightsapp.core;

import android.hardware.Camera;
import android.util.Log;

public class LightOutput {
    private final String TAG = OutputController.class.getSimpleName();

    private Camera mCamera;

    public LightOutput(Camera camera)
    {
        mCamera = camera;
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    public void setLight(boolean b) {
        Camera.Parameters p;

        try {
            p = mCamera.getParameters();
            if (b) {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            else {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCamera.setParameters(p);
        }
        catch (Exception e) {
            Log.e(TAG, "error setting led flash " + (b ? "on" : "off"));
        }
    }
}
