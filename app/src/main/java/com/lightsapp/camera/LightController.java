package com.lightsapp.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import com.lightsapp.utils.Beep;
import com.lightsapp.utils.MyRunnable;
import com.lightsapp.lightsapp.MainActivity;
import com.lightsapp.morse.MorseConverter;
import static com.lightsapp.utils.Utils.*;
import static com.lightsapp.utils.HandlerUtils.*;

public class LightController extends MyRunnable {
    private final String TAG = LightController.class.getSimpleName();

    private MainActivity mCtx;

    private Camera mCamera;
    private volatile String data;
    private long[] pattern;
    private int progress;
    private boolean sound;

    private MorseConverter mMorse;
    private Beep mBeep;
    private final int beepFreq = 850;

    public LightController(Context context) {
        super(false);

        mCtx = (MainActivity) context;

        mCamera = mCtx.mCamera;
        mMorse = mCtx.mMorse;
        mBeep = new Beep();
        sound = mCtx.mPrefs.getBoolean("enable_sound", false);
        data = "";
    }

    private long sound(int t) { // TODO optimize this
        final long timestamp = System.currentTimeMillis();
        mBeep.genTone(t / 1000f, beepFreq);
        mBeep.playSound();
        return (System.currentTimeMillis() - timestamp);
    }

    private void flash(int t) {
        long ret = 0;

        Camera.Parameters p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(p);

        if (sound)
            ret = sound(t);

        if ((t - ret) > 0)
            ForcedSleep((int) (t - ret));
        else
            Log.v(TAG, "disable sound output please...");

        p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(p);
    }

    public void setString(String str) {
        data = new String(str);
        if (mMorse != null)
            pattern = mMorse.pattern(data);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    @Override
    public void loop() {
        final long DOT = mMorse.get("DOT");
        final long DASH = mMorse.get("DASH");
        final long GAP = mMorse.get("GAP");
        final long LETTER_GAP = mMorse.get("LETTER_GAP");
        final long WORD_GAP = mMorse.get("WORD_GAP");
        pattern = mMorse.pattern(data);
        progress = 0;

        for (int i = 0; i < pattern.length; i++) {
            if (!getStatus()) {
                Log.v(TAG, "STOPPING LED OUTPUT");
                break;
            }
            if (i % 2 != 0) {
                signalStr(mCtx.mHandlerSend, "light", "on");
                if (Math.abs(pattern[i]) > DOT)
                    signalStr(mCtx.mHandlerSend, "message", "DASH\n" + pattern[i] + "ms");
                else
                    signalStr(mCtx.mHandlerSend, "message", "DOT\n" + pattern[i] + "ms");
                progress++;
                signalInt(mCtx.mHandlerSend, "progress", progress);
                flash((int) Math.abs(pattern[i]));
                signalStr(mCtx.mHandlerSend, "light", "off");
            } else {
                signalStr(mCtx.mHandlerSend, "message", "...\n" + pattern[i] + "ms");
                if (Math.abs(pattern[i]) == LETTER_GAP)
                    progress++;
                else if (Math.abs(pattern[i]) == WORD_GAP)
                    progress += 3;
                signalInt(mCtx.mHandlerSend, "progress", progress);
                ForcedSleep((int) Math.abs(pattern[i]));
            }
        }
        Log.v(TAG, "END LED OUTPUT");
    }
}