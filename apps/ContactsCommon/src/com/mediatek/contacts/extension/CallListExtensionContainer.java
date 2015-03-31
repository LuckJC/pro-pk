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

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.mediatek.contacts.ext.CallListExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class CallListExtensionContainer extends CallListExtension {

    private static final String TAG = "CallListExtensionContainer";

    private LinkedList<CallListExtension> mSubExtensionList;

    public void add(CallListExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<CallListExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(CallListExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public int layoutExtentionIcon(int leftBound, int topBound, int bottomBound, int rightBound,
            int mGapBetweenImageAndText, ImageView mExtentionIcon, String commd) {
        Log.i(TAG, "[layoutExtentionIcon]");
        if (null == mSubExtensionList) {
            return rightBound;
        } else {
            Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                int result = iterator.next().layoutExtentionIcon(leftBound, topBound, bottomBound,
                        rightBound, mGapBetweenImageAndText, mExtentionIcon, commd);
                if (result != rightBound) {
                    return result;
                }
            }
        }
        return rightBound;
    }

    public void measureExtention(ImageView mExtentionIcon, String commd) {
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().measureExtention(mExtentionIcon, commd);
        }
    }

    public boolean setExtentionIcon(String number, String commd) {
        Log.i(TAG, "[setExtentionIcon]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().setExtentionIcon(number, commd);
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }


    public boolean checkPluginSupport(String commd) {
        Log.i(TAG, "[checkPluginSupport]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().checkPluginSupport(commd);
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }

    public void onCreate(ListFragment fragment) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onCreate(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onCreate(), fragment is " + fragment);
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onCreate(fragment);
        }
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onViewCreated(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onViewCreated(), view = " + view + ", savedInstanceState = " + savedInstanceState);
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onViewCreated(view, savedInstanceState);
        }
    }

    public void onDestroy() {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onDestroy(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onDestroy()");
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onDestroy();
        }
    }

    public boolean onListItemClick(ListView l, View v, int position, long id) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onListItemClick(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "onListItemClick(), list view = " + l + 
                ", view = " + v + ", position = " + position + ", id = " + id);
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallListExtension extension = iterator.next();
            if (extension.onListItemClick(l, v, position, id)) {
                return true;
            }
        }
        return false;
    }

    /*public boolean onItemLongClick(ListView l, View v, int position, long id) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onListItemLongClick(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "onListItemLongClick(), list view = " + l + 
                ", view = " + v + ", position = " + position + ", id = " + id);;
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallListExtension extension = iterator.next();
            if (extension.onItemLongClick(l, v, position, id)) {
                return true;
            }
        }
        return false;
    }*/

    public boolean onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onListItemLongClick(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "onCreateContextMenu(), menu = " + menu + ", view = " + view + ", menuInfo = " + menuInfo);
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallListExtension extension = iterator.next();
            if (extension.onCreateContextMenu(menu, view, menuInfo)) {
                return true;
            }
        }
        return false;
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onContextItemSelected(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "onContextItemSelected(), item = " + item);
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallListExtension extension = iterator.next();
            if (extension.onContextItemSelected(item)) {
                return true;
            }
        }
        return false;
    }
}
