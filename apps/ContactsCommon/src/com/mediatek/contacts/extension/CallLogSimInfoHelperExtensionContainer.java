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
import android.graphics.drawable.Drawable;

import com.mediatek.contacts.ext.CallLogSimInfoHelperExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class CallLogSimInfoHelperExtensionContainer extends CallLogSimInfoHelperExtension {

    private static final String TAG = "CallLogSimInfoHelperExtensionContainer";

    private LinkedList<CallLogSimInfoHelperExtension> mSubExtensionList;

    public void add(CallLogSimInfoHelperExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<CallLogSimInfoHelperExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(CallLogSimInfoHelperExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    /**
     * get sim name by sim id
     * 
     * @param simId from datebase
     * @param callDisplayName return plugin modify result
     * @return boolean if plugin processed
     */
    public boolean getSimDisplayNameById(int simId, StringBuffer callDisplayName) {
        if (null == mSubExtensionList) {
            return false;
        }
        Log.i(TAG, "getSimDisplayNameById(), simId = " + simId);
        Iterator<CallLogSimInfoHelperExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallLogSimInfoHelperExtension extension = iterator.next();
            if (extension.getSimDisplayNameById(simId, callDisplayName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * get sim color drawable by sim id
     * 
     * @param simId form datebases
     * @param simColorDrawable return plugin modify result
     * @return boolean if plugin processed
     */
    public boolean getSimColorDrawableById(int simId, Drawable simColorDrawable) {
        Log.i(TAG, "getSimColorDrawableById(), simId = " + simId);
        if (null == mSubExtensionList) {
            return false;
        }
        Iterator<CallLogSimInfoHelperExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallLogSimInfoHelperExtension extension = iterator.next();
            if (extension.getSimColorDrawableById(simId, simColorDrawable)) {
                return true;
            }
        }
        return false;
    }

    /**
     * get sim background dark resource by color id
     * 
     * @param colorId form datebases
     * @param simBackgroundDarkRes return plugin modify result
     * @return boolean if plugin processed
     */
    public boolean getSimBackgroundDarkResByColorId(int colorId, int[] simBackgroundDarkRes) {
        Log.i(TAG, "getSimBackgroundDarkResByColorId(), mSubExtensionList = " + mSubExtensionList);
        if (null == mSubExtensionList) {
            return false;
        }
        Iterator<CallLogSimInfoHelperExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallLogSimInfoHelperExtension extension = iterator.next();
            if (extension.getSimBackgroundDarkResByColorId(colorId, simBackgroundDarkRes)) {
                return true;
            }
        }
        return false;
    }

}
