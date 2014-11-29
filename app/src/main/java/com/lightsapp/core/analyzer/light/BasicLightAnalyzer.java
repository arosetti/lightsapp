package com.lightsapp.core.analyzer.light;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import static com.lightsapp.utils.HandlerUtils.*;

public class BasicLightAnalyzer extends LightAnalyzer {
    private int start_frame = 0;

    public BasicLightAnalyzer(Context context){
        super(context);
        NAME = "basic";
    }

    @Override
    public void reset() {
        super.reset();
        start_frame = 0;
    }

    @Override
    public void analyze() {
        if ((lframes.size() - start_frame) < 2) {
            signalStr(mContext.mHandlerRecv, "data_message_text", "<basic algorithm>");
            return;
        }

        // search for a possible start of the transmission
        if (start_frame == 0) {
            for (int i = start_frame; i < (lframes.size() - 1); i++) {
                if ( ((float) lframes.get(i).luminance *
                     ((float) sensitivity / 25f)) < lframes.get(i + 1).luminance ) {
                    start_frame = i;
                    signalStr(mContext.mHandlerRecv, "data_message_text", "start_frame: " + start_frame);
                }
            }
            return;
        }

        int dsum = 0;
        int n = 0;
        List<Long> ldata = new ArrayList<Long>();

        for (int i = start_frame; i < (lframes.size() - 1); i++) {
            long lcur = lframes.get(i).luminance;
            long lnext = lframes.get(i + 1).luminance;
            long ldiff = Math.abs(lcur - lnext);

            // add to counter if signal does not change too much
            // add new element list on change and reset counter.
            if (ldiff < (lcur / 2)) {
                dsum += lframes.get(i).delta;
            }
            else {
                dsum += lframes.get(i).delta;
                long sign = ((n % 2) == 0) ? 1:-1;

                if (dsum > 8 * mContext.mMorseA.getSpeedBase()) {
                    reset();
                    return;
                }

                if (dsum > (mContext.mMorseA.getSpeedBase() / 3)) {
                    ldata.add(new Long(sign * dsum));
                    n++;
                }
                dsum = 0;
            }
        }

        mContext.mMorseA.analyze(ldata);
    }
}
