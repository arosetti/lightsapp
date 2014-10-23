package com.lightsapp.core;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MyHandler implements MyHandlerInterface {
    private String TAG = MyHandler.class.getSimpleName();
    Handler mHandler = null;

    public MyHandler(Handler handler, String TAG) {

        this.TAG += "_" + TAG;
        mHandler = handler;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }
    public Handler getHandler() { return mHandler; }

    public boolean isHandlerNull() { return mHandler == null; }

    public void signalStr(String key, String str) {
        signalStr(mHandler, key, str);
    }

    public void signalStr(Handler handler, String key, String str) {
        if (handler == null) {
            Log.e(TAG, "handler is null");
            return;
        }

        if (str != null && key != null && !key.equals("")) {
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putString(key, str);
            msg.setData(b);
            handler.sendMessage(msg);
        }
    }

    public void signalInt(String key, int i) {
        signalInt(mHandler, key, i);
    }

    public void signalInt(Handler handler, String key, int i) {
        if (handler == null) {
            Log.e(TAG, "handler is null");
            return;
        }

        if (key != null && !key.equals("")) {
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt(key, i);
            msg.setData(b);
            handler.sendMessage(msg);
        }
    }
}