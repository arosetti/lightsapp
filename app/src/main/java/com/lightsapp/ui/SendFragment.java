package com.lightsapp.ui;

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

import com.lightsapp.lightsapp.R;

import static com.lightsapp.utils.Utils.*;


public class SendFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "0";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mContext;

    private ImageView mImageViewLightbulb;
    private Drawable lightbulb_on, lightbulb_off;

    private EditText mEdit;
    private Button mButtonSend;
    private TextView mTextViewMessage, mTextViewMorse;
    private CheckBox mCheckBoxSound, mCheckBoxLight, mCheckBoxRepeat;

    public Handler mHandler;

    public static SendFragment newInstance(int sectionNumber) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public SendFragment() {
        mContext = (MainActivity) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSettings(); // TODO bugfix ?! on new install
        resetText();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_send, container, false);

        mContext = (MainActivity) getActivity();

        mImageViewLightbulb = (ImageView) v.findViewById(R.id.ImageViewLightbulb);
        lightbulb_off = getResources().getDrawable(R.drawable.lightbulb_off);
        lightbulb_on = getResources().getDrawable(R.drawable.lightbulb_on);

        mTextViewMessage = (TextView) v.findViewById(R.id.TextViewStatus);

        mEdit = (EditText) v.findViewById(R.id.EditViewSend);
        mEdit.setText(mContext.mPrefs.getString("default_text", "sos"));

        mTextViewMorse = (TextView) v.findViewById(R.id.TextViewSend);
        mTextViewMorse.setText(mContext.mMorse.getMorse(CleanString(mEdit.getText().toString())));

        mEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            @Override
            public void afterTextChanged(Editable editable) {
                mTextViewMorse.setText(mContext.mMorse.getMorse(CleanString(mEdit.getText().toString())));
            }
        });

        mCheckBoxRepeat = (CheckBox) v.findViewById(R.id.CheckBoxRepeat);
        mCheckBoxRepeat.setChecked(mContext.mPrefs.getBoolean("repeat_send", false));
        mCheckBoxRepeat.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        mContext.mPrefs.edit().putBoolean("repeat_send",
                                !mContext.mPrefs.getBoolean("repeat_send", false)).commit();
                    }
                }
        );

        mCheckBoxSound = (CheckBox) v.findViewById(R.id.CheckBoxSound);
        mCheckBoxSound.setChecked(mContext.mPrefs.getBoolean("enable_sound", false));
        mCheckBoxSound.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (!mContext.mPrefs.getBoolean("enable_light", false) &&
                             mContext.mPrefs.getBoolean("enable_sound", false)) {
                            mContext.mPrefs.edit().putBoolean("enable_light",
                                    !mContext.mPrefs.getBoolean("enable_light", true)).commit();
                            mCheckBoxLight.setChecked(true);
                            Toast toast = Toast.makeText(mContext,
                                    "You have to keep one option enabled. light enabled.",
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        mContext.mPrefs.edit().putBoolean("enable_sound",
                                           !mContext.mPrefs.getBoolean("enable_sound", false)).commit();
                    }
                }
        );

        mCheckBoxLight = (CheckBox) v.findViewById(R.id.CheckBoxLight);
        mCheckBoxLight.setChecked(mContext.mPrefs.getBoolean("enable_light", true));
        mCheckBoxLight.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (!mContext.mPrefs.getBoolean("enable_sound", false) &&
                                mContext.mPrefs.getBoolean("enable_light", true)) {
                            mContext.mPrefs.edit().putBoolean("enable_sound",
                                    !mContext.mPrefs.getBoolean("enable_sound", true)).commit();
                            mCheckBoxSound.setChecked(true);
                            Toast toast = Toast.makeText(mContext,
                                    "You have to keep one option enabled. enable sound.",
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        mContext.mPrefs.edit().putBoolean("enable_light",
                                !mContext.mPrefs.getBoolean("enable_light", true)).commit();
                    }
                }
        );

        mButtonSend = (Button) v.findViewById(R.id.ButtonSend);
        mButtonSend.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (!mContext.hasFlash()) {
                            Toast toast = Toast.makeText(mContext,
                                                         "You need a flash to send morse code.",
                                                         Toast.LENGTH_LONG);
                            toast.show();
                        }

                        if (mButtonSend.getText() == getResources().getString(R.string.btn_start) ) {
                            String str = CleanString(mEdit.getText().toString());
                            if (str.length() <= 0)
                                return;

                            if (mContext.mMorse != null) {
                                mContext.mMorse.updateValues(Integer.valueOf(
                                                         mContext.mPrefs.getString("interval", "500")));
                            }

                            if (mContext.mOutputController != null) {
                                mContext.mOutputController.setString(str);
                                mButtonSend.setText(R.string.btn_stop);
                                if (mCheckBoxRepeat.isChecked())
                                    mContext.mOutputController.repeat();
                                else
                                    mContext.mOutputController.start();

                                mContext.mOutputController.activate();
                            }
                        }
                        else if (mButtonSend.getText() == getResources().getString(R.string.btn_stop) ) {
                            if (mContext.mOutputController != null) {
                                mContext.mOutputController.stop();
                            }
                            mImageViewLightbulb.setImageDrawable(lightbulb_off);
                            resetText();
                            mButtonSend.setText(R.string.btn_start);
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
                           mstr = mContext.mMorse.getMorse(CleanString(mEdit.getText().toString()));
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

                        if (!mCheckBoxRepeat.isChecked()) {
                            resetText();
                            mButtonSend.setText(R.string.btn_start);
                        }
                    }

                    Log.v(TAG, "handler gui");
                }
            }
        };

        mContext.mHandlerSend = mHandler;

        return v;
    }

    public void resetText() {
        mTextViewMessage.setText("morse interval is " + mContext.mMorse.get("SPEED_BASE") + "ms");
        mTextViewMorse.setText(mContext.mMorse.getMorse(CleanString(mEdit.getText().toString())));
    }

    public void updateSettings() {
        mCheckBoxRepeat.setChecked(mContext.mPrefs.getBoolean("repeat_send", false));

        if (!mContext.mPrefs.getBoolean("enable_light", true) &&
            !mContext.mPrefs.getBoolean("enable_sound", false)) {
            mContext.mPrefs.edit().putBoolean("enable_light", true).commit();
            Toast toast = Toast.makeText(mContext,
                    "You have to keep one option enabled. defalut light output enabled.",
                    Toast.LENGTH_LONG);
            toast.show();
        }

        mCheckBoxLight.setChecked(mContext.mPrefs.getBoolean("enable_light", true));
        mCheckBoxSound.setChecked(mContext.mPrefs.getBoolean("enable_sound", false));
        //mTextViewMorse.setText(mContext.mPrefs.getString("default_text", ""));
    }
}