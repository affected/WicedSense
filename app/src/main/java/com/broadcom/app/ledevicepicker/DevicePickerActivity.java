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
/**
 * NOTE: replace "R.class" with your R.class declared in your application
 * Also, make sure you include the following resources
 * layout/devicepicker_activity.xml
 * layout/devicepicker_fragment.xml
 * layout/devicepicker_listitem.xml
 * values/strings_devicepicker.xml
 * values-v11/styles_devicepicker.xml
 * values-v14/styles_devicepicker.xml
 */
import com.broadcom.app.wicedsense.R;

import com.broadcom.app.ledevicepicker.DevicePicker;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * Container activity used to display the device picker
 * @author fredc
 *
 */
public class DevicePickerActivity extends Activity implements DeviceListFragment.Callback,
        OnClickListener {
    private static final String TAG = DevicePickerSettings.TAG_PREFIX + "DevicePickerActivity";

    private String mDataUri;
    private String mLaunchPackage;
    private String mLaunchClass;
    private String mTitle;
    private Button mScanButton;
    private boolean mIsScanning;
    private boolean mStartScanning;
    private DeviceListFragment mDevicePickerFragment;
    private String[] mDeviceFilters;
    private String[] mServiceFilters;
    private boolean mDevicePicked;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.devicepicker_activity);
        Intent intent = getIntent();
        mDataUri = intent.getStringExtra(DevicePicker.EXTRA_DATA);
        mLaunchPackage = intent.getStringExtra(DevicePicker.EXTRA_LAUNCH_PACKAGE);
        mLaunchClass = intent.getStringExtra(DevicePicker.EXTRA_LAUNCH_CLASS);
        mTitle = intent.getStringExtra(DevicePicker.EXTRA_TITLE);
        mStartScanning = intent.getBooleanExtra(DevicePicker.EXTRA_START_SCANNING, true);
        mDeviceFilters = intent.getStringArrayExtra(DevicePicker.EXTRA_DEVICE_FILTERS);
        mServiceFilters = intent.getStringArrayExtra(DevicePicker.EXTRA_SERVICE_FILTERS);

        if (mTitle != null) {
            setTitle(mTitle);
        } else {
            setTitle(R.string.default_title);
        }

        mDevicePickerFragment = (DeviceListFragment) getFragmentManager().findFragmentByTag(
                "device_picker_id");
        if (mDevicePickerFragment != null) {
            mDevicePickerFragment.setCallback(this);
            mDevicePickerFragment.setServiceFilter(mServiceFilters);
        }

        mScanButton = (Button) findViewById(R.id.scan_button);
        mScanButton.setOnClickListener(this);

    }

    private void setScanState(boolean isScanning) {
        if (isScanning) {
            mScanButton.setText(R.string.menu_stop);
        } else {
            mScanButton.setText(R.string.menu_scan);
        }
        mIsScanning = isScanning;

    }

    @Override
    protected void onResume() {
        if (mStartScanning) {
            mStartScanning = false;
            setScanState(true);
            mDevicePickerFragment.scan(true);
        } else {
            setScanState(false);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mIsScanning) {
            mDevicePickerFragment.scan(false);
            mIsScanning = false;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (!mDevicePicked) {
            Intent intent = new Intent();
            intent.setAction(DevicePicker.ACTION_CANCELLED);
            if (mDataUri != null) {
                Uri uri = null;
                try {
                    uri = Uri.parse(mDataUri);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing uri", e);
                }
                if (uri != null) {
                    intent.setData(uri);
                }
            }
            sendBroadcast(intent);
        }
        super.onDestroy();
    }


    @Override
    public void onDevicePicked(BluetoothDevice device) {
        if (device != null) {
            mDevicePicked = true;
            Intent intent = new Intent();
            intent.setAction(DevicePicker.ACTION_DEVICE_SELECTED);
            intent.putExtra(DevicePicker.EXTRA_DEVICE, device);

            if (mLaunchPackage != null && mLaunchClass != null) {
                intent.setClassName(mLaunchPackage, mLaunchClass);
            }
            if (mDataUri != null) {
                Uri uri = null;
                try {
                    uri = Uri.parse(mDataUri);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing uri", e);
                }
                if (uri != null) {
                    intent.setData(uri);
                }
            }
            finish();
            sendBroadcast(intent);
        }

    }

    @Override
    public void onError() {
        Toast.makeText(getApplicationContext(), R.string.error_internal, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onClick(View v) {
        boolean isScanning = !mIsScanning;
        setScanState(isScanning);
        mDevicePickerFragment.scan(isScanning);
    }

    @Override
    public boolean canAddDevice(BluetoothDevice device) {
        if (mDeviceFilters == null || mDeviceFilters.length == 0) {
            return true;
        }
        String address = device.getAddress();
        for (int i = 0; i < mDeviceFilters.length; i++) {
            if (mDeviceFilters[i].equals(address)) {
                return false;
            }
        }
        return true;
    }

}
