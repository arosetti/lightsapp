package com.lightsapp.lightsapp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_about);

        String appname = "Lightsapp";
        String version;

        try {
            PackageInfo manager = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = manager.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "?";
        }

        TextView mTextViewInfo = (TextView) findViewById(R.id.TextViewInfo);

        String video = "320x240@30fps";

        mTextViewInfo.setText(Html.fromHtml(
                "<h3>app: " + appname + ", version: " + version + "</h3>" +
                "<b>Authors:</b> <br/>" +
                "<a href=\"mailto:alessandro.rosetti@gmail.com\">Alessandro Rosetti</a><br/>" +
                "<a href=\"mailto:lazzalf@yahoo.com\">Daniele Lazzarini</a><br/><br/>" +
                "<b>System Info:</b><br/> Camera: " + video + "<br/><br/>" +
                "<b>Open Source: <br/>Android's examples morse class has been modified to our needs.<br/>graphview:3.1.3</b><br/><br/>" +
                "<b>Tips:</b><br/>These tips may help you having a better experience.<br/>" +
                "[1] You can't successfully use this program if the external light is too high or if there are too much interferences.<br/>" +
                "[2] Use the graph section to check if there is a good variation of luminance while transmitting or receiving.<br/>" +
                "[3] Try to lower the sensitivity, it will dynamically adjust the thresholds and it may successfully translate the signal to text.<br/>" +
                "[4] Try to use higher base morse interval time. It depends on the camera that can't deliver a good frame rate or the flash led that can't switch on and off quickly."));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
