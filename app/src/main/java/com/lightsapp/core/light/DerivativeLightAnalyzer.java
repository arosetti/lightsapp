package com.lightsapp.core.light;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.lightsapp.core.light.DerivativeLightAnalyzer.StatusCode.*;

public class DerivativeLightAnalyzer extends LightAnalyzer {
    enum StatusCode{SEARCH_HIGH, SET_DATA, SEARCH_LOW, SET_GAP}
    boolean smooth = false;
    float min_fraction = 0.6f;
    long  min_time = 100;

    public DerivativeLightAnalyzer(Context context){
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

        Log.w(TAG, "Start!");

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
                        }
                        continue;
                    }

                    if (found) {
                        found = false;
                        fmax = Long.MIN_VALUE;
                        tstart = lframes.get(fmax_id).timestamp;
                        if (tstop == 0) {
                            statcode = SEARCH_LOW;
                        }
                        else {
                            statcode = SET_GAP;
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
                        }
                        continue;
                    }

                    if (found) {
                        found = false;
                        fmin = Long.MAX_VALUE;
                        tstop = lframes.get(fmin_id).timestamp;
                        statcode = SET_DATA;
                    }
                    break;

                case SET_DATA:
                    diff = tstop - tstart;

                    Log.d(TAG, "data " + diff + "ms");
                    if (mContext.mMorseA.getAutoInterval() ? (diff > (min_time)):
                        (diff > (long) (min_fraction * (float) mContext.mMorseA.getSpeedBase())) ) {
                        ldata.add(new Long(diff));
                        statcode = SEARCH_HIGH;
                    }
                    else {
                        statcode = SEARCH_LOW;
                    }
                    break;

                case SET_GAP:
                    diff = tstart - tstop;

                    Log.d(TAG, "gap  " + diff + "ms");
                    if (mContext.mMorseA.getAutoInterval() ? (diff > (min_time)):
                        (diff > (long) (min_fraction * (float) mContext.mMorseA.getSpeedBase())) ) {
                        ldata.add(new Long(-diff));
                        statcode = SEARCH_LOW;
                    }
                    else {
                        statcode = SEARCH_HIGH;
                    }
                    break;
            }
        }

        if (ldata.size() > 0)
            mContext.mMorseA.analyze(ldata);
    }
}
