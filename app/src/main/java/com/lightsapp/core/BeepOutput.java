package com.lightsapp.core;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class BeepOutput {
    private final String TAG = BeepOutput.class.getSimpleName();
    AudioTrack audioTrack = null;
    private byte sound_data[];
    private int sample_rate = 8000;
    private int samples;

    public void genTone(double duration, double freq) {
        double dsamples = duration * sample_rate;
        dsamples = Math.ceil(dsamples);
        samples = (int) dsamples;
        double sample[] = new double[samples];
        sound_data = new byte[2 * samples];

        for (int i = 0; i < samples; ++i) {
            sample[i] = Math.sin(freq * 2 * Math.PI * i / (sample_rate));
        }

        int idx = 0;
        int i = 0;
        int ramp = samples / 20;

        for (i = 0; i < ramp; ++i) {
            double dVal = sample[i];
            final short val = (short) ((dVal * 32767 * i / ramp));
            sound_data[idx++] = (byte) (val & 0x00ff);
            sound_data[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        for (i = i; i < samples - ramp; ++i) {
            double dVal = sample[i];
            final short val = (short) ((dVal * 32767));
            sound_data[idx++] = (byte) (val & 0x00ff);
            sound_data[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        for (i = i; i < samples; ++i) {
            double dVal = sample[i];
            final short val = (short) ((dVal * 32767 * (samples - i) / ramp));
            sound_data[idx++] = (byte) (val & 0x00ff);
            sound_data[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    public void playSound() {
        try {
            if (audioTrack != null)
                audioTrack.release();
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sample_rate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, (int) samples * 2,
                    AudioTrack.MODE_STATIC);
            audioTrack.write(sound_data, 0, sound_data.length);
            audioTrack.play();
        }
        catch (Exception e) {
            Log.v(TAG, "Error: " + e);
        }

        /* int x =0;
        do {
            if (audioTrack != null)
                x = audioTrack.getPlaybackHeadPosition();
            else
                x = samples;
        } while (x<samples);

        if (audioTrack != null) audioTrack.release(); */
    }
}