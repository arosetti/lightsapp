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

    public LightController(MorseConverter morse, Camera camera, Handler handler, boolean sound) {
        super(false);
        myHandler = new MyHandler(handler);
        data = "";
        mCamera = camera;
        mMorse = morse;
        mBeep = new Beep();
        this.sound = sound;
    }

    private long sound(int t) {
        int vol = 75;
        long delta, timestamp;
        //AudioManager mAudio = (AudioManager) Context.getSystemService(Context.AUDIO_SERVICE);
        //vol = 100 * mAudio.getStreamVolume(AudioManager.STREAM_MUSIC) /
        //        mAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        timestamp = System.currentTimeMillis();
        mBeep.genTone(t/1000f, 800);
        mBeep.playSound();
        delta = (System.currentTimeMillis() - timestamp);
        Log.v(TAG, ">>>>>>>>>>>>>> t: " + t);
        Log.v(TAG, ">>>>>>>>>>>>>> delta: " + delta);
        return delta;
    }

    private void flash(int tOn) {
        long ret = 0;
        Camera.Parameters p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(p);
        if (sound)
            ret = sound(tOn);
        try {
            Thread.sleep(tOn - ret); //TODO check > 0 O.o
        } catch (InterruptedException e) {}
        p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(p);
    }

    public void setString(String str)
    {
        data = new String(str);
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
        pattern = mMorse.pattern(data);
        progress = 0;
        for (int i = 0; i < pattern.length; i++) {
            if (!getStatus())
                break;
            if (i % 2 != 0) {
                if (pattern[i] > mMorse.get("DOT"))
                    myHandler.signalStr("message", "DASH");
                else
                    myHandler.signalStr("message", "DOT");
                flash((int) pattern[i]);
                progress++;
            }
            else {
                myHandler.signalStr("message", "...");
                try {
                    Thread.sleep(pattern[i]);
                } catch (InterruptedException e) {}
                if (pattern[i] == mMorse.get("LETTER_GAP") )
                    progress++;
                else if (pattern[i] == mMorse.get("WORD_GAP"))
                    progress += 3;
            }
            myHandler.signalInt("progress", progress);
        }
    }
}