package com.lightsapp.lightsapp;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.lightsapp.camera.CameraController;
import com.lightsapp.camera.LightController;

import static com.lightsapp.utils.HandlerUtils.signalStr;

public class SetupHandler extends HandlerThread {
    private final String TAG = SetupHandler.class.getSimpleName();
    Handler mHandlerSetup = null;
    private MainActivity mContext;

    SetupHandler() {
        super("SetupHandler");
        start();
        mHandlerSetup = new Handler(getLooper());
    }

    void setupHandler(Context context) {
        mContext = (MainActivity) context;
        mHandlerSetup.post(new Runnable() {
            @Override
            public void run() {
                boolean done = false;

                while (!done) {
                    try {
                        if (mContext.mCamera == null) {
                            mContext.mCameraController = new CameraController(mContext);
                            mContext.mCamera = mContext.mCameraController.setup();
                        }

                        if (mContext.mCamera != null) {
                            mContext.mLightController = new LightController(mContext);
                            mContext.mLightController.start();
                        }

                        if (mContext.mHandlerGraph != null && mContext.mCameraController != null &&
                                mContext.mCamera != null && mContext.mLightController != null) {
                            signalStr(mContext.mHandlerGraph, "setup_done", "");
                            done = true;
                        }
                    }
                    catch (RuntimeException e) {
                        Log.e(TAG, "error: " + e.getMessage());
                    }
                    finally {
                        if (done)
                            Log.v(TAG, "setup done");
                    }

                    try {
                        if (!done)
                            Thread.sleep(100);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        });
    }
}