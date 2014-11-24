package com.lightsapp.core;

import android.content.Context;
import android.util.Log;

import com.lightsapp.utils.MyRunnable;
import com.lightsapp.lightsapp.MainActivity;
import com.lightsapp.core.morse.MorseConverter;
import static com.lightsapp.utils.Utils.*;
import static com.lightsapp.utils.HandlerUtils.*;

public class OutputController extends MyRunnable {
    private final String TAG = OutputController.class.getSimpleName();

    private MainActivity mCtx;

    private volatile String data = "";
    private long[] pattern;
    private int progress;
    private boolean enable_sound, enable_light;

    private MorseConverter mMorse;
    private BeepOutput mBeepOutput;
    private LightOutput mLightOutput;
    private int beepFreq;

    public OutputController(Context context) {
        super(false);

        mCtx = (MainActivity) context;

        mMorse = mCtx.mMorse;
        mBeepOutput = new BeepOutput();
        mLightOutput = new LightOutput(mCtx.mCamera);

        try {
            beepFreq = Integer.valueOf(mCtx.mPrefs.getString("beep_freq", "850"));
        }
        catch (Exception e)
        {
            Log.e(TAG,"BOOM!");
        }
    }

    private long sound(int t) { // TODO optimize this
        final long timestamp = System.currentTimeMillis();
        mBeepOutput.genTone(t / 1000f, beepFreq);
        mBeepOutput.playSound();
        return (System.currentTimeMillis() - timestamp);
    }

    private void flash(int t) {
        long ret = 0;

        if (enable_light)
            mLightOutput.setLight(true);

        if (enable_sound)
            ret = sound(t);

        if ((t - ret) > 0)
            ForcedSleep((int) (t - ret));
        else
            Log.v(TAG, "please, disable enable_sound output...");

        if (enable_light)
            mLightOutput.setLight(false);
    }

    public void setString(String str) {
        data = new String(str);
        if (mMorse != null)
            pattern = mMorse.pattern(data);
    }

    @Override
    public void ondie() {
        mLightOutput.setLight(false);
        signalStr(mCtx.mHandlerSend, "light", "off");
        signalInt(mCtx.mHandlerSend, "progress", -1);
    }

    @Override
    public void loop() {
        final long DOT = mMorse.get("DOT");
        final long LETTER_GAP = mMorse.get("LETTER_GAP");
        final long WORD_GAP = mMorse.get("WORD_GAP");
        pattern = mMorse.pattern(data);
        progress = 0;

        for (int i = 0; i < pattern.length; i++) {
            enable_sound = mCtx.mPrefs.getBoolean("enable_sound", false);
            enable_light = mCtx.mPrefs.getBoolean("enable_light", false);

            if (!getStatus()) {
                Log.v(TAG, "STOPPING LED OUTPUT");
                break;
            }

            if (i % 2 != 0) {
                signalStr(mCtx.mHandlerSend, "light", "on");
                if (Math.abs(pattern[i]) > DOT)
                    signalStr(mCtx.mHandlerSend, "message", "DASH\n" + pattern[i] + "ms");
                else
                    signalStr(mCtx.mHandlerSend, "message", "DOT\n" + pattern[i] + "ms");
                progress++;
                signalInt(mCtx.mHandlerSend, "progress", progress);
                flash((int) Math.abs(pattern[i]));
                signalStr(mCtx.mHandlerSend, "light", "off");
            }
            else {
                signalStr(mCtx.mHandlerSend, "message", "...\n" + pattern[i] + "ms");
                if (Math.abs(pattern[i]) == LETTER_GAP)
                    progress++;
                else if (Math.abs(pattern[i]) == WORD_GAP)
                    progress += 3;
                signalInt(mCtx.mHandlerSend, "progress", progress);
                ForcedSleep((int) Math.abs(pattern[i]));
            }
        }
        Log.v(TAG, "END LED OUTPUT");
    }
}