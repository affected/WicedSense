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
package com.broadcom.app.wicedsmart.ota.ui;

/**
 * NOTE: replace "R.class" with your R.class declared in your application if you embed OTA in your app
 * Also, make sure you include the following in your application
 *
 * values/strings_ota.xml in
 * layout/ota_app_info.xml
 */
import com.broadcom.app.wicedsense.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import android.app.FragmentManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.broadcom.app.wicedsmart.ota.OtaSettings;
import com.broadcom.app.wicedsmart.ota.OtaManager;
import com.broadcom.app.wicedsmart.ota.OtaCallback;
import com.broadcom.util.GattRequestManager;

public class OtaUiHelper implements com.broadcom.app.wicedsmart.ota.ui.OtaChooserFragment.Callback,
        OtaConfirmFragment.Callback, OtaCallback,
        com.broadcom.app.wicedsmart.ota.ui.OtaFinishedFragment.Callback,
        com.broadcom.app.wicedsmart.ota.ui.OtaProgressFragment.Callback {
    private static final String TAG = OtaSettings.TAG_PREFIX + "OtaUiHelper";

    private static final String FRAG_OTA_PICK = "ota_pick";
    private static final String FRAG_OTA_CONFIRM = "ota_confirm";
    private static final String FRAG_OTA_PROGRESS = "ota_progress";
    private static final String FRAG_OTA_FINISHED = "ota_finished";

    private static class RawOtaResource implements OtaResource {
        private final String mName;
        private final Resources mResources;
        private final int mResourceId;
        private long mLength;
        private InputStream mInputStream;
        private final int mAppId;
        private final int mMajor;
        private final int mMinor;
        private final boolean mMandatory;

        public RawOtaResource(String name, int appId, int major, int minor, Resources resources,
                int resourceId, boolean mandatory) {
            mAppId = appId;
            mName = name;
            mResources = resources;
            mResourceId = resourceId;
            mMandatory = mandatory;
            InputStream i = getStream();
            try {
                // Calculate the length
                int length = 0;
                if (i != null) {
                    while (i.read() >= 0) {
                        length++;
                    }
                }
                mLength = length;
            } catch (Throwable t) {
                Log.d(TAG, "RawOtaResource(): error opening resource " + mResourceId, t);
                throw new RuntimeException();
            }
            Log.d(TAG, "length = " + mLength);
            closeStream();
            mMajor = major;
            mMinor = minor;
        }

        @Override
        public String getName() {
            return mName;
        }

        @Override
        public int getAppId() {
            return mAppId;
        }

        @Override
        public int getMajor() {
            return mMajor;
        }

        @Override
        public int getMinor() {
            return mMinor;
        }

        @Override
        public long getLength() {
            return mLength;
        }

        @Override
        public boolean isMandatory() {
            return mMandatory;
        }

        @Override
        public InputStream getStream() {
            try {
                return mInputStream = mResources.openRawResource(mResourceId);
            } catch (Throwable t) {

            }
            return null;
        }

        @Override
        public void closeStream() {
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                    mInputStream = null;
                } catch (Throwable t) {
                }
            }
        }
    }

    private static class FileOtaResource implements OtaResource {
        File mOtaFile;
        FileInputStream mFileStream;

        private FileOtaResource(File otaFile) {
            mOtaFile = otaFile;
        }

        @Override
        public String getName() {
            return mOtaFile.getName();
        }

        @Override
        public long getLength() {
            return mOtaFile.length();
        }

        @Override
        public InputStream getStream() {
            try {
                mFileStream = new FileInputStream(mOtaFile);
                return mFileStream;
            } catch (Throwable t) {
            }
            return null;
        }

        @Override
        public void closeStream() {
            if (mFileStream != null) {
                try {
                    mFileStream.close();
                    mFileStream = null;
                } catch (Throwable t) {
                }
            }
        }

        @Override
        public int getAppId() {
            return 0;
        }

        @Override
        public int getMajor() {
            return 0;
        }

        @Override
        public int getMinor() {
            return 0;
        }

        @Override
        public boolean isMandatory() {
            return false;
        }
    }

    public static interface OtaUiCallback {
        public void onOtaFinished(boolean completed);
    }

    public static OtaResource createRawOtaResource(String name, int appId, int major, int minor,
            Resources resources, int resourceId, boolean mandatory) {
        return new RawOtaResource(name, appId, major, minor, resources, resourceId, mandatory);
    }

    public static void createOtaResources(File fwDirectory, FilenameFilter filter,
            List<OtaResource> resources) {
        File[] fwFiles = null;

        if (fwDirectory != null && fwDirectory.isDirectory()) {
            if (filter == null) {
                fwFiles = fwDirectory.listFiles();
            } else {
                fwFiles = fwDirectory.listFiles(filter);
            }
        }
        if (fwFiles != null && fwFiles.length > 0) {
            for (int i = 0; i < fwFiles.length; i++) {
                FileOtaResource r = new FileOtaResource(fwFiles[i]);
                if (resources != null) {
                    resources.add(r);
                }
            }
        }
    }

    private Context mContext;
    private GattRequestManager mGatt;
    private FragmentManager mFragMgr;
    private final LinkedList<OtaResource> mOtaResources = new LinkedList<OtaResource>();

    private BluetoothDevice mDevice;
    private OtaUiCallback mCallback;
    private OtaConfirmFragment mConnectConfirmFragment;
    private OtaProgressFragment mProgressFragment;
    private boolean mShowProgressDialog = true;
    private boolean mShowFinishedDialog = true;
    private int mLastOtaState;
    private boolean mDownloadProgressStarted;
    private OtaManager mOtaManager;
    private OtaResource mSelectedResource;
    private boolean mDisconnectOnFinished;

    private void reset() {
        mOtaResources.clear();
        mDevice = null;
        mCallback = null;
        mProgressFragment = null;
        mShowProgressDialog = true;
        mShowFinishedDialog = true;
        mLastOtaState = 0;
        mDownloadProgressStarted = false;
        mOtaManager = null;
        mSelectedResource = null;
    }

    private void showFinishedDialog(String msg, boolean error, boolean abort) {
        if (mProgressFragment != null) {
            mProgressFragment.dismiss();
            mProgressFragment = null;
        }
        if (mShowFinishedDialog) {
            OtaFinishedFragment mFinishedFragment = OtaFinishedFragment.createDialog(this, msg,
                    error, abort);
            mFinishedFragment.show(mFragMgr, FRAG_OTA_FINISHED);
        } else {
            onOtaFinished(!error & !abort);
        }
    }

    private void getSelectedFirmware() {
        if (mOtaResources != null && mOtaResources.size() > 0) {
            if (mOtaResources.size() == 1) {
                onOtaSoftwareSelected(mOtaResources.get(0));
            } else {
                // Show chooser
                OtaChooserFragment.createDialog(mOtaResources, this).show(mFragMgr, FRAG_OTA_PICK);
            }
            return;
        }
        // Show no firmware dialog
        showFinishedDialog(mContext.getString(R.string.ota_nofirmware_msg), true, false);
    }

    @Override
    public void onOtaSoftwareSelected(OtaResource r) {
        mSelectedResource = r;
        showConfirm();
    }

    @Override
    public void onOtaProgressDialogShow() {
        startUpdate();
    }

    private boolean mIsConfirmDone;

    private void showConfirm() {
        mConnectConfirmFragment = OtaConfirmFragment.createDialog(this, mSelectedResource);
        mConnectConfirmFragment.show(mFragMgr, FRAG_OTA_CONFIRM);
    }

    @Override
    public void onOtaConfirmed() {
        mIsConfirmDone = true;
        initUpdate();
    }

    private void initUpdate() {
        if (mShowProgressDialog) {
            mProgressFragment = OtaProgressFragment.createDialog(this,
                    mContext.getString(R.string.ota_dialog_progress_msg_starting));
            mProgressFragment.show(mFragMgr, FRAG_OTA_PROGRESS);
        } else {
            startUpdate();
        }
    }

    private void startUpdate() {
        Log.d(TAG, "startUpdate: selected resource = " + mSelectedResource);
        if (mSelectedResource == null) {
            showFinishedDialog(mContext.getString(R.string.ota_error_fw_open_error), true, false);
            return;
        }
        // Create a Gatt Instance
        if (mGatt == null) {
            mGatt = new GattRequestManager(mContext, mDevice);
        }
        BufferedInputStream in = null;
        long fwSizeBytes = 0;
        try {
            in = new BufferedInputStream(mSelectedResource.getStream());
            fwSizeBytes = mSelectedResource.getLength();
            // Create OtaManager instance
            mOtaManager = new OtaManager();
            mOtaManager.setDisconnectOnFinished(mDisconnectOnFinished);
            mOtaManager.addCallback(this);
            Log.d(TAG, "Starting update: size=" + fwSizeBytes);
            // Start upgrade
            mOtaManager.startUpdate(mContext, mGatt, (int) fwSizeBytes, in);
        } catch (Throwable t) {
            Log.e(TAG, "startUpdate(): error", t);
            showFinishedDialog(mContext.getString(R.string.ota_error_fw_open_error), true, false);
        }

    }

    @Override
    public void onOtaStateChanged(int state) {
        Log.d(TAG, "onOtaStateChanged: state= " + state + "(" + OtaManager.getStateString(state)
                + ")");

        if (mProgressFragment == null || mLastOtaState == state) {
            Log.d(TAG, "onOtaStateChanged: same state...ignoring...");
            return;
        }
        mLastOtaState = state;
        switch (state) {
        case OtaManager.STATE_CONNECT:
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_connecting));
            break;
        case OtaManager.STATE_DISCOVER:
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_discovering));
            break;
        case OtaManager.STATE_ENABLE_NOTIFY:
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_enablenotify));
            break;

        case OtaManager.STATE_PREPARE_DOWNLOAD:
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_preparing));
            break;
        case OtaManager.STATE_START_DOWNLOAD:
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_enablenotify));
            break;
        case OtaManager.STATE_SEND_FW_INFO:
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_send_fw_info));
            break;
        case OtaManager.STATE_SEND_FW:
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_send_fw));
            break;
        case OtaManager.STATE_VERIFY_FW:
            mProgressFragment.setProgress(mProgressFragment.mProgressDialog.getMax());
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_verify_fw));
            break;
        case OtaManager.STATE_UPGRADE_COMPLETED:
            showFinishedDialog(mContext.getString(R.string.ota_dialog_completed), false, false);
            break;
        default:
        }
    }

    @Override
    public void onOtaError(int currentState, int statusCode) {
        Log.d(TAG,
                "onOtaError: currentState= " + currentState + "("
                        + OtaManager.getStateString(currentState) + "), status="
                        + OtaManager.getStatusString(statusCode));

        String message = null;
        String status = OtaManager.getStatusString(statusCode);
        switch (statusCode) {
        case OtaManager.WS_UPGRADE_STATUS_UNSUPPORTED_COMMAND:
        case OtaManager.WS_UPGRADE_STATUS_ILLEGAL_STATE:
        case OtaManager.ERROR_SERVICE:
        case OtaManager.ERROR_DESCRIPTOR_WRITE:
        case OtaManager.ERROR_DESCRIPTOR_NOT_FOUND:
            message = mContext.getString(R.string.ota_error_internal, status);
            break;
        case OtaManager.ERROR_CONNECT:
            message = mContext.getString(R.string.ota_error_connect);
            break;
        case OtaManager.ERROR_DISCOVER:
            message = mContext.getString(R.string.ota_error_discover);
            break;
        case OtaManager.ERROR_TIMEOUT:
            message = mContext.getString(R.string.ota_error_timeout,
                    OtaManager.getStatusString(statusCode));
            break;
        case OtaManager.ERROR_CHARACTERISTIC_WRITE:
            message = mContext.getString(R.string.ota_error_write_char);
            break;

        case OtaManager.ERROR_FIRMWARE_INFO_WRITE:
            message = mContext.getString(R.string.ota_error_write_fw_info);

            break;
        case OtaManager.ERROR_FIRMWARE_WRITE:
            message = mContext.getString(R.string.ota_error_write_fw);

            break;

        case OtaManager.ERROR_FIRMWARE_INFO_READ:
        case OtaManager.WS_UPGRADE_STATUS_INVALID_APPID:
        case OtaManager.WS_UPGRADE_STATUS_INVALID_VERSION:
        case OtaManager.WS_UPGRADE_WRITE_STATUS_BAD_ID:
        case OtaManager.WS_UPGRADE_WRITE_STATUS_BAD_MAJOR:
            message = mContext.getString(R.string.ota_error_bad_fw_info, status);
            break;
        case OtaManager.ERROR_FIRMWARE_READ:
        case OtaManager.WS_UPGRADE_STATUS_INVALID_IMAGE:
        case OtaManager.WS_UPGRADE_STATUS_INVALID_IMAGE_SIZE:
        case OtaManager.WS_UPGRADE_WRITE_STATUS_TOO_MUCH_DATA:
        case OtaManager.WS_UPGRADE_WRITE_STATUS_TOO_SHORT:
            message = mContext.getString(R.string.ota_error_bad_fw, status);
            break;
        case OtaManager.WS_UPGRADE_STATUS_VERIFICATION_FAILED:
            message = mContext.getString(R.string.ota_error_fw_validate_error, status);
            break;
        }
        showFinishedDialog(message, true, false);

    }

    @Override
    public void onOtaUploadProgress(int loopCount, int bytesCurrent, int bytesTotal) {
        Log.d(TAG, "onOtaDownload: loopCount= " + loopCount + ", bytesCurrent=" + bytesCurrent
                + ", bytesTotal=" + bytesTotal);
        if (mProgressFragment != null) {
            if (!mDownloadProgressStarted) {
                mProgressFragment.setProgressMax(bytesTotal);
                mDownloadProgressStarted = true;
                mProgressFragment.setProgress(1);
            }
            mProgressFragment.setMessage(mContext
                    .getString(R.string.ota_dialog_progress_msg_send_fw));
            mProgressFragment.setProgress(bytesCurrent);
        }
    }

    @Override
    public void onOtaCancelled() {
        if (!mIsConfirmDone) {
            onOtaFinished(false);
        } else {
            abortUpdate();
        }
    }

    @Override
    public void onOtaAborted() {
        showFinishedDialog(mContext.getString(R.string.ota_aborted), true, true);
    }

    @Override
    public void onOtaFinished(boolean isComplete) {
        if (mSelectedResource != null) {
            mSelectedResource.closeStream();
        }
        // Make call back into app that launched this helper
        if (mCallback != null) {
            mCallback.onOtaFinished(isComplete);
        }
    }

    // ----------------Public APIs---------------------------------------------

    public void setOptionShowFinishedDialog(boolean showFinished) {
        mShowFinishedDialog = showFinished;
    }

    public void setOptionShowProgressDialog(boolean showProgress) {
        mShowProgressDialog = showProgress;
    }

    /**
     * Start OTA update for specified name and strem
     *
     * @param ctx
     * @param device
     * @param gattMgr
     * @param fragManager
     * @param otaName
     * @param inputStream
     * @param fwSize
     * @param cb
     */
    public void startUpdate(Context ctx, BluetoothDevice device, GattRequestManager gattMgr,
            FragmentManager fragManager, OtaResource otaResource, OtaUiCallback cb,
            boolean disconnectOnFinish) {
        LinkedList<OtaResource> resources = null;
        if (otaResource != null) {
            resources = new LinkedList<OtaResource>();
            resources.add(otaResource);
        }
        startUpdate(ctx, device, gattMgr, fragManager, resources, cb, disconnectOnFinish);
    }

    /**
     * Ask user which OTA file they want to upgrade to
     *
     * @param ctx
     * @param device
     * @param gattMgr
     * @param fragManager
     * @param fwDirectory
     * @param fwFileFilter
     * @param cb
     */
    public void startUpdate(Context ctx, BluetoothDevice device, GattRequestManager gattMgr,
            FragmentManager fragManager, List<OtaResource> otaResources, OtaUiCallback cb,
            boolean disconnectOnFinish) {
        reset();
        mDisconnectOnFinished = disconnectOnFinish;
        mGatt = gattMgr;
        mDevice = device;
        if (mGatt == null && mDevice == null) {
            return;
        }
        mContext = ctx;
        mCallback = cb;
        mFragMgr = fragManager;
        mOtaResources.clear();
        if (otaResources != null) {
            mOtaResources.addAll(otaResources);
        }
        getSelectedFirmware();

    }

    public void abortUpdate() {
        if (mOtaManager != null) {
            mOtaManager.abortUpdate();
        } else {
            onOtaAborted();
        }
    }

    public void finish() {
        mFragMgr = null;
        if (mOtaManager != null) {
            mOtaManager.finish();
        }
    }
}
