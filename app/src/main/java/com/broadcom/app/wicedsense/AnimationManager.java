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

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.animation.LinearInterpolator;

/**
 * Manages and coordinates multiple "Animated" objects
 *
 */
public class AnimationManager {

    public interface Animated extends AnimatorUpdateListener {
        public void showFirstAnimatedValues();

        public boolean hasAnimatedValuesChanged();

        public void saveAnimatedValues();

        public void prepareAnimatedValues(List<PropertyValuesHolder> values);
    }

    private final int mFrameDelayMs;
    private final int mAnimateIntervalMs;
    private final ValueAnimator mAnimator;
    private long mLastTime;
    private long mCurrentTime;
    private boolean mIsReady;
    private final ArrayList<PropertyValuesHolder> mPropertyValues;

    public AnimationManager(int frameDelayMs, int animateIntervalMs) {
        mAnimator = new ValueAnimator();
        mAnimator.setInterpolator(new LinearInterpolator());
        mFrameDelayMs = frameDelayMs;
        mAnimateIntervalMs = animateIntervalMs;
        ValueAnimator.setFrameDelay(mFrameDelayMs);

        mLastTime = 0;
        mCurrentTime = 0;
        mPropertyValues = new ArrayList<PropertyValuesHolder>();
    }

    public boolean useAnimation() {
        return Settings.animate();
    }

    public void addAnimated(Animated a) {
        ArrayList<Animator.AnimatorListener> l = mAnimator.getListeners();
        if (l == null || !l.contains(a)) {
            mAnimator.addUpdateListener(a);
        }
    }

    public void removeAnimated(Animated a) {
        mAnimator.removeUpdateListener(a);
    }

    public void init() {
        mCurrentTime = System.currentTimeMillis();

        long duration = mCurrentTime - mLastTime;
        if (duration < mAnimateIntervalMs) {
            mIsReady = false;
            return;
        }

        if (mAnimator.isRunning()) {
            // mAnimator.end();
            mIsReady = false;
            return;
        }

        mIsReady = true;
    }

    public void prepareAnimated(Animated a) {
        if (!mIsReady) {
            return;
        }

        // Check if this is the first values: don't animate first value
        if (mLastTime == 0) {
            a.showFirstAnimatedValues();
            a.saveAnimatedValues();
            mLastTime = mCurrentTime;
            return;
        }

        if (a.hasAnimatedValuesChanged()) {
            a.prepareAnimatedValues(mPropertyValues);
            a.saveAnimatedValues();
        } else {

        }
    }

    public void animate() {
        if (mPropertyValues.size() > 0) {
            PropertyValuesHolder[] values = new PropertyValuesHolder[mPropertyValues.size()];
            mPropertyValues.toArray(values);
            mAnimator.setValues(values);
            mAnimator.setDuration(mAnimateIntervalMs);
            mAnimator.start();
            mLastTime = mCurrentTime;
            mPropertyValues.clear();
            mIsReady = false;
        }
    }

}
