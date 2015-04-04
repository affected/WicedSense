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
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Fragment to display the Accelerometer data. This view consists of
 *
 * 1. A "bubble" view that displays the X and Y data as the position of a
 * bubble.
 *
 * 2. A hidden text view that displays the X,Y,Z raw data that is shown when the
 * user taps on this fragment.
 *
 */
public class AccelerometerFragment extends Fragment implements OnClickListener,
        AnimatorUpdateListener, Animated {
    private static final int mMaxValue = SensorDataParser.SENSOR_ACCEL_MAX;
    private static final int mMinValue = SensorDataParser.SENSOR_ACCEL_MIN;
    private static final float mValueLength = (mMaxValue - mMinValue);

    private ImageView mBubble;
    private TextView mRawX;
    private TextView mRawY;
    private TextView mRawZ;
    private View mClickablePanel;
    private boolean mRangeInited;
    private View mRange;
    private float mAccelMaxWidth;
    private float mAccelMaxHeight;

    // Current x,y,z values of accelerometer
    private float mX;
    private float mY;
    private float mZ;

    // Variables used for animation
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.accelerometer_fragment, null);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBubble = (ImageView) view.findViewById(R.id.bubble);
        mRange = view.findViewById(R.id.range);
        mClickablePanel = view.findViewById(R.id.clickable_panel);
        mClickablePanel.setOnClickListener(this);
        mRawX = (TextView) view.findViewById(R.id.raw_x);
        mRawY = (TextView) view.findViewById(R.id.raw_y);
        mRawZ = (TextView) view.findViewById(R.id.raw_z);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Restore saved values if we are returning from being paused
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
        // Save values if we pause
        outState.putBoolean("s", true);
        outState.putFloat("x", mX);
        outState.putFloat("y", mY);
        outState.putFloat("z", mZ);
    }

    /**
     * Get the horizontal and vertical boundaries that the bubble can travel
     * dynamically, based on the current screen size
     */
    private void initGaugeRange() {
        if (!mRangeInited) {
            int rangeWidth = mRange.getWidth();
            int rangeHeight = mRange.getHeight();
            if (rangeWidth == 0 || rangeHeight == 0) {
                return;
            }
            mAccelMaxWidth = rangeWidth - rangeWidth / 5;
            mAccelMaxHeight = rangeHeight - rangeHeight / 5;
            mRangeInited = true;
        }

    }

    /**
     * Limit the accelerometer value to the bounded range of accepted values
     *
     * @param value
     * @return
     */
    private float getBoundedValue(float value) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        if (value < mMinValue) {
            value = mMinValue;
        }
        return value;
    }

    /**
     * Set the accelerometer gauge NOTE: currently only x and y are displayed (z
     * is not displayed in the bubble view)
     *
     * @param x
     * @param y
     */
    private void setGauge(float x, float y) {
        // scale x translation
        mBubble.setTranslationX(x * mAccelMaxWidth / mValueLength);

        // scale y translation
        mBubble.setTranslationY(y * mAccelMaxHeight / mValueLength);
    }

    /**
     * Update the text widgets that show x,y,z
     */
    private void updateTextWidgets() {
        mRawX.setText(getString(R.string.raw_x, String.format("%.1f", mX)));
        mRawY.setText(getString(R.string.raw_y, String.format("%.1f", mY)));
        mRawZ.setText(getString(R.string.raw_z, String.format("%.1f", mZ)));
    }

    /**
     * Store the accelerator values x,y,z, and if animation is enabled, prepare
     * the view for animation..Otherwise, if animation is not enabled, update
     * the bubble view instantaneously.
     *
     * @param animation
     * @param x
     * @param y
     * @param z
     */
    public void setValue(AnimationManager animation, float x, float y, float z) {

        initGaugeRange();
        mX = getBoundedValue(x);
        mY = getBoundedValue(y);
        mZ = getBoundedValue(z);
        if (!hasAnimatedValuesChanged()) {
            return;
        }
        if (animation != null && animation.useAnimation()) {
            animation.prepareAnimated(this);
        } else {
            saveAnimatedValues();
            setGauge(mX, mY);
        }
        updateTextWidgets();
    }

    /**
     * Callback invoked when the user clicks on the accelerometer view. Toggles
     * showing/hiddening the raw x,y,z values
     */
    @Override
    public void onClick(View v) {
        int visibility = mRawX.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        mRawX.setVisibility(visibility);
        mRawY.setVisibility(visibility);
        mRawZ.setVisibility(visibility);
    }

    /**
     * Reset all UI components and values to initial conditions
     */
    public void reset() {
        mRawX.setText("");
        mRawY.setText("");
        mRawZ.setText("");
        mX = 0;
        mY = 0;
        mZ = 0;
    }

    /**
     * Called by the animation manager to display the first value used in the
     * animation.
     */
    @Override
    public void showFirstAnimatedValues() {
        setGauge(mX, mY);

    }

    /**
     * Called by the animation manager to determine if values have changed that
     * need to be animated
     */
    @Override
    public boolean hasAnimatedValuesChanged() {
        return mPreviousX != mX || mPreviousY != mY;
    }

    /**
     * Called by the animation manager to save the current values for the next
     * interaction of animation
     */
    @Override
    public void saveAnimatedValues() {
        mPreviousX = mX;
        mPreviousY = mY;
    }

    /**
     * Called by the animation manager to prepare the values to animate
     */
    @Override
    public void prepareAnimatedValues(List<PropertyValuesHolder> values) {
        values.add(PropertyValuesHolder.ofFloat("accel.x", mPreviousX, mX));
        values.add(PropertyValuesHolder.ofFloat("accel.y", mPreviousY, mY));
    }

    /**
     * Called by the animation manager to update the UI with the currently
     * specified values
     */
    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        Object x = animation.getAnimatedValue("accel.x");
        Object y = animation.getAnimatedValue("accel.y");
        if (x != null && y != null) {
            setGauge((Float) x, (Float) y);
        }
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            mRange.setVisibility(View.VISIBLE);
            mBubble.setVisibility(View.VISIBLE);
            mClickablePanel.setOnClickListener(this);
        } else {
            mRange.setVisibility(View.INVISIBLE);
            mBubble.setVisibility(View.INVISIBLE);
            mRawX.setVisibility(View.INVISIBLE);
            mRawY.setVisibility(View.INVISIBLE);
            mRawZ.setVisibility(View.INVISIBLE);
            mClickablePanel.setOnClickListener(null);
        }

    }
}
