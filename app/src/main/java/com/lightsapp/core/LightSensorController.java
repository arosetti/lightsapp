package com.lightsapp.core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.lightsapp.lightsapp.MainActivity;

public class LightSensorController {
    public final String TAG = LightSensorController.class.getSimpleName();
    private MainActivity mContext;

    public float light = 0;

    public LightSensorController(Context context) {
        mContext = (MainActivity) context;
    }

    public void setup() {
        SensorManager sensorManager = (SensorManager)mContext.getSystemService(mContext.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor != null) {
            sensorManager.registerListener(lightSensorEventListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private SensorEventListener lightSensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                light = event.values[0];
                Log.v(TAG, "Light sensor output: " + light);
            }
        }
    };
}
