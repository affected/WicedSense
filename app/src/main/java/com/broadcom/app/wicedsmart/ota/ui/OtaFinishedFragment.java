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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class OtaFinishedFragment extends DialogFragment {
    public interface Callback {
        public void onOtaFinished(boolean updateComplete);
    }

    public static OtaFinishedFragment createDialog(Callback cb, String msg, boolean isError,
            boolean isAbort) {
        OtaFinishedFragment f = new OtaFinishedFragment();
        f.mCallback = cb;
        f.mMessage = msg;
        f.mIsError = isError;
        f.mIsAbort = isAbort;
        return f;
    }

    private Callback mCallback;
    private String mMessage;
    private boolean mIsError;
    private boolean mIsAbort;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.ota_dialog_title)).setMessage(mMessage)
                .setPositiveButton(R.string.ota_lbl_ok, null).create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mCallback != null) {
            mCallback.onOtaFinished(!mIsError && !mIsAbort);
        }
    }

}
