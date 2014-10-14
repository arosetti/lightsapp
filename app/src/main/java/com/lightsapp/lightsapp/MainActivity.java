package com.lightsapp.lightsapp;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.lightsapp.camera.CameraController;
import com.lightsapp.camera.FrameAnalyzer;
import com.lightsapp.camera.LightController;
import com.lightsapp.morse.MorseConverter;

import java.util.Locale;


public class MainActivity extends Activity {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TextView mTextViewMessage;
    private TextView mTextViewMorse;

    private MorseConverter mMorse;
    private String mStrMorse;

    private LightController mLight;

    private Camera mCamera;
    private CameraController mPreview;


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (mTextViewMessage != null && msg.getData().containsKey("message")) {
                mTextViewMessage = (TextView) findViewById(R.id.txt_status);
                mTextViewMessage.setText((String) msg.getData().get("message")); // yep that's a String
            }

            if (mTextViewMorse != null && msg.getData().containsKey("progress")) {
                int p = (Integer) msg.getData().get("progress");
                int cut = (p * mMorse.getString(mStrMorse).length() ) / 100;
                int len = mMorse.getString(mStrMorse).length();
                String mstr = mMorse.getString(mStrMorse);
                String str = cut + "/" + len + "", str1 = "", str2 = "";
                try {
                    str1 = mstr.substring(0, cut);
                } catch (IndexOutOfBoundsException e) {}
                try {
                    str2 = mstr.substring(cut);
                } catch (IndexOutOfBoundsException e) {}
                String text = "<font color='green'>" + str + "</font> <font color='red'>" + str1 + "</font><font color='black'>" + str2 + "</font>.";
                mTextViewMorse.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        setContentView(R.layout.fragment_main);

        mCamera = Camera.open();

        mLight = new LightController(mCamera, mHandler);
        mLight.start();

        mTextViewMessage = (TextView) findViewById(R.id.txt_status);
        mTextViewMessage.setText("idle");

        Button mButton = (Button)findViewById(R.id.button_tx);
        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        EditText mEdit = (EditText) findViewById(R.id.edit_tx);
                        mStrMorse = mEdit.getText().toString();

                        mMorse = new MorseConverter();

                        mTextViewMorse = (TextView) findViewById(R.id.txt_tx);
                        mTextViewMorse.setText(mMorse.getString(mStrMorse));
                        mLight.setString(mStrMorse);
                        mLight.activate();
                    }
                });

        mPreview = new CameraController(this, mCamera, mHandler);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    protected void onResume() {
        super.onResume();
        if (this.mCamera == null)
            this.mCamera = mCamera.open();
    }

    protected void onStop() {
        super.onStop();
        mLight.stop();
        // mPreview. TODO kil prev class
        if (mCamera != null)
            mCamera.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
