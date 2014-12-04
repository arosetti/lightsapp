package com.lightsapp.ui;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import com.lightsapp.core.CameraController;
import com.lightsapp.core.OutputController;
import com.lightsapp.core.SoundController;

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

    void setupHandler(final Context context) {
        mContext = (MainActivity) context;

        Log.d(TAG, "Setup Handler activated");
        mHandlerSetup.post(new Runnable() {
            @Override
            public void run() {
                int step = 0;
                int tentatives = 0;

                while (step < 2) {
                    if (step == 0) {
                        try {
                            if (mContext.mCameraController == null) {
                                Log.d(TAG, "new CameraController");
                                mContext.mCameraController = new CameraController(context);
                                if (mContext.mCameraController.isCameraNull())
                                    Log.d(TAG, "camera is null");
                            }

                            if (mContext.mOutputController == null &&
                                !mContext.mCameraController.isCameraNull()) {
                                Log.d(TAG, "new OutputController");
                                mContext.mOutputController = new OutputController(context);
                                mContext.mOutputController.start();
                            }

                            if (mContext.mSoundController == null) {
                                Log.d(TAG, "new SoundController");
                                mContext.mSoundController = new SoundController(context);
                                mContext.mSoundController.setup();
                            }

                            if (mContext.mHandlerInfo != null && mContext.mHandlerRecv != null &&
                                    mContext.mCameraController != null &&
                                    !mContext.mCameraController.isCameraNull() &&
                                    mContext.mOutputController != null &&
                                    mContext.mSoundController != null) {
                                Log.d(TAG, "Sending setup_done to handlers");
                                signalStr(mContext.mHandlerInfo, "setup_done", "");
                                signalStr(mContext.mHandlerRecv, "setup_done", "");
                                step = 1;
                            }

                            if (step == 0)
                            {
                                Log.e(TAG, "setup first step failed... ");
                                Thread.sleep(300);
                            }
                        }
                        catch (InterruptedException e) { }
                        catch (Exception e) {
                            Log.e(TAG, "setup first step error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else if (step == 1)
                    {
                        try {
                            if (mContext.mCameraController != null && mContext.mCameraController.isDone()) {
                                step = 2;
                            }
                            else {
                                Log.d(TAG, "waiting for surface creation... ");
                                tentatives++;
                                Thread.sleep(100);
                            }

                            if (tentatives > 10) {
                                step = 0;
                                tentatives = 0;
                                Thread.sleep(3000);
                            }
                        }
                        catch (InterruptedException e) {
                        }
                    }

                    /*
                    try {
                        if (!done && tentatives > 10) {
                            Toast toast = Toast.makeText(context,
                                    "Setup failed!",
                                    Toast.LENGTH_LONG);
                            toast.show();
                            break;
                        }
                        else if (!done) {
                            Log.e(TAG, "setup failed, retrying... ");
                            Thread.sleep(100);
                        }
                    }
                    catch (InterruptedException e) {
                    }
                    */
                }

                Log.v(TAG, "setup done");
            }
        });
    }
}