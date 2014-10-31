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

        mTextViewInfo.setText(Html.fromHtml(
                "<h3>app: " + appname + ", version: " + version + "</h3>" +
                "<b>Authors:</b> <br/>" +
                "<a href=\"mailto:alessandro.rosetti@gmail.com\">Alessandro Rosetti</a><br/>" +
                "<a href=\"mailto:lazzalf@yahoo.com\">Daniele Lazzarini</a><br/><br/>" +
                "<b>System Info:</b><br/> Camera -> [w]x[h]@30fps<br/><br/>" +
                "<b>Tips:</b><br/>"));
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
