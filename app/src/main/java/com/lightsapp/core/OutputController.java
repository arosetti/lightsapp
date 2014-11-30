package com.lightsapp.core;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.lightsapp.core.output.BeepOutput;
import com.lightsapp.core.output.LightOutput;
import com.lightsapp.utils.MyRunnable;
import com.lightsapp.ui.MainActivity;
import com.lightsapp.core.analyzer.morse.MorseConverter;
import static com.lightsapp.utils.Utils.*;
import static com.lightsapp.utils.HandlerUtils.*;

public class OutputController extends MyRunnable {
    private final String TAG = OutputController.class.getSimpleName();

    private MainActivity mContext;

    private volatile String data = "";
    private long[] pattern;
    private int progress;
    private boolean enable_sound, enable_light;

    private BeepOutput mBeepOutput;
    private LightOutput mLightOutput;
    private int beepFreq;

    public OutputController(Context context) {
        super(false);

        mContext = (MainActivity) context;

        mBeepOutput = new BeepOutput();
        mLightOutput = new LightOutput(mContext.mCameraController.getCamera());

        try {
            beepFreq = Integer.valueOf(mContext.mPrefs.getString("beep_freq", "850"));
        }
        catch (Exception e)
        {
            beepFreq = 850;
        }

        if(beepFreq > 12000 || beepFreq < 100) {
            Toast t = Toast.makeText(mContext,
                    "Invalid beep sampleRate, valid range [100,12KHz], defaulting to 850Hz.",
                    Toast.LENGTH_SHORT);
            t.show();
            beepFreq = 850;
        }
    }

    private long sound(int t) {
        final long timestamp = System.currentTimeMillis();
        mBeepOutput.play(t, beepFreq);
        return (System.currentTimeMillis() - timestamp);
    }

    private void signal(int t) {
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
        if (mContext.mMorse != null)
            pattern = mContext.mMorse.pattern(data);
    }

    @Override
    public void onDie() {
        mLightOutput.setLight(false);
        signalStr(mContext.mHandlerSend, "light", "off");
        signalInt(mContext.mHandlerSend, "progress", -1);
    }

    @Override
    public void loop() {
        final long DOT = mContext.mMorse.get("DOT");
        final long LETTER_GAP = mContext.mMorse.get("LETTER_GAP");
        final long WORD_GAP = mContext.mMorse.get("WORD_GAP");
        pattern = mContext.mMorse.pattern(data);
        progress = 0;

        for (int i = 0; i < pattern.length; i++) {
            enable_sound = mContext.mPrefs.getBoolean("enable_sound", false);
            enable_light = mContext.mPrefs.getBoolean("enable_light", false);

            if (!getStatus()) {
                Log.v(TAG, "Transmission canceled");
                break;
            }

            if (i % 2 != 0) {
                signalStr(mContext.mHandlerSend, "light", "on");
                if (Math.abs(pattern[i]) > DOT)
                    signalStr(mContext.mHandlerSend, "message", "DASH\n" + pattern[i] + "ms");
                else
                    signalStr(mContext.mHandlerSend, "message", "DOT\n" + pattern[i] + "ms");
                progress++;
                signalInt(mContext.mHandlerSend, "progress", progress);
                signal((int) Math.abs(pattern[i]));
                signalStr(mContext.mHandlerSend, "light", "off");
            }
            else {
                signalStr(mContext.mHandlerSend, "message", "...\n" + pattern[i] + "ms");
                if (Math.abs(pattern[i]) == LETTER_GAP)
                    progress++;
                else if (Math.abs(pattern[i]) == WORD_GAP)
                    progress += 3;
                signalInt(mContext.mHandlerSend, "progress", progress);
                ForcedSleep((int) Math.abs(pattern[i]));
            }
        }

        ForcedSleep((int) Math.abs(WORD_GAP));
        if (mContext.mPrefs.getBoolean("repeat_send", false))
            setLoop(true);
        else
            setLoop(false);
        Log.v(TAG, "Transmission completed");
    }
}