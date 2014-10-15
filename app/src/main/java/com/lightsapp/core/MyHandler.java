package com.lightsapp.core;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class MyHandler implements MyHandlerInterface {
    private final String TAG = MyHandler.class.getSimpleName();
    Handler mHandler = null;

    public MyHandler(Handler handler) {
        mHandler = handler;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void signalStr(String key, String str) {
        if (str != null && key != null && !key.equals("")) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putString(key, str);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    public void signalInt(String key, int i) {
        if (key != null && !key.equals("")) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt(key, i);
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }
}