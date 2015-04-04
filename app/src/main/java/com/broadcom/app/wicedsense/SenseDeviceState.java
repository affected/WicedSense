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

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.broadcom.app.wicedsmart.ota.OtaAppInfo;
import com.broadcom.app.wicedsmart.ota.OtaAppInfoReader;
import com.broadcom.app.wicedsmart.ota.OtaAppInfoReader.Callback;
import com.broadcom.util.GattRequestManager;
import com.broadcom.util.GattRequestManager.LePairingCallback;

/**
 * Manages the connection and service/characteristics to a WICED Sense Device
 *
 *
 */
public class SenseDeviceState extends BluetoothGattCallback implements Handler.Callback, Callback,
        LePairingCallback {
    private static final String TAG = Settings.TAG_PREFIX + "DeviceState";
    private static final boolean DBG = Settings.DBG;

    /**
     * Event callback interface invoked to report events to interested listeners
     *
     */
    public interface EventCallback {
        public void onConnected(SenseDeviceState deviceState);

        public void onDisconnected(SenseDeviceState deviceState);

        public void onUnsupportedDevice(SenseDeviceState deviceState);

        public void onBatteryStatus(SenseDeviceState deviceState, int batteryLevel);

        public void onSensorData(SenseDeviceState deviceState, byte[] sensorData);

        public void onAppInfoRead(SenseDeviceState deviceState, boolean success, OtaAppInfo info);
    }

    private static final int DISCONNECT_DELAY_MS = 500;
    private static final int CONNECT_COMPLETE_TIMER = 5000;

    /** Descriptor used to enable/disable notifications/indications */
    private static final UUID CLIENT_CONFIG_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID SENSOR_SERVICE_UUID = UUID
            .fromString("739298B6-87B6-4984-A5DC-BDC18B068985");
    private static final UUID SENSOR_NOTIFICATION_UUID = UUID
            .fromString("33EF9113-3B55-413E-B553-FEA1EAADA459");

    private static final UUID BATTERY_SERVICE_UUID = UUID
            .fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL_UUID = UUID
            .fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private static final int GET_BATTERY_STATUS = 100;
    private static final int DISCONNECT = 102;

    private static String getMessageName(int what) {
        switch (what) {
        case GET_BATTERY_STATUS:
            return "GET_BATTERY_STATUS";
        case DISCONNECT:
            return "DISCONNECT";
        }
        return "UNKNOWN:" + what;
    }

    private final EventCallback mEventCallback;

    private final Handler mHandler;
    private final BluetoothDevice mDevice;
    private final GattRequestManager mGattManager;
    private boolean mIsConnectedAndAvailable;
    private BluetoothGattCharacteristic mSensorNotification;
    private BluetoothGattDescriptor mSensorNotificationClientConfig;
    private BluetoothGattService mSensorService;
    private BluetoothGattService mBatteryService;
    private BluetoothGattCharacteristic mBatteryLevel;
    private boolean mEnableSensorNotifications;
    private boolean mSensorNotificationsEnabled;
    private boolean mMonitorBattery;
    private boolean mConnectAfterBonding;
    private final OtaAppInfoReader mOtaAppReader;

    public SenseDeviceState(Context ctx, BluetoothDevice device, Looper l, EventCallback cb) {
        mEventCallback = cb;
        mHandler = new Handler(l, this);
        mDevice = device;
        mGattManager = new GattRequestManager(ctx, device);
        mGattManager.setAutoConnect(false);
        mGattManager.setDiscoverServices(true);
        mGattManager.setRetryFailedConnection(false, -1);
        if (Settings.PAIRNG_REQUIRED) {
            mGattManager.setPairingTimeout(Settings.PAIRING_TIMEOUT_MS);
        }
        mGattManager.addCallback(this);
        mGattManager.addPairingCallback(this);

        mOtaAppReader = new OtaAppInfoReader(this, l);
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public GattRequestManager getGattManager() {
        return mGattManager;
    }

    public OtaAppInfoReader getAppInfoReader() {
        return mOtaAppReader;
    }

    public boolean isConnectedAndAvailable() {
        return mIsConnectedAndAvailable;
    }

    private void cancelBatteryStatus() {
        mHandler.removeMessages(GET_BATTERY_STATUS);
        mGattManager.removeRequest(GattRequestManager.REQUEST_READ_CHAR, mBatteryLevel);
    }

    private boolean loadServicesAndCharacteristics(BluetoothGatt gatt) {
        // Get sensor service
        mSensorService = gatt.getService(SENSOR_SERVICE_UUID);
        if (mSensorService == null) {
            Log.w(TAG,
                    "onServicesDiscovered: Sensor Service not found. This device is not supported");
            return false;
        }

        // Get notification characteristic
        mSensorNotification = mSensorService.getCharacteristic(SENSOR_NOTIFICATION_UUID);
        if (mSensorNotification == null) {
            Log.w(TAG,
                    "onServicesDiscovered: Sensor Characteristic not found. This device is not supported");
            return false;
        }
        // Get client config for notification
        mSensorNotificationClientConfig = mSensorNotification.getDescriptor(CLIENT_CONFIG_UUID);
        if (mSensorNotificationClientConfig == null) {
            Log.w(TAG,
                    "onServicesDiscovered: Sensor Descriptor not found. This device is not supported");
            return false;
        }

        // Get battery service and characteristic
        mBatteryService = gatt.getService(BATTERY_SERVICE_UUID);
        if (mBatteryService != null) {
            mBatteryLevel = mBatteryService.getCharacteristic(BATTERY_LEVEL_UUID);
        }
        if (mBatteryLevel == null) {
            Log.w(TAG, "onServiceDiscovered: Battery Level Characteristic not found.");
        }

        // Get OTA Service and characteristic
        if (!mOtaAppReader.initServicesAndCharacteristics(mGattManager)) {
            Log.w(TAG, "onServiceDiscovered: AppInfo Characteristics not found.");
        }
        return true;

    }

    @Override
    public boolean handleMessage(Message msg) {
        if (DBG) {
            Log.d(TAG, "handleMessage:" + getMessageName(msg.what));
        }
        switch (msg.what) {
        case DISCONNECT:
            boolean closeResources = msg.arg1 == 1;
            if (mGattManager != null) {
                mGattManager.disconnect(closeResources);
            }
            break;
        case GET_BATTERY_STATUS:
            mGattManager.read(mBatteryLevel);
            break;
        }
        return true;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (DBG) {
            Log.d(TAG, "onConnectionStateChange: " + mDevice + " status " + status + ": state="
                    + newState);
        }

        if (BluetoothGatt.STATE_DISCONNECTED == newState) {
            mIsConnectedAndAvailable = false;
            mSensorNotificationsEnabled = false;
            cancelBatteryStatus();
            mOtaAppReader.finish();

            // Send disconnected event
            if (mEventCallback != null) {
                mEventCallback.onDisconnected(this);
            }
            return;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (mIsConnectedAndAvailable) {
            if (DBG) {
                Log.d(TAG,
                        "onServicesDiscovered: Device already connected and services already discovered...");
            }
            return;
        }

        if (DBG) {
            Log.d(TAG, "onServicesDiscovered: status=" + status);
            List<BluetoothGattService> services = gatt.getServices();
            if (services != null && services.size() > 0) {
                for (int i = 0; i < services.size(); i++) {
                    BluetoothGattService s = services.get(i);
                    Log.d(TAG, "Service #" + i + ": " + s.getUuid().toString());
                }
            }
        }

        // Load the services and characteristics
        if (!loadServicesAndCharacteristics(gatt)) {
            if (mEventCallback != null) {
                mEventCallback.onUnsupportedDevice(this);
            }
            disconnectDelayed();
            return;
        }
        mIsConnectedAndAvailable = true;
        mGattManager.startConnectCompleteTimer(CONNECT_COMPLETE_TIMER);
        if (mEventCallback != null) {
            mEventCallback.onConnected(this);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (DBG) {
            Log.d(TAG, "onDescriptorWrite  " + descriptor.getUuid());
        }

        if (mSensorNotification == null) {
            Log.w(TAG, "onDescriptorWrite: mSensorNotification not found...");
            return;
        }
        boolean success = mGattManager.setCharacteristicNotification(mSensorNotification,
                mEnableSensorNotifications);
        if (success) {
            mSensorNotificationsEnabled = mEnableSensorNotifications;
        }
        Log.d(TAG,"onDescriptorWrite(): set char notification status success= " + success );
        Log.d(TAG,"onDescriptorWrite(): mSensorNotificationsEnabled = " + mSensorNotificationsEnabled);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        if (DBG) {
            Log.d(TAG, "onCharacteristicRead  " + characteristic.getUuid());
        }

        if (BATTERY_LEVEL_UUID.equals(characteristic.getUuid())) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    int batteryLevel = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    if (mEventCallback != null) {
                        mEventCallback.onBatteryStatus(this, batteryLevel);
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Unable to read battery level", t);
                    return;
                }
            } else {
                // Read right away for error case
                mGattManager.read(mBatteryLevel);
                return;
            }

            if (mMonitorBattery) {
                getBatteryStatus(true);
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {
        if (SENSOR_NOTIFICATION_UUID.equals(characteristic.getUuid())) {
            byte[] value = characteristic.getValue();
            if (mEventCallback != null) {
                mEventCallback.onSensorData(this, value);
            }
        }
    }

    public boolean pairIfNeeded() {
        if (Settings.PAIRNG_REQUIRED && mDevice != null
                && mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
            try {
                mConnectAfterBonding = true;
                return mGattManager.pair();
            } catch (Throwable t) {
                mConnectAfterBonding = false;
                Log.e(TAG, "error", t);
            }
        }
        return false;
    }

    @Override
    public void onPaired(boolean paired) {
        if (DBG) {
            Log.d(TAG, "onPaired: paired=" + paired);
        }
        if (mConnectAfterBonding) {
            mConnectAfterBonding = false;
            if (paired) {
                mGattManager.connect();
            } else {
                if (mEventCallback != null) {
                    mEventCallback.onDisconnected(this);
                }
            }
        }

    }

    public void enableNotifications(boolean enable) {
        enableSensorNotifications(enable);
        enableBatteryMonitoring(enable);
    }

    private void disconnectDelayed() {
        mHandler.sendEmptyMessageDelayed(DISCONNECT, DISCONNECT_DELAY_MS);
    }

    private boolean enableSensorNotifications(boolean enable) {
        if (DBG) {
            Log.d(TAG, "enableSensorNotifications: enable=" + enable);
        }

        if (mGattManager == null || mSensorNotification == null
                || mSensorNotificationClientConfig == null) {
            Log.w(TAG, "enableSensorNotifications: resources not available");
            return false;
        }
        if (enable == mSensorNotificationsEnabled) {
            Log.w(TAG, "enableSensorNotifications: notifications state already is enabled="
                    + mSensorNotificationsEnabled);
        }

        mEnableSensorNotifications = enable; // Set flag used in callback
        try {
            mGattManager.write(mSensorNotificationClientConfig,
                    mEnableSensorNotifications ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        } catch (Throwable t) {
            Log.e(TAG, "enableSensorNotifications: error", t);
            return false;
        }
        return true;
    }

    private void enableBatteryMonitoring(boolean enable) {
        if (DBG) {
            Log.d(TAG, "enableBatteryMonitoring: enable=" + enable + ", mMonitorBattery="
                    + mMonitorBattery);
        }

        if (mMonitorBattery == enable) {
            Log.w(TAG, "enableBatteryMonitoring: already in state enabled=" + mMonitorBattery);
            return;
        }
        mMonitorBattery = enable;

        if (enable) {
            getBatteryStatus(false);
        } else {
            cancelBatteryStatus();
        }
    }

    public void getBatteryStatus(boolean delayed) {
        if (!mIsConnectedAndAvailable) {
            return;
        }
        cancelBatteryStatus();
        mGattManager.removeRequest(GattRequestManager.REQUEST_READ_CHAR, mBatteryLevel);
        if (delayed) {
            mHandler.sendEmptyMessageDelayed(GET_BATTERY_STATUS,
                    Settings.BATTERY_STATUS_INTERVAL_MS);
        } else {
            mGattManager.read(mBatteryLevel, true);
        }
    }

    @Override
    public void onAppInfoRead(boolean success, OtaAppInfo info) {
        if (DBG) {
            Log.d(TAG, "onAppInfoRead");
        }

        OtaAppInfoReader reader = mOtaAppReader;
        GattRequestManager gattManager = mGattManager;
        if (gattManager != null && reader != null) {
            gattManager.removeCallback(reader);
        }
        if (mEventCallback != null) {
            mEventCallback.onAppInfoRead(this, success, info);
        }
    }
}
