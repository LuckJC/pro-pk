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

import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.View;

import com.mediatek.contacts.ext.CallLogAdapterExtension;
import com.mediatek.dialer.calllogex.ContactInfoEx;

import java.util.Iterator;
import java.util.LinkedList;

public class CallLogAdapterExtensionContainer extends CallLogAdapterExtension {

    private static final String TAG = "CallLogAdapterExtensionContainer";

    private LinkedList<CallLogAdapterExtension> mSubExtensionList;

    public void add(CallLogAdapterExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<CallLogAdapterExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(CallLogAdapterExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public boolean setListItemViewTag(View itemView, ContactInfoEx contactInfo,
                                      Cursor c, Intent callDetailIntent) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "setListItemViewTag(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "setListItemViewTag(), item view = " + itemView + ", contactInfo = "
                + contactInfo + ", cursor = " + c + ", callDetailIntent = " + callDetailIntent);
        Iterator<CallLogAdapterExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallLogAdapterExtension extension = iterator.next();
            if (extension.setListItemViewTag(itemView, contactInfo, c, callDetailIntent)) {
                return true;
            }
        }
        return false;
    }

    public void bindViewPre(View view, Cursor c, int count, ContactInfoEx contactInfo) {
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<CallLogAdapterExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().bindViewPre(view, c, count, contactInfo);
        }
    }
}
