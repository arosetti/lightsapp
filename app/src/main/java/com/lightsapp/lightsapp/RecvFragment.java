package com.lightsapp.lightsapp;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class RecvFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "1";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx;

    private ScrollView mScrollViewMorse;
    private TextView mTextViewMessageData;
    private TextView mTextViewMessageMorse;
    private SeekBar mSeekBarSensitivity;
    private Button mButtonRecv;

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



        mTextViewMessageData = (TextView) v.findViewById(R.id.txt_rx);
        mTextViewMessageData.setText("press start");// TODO string res

        mScrollViewMorse = (ScrollView) v.findViewById(R.id.scrollView_morse);
        mTextViewMessageMorse = (TextView) v.findViewById(R.id.txt_morse);
        mTextViewMessageMorse.setText("...");
        mTextViewMessageMorse.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                mScrollViewMorse.fullScroll(ScrollView.FOCUS_DOWN);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
        });

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
                        mTextViewMessageData.setText("***");
                        mTextViewMessageMorse.setText("");
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

                if (mTextViewMessageData != null && msg.getData().containsKey("data_message_text")) {
                    mTextViewMessageData.setText((String) msg.getData().get("data_message_text"));
                }

                if (mTextViewMessageMorse != null && msg.getData().containsKey("data_message_morse")) {
                    mTextViewMessageMorse.setText((String) msg.getData().get("data_message_morse"));
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