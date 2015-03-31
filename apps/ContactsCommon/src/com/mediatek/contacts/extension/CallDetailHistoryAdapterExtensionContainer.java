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

//import ICallDetailActivity;
//import IPhoneNumberHelper;
//import android.app.Activity;
//import android.net.Uri;
//import android.util.Log;
//import android.view.ContextMenu;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.ContextMenu.ContextMenuInfo;
//import android.widget.ListView;
//import android.widget.TextView;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.mediatek.contacts.ext.CallDetailHistoryAdapterExtension;
import com.mediatek.dialer.PhoneCallDetailsEx;
//import com.android.contacts.ext.ICallDetailActivity;
//import com.android.contacts.ext.IPhoneNumberHelper;

import java.util.Iterator;
import java.util.LinkedList;

public class CallDetailHistoryAdapterExtensionContainer extends CallDetailHistoryAdapterExtension {

    private static final String TAG = "CallDetailExtensionContainer";

    private LinkedList<CallDetailHistoryAdapterExtension> mSubExtensionList;

    public void add(CallDetailHistoryAdapterExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<CallDetailHistoryAdapterExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(CallDetailHistoryAdapterExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public void init(Context context, PhoneCallDetailsEx[] phoneCallDetails) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "init(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "init(), context = " + context + ", phoneCallDetails = " + phoneCallDetails);
        Iterator<CallDetailHistoryAdapterExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().init(context, phoneCallDetails);
        }
    }

    /*public int getCount(int currentCount) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "getCount(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "getCount(), currentCount = " + currentCount);
        Iterator<CallDetailHistoryAdapterExtension> iterator = mSubExtensionList.iterator();
        int resultCount = currentCount;
        while (iterator.hasNext()) {
            CallDetailHistoryAdapterExtension extension = iterator.next();
            resultCount = extension.getCount(resultCount);
        }
        return resultCount;
    }*/

    /*public boolean getItem(int position, Object object) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "getItem(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "getItem(), position = " + position + ", object = " + object);
        Iterator<CallDetailHistoryAdapterExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallDetailHistoryAdapterExtension extension = iterator.next();
            if (extension.getItem(position, object)) {
                return true;
            }
        }
        return false;
    }*/

    /*public boolean getItemId(int position, Long itemId) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "getItemId(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "getItemId(), position = " + position + ", itemId = " + itemId);
        Iterator<CallDetailHistoryAdapterExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallDetailHistoryAdapterExtension extension = iterator.next();
            if (extension.getItemId(position, itemId)) {
                return true;
            }
        }
        return false;
    }*/

    public int getViewTypeCount(int currentViewTypeCount) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "getViewTypeCount(), sub extension list is null, just return");
            return currentViewTypeCount;
        }
        Log.i(TAG, "getViewTypeCount(), currentViewTypeCount = " + currentViewTypeCount);
        Iterator<CallDetailHistoryAdapterExtension> iterator = mSubExtensionList.iterator();
        int resultViewTypeCount = currentViewTypeCount;
        while (iterator.hasNext()) {
            CallDetailHistoryAdapterExtension extension = iterator.next();
            resultViewTypeCount = extension.getViewTypeCount(resultViewTypeCount);
        }
        return resultViewTypeCount;
    }

    public int getItemViewType(int position) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "getItemViewType(), sub extension list is null, just return");
            return -1;
        }
        Log.i(TAG, "getItemViewType(), position = " + position);
        Iterator<CallDetailHistoryAdapterExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallDetailHistoryAdapterExtension extension = iterator.next();
            int result = extension.getItemViewType(position);
            if (-1 != result) {
                return result;
            }
        }
        return -1;
    }

    public View getViewPre(int position, View convertView, ViewGroup parent) {
        // This function do not record log for performance
        if (null == mSubExtensionList) {
            return null;
        }
        Iterator<CallDetailHistoryAdapterExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallDetailHistoryAdapterExtension extension = iterator.next();
            View resultView = extension.getViewPre(position, convertView, parent);
            if (null != resultView) {
                return resultView;
            }
        }
        return null;
    }

    public View getViewPost(int position, View convertView, ViewGroup parent) {
        // This function do not record log for performance
        if (null == mSubExtensionList) {
            return convertView;
        }
        Iterator<CallDetailHistoryAdapterExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallDetailHistoryAdapterExtension extension = iterator.next();
            convertView = extension.getViewPost(position, convertView, parent);
        }
        return convertView;
    }
}
