package com.lightsapp.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.lightsapp.lightsapp.MainActivity;

public class SoundController {
    private final String TAG = SoundController.class.getSimpleName();
    private MainActivity mContext;
    public AudioRecord mAudioRec;

    int sampleRate = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    public SoundController(Context context) {
        mContext = (MainActivity) context;
    }

    public AudioRecord setup() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                    channelConfiguration, audioEncoding);
            mAudioRec = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, sampleRate,
                    channelConfiguration, audioEncoding, bufferSize);
            mAudioRec.startRecording();
            Log.v(TAG, "Start Recording");
        } catch (Throwable t) {
            Log.e(TAG, "Recording Failed");
        }
        finally {
            return mAudioRec;
        }
    }
}
