package com.lightsapp.camera.FrameAnalyzer;

import android.content.Context;
import android.os.Handler;

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

        // TODO *

        //endAnalyze(ldata);
    }
}
