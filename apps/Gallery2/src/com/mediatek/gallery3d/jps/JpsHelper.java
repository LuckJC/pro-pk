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
package com.mediatek.gallery3d.jps;

import android.graphics.Rect;
import android.util.Log;

import com.mediatek.gallery3d.stereo.StereoHelper;

public class JpsHelper {
	
    private static final String TAG = "JpsHelper";

    public static final String FILE_EXTENSION = "jps";

    public static final String MIME_TYPE = "image/x-jps";

    public static void adjustRect(int localLayout,boolean firstFrame, 
                                  Rect imageRect) {
        boolean isLeftRight =
            (StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT == localLayout) ||
            (StereoHelper.STEREO_LAYOUT_SWAP_LEFT_AND_RIGHT == localLayout);
        //change index for swapped type
        if (StereoHelper.STEREO_LAYOUT_SWAP_LEFT_AND_RIGHT == localLayout ||
            StereoHelper.STEREO_LAYOUT_SWAP_TOP_AND_BOTTOM == localLayout) {
            firstFrame = !firstFrame;
        }
        StereoHelper.adjustRect(isLeftRight, firstFrame, imageRect);
    }

}
