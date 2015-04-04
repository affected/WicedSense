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

import java.util.ArrayList;
import java.util.Iterator;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Helper class used by the device picker to render the state of the device
 *
 */
class DeviceAdapter extends BaseAdapter {
    class DeviceRecord {
        public BluetoothDevice device;
        public int rssi;
        public Long last_scanned;
        public int state;

        public DeviceRecord(BluetoothDevice device, int rssi, int state) {
            this.device = device;
            this.rssi = rssi;
            this.state = state;
            last_scanned = System.currentTimeMillis() / 1000;
        }
    }

    public static final int DEVICE_SOURCE_SCAN = 0;
    public static final int DEVICE_SOURCE_BONDED = 1;
    public static final int DEVICE_SOURCE_CONNECTED = 2;

    public static final int STATE_BONDED = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_NONE = 0;

    private long mLastUpdate = 0;

    private final Context mContext;
    private final ArrayList<DeviceRecord> mDevices;
    private final LayoutInflater mInflater;

    public DeviceAdapter(Context context) {
        mContext = context;

        mInflater = LayoutInflater.from(context);
        mDevices = new ArrayList<DeviceRecord>();
    }

    public void addDevice(BluetoothDevice device, int rssi, int state) {
        synchronized (mDevices) {
            for (DeviceRecord rec : mDevices) {
                if (rec.device.equals(device)) {
                    rec.rssi = rssi;
                    rec.last_scanned = System.currentTimeMillis() / 1000;
                    updateUi(false);
                    return;
                }
            }

            mDevices.add(new DeviceRecord(device, rssi, state));
            updateUi(true);
        }
    }

    public void removeDevice(BluetoothDevice device) {
        synchronized (mDevices) {
            for (DeviceRecord rec : mDevices) {
                if (rec.device.equals(device)) {
                    mDevices.remove(rec);
                    updateUi(true);
                    break;
                }
            }
        }
    }

    public void clear() {
        synchronized (mDevices) {
            mDevices.clear();
            updateUi(true);
        }
    }

    public BluetoothDevice getDevice(int position) {
        if (position < mDevices.size())
            return mDevices.get(position).device;
        return null;
    }

    public String getName(int position) {
        if (position < mDevices.size())
            return mDevices.get(position).device.getName();
        return null;
    }

    public String getAddress(int position) {
        if (position < mDevices.size())
            return mDevices.get(position).device.getAddress();
        return null;
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null || convertView.findViewById(R.id.device_name) == null) {
            convertView = mInflater.inflate(R.layout.devicepicker_listitem, null);
            holder = new ViewHolder();
            holder.device_name = (TextView) convertView.findViewById(R.id.device_name);
            holder.device_addr = (TextView) convertView.findViewById(R.id.device_addr);
            holder.device_rssi = (ProgressBar) convertView.findViewById(R.id.device_rssi);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DeviceRecord rec = mDevices.get(position);
        holder.device_rssi.setProgress(normaliseRssi(rec.rssi));

        String deviceName = rec.device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            holder.device_name.setText(rec.device.getName());
            holder.device_addr.setText(rec.device.getAddress());
        } else {
            holder.device_name.setText(rec.device.getAddress());
            holder.device_addr.setText(mContext.getResources().getString(R.string.unknown_device));
        }

        return convertView;
    }

    static class ViewHolder {
        TextView device_name;
        TextView device_addr;
        ProgressBar device_rssi;
    }

    private void updateUi(boolean force) {
        Long ts = System.currentTimeMillis() / 1000;
        if (force || ((ts - mLastUpdate) >= 1)) {
            removeOutdated();
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        mLastUpdate = ts;
    }

    private void removeOutdated() {
        Long ts = System.currentTimeMillis() / 1000;
        synchronized (mDevices) {
            for (Iterator<DeviceRecord> it = mDevices.iterator(); it.hasNext();) {
                DeviceRecord rec = it.next();
                if ((ts - rec.last_scanned) > 3 && rec.state == DEVICE_SOURCE_SCAN) {
                    it.remove();
                }
            }
        }
    }

    private int normaliseRssi(int rssi) {
        // Expected input range is -127 -> 20
        // Output range is 0 -> 100
        final int RSSI_RANGE = 147;
        final int RSSI_MAX = 20;

        return (RSSI_RANGE + (rssi - RSSI_MAX)) * 100 / RSSI_RANGE;
    }
}
