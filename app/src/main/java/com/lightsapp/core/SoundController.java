package com.lightsapp.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.lightsapp.core.analyzer.sound.SoundAnalyzer;
import com.lightsapp.ui.MainActivity;


public class SoundController {
    private final String TAG = SoundController.class.getSimpleName();
    private MainActivity mContext;

    public AudioRecord mAudioRec = null;

    int sampleRate = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSize;

    public SoundController(Context context) {
        mContext = (MainActivity) context;
        sampleRate = Integer.valueOf(mContext.mPrefs.getString("sample_freq", "8000"));
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding);
    }

    public void setup() {
        try {
            mAudioRec = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                        sampleRate, channelConfiguration, audioEncoding,
                                        bufferSize);
            mContext.mSoundController.mAudioRec.startRecording();
        }
        catch (Exception e) {
            Log.e(TAG, "Audio recording setup failed: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            mContext.mSoundA = new SoundAnalyzer(mContext);
        }
        catch (Exception e) {
            Log.e(TAG, "Error starting SoundAnalyzer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void release() {
        Log.d(TAG, "Release audio recorder");
        if (mAudioRec != null) {
            mAudioRec.stop();
            mAudioRec.release();
        }
    }
}
