package com.lightsapp.utils;

import android.util.Log;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class MyRunnable implements Runnable {
    private final String TAG = HandlerUtils.class.getSimpleName();
    private final Lock lock;
    private final Condition started;
    private final Condition stopped;

    private final AtomicReference<Boolean> status;

    private boolean loop = false;
    private Thread tid;

    public MyRunnable(boolean loop) {
        lock = new ReentrantLock(true);
        started = lock.newCondition();
        stopped = lock.newCondition();
        status = new AtomicReference<Boolean>(false);
        setLoop(loop);
    }

    public final void setLoop(boolean loop) {
        this.loop = loop;
    }

    public final boolean getLoop() {
        return loop;
    }

    public final boolean getStatus() {
        return status.get();
    }

    public final void setStatus(boolean s) {
        status.getAndSet(s);
    }

    public final void start() {
        tid = new Thread(this);
        tid.start();
    }

    public final void stop() {
        lock.lock();
        tid.interrupt();
        lock.unlock();
    }

    public final void activate() {
        try {
            lock.lock();
            if (!loop) {
                while (status.get())
                    stopped.await();
            }
            status.getAndSet(true);
            started.signal();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public void setup() {
    }

    public void beforeloop() {
    }

    public void loop() {
    }

    public void afterloop() {
    }

    public final void run() {
        setup();
        while (true) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    lock.lock();
                    while (!status.get())
                        started.await();
                    beforeloop();
                    loop();
                    afterloop();
                    if (!loop) {
                        status.getAndSet(false);
                        stopped.signal();
                    }
                    lock.unlock();
                }
            }
            catch (InterruptedException e) {
                Log.d(TAG, "Thread Interrupted");
                Thread.currentThread().interrupt();
            }
            catch (Exception e) {
                Log.e(TAG, "loop error -> " + e.getMessage());
            }
        }
    }
}
