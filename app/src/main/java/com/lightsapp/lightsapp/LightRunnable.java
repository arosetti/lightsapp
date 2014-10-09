package com.lightsapp.lightsapp;

import android.hardware.Camera;

import com.lightsapp.morse.MorseCodeConverter;

public class LightRunnable implements Runnable {
    private Thread tid;
    private boolean status = false;
    private Camera mCamera;

    private volatile String data;
    private long[] pattern;

    MorseCodeConverter mMorse;

    public LightRunnable(String data) {
        this.data = data;
        mCamera = Camera.open(); // TODO throw error.
        mMorse = new MorseCodeConverter(); // TODO pass reference of upper object in constructor
    }

    private void flash(int tOn) {
        Camera.Parameters p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(p);
        mCamera.startPreview();
        try {
            Thread.sleep(tOn);
        } catch (InterruptedException e) {}
        p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(p);
        mCamera.stopPreview();
    }

    public void setString(String str)
    {
        data = new String(str);
        pattern = mMorse.pattern(data);
    }

    public void start() {
        if (status)
            return;
        tid = new Thread(this);
        tid.start();
        status = true;
    }

    public void run() {
        pattern = mMorse.pattern(data);

        while(true) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // TODO stop to a semaphore
                    if (status)
                        for (int i=0; i < pattern.length; i++) {
                            if (!status)
                                break;
                            if (i % 2 != 0) {
                                flash((int) pattern[i]);
                            }
                            else
                                Thread.sleep(pattern[i]);
                        }
                        // TODO attendere una nuova parola prima di un nuovo giro?
                    else {
                        Thread.sleep(3000);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void pause()
    {
        status = false;
    }

    public void stop()
    {
        tid.interrupt();
        if (mCamera != null)
            mCamera.release();
     }
}