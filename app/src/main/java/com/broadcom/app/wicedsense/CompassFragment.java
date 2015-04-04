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

public class CompassFragment extends Fragment implements OnClickListener, Animated {
    private ImageView mNeedle;
    private TextView mRawX;
    private TextView mRawY;
    private TextView mRawZ;
    private TextView mRawAngle;
    private View mClickablePanel;

    private float mX;
    private float mY;
    private float mZ;
    private float mAngle;

    private float mPreviousAngle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.compass_fragment, null);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNeedle = (ImageView) view.findViewById(R.id.needle);

        mClickablePanel = view.findViewById(R.id.clickable_panel);
        mClickablePanel.setOnClickListener(this);
        mRawX = (TextView) view.findViewById(R.id.raw_x);
        mRawY = (TextView) view.findViewById(R.id.raw_y);
        mRawZ = (TextView) view.findViewById(R.id.raw_z);
        mRawAngle = (TextView) view.findViewById(R.id.raw_angle);
    }

    public void setValue(AnimationManager animation, float angle, float x, float y, float z) {


        if (angle >= 360) {
            angle -= 360;
        }
        if (angle < 0) {
            angle += 360;
        }

        mX = x;
        mY = y;
        mZ = z;
        mAngle = angle;
        if (!hasAnimatedValuesChanged()) {
            return;
        }
        if (animation != null && animation.useAnimation()) {
            animation.prepareAnimated(this);
        } else {
            saveAnimatedValues();
            mNeedle.setRotation(mAngle);
        }
        updateTextWidgets();
    }

    private void updateTextWidgets() {
        mRawX.setText(getString(R.string.raw_x, String.format("%.1f", mX)));
        mRawY.setText(getString(R.string.raw_y, String.format("%.1f", mY)));
        mRawZ.setText(getString(R.string.raw_z, String.format("%.1f", mZ)));
        mRawAngle.setText(getString(R.string.compass_raw_angle, String.format("%.1f", mAngle)));
    }

    @Override
    public void onClick(View v) {
        int visibility = mRawX.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        mRawX.setVisibility(visibility);
        mRawY.setVisibility(visibility);
        mRawZ.setVisibility(visibility);
        mRawAngle.setVisibility(visibility);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getBoolean("s", false)) {
            mX = savedInstanceState.getFloat("x", 0);
            mY = savedInstanceState.getFloat("y", 0);
            mZ = savedInstanceState.getFloat("z", 0);
            mAngle = savedInstanceState.getFloat("a", 0);
            setValue(null, mAngle, mX, mY, mZ);
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
        outState.putFloat("a", mAngle);
    }

    public void reset() {
        mRawX.setText("");
        mRawY.setText("");
        mRawZ.setText("");
        mRawAngle.setText("");
        mX = 0;
        mY = 0;
        mZ = 0;
        mAngle = 0;
    }

    @Override
    public void showFirstAnimatedValues() {
        mNeedle.setRotation(mAngle);
    }

    @Override
    public boolean hasAnimatedValuesChanged() {
        return mPreviousAngle != mAngle;
    }

    @Override
    public void saveAnimatedValues() {
        mPreviousAngle = mAngle;
    }

    @Override
    public void prepareAnimatedValues(List<PropertyValuesHolder> values) {
        PropertyValuesHolder v = null;

        float angleChange = mAngle - mPreviousAngle;
        if (angleChange > -180 && angleChange < 180) {
            v = PropertyValuesHolder.ofFloat("compass.angle", mPreviousAngle, mAngle);
        } else {
            // we jumped more than +-180 degrees==>crossing 0 mark
            if (mAngle < mPreviousAngle) {
                // Example: 350->10 CW
                v = PropertyValuesHolder.ofFloat("compass.angle", mPreviousAngle, mAngle + 360);
            } else {
                // Example 10 >350 CCW
                v = PropertyValuesHolder.ofFloat("compass.angle", mPreviousAngle + 360, mAngle);
            }
        }
        values.add(v);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator a) {
        Object angle = a.getAnimatedValue("compass.angle");
        if (angle != null) {
            mNeedle.setRotation((Float) angle);
        }
    }


    public void setEnabled(boolean enabled) {
        if (enabled) {
            mNeedle.setVisibility(View.VISIBLE);
            mClickablePanel.setOnClickListener(this);
        } else {
            mNeedle.setVisibility(View.INVISIBLE);
            mRawX.setVisibility(View.INVISIBLE);
            mRawY.setVisibility(View.INVISIBLE);
            mRawZ.setVisibility(View.INVISIBLE);
            mRawAngle.setVisibility(View.INVISIBLE);
            mClickablePanel.setOnClickListener(null);
        }
    }
}
