package com.lightsapp.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.lightsapp.lightsapp.MainActivity;

public class SoundController {
    private final String TAG = SoundController.class.getSimpleName();
    private MainActivity mCtx;
    public AudioRecord mAudioRec;

    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    public SoundController(Context context) {
        mCtx = (MainActivity) context;
    }

    public AudioRecord setup() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);
            mAudioRec = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, frequency,
                    channelConfiguration, audioEncoding, bufferSize);
            mAudioRec.startRecording();
        } catch (Throwable t) {
            Log.e(TAG, "Recording Failed");
        }
        finally {
            return mAudioRec;
        }
    }

}
