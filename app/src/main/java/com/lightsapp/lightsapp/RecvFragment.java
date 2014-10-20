package com.lightsapp.lightsapp;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.lightsapp.camera.CameraController;
import com.lightsapp.morse.MorseConverter;

public class RecvFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "1";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx;

    private TextView mTextViewMessageStatus;
    private TextView mTextViewMessageData;
    FrameLayout mPreview;

    public Handler mHandler;

    public static RecvFragment newInstance(int sectionNumber) {
        RecvFragment fragment = new RecvFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public RecvFragment() {
        mCtx = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recv, container, false);

        mCtx = (MainActivity) getActivity();

        mTextViewMessageStatus = (TextView) v.findViewById(R.id.txt_status);
        mTextViewMessageStatus.setText("idle");

        mTextViewMessageData = (TextView) v.findViewById(R.id.txt_rx);
        mTextViewMessageData.setText("***");

        mPreview = (FrameLayout) v.findViewById(R.id.camera_preview);
        mPreview.addView(new SurfaceView(getActivity()), 0);   // BLACK MAGIC avoids black flashing.

        Button mButton = (Button)v.findViewById(R.id.button_reset);
        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        mCtx.mCameraController.reset();
                    }
                });


        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.getData().containsKey("update")) {
                    if (mCtx.mHandlerGraph != null) {
                        Message msgx = mCtx.mHandlerGraph.obtainMessage();
                        msgx.copyFrom(msg);
                        mCtx.mHandlerGraph.sendMessage(msgx); // lol forward
                    }
                }

                if (mTextViewMessageStatus != null && msg.getData().containsKey("info_message")) {
                    mTextViewMessageStatus.setText((String) msg.getData().get("info_message"));
                }

                if (mTextViewMessageData != null && msg.getData().containsKey("data_message")) {
                    mTextViewMessageData.setText((String) msg.getData().get("data_message"));
                }

                if (msg.getData().containsKey("setup_done")) {
                    if (mCtx.mCamera == null)
                        Log.e(TAG, "camera is null");
                    mCtx.mCameraController = new CameraController(mCtx,
                            mCtx.mCamera,
                            mHandler,
                            Integer.parseInt(mCtx.mPrefs.getString("speed", "500")));
                    mPreview.removeAllViews();
                    mPreview.addView(mCtx.mCameraController);
                    Log.v(TAG, "init camera preview");
                }
            }
        };

        mCtx.mHandlerRecv = mHandler;

        return v;
    }
}