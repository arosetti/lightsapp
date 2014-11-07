package com.lightsapp.camera.FrameAnalyzer;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.lightsapp.utils.HandlerUtils.*;


public class ThresholdFrameAnalyzer extends FrameAnalyzer {

    public ThresholdFrameAnalyzer(Context context){
        super(context);
        NAME = "threshold";
    }

    @Override
    public void analyze() {
        if (lframes.size() < 2) {
            return;
        }

        int dsum = 0;
        List<Long> ldata = new ArrayList<Long>();

        int m_sensitivity = sensitivity;
        boolean light = false;

        for (int i = 0; i < lframes.size(); i++) {
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
