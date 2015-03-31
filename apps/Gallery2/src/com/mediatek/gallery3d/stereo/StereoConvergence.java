/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mediatek.gallery3d.stereo;

import android.graphics.Bitmap;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.media.effect.EffectUpdateListener;
import android.util.Log;

import com.android.gallery3d.common.Utils;
import com.mediatek.gallery3d.data.DecodeHelper;
import com.mediatek.gallery3d.stereo.StereoEffectHandle.StereoEffect;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

import com.mediatek.effect.effects.MTKEffectFactory;
import com.mediatek.effect.effects.Stereo3DAntiFatigueEffect;
import com.mediatek.effect.effects.Stereo3DAntiFatigueEffect.AntiFatigueInfo;

public class StereoConvergence implements StereoEffect {

    private static final String TAG = "StereoConvergence";
    private static int num = 0;
    public static final int[] CONVERGENCE_INDEX = { 0, 10, 20, 30, 40, 50, 60, 70, 80 };
    private static final int SUB_INDEX_NUM = 10;
    public static final int TOTAL_INDEX_NUM = (9 - 1) * SUB_INDEX_NUM + 1;

    private int mDefaultPosition = -1;

    private float mWidthRate[];
    private float mLeftOffsetXRate[];
    private float mRightOffsetXRate[];

    private float mHeightRate[];
    private float mLeftOffsetYRate;
    private float mRightOffsetYRate;

    // the following two variable should not be used by outside
    private int mInputWidth;
    private int mInputHeight;

    private Bitmap mBitmapLeft;
    private Bitmap mBitmapRight;
    private boolean mIsMtk3d;
    private Bitmap mRightBitmapWarp;
    private AntiFatigueInfo mAntiFatigueInfo;
    private boolean mEffectDone;
    private int mActiveFlags[];

    public int[] getActiveFlags() {
        return mActiveFlags;
    }

    public int getDefaultPosition() {
        if (mDefaultPosition < 0) {
            return -1;
        }
        return mDefaultPosition * SUB_INDEX_NUM;
    }

    public float getWidthRate(int position) {
        if (null == mWidthRate) {
            return 0.0f;
        }
        if (position < 0 || position > mWidthRate.length) {
            return 0.0f;
        }
        return mWidthRate[position];
    }

    public float getLeftOffsetXRate(int position) {
        if (null == mLeftOffsetXRate) {
            return 0.0f;
        }
        if (position < 0 || position > mLeftOffsetXRate.length) {
            return 0.0f;
        }
        return mLeftOffsetXRate[position];
    }

    public float getRightOffsetXRate(int position) {
        if (null == mRightOffsetXRate) {
            return 0.0f;
        }
        if (position < 0 || position > mRightOffsetXRate.length) {
            return 0.0f;
        }
        return mRightOffsetXRate[position];
    }

    public float getHeightRate(int position) {
        if (null == mHeightRate) {
            return 0.0f;
        }
        if (position < 0 || position > mHeightRate.length) {
            return 0.0f;
        }
        return mHeightRate[position];
    }

    public float getLeftOffsetYRate() {
        return mLeftOffsetYRate;
    }

    public float getRightOffsetYRate() {
        return mRightOffsetYRate;
    }

    public Bitmap getRightBitmapAfterWarp() {
        return mRightBitmapWarp;
    }

    public StereoConvergence(Bitmap left, Bitmap right, boolean isMtk3d) {
        Utils.assertTrue(left != null && right != null);
        if (left.getWidth() == right.getWidth() && left.getHeight() == right.getHeight()) {
            mBitmapLeft = left.copy(left.getConfig(), false);
            mBitmapRight = right.copy(right.getConfig(), false);
        } else if (left.getWidth() * left.getHeight() > right.getWidth() * right.getHeight()) {
            Log.w(TAG, "<StereoConvergence> left: [" + left.getWidth() + "x" + left.getHeight() + "]");
            Log.w(TAG, "<StereoConvergence> right:[" + right.getWidth() + "x" + right.getHeight() + "]");
            // we scale down left image
            mBitmapLeft = DecodeHelper.resizeBitmap(right.getWidth(), right.getHeight(), left);
            mBitmapRight = right.copy(right.getConfig(), false);
        } else {
            Log.w(TAG, "<StereoConvergence> left: [" + left.getWidth() + "x" + left.getHeight() + "]");
            Log.w(TAG, "<StereoConvergence> right:[" + right.getWidth() + "x" + right.getHeight() + "]");
            mBitmapLeft = left.copy(left.getConfig(), false);
            // we scale down right image
            mBitmapRight = DecodeHelper.resizeBitmap(left.getWidth(), left.getHeight(), right);
        }
        mIsMtk3d = isMtk3d;
        mInputWidth = mBitmapLeft.getWidth();
        mInputHeight = mBitmapLeft.getHeight();
    }

    @Override
    public boolean isEffectDone() {
        return mEffectDone;
    }

    @Override
    public void run(EffectContext effectContext) {
        Log.i(TAG, "<run> l.width = " + mBitmapLeft.getWidth() + ", l.height = "
                + mBitmapLeft.getHeight() + ", r.width = " + mBitmapRight.getWidth()
                + ", r.height = " + mBitmapRight.getHeight());

        EffectFactory effectFactory = effectContext.getFactory();
        Stereo3DAntiFatigueEffect effect = (Stereo3DAntiFatigueEffect) effectFactory
                .createEffect(MTKEffectFactory.EFFECT_ANTIFATIGUE);
        effect.setParameter(MTKEffectFactory.EFFECT_ANTIFATIGUE_RIGHT_BITMAP, mBitmapRight);
        effect.setParameter(MTKEffectFactory.EFFECT_ANTIFATIGUE_LEFT_BITMAP, mBitmapLeft);
        effect.setParameter(MTKEffectFactory.EFFECT_ANTIFATIGUE_OPERATION,
                MTKEffectFactory.EFFECT_ANTIFATIGUE_OPERATION_PLAYBACK);
        effect.setParameter(MTKEffectFactory.EFFECT_ANTIFATIGUE_LAYOUT,
                MTKEffectFactory.EFFECT_ANTIFATIGUE_LAYOUT_HORIZONTAL);
        if (mIsMtk3d) {
            effect.setParameter(MTKEffectFactory.EFFECT_ANTIFATIGUE_MTK3DTAG,
                    MTKEffectFactory.EFFECT_ANTIFATIGUE_MTK3DTAG_Y);
        } else {
            effect.setParameter(MTKEffectFactory.EFFECT_ANTIFATIGUE_MTK3DTAG,
                    MTKEffectFactory.EFFECT_ANTIFATIGUE_MTK3DTAG_N);
        }
        if (MtkLog.DBG_AC) {
            num++;
            MtkUtils.dumpBitmap(mBitmapLeft, "left-" + num + ".png");
            MtkUtils.dumpBitmap(mBitmapRight, "right-" + num + ".png");
        }
        effect.setUpdateListener(new EffectUpdateListener() {
            public void onEffectUpdated(Effect effect, Object info) {
                mAntiFatigueInfo = (AntiFatigueInfo) info;
                Log.i(TAG, "<onEffectUpdated>");
                if (MtkLog.DBG_AC) {
                    MtkUtils.dumpBitmap(mAntiFatigueInfo.mBitmap, "warp-" + num + ".png");
                }
                retrieveInfo();
                mBitmapLeft.recycle();
                mBitmapRight.recycle();
                mBitmapLeft = null;
                mBitmapRight = null;
                mEffectDone = true;
                synchronized (StereoConvergence.this) {
                    StereoConvergence.this.notifyAll();
                }
            }
        });
        mEffectDone = false;
        effect.apply(0, mInputWidth, mInputHeight, 0);
    }

    private void retrieveInfo() {
        if (mAntiFatigueInfo == null) {
            Log.e(TAG, "<retrieveInfo> why got invalid parameters");
            return;
        }
        mDefaultPosition = mAntiFatigueInfo.mCroppingIntervalDefault;
        mActiveFlags = new int[CONVERGENCE_INDEX.length];
        for (int i = 0; i < mActiveFlags.length; i++) {
            mActiveFlags[i] = 0;
        }
        mActiveFlags[mDefaultPosition] = 1;
        mLeftOffsetXRate = new float[TOTAL_INDEX_NUM];
        mRightOffsetXRate = new float[TOTAL_INDEX_NUM];
        mWidthRate = new float[TOTAL_INDEX_NUM];
        mHeightRate = new float[TOTAL_INDEX_NUM];

        // inteplate original offsets-Left
        int[] intervals = mAntiFatigueInfo.mCroppingOffectX_L;
        for (int i = 0; i < intervals.length; i++) {
            mLeftOffsetXRate[i * SUB_INDEX_NUM] = (float) intervals[i];
        }
        for (int i = 0; i < intervals.length - 1; i++) {
            LinearDifference(mLeftOffsetXRate, i * SUB_INDEX_NUM, (i + 1) * SUB_INDEX_NUM);
        }

        // inteplate original offsets-Right
        intervals = mAntiFatigueInfo.mCroppingOffectX_R;
        for (int i = 0; i < intervals.length; i++) {
            mRightOffsetXRate[i * SUB_INDEX_NUM] = (float) intervals[i];
        }
        for (int i = 0; i < intervals.length - 1; i++) {
            LinearDifference(mRightOffsetXRate, i * SUB_INDEX_NUM, (i + 1) * SUB_INDEX_NUM);
        }
        
        // inteplate original cropsize width
        intervals = mAntiFatigueInfo.mCroppingSizeWidth;
        for (int i = 0; i < intervals.length; i++) {
            mWidthRate[i * SUB_INDEX_NUM] = (float) intervals[i];
        }
        for (int i = 0; i < intervals.length - 1; i++) {
            LinearDifference(mWidthRate, i * SUB_INDEX_NUM, (i + 1) * SUB_INDEX_NUM);
        }
        
        // inteplate original cropsize height
        intervals = mAntiFatigueInfo.mCroppingSizeHeight;
        for (int i = 0; i < intervals.length; i++) {
            mHeightRate[i * SUB_INDEX_NUM] = (float) intervals[i];
        }
        for (int i = 0; i < intervals.length - 1; i++) {
            LinearDifference(mHeightRate, i * SUB_INDEX_NUM, (i + 1) * SUB_INDEX_NUM);
        }
        
        for (int i = 0; i < TOTAL_INDEX_NUM; i++) {
            mLeftOffsetXRate[i] = mLeftOffsetXRate[i] / (float) mInputWidth;
            mRightOffsetXRate[i] = mRightOffsetXRate[i] / (float) mInputWidth;
            mHeightRate[i] = mHeightRate[i] / (float) mInputHeight;
            mWidthRate[i] = mWidthRate[i] / (float) mInputWidth;
        }

        float lOffsetY = (float) mAntiFatigueInfo.mCroppingOffectY_L;
        float rOffsetY = (float) mAntiFatigueInfo.mCroppingOffectY_R;

        mLeftOffsetYRate = lOffsetY / (float) mInputHeight;
        mRightOffsetYRate = rOffsetY / (float) mInputHeight;

        mRightBitmapWarp = mAntiFatigueInfo.mBitmap;
        mAntiFatigueInfo = null;
    }

    private static void LinearDifference(float[] array, int startIndex, int endIndex) {
        if (null == array || startIndex < 0 || endIndex < startIndex) {
            Log.e(TAG, "LinearDifferece:invalid input params");
            return;
        }
        float startValue = array[startIndex];
        float endValue = array[endIndex];
        for (int i = startIndex + 1; i < endIndex; i++) {
            array[i] = (endValue - startValue) * (i - startIndex) / (endIndex - startIndex)
                    + array[startIndex];
        }
        return;
    }
}
