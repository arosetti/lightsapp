package com.lightsapp.core.analyzer.morse;

import android.content.Context;
import android.util.Log;

import com.lightsapp.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

import static com.lightsapp.utils.HandlerUtils.signalStr;
import static com.lightsapp.utils.Utils.ListToPrimitiveArray;
import static com.lightsapp.utils.Utils.StripString;


public class MorseAnalyzer {
    protected final String TAG = MorseAnalyzer.class.getSimpleName();

    protected MainActivity mContext;

    protected final MorseConverter mMorse;
    protected boolean auto_interval = false;
    protected long speed_base;

    protected String str = "";

    public MorseAnalyzer(Context context) {
        mContext = (MainActivity) context;

        mMorse = new MorseConverter(Integer.parseInt(mContext.mPrefs.getString("interval", "500")));

        speed_base = mMorse.get("SPEED_BASE");
        auto_interval = mContext.mPrefs.getBoolean("auto_interval", false);
    }

    private long getClosestMorseInterval(long val) {
        ArrayList<Long> speed_val = new ArrayList<Long>();
        speed_val.add(0L);
        for (long i = 0; i <= 3000;) {
            if (i < 1000)
                i = speed_val.get(speed_val.size() - 1) + 100;
            else if (i < 2000)
                i = speed_val.get(speed_val.size() - 1) + 250;
            else
                i = speed_val.get(speed_val.size() - 1) + 500;
            speed_val.add(i);
        }

        int j = 0, min=Integer.MAX_VALUE;
        for (int i= 0; i < speed_val.size(); i++) {
            if (min > Math.abs(speed_val.get(i) - val))
            {
                j = i;
                min = (int) Math.abs(speed_val.get(i) - val);
            }
        }

        return speed_val.get(j);
    }

    public long getSpeedBase() {
        return speed_base;
    }

    public boolean getAutoInterval() {
        return auto_interval;
    }

    private long getAvgGap(List<Long> ldata) {
        long g = 0, last = Long.MAX_VALUE;
        int n = 0;

        for (int i = 1; i < ldata.size(); i+=2) {
            if ((Math.abs(ldata.get(i))/2) < last) {
                last = Math.abs(ldata.get(i));
                g += last;
                n++;
            }
        }
        if (n == 0)
            return 0;
        else
            return g/n;
    }

    // approximate to morse values and generate long[]
    public final void analyze(List<Long> ldata) {
        long dbase, dlong, dvlong, sign, closest;

        if (ldata.size() == 0)
            return;

        auto_interval = mContext.mPrefs.getBoolean("auto_interval", false);
        if (auto_interval) {
            long avg_gap = getAvgGap(ldata);
            long s = getClosestMorseInterval(avg_gap);
            if (s > 0) {
                speed_base = s;
                Log.v(TAG, "avg_gap:" + avg_gap + " -> new speed_base: " + speed_base);
            }
        }

        for (int i = 0; i < ldata.size(); i++) {
            dbase = Math.abs(Math.abs(ldata.get(i)) - speed_base);
            dlong = Math.abs(Math.abs(ldata.get(i)) - 3 * speed_base);
            dvlong = Math.abs(Math.abs(ldata.get(i)) - 7 * speed_base);

            // approximate to the closest value
            closest = Math.min(Math.min(dbase, dlong), dvlong);
            sign = (ldata.get(i) > 0)? 1:-1;

            if (closest == dbase) {
                ldata.set(i, sign * speed_base);
            } else if (closest == dlong) {
                ldata.set(i, 3 * sign * speed_base);
            } else if (closest == dvlong) {
                ldata.set(i, 7 * sign * speed_base);
            }
        }

        signalData(ldata);
    }

    protected final void signalData(List<Long> ldata) {
        synchronized (str) {
            mMorse.updateValues(speed_base);
            str = mMorse.getText(ListToPrimitiveArray(ldata));
        }
        signalStr(mContext.mHandlerRecv, "data_message_text", StripString(str, 30));
        signalStr(mContext.mHandlerRecv, "data_message_morse", StripString(mMorse.getMorse(str), 30));

        String tmp = "";
        int q = 20, start = (ldata.size() > q)?(ldata.size() - q):0;
        for(int i=start; i<ldata.size(); i++){
            tmp += String.valueOf(ldata.get(i)) + " ";
        }
        signalStr(mContext.mHandlerRecv, "data_message_morse_times", tmp);
    }

    protected final void signalReset() {
        signalStr(mContext.mHandlerRecv, "data_message_text", "");
        signalStr(mContext.mHandlerRecv, "data_message_morse", "");
        signalStr(mContext.mHandlerRecv, "data_message_morse_times", "");
    }

    public synchronized String getCurrentText() {
        return StripString(str, 30);
    }

    public synchronized String getCurrentMorse() {
        return StripString(mMorse.getMorse(str), 30);
    }

    public synchronized void reset() {
        str = "";
        signalReset();
    }
}
