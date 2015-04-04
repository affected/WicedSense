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
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Base class used for the thermometer, pressure, humidity
 *
 */
public abstract class BaseThermoFragment extends Fragment implements OnLayoutChangeListener,
        Animated {
    private static final int LEVEL_WIDTH_PX = 17;
    private static final int LEVEL_FRAME_WIDTH_PX = 110;

    private static final int RANGE_TOP_PAD_PX = 49;
    private static final int RANGE_BOTTOM_PAD_PX = 98;
    private static final int RANGE_HEIGHT_PX = 788;

    protected float mMaxValue;
    protected float mMinValue;
    protected float mValueRange;

    protected TextView mGaugeValue;
    protected View mGaugeBg;
    protected View mGaugeLevelFrame;
    protected View mGaugeLevel;
    protected View mGaugeRange;
    protected View mGaugeLabel;

    private boolean mGaugeAdjusted;

    protected float mPreviousValue;
    protected float mValue;
    protected boolean mValueSet;

    public BaseThermoFragment() {
        super();
        initRange();
    }

    protected void initRange() {
        initRangeValues();
        mValueRange = mMaxValue - mMinValue;
        mValue = mMinValue;
    }

    public void setInitialValue(float value) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        if (value < mMinValue) {
            value = mMinValue;
        }
        mValue = value;
        mValueSet = true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGaugeBg = view.findViewById(R.id.gauge_bg);
        mGaugeLabel = view.findViewById(R.id.gauge_label);

        mGaugeLevelFrame = view.findViewById(R.id.gauge_level_frame);
        mGaugeLevelFrame.addOnLayoutChangeListener(this);
        mGaugeLevel = view.findViewById(R.id.gauge_level);
        mGaugeLevel.setVisibility(View.INVISIBLE);
        mGaugeValue = (TextView) view.findViewById(R.id.gauge_value);
        mGaugeRange = view.findViewById(R.id.gauge_range);

        if (mValueSet) {
            setValue(null, mValue);
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
            int oldTop, int oldRight, int oldBottom) {
        if (v == mGaugeLevelFrame) {
            if (!mGaugeAdjusted) {
                setGauge(mValue);
                adjustGaugeLevel();
                return;
            }
            if (mGaugeLevel.getVisibility() != View.VISIBLE) {
                mGaugeLevel.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getBoolean("s", false)) {
            mValue = savedInstanceState.getFloat("v", mMinValue - 1);
            if (mValue >= mMinValue) {
                setValue(null, mValue);
                return;
            }
        }
        reset();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("s", mValueSet);
        outState.putFloat("v", mValue);
    }

    private void adjustGaugeLevel() {
        if (mGaugeAdjusted) {
            return;
        }
        int gaugeBgWidth = mGaugeBg.getWidth();
        int gaugeBgHeight = mGaugeBg.getHeight();

        if (gaugeBgWidth == 0 || gaugeBgHeight == 0) {
            return;
        }

        RelativeLayout.LayoutParams lpGaugeFrame = (RelativeLayout.LayoutParams) mGaugeLevelFrame
                .getLayoutParams();

        int newGaugeLevelWidth = (int) (gaugeBgWidth * LEVEL_WIDTH_PX / LEVEL_FRAME_WIDTH_PX);
        lpGaugeFrame.width = newGaugeLevelWidth;
        int newGaugeTopMargin = gaugeBgHeight * RANGE_TOP_PAD_PX / RANGE_HEIGHT_PX;
        int newGaugeBottomMargin = gaugeBgHeight * RANGE_BOTTOM_PAD_PX / RANGE_HEIGHT_PX;
        lpGaugeFrame.topMargin = newGaugeTopMargin;
        lpGaugeFrame.bottomMargin = newGaugeBottomMargin;
        mGaugeLevelFrame.setLayoutParams(lpGaugeFrame);
        mGaugeAdjusted = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        adjustGaugeLevel();
    }

    public void setValue(AnimationManager animation, float value) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        if (value < mMinValue) {
            value = mMinValue;
        }
        mValue = value;
        mValueSet = true;
        if (!hasAnimatedValuesChanged()) {
            return;
        }
        if (animation != null && animation.useAnimation()) {
            animation.prepareAnimated(this);
        } else {
            saveAnimatedValues();
            setGauge(mValue);
        }
        updateTextWidgets();
    }

    private void updateTextWidgets() {
        setGaugeText(mValue);
    }

    public void reset() {
        mGaugeValue.setText("");
        setGauge(mMinValue);
        mGaugeLevel.setVisibility(View.INVISIBLE);
    }

    protected void setGauge(float value) {
        if (mGaugeLevel == null) {
            return;
        }
        float gaugeOffset = ((mMaxValue - value) / mValueRange) * mGaugeLevel.getHeight();
        float newY = mGaugeRange.getY() + gaugeOffset;
        mGaugeLevel.setY(newY);
    }

    @Override
    public void showFirstAnimatedValues() {
        setGauge(mValue);
    }

    @Override
    public boolean hasAnimatedValuesChanged() {
        return mPreviousValue != mValue;
    }

    @Override
    public void saveAnimatedValues() {
        mPreviousValue = mValue;
    }

    @Override
    public void prepareAnimatedValues(List<PropertyValuesHolder> values) {
        values.add(PropertyValuesHolder.ofFloat(getPropertyName(), mPreviousValue, mValue));
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animator) {
        Object value = animator.getAnimatedValue(getPropertyName());
        if (value != null) {
            setGauge((Float) value);
        }
    }

    protected abstract void initRangeValues();

    protected abstract void setGaugeText(float value);

    protected abstract String getPropertyName();

}
