package com.lightsapp.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lightsapp.lightsapp.R;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class RecvFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "1";
    private static final String TAG = RecvFragment.class.getSimpleName();

    private MainActivity mContext;
    private Lock lock;

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
        mContext = (MainActivity) getActivity();
        lock = new ReentrantLock(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_recv, container, false);

        mContext = (MainActivity) getActivity();

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
                mContext.mLightA.setSensitivity(progress);
                mContext.mSoundA.setSensitivity(progress);
                mTextViewSensitivity.setText(getResources().getString(R.string.sensitivity) +
                                             ": " + progress);
                if (mContext.mLightA.getAnalyzer()) {
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
                    layoutGraph.addView(mContext.graphView_lum2);
                }
                else if (checkedId == R.id.radioButtonSound) {
                    Log.d(TAG, "CHK sound enabled!");
                    layoutGraph.removeAllViews();
                    layoutGraph.addView(mContext.graphView_snd);
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
                            mContext.mLightA.reset();
                            if (!mContext.mLightA.getAnalyzer())
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
                        mRadioGroupMode.setEnabled(true);
                        mRadioButtonLight.setEnabled(true);
                        mRadioButtonSound.setEnabled(true);

                        /*if (!(mContext.hasCamera()  || mContext.hasFrontCamera())) {
                            Toast toast = Toast.makeText(mContext,
                                    "You need a camera to receive morse code.",
                                    Toast.LENGTH_LONG);
                            toast.show();
                            return;
                        }

                        if (mContext.mCameraController == null) {
                            Toast toast = Toast.makeText(mContext,
                                    "Camera init failed! please report.",
                                    Toast.LENGTH_LONG);
                            toast.show();
                            return;
                        }*/

                        if (mButtonRecv.getText() ==
                                getResources().getString(R.string.btn_start)) {
                            //mContext.mLightA.reset();
                            if (mRadioButtonLight.isChecked())
                                mContext.mLightA.setAnalyzer(true);
                            else if (mRadioButtonSound.isChecked())
                                mContext.mSoundA.setAnalyzer(true);
                            mButtonRecv.setText(R.string.btn_stop);
                            setInitText();
                            mRadioGroupMode.setEnabled(false);
                            mRadioButtonLight.setEnabled(false);
                            mRadioButtonSound.setEnabled(false);
                        } else if (mButtonRecv.getText() ==
                                getResources().getString(R.string.btn_stop)) {
                            //mContext.mLightA.reset();
                            if (mRadioButtonLight.isChecked())
                                mContext.mLightA.setAnalyzer(false);
                            else if (mRadioButtonSound.isChecked())
                                mContext.mSoundA.setAnalyzer(false);
                            mButtonRecv.setText(R.string.btn_start);

                            lock.lock();
                            SpannableString spstr = new SpannableString(mTextViewRecv.getText());
                            String str = Html.toHtml(spstr).toString();

                            if (str.equals(analyzerInfoText())) {
                                setDefaultText();
                            }
                            lock.unlock();
                        }
                }
        });

        setDefaultText();

        return v;
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume recv fragment");
        super.onResume();

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (mTextViewRecv != null && msg.getData().containsKey("data_message_text")) {
                    lock.lock();
                    mTextViewRecv.setText((String) msg.getData().get("data_message_text"));
                    lock.unlock();
                }

                if (mTextViewRecvM != null && msg.getData().containsKey("data_message_morse")) {
                    mTextViewRecvM.setText((String) msg.getData().get("data_message_morse"));
                }

                if (mTextViewRecvMT != null && msg.getData().containsKey("data_message_morse_times")) {
                    mTextViewRecvMT.setText((String) msg.getData().get("data_message_morse_times"));
               }

                if (msg.getData().containsKey("setup_done")) {
                    Log.d(TAG, "setup_done received!");
                    int progress = mSeekBarSensitivity.getProgress();
                    mContext.mLightA.setSensitivity(progress);
                    mContext.mSoundA.setSensitivity(progress);
                    mTextViewSensitivity.setText(getResources().getString(R.string.sensitivity) +
                            ": " + progress);
                    mContext.mLightA.start();
                    mContext.mLightA.activate();
                    mContext.mSoundA.start();
                    mContext.mSoundA.activate();
                }

                if (msg.getData().containsKey("graph_setup_done")) {
                    Log.d(TAG, "graph_setup_done received!");
                    layoutGraph.removeAllViews();
                    if (mRadioButtonLight.isChecked())
                        layoutGraph.addView(mContext.graphView_lum2);
                    else
                        layoutGraph.addView(mContext.graphView_snd);
                }
            }
        };

        mContext.mHandlerRecv = mHandler;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause recv fragment");
        if(mContext.mLightA != null)
            mContext.mLightA.stop();
        mContext.mHandlerRecv = null;
    }

    public void setDefaultText() {
        mTextViewRecv.setText("press start!");
        mTextViewRecvM.setText("morse interval is " + mContext.mMorse.get("SPEED_BASE") + "ms\n" +
                "" +
                "lower the sensibility if needed");
        mTextViewRecvMT.setText("");
    }

    private String analyzerInfoText() {
        return "using " + mContext.mLightA.getName() + " analyzer";
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

