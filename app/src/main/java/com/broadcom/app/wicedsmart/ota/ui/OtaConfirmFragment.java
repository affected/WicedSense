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

import com.broadcom.app.wicedsmart.ota.OtaSettings;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

public class OtaConfirmFragment extends DialogFragment implements
        android.content.DialogInterface.OnClickListener {
    public interface Callback {
        public void onOtaConfirmed();

        public void onOtaCancelled();
    }

    private static final String TAG = OtaSettings.TAG_PREFIX + "OtaConnectConfirmFragment";

    public static OtaConfirmFragment createDialog(Callback cb, OtaResource resource) {
        OtaConfirmFragment f = new OtaConfirmFragment();
        f.mCallback = cb;
        f.mOtaResource = resource;
        return f;
    }

    private Callback mCallback;
    private boolean mIsOk;
    private OtaResource mOtaResource;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog()");
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.ota_confirm_msg, mOtaResource.getName()))
                .setPositiveButton(R.string.ota_lbl_ok, this)
                .setNegativeButton(R.string.ota_lbl_cancel, this);
        if (mOtaResource.isMandatory()) {
            b.setTitle(getString(R.string.ota_dialog_title_mandatory));
        } else {
            b.setTitle(getString(R.string.ota_dialog_title_available));
        }
        return b.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mCallback == null) {
            return;
        }
        if (which == AlertDialog.BUTTON_POSITIVE) {
            mIsOk = true;
            mCallback.onOtaConfirmed();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!mIsOk) {
            if (mCallback != null) {
                mCallback.onOtaCancelled();
            }
        }
    }

}
