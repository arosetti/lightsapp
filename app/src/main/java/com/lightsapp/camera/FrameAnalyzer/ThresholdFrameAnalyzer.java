package com.lightsapp.camera.FrameAnalyzer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ThresholdFrameAnalyzer extends FrameAnalyzer {

    public ThresholdFrameAnalyzer(Context context, Handler handler){
        super(context, handler);
    }

    @Override
    public void analyze() {
        if ((lframes.size() - start_frame) < 2) {
            myHandler.signalStr("data_message", "<threshold algorithm>");
            return;
        }

        // search for a possible start of the transmission
        /*if (start_frame == 0) {
            for (int i = start_frame; i < (lframes.size() - 1); i++) {
                if ((lframes.get(i).luminance * 2) < lframes.get(i + 1).luminance) {
                    start_frame = i;
                    myHandler.signalStr("data_message", "start_frame: " + start_frame);
                }
            }
            return;
        }*/

        int dsum = 0;
        List<Long> ldata = new ArrayList<Long>();

        int m_sensitivity = sensitivity;
        boolean light = false;
        for (int i = start_frame; i < (lframes.size() - 1); i++) {
            if (i == 0 && lframes.get(i).luminance > m_sensitivity) {
                    light = true;
            }

            Log.v(TAG, "Luminance " + lframes.get(i).luminance);

            if (light) {
                if (lframes.get(i).luminance > m_sensitivity) {
                        dsum += lframes.get(i).delta;
                }
                else {
                        dsum += lframes.get(i).delta;
                        ldata.add(new Long(dsum));
                        light = false;
                        dsum = 0;
                }
            }
            else {
                if (lframes.get(i).luminance < m_sensitivity) {
                        dsum += lframes.get(i).delta;
                }
                else {
                        dsum += lframes.get(i).delta;
                        ldata.add(new Long(-dsum));
                        light = true;
                        dsum = 0;
                }
            }
    }

        endAnalyze(ldata);
    }
}
