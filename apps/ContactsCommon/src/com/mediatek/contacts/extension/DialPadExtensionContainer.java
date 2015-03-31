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

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.mediatek.contacts.ext.DialPadExtension;
import com.mediatek.contacts.ext.IDialpadFragment;

import java.util.Iterator;
import java.util.LinkedList;

public class DialPadExtensionContainer extends DialPadExtension {

    private static final String TAG = "DialPadExtensionContainer";

    private LinkedList<DialPadExtension> mSubExtensionList;

    public void add(DialPadExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<DialPadExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(DialPadExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public String changeChar(String string, String string2, String commd) {
        Log.i(TAG, "[changeChar] string : " + string + " | string2 : " + string2);
        if (null == mSubExtensionList) {
            return string2;
        } else {
            Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                final String result = iterator.next().changeChar(string, string2, commd);
                if (result != null) {
                    return result;
                }
            }
        }
        return string2;
    }

    public boolean handleChars(Context context, String input, String commd) {
        Log.i(TAG, "[handleChars] input : " + input);
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                final boolean result = iterator.next().handleChars(context, input, commd);
                if (result) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onCreate(Fragment fragment, IDialpadFragment dialpadFragment) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onCreate(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onCreate(), fragment is " + fragment + ", dialpadFragment = " + dialpadFragment);
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onCreate(fragment, dialpadFragment);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState, View view) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onCreateView(), sub extension list is null, just return");
            return view;
        }
        Log.i(TAG, "onCreateView(), inflater = " + inflater + ", container = " + container +
                "savedState = " + savedState + ", view = " + view);
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        View resultView = view;
        while (iterator.hasNext()) {
            resultView = iterator.next().onCreateView(inflater, container, savedState, resultView);
        }
        return resultView;
    }

    public void onDestroy() {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onDestroy(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onDestroy()");
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onDestroy();
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onCreateOptionsMenu(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onCreateOptionsMenu(), menu is " + menu + ", inflater = " + inflater);
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onCreateOptionsMenu(menu, inflater);
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onPrepareOptionsMenu(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "onPrepareOptionsMenu(), menu is " + menu);
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onPrepareOptionsMenu(menu);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onOptionsItemSelected(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "onOptionsItemSelected(), item = " + item);
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            DialPadExtension extension = iterator.next();
            if (extension.onOptionsItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    public void constructPopupMenu(PopupMenu popupMenu, View anchorView, Menu menu) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "constructPopupMenu(), sub extension list is null, just return");
            return;
        }
        Log.i(TAG, "constructPopupMenu(), popupMenu is " + popupMenu +
                ", archorView = " + anchorView + "menu = " + menu);
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().constructPopupMenu(popupMenu, anchorView, menu);
        }
    }

    public boolean onMenuItemClick(MenuItem item) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "onMenuItemClick(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "onMenuItemClick(), item = " + item);
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            DialPadExtension extension = iterator.next();
            if (extension.onMenuItemClick(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean updateDialAndDeleteButtonEnabledState(final String lastNumberDialed) {
        if (null == mSubExtensionList) {
            Log.i(TAG, "updateDialAndDeleteButtonEnabledState(), sub extension list is null, just return");
            return false;
        }
        Log.i(TAG, "updateDialAndDeleteButtonEnabledState(), lastNumberDialed = " + lastNumberDialed);
        Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            DialPadExtension extension = iterator.next();
            if (extension.updateDialAndDeleteButtonEnabledState(lastNumberDialed)) {
                return true;
            }
        }
        return false;
    }
}
