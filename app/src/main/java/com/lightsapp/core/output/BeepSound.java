package com.lightsapp.core.output;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class BeepSound {
    private final String TAG = BeepSound.class.getSimpleName();

    private AudioTrack audioTrack = null;

    private byte data[];
    private int sample_rate = 8000, samples;
    public final int duration, frequency;

    public BeepSound(int duration, int frequency) {

        this.duration = duration;
        this.frequency = frequency;

        genTone();
    }

    private void genTone() {
        samples = (int) Math.ceil(((double) duration / 1000) * sample_rate);
        double sample[] = new double[samples];
        data = new byte[2 * samples];

        for (int i = 0; i < samples; ++i) {
            sample[i] = Math.sin(frequency * 2 * Math.PI * i / (sample_rate));
        }

        int idx = 0;
        int i = 0;
        int ramp = samples / 20;

        for (i = 0; i < ramp; ++i) {
            double dVal = sample[i];
            final short val = (short) ((dVal * 32767 * i / ramp));
            data[idx++] = (byte) (val & 0x00ff);
            data[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        for (i = i; i < samples - ramp; ++i) {
            double dVal = sample[i];
            final short val = (short) ((dVal * 32767));
            data[idx++] = (byte) (val & 0x00ff);
            data[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        for (i = i; i < samples; ++i) {
            double dVal = sample[i];
            final short val = (short) ((dVal * 32767 * (samples - i) / ramp));
            data[idx++] = (byte) (val & 0x00ff);
            data[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sample_rate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
                data.length, AudioTrack.MODE_STATIC);
        audioTrack.write(data, 0, data.length);
    }

    public void play() {
        try {
            if (audioTrack == null)
                genTone();
            else {
                audioTrack.release();
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sample_rate,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        data.length, AudioTrack.MODE_STATIC);
                audioTrack.write(data, 0, data.length);
            }
            audioTrack.play();
        }
        catch (Exception e) {
            Log.v(TAG, "beep error: " + e.getMessage());
        }
    }
}
