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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class RecvFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "1";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx;

    private HorizontalScrollView mHScrollViewRecv, mHScrollViewRecvM, mHScrollViewRecvMT;
    private TextView mTextViewRecv, mTextViewRecvM, mTextViewRecvMT;
    private TextView mTextViewSensitivity;
    private SeekBar mSeekBarSensitivity;
    private RadioButton mRadioButtonLight, mRadioButtonSound;
    private RadioGroup mRadioGroupMode;
    private Button mButtonRecv;
    private LinearLayout layoutGraph;

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
        final View v = inflater.inflate(R.layout.fragment_recv, container, false);

        mCtx = (MainActivity) getActivity();

        layoutGraph = (LinearLayout) v.findViewById(R.id.linearLayoutGraph);

        mTextViewRecv = (TextView) v.findViewById(R.id.TextViewRecv);
        mTextViewRecvM = (TextView) v.findViewById(R.id.TextViewRecvM);
        mTextViewRecvMT = (TextView) v.findViewById(R.id.TextViewRecvMT);

        mHScrollViewRecv = (HorizontalScrollView) v.findViewById(R.id.horizontalScrollViewRecv);
        //mHScrollViewRecv.setSmoothScrollingEnabled(false);
        mHScrollViewRecvM = (HorizontalScrollView) v.findViewById(R.id.horizontalScrollViewRecvM);
        //mHScrollViewRecvM.setSmoothScrollingEnabled(false);
        mHScrollViewRecvMT = (HorizontalScrollView) v.findViewById(R.id.horizontalScrollViewRecvMT);
        //mHScrollViewRecvMT.setSmoothScrollingEnabled(false);

        mTextViewRecv.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                mHScrollViewRecv.fullScroll(HorizontalScrollView.FOCUS_RIGHT); //scrollTo(mTextViewRecv.getWidth(), 0);
                mHScrollViewRecvM.fullScroll(HorizontalScrollView.FOCUS_RIGHT); //scrollTo(mTextViewRecvM.getWidth(), 0);
                mHScrollViewRecvMT.fullScroll(HorizontalScrollView.FOCUS_RIGHT); //scrollTo(mTextViewRecvMT.getWidth(), 0);
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
                mCtx.mLightA.setSensitivity(progress);
                mTextViewSensitivity.setText(getResources().getString(R.string.sensitivity) +
                                             ": " + progress);
                if (mCtx.mLightA.getAnalyzer()) {
                    setEmptyText();
                }
            }
        });
        mTextViewSensitivity = (TextView) v.findViewById(R.id.textViewSensitivity);
        mTextViewSensitivity.setText(getResources().getString(R.string.sensitivity) +
                ": " + mSeekBarSensitivity.getProgress());

        mRadioGroupMode = (RadioGroup) v.findViewById(R.id.radioGroupMode);
        mRadioButtonLight = (RadioButton) v.findViewById(R.id.radioButtonLight);
        mRadioButtonSound = (RadioButton) v.findViewById(R.id.radioButtonSound);

        mRadioGroupMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioButtonLight) {
                    Log.d(TAG, "CHK light enabled!");
                    layoutGraph.removeAllViews();
                    layoutGraph.addView(mCtx.graphView_lum2);
                }
                else if (checkedId == R.id.radioButtonSound) {
                    Log.d(TAG, "CHK sound enabled!");
                    layoutGraph.removeAllViews();
                    layoutGraph.addView(mCtx.graphView_snd);
                }
                else
                    Log.d(TAG, "CHK ERROR");
            }
        });

        Button mButton = (Button) v.findViewById(R.id.ButtonReset);
        mButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (mRadioButtonLight.isChecked()) {
                            mCtx.mLightA.reset();
                            if (!mCtx.mLightA.getAnalyzer())
                                setDefaultText();
                            else {
                                setEmptyText();
                            }
                        }
                        else if (mRadioButtonSound.isChecked()) {

                        }
                    }
                });

        mButtonRecv = (Button) v.findViewById(R.id.ButtonRecv);
        mButtonRecv.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (mRadioButtonLight.isChecked()) {
                            if (!(mCtx.hasCamera()  || mCtx.hasFrontCamera())) {
                                Toast toast = Toast.makeText(mCtx,
                                        "You need a camera to receive morse code.",
                                        Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }

                            if (mCtx.mCameraController == null) {
                                Toast toast = Toast.makeText(mCtx,
                                        "Camera init failed! please report.",
                                        Toast.LENGTH_LONG);
                                toast.show();
                                return;
                            }

                            if (mButtonRecv.getText() ==
                                    getResources().getString(R.string.btn_start) ) {
                                mCtx.mLightA.reset();
                                mCtx.mLightA.setAnalyzer(true);
                                mButtonRecv.setText(R.string.btn_stop);
                                setInitText();
                            }
                            else if (mButtonRecv.getText() ==
                                    getResources().getString(R.string.btn_stop) ) {
                                mCtx.mLightA.reset();
                                mCtx.mLightA.setAnalyzer(false);
                                mButtonRecv.setText(R.string.btn_start);

                                if (((String) mTextViewRecv.getText()).equals(analyzerInfoText())) { // TODO BUGFIX crash!!! syncronized ?!
                                    setDefaultText();
                                }
                            }
                        }
                        else if (mRadioButtonSound.isChecked())
                        {

                        }
                    }
                });

        setDefaultText();

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (mTextViewRecv != null && msg.getData().containsKey("data_message_text")) {
                    mTextViewRecv.setText((String) msg.getData().get("data_message_text"));
                }

                if (mTextViewRecvM != null && msg.getData().containsKey("data_message_morse")) {
                    mTextViewRecvM.setText((String) msg.getData().get("data_message_morse"));
                }

                if (mTextViewRecvMT != null && msg.getData().containsKey("data_message_morse_times")) {
                    mTextViewRecvMT.setText((String) msg.getData().get("data_message_morse_times"));
                }

                if (msg.getData().containsKey("setup_done")) {
                    int progress = mSeekBarSensitivity.getProgress();
                    mCtx.mLightA.setSensitivity(progress);
                    mTextViewSensitivity.setText(getResources().getString(R.string.sensitivity) +
                                                 ": " + progress);
                    mCtx.mLightA.start();
                    mCtx.mLightA.activate();
                }

                if (msg.getData().containsKey("graph_setup_done")) {
                    if (mRadioButtonLight.isChecked())
                        layoutGraph.addView(mCtx.graphView_lum2);
                    else
                        layoutGraph.addView(mCtx.graphView_snd);
                }
            }
        };

        mCtx.mHandlerRecv = mHandler;

        return v;
    }

    public void setDefaultText() {
        mTextViewRecv.setText("press start!");
        mTextViewRecvM.setText("morse interval is " + mCtx.mMorse.get("SPEED_BASE") + "ms\n" +
                "lower the sensibility if needed");
    }

    private String analyzerInfoText() {
        return "using " + mCtx.mLightA.getName() + " analyzer"; // TODO use string with format
    }

    public void setInitText() {
        mTextViewRecv.setText(analyzerInfoText());
        mTextViewRecvM.setText("");
        mTextViewRecvMT.setText("");
    }

    public void setEmptyText() {
        mTextViewRecv.setText("");
        mTextViewRecvM.setText("");
        mTextViewRecvMT.setText("");
    }
}

