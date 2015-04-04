/******************************************************************************
 *
 *  Copyright (C) 2013-2014 Broadcom Corporation
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
package com.broadcom.app.ledevicepicker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PatternMatcher;
import android.util.Log;

/**
 * Helper class to show a device picker dialog and obtain the device selected by
 * the user.
 *
 *
 */
public class DevicePicker {
    private static final String TAG = DevicePickerSettings.TAG_PREFIX + "DevicePicker";

    /*
     * Intent action when someone wants to select a BLE device from a pick list
     */
    public static final String ACTION_LAUNCH = "com.broadcom.app.ledevicepicker.action.LAUNCH";
    /**
     * The target package to call when the device is picked
     */

    public static final String EXTRA_START_SCANNING = "START_SCANNING";

    public static final String EXTRA_DATA = "DATA";

    public static final String EXTRA_LAUNCH_PACKAGE = "LAUNCH_PACKAGE";
    /**
     * The target class to call when the device is picked
     */
    public static final String EXTRA_LAUNCH_CLASS = "LAUNCH_CLASS";

    public static final String EXTRA_TITLE = "TITLE";

    public static final String EXTRA_DEVICE_FILTERS = "DEVICE_FILTERS";

    public static final String EXTRA_SERVICE_FILTERS = "SERVICE_FILTERS";
    /**
     * Broadcast intent when a BLE device is selected from the BLE device picker
     * screen
     */
    public static final String ACTION_DEVICE_SELECTED = "com.broadcom.app.ledevicepicker.action.DEVICE_SELECTED";

    public static final String ACTION_CANCELLED = "com.broadcom.app.ledevicepicker.action.CANCELED";
    /**
     * Extra field containing the picked BluetoothDevice
     */
    public static final String EXTRA_DEVICE = BluetoothDevice.EXTRA_DEVICE;

    /**
     * Callback to receive device picked event
     *
     * @author fredc
     *
     */
    public interface Callback {
        public void onDevicePicked(BluetoothDevice device);

        public void onDevicePickCancelled();
    }

    public static Intent createLaunchIntent(String title, String launchAction, String packageName,
            String className, String[] deviceFilters) {
        Intent intent = new Intent(launchAction);
        intent.putExtra(EXTRA_LAUNCH_PACKAGE, packageName);
        intent.putExtra(EXTRA_LAUNCH_CLASS, className);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_DEVICE_FILTERS, deviceFilters);
        return intent;
    }

    /**
     * Create a Device Picker Launch Intent
     *
     * @param title
     * @param dataUri
     * @return
     */
    public static Intent createLaunchIntent(String title, String launchAction, Uri dataUri,
            String[] deviceFilters) {
        Intent intent = new Intent(launchAction);
        if (dataUri != null) {
            intent.putExtra(EXTRA_DATA, dataUri.toString());
        }
        intent.putExtra(EXTRA_TITLE, title);
        if (deviceFilters != null && deviceFilters.length > 0) {
            intent.putExtra(EXTRA_DEVICE_FILTERS, deviceFilters);
        }
        return intent;
    }

    public static Intent createLaunchIntent(String title, String launchPackageName,
            String launchClassName, String resultPackageName, String resultClassName,
            String[] deviceFilters, String[] serviceUuids) {
        Intent intent = new Intent();
        intent.setClassName(launchPackageName, launchClassName);
        intent.putExtra(EXTRA_LAUNCH_PACKAGE, resultPackageName);
        intent.putExtra(EXTRA_LAUNCH_CLASS, resultClassName);
        intent.putExtra(EXTRA_TITLE, title);
        if (deviceFilters != null && deviceFilters.length > 0) {
            intent.putExtra(EXTRA_DEVICE_FILTERS, deviceFilters);
        }
        if (serviceUuids != null && serviceUuids.length > 0) {
            intent.putExtra(EXTRA_SERVICE_FILTERS, serviceUuids);
        }
        return intent;
    }

    /**
     * Create a Device Picker Launch Intent
     *
     * @param title
     * @param dataUri
     * @return
     */
    public static Intent createLaunchIntent(String title, String launchPackageName,
            String launchClassName, Uri dataUri, String[] deviceFilters, String[] serviceUuids) {
        Intent intent = new Intent();
        intent.setClassName(launchPackageName, launchClassName);
        if (dataUri != null) {
            intent.putExtra(EXTRA_DATA, dataUri.toString());
        }
        intent.putExtra(EXTRA_TITLE, title);
        if (deviceFilters != null && deviceFilters.length > 0) {
            intent.putExtra(EXTRA_DEVICE_FILTERS, deviceFilters);
        }
        if (serviceUuids != null && serviceUuids.length > 0) {
            intent.putExtra(EXTRA_SERVICE_FILTERS, serviceUuids);
        }

        return intent;
    }

    /**
     * Create a Device Picker Broadcast Receiver filter that filters for the
     * data uri
     *
     * @param filterUri
     * @return
     */
    public static IntentFilter createResultIntentFilter(Uri filterUri) {
        IntentFilter filter = new IntentFilter(ACTION_DEVICE_SELECTED);
        filter.addAction(ACTION_CANCELLED);
        if (filterUri != null) {
            String scheme = filterUri.getScheme();
            String host = filterUri.getHost();
            int port = filterUri.getPort();
            String path = filterUri.getPath();
            filter.addDataScheme(scheme);
            filter.addDataAuthority(host, port == -1 ? null : String.valueOf(port));
            if (path != null) {
                filter.addDataPath(path, PatternMatcher.PATTERN_LITERAL);
            }
        }
        return filter;
    }

    private final Context mCtx;
    private final Uri mDevicePickerDataUri;
    private final IntentFilter mDevicePickFilter;
    private final Callback mCallback;
    private final String mlaunchPackageName;
    private final String mlaunchClassName;
    private final String mBroadcastPackageName;
    private final String mBroadcastClassName;
    private final BroadcastReceiver mDevicePickerReceiver;

    private class DevicePickerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCallback != null) {
                String action = intent.getAction();
                if (ACTION_DEVICE_SELECTED.equals(action)) {
                    final BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                    if (device != null) {
                        try {
                            mCallback.onDevicePicked(device);
                        } catch (Throwable t) {
                        }
                        return;
                    }

                }
                try {
                    mCallback.onDevicePickCancelled();
                } catch (Throwable t) {
                }
            }
        }
    };

    /**
     * Create a DevicePicker, and broadcast an intent to the specificed
     * package/class receiver specified
     *
     * @param ctx
     * @param broadcastPackageName
     * @param broadcastClassName
     */
    public DevicePicker(Context ctx, String launchPackageName, String launchClassName,
            String broadcastPackageName, String broadcastClassName) {
        mCtx = ctx;
        mlaunchPackageName = launchPackageName;
        mlaunchClassName = launchClassName;
        mDevicePickerDataUri = null;
        mDevicePickFilter = createResultIntentFilter(null);
        mCallback = null;
        mDevicePickerReceiver = null;
        mBroadcastPackageName = broadcastPackageName;
        mBroadcastClassName = broadcastClassName;
    }

    /**
     * Create a DevicePicker, automatically register a broadcast receiver, and
     * invoke the callback when the device is picked
     *
     * @param ctx
     * @param devicePickerDataFilterUri
     * @param cb
     */
    public DevicePicker(Context ctx, String launchPackageName, String launchClassName, Callback cb,
            Uri devicePickerDataFilterUri) {
        mCtx = ctx;
        mlaunchPackageName = launchPackageName;
        mlaunchClassName = launchClassName;
        mDevicePickerDataUri = devicePickerDataFilterUri;
        mDevicePickFilter = createResultIntentFilter(mDevicePickerDataUri);
        mCallback = cb;
        mDevicePickerReceiver = new DevicePickerBroadcastReceiver();
        mBroadcastPackageName = null;
        mBroadcastClassName = null;
    }

    boolean mDevicePickerReceiverRegistered;

    /**
     * Initializes device picker resources
     */
    public boolean init() {
        if (mCallback != null && !mDevicePickerReceiverRegistered) {
            try {
                mCtx.registerReceiver(mDevicePickerReceiver, mDevicePickFilter);
                mDevicePickerReceiverRegistered = true;
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "init(): error", t);
            }
        }
        return false;
    }

    /**
     * Cleanup device picker resources
     */
    public void cleanup() {
        Log.d(TAG, "cleanup");
        if (mDevicePickerReceiverRegistered) {
            try {
                mCtx.unregisterReceiver(mDevicePickerReceiver);
                mDevicePickerReceiverRegistered = false;
            } catch (Throwable t) {
                Log.e(TAG, "init(): error", t);
            }
        }
    }

    /**
     * Launches the device picker. Returns false if Bluetooth is not available
     */
    public boolean launch(String title, String[] deviceFilters, String[] serviceUuids) {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            return false;
        }
        Intent intent = null;
        if (mCallback != null) {
            intent = createLaunchIntent(title, mlaunchPackageName, mlaunchClassName,
                    mDevicePickerDataUri, deviceFilters, serviceUuids);
        } else {
            intent = createLaunchIntent(title, mlaunchPackageName, mlaunchClassName,
                    mBroadcastPackageName, mBroadcastClassName, deviceFilters, serviceUuids);
        }
        try {
            mCtx.startActivity(intent);

        } catch (Throwable t) {
            Log.e(TAG, "launch(): error", t);
        }
        return false;
    }

}