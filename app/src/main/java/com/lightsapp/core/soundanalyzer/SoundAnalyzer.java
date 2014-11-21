package com.lightsapp.core.soundanalyzer;

import android.content.Context;

import com.lightsapp.lightsapp.MainActivity;
import com.lightsapp.utils.MyRunnable;

import java.util.ArrayList;
import java.util.List;

public class SoundAnalyzer extends MyRunnable {
    protected final String TAG = SoundAnalyzer.class.getSimpleName();
    protected String NAME = "???";

    protected MainActivity mCtx;

    protected List<Frame> sframes;
    protected List<Frame> sframes_tmp;

    protected SoundAnalyzer(Context context) {
        super(true);

        mCtx = (MainActivity) context;

        sframes = new ArrayList<Frame>();
        sframes_tmp = new ArrayList<Frame>();
    }

}