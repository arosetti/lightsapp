package com.lightsapp.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.view.View;
import android.widget.FrameLayout;

import com.lightsapp.morse.MorseConverter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FrameAnalyzer implements Runnable {
    private final Lock lock;
    private final Condition started;
    private final Condition stopped;
    private boolean status = false;

    private Thread tid;
    private Camera mCamera;

    private FrameLayout frame;

    MorseConverter mMorse;

    public FrameAnalyzer(FrameLayout frame) {
        lock = new ReentrantLock(true);
        started = lock.newCondition();
        stopped = lock.newCondition();
        this.frame = frame;
     }

    public Bitmap viewToBitmap(View view) {
        if (view.getWidth() == 0 || view.getHeight() == 0) // TODO throw err
            return null;
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public void saveBMPTo(String filename, Bitmap bmp){
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveFrameLayout(FrameLayout frameLayout, String path) {
        frameLayout.setDrawingCacheEnabled(true);
        frameLayout.buildDrawingCache();
        Bitmap cache = frameLayout.getDrawingCache();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            cache.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            frameLayout.destroyDrawingCache();
        }
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
        while(true) {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    lock.lock();
                    while(!status)
                        started.await();

                    //Bitmap bm = null;
                    //bm = viewToBitmap(view);
                    //FrameLayout savedImage = null;
                    //savedImage = (FrameLayout)findViewById(R.id.imageWithoutFrame);
                    //savedImage.setDrawingCacheEnabled(true);
                    //savedImage.buildDrawingCache();
                    //bm = savedImage.getDrawingCache();
                    //if (bm != null)
                    //    saveBMPTo("/sdcard/prova.png", bm);

                    Thread.sleep(1);

                    status = false;
                    stopped.signal();
                    lock.unlock();
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