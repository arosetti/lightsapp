package com.lightsapp.lightsapp;

import android.os.Handler;

interface MyHandlerInterface {
    public void setHandler(Handler handler);
    public void signalStr(String key, String str);
    public void signalInt(String key, int i);
}