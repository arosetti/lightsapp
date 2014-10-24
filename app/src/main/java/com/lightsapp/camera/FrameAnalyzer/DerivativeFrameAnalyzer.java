package com.lightsapp.camera.FrameAnalyzer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DerivativeFrameAnalyzer extends FrameAnalyzer {

    public DerivativeFrameAnalyzer(Context context){
        super(context);
    }

    @Override
    public void analyze() {
        if ((lframes.size() - start_frame) < 2) {
            myHandler.signalStr("data_message", "<derivative algorithm>");
            return;
        }

        List<Long> ldata = new ArrayList<Long>();
        List<Long> lframes_d = new ArrayList<Long>();

        long tstart = 0, tstop = 0, diff;

        // TODO optimize and compute incrementally, use, last_diff and add new frames.
        for (int i = start_frame + 1; i < lframes.size(); i++) {
              long dd = lframes.get(i).luminance - lframes.get(i - 1).luminance;
              lframes_d.add(dd);
              long last = lframes_d.get(lframes_d.size() - 1);
        }

        Log.w(TAG, "START!");

        for (int i = 0; i < (lframes_d.size() - 1); i++) {
            // TODO use the avg/max/min values to adjust the thresholds
            if ( (tstop == 0) && (tstart == 0) && (lframes_d.get(i) > sensitivity)) { //(((float) sensitivity / (float) 100) * avg)) ) {
                tstart = lframes.get(i + start_frame).timestamp;
                Log.w(TAG, "1) up");
                continue;
            }

            if (tstart != 0 && tstop == 0) {
                diff = lframes.get(i + start_frame).timestamp - tstart;

                if (diff > (8 * speed_base)) {
                    Log.w(TAG, "too long high signal");
                    tstart = 0;
                    continue;
                }

                if (lframes_d.get(i) <  -sensitivity) {
                    Log.w(TAG, "2) down");
                    if (diff > (long) (0.6 * (float) speed_base)) {
                        ldata.add(new Long(diff));
                        tstop = lframes.get(i + start_frame).timestamp;
                        tstart = 0;
                        continue;
                    }
                    else
                        Log.w(TAG, "skip short frame");
                }
            }

            if (tstop != 0 && tstart == 0) {
                diff = lframes.get(i + start_frame).timestamp - tstop;

                if (diff > (8 * speed_base)) {
                    Log.w(TAG, "too long low signal");
                    tstop = 0;
                    continue;
                }

                if ((lframes_d.get(i) > sensitivity)) {
                    Log.w(TAG, "3) up");
                    if (diff > (long) (0.6 * (float) speed_base)) {
                        ldata.add(new Long(-diff));
                        tstop = 0;
                        tstart = lframes.get(i + start_frame).timestamp;
                    }
                    else
                        Log.w(TAG, "skip short frame");
                }
            }
        }

        if (ldata.size() > 0)
            endAnalyze(ldata);
    }
}
