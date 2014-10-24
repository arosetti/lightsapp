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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import static com.lightsapp.utils.Utils.*;


public class SendFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "0";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx = (MainActivity) getActivity();

    private ImageView mImageView_lightbulb;
    private Drawable lightbulb_on, lightbulb_off;

    private TextView mTextViewMessage;
    private TextView mTextViewMorse;
    private EditText mEdit;
    private Button mButtonSend;

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

    public void reset() {
        mTextViewMessage.setText("idle");
        mTextViewMorse.setText(mCtx.mMorse.getMorse(CleanString(mEdit.getText().toString())));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_send, container, false);

        mCtx = (MainActivity) getActivity();

        mImageView_lightbulb = (ImageView) v.findViewById(R.id.imageView_lightbulb);
        lightbulb_off = getResources().getDrawable(R.drawable.lightbulb_off);
        lightbulb_on = getResources().getDrawable(R.drawable.lightbulb_on);

        mTextViewMessage = (TextView) v.findViewById(R.id.txt_status);
        mTextViewMessage.setText("idle");

        mEdit = (EditText) v.findViewById(R.id.edit_tx);
        mEdit.setText(mCtx.mPrefs.getString("default_text", "sos"));

        mTextViewMorse = (TextView) v.findViewById(R.id.txt_tx);
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

        mButtonSend = (Button) v.findViewById(R.id.button_send);
        mButtonSend.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        if (mButtonSend.getText() ==
                            getResources().getString(R.string.btn_start) ) {
                            if (mCtx.mMorse != null) {
                                mCtx.mMorse.updateValues(Integer.valueOf(mCtx.mPrefs.getString("interval", "500")));
                            }
                            if (mCtx.mLight != null) {
                                mCtx.mLight.setString(CleanString(mEdit.getText().toString()));
                                mCtx.mLight.activate();
                                mButtonSend.setText(R.string.btn_stop);
                            }
                        }
                        else if (mButtonSend.getText() ==
                                 getResources().getString(R.string.btn_stop) ) {
                            if (mCtx.mLight != null)
                                mCtx.mLight.setStatus(false);
                            mImageView_lightbulb.setImageDrawable(lightbulb_off);
                            mButtonSend.setText(R.string.btn_start);
                            reset();
                        }
                    }
                });

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (mTextViewMessage != null && msg.getData().containsKey("message")) {
                    mTextViewMessage.setText((String) msg.getData().get("message"));
                }

                if (mImageView_lightbulb != null && msg.getData().containsKey("light")) {
                    String str = msg.getData().getString("light");

                    if (str == "on") {
                        mImageView_lightbulb.setImageDrawable(lightbulb_on);
                    } else {
                        mImageView_lightbulb.setImageDrawable(lightbulb_off);
                    }
                }

                if (mTextViewMorse != null && msg.getData().containsKey("progress")) {
                    String str, str1, str2,
                           mstr = mCtx.mMorse.getMorse(CleanString(mEdit.getText().toString()));
                    int len, cut = (Integer) msg.getData().get("progress");

                    len = mstr.length();

                    if (len == 0)
                        return;

                    str = String.format("%d%%", 100 * cut / len);
                    str1 = "";
                    str2 = "";

                    try {
                        str1 = mstr.substring(0, cut);
                    } catch (IndexOutOfBoundsException e) {
                    }
                    try {
                        str2 = mstr.substring(cut);
                    } catch (IndexOutOfBoundsException e) {
                    }
                    String text = "<font color='green'>" + str + "</font>" +
                                  "<font color='grey'> | </font>" +
                                  "<font color='red'>" + str1 + "</font>" +
                                  "<font color='black'>" + str2 + "</font>";
                    mTextViewMorse.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);

                    if (((float)cut / (float)len) == 1) {
                        mButtonSend.setText(R.string.btn_start);
                        ForcedSleep(500);
                        reset();
                    }

                    Log.v(TAG, "handler gui");
                }
            }
        };

        mCtx.mHandlerSend = mHandler;

        return v;
    }
}