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

import java.util.List;

import com.broadcom.app.wicedsense.AnimationManager.Animated;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class GyroFragment extends Fragment implements OnClickListener, Animated {
    public static final String TAG = Settings.TAG_PREFIX + ".GyroFragment";
    private static final float mMaxValue = SensorDataParser.SENSOR_GYRO_MAX;
    private static final float mMinValue = SensorDataParser.SENSOR_GYRO_MIN;
    private static final float GAUGE_MAX_ANGLE = 120;
    private static final float GAUGE_MIN_ANGLE = -120;

    private ImageView mNeedleX;
    private ImageView mNeedleY;
    private ImageView mNeedleZ;
    private TextView mRawX;
    private TextView mRawY;
    private TextView mRawZ;
    private View mClickablePanel;

    private float mX;
    private float mY;
    private float mZ;

    private float mPreviousX;
    private float mPreviousY;
    private float mPreviousZ;

    private float getGaugeScaledAngle(float gyroValue) {
        float a = GAUGE_MIN_ANGLE
                + ((GAUGE_MAX_ANGLE - GAUGE_MIN_ANGLE) * (gyroValue - mMinValue) / (mMaxValue - mMinValue));
        return a;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.gyro_fragment, null);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNeedleX = (ImageView) view.findViewById(R.id.needle_x);
        mNeedleY = (ImageView) view.findViewById(R.id.needle_y);
        mNeedleZ = (ImageView) view.findViewById(R.id.needle_z);

        mClickablePanel = view.findViewById(R.id.clickable_panel);
        mClickablePanel.setOnClickListener(this);
        mRawX = (TextView) view.findViewById(R.id.raw_x);
        mRawY = (TextView) view.findViewById(R.id.raw_y);
        mRawZ = (TextView) view.findViewById(R.id.raw_z);
    }

    public boolean mPivotSet = false;

    public void setValue(AnimationManager animation, float x, float y, float z) {

        mX = x;
        mY = y;
        mZ = z;
        if (!hasAnimatedValuesChanged()) {
            return;
        }
        if (animation != null && animation.useAnimation()) {
            animation.prepareAnimated(this);
        } else {
            saveAnimatedValues();
            setGauge(x, y, z);
        }
        updateTextWidgets();
    }

    private void updateTextWidgets() {
        mRawX.setText(getString(R.string.raw_x, String.format("%.1f", mX)));
        mRawY.setText(getString(R.string.raw_y, String.format("%.1f", mY)));
        mRawZ.setText(getString(R.string.raw_z, String.format("%.1f", mZ)));
    }

    private void setGauge(float x, float y, float z) {
        setNeedle(mNeedleX, x);
        setNeedle(mNeedleY, y);
        setNeedle(mNeedleZ, z);
    }

    private void setNeedle(ImageView mNeedleView, float value) {

        if (value < mMinValue) {
            value = mMinValue;
        }
        if (value > mMaxValue) {
            value = mMaxValue;
        }

        float rotAngle = getGaugeScaledAngle(value);
        float curAngle = mNeedleView.getRotation();

        float rotationDist = Math.abs(curAngle - rotAngle);
        if (rotationDist < 180.0) {
            // Rotate normally
            mNeedleView.setRotation(rotAngle);
        } else {
            // Rotate in 2 steps
            float rotDir = (rotAngle > curAngle) ? 1.0f : -1.0f;
            float rot1 = curAngle + ((rotationDist / 2.0f) * rotDir);
            float rot2 = rotAngle;
            mNeedleView.setRotation(rot1);
            mNeedleView.setRotation(rot2);
        }
    }

    @Override
    public void onClick(View v) {
        int visibility = mRawX.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        mRawX.setVisibility(visibility);
        mRawY.setVisibility(visibility);
        mRawZ.setVisibility(visibility);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getBoolean("s", false)) {
            mX = savedInstanceState.getFloat("x", 0);
            mY = savedInstanceState.getFloat("y", 0);
            mZ = savedInstanceState.getFloat("z", 0);
            setValue(null, mX, mY, mZ);
            return;
        }
        reset();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("s", true);
        outState.putFloat("x", mX);
        outState.putFloat("y", mY);
        outState.putFloat("z", mZ);
    }

    public void reset() {
        mRawX.setText("");
        mRawY.setText("");
        mRawZ.setText("");
        mX = 0;
        mY = 0;
        mZ = 0;
    }

    @Override
    public boolean hasAnimatedValuesChanged() {
        return mPreviousX != mX || mPreviousY != mY || mPreviousZ != mZ;
    }

    @Override
    public void saveAnimatedValues() {
        mPreviousX = mX;
        mPreviousY = mY;
        mPreviousZ = mZ;
    }

    @Override
    public void showFirstAnimatedValues() {
        setGauge(mX, mY, mZ);
    }

    @Override
    public void prepareAnimatedValues(List<PropertyValuesHolder> values) {
        values.add(PropertyValuesHolder.ofFloat("gyro.x", mPreviousX, mX));
        values.add(PropertyValuesHolder.ofFloat("gyro.y", mPreviousY, mY));
        values.add(PropertyValuesHolder.ofFloat("gyro.z", mPreviousY, mZ));
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        Object x = animation.getAnimatedValue("gyro.x");
        Object y = animation.getAnimatedValue("gyro.y");
        Object z = animation.getAnimatedValue("gyro.z");
        if (x != null && y != null && z != null) {
            setGauge((Float) x, (Float) y, (Float) z);
        }
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            mNeedleX.setVisibility(View.VISIBLE);
            mNeedleY.setVisibility(View.VISIBLE);
            mNeedleZ.setVisibility(View.VISIBLE);
            mClickablePanel.setOnClickListener(this);
        } else {
            mNeedleX.setVisibility(View.INVISIBLE);
            mNeedleY.setVisibility(View.INVISIBLE);
            mNeedleZ.setVisibility(View.INVISIBLE);
            mRawX.setVisibility(View.INVISIBLE);
            mRawY.setVisibility(View.INVISIBLE);
            mRawZ.setVisibility(View.INVISIBLE);
            mClickablePanel.setOnClickListener(null);
        }
    }
}
