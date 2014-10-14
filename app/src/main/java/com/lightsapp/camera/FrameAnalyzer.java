package com.lightsapp.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.lightsapp.lightsapp.MyRunnable;
import com.lightsapp.morse.MorseConverter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FrameAnalyzer extends MyRunnable {
    private Camera mCamera;
    MorseConverter mMorse;

    public FrameAnalyzer() {
        super(true);
     }

    @Override
    public void main() {
        try {
            Thread.sleep(10);
            Log.v("TAG", "prova prova");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}