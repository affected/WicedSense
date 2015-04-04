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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

/**
 * Displays a list of Bluetooth GATT device
 *
 */
public class DeviceListFragment extends ListFragment implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = DevicePickerSettings.TAG_PREFIX + "DeviceListFragment";

    public interface Callback {
        public boolean canAddDevice(BluetoothDevice device);

        public void onDevicePicked(BluetoothDevice device);

        public void onError();
    }

    private DeviceAdapter mDeviceAdapter;
    private BluetoothAdapter mBluetoothAdapter = null;
    private Callback mCallback;

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null && mCallback.canAddDevice(device)) {
                    mDeviceAdapter.addDevice(device, rssi, DeviceAdapter.DEVICE_SOURCE_SCAN);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        mDeviceAdapter = new DeviceAdapter(activity);
        setListAdapter(mDeviceAdapter);
        BluetoothManager bluetoothManager = (BluetoothManager) activity
                .getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            if (mCallback != null) {
                mCallback.onError();
                return;
            }
        }
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        BluetoothDevice device = mDeviceAdapter.getDevice(position);
        if (device != null && mCallback != null) {
            mCallback.onDevicePicked(device);
        }
    }

    UUID[] mServiceFilters;

    public void setServiceFilter(String[] filters) {
        if (filters == null || filters.length == 0) {
            mServiceFilters = null;
        } else {
            ArrayList<UUID> f = new ArrayList<UUID>();
            mServiceFilters = new UUID[filters.length];
            for (int i = 0; i < mServiceFilters.length; i++) {
                try {
                    f.add(UUID.fromString(filters[i]));
                } catch (Throwable t) {
                    Log.e(TAG, "setServiceFilter: error", t);
                }
            }
            if (f.size() == 0) {
                mServiceFilters = null;
            } else {
                mServiceFilters = new UUID[f.size()];
                f.toArray(mServiceFilters);
            }
        }
    }

    public void scan(boolean enable) {
        if (mBluetoothAdapter == null)
            return;

        if (enable) {

            addDevices();
            if (mServiceFilters == null || mServiceFilters.length == 0) {
                mBluetoothAdapter.startLeScan(this);
            } else {
                mBluetoothAdapter.startLeScan(mServiceFilters, this);
            }
        } else {
            mBluetoothAdapter.stopLeScan(this);
        }

        getActivity().invalidateOptionsMenu();
    }

    private void addDevices() {
        BluetoothManager btManager = null;

        if (mBluetoothAdapter != null) {
            btManager = (BluetoothManager) getActivity()
                    .getSystemService(Context.BLUETOOTH_SERVICE);
        }
        if (btManager == null) {
            if (mCallback != null) {
                mCallback.onError();
            }
            return;
        }

        List<BluetoothDevice> devices = btManager.getDevicesMatchingConnectionStates(
                BluetoothProfile.GATT_SERVER, new int[] { BluetoothProfile.STATE_CONNECTED,
                        BluetoothProfile.STATE_DISCONNECTED });
        for (BluetoothDevice device : devices) {
            if (mCallback != null && mCallback.canAddDevice(device)) {
                mDeviceAdapter.addDevice(device, 0, DeviceAdapter.DEVICE_SOURCE_CONNECTED);
            }
        }
    }

    // ---------------------Public APIs--------------------------

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

}