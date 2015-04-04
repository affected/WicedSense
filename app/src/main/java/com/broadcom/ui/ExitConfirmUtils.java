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

import com.broadcom.ui.ExitConfirmFragment.ExitConfirmCallback;

import android.app.FragmentManager;

/**
 * Helper class to show/hide the ExitConfirm dialog, and to receive callbacks
 * for the user's interaction
 *
 * @author fredc
 *
 */
public class ExitConfirmUtils implements ExitConfirmCallback {

    private final ExitConfirmFragment.ExitConfirmCallback mListener;
    private ExitConfirmFragment mDialog;

    @Override
    public void onExit() {
        mDialog = null;
        if (mListener != null) {
            try {
                mListener.onExit();
            } catch (Throwable t) {
            }
        }
    }

    @Override
    public void onExitCancelled() {
        mDialog = null;
        if (mListener != null) {
            try {
                mListener.onExitCancelled();
            } catch (Throwable t) {
            }
        }
    }

    public ExitConfirmUtils(ExitConfirmFragment.ExitConfirmCallback listener) {
        mListener = listener;
    }

    public void show(FragmentManager mgr) {

        if (mDialog == null) {
            mDialog = ExitConfirmFragment.createDialog(this);
            mDialog.show(mgr, null);
        }
    }

    public void dismiss() {
        if (mDialog != null) {
            mDialog.setCallback(null);
            mDialog.dismiss();
            mDialog = null;
        }
    }

}
