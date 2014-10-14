package com.lightsapp.core;

import android.os.Handler;

interface MyHandlerInterface {
    public void setHandler(Handler handler);
    public void signalStr(String key, String str);
    public void signalInt(String key, int i);
}