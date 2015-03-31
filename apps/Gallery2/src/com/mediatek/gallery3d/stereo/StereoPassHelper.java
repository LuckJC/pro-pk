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

import android.graphics.Rect;

import android.util.Log;

import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLRoot;

import com.mediatek.gallery3d.stereo.StereoHelper;

import javax.microedition.khronos.opengles.GL11;

public class StereoPassHelper {

    private static final String TAG = "StereoPassHelper";

    private static final int STEREO_DRAW_FIRST_PASS = 0;
    private static final int STEREO_DRAW_SECOND_PASS = 1;

    public static class StereoPass {
        public StereoPass() {
             this(0.0f, 0.0f, 1.0f, 1.0f,
                  1.0f, 1.0f, 0.0f, 0.0f);
        }

        public StereoPass(float rX, float rY, float rW, float rH,
                          float sX, float sY, float tX, float tY) {
            rateX = rX;
            rateY = rY;
            rateW = rW;
            rateH = rH;

            scaleX = sX;
            scaleY = sY;
            transfX = tX;
            transfY = tY;
        }
        // the following variable defines visible window size
        public float rateX;
        public float rateY;
        public float rateW;
        public float rateH;
        // the following variable defines OpenGL matrix
        public float scaleX;
        public float scaleY;
        public float transfX;
        public float transfY;
    }

    // for side-by-side (swap) stereo layout
    private static StereoPass [] mStereoPassSbs;
    // for top-and-bottom (swap) stereo layout
    private static StereoPass [] mStereoPassTab;

    static {
        mStereoPassSbs = new StereoPass[3];
        mStereoPassSbs[StereoHelper.STEREO_DISPLAY_NORMAL_PASS] =
            new StereoPass();
        mStereoPassSbs[StereoHelper.STEREO_DISPLAY_FIRST_PASS] =
            new StereoPass(0.0f, 0.0f, 0.5f, 1.0f, 0.5f, 1.0f, 0.0f, 0.0f);
        mStereoPassSbs[StereoHelper.STEREO_DISPLAY_SECOND_PASS] =
            new StereoPass(0.5f, 0.0f, 0.5f, 1.0f, 0.5f, 1.0f, 0.5f, 0.0f);

        mStereoPassTab = new StereoPass[3];
        mStereoPassTab[StereoHelper.STEREO_DISPLAY_NORMAL_PASS] =
            new StereoPass();
        mStereoPassTab[StereoHelper.STEREO_DISPLAY_FIRST_PASS] =
            new StereoPass(0.0f, 0.0f, 1.0f, 0.5f, 1.0f, 0.5f, 0.0f, 0.0f);
        mStereoPassTab[StereoHelper.STEREO_DISPLAY_SECOND_PASS] =
            new StereoPass(0.0f, 0.5f, 1.0f, 0.5f, 1.0f, 0.5f, 0.0f, 0.5f);
    }

    public static StereoPass [] getStereoPassForLayout(int stereoLayout) {
        switch (stereoLayout) {
        case StereoHelper.STEREO_TYPE_2D:
            return null;
        case StereoHelper.STEREO_TYPE_SIDE_BY_SIDE:
        case StereoHelper.STEREO_TYPE_SWAP_LEFT_RIGHT:
            return mStereoPassSbs;
        case StereoHelper.STEREO_TYPE_TOP_BOTTOM:
        case StereoHelper.STEREO_TYPE_SWAP_TOP_BOTTOM:
            return mStereoPassTab;
        default:
            return null;
        }
    }

    public static StereoPass getStereoPass(int passId, int stereoLayout) {
        StereoPass [] stereoPasses = getStereoPassForLayout(stereoLayout);
        if (null == stereoPasses) {
            return null;
        }
        return stereoPasses[passId];
    }

    public static void
        prepareForStereoPass(GLCanvas canvas, int passId, int stereoLayout,
                             int viewWidth, int viewHeight) {
        if (null == canvas || passId < 0 || stereoLayout < 0 ||
            passId > StereoHelper.STEREO_DISPLAY_SECOND_PASS ||
            stereoLayout > StereoHelper.STEREO_TYPE_SWAP_TOP_BOTTOM) {
            Log.w(TAG, "prepareForStereoPass:got invalid params");
            return;
        }

        StereoPass stereoPass = getStereoPass(passId, stereoLayout);
        if (null == stereoPass) {
            //Log.w(TAG, "prepareForStereoPass:why we got null stereo pass");
            return;
        }

        //set new viewport and projection matrix
        canvas.setSize(
            (int)(viewWidth * stereoPass.rateX), 
            (int)(viewHeight * stereoPass.rateY),
            (int)(viewWidth * stereoPass.rateW),
            (int)(viewHeight * stereoPass.rateH),
            viewWidth,
            viewHeight);

        //translate to target position
        canvas.translate(
                (float)viewWidth * stereoPass.transfX,
                (float)viewHeight * stereoPass.transfY,
                0.0f);

        //scale to target scale
        canvas.scale(
                stereoPass.scaleX,
                stereoPass.scaleY,
                1.0f);
    }

    public static void setScissorBox(GLRoot root, GLCanvas canvas, Rect displayedImage,
            int viewWidth, int viewHeight) {
        if (null == root || null == canvas || null == displayedImage ||
            viewWidth <= 0 || viewHeight <= 0) {
            Log.w(TAG, "setScissorBox:got invalid params");
            return;
        }
        int stereoLayout = root.getStereoLayout();
        int passId = root.getStereoPassId();
        StereoPass stereoPass = getStereoPass(passId, stereoLayout);
        if (null == stereoPass) {
            Log.w(TAG, "setScissorBox:why we got null stereo pass");
            return;
        }

        canvas.setScissorBox(
            (int)(viewWidth * stereoPass.rateX +
                  displayedImage.left * stereoPass.scaleX),
            viewHeight - (int)(viewHeight * stereoPass.rateY +
                               displayedImage.bottom * stereoPass.scaleY),
            (int)((displayedImage.right - displayedImage.left) *
                  stereoPass.scaleX),
            (int)((displayedImage.bottom - displayedImage.top) *
                  stereoPass.scaleY));
    }
}
