package com.lightsapp.camera;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.lightsapp.morse.MorseConverter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LightController implements Runnable {
    private final Lock lock;
    private final Condition started;
    private final Condition stopped;
    private boolean status = false;

    private Thread tid;
    private Camera mCamera;
    private Handler mHandler;

    private volatile String data;
    private long[] pattern;

    MorseConverter mMorse;

    // TODO derive from class implementing this and handler class.
    private void signalUI(String key, String str) {
        if (str != null && key != null && !key.equals("")) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString(key, str);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    public LightController(Camera camera, Handler handler) {
        lock = new ReentrantLock(true);
        started = lock.newCondition();
        stopped = lock.newCondition();

        data = "";
        mCamera = camera;
        mHandler = handler;
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
                    while(!status) {
                        signalUI("message", "idle");
                        started.await();
                    }
                    for (int i=0; i < pattern.length; i++) {
                        if (!status)
                            break;
                        if (i % 2 != 0) {
                            if (pattern[i] > mMorse.DOT)
                                signalUI("message", "DASH");
                            else
                                signalUI("message", "DOT");
                            flash((int) pattern[i]);
                        }
                        else {
                            signalUI("message", "...");
                            Thread.sleep(pattern[i]);
                        }
                        signalUI("progress", (100 * (i+1)) / pattern.length + "%");
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