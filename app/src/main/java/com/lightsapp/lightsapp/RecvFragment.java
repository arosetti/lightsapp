package com.lightsapp.lightsapp;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class RecvFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "1";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx;

    private ScrollView mScrollViewMorse;
    private TextView mTextViewData;
    private TextView mTextViewMorse;
    private TextView mTextViewSensitivity;
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
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recv, container, false);

        mCtx = (MainActivity) getActivity();

        mTextViewData = (TextView) v.findViewById(R.id.TextViewRecv);
        mScrollViewMorse = (ScrollView) v.findViewById(R.id.scrollViewMorse);
        mTextViewMorse = (TextView) v.findViewById(R.id.TextViewMorse);
        mTextViewMorse.addTextChangedListener(new TextWatcher() {
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
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                mCtx.mFrameA.setSensitivity(progress);
                mTextViewSensitivity.setText(getResources().getString(R.string.sensitivity) +
                                             ": " + progress);
                if (mCtx.mFrameA.getAnalyzer()) {
                    setEmptyText();
                }
            }
        });
        mTextViewSensitivity = (TextView) v.findViewById(R.id.textViewSensitivity);
        mTextViewSensitivity.setText(getResources().getString(R.string.sensitivity) +
                ": " + mSeekBarSensitivity.getProgress());

        Button mButton = (Button) v.findViewById(R.id.ButtonReset);
        mButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        mCtx.mFrameA.reset();
                        if (!mCtx.mFrameA.getAnalyzer())
                            setDefaultText();
                        else {
                            setEmptyText();
                        }
                    }
                });

        mButtonRecv = (Button) v.findViewById(R.id.ButtonRecv);
        mButtonRecv.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (!(mCtx.hasCamera()  || mCtx.hasFrontCamera())) {
                            Toast toast = Toast.makeText(mCtx,
                                    "You need a camera to receive morse code.",
                                    Toast.LENGTH_LONG);
                            toast.show();
                            return;
                        }

                        if (mCtx.mCameraController == null)
                        {
                            Toast toast = Toast.makeText(mCtx,
                                    "Camera init failed! please report.",
                                    Toast.LENGTH_LONG);
                            toast.show();
                            return;
                        }

                        if (mButtonRecv.getText() ==
                                getResources().getString(R.string.btn_start) ) {
                            mCtx.mFrameA.reset();
                            mCtx.mFrameA.setAnalyzer(true);
                            mButtonRecv.setText(R.string.btn_stop);
                            setInitText();
                        }
                        else if (mButtonRecv.getText() ==
                                getResources().getString(R.string.btn_stop) ) {
                            mCtx.mFrameA.reset();
                            mCtx.mFrameA.setAnalyzer(false);
                            mButtonRecv.setText(R.string.btn_start);

                            if (((String) mTextViewData.getText()).equals(analyzerInfoText())) {
                                setDefaultText();
                            }
                        }
                    }
                });

        setDefaultText();

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (mTextViewData != null && msg.getData().containsKey("data_message_text")) {
                    mTextViewData.setText((String) msg.getData().get("data_message_text"));
                }

                if (mTextViewMorse != null && msg.getData().containsKey("data_message_morse")) {
                    mTextViewMorse.setText((String) msg.getData().get("data_message_morse"));
                }

                if (msg.getData().containsKey("setup_done")) {
                    int progress = mSeekBarSensitivity.getProgress();
                    mCtx.mFrameA.setSensitivity(progress);
                    mTextViewSensitivity.setText(getResources().getString(R.string.sensitivity) +
                                                 ": " + progress);
                    // now we can start  the analyzer
                    mCtx.mFrameA.start();
                    mCtx.mFrameA.activate();
                }
            }
        };

        mCtx.mHandlerRecv = mHandler;

        return v;
    }

    public void setDefaultText() {
        mTextViewMorse.setText("morse interval is " + mCtx.mMorse.get("SPEED_BASE") + "ms\n" +
                "lower the sensibility if needed");
        mTextViewData.setText("press start!");
    }

    private String analyzerInfoText() {
        return "using " + mCtx.mFrameA.getName() + " analyzer"; // TODO use string with format
    }

    public void setInitText() {
        mTextViewData.setText(analyzerInfoText());
        mTextViewMorse.setText("");
    }

    public void setEmptyText() {
        mTextViewMorse.setText("");
        mTextViewData.setText("");
    }
}

