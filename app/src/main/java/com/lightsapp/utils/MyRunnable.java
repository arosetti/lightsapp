package com.lightsapp.utils;

import android.util.Log;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public abstract class MyRunnable implements Runnable {
    protected String TAG = MyRunnable.class.getSimpleName();
    private final Lock lock;
    private final Condition started;
    private final Condition stopped;

    private AtomicReference<Boolean> status;
    private AtomicReference<Boolean> loop;

    private Thread tid;

    public MyRunnable(boolean loop) {
        lock = new ReentrantLock(true);
        started = lock.newCondition();
        stopped = lock.newCondition();
        status = new AtomicReference<Boolean>(false);
        this.loop = new AtomicReference<Boolean>(loop);
        setLoop(loop);
    }

    public final boolean getLoop() {
        return loop.get();
    }

    public final void setLoop(boolean l) {
        loop.getAndSet(l);
    }

    public final boolean getStatus() {
        return status.get();
    }

    public final void setStatus(boolean s) {
        status.getAndSet(s);
    }

    public final void start() {
        if (tid == null) {
            tid = new Thread(this);
            tid.start();
        }
    }

    public final void repeat() {
        setLoop(true);
        start();
    }

    public final void stop() {
        setLoop(false);
        if (tid != null)
            tid.interrupt();
        status.getAndSet(false);
    }

    public final void activate() {
        try {
            lock.lock();
            if (!loop.get()) {
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

    public final void deactivate() {
        lock.lock();
        status.getAndSet(false);
        lock.unlock();
    }

    public void setup() {
    }

    public void beforeloop() {
    }

    public void loop() {
    }

    public void afterloop() {
    }

    public void onDie() {
    }

    public final void run() {
        boolean run = true;
        setup();

        while (run) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    lock.lock();
                    while (!status.get())
                        started.await();
                    beforeloop();
                    loop();
                    afterloop();
                    if (!loop.get()) {
                        status.getAndSet(false);
                        stopped.signal();
                    }
                    lock.unlock();
                }
            }
            catch (InterruptedException e) {
                Log.d(TAG, "Thread Interrupted");
                Thread.currentThread().interrupt();
                run = false;
                onDie();
            }
            catch (Exception e) {
                Log.e(TAG, "loop exception -> " + e.toString() + " " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
