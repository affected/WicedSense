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
package com.broadcom.app.wicedsmart.ota;

import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.broadcom.util.ByteUtils;
import com.broadcom.util.GattRequestManager;
import com.broadcom.util.GattRequestManager.GattRequest;
import com.broadcom.util.GattRequestManager.GattTimeoutCallback;

public class OtaAppInfoReader extends BluetoothGattCallback implements android.os.Handler.Callback,
        GattTimeoutCallback {
    private static final String TAG = OtaSettings.TAG_PREFIX + "OtaAppInfoReader";

    public interface Callback {
        public void onAppInfoRead(boolean success, OtaAppInfo info);
    }

    private static final UUID UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_APP_INFO = UUID
            .fromString("6AA5711B-0376-44F1-BCA1-8647B48BDB55");
    private static final int EVENT_APP_INFO = 100;

    private GattRequestManager mGattManager;
    private final Callback mCallback;
    private BluetoothGattCharacteristic mAppInfoChar;
    private Handler mEventHandler;

    public OtaAppInfoReader(Callback cb) {
        this(cb, null);
    }

    public OtaAppInfoReader(Callback cb, Looper l) {
        mCallback = cb;
        if (l != null) {
            mEventHandler = new Handler(l, this);
        } else {
            mEventHandler = new Handler(this);
        }
    }

    private void sendAppInfoEvent(boolean success, byte[] appInfo) {
        mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_APP_INFO, success ? 1 : 0, 0,
                appInfo));
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        if (UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_APP_INFO.equals(characteristic.getUuid())) {
            byte[] appInfoBytes = characteristic.getValue();
            sendAppInfoEvent(true, appInfoBytes);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case EVENT_APP_INFO:
            boolean success = msg.arg1 == 1;
            byte[] appInfoBytes = (byte[]) msg.obj;
            processAppInfoRead(success, appInfoBytes);

        }
        return false;
    }

    @Override
    public void onTimeout(GattRequest request) {
        if (request == null) {
            return;
        }
        BluetoothGattCharacteristic c = request.mCharacteristic;
        if (c == null) {
            return;
        }

        if (UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_APP_INFO.equals(c.getUuid())) {
            Log.w(TAG, "read(): error timeout reading characeristic...");
            sendAppInfoEvent(false, null);
        }
    }

    private void processAppInfoRead(boolean success, byte[] appInfoBytes) {
        OtaAppInfo info = null;
        if (success && appInfoBytes != null && appInfoBytes.length >= 4) {

            info = new OtaAppInfo(ByteUtils.bytesToUInt16LI(appInfoBytes, 0, 0), appInfoBytes[2],
                    appInfoBytes[3]);
        }
        if (mCallback != null) {
            mCallback.onAppInfoRead(success, info);
        }
    }

    public boolean initServicesAndCharacteristics(GattRequestManager mgr) {
        mGattManager = mgr;

        // Find the app info characteristic
        mAppInfoChar = mGattManager.findCharacteristic(OtaManager.UUID_WS_SECURE_UPGRADE_SERVICE,
                UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_APP_INFO);
        if (mAppInfoChar == null) {
            return false;
        }

        mGattManager.addCallback(this);
        mGattManager.addTimeoutCallback(this);
        return true;
    }

    public void finish() {
        if (mGattManager != null) {
            mGattManager.removeCallback(this);
            mGattManager.removeTimeoutCallback(this);
        }
    }

    public boolean read() {
        if (mAppInfoChar == null) {
            return false;
        }

        mGattManager.read(mAppInfoChar);
        return true;
    }

}
