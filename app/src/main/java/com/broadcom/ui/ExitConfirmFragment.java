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
package com.broadcom.ui;

/**
 * NOTE: replace "R.class" with your R.class declared in your application
 * Also, make sure you include the following resources
 * assets/license.html
 * values/strings_exitconfirm.xml
 */
import com.broadcom.app.wicedsense.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Displays a dialog prompting the user to quit the application
 *
 */
public class ExitConfirmFragment extends DialogFragment implements
        android.content.DialogInterface.OnClickListener {

    public static interface ExitConfirmCallback {
        public void onExit();

        public void onExitCancelled();
    }

    public static ExitConfirmFragment createDialog(ExitConfirmCallback cb) {
        ExitConfirmFragment f = new ExitConfirmFragment();
        f.mCallback = cb;
        return f;
    }

    private ExitConfirmCallback mCallback;

    public void setCallback(ExitConfirmCallback cb) {
        mCallback = cb;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.exit_title))
                .setPositiveButton(R.string.exit_ok, this)
                .setNegativeButton(R.string.exit_cancel, this).create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (mCallback != null) {
            mCallback.onExitCancelled();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mCallback != null) {
            try {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    mCallback.onExit();
                } else if (which == AlertDialog.BUTTON_NEGATIVE) {
                    mCallback.onExitCancelled();
                }

            } catch (Throwable t) {
            }
        }
    }

}
