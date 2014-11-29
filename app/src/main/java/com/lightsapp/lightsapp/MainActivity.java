package com.lightsapp.lightsapp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.lightsapp.core.CameraController;
import com.lightsapp.core.analyzer.morse.MorseAnalyzer;
import com.lightsapp.core.SoundController;
import com.lightsapp.core.analyzer.light.LightAnalyzer;
import com.lightsapp.core.OutputController;
import com.lightsapp.core.analyzer.sound.SoundAnalyzer;
import com.lightsapp.core.analyzer.morse.MorseConverter;

import java.util.Locale;


public class MainActivity extends Activity implements ActionBar.TabListener {
    private final String TAG = MainActivity.class.getSimpleName();

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    public SharedPreferences mPrefs;

    public Camera mCamera;
    public MorseConverter mMorse;
    public OutputController mOutputController;
    public CameraController mCameraController;
    public SoundController mSoundController;
    public LightAnalyzer mLightA;
    public SoundAnalyzer mSoundA;
    public MorseAnalyzer mMorseA;

    public GraphView graphView_delay, graphView_lum, graphView_lum2, graphView_dlum, graphView_snd;

    public Handler mHandlerSend = null;
    public Handler mHandlerRecv = null;
    public Handler mHandlerInfo = null;

    private SetupHandler mThreadSetup = null;

    private void setup(Context context) {
        if (mThreadSetup == null) {
            mThreadSetup = new SetupHandler();
        }

        synchronized (mThreadSetup) {
            mThreadSetup.setupHandler(context);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "RESUME");
        if (mCamera == null) {
            setup(this);
        }
        mMorse = new MorseConverter(Integer.valueOf(mPrefs.getString("interval", "500")));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "PAUSE");
        if (mOutputController != null)
            mOutputController.stop();
        mOutputController = null;
        if (mCameraController != null)
            mCameraController.stopPreviewAndFreeCamera();
        mCamera = null;
        mThreadSetup = null;
        mCameraController = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "STOP");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG, "CREATE");

        // prevent screen switching off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onPostResume() {
        super.onPostResume();

        if (!(hasCamera() || hasFrontCamera()) && !hasFlash()) { // TODO display only once
            Toast toast = Toast.makeText(this,
                                         "You need a camera and flash to send-receive using light.",
                                         Toast.LENGTH_LONG);
            toast.show();
        } else {
            if (!hasFlash()) {
                Toast toast = Toast.makeText(this,
                                             "You need a camera flash to send morse code with light.",
                                             Toast.LENGTH_LONG);
                toast.show();
            }
            if (!(hasCamera() || hasFrontCamera())) {
                Toast toast = Toast.makeText(this,
                                             "You need a camera to receive light emitted morse code.",
                                             Toast.LENGTH_LONG);
                toast.show();
            }
        }
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
            if(mOutputController != null)
                mOutputController.stop();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_about) {
            if(mOutputController != null)
                mOutputController.stop();
            Intent intent = new Intent(this, AboutActivity.class);
            intent.putExtra("info", mCameraController.getInfo());
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_exit) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return SendFragment.newInstance(position + 1);
                case 1:
                    return RecvFragment.newInstance(position + 1);
                case 2:
                    return InfoFragment.newInstance(position + 1);
            }
            return null;
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
                    return getString(R.string.title_section_send).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section_recv).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section_info).toUpperCase();
            }
            return null;
        }
    }

    public boolean hasCamera() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public boolean hasFrontCamera() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    public boolean hasFlash() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
}