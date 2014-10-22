package com.lightsapp.camera.FrameAnalyzer;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public class BasicFrameAnalyzer extends FrameAnalyzer {

    public BasicFrameAnalyzer(Context context, Handler handler){
        super(context, handler);
    }

    @Override
    public void analyze() {
        if ((lframes.size() - start_frame) < 2) {
            myHandler.signalStr("data_message", "<basic algorithm>");
            return;
        }

        // search for a possible start of the transmission
        if (start_frame == 0) {
            for (int i = start_frame; i < (lframes.size() - 1); i++) {
                if ((lframes.get(i).luminance * 2) < lframes.get(i + 1).luminance) {
                    start_frame = i;
                    myHandler.signalStr("data_message", "start_frame: " + start_frame);
                }
            }
            return;
        }

        int dsum = 0;
        List<Long> ldata = new ArrayList<Long>();

        for (int i = start_frame; i < (lframes.size() - 1); i++) {
            long lcur = lframes.get(i).luminance;
            long lnext = lframes.get(i + 1).luminance;
            long ldiff = Math.abs(lcur - lnext);

            // add to counter if signal does not change too much
            // add new element list on change and reset counter.
            if (ldiff < (lcur / 2)) {
                dsum += lframes.get(i).delta;
            } else { //else if (ldiff > lcur) {
                dsum += lframes.get(i).delta;
                ldata.add(new Long(dsum));
                dsum = 0;
            }
        }

        endAnalyze(ldata);
    }
}
