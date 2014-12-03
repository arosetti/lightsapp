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
import com.lightsapp.core.analyzer.morse.MorseConverter;
import com.lightsapp.core.analyzer.sound.SoundAnalyzer;

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

        mHandlerSetup.post(new Runnable() {
            @Override
            public void run() {
                boolean done = false;
                int tentatives = 0;

                Camera camera = null;
                while (!done) {
                    try {
                        if (mContext.mCameraController == null) {
                            mContext.mCameraController = new CameraController(mContext);
                            camera = mContext.mCameraController.setup();
                        }

                        if (mContext.mOutputController == null && camera != null) {
                            mContext.mOutputController = new OutputController(context);
                            mContext.mOutputController.start();
                        }

                        if (mContext.mSoundController == null) {
                            mContext.mSoundController = new SoundController(context);
                            mContext.mSoundController.setup();
                        }

                        if (mContext.mHandlerInfo != null && mContext.mHandlerRecv != null &&
                            mContext.mCameraController != null && camera != null &&
                            mContext.mOutputController != null &&
                            mContext.mSoundController != null) {
                            signalStr(mContext.mHandlerInfo, "setup_done", "");
                            signalStr(mContext.mHandlerRecv, "setup_done", "");
                            done = true;
                        }
                        tentatives++;
                    }
                    catch (Exception e) {
                        Log.e(TAG, "setup error: " + e.getMessage());
                    }

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
                }
                if (done)
                    Log.v(TAG, "setup done");
            }
        });
    }
}