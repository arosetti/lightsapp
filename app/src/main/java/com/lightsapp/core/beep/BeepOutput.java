package com.lightsapp.core.beep;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BeepOutput {
    private final String TAG = BeepOutput.class.getSimpleName();

    private List<BeepSound> buffer;

    public BeepOutput()
    {
        buffer = new ArrayList<>();
    }

    public void play(int duration, int frequency) {
        boolean done = false;

        for(BeepSound b: buffer) {
            if (b.duration == duration && b.frequency == frequency)
            {
                b.play();
                done = true;
            }
        }

        if (!done) {
            BeepSound b = new BeepSound(duration,frequency);
            b.play();
            buffer.add(b);
        }
    }
}