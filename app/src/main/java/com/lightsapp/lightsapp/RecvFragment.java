package com.lightsapp.lightsapp;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class RecvFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "1";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx;

    private TextView mTextViewMessageData;
    private SeekBar mSeekBarSensitivity;
    private Button mButtonRecv;

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

        mPreview = (FrameLayout) v.findViewById(R.id.camera_preview);
        mPreview.addView(new SurfaceView(getActivity()), 0);   // BLACK MAGIC avoids black flashing.

        mTextViewMessageData = (TextView) v.findViewById(R.id.txt_rx);
        mTextViewMessageData.setText("***");

        mSeekBarSensitivity = (SeekBar) v.findViewById(R.id.seekBarSensitivity);
        mSeekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                // TODO Auto-generated method stub
                mCtx.mCameraController.setSensitivity(progress);
            }
        });

        Button mButton = (Button) v.findViewById(R.id.button_reset);
        mButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        mCtx.mCameraController.reset();
                    }
                });


        mButtonRecv = (Button) v.findViewById(R.id.button_recv);
        mButtonRecv.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (mButtonRecv.getText() ==
                                getResources().getString(R.string.btn_start) ) {
                            mCtx.mCameraController.startAnalyzer();
                            mButtonRecv.setText(R.string.btn_stop);
                        }
                        else if (mButtonRecv.getText() ==
                                getResources().getString(R.string.btn_stop) ) {
                            mCtx.mCameraController.stopAnalyzer();
                            mButtonRecv.setText(R.string.btn_start);
                        }
                    }
                });


        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (mTextViewMessageData != null && msg.getData().containsKey("data_message")) {
                    mTextViewMessageData.setText((String) msg.getData().get("data_message"));
                }

                if (msg.getData().containsKey("setup_done")) {
                    mPreview.removeAllViews();
                    mPreview.addView(mCtx.mCameraController);
                    Log.v(TAG, "init camera preview done");
                }

                if ( msg.getData().containsKey("set_sensitivity")) {
                    mCtx.mCameraController.setSensitivity(mSeekBarSensitivity.getProgress());
                }
            }
        };

        mCtx.mHandlerRecv = mHandler;

        return v;
    }
}