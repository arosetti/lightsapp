package com.lightsapp.camera;

import android.hardware.Camera;
import android.os.Handler;

import com.lightsapp.lightsapp.MyHandler;
import com.lightsapp.lightsapp.MyRunnable;
import com.lightsapp.morse.MorseConverter;

public class LightController extends MyRunnable {
    private Camera mCamera;
    private MyHandler myHandler;
    private volatile String data;
    private long[] pattern;

    MorseConverter mMorse;

    public LightController(Camera camera, Handler handler) {
        super(false);
        myHandler = new MyHandler(handler);
        data = "";
        mCamera = camera;
        mMorse = new MorseConverter(); // TODO pass reference of upper object in constructor
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

    @Override
    public void setup() {
        pattern = mMorse.pattern(data);
    }

    @Override
    public void main() {
        for (int i=0; i < pattern.length; i++) {
            //if (!status)
            //    break;
            if (i % 2 != 0) {
                if (pattern[i] > mMorse.DOT)
                    myHandler.signalStr("message", "DASH");
                else
                    myHandler.signalStr("message", "DOT");
                flash((int) pattern[i]);
            }
            else {
                myHandler.signalStr("message", "...");
                try {
                    Thread.sleep(pattern[i]);
                } catch (InterruptedException e) {}
            }
            myHandler.signalStr("progress", (100 * (i+1)) / pattern.length + "%");
        }
    }
}