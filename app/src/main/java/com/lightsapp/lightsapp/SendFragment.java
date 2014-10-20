package com.lightsapp.lightsapp;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lightsapp.camera.CameraController;
import com.lightsapp.morse.MorseConverter;

public class SendFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "0";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx = (MainActivity) getActivity();

    private TextView mTextViewMessage;
    private TextView mTextViewMorse;
    private EditText mEdit;

    private String mStrMorse;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_send, container, false);

        mCtx = (MainActivity) getActivity();

        mTextViewMessage = (TextView) v.findViewById(R.id.txt_status);
        mTextViewMessage.setText("idle");

        mEdit = (EditText) v.findViewById(R.id.edit_tx);
        mEdit.setText(mCtx.mPrefs.getString("default_text", "sos"));

        Button mButton = (Button)v.findViewById(R.id.button_start);
        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        mStrMorse = mEdit.getText().toString();
                        mTextViewMorse = (TextView) v.findViewById(R.id.txt_tx);
                        if (mCtx.mMorse != null) {
                            mCtx.mMorse.updateValues(Integer.valueOf(mCtx.mPrefs.getString("speed", "500")));
                            mTextViewMorse.setText(mCtx.mMorse.getMorse(mStrMorse));
                        }
                        if (mCtx.mLight != null) {
                            mCtx.mLight.setString(mStrMorse);
                            mCtx.mLight.activate();
                        }
                    }
                });

        mButton = (Button)v.findViewById(R.id.button_stop);
        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        if (mCtx.mLight != null )
                            mCtx.mLight.setStatus(false);
                    }
                });

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (mTextViewMessage != null && msg.getData().containsKey("message")) {
                    mTextViewMessage.setText((String) msg.getData().get("message"));
                }

                if (mTextViewMorse != null && msg.getData().containsKey("progress")) {
                    String str, str1, str2, mstr = mCtx.mMorse.getMorse(mStrMorse);
                    int len, cut = (Integer) msg.getData().get("progress");

                    len = mstr.length();
                    str = String.format("%03d%% |", 100 * cut / len);
                    str1 = "";
                    str2 = "";

                    try {
                        str1 = mstr.substring(0, cut);
                    } catch (IndexOutOfBoundsException e) { }
                    try {
                        str2 = mstr.substring(cut);
                    } catch (IndexOutOfBoundsException e) { }
                    String text = "<font color='green'>" + str + "</font> <font color='red'>" + str1 + "</font><font color='black'>" + str2 + "</font>";
                    mTextViewMorse.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
                    Log.v(TAG, "handler gui");
                }
            }
        };

        mCtx.mHandlerSend = mHandler;

        return v;
    }
}