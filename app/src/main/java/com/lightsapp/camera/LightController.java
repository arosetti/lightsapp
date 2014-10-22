package com.lightsapp.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

import com.lightsapp.core.Beep;
import com.lightsapp.core.MyHandler;
import com.lightsapp.core.MyRunnable;
import com.lightsapp.morse.MorseConverter;

public class LightController extends MyRunnable {
    private final String TAG = LightController.class.getSimpleName();
    private Camera mCamera;
    private MyHandler myHandler;
    private volatile String data;
    private long[] pattern;
    private int progress;
    private boolean sound;

    private MorseConverter mMorse;
    private Beep mBeep;
    private final int beepFreq = 850;

    public LightController(MorseConverter morse, Camera camera, Handler handler, boolean sound) {
        super(false);
        myHandler = new MyHandler(handler);
        data = "";
        mCamera = camera;
        mMorse = morse;
        mBeep = new Beep();
        this.sound = sound;
    }

    public static void forceSleep(int msec) {
        final long endingTime = System.currentTimeMillis() + msec;
        long remainingTime = msec;
        while (remainingTime > 0) {
            try {
                Thread.sleep(remainingTime);
            } catch (InterruptedException ignore) {
            }
            remainingTime = endingTime - System.currentTimeMillis();
        }
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
            forceSleep((int) (t - ret));
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
    public void afterloop() {
        myHandler.signalInt("progress", 0);
        myHandler.signalStr("message", "");
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
            if (!getStatus())
                break;
            if (i % 2 != 0) {
                myHandler.signalStr("light", "on");
                if (Math.abs(pattern[i]) > DOT)
                    myHandler.signalStr("message", "DASH\n" + pattern[i] + "ms");
                else
                    myHandler.signalStr("message", "DOT\n" + pattern[i] + "ms");
                progress++;
                myHandler.signalInt("progress", progress);
                flash((int) Math.abs(pattern[i]));
                myHandler.signalStr("light", "off");
            } else {
                myHandler.signalStr("message", "...\n" + pattern[i] + "ms");
                if (Math.abs(pattern[i]) == LETTER_GAP)
                    progress++;
                else if (Math.abs(pattern[i]) == WORD_GAP)
                    progress += 3;
                myHandler.signalInt("progress", progress);
                forceSleep((int) Math.abs(pattern[i]));
            }
        }
    }
}