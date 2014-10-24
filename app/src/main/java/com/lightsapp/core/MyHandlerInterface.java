package com.lightsapp.core;

import android.os.Handler;

interface MyHandlerInterface {
    public void setHandler(Handler handler);
    public Handler getHandler();

    public boolean isHandlerNull();

    public void signalStr(String key, String str);
    public void signalStr(Handler handler, String key, String str);
    public void signalInt(String key, int i);
    public void signalInt(Handler handler, String key, int i);
}