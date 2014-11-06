package com.lightsapp.camera.FrameAnalyzer;

import android.content.Context;
import android.util.Log;

import com.lightsapp.utils.LinearFilter;

import java.util.ArrayList;
import java.util.List;

import static com.lightsapp.camera.FrameAnalyzer.DerivativeFrameAnalyzer.StatusCode.*;
import static com.lightsapp.utils.HandlerUtils.*;

public class DerivativeFrameAnalyzer extends FrameAnalyzer {
    enum StatusCode{SEARCH_HIGH, SET_DATA, SEARCH_LOW, SET_GAP}
    boolean smooth = false;

    public DerivativeFrameAnalyzer(Context context){
        super(context);
        NAME = "derivative";
    }

    @Override
    public void analyze() {
        if ((lframes.size()) < 2) {
            return;
        }

        List<Long> ldata = new ArrayList<Long>();

        // TODO optimize and compute incrementally.
        List<Long> lframes_d = new ArrayList<Long>();
        for (int i = 1; i < lframes.size(); i++) {
              long dd = (long) (lframes.get(i).luminance - lframes.get(i - 1).luminance);
              lframes_d.add(dd);
        }

        Log.w(TAG, "START!");

        long tstart = 0, tstop = 0, diff;
        long fmax = Long.MIN_VALUE;
        long fmin = Long.MAX_VALUE;
        int fmax_id = 0, fmin_id = 0;
        boolean found = false;

        StatusCode statcode = SEARCH_HIGH;

        for (int i = 0; i < (lframes_d.size() - 1); i++) {
            switch (statcode) {
                case SEARCH_HIGH:
                    if ((lframes_d.get(i) > sensitivity)) {
                        if (lframes_d.get(i) > fmax)
                        {
                            fmax = lframes_d.get(i);
                            fmax_id = i;
                            found = true;
                            Log.w(TAG, "new max");
                        }
                        continue;
                    }

                    if (found) {
                        Log.w(TAG, "maxid: " + fmax_id);
                        found = false;
                        fmax = Long.MIN_VALUE;
                        tstart = lframes.get(fmax_id).timestamp;
                        if (tstop == 0) {
                            statcode = SEARCH_LOW;
                            Log.w(TAG, "Searching low front");
                        }
                        else {
                            statcode = SET_GAP;
                            Log.w(TAG, "Setting gap");
                        }
                    }
                    break;

                case SEARCH_LOW:
                    if ((lframes_d.get(i) < -sensitivity)) {
                        if (lframes_d.get(i) < fmin)
                        {
                            fmin = lframes_d.get(i);
                            fmin_id = i;
                            found = true;
                            Log.w(TAG, "new min");
                        }
                        continue;
                    }

                    if (found) {
                        Log.w(TAG, "minid: " + fmin_id);
                        found = false;
                        fmin = Long.MAX_VALUE;
                        tstop = lframes.get(fmin_id).timestamp;
                        statcode = SET_DATA;
                        Log.w(TAG, "Setting data");
                    }
                    break;

                case SET_DATA:
                    diff = tstop - tstart;

                    Log.w(TAG, "data diff: " + diff);
                    if (diff > (long) (0.6 * (float) speed_base)) {
                        ldata.add(new Long(diff));
                        statcode = SEARCH_HIGH;
                        Log.w(TAG, "Searching high front for the gap end");
                    }
                    else {
                        statcode = SEARCH_LOW;
                        Log.w(TAG, "Skip short data frame, go back to search the real low front");
                    }
                    break;

                case SET_GAP:
                    diff = tstart - tstop;

                    Log.w(TAG, "gap diff: " + diff);
                    if (diff > (long) (0.6 * (float) speed_base)) {
                        ldata.add(new Long(-diff));
                        statcode = SEARCH_LOW;
                        Log.w(TAG, "Searching low front for the data end");
                    }
                    else {
                        statcode = SEARCH_HIGH;
                        Log.w(TAG, "Skip short gap frame, go back to search the real high front");
                    }
                    break;
            }
        }

        if (ldata.size() > 0)
            endAnalyze(ldata);
    }
}
