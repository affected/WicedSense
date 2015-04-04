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

import com.broadcom.app.ledevicepicker.DeviceListFragment;
import com.broadcom.app.ledevicepicker.DeviceListFragment.Callback;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * Not used
 *
 * @deprecated
 * @author fredc
 *
 */
@Deprecated
public class DevicePickerFragment extends DialogFragment implements OnClickListener,
        OnShowListener, Callback {
    private String mTitle;
    private Button mScanButton;
    private boolean mIsScanning;
    private boolean mStartScanning;
    private DeviceListFragment mDevicePickerFragment;
    private String[] mDeviceFilters;
    private String[] mServiceFilters;

    // private boolean mDevicePicked;

    public static DevicePickerFragment createDialog(String title, String[] deviceFilters,
            String[] serviceFilters, boolean startScanning) {
        DevicePickerFragment f = new DevicePickerFragment();
        f.mTitle = title;
        f.mDeviceFilters = deviceFilters;
        f.mServiceFilters = serviceFilters;
        f.mStartScanning = startScanning;
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d("DevicePickerFragment", "onCreateDialog()");
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity(),
                R.style.DevicePickerDialogTheme);
        b.setTitle(mTitle == null ? getString(R.string.devicepicker_pick) : mTitle)
                .setPositiveButton(R.string.menu_scan, null);
        View v = getActivity().getLayoutInflater().inflate(R.layout.devicepicker_fragment, null);
        b.setView(v);
        AlertDialog d = b.create();
        d.setOnShowListener(this);

        return d;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mScanButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
        mScanButton.setOnClickListener(this);
        mDevicePickerFragment = (DeviceListFragment) getFragmentManager().findFragmentById(
                R.id.device_picker_id);
        if (mDevicePickerFragment != null) {
            mDevicePickerFragment.setCallback(this);
            mDevicePickerFragment.setServiceFilter(mServiceFilters);
        }

        Log.d("Wiced", "Fragment =" + mDevicePickerFragment);
        if (mStartScanning) {
            mStartScanning = false;
            setScanState(true);
            if (mDevicePickerFragment != null) {
                mDevicePickerFragment.scan(true);
            }
        } else {
            setScanState(false);
        }
    }

    @Override
    public void onClick(View v) {
        boolean isScanning = !mIsScanning;
        setScanState(isScanning);
        if (mDevicePickerFragment != null) {
            mDevicePickerFragment.scan(isScanning);
        }
    }

    public void setScanState(boolean isScanning) {
        if (mScanButton != null) {
            if (isScanning) {
                mScanButton.setText(R.string.menu_stop);
            } else {
                mScanButton.setText(R.string.menu_scan);
            }
            mIsScanning = isScanning;
        }
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

    @Override
    public void onDevicePicked(BluetoothDevice device) {

    }

    @Override
    public void onError() {
        // TODO Auto-generated method stub

    }

}