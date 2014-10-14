package com.lightsapp.camera;

import android.hardware.Camera;
import android.os.Handler;

import com.lightsapp.core.MyHandler;
import com.lightsapp.core.MyRunnable;
import com.lightsapp.morse.MorseConverter;

public class LightController extends MyRunnable {
    private Camera mCamera;
    private MyHandler myHandler;
    private volatile String data;
    private long[] pattern;
    private int progress;

    MorseConverter mMorse;

    public LightController(MorseConverter morse, Camera camera, Handler handler) {
        super(false);
        myHandler = new MyHandler(handler);
        data = "";
        mCamera = camera;
        mMorse = morse;
    }

    private void flash(int tOn) {
        Camera.Parameters p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(p);
        //mCamera.startPreview();
        try {
            Thread.sleep(tOn);
        } catch (InterruptedException e) {}
        p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(p);
        //mCamera.stopPreview();
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
    public void setup() {

    }

    @Override
    public void main() {
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