package com.lightsapp.lightsapp;

import java.util.Locale;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.lightsapp.camera.CameraController;
import com.lightsapp.camera.LightController;
import com.lightsapp.morse.MorseConverter;


public class MainActivity extends Activity implements ActionBar.TabListener {
    private final String TAG = MainActivity.class.getSimpleName();

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    public SharedPreferences mPrefs;

    public Camera mCamera;
    public MorseConverter mMorse;
    public LightController mLight;
    public CameraController mCameraController;

    Handler mHandlerSend = null;
    Handler mHandlerRecv = null;
    Handler mHandlerGraph = null;
    private SetupHandler mThreadSetup = null;

    private void setup()
    {
        if (mThreadSetup == null) {
            mThreadSetup = new SetupHandler();
        }

        synchronized (mThreadSetup) {
            mThreadSetup.setupHandler();
        }
    }

    private class SetupHandler extends HandlerThread {
        private final String TAG = SetupHandler.class.getSimpleName();
        Handler mHandlerSetup = null;

        SetupHandler() {
            super("CameraHandler");
            start();
            mHandlerSetup = new Handler(getLooper());
        }

        void setupHandler() {
            mHandlerSetup.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCamera = Camera.open();
                        mLight = new LightController(mMorse, mCamera, mHandlerSend,
                                                     mPrefs.getBoolean("enable_sound", true));
                        mLight.start();

                        Message msg = mHandlerRecv.obtainMessage();
                        Bundle b = new Bundle();
                        b.putString("setup_done", "");
                        msg.setData(b);

                        mHandlerRecv.sendMessage(msg);
                        Log.v(TAG, "setup done");
                    }
                    catch (RuntimeException e) {
                        Log.e(TAG, "failed to setup in setuphandler");
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "RESUME");
        if (mCamera == null) {
            setup();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "PAUSE");
        if (mLight != null)
            mLight.stop();
        mLight = null;
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

    public void setFragment(Fragment frag)
    {
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentById(R.id.pager) == null) {
            fm.beginTransaction().add(R.id.pager, frag).commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG, "CREATE");

        final ActionBar actionBar = getActionBar();
        if(actionBar != null)
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
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
        mMorse = new MorseConverter(Integer.valueOf(mPrefs.getString("speed", "500")));

        SendFragment sf = new SendFragment();

        setFragment(sf);
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        if (!hasCamera() && !hasFlash())
        {
            Toast toast = Toast.makeText(this, "You can't use this app, you'll need a camera and flash.", Toast.LENGTH_LONG);
            toast.show();
        }
        else {
            if (!hasFlash()) {
                Toast toast = Toast.makeText(this, "This app need a flash to send morse code.", Toast.LENGTH_LONG);
                toast.show();
            }
            if (!hasCamera()) {
                Toast toast = Toast.makeText(this, "This app need a camera to receive morse code.", Toast.LENGTH_LONG);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_exit) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
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
                    return GraphFragment.newInstance(position + 1);
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
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return "Graph".toUpperCase();
            }
            return null;
        }
    }

    private boolean hasCamera() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private boolean hasFlash() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
}