package com.lightsapp.lightsapp;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static com.lightsapp.utils.Utils.*;


public class SendFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "0";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx;

    private ImageView mImageViewLightbulb;
    private Drawable lightbulb_on, lightbulb_off;

    private EditText mEdit;
    private Button mButtonSend;
    private TextView mTextViewMessage, mTextViewMorse;
    private CheckBox mCheckBoxSound, mCheckBoxLight;

    public Handler mHandler;

    public static SendFragment newInstance(int sectionNumber) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public SendFragment() {
        mCtx = (MainActivity) getActivity();
    }

    public void resetText() {
        mTextViewMessage.setText("morse interval is " + mCtx.mMorse.get("SPEED_BASE") + "ms");
        mTextViewMorse.setText(mCtx.mMorse.getMorse(CleanString(mEdit.getText().toString())));
    }

    @Override
    public void onResume() {
        super.onResume();
        resetText();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_send, container, false);

        mCtx = (MainActivity) getActivity();

        mImageViewLightbulb = (ImageView) v.findViewById(R.id.ImageViewLightbulb);
        lightbulb_off = getResources().getDrawable(R.drawable.lightbulb_off);
        lightbulb_on = getResources().getDrawable(R.drawable.lightbulb_on);

        mTextViewMessage = (TextView) v.findViewById(R.id.TextViewStatus);

        mEdit = (EditText) v.findViewById(R.id.EditViewSend);
        mEdit.setText(mCtx.mPrefs.getString("default_text", "sos"));

        mTextViewMorse = (TextView) v.findViewById(R.id.TextViewSend);
        mTextViewMorse.setText(mCtx.mMorse.getMorse(CleanString(mEdit.getText().toString())));

        mEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            @Override
            public void afterTextChanged(Editable editable) {
                mTextViewMorse.setText(mCtx.mMorse.getMorse(CleanString(mEdit.getText().toString())));
            }
        });

        mCheckBoxSound = (CheckBox) v.findViewById(R.id.CheckBoxSound);
        mCheckBoxSound.setChecked(mCtx.mPrefs.getBoolean("enable_sound", false));
        mCheckBoxSound.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (!mCtx.mPrefs.getBoolean("enable_light", false) &&
                             mCtx.mPrefs.getBoolean("enable_sound", false)) {
                            mCtx.mPrefs.edit().putBoolean("enable_light",
                                    !mCtx.mPrefs.getBoolean("enable_light", true)).commit();
                            mCheckBoxLight.setChecked(true);
                            Toast toast = Toast.makeText(mCtx,
                                    "You have to keep one option enabled. light enabled.",
                                    Toast.LENGTH_SHORT);
                        }

                        mCtx.mPrefs.edit().putBoolean("enable_sound",
                                           !mCtx.mPrefs.getBoolean("enable_sound", false)).commit();
                    }
                }
        );

        mCheckBoxLight = (CheckBox) v.findViewById(R.id.CheckBoxLight);
        mCheckBoxLight.setChecked(mCtx.mPrefs.getBoolean("enable_light", true));
        mCheckBoxLight.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (!mCtx.mPrefs.getBoolean("enable_sound", false) &&
                                mCtx.mPrefs.getBoolean("enable_light", true)) {
                            mCtx.mPrefs.edit().putBoolean("enable_sound",
                                    !mCtx.mPrefs.getBoolean("enable_sound", true)).commit();
                            mCheckBoxSound.setChecked(true);
                            Toast toast = Toast.makeText(mCtx,
                                    "You have to keep one option enabled. enable sound.",
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        mCtx.mPrefs.edit().putBoolean("enable_light",
                                !mCtx.mPrefs.getBoolean("enable_light", true)).commit();
                    }
                }
        );

        mButtonSend = (Button) v.findViewById(R.id.ButtonSend);
        mButtonSend.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (!mCtx.hasFlash()) {
                            Toast toast = Toast.makeText(mCtx,
                                                         "You need a flash to send morse code.",
                                                         Toast.LENGTH_LONG);
                            toast.show();
                        }

                        if (mButtonSend.getText() == getResources().getString(R.string.btn_start) ) {
                            if (mCtx.mMorse != null) {
                                mCtx.mMorse.updateValues(Integer.valueOf(
                                                         mCtx.mPrefs.getString("interval", "500")));
                            }
                            if (mCtx.mOutputController != null) {
                                mCtx.mOutputController.setString(CleanString(mEdit.getText().toString()));
                                mCtx.mOutputController.start();
                                mCtx.mOutputController.activate();
                                mButtonSend.setText(R.string.btn_stop);
                            }
                        }
                        else if (mButtonSend.getText() == getResources().getString(R.string.btn_stop) ) {
                            if (mCtx.mOutputController != null) {
                                mCtx.mOutputController.stop();
                            }
                            mImageViewLightbulb.setImageDrawable(lightbulb_off);
                            mButtonSend.setText(R.string.btn_start);
                            resetText();
                        }
                    }
                });

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (mTextViewMessage != null && msg.getData().containsKey("message")) {
                    mTextViewMessage.setText((String) msg.getData().get("message"));
                }

                if (mImageViewLightbulb != null && msg.getData().containsKey("light")) {
                    String str = msg.getData().getString("light");

                    if (str == "on") {
                        mImageViewLightbulb.setImageDrawable(lightbulb_on);
                    } else {
                        mImageViewLightbulb.setImageDrawable(lightbulb_off);
                    }
                }

                if (mTextViewMorse != null && msg.getData().containsKey("progress")) {
                    String str, str1, str2,
                           mstr = mCtx.mMorse.getMorse(CleanString(mEdit.getText().toString()));
                    int len, cut = (Integer) msg.getData().get("progress");

                    if (cut < 0) {
                        resetText();
                        mButtonSend.setText(R.string.btn_start);
                        return;
                    }

                    len = mstr.length();

                    if (len == 0)
                        return;

                    str = String.format("%d%%", 100 * cut / len);
                    str1 = "";
                    str2 = "";

                    try {
                        str1 = mstr.substring(0, cut);
                    }
                    catch (IndexOutOfBoundsException e) {
                    }
                    try {
                        str2 = mstr.substring(cut);
                    }
                    catch (IndexOutOfBoundsException e) {
                    }

                    String text = "<font color='green'>" + str + "</font>" +
                                  "<font color='grey'> | </font>" +
                                  "<font color='red'>" + str1 + "</font>" +
                                  "<font color='black'>" + str2 + "</font>";
                    mTextViewMorse.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);

                    if (((float)cut / (float)len) == 1) {
                        ForcedSleep(500);
                        resetText();
                        mButtonSend.setText(R.string.btn_start);
                    }

                    Log.v(TAG, "handler gui");
                }
            }
        };

        mCtx.mHandlerSend = mHandler;

        return v;
    }
}