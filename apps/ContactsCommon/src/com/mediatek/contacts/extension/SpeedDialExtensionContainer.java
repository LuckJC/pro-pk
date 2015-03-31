/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.extension;

import android.util.Log;
import android.view.View;

import com.mediatek.contacts.ext.SpeedDialExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class SpeedDialExtensionContainer extends SpeedDialExtension {

    private static final String TAG = "SpeedDialExtensionContainer";

    private LinkedList<SpeedDialExtension> mSubExtensionList;

    public void add(SpeedDialExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<SpeedDialExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(SpeedDialExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public void setView(View view, int viewId, boolean mPrefNumContactState, int sdNumber,
            String commd) {
        Log.i(TAG, "[setView()]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setView(view, viewId, mPrefNumContactState, sdNumber, commd);
        }
    }

    public int setAddPosition(int mAddPosition, boolean mNeedRemovePosition, String commd) {
        Log.i(TAG, "[setAddPosition()]");
        if (null == mSubExtensionList) {
            return mAddPosition;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                int result = iterator.next().setAddPosition(mAddPosition, mNeedRemovePosition,
                        commd);
                if (result != mAddPosition) {
                    return result;
                }
            }
        }
        return mAddPosition;
    }

    public boolean needClearPreState(String commd) {
        Log.i(TAG, "[needClearPreState()]");
        if (null == mSubExtensionList) {
            return true;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().needClearPreState(commd);
                if (!result) {
                    return result;
                }
            }
        }
        return true;
    }

    public boolean showSpeedInputDialog(String commd) {
        Log.i(TAG, "[showSpeedInputDialog()]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().showSpeedInputDialog(commd);
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }

    public boolean needClearSharedPreferences(String commd) {
        Log.i(TAG, "needClearSharedPreferences()");
        if (null == mSubExtensionList) {
            Log.i(TAG, "[needClearSharedPreferences()");
            return true;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().needClearSharedPreferences(commd);
                if (!result) {
                    Log.i(TAG, "needClearSharedPreferences()]");
                    return result;
                }
            }
        }
        Log.i(TAG, "[needClearSharedPreferences()]");
        return true;
    }

    public boolean clearPrefStateIfNecessary(String commd) {
        Log.i(TAG, "SpeedDialManageActivity: [clearPrefStateIfNecessary]");
        if (null == mSubExtensionList) {
            return true;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().clearPrefStateIfNecessary(commd);
                if (!result) {
                    return result;
                }
            }
        }
        return true;
    }

    public boolean needCheckContacts(String commd) {
        Log.i(TAG, "SpeedDialManageActivity: [needCheckContacts]");
        if (null == mSubExtensionList) {
            return true;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().needCheckContacts(commd);
                if (!result) {
                    return result;
                }
            }
        }
        return true;
    }    
}
