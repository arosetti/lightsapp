package com.lightsapp.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HandlerUtils {
    private static String TAG = "HandlerUtils";

    public static void signalStr(Handler handler, String key, String str) {
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

    public static void signalInt(Handler handler, String key, int i) {
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