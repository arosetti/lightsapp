package com.lightsapp.core;

import android.util.Log;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class MyRunnable implements Runnable {
    private final String TAG = MyHandler.class.getSimpleName();
    private final Lock lock;
    private final Condition started;
    private final Condition stopped;
    private boolean status = false;
    private boolean loop = false;
    private Thread tid;

    public MyRunnable(boolean loop) {
        lock = new ReentrantLock(true);
        started = lock.newCondition();
        stopped = lock.newCondition();
        setLoop(loop);
    }

    public final void setLoop(boolean loop) {
        this.loop = loop;
    }

    public final boolean getLoop() {
        return loop;
    }

    public final boolean getStatus() {
        return status;
    }

    public final void setStatus(boolean s) { status = s; }

    public final void start() {
        tid = new Thread(this);
        tid.start();
    }

    public final void activate() {
        try {
            lock.lock();
            if (!loop) {
                while (status)
                    stopped.await();
            }
            status = true;
            started.signal();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public void setup() {}
    public void beforeloop() {}
    public void loop() {}
    public void afterloop() {}

    public final void run() {
        setup();
        while (true) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    lock.lock();
                    while (!status)
                        started.await();
                    beforeloop();
                    loop();
                    afterloop();
                    if (!loop) {
                        status = false;
                        stopped.signal();
                    }
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    public final void stop() {
        tid.interrupt();
    }
}
