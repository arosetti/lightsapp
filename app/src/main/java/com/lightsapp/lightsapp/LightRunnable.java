package com.lightsapp.lightsapp;

import android.hardware.Camera;

import com.lightsapp.morse.MorseCodeConverter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LightRunnable implements Runnable {
    private final Lock lock;
    private final Condition started;
    private final Condition stopped;
    private boolean status = false;

    private Thread tid;
    private Camera mCamera;

    private volatile String data;
    private long[] pattern;

    MorseCodeConverter mMorse;

    public LightRunnable(Camera camera, String data) {
        lock = new ReentrantLock(true);
        started = lock.newCondition();
        stopped = lock.newCondition();

        this.data = data;
        mCamera = camera;
        mMorse = new MorseCodeConverter(); // TODO pass reference of upper object in constructor

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

    public void start() {
        tid = new Thread(this);
        tid.start();
    }

    public void activate() {
        try {
            lock.lock();
            while(status)
                stopped.await();
            status = true;
            started.signal();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        finally {
            lock.unlock();
        }
    }

    public void run() {
        pattern = mMorse.pattern(data);

        while(true) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    lock.lock();
                    while(!status)
                        started.await();
                    for (int i=0; i < pattern.length; i++) {
                        if (!status)
                            break;
                        if (i % 2 != 0) {
                            flash((int) pattern[i]);
                        }
                        else
                            Thread.sleep(pattern[i]);
                    }
                    status = false;
                    stopped.signal();
                    lock.unlock();
                    // TODO attendere una nuova parola prima di un nuovo giro?
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void pause() // TODO 100% not working :D
    {
        try {
            lock.lock();
            while(!status)
                started.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        finally {
            lock.unlock();
        }
    }

    public void stop()
    {
        tid.interrupt();
    }
}