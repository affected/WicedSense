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
 */
import com.broadcom.app.wicedsense.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Helper class to display a dialog requesting the user to turn on Bluetooth, if
 * it is not already turned on
 *
 *
 */
public class BluetoothEnabler {
    public static final int REQUEST_ENABLE_BT = 100;

    public static boolean checkBluetoothOn(Activity a) {

        BluetoothManager bluetoothManager = (BluetoothManager) a
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothManager == null || bluetoothAdapter == null) {
            Toast.makeText(a, R.string.notifier_bluetooth_unsupported, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            a.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
    }

}
