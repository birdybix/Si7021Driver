package com.example.androidthings.weatherstation;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.contrib.driver.si7021.Si7021;
import com.google.android.things.contrib.driver.si7021.Si7021SensorDriver;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;

/**
 * Created by Cirrus on 21/11/2017.
 */

public class WeatherStationActivity extends Activity {

    private static final String TAG = WeatherStationActivity.class.getSimpleName();

    private Si7021SensorDriver mEnvironmentalSensorDriver;
    private SensorManager mSensorManager;
    private final PeripheralManagerService managerService = new PeripheralManagerService();


    private float mLastTemperature;
    private float mLastHumidity;


    // Callback used when we register the Si7021 sensor driver with the system's SensorManager.
    private SensorManager.DynamicSensorCallback mDynamicSensorCallback = new SensorManager.DynamicSensorCallback() {

        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                // Our sensor is connected. Start receiving temperature data.
                mSensorManager.registerListener(mTemperatureListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Test 1");

            } else if (sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                // Our sensor is connected. Start receiving pressure data.
                mSensorManager.registerListener(mHumidityListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Test 2");
            }
        }

        @Override
        public void onDynamicSensorDisconnected(Sensor sensor) {
            super.onDynamicSensorDisconnected(sensor);
            Log.d(TAG, "Sensor disco: "+ sensor);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Started Weather Station");

        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        try {
            mEnvironmentalSensorDriver = new Si7021SensorDriver(BoardDefaults.getI2cBus());
            mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback);
            mEnvironmentalSensorDriver.registerTemperatureSensor();
            mEnvironmentalSensorDriver.registerHumiditySensor();

            Log.d(TAG, "Initialized I2C SI7021");
        } catch (IOException e) {
            throw new RuntimeException("Error initializing SI7021", e);
        }
    }



    // Callback when SensorManager delivers temperature data.
    private SensorEventListener mTemperatureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastTemperature = event.values[0];
            Log.d(TAG, "sensor changed: " + mLastTemperature);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };

    // Callback when SensorManager delivers humidity data.
    private SensorEventListener mHumidityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastHumidity = event.values[0];
            Log.d(TAG, "sensor changed: " + mLastHumidity);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Destroying");
        // Clean up sensor registrations
        mSensorManager.unregisterListener(mTemperatureListener);
        mSensorManager.unregisterListener(mHumidityListener);
        mSensorManager.unregisterDynamicSensorCallback(mDynamicSensorCallback);

        // Clean up peripheral.
        if (mEnvironmentalSensorDriver != null) {
            try {
                mEnvironmentalSensorDriver.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mEnvironmentalSensorDriver = null;
        }
    }


}
