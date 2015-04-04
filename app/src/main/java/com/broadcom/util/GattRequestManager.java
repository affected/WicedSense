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
package com.broadcom.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.broadcom.app.wicedsense.Settings;

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
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Helper class to manage GATT requests to a remote device, ensuring that GATT
 * read and are executed serially. In addition, this manages a timeout timer for
 * each GATT read/write request, connect/disconnect requests, and a retry
 * mechanism for service discovery
 *
 */
public class GattRequestManager extends BluetoothGattCallback implements Handler.Callback {
    private static final String TAG = Settings.TAG_PREFIX + "GattRequestManager";
    private static final boolean DBG = true;

    private static final int REQUEST_NONE = 0;
    public static final int REQUEST_READ_CHAR = 1;
    public static final int REQUEST_WRITE_CHAR = 2;
    public static final int REQUEST_READ_DESCRIPTOR = 3;
    public static final int REQUEST_WRITE_DESCRIPTOR = 4;
    private static final int REQUEST_CONNECT = 10;
    private static final int REQUEST_DISCONNECT = 20;
    private static final int CONNECT_COMPLETE = 30;
    private static final int EVENT_PAIRED = 40;
    private static final int EVENT_UNPAIRED = 50;
    private static final int CONNECT_DELAY_MS = 2000;

    private static final int REQUEST_DISCOVER_SERVICES = 12;
    private static final int DISCOVER_SERVICES_DELAY_MS = 3000;
    private static final int TIMEOUT_TYPE_CONNECT = -1000;
    private static final int TIMEOUT_TYPE_DISCONNECT = -1001;
    private static final int TIMEOUT_TYPE_SERVICE_DISCOVERY = -1002;
    private static final int TIMEOUT_TYPE_GATT_REQUEST = -1010;
    private static final int TIMEOUT_TYPE_PAIRING = -1020;

    private static final int TIMEOUT_CONNECT_MS = 10000;
    private static final int TIMEOUT_DISCONNECT_MS = 3000;
    private static final int TIMEOUT_SERVICE_DISCOVERY_MS = 10000;
    private static final int TIMEOUT_GATT_REQUEST_MS = 5000;
    private static final int TIMEOUT_PAIRING_MS = 5000;

    private static String getMessageString(int msg) {
        switch (msg) {
        case REQUEST_NONE:
            return "REQUEST_NONE";
        case REQUEST_CONNECT:
            return "REQUEST_CONNECT";
        case REQUEST_DISCONNECT:
            return "REQUEST_DISCONNECT";
        case CONNECT_COMPLETE:
            return "CONNECT_COMPLETE";
        case EVENT_PAIRED:
            return "EVENT_PAIRED";
        case EVENT_UNPAIRED:
            return "EVENT_UNPAIRED";
        case REQUEST_READ_CHAR:
            return "REQUEST_READ_CHAR";
        case REQUEST_WRITE_CHAR:
            return "REQUEST_WRITE_CHAR";
        case REQUEST_READ_DESCRIPTOR:
            return "REQUEST_READ_DESCRIPTOR";
        case REQUEST_WRITE_DESCRIPTOR:
            return "REQUEST_WRITE_DESCRIPTOR";
        case REQUEST_DISCOVER_SERVICES:
            return "REQUEST_DISCOVER_SERVICES";
        case DISCOVER_SERVICES_DELAY_MS:
            return "DISCOVER_SERVICES_DELAY_MS";
        case TIMEOUT_TYPE_CONNECT:
            return "TIMEOUT_TYPE_CONNECT";
        case TIMEOUT_TYPE_DISCONNECT:
            return "TIMEOUT_TYPE_DISCONNECT";
        case TIMEOUT_TYPE_SERVICE_DISCOVERY:
            return "TIMEOUT_TYPE_SERVICE_DISCOVERY";
        case TIMEOUT_TYPE_GATT_REQUEST:
            return "TIMEOUT_TYPE_GATT_REQUEST";
        default:
            return "UNKNOWN_TYPE " + msg;
        }
    }

    private static String getConnectionStateString(int gattState) {
        switch (gattState) {
        case BluetoothGatt.STATE_CONNECTED:
            return "STATE_CONNECTED";
        case BluetoothGatt.STATE_DISCONNECTED:
            return "STATE_DISCONNECTED";
        case BluetoothGatt.STATE_CONNECTING:
            return "STATE_CONNECTED";
        case BluetoothGatt.STATE_DISCONNECTING:
            return "STATE_DISCONNECTING";
        default:
            return "STATE_UNKNOWN " + gattState;
        }
    }

    private static Method sSetPrCr;
    private static Method sCrBd;
    private static Method sRmBd;

    static {
        try {
            sSetPrCr = BluetoothDevice.class.getMethod("setPairingConfirmation", boolean.class);
            sCrBd = BluetoothDevice.class.getMethod("createBond", (Class[]) null);
            sRmBd = BluetoothDevice.class.getMethod("removeBond", (Class[]) null);
        } catch (Throwable t) {
            Log.e(TAG, "error: getting bonding services", t);
            sSetPrCr = null;
            sCrBd = null;
        }
    }

    private static boolean unpair(BluetoothDevice device) {
        if (sRmBd != null) {
            try {
                sRmBd.invoke(device, (Object[]) null);
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "unpair: error", t);
            }
        }
        return false;
    }

    private static boolean pair(BluetoothDevice device) {
        if (sSetPrCr != null && sCrBd != null) {
            try {
                // sSetPrCr.invoke(device, true);
                sCrBd.invoke(device, (Object[]) null);
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "pair: error", t);
            }
        }
        return false;
    }

    public interface LePairingCallback {
        public void onPaired(boolean isPaired);
    }

    public interface GattTimeoutCallback {
        public void onTimeout(GattRequest request);
    }

    public static class GattRequest {
        public final int mId;
        public final BluetoothGattCharacteristic mCharacteristic;
        public final BluetoothGattDescriptor mDescriptor;
        public final int mRequestType;
        public final byte[] mByteValues;

        private GattRequest(int requestType, BluetoothGattCharacteristic c,
                BluetoothGattDescriptor d, byte[] byteValues) {
            this(0, requestType, c, d, byteValues);
        }

        private GattRequest(int requestType, BluetoothGattCharacteristic c,
                BluetoothGattDescriptor d) {
            this(requestType, c, d, null);
        }

        private GattRequest(int id, int requestType, BluetoothGattCharacteristic c,
                BluetoothGattDescriptor d, byte[] byteValues) {
            mId = id;
            mRequestType = requestType;
            mCharacteristic = c;
            mDescriptor = d;
            mByteValues = byteValues;
        }
    }

    private class BluetoothPairingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothDevice managedDevice = getDevice();
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
            if (DBG) {
                Log.d(TAG, "BluetoothBondingReceiver.onReceive(): device = " + device
                        + ", bondState= " + bondState + ", managedDevice= " + mDevice);
            }
            if (device != null && device.equals(managedDevice)) {
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    mHandler.sendEmptyMessage(EVENT_PAIRED);
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    mHandler.sendEmptyMessage(EVENT_UNPAIRED);
                }
            }
        }
    }

    private final Object mConnectionLock = new Object(); // Lock variable
    private final Context mContext;

    private final ArrayList<BluetoothGattCallback> mGattCallbacks = new ArrayList<BluetoothGattCallback>();
    private final ArrayList<GattTimeoutCallback> mGattTimeoutCallbacks = new ArrayList<GattTimeoutCallback>();
    private final ArrayList<LePairingCallback> mPairingCallbacks = new ArrayList<GattRequestManager.LePairingCallback>();

    private final Handler mHandler = new Handler(this);
    private final LinkedList<GattRequest> mQueuedRequests = new LinkedList<GattRequest>();
    private GattRequest mPendingRequest;

    private int mPairingTimeoutMs = TIMEOUT_PAIRING_MS;
    private boolean mAutoConnect = false;
    private boolean mRetryFailedConnection = true;
    private boolean mDiscoverServices = true;
    private int mMaxConnectionRetries = 2;

    private BluetoothPairingReceiver mPairingReceiver;
    private boolean mPairingReceiverRegistered;
    private final BluetoothDevice mDevice;
    private BluetoothGatt mGatt;
    private int mConnectState;
    private boolean mServicesDiscovered;
    private int mPendingConnectDisconnectRequest;
    private boolean mIsUserDisconnectRequested;
    private boolean mPendingClose;
    private boolean mIsClosedAndCleanedUp;
    private int mReconnectCount;

    private void startRequestTimeout(GattRequest r) {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(TIMEOUT_TYPE_GATT_REQUEST, r),
                TIMEOUT_GATT_REQUEST_MS);
    }

    private void cancelRequestTimeout() {
        mHandler.removeMessages(TIMEOUT_TYPE_GATT_REQUEST);
    }

    private void queueRequest(GattRequest r, boolean atFront) {
        synchronized (mQueuedRequests) {
            if (atFront) {
                mQueuedRequests.addFirst(r);
            } else {
                mQueuedRequests.add(r);
            }
            if (mPendingRequest == null) {
                processQueuedRequests();
            }
        }
    }

    private void processQueuedRequests() {
        GattRequest r = null;
        synchronized (mQueuedRequests) {
            mPendingRequest = mQueuedRequests.poll();
            if (mPendingRequest == null) {
                return;
            }
            r = mPendingRequest;
        }

        switch (r.mRequestType) {
        case REQUEST_READ_CHAR:
            if (mGatt == null) {
                onCharacteristicRead(null, r.mCharacteristic, BluetoothGatt.GATT_FAILURE);
                return;
            }
            startRequestTimeout(r);
            mGatt.readCharacteristic(r.mCharacteristic);
            break;
        case REQUEST_WRITE_DESCRIPTOR:
            if (mGatt == null) {
                onDescriptorWrite(null, r.mDescriptor, BluetoothGatt.GATT_FAILURE);
                return;
            }
            startRequestTimeout(r);
            if (r.mByteValues != null) {
                r.mDescriptor.setValue(r.mByteValues);
            }
            mGatt.writeDescriptor(r.mDescriptor);
            break;
        case REQUEST_WRITE_CHAR:
            if (mGatt == null) {
                onCharacteristicWrite(null, r.mCharacteristic, BluetoothGatt.GATT_FAILURE);
                return;
            }
            startRequestTimeout(r);
            if (DBG) {
                Log.d(TAG, "writeChar: id=" + r.mId + ",value=" + r.mByteValues);
            }
            if (r.mByteValues != null) {
                if (DBG) {
                    Log.d(TAG, " valueLength=" + r.mByteValues.length);
                }
            }
            if (r.mByteValues != null) {
                r.mCharacteristic.setValue(r.mByteValues);
            }
            mGatt.writeCharacteristic(r.mCharacteristic);
            break;
        case REQUEST_READ_DESCRIPTOR:
            if (mGatt == null) {
                onDescriptorRead(null, r.mDescriptor, BluetoothGatt.GATT_FAILURE);
                return;
            }
            startRequestTimeout(r);
            mGatt.readDescriptor(r.mDescriptor);
            break;
        }
    }

    private void sendTimeoutEvent(GattRequest r) {
        GattTimeoutCallback[] l = getTimeoutCallbacks();
        int i = 0;
        for (; i < l.length; i++) {
            try {
                l[i].onTimeout(r);
            } catch (Throwable t) {
                Log.e(TAG, "sendTimeoutEvent error : index =" + i, t);
            }
        }
    }

    private BluetoothGattCallback[] getGattCallbacks() {
        BluetoothGattCallback[] cb = null;
        synchronized (mGattCallbacks) {
            cb = new BluetoothGattCallback[mGattCallbacks.size()];
            mGattCallbacks.toArray(cb);
        }
        return cb;
    }

    private LePairingCallback[] getPairingCallbacks() {
        LePairingCallback[] cb = null;
        synchronized (mPairingCallbacks) {
            cb = new LePairingCallback[mPairingCallbacks.size()];
            mPairingCallbacks.toArray(cb);
        }
        return cb;
    }

    private GattTimeoutCallback[] getTimeoutCallbacks() {
        GattTimeoutCallback[] l = null;
        synchronized (mGattTimeoutCallbacks) {
            l = new GattTimeoutCallback[mGattTimeoutCallbacks.size()];
            mGattTimeoutCallbacks.toArray(l);
        }
        return l;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (DBG) {
            Log.d(TAG, "handleMessage:" + getMessageString(msg.what));
        }
        switch (msg.what) {
        case EVENT_PAIRED:
            onPaired(false);
            break;
        case EVENT_UNPAIRED:
            onPaired(true);
            break;
        case REQUEST_CONNECT:
            connect(true);
            break;
        case CONNECT_COMPLETE:
            mReconnectCount = mMaxConnectionRetries;
            break;
        case REQUEST_DISCOVER_SERVICES:
            if (mGatt.discoverServices()) {
                mHandler.sendEmptyMessageDelayed(TIMEOUT_TYPE_SERVICE_DISCOVERY,
                        TIMEOUT_SERVICE_DISCOVERY_MS);
            } else {
                Log.w(TAG, "Error calling discover services.");
                onServicesDiscovered(mGatt, BluetoothGatt.GATT_FAILURE);
            }
            break;
        case TIMEOUT_TYPE_PAIRING:
            onPaired(false);
        case TIMEOUT_TYPE_CONNECT:
            Log.w(TAG, "timeout: connect");
            synchronized (mConnectionLock) {
                mPendingClose = true; // Force gatt connection to close
                onConnectionStateChange(mGatt, BluetoothGatt.GATT_FAILURE,
                        BluetoothGatt.STATE_DISCONNECTED);
            }
            break;
        case TIMEOUT_TYPE_DISCONNECT:
            Log.w(TAG, "timeout: disconnect");
            onConnectionStateChange(mGatt, BluetoothGatt.GATT_FAILURE,
                    BluetoothGatt.STATE_DISCONNECTED);
            break;
        case TIMEOUT_TYPE_SERVICE_DISCOVERY:
            Log.w(TAG, "timeout: service discovery");
            onServicesDiscovered(mGatt, BluetoothGatt.GATT_FAILURE);
            break;
        case TIMEOUT_TYPE_GATT_REQUEST:
            GattRequest r = (GattRequest) msg.obj;
            Log.w(TAG, "timeout: " + r.mRequestType);
            sendTimeoutEvent(r);
            processQueuedRequests();
            break;
        }

        return true;
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onCharacteristicChanged(gatt, characteristic);
            } catch (Throwable t) {
                Log.e(TAG, "onCharacteristicChanged[" + i + "]: error", t);
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        cancelRequestTimeout();
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onCharacteristicRead(gatt, characteristic, status);
            } catch (Throwable t) {
                Log.e(TAG, "onCharacteristicRead[" + i + "]: error", t);
            }
        }
        processQueuedRequests();

    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic, int status) {
        cancelRequestTimeout();
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onCharacteristicWrite(gatt, characteristic, status);
            } catch (Throwable t) {
                Log.e(TAG, "onCharacteristicWrite[" + i + "]: error", t);
            }
        }
        processQueuedRequests();

    }

    private void registerPairingReceiver() {
        if (mContext != null && !mPairingReceiverRegistered) {
            if (mPairingReceiver == null) {
                mPairingReceiver = new BluetoothPairingReceiver();
            }
            mContext.registerReceiver(mPairingReceiver, new IntentFilter(
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED));
            mPairingReceiverRegistered = true;
        }
    }

    private void unregisterPairingReceiver() {
        if (mContext != null && mPairingReceiverRegistered && mPairingReceiver != null) {
            mContext.unregisterReceiver(mPairingReceiver);
            mPairingReceiverRegistered = false;
        }
    }

    private void onPaired(boolean paired) {
        mHandler.removeMessages(TIMEOUT_TYPE_PAIRING);
        unregisterPairingReceiver();
        LePairingCallback[] cb = getPairingCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onPaired(paired);
            } catch (Throwable t) {
                Log.e(TAG, "onPaired: error " + cb[i], t);
            }
        }
    }

    /**
     * Cleanup after a connection is closed
     */
    private void closeAndCleanupGattConnection() {
        Log.d(TAG, "closeAndCleanupGattConnection");
        synchronized (mConnectionLock) {

            // Check if we are already closed
            if (mIsClosedAndCleanedUp) {
                return;
            }

            // Remove all pending requests
            mHandler.removeCallbacksAndMessages(null);
            mQueuedRequests.clear();
            mPendingRequest = null;

            // Close gatt connection
            if (mGatt != null) {
                try {
                    mGatt.close();
                    mGatt = null;
                } catch (Throwable t) {
                    Log.w(TAG, "closeAndCleanupGattConnection: error", t);
                }
            }
            mIsClosedAndCleanedUp = true;
        }
    }

    /**
     * Initialize state variables before connect request
     */
    private void initStateBeforeConnect() {
        synchronized (mConnectionLock) {
            mServicesDiscovered = false;
            mPendingClose = false;
            mIsClosedAndCleanedUp = false;
            mIsUserDisconnectRequested = false;
        }
    }

    private void startConnectDisconnectTimeout(boolean connect) {
        synchronized (mConnectionLock) {
            if (connect) {
                mPendingConnectDisconnectRequest = REQUEST_CONNECT;
                mHandler.sendEmptyMessageDelayed(TIMEOUT_TYPE_CONNECT, TIMEOUT_CONNECT_MS);
                if (DBG) {
                    Log.d(TAG, "startConnectDisconnectTimeout(): starting connect timeout");
                }
            } else {
                mPendingConnectDisconnectRequest = REQUEST_DISCONNECT;
                mHandler.sendEmptyMessageDelayed(TIMEOUT_TYPE_DISCONNECT, TIMEOUT_DISCONNECT_MS);
                if (DBG) {
                    Log.d(TAG, "startConnectDisconnectTimeout(): starting disconnect timeout");
                }
            }
        }
    }

    private void stopConnectDisconnectTimeout() {
        synchronized (mConnectionLock) {
            if (mPendingConnectDisconnectRequest != REQUEST_NONE) {
                if (DBG) {
                    Log.d(TAG, "stopConnectDisconnectTimeout(): cancelling timeout "
                            + getMessageString(mPendingConnectDisconnectRequest));
                }
                if (mPendingConnectDisconnectRequest == REQUEST_CONNECT) {
                    mHandler.removeMessages(TIMEOUT_TYPE_CONNECT);
                } else if (mPendingConnectDisconnectRequest == REQUEST_DISCONNECT) {
                    mHandler.removeMessages(TIMEOUT_TYPE_DISCONNECT);
                }
                mPendingConnectDisconnectRequest = REQUEST_NONE;
            }
        }
    }

    private boolean hasPendingConnectDisconnect() {
        return mPendingConnectDisconnectRequest != REQUEST_NONE;
    }

    public void startConnectCompleteTimer(int time) {
        if (DBG) {
            Log.d(TAG, "startConnectCompleteTimer(): timer=" + time);
        }
        if (time > 0) {
            mHandler.sendEmptyMessageDelayed(CONNECT_COMPLETE, time);
        }
    }

    public void stopConnectCompleteTimer() {
        if (DBG) {
            Log.d(TAG, "stopConnectCompleteTimer(): timer=");
        }
        mHandler.removeMessages(CONNECT_COMPLETE);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (DBG) {
            Log.d(TAG, "onConnectionStateChange: state=" + getConnectionStateString(newState)
                    + ", status=" + status);
        }
        stopConnectDisconnectTimeout();
        mConnectState = newState;
        // Handle disconnect
        if (newState == BluetoothGatt.STATE_DISCONNECTED
                || newState == BluetoothGatt.STATE_DISCONNECTING) {
            // If we got a disconnect, check if if this was user initiated.
            // If not, try to reconnect
            if (DBG) {
                Log.d(TAG, "onConnectionStateChange: mIsUserDisconnectRequested="
                        + mIsUserDisconnectRequested + ",mPendingClose=" + mPendingClose
                        + ", mRetryFailedConnection=" + mRetryFailedConnection);
            }
            if (mPendingClose) {
                closeAndCleanupGattConnection();
            } else {
                stopConnectCompleteTimer();
                mHandler.removeCallbacksAndMessages(null);
                mQueuedRequests.clear();
            }

            if (!mIsUserDisconnectRequested && mRetryFailedConnection
                    && ++mReconnectCount < mMaxConnectionRetries) {
                if (DBG) {
                    Log.d(TAG, "onConnectionStateChange: Auto-reconnecting...");
                }
                connectDelayed();
                return;
            }
        } else if (newState == BluetoothGatt.STATE_CONNECTED) {
            if (mDiscoverServices) {
                // Android 4.3: some devices can't handle an immediate service
                // discovery request
                discoverServicesDelayed();
            }
        }
        // Send events to interested listeners
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onConnectionStateChange(gatt, status, newState);
            } catch (Throwable t) {
                Log.e(TAG, "onConnectionStateChange[" + i + "]: error", t);
            }
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        cancelRequestTimeout();
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onDescriptorRead(gatt, descriptor, status);
            } catch (Throwable t) {
                Log.e(TAG, "onDescriptorRead[" + i + "]: error", t);
            }
        }
        processQueuedRequests();

    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        cancelRequestTimeout();
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onDescriptorWrite(gatt, descriptor, status);
            } catch (Throwable t) {
                Log.e(TAG, "onDescriptorWrite[" + i + "]: error", t);
            }
        }
        processQueuedRequests();
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onReadRemoteRssi(gatt, rssi, status);
            } catch (Throwable t) {
                Log.e(TAG, "onReadRemoteRssi[" + i + "]: error", t);
            }
        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onReliableWriteCompleted(gatt, status);
            } catch (Throwable t) {
                Log.e(TAG, "onReliableWriteCompleted[" + i + "]: error", t);
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        mHandler.removeMessages(TIMEOUT_TYPE_SERVICE_DISCOVERY);

        // Check if we need to retry
        if (status == BluetoothGatt.GATT_SUCCESS) {
            mServicesDiscovered = true;
        }
        if (mDiscoverServices && !mServicesDiscovered) {
            // Disconnect and reconnect if mRetryFailedConnection=true
            disconnect(false, false);
            return;
        }
        BluetoothGattCallback[] cb = getGattCallbacks();
        for (int i = 0; i < cb.length; i++) {
            try {
                cb[i].onServicesDiscovered(gatt, status);
            } catch (Throwable t) {
                Log.e(TAG, "onServicesDiscovered[" + i + "]: error", t);
            }
        }
    }

    private boolean connect(boolean isReconnect) {
        synchronized (mConnectionLock) {

            // Check if we have a pending connect/disconnect request
            if (hasPendingConnectDisconnect()) {
                Log.w(TAG, "connect: cannot connect, because there is a pending request "
                        + getMessageString(mPendingConnectDisconnectRequest));
                return false;
            }
            if (!isReconnect) {
                mReconnectCount = 0;
            }
            // Remove any queued connect requests
            mHandler.removeMessages(REQUEST_CONNECT);
            initStateBeforeConnect();
            boolean success = false;
            startConnectDisconnectTimeout(true);
            if (mGatt != null) {
                success = mGatt.connect();
            } else {
                mGatt = mDevice.connectGatt(mContext, mAutoConnect, this);
                success = (null != mGatt);
            }
            return success;
        }
    }

    private boolean disconnect(boolean isUserRequested, boolean closeResources) {
        synchronized (mConnectionLock) {
            // Check if we have a pending connect/disconnect request
            if (hasPendingConnectDisconnect()) {
                Log.w(TAG, "disconnect: cannot disconnect, because there is a pending request "
                        + getMessageString(mPendingConnectDisconnectRequest));
                return false;
            }
            stopConnectCompleteTimer();
            mIsUserDisconnectRequested = isUserRequested;
            mServicesDiscovered = false;
            if (mGatt != null) {
                mPendingClose = closeResources;
                startConnectDisconnectTimeout(false);
                mGatt.disconnect();
            } else {
                Log.w(TAG, "disconnect: cannot disconnect, because gatt is null");
                return false;
            }
            return true;
        }
    }

    private boolean discoverServicesDelayed() {
        if (mGatt == null) {
            return false;
        }
        mHandler.sendEmptyMessageDelayed(REQUEST_DISCOVER_SERVICES, DISCOVER_SERVICES_DELAY_MS);
        return true;
    }

    // ---------------------Public API--------------------------------------
    public GattRequestManager(Context ctx, BluetoothDevice device) {
        mDevice = device;
        mContext = ctx;
    }

    public void addTimeoutCallback(GattTimeoutCallback l) {
        synchronized (mGattTimeoutCallbacks) {
            if (!mGattTimeoutCallbacks.contains(l)) {
                mGattTimeoutCallbacks.add(l);
            }
        }
    }

    public void removeTimeoutCallback(GattTimeoutCallback l) {
        synchronized (mGattTimeoutCallbacks) {
            mGattTimeoutCallbacks.remove(l);
        }
    }

    public void addCallback(BluetoothGattCallback cb) {
        synchronized (mGattCallbacks) {
            if (!mGattCallbacks.contains(cb)) {
                mGattCallbacks.add(cb);
            }
        }
    }

    public void removeCallback(BluetoothGattCallback cb) {
        synchronized (mGattCallbacks) {
            mGattCallbacks.remove(cb);
        }
    }

    public void addPairingCallback(LePairingCallback cb) {
        synchronized (mGattCallbacks) {
            if (!mPairingCallbacks.contains(cb)) {
                mPairingCallbacks.add(cb);
            }
        }
    }

    public void removePairingCallback(LePairingCallback cb) {
        synchronized (mGattCallbacks) {
            mPairingCallbacks.remove(cb);
        }
    }

    public void removeAllCallbacks() {
        mGattCallbacks.clear();
        mGattTimeoutCallbacks.clear();
        mPairingCallbacks.clear();
    }

    public void setAutoConnect(boolean autoConnect) {
        mAutoConnect = autoConnect;
    }

    public void setRetryFailedConnection(boolean retry, int maxRetries) {
        synchronized (mConnectionLock) {
            mRetryFailedConnection = retry;
            mMaxConnectionRetries = maxRetries;
        }
    }

    public void setPairingTimeout(int timeoutMs) {
        if (timeoutMs > 0) {
            mPairingTimeoutMs = timeoutMs;
        }
    }

    public void setDiscoverServices(boolean discoverServices) {
        mDiscoverServices = discoverServices;
    }

    public boolean unpair() {
        registerPairingReceiver();
        boolean success = mDevice != null && unpair(mDevice);
        if (success) {
            mHandler.sendEmptyMessageDelayed(TIMEOUT_TYPE_PAIRING, mPairingTimeoutMs);
        } else {
            unregisterPairingReceiver();
        }
        return success;
    }

    public boolean pair() {
        registerPairingReceiver();
        boolean success = mDevice != null && pair(mDevice);
        if (success) {
            mHandler.sendEmptyMessageDelayed(TIMEOUT_TYPE_PAIRING, mPairingTimeoutMs);
        } else {
            unregisterPairingReceiver();
        }
        return success;
    }

    /**
     * Connect to remote device
     *
     * @return
     */
    public boolean connect() {
        return connect(false);
    }

    public boolean connectDelayed() {
        // Check if we have a pending connect/disconnect request
        if (hasPendingConnectDisconnect()) {
            Log.w(TAG, "connect: cannot connect, because there is a pending request "
                    + getMessageString(mPendingConnectDisconnectRequest));
            return false;
        }
        mHandler.sendEmptyMessageDelayed(REQUEST_CONNECT, CONNECT_DELAY_MS);
        return true;
    }

    /**
     * Disconnect from the remote device (if connected), and finish the gatt
     * connection if requested
     */
    public boolean disconnect(boolean closeResources) {
        return disconnect(true, closeResources);
    }

    /**
     * Returns true if a LE connection is established to the remote device. This
     * does NOT guarantee that services/ characteristics have been discovered
     *
     * @return
     */
    public boolean isConnected() {
        return mConnectState == BluetoothGatt.STATE_CONNECTED;
    }

    public boolean servicesDiscovered() {
        return mServicesDiscovered;
    }

    public boolean discoverServices() {
        if (mGatt == null) {
            return false;
        }

        mHandler.sendEmptyMessage(REQUEST_DISCOVER_SERVICES);
        return true;
    }

    public void read(BluetoothGattDescriptor d) {
        queueRequest(new GattRequest(REQUEST_READ_DESCRIPTOR, null, d), false);
    }

    public void read(BluetoothGattCharacteristic c, boolean immediate) {
        queueRequest(new GattRequest(REQUEST_READ_CHAR, c, null), immediate);
    }

    public void read(BluetoothGattCharacteristic c) {
        read(c, false);
    }

    public void write(BluetoothGattDescriptor d, byte[] value) {
        queueRequest(new GattRequest(REQUEST_WRITE_DESCRIPTOR, null, d, value), false);
    }

    public void write(BluetoothGattCharacteristic c, byte[] value, boolean immediate) {
        queueRequest(new GattRequest(REQUEST_WRITE_CHAR, c, null, value), immediate);
    }

    public void write(BluetoothGattCharacteristic c, byte[] value) {
        write(c, value, false);
    }

    public void write(int id, BluetoothGattCharacteristic c, byte[] value) {
        queueRequest(new GattRequest(id, REQUEST_WRITE_CHAR, c, null, value), false);

    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic c, boolean enable) {
        return mGatt != null && mGatt.setCharacteristicNotification(c, enable);
    }

    public void removeRequest(int requestType, BluetoothGattCharacteristic c) {
        synchronized (mQueuedRequests) {
            Iterator<GattRequest> i = mQueuedRequests.iterator();
            while (i.hasNext()) {
                GattRequest r = i.next();
                if (r.mRequestType == requestType && r.mCharacteristic == c) {
                    i.remove();
                }
            }
        }
    }

    public void removeRequest(int requestType, BluetoothGattDescriptor d) {
        synchronized (mQueuedRequests) {
            Iterator<GattRequest> i = mQueuedRequests.iterator();
            while (i.hasNext()) {
                GattRequest r = i.next();
                if (r.mRequestType == requestType && r.mDescriptor == d) {
                    i.remove();
                }
            }
        }
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public BluetoothGattService findService(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        List<BluetoothGattService> services = mGatt.getServices();
        if (services == null) {
            return null;
        }
        Iterator<BluetoothGattService> i = services.iterator();
        while (i.hasNext()) {
            BluetoothGattService s = i.next();
            if (uuid.equals(s.getUuid())) {
                return s;
            }
        }
        return null;
    }

    public BluetoothGattCharacteristic findCharacteristic(BluetoothGattService s, UUID charUuid) {
        if (s == null || charUuid == null) {
            return null;
        }
        List<BluetoothGattCharacteristic> chars = s.getCharacteristics();
        if (chars == null) {
            return null;
        }
        Iterator<BluetoothGattCharacteristic> i = chars.iterator();
        while (i.hasNext()) {
            BluetoothGattCharacteristic c = i.next();
            if (charUuid.equals(c.getUuid())) {
                return c;
            }
        }
        return null;
    }

    public BluetoothGattCharacteristic findCharacteristic(UUID serviceUuid, UUID charUuid) {
        return findCharacteristic(findService(serviceUuid), charUuid);
    }

    public void finish() {
        synchronized (mConnectionLock) {
            if (mGatt != null) {
                mGatt.close();
                mGatt = null;
            }
        }
    }

    public void debugDumpInfo() {
        Log.d(TAG, "DEBUG mAutoConnect=" + mAutoConnect);
        Log.d(TAG, "DEBUG mRetryFailedConnection=" + mRetryFailedConnection);
        Log.d(TAG, "DEBUG mMaxConnectionRetries=" + mMaxConnectionRetries);
        Log.d(TAG, "DEBUG mDiscoverServices=" + mDiscoverServices);
        Log.d(TAG, "");
        Log.d(TAG, "DEBUG GattRequestManager = " + this);
        Log.d(TAG, "DEBUG mGattCallbacks size= " + mGattCallbacks.size());
        Log.d(TAG, "DEBUG mGattRequestListeners size= " + mGattTimeoutCallbacks.size());
        Log.d(TAG, "");
        Log.d(TAG, "DEBUG mQueuedRequests size=" + mQueuedRequests.size());
        Log.d(TAG, "DEBUG mPendingRequest=" + mPendingRequest + ", id= "
                + (mPendingRequest != null ? mPendingRequest.mId : "") + ", type="
                + (mPendingRequest != null ? mPendingRequest.mRequestType : ""));
        Log.d(TAG, "DEBUG mDevice=" + mDevice);
        Log.d(TAG, "DEBUG mGatt=" + mGatt);
        Log.d(TAG, "DEBUG mConnectState=" + getConnectionStateString(mConnectState));
        Log.d(TAG, "DEBUG mServicesDiscovered=" + mServicesDiscovered);
        Log.d(TAG, "DEBUG mPendingConnectDisconnectRequest=" + mPendingConnectDisconnectRequest);
        Log.d(TAG, "DEBUG mIsUserDisconnectRequested=" + mIsUserDisconnectRequested);
        Log.d(TAG, "DEBUG mPendingClose=" + mPendingClose);
        Log.d(TAG, "DEBUG mIsClosedAndCleanedUp=" + mIsClosedAndCleanedUp);
    }
}
