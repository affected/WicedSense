/******************************************************************************
 *
 *  Copyright (C) 2014 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
package com.broadcom.app.wicedsense;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import com.broadcom.app.wicedsmart.ota.ui.OtaResource;
import com.broadcom.app.wicedsmart.ota.ui.OtaUiHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.util.Log;

/**
 * Configurable settings for the WICED Sense Application
 *
 */
public class Settings {
    public static final String TEMPERATURE_SCALE_TYPE_F = "F";
    public static final String TEMPERATURE_SCALE_TYPE_C = "C";

    /**
     * Specifies if pairing/encryption is required to WICED Sense tag
     */
    public static final boolean PAIRNG_REQUIRED = true;
    public static final int PAIRING_TIMEOUT_MS = 20000;

    /**
     * If true, WICED Sense app automatically connects to the WICED Sense tag
     * after it is picked from the device picker
     */
    public static final boolean CONNECT_AFTER_DEVICE_PICK = false;

    /**
     * Frequency the battery status of the WICED Sense tag should be polled (in
     * msec)
     */
    public static final int BATTERY_STATUS_INTERVAL_MS = 120000; // Every 2mins

    /**
     * Timeout to wait for a connection to the SenseManager service
     */
    public static final int SERVICE_INIT_TIMEOUT_MS = 250;

    /**
     * Number of retry attempts if BLE service discovery of the WICED Sense tag
     * fails
     */
    public static final int SERVICE_DISCOVERY_RETRY = 2;

    /**
     * If true, a firmware update check will be performed after the WICED Sense
     * tag is connected
     */
    public static final boolean CHECK_FOR_UPDATES_ON_CONNECT = true;

    /**
     * Enable debug tracing to adb logcat logs
     */
    public static final boolean DBG = true;

    /**
     * String contant that all adb logcat messages will be prefixed with (to
     * simplify filtering)
     */
    public static final String TAG_PREFIX = "WicedSense.";

    // --------------------Do not modify below-------------------------------
    public interface SettingChangeListener {
        public void onSettingsChanged(String settingName);
    }

    private static final String TAG = TAG_PREFIX + "Settings";

    public static final String PACKAGE_NAME = "com.broadcom.app.wicedsense";

    private static final String SETTINGS_PREF_NAME = "com.broadcom.app.wicedsense_preferences";
    static final String SETTINGS_KEY_PREFIX = "settings_";
    static final String SETTINGS_KEY_ANIMATION = SETTINGS_KEY_PREFIX + "animation";

    static final String SETTINGS_KEY_GYRO = SETTINGS_KEY_PREFIX + "gyro";
    static final String SETTINGS_KEY_ECOMPASS = SETTINGS_KEY_PREFIX + "ecompass";
    static final String SETTINGS_KEY_ACCELEROMETER = SETTINGS_KEY_PREFIX + "accelerometer";

    static final String SETTINGS_KEY_TEMPERATURE_SCALE_TYPE = SETTINGS_KEY_PREFIX
            + "temperature_scale_type";
    static final String SETTINGS_KEY_VERSION = SETTINGS_KEY_PREFIX + "version";

    /**
     * Period of time gauge values are animated
     */
    public static int ANIMATE_TIME_INTERVAL_MS = 150;
    public static final int ANIMATION_FRAME_DELAY_MS = 50;
    public static final int REFRESH_INTERVAL_MS = 50;
    public static final int REFRESH_INTERVAL_SLOWER_MS = 3000;

    private static String sVersionName;
    private static SharedPreferences sPrefs;
    private static OtaResource sDefaultOtaResource;
    private static boolean sAnimate;
    private static String sTemperatureScaleType;
    private static File sOtaDirectory;
    private static FilenameFilter sOtaFileFilter;
    private static final ArrayList<SettingChangeListener> mChangeListeners = new ArrayList<Settings.SettingChangeListener>();
    private static boolean sGyro;
    private static boolean sEcompass;
    private static boolean sAccelerometer;


    private static OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (!key.startsWith(SETTINGS_KEY_PREFIX)) {
                return;
            }
            if (SETTINGS_KEY_ANIMATION.equals(key)) {
                sAnimate = sharedPreferences.getBoolean(SETTINGS_KEY_ANIMATION, false);
                Log.d(TAG, "sAnimate = " + sAnimate);
            } else if (SETTINGS_KEY_TEMPERATURE_SCALE_TYPE.equals(key)) {
                sTemperatureScaleType = sharedPreferences.getString(
                        SETTINGS_KEY_TEMPERATURE_SCALE_TYPE, TEMPERATURE_SCALE_TYPE_F);
            }else if (SETTINGS_KEY_GYRO.equals(key)){
              	 sGyro = sharedPreferences.getBoolean(SETTINGS_KEY_GYRO, false);
                 Log.d(TAG, "sGyro = " + sGyro);
            }else if (SETTINGS_KEY_ECOMPASS.equals(key)){
                 sEcompass = sharedPreferences.getBoolean(SETTINGS_KEY_ECOMPASS, false);
                 Log.d(TAG, "sEcompass = " + sEcompass);
            }else if (SETTINGS_KEY_ACCELEROMETER.equals(key)){
            	 sAccelerometer = sharedPreferences.getBoolean(SETTINGS_KEY_ACCELEROMETER, false);
            	 Log.d(TAG, "sAccelerometer = " + sAccelerometer);
           }
            else {
                return;
            }

            SettingChangeListener[] listeners = new SettingChangeListener[mChangeListeners.size()];
            mChangeListeners.toArray(listeners);
            for (int i = 0; i < mChangeListeners.size(); i++) {
                try {
                    mChangeListeners.get(i).onSettingsChanged(key);
                } catch (Throwable t) {
                    Log.e(TAG, "onSharedPreferenceChanged error. Listener#" + i, t);
                }
            }
        }


    };

    private static void checkAndInitializeSettings(SharedPreferences pref) {
        boolean initialized = pref.getBoolean(SETTINGS_KEY_PREFIX + "initialized", false);
        if (initialized) {
            return;
        }

        Editor editor = pref.edit();
        editor.putBoolean(SETTINGS_KEY_ANIMATION, false);
        editor.putString(SETTINGS_KEY_TEMPERATURE_SCALE_TYPE, TEMPERATURE_SCALE_TYPE_F);
        editor.putBoolean(SETTINGS_KEY_PREFIX + "initialized", true);
        editor.putBoolean(SETTINGS_KEY_GYRO, true);
        editor.putBoolean(SETTINGS_KEY_ECOMPASS, true);
        editor.putBoolean(SETTINGS_KEY_ACCELEROMETER, true);
        editor.commit();
    }

    private static void initOtaResources(Context ctx) {
        int major = 0;
        int minor = 0;
        int appId = 0;
        boolean mandatory=false;
        try {
            major = Integer.parseInt(ctx.getString(R.string.default_ota_fw_version_major));
        } catch (Throwable t) {
        }
        try {
            minor = Integer.parseInt(ctx.getString(R.string.default_ota_fw_version_minor));
        } catch (Throwable t) {
        }
        try {
            appId = Integer.parseInt(ctx.getString(R.string.default_ota_fw_version_minor));
        } catch (Throwable t) {
        }

        try {
            mandatory = Boolean.parseBoolean(ctx.getString(R.string.default_ota_fw_mandatory));
        } catch (Throwable t) {
        }

        sDefaultOtaResource = OtaUiHelper.createRawOtaResource(
                ctx.getString(R.string.default_ota_fw_version_name), appId, major, minor,
                ctx.getResources(), R.raw.wiced_sense_1_3_update,mandatory);
        sOtaDirectory = new File(Environment.getExternalStorageDirectory(), "broadcom/wicedsense");
        sOtaFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("wiced_sense") && filename.endsWith(".ota.bin.signed");
            }
        };
    }

    public static void addChangeListener(SettingChangeListener l) {
        if (!mChangeListeners.contains(l)) {
            mChangeListeners.add(l);
        }
    }

    public static void removeChangeListener(SettingChangeListener l) {
        mChangeListeners.remove(l);
    }

    public static void init(Context ctx) {
        try {
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            sVersionName = pInfo.versionName;
        } catch (Throwable t) {

        }
        sPrefs = ctx.getSharedPreferences(SETTINGS_PREF_NAME, Context.MODE_PRIVATE);
        sPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);

        // Check if preferences initialized. If not, initialize with default
        // values.
        checkAndInitializeSettings(sPrefs);
        sAnimate = sPrefs.getBoolean(SETTINGS_KEY_ANIMATION, false);
        sGyro = sPrefs.getBoolean(SETTINGS_KEY_GYRO, false);
        sEcompass = sPrefs.getBoolean(SETTINGS_KEY_ECOMPASS, false);
        sAccelerometer = sPrefs.getBoolean(SETTINGS_KEY_ACCELEROMETER, false);
        sTemperatureScaleType = sPrefs.getString(SETTINGS_KEY_TEMPERATURE_SCALE_TYPE,
                TEMPERATURE_SCALE_TYPE_F);
        initOtaResources(ctx);

    }

    public static void finish() {
        sPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
    }

    static String getVersionName() {
        return sVersionName;
    }

    static boolean animate() {
        return sAnimate;
    }

    static String getTemperatureeScaleType() {
        return sTemperatureScaleType;
    }

    static OtaResource getDefaultOtaResource() {
        return sDefaultOtaResource;
    }

    static boolean hasMandatoryUpdate() {
        return sDefaultOtaResource != null && sDefaultOtaResource.isMandatory();
    }

    static File getOtaDirectory() {
        return sOtaDirectory;

    }

    static FilenameFilter getOtaFileFilter() {
        return sOtaFileFilter;
    }

    static boolean gyroEnabled(){
    	return sGyro;
    }
    static boolean compassEnabled(){
    	return sEcompass;
    }

    static boolean accelerometerEnabled(){
    	return sAccelerometer;
    }


}