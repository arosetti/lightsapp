package com.lightsapp.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class HandlerUtils {
    private static String TAG = HandlerUtils.class.getSimpleName();

    public static void signalStr(Handler handler, String key, String str) {
        assert(handler != null);

        if (str != null && key != null && !key.equals("")) {
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putString(key, str);
            msg.setData(b);
            handler.sendMessage(msg);
        }
    }

    public static void signalInt(Handler handler, String key, int i) {
        assert(handler != null);

        if (key != null && !key.equals("")) {
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt(key, i);
            msg.setData(b);
            handler.sendMessage(msg);
        }
    }
}