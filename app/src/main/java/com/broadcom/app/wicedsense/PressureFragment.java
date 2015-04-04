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
package com.broadcom.app.wicedsense;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PressureFragment extends BaseThermoFragment {

    @Override
    public void initRangeValues() {
        mMaxValue = SensorDataParser.SENSOR_PRESSURE_MAX;
        mMinValue = SensorDataParser.SENSOR_PRESSURE_MIN;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.pressure_fragment, null);
        return v;
    }

    @Override
    protected void setGaugeText(float value) {
        mGaugeValue.setText(getString(R.string.pressure_value, String.valueOf((int) value)));
    }

    @Override
    protected String getPropertyName() {
        return "pres";
    }

}
