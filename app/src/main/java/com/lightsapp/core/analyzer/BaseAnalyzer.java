package com.lightsapp.core.analyzer;

import android.content.Context;
import android.util.Log;

import com.lightsapp.ui.MainActivity;
import com.lightsapp.utils.MyRunnable;

import java.util.concurrent.atomic.AtomicReference;


public class BaseAnalyzer extends MyRunnable {
    protected final String TAG = BaseAnalyzer.class.getSimpleName();
    protected String NAME = "???";

    protected MainActivity mContext;

    protected int sleep_time = 100,
                  sensitivity = -1;
    protected long timestamp_last;

    protected AtomicReference<Boolean> enable_analyze;

    protected BaseAnalyzer(Context context) {
        super(true);

        mContext = (MainActivity) context;

        enable_analyze = new AtomicReference<Boolean>(false);
    }

    public final String getName() {
        return NAME;
    }

    public void setAnalyzer(boolean val) {
        enable_analyze.getAndSet(val);
    }

    public boolean getAnalyzer() {
        return enable_analyze.get();
    }

    // to be overridden
    protected void analyze() {

    }

    public final void setSensitivity(int sensitivity) {
        Log.v(TAG, "Sensitivity set to " + sensitivity);
        this.sensitivity = sensitivity;
    }
}