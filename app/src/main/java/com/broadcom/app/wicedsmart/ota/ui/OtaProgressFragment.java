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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;

public class OtaProgressFragment extends DialogFragment implements
        android.content.DialogInterface.OnClickListener, OnShowListener {

    public interface Callback {
        public void onOtaProgressDialogShow();
        public void onOtaCancelled();
    }

    public static OtaProgressFragment createDialog(Callback cb, String initialMsg) {
        OtaProgressFragment f = new OtaProgressFragment();
        f.mCallback = cb;
        f.mInitialMsg = initialMsg;
        return f;
    }

    public ProgressDialog mProgressDialog;
    private Callback mCallback;
    private String mInitialMsg;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog d = new ProgressDialog(getActivity());
        d.setTitle(R.string.ota_dialog_progress_title);
        d.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.ota_lbl_cancel), this);
        d.setCancelable(false);
        d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        d.setMax(100);
        d.setProgress(0);
        d.setMessage(mInitialMsg == null ? "" : mInitialMsg);
        mProgressDialog = d;
        mProgressDialog.setOnShowListener(this);
        return d;
    }

    public void setProgressMax(int max) {
        ProgressDialog d = mProgressDialog;
        if (d == null)
            return;
        d.setMax(max);
    }

    public void setProgress(int p) {
        ProgressDialog d = mProgressDialog;
        if (d == null)
            return;
        d.setProgress(p);
    }

    public void setMessage(String m) {
        ProgressDialog d = mProgressDialog;
        if (d == null)
            return;
        d.setMessage(m);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            if (mCallback != null) {
                mCallback.onOtaCancelled();
            }
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (mCallback != null) {
            mCallback.onOtaProgressDialogShow();
        }
    }
}
