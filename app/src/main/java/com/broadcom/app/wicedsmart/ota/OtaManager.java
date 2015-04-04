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

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.BufferedInputStream;
import java.util.UUID;
import com.broadcom.util.ByteUtils;
import com.broadcom.util.GattRequestManager;
import com.broadcom.util.GattRequestManager.GattRequest;
import com.broadcom.util.GattRequestManager.GattTimeoutCallback;

public class OtaManager extends BluetoothGattCallback implements Callback, GattTimeoutCallback {
    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECT = 1;
    public static final int STATE_DISCOVER = 2;
    public static final int STATE_ENABLE_NOTIFY = 3;
    public static final int STATE_PREPARE_DOWNLOAD = 4;
    public static final int STATE_START_DOWNLOAD = 5;
    public static final int STATE_SEND_FW_INFO = 6;
    public static final int STATE_SEND_FW = 7;
    public static final int STATE_SEND_FW_COMPLETED = 8;
    public static final int STATE_VERIFY_FW = 9;
    public static final int STATE_UPGRADE_COMPLETED = 10;
    public static final int STATE_ABORTED = -10;

    public static final int WS_UPGRADE_STATUS_OK = 0;
    public static final int WS_UPGRADE_STATUS_UNSUPPORTED_COMMAND = 1;
    public static final int WS_UPGRADE_STATUS_ILLEGAL_STATE = 2;
    public static final int WS_UPGRADE_STATUS_VERIFICATION_FAILED = 3;
    public static final int WS_UPGRADE_STATUS_INVALID_IMAGE = 4;
    public static final int WS_UPGRADE_STATUS_INVALID_IMAGE_SIZE = 5;
    public static final int WS_UPGRADE_STATUS_MORE_DATA = 6;
    public static final int WS_UPGRADE_STATUS_INVALID_APPID = 7;
    public static final int WS_UPGRADE_STATUS_INVALID_VERSION = 8;
    public static final int WS_UPGRADE_WRITE_STATUS_SUCCESS = 0x00;
    public static final int WS_UPGRADE_WRITE_STATUS_BAD_ID = 0x81;
    public static final int WS_UPGRADE_WRITE_STATUS_BAD_MAJOR = 0x82;
    public static final int WS_UPGRADE_WRITE_STATUS_TOO_MUCH_DATA = 0x83;
    public static final int WS_UPGRADE_WRITE_STATUS_TOO_SHORT = 0x84;
    public static final int WS_UPGRADE_WRITE_STATUS_ABORTED = 0x85;
    public static final int ERROR_CONNECT = -100;
    public static final int ERROR_DISCOVER = -101;
    public static final int ERROR_SERVICE = -102;
    public static final int ERROR_DESCRIPTOR_WRITE = -110;
    public static final int ERROR_DESCRIPTOR_NOT_FOUND = -111;
    public static final int ERROR_CHARACTERISTIC_WRITE = -112;
    public static final int ERROR_FIRMWARE_INFO_READ = -120;
    public static final int ERROR_FIRMWARE_INFO_WRITE = -121;
    public static final int ERROR_FIRMWARE_READ = -122;
    public static final int ERROR_FIRMWARE_WRITE = -123;
    public static final int ERROR_TIMEOUT = -200;

    public static final String getStateString(int state) {
        switch (state) {
        case STATE_NONE:
            return "STATE_NONE";
        case STATE_CONNECT:
            return "STATE_CONNECT";
        case STATE_DISCOVER:
            return "STATE_DISCOVER";
        case STATE_ENABLE_NOTIFY:
            return "STATE_ENABLE_NOTIFY";
        case STATE_PREPARE_DOWNLOAD:
            return "STATE_PREPARE_DOWNLOAD";
        case STATE_START_DOWNLOAD:
            return "STATE_START_DOWNLOAD";
        case STATE_SEND_FW_INFO:
            return "STATE_SEND_FW_INFO";
        case STATE_SEND_FW:
            return "STATE_SEND_FW";
        case STATE_SEND_FW_COMPLETED:
            return "STATE_SEND_FW_COMPLETED";
        case STATE_VERIFY_FW:
            return "STATE_VERIFY_FW";
        case STATE_UPGRADE_COMPLETED:
            return "STATE_UPGRADE_COMPLETED";
        case STATE_ABORTED:
            return "STATE_ABORTED";
        default:
            return "STATE_UNKNOWN";
        }
    }

    public static final String getStatusString(int status) {
        switch (status) {
        case WS_UPGRADE_STATUS_OK:
            return "WS_UPGRADE_STATUS_OK";
        case WS_UPGRADE_STATUS_UNSUPPORTED_COMMAND:
            return "WS_UPGRADE_STATUS_UNSUPPORTED_COMMAND";
        case WS_UPGRADE_STATUS_ILLEGAL_STATE:
            return "WS_UPGRADE_STATUS_ILLEGAL_STATE";
        case WS_UPGRADE_STATUS_VERIFICATION_FAILED:
            return "WS_UPGRADE_STATUS_VERIFICATION_FAILED";
        case WS_UPGRADE_STATUS_INVALID_IMAGE:
            return "WS_UPGRADE_STATUS_INVALID_IMAGE";
        case WS_UPGRADE_STATUS_INVALID_IMAGE_SIZE:
            return "WS_UPGRADE_STATUS_INVALID_IMAGE_SIZE";
        case WS_UPGRADE_STATUS_MORE_DATA:
            return "WS_UPGRADE_STATUS_MORE_DATA";
        case WS_UPGRADE_STATUS_INVALID_APPID:
            return "WS_UPGRADE_STATUS_INVALID_APPID";
        case WS_UPGRADE_STATUS_INVALID_VERSION:
            return "WS_UPGRADE_STATUS_INVALID_VERSION";
        case WS_UPGRADE_WRITE_STATUS_BAD_ID:
            return "WS_UPGRADE_WRITE_STATUS_BAD_ID";
        case WS_UPGRADE_WRITE_STATUS_BAD_MAJOR:
            return "WS_UPGRADE_WRITE_STATUS_BAD_MAJOR";
        case WS_UPGRADE_WRITE_STATUS_TOO_MUCH_DATA:
            return "WS_UPGRADE_WRITE_STATUS_TOO_MUCH_DATA";
        case WS_UPGRADE_WRITE_STATUS_TOO_SHORT:
            return "WS_UPGRADE_WRITE_STATUS_TOO_SHORT";
        case WS_UPGRADE_WRITE_STATUS_ABORTED:
            return "WS_UPGRADE_WRITE_STATUS_ABORTED";
        case ERROR_CONNECT:
            return "ERROR_CONNECT";
        case ERROR_DISCOVER:
            return "ERROR_DISCOVER";
        case ERROR_SERVICE:
            return "ERROR_SERVICE";
        case ERROR_DESCRIPTOR_WRITE:
            return "ERROR_DESCRIPTOR_WRITE";
        case ERROR_DESCRIPTOR_NOT_FOUND:
            return "ERROR_DESCRIPTOR_NOT_FOUND";
        case ERROR_CHARACTERISTIC_WRITE:
            return "ERROR_CHARACTERISTIC_WRITE";
        case ERROR_FIRMWARE_INFO_READ:
            return "ERROR_FIRMWARE_INFO_READ";
        case ERROR_FIRMWARE_INFO_WRITE:
            return "ERROR_FIRMWARE_INFO_WRITE";
        case ERROR_FIRMWARE_READ:
            return "ERROR_FIRMWARE_READ";
        case ERROR_FIRMWARE_WRITE:
            return "ERROR_FIRMWARE_WRITE";
        case ERROR_TIMEOUT:
            return "ERROR_TIMEOUT";
        default:
            return "UNKNOWN_STATUS";
        }
    }

    private static final String TAG = OtaSettings.TAG_PREFIX + "OtaManager";
    private static final boolean DBG = OtaSettings.DBG;
    static final UUID UUID_WS_SECURE_UPGRADE_SERVICE = UUID
            .fromString("A86ABC2D-D44C-442E-99F7-80059A873E36");
    private static final UUID UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_CONTROL_POINT = UUID
            .fromString("1BD19C14-B78A-4E0F-AEB5-8E0352BAC382");
    private static final UUID UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_DATA = UUID
            .fromString("279F9DAB-79BE-4663-AF1D-24407347AF13");
    private static final UUID GATT_DESCRIPTOR_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805F9B34FB");
    private static final int OTA_FW_INFO_PACKET_LENGTH = 4;
    private static final int OTA_MAX_TX_WRITE_PACKET_LENGTH = 20;
    private static final int EVENT_TIMEOUT_PAIRING = -1000;
    private static final int EVENT_ERROR = 1;
    private static final int EVENT_ABORTED = 2;
    private static final int EVENT_STATE_CHANGED = 3;
    private static final int EVENT_SEND_PROGRESS = 4;

    private class BluetoothBondingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothDevice d = mGatt.getDevice();
            if (device != null
                    && d != null
                    && device.equals(d)
                    && (device.getBondState() == BluetoothDevice.BOND_BONDED || device
                            .getBondState() == BluetoothDevice.BOND_BONDING)) {
                onPaired();
            }
        }
    }

    private Context mContext;
    private GattRequestManager mGatt;
    private BluetoothGattService mOtaFwService;
    private BluetoothGattCharacteristic mOtaCharControlPoint;
    private BluetoothGattCharacteristic mOtaCharData;

    private BluetoothGattDescriptor mCccDescriptor;
    private int mFirmwareLength;
    private BufferedInputStream mFirmwareInputStream;
    private int mState = STATE_NONE;
    private int mFirmwareSendLoopCount;
    private int mFirmwareBytesSent;
    private OtaCallback mCallback;
    private final Handler mEventHandler = new Handler(this);
    private boolean mHasError;
    private boolean mConnectAfterBonding = true;
    private boolean mDisconnectOnFinished = false;
    private BluetoothBondingReceiver mBondReceiver;

    public void setDisconnectOnFinished(boolean disconnect) {
        mDisconnectOnFinished = disconnect;
    }

    @Override
    public boolean handleMessage(Message msg) {
        OtaCallback cb = mCallback;
        if (cb == null) {
            return true;
        }
        try {
            switch (msg.what) {
            case EVENT_TIMEOUT_PAIRING:
                cb.onOtaError(STATE_CONNECT, msg.arg2);
                break;
            case EVENT_STATE_CHANGED:
                if (msg.arg1 == STATE_UPGRADE_COMPLETED) {
                    unregisterGatt();
                }
                cb.onOtaStateChanged(msg.arg1);
                break;
            case EVENT_SEND_PROGRESS:
                cb.onOtaUploadProgress(msg.getData().getInt("l", -1), msg.arg1, msg.arg2);
                break;
            case EVENT_ABORTED:
                unregisterGatt();
                cb.onOtaAborted();
                break;
            case EVENT_ERROR:
                unregisterGatt();
                mHasError = true;
                cb.onOtaError(msg.arg1, msg.arg2);
                break;
            }
        } catch (Throwable t) {
            Log.e(TAG, "handleMessage: error", t);
        }
        return true;
    }

    private void setState(int state) {
        mState = state;
        mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_STATE_CHANGED, state, 0));
    }

    private boolean loadGattServicesAndCharacteristics() {
        BluetoothGatt gatt = mGatt.getGatt();
        if (gatt == null) {
            return false;
        }

        mOtaFwService = gatt.getService(UUID_WS_SECURE_UPGRADE_SERVICE);
        if (mOtaFwService == null) {
            return false;
        }

        mOtaCharData = mOtaFwService.getCharacteristic(UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_DATA);
        if (mOtaCharData == null) {
            return false;
        }

        mOtaCharControlPoint = mOtaFwService
                .getCharacteristic(UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_CONTROL_POINT);
        if (mOtaCharControlPoint == null) {
            return false;
        }

        mCccDescriptor = mOtaCharControlPoint.getDescriptor(GATT_DESCRIPTOR_UUID);
        if (mCccDescriptor == null) {
            return false;
        }
        return true;
    }

    private void enableOtaNotify(boolean enable) {
        if (DBG) {
            Log.d(TAG, "enableOtaNotify");
        }
        setState(STATE_ENABLE_NOTIFY);
        if (mCccDescriptor == null) {
            Log.e(TAG, "mCccDescriptor is null");
            onOtaNotifyCompleted(false, ERROR_DESCRIPTOR_NOT_FOUND);
        }
        mGatt.write(mCccDescriptor, new byte[] { 0x00, 0x02 });
        mGatt.setCharacteristicNotification(mOtaCharControlPoint, enable);
    }

    private void onOtaNotifyCompleted(boolean success, int errorCode) {
        if (DBG) {
            Log.d(TAG, "onOtaNotifyCompleted: success=" + success + ", errorCode=" + errorCode
                    + "=" + getStateString(errorCode));
        }
        if (!success) {
            mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_ERROR, mState, errorCode));
            return;
        }
        prepareDownload();
    }

    private void prepareDownload() {
        if (DBG) {
            Log.d(TAG, "prepareDownload");
        }
        setState(STATE_PREPARE_DOWNLOAD);
        mGatt.write(mOtaCharControlPoint, new byte[] { 0x01 });
    }

    private void onDownloadPrepared(boolean success, int errorCode) {
        if (DBG) {
            Log.d(TAG, "onDownloadPrepared: success=" + success + ", errorCode=" + errorCode + "="
                    + getStateString(errorCode));
        }
        if (!success) {
            mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_ERROR, mState, errorCode));
            return;
        }
        startDownload(mFirmwareLength);

    }

    private void startDownload(int firmwareLength) {
        if (DBG) {
            Log.d(TAG, "startDownload: firmwareLength=" + firmwareLength);
        }
        mState = STATE_START_DOWNLOAD;
        byte[] packet = new byte[] { 0x02, 0x00, 0x00 };
        ByteUtils.uInt16ToBytesLI(firmwareLength, packet, 1);
        mGatt.write(mOtaCharControlPoint, packet);
    }

    private void onDownloadStarted(boolean success, int errorCode) {
        if (DBG) {
            Log.d(TAG, "onDownloadStarted: success=" + success + ", errorCode=" + errorCode + "="
                    + getStateString(errorCode));
        }
        if (!success) {
            mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_ERROR, mState, errorCode));
            return;
        }
        sendFirmwareInfo();
    }

    private byte[] getFirmwareInfoBytes() {
        if (mFirmwareInputStream == null) {
            return null;
        }
        byte[] firmwareInfo = new byte[OTA_FW_INFO_PACKET_LENGTH];

        try {
            int bytesRead = mFirmwareInputStream.read(firmwareInfo, 0, OTA_FW_INFO_PACKET_LENGTH);
            if (bytesRead != OTA_FW_INFO_PACKET_LENGTH) {
                Log.w(TAG, "Could not read firmware info... Incorrect length.");
                return null;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not read firmware info.", t);
            return null;
        }

        return firmwareInfo;
    }

    private void sendFirmwareInfo() {
        if (DBG) {
            Log.d(TAG, "sendFirmwareInfo");
        }
        setState(STATE_SEND_FW_INFO);

        byte[] firmwareInfo = getFirmwareInfoBytes();
        if (firmwareInfo == null) {
            onFirmwareInfoSent(false, ERROR_FIRMWARE_INFO_READ);
        }
        mGatt.write(mOtaCharData, firmwareInfo);
    }

    private void onFirmwareInfoSent(boolean success, int errorCode) {
        Log.d(TAG, "onFirmwareInfoSent: success=" + success + ", errorCode=" + errorCode + "="
                + getStateString(errorCode));

        if (!success) {
            mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_ERROR, mState, errorCode));
            return;
        }
        mFirmwareSendLoopCount = 0;
        mFirmwareBytesSent = 0;
        sendFirmware();
    }

    private byte[] getFirmwareBytes() {
        if (mFirmwareInputStream == null) {
            return null;
        }
        byte[] firmware = new byte[OTA_MAX_TX_WRITE_PACKET_LENGTH];
        try {
            int bytesRead = mFirmwareInputStream.read(firmware, 0, OTA_MAX_TX_WRITE_PACKET_LENGTH);
            if (bytesRead <= 0) {
                return new byte[0];
            }

            if (bytesRead == OTA_FW_INFO_PACKET_LENGTH) {
                return firmware;
            }
            // Truncate byte array
            byte[] truncatedBytes = new byte[bytesRead];
            if (bytesRead > 0) {
                System.arraycopy(firmware, 0, truncatedBytes, 0, bytesRead);
            }
            return truncatedBytes;

        } catch (Throwable t) {
            Log.w(TAG, "Could not read firmware info.", t);
            return null;
        }
    }

    private void sendFirmware() {
        if (DBG) {
            Log.d(TAG, "sendFirmware");
        }
        if (mState != STATE_SEND_FW) {
            setState(STATE_SEND_FW);
        }

        byte[] firmware = getFirmwareBytes();
        if (firmware == null) {
            onFirmwareSent(false, ERROR_FIRMWARE_READ);
        }
        if (firmware.length == 0) {
            onFirmwareSendCompleted();
        } else {
            mFirmwareSendLoopCount++;
            mFirmwareBytesSent += firmware.length;
            Message m = mEventHandler.obtainMessage(EVENT_SEND_PROGRESS, mFirmwareBytesSent,
                    mFirmwareLength);
            m.getData().putInt("l", mFirmwareSendLoopCount);
            mEventHandler.sendMessage(m);
            if (DBG) {
                Log.d(TAG, "sendFirmware: " + mFirmwareSendLoopCount + ", value= " + firmware);
                if (firmware != null) {
                    Log.d(TAG, "sendFirmware: length =" + firmware.length);
                }
            }
            mGatt.write(mFirmwareSendLoopCount, mOtaCharData, firmware);
        }
    }

    private void onFirmwareSent(boolean success, int errorCode) {
        if (DBG) {
            Log.d(TAG, "onFirmwareSent: success=" + success + ", errorCode=" + errorCode + "="
                    + getStateString(errorCode));
        }
        if (!success) {
            mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_ERROR, mState, errorCode));
            return;
        }
        // Otherwise keep sending remaining firmware
        sendFirmware();
    }

    private void onFirmwareSendCompleted() {
        Log.d(TAG, "onFirmwareSendCompleted");
        setState(STATE_SEND_FW_COMPLETED);
        verifyFirmware();
    }

    private void verifyFirmware() {
        if (DBG) {
            Log.d(TAG, "verifyFirmware");
        }
        setState(STATE_VERIFY_FW);
        mGatt.write(mOtaCharControlPoint, new byte[] { 0x3 });
    }

    private void onFirmwareVerified(boolean success, int errorCode) {
        if (DBG) {
            Log.d(TAG, "onFirmwareVerified: success=" + success + ", errorCode=" + errorCode + "="
                    + getStateString(errorCode));
        }
        if (!success) {
            mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_ERROR, mState, errorCode));
            return;
        }
        setState(STATE_UPGRADE_COMPLETED);
    }

    private void processControlPointStatus(boolean success, int errorCode) {
        int state = mState;
        switch (state) {
        case STATE_ENABLE_NOTIFY:
            onOtaNotifyCompleted(success, errorCode);
            break;
        case STATE_PREPARE_DOWNLOAD:
            onDownloadPrepared(success, errorCode);
            break;
        case STATE_START_DOWNLOAD:
            onDownloadStarted(success, errorCode);
            break;
        case STATE_SEND_FW_INFO:
            onFirmwareInfoSent(success, errorCode);
            break;
        case STATE_SEND_FW:
            onFirmwareSent(success, errorCode);
            break;
        case STATE_VERIFY_FW:
            onFirmwareVerified(success, errorCode);
            break;
        }
    }

    private void processDataWriteError() {
        int state = mState;
        if (state == STATE_SEND_FW_INFO) {
            onFirmwareSent(false, ERROR_FIRMWARE_INFO_WRITE);
        } else if (state == STATE_SEND_FW) {
            onFirmwareSent(false, ERROR_FIRMWARE_WRITE);
        }
    }

    /**
     * GATT client callbacks
     */

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothGatt.STATE_DISCONNECTED
                || newState == BluetoothGatt.STATE_DISCONNECTING) {
            if (mState != STATE_NONE && mState != STATE_UPGRADE_COMPLETED && !mHasError) {
                mEventHandler.sendEmptyMessage(EVENT_ABORTED);
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, "onServicesDiscovered -- status " + status);
        if (!loadGattServicesAndCharacteristics()) {
            mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_ERROR, mState,
                    ERROR_SERVICE));
        } else {
            enableOtaNotify(true);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (descriptor.getCharacteristic().getUuid()
                .equals(UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_CONTROL_POINT)) {
            boolean success = status == BluetoothGatt.GATT_SUCCESS;
            onOtaNotifyCompleted(success, success ? 0 : ERROR_DESCRIPTOR_WRITE);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {
        UUID uuid = characteristic.getUuid();
        if (!UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_CONTROL_POINT.equals(uuid)) {
            Log.e(TAG, "onCharacteristicChanged: UUID" + uuid + " not control point");
            return;
        }
        int status = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (DBG) {
            Log.d(TAG, "onCharacteristicChanged(): + " + uuid + ": status=" + status);
        }
        switch (status) {

        case WS_UPGRADE_STATUS_OK:
            // case WS_UPGRADE_WRITE_STATUS_SUCCESS:
        case WS_UPGRADE_STATUS_MORE_DATA:
            processControlPointStatus(true, WS_UPGRADE_STATUS_OK);
            break;
        case WS_UPGRADE_WRITE_STATUS_ABORTED:
            // mEventHandler.sendEmptyMessage(EVENT_ABORTED);
            break;
        default:
            processControlPointStatus(false, status);
            break;
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        UUID uuid = characteristic.getUuid();
        if (DBG) {
            Log.d(TAG, "onCharacteristicWrite: " + uuid + ", status= " + status);
            Log.d(TAG, "write type = " + characteristic.getWriteType());
        }
        if (UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_CONTROL_POINT.equals(uuid)) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                processControlPointStatus(false, ERROR_CHARACTERISTIC_WRITE);
            }
        }

        else if (UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_DATA.equals(uuid)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processControlPointStatus(true, WS_UPGRADE_STATUS_OK);
            } else {
                processDataWriteError();
            }
        }

    }

    @Override
    public void onTimeout(GattRequest request) {
        if (DBG) {
            Log.d(TAG, "onTimeout()");
        }
        if (request.mRequestType == GattRequestManager.REQUEST_WRITE_CHAR) {
            UUID uuid = request.mCharacteristic == null ? null : request.mCharacteristic.getUuid();
            if (UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_CONTROL_POINT.equals(uuid)
                    || UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_DATA.equals(uuid)) {
                Log.d(TAG, "onTimeout(): CP");
                processControlPointStatus(false, ERROR_TIMEOUT);
            }
        } else if (request.mRequestType == GattRequestManager.REQUEST_WRITE_DESCRIPTOR) {
            UUID uuid = null;
            UUID cUuid = null;
            if (request.mDescriptor != null) {
                uuid = request.mDescriptor.getUuid();
                cUuid = request.mDescriptor.getCharacteristic().getUuid();
            }
            if (UUID_WS_SECURE_UPGRADE_CHARACTERISTIC_CONTROL_POINT.equals(cUuid)
                    && GATT_DESCRIPTOR_UUID.equals(uuid)) {
                processControlPointStatus(false, ERROR_TIMEOUT);
            }
        }
    }

    private void connect() {
        if (!mGatt.connect()) {
            mEventHandler.sendMessage(mEventHandler.obtainMessage(EVENT_ERROR, mState,
                    ERROR_CONNECT));
        } else {
            mDisconnectOnFinished = true;
        }
    }

    private boolean pairIfNeeded() {
        BluetoothDevice device = mGatt.getDevice();

        if (OtaSettings.PAIRING_REQUIRED && device.getBondState() != BluetoothDevice.BOND_BONDED) {
            if (DBG) {
                Log.d(TAG, "Pairing device...");
            }
            try {
                mConnectAfterBonding = true;
                mEventHandler.sendEmptyMessageDelayed(EVENT_TIMEOUT_PAIRING,
                        OtaSettings.TIMEOUT_PAIRING_MS);
                mGatt.pair();
                return true;
            } catch (Throwable t) {
                mConnectAfterBonding = false;
                Log.e(TAG, "error", t);
            }
        }
        return false;
    }

    private void onPaired() {
        if (DBG) {
            Log.d(TAG, "onPaired");
        }
        if (mConnectAfterBonding) {
            mEventHandler.removeMessages(EVENT_TIMEOUT_PAIRING);
            mConnectAfterBonding = false;
            connect();
        }

    }

    private void unregisterGatt() {
        if (mGatt != null) {
            mGatt.removeCallback(this);
            mGatt.removeTimeoutCallback(this);
        }
    }

    // ---------------Public API-----------------------------------------------

    public void addCallback(OtaCallback cb) {
        mCallback = cb;
    }

    public void removeCallback(OtaCallback cb) {
        if (mCallback == cb) {
            mCallback = null;
        }
    }

    public void startUpdate(Context ctx, GattRequestManager gattManager, int firmwareLength,
            BufferedInputStream in) {
        mContext = ctx;
        mBondReceiver = new BluetoothBondingReceiver();
        mContext.registerReceiver(mBondReceiver, new IntentFilter(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mGatt = gattManager;
        mGatt.addCallback(this);
        mGatt.addTimeoutCallback(this);

        mFirmwareLength = firmwareLength;
        mFirmwareInputStream = in;
        mState = STATE_NONE;
        mHasError = false;
        mFirmwareSendLoopCount = 0;
        mFirmwareBytesSent = 0;
        if (mGatt.isConnected()) {
            if (DBG) {
                Log.d(TAG, "startUpgrade: GattManager already connected");
            }
            if (!loadGattServicesAndCharacteristics()) {
                if (!mGatt.discoverServices()) {
                    onServicesDiscovered(mGatt.getGatt(), -600);
                }
            } else {
                enableOtaNotify(true);
            }
        } else {
            if (!pairIfNeeded()) {
                if (DBG) {
                    Log.d(TAG, "startUpgrade: connecting...");
                }
                connect();
            }
        }
    }

    public void abortUpdate() {
        mGatt.write(mOtaCharControlPoint, new byte[] { 0x7 });
        setState(STATE_ABORTED);
        mEventHandler.sendEmptyMessage(EVENT_ABORTED);
    }

    public void finish() {
        mContext.unregisterReceiver(mBondReceiver);
        unregisterGatt();
        if (mDisconnectOnFinished) {
            mGatt.disconnect(true);
        }
    }

}
