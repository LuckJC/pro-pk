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

import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.contacts.ext.ContactListExtension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;


public class ContactListExtensionContainer extends ContactListExtension {

    private static final String TAG = "ContactListExtensionContainer";

    private LinkedList<ContactListExtension> mSubExtensionList;

    public void add(ContactListExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<ContactListExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(ContactListExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public void setMenuItem(MenuItem blockVoiceCallmenu, boolean mOptionsMenuOptions, String commd) {
        Log.i(TAG, "[setMenuItem] mOptionsMenuOptions : " + mOptionsMenuOptions);
        if (null != mSubExtensionList) {
            Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                ContactListExtension extension = iterator.next();
                if (extension.getCommand().equals(commd)) {
                    extension.setMenuItem(blockVoiceCallmenu, mOptionsMenuOptions, commd);
                    return ;
                }
            }
        }
        super.setMenuItem(blockVoiceCallmenu, mOptionsMenuOptions, commd);
    }

//    public boolean[] setAllRejectedCall() {
//        Log.i(TAG, "[setAllRejectedCall] ");
//        if (null == mSubExtensionList) {
//            return null;
//        } else {
//            Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
//            while (iterator.hasNext()) {
//                return iterator.next().setAllRejectedCall();
//            }
//        }
//        return null;
//    }

    public void setLookSimStorageMenuVisible(MenuItem lookSimStorageMenu, boolean flag, String commd) {
        Log.i(TAG, "[setLookSimStorageMenuVisible] flag : " + flag);
        if (null != mSubExtensionList) {
            Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                ContactListExtension extension = iterator.next();
                if (extension.getCommand().equals(commd)) {
                    extension.setLookSimStorageMenuVisible(lookSimStorageMenu, flag, commd);
                    return;
                }
            }
        }
        super.setLookSimStorageMenuVisible(lookSimStorageMenu, flag, commd);
    }

    public String getReplaceString(final String src, String commd) {
        Log.i(TAG, "[getReplaceString] src : " + src);
        if (null == mSubExtensionList) {
            return src.replace('p', PhoneNumberUtils.PAUSE).replace('w', PhoneNumberUtils.WAIT);
        } else {
            Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                ContactListExtension extension = iterator.next();
                if (extension.getCommand().equals(commd)) {
                    final String result = extension.getReplaceString(src, commd);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return src.replace('p', PhoneNumberUtils.PAUSE).replace('w', PhoneNumberUtils.WAIT);
    }

    public void setExtentionImageView(ImageView view, String commd) {
        Log.i(TAG, "[setExtentionIcon]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setExtentionImageView(view, commd);
        }
    }

    public void setExtentionTextView(TextView view, long contactId, String commd) {
        Log.i(TAG, "[setExtentionIcon]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setExtentionTextView(view, contactId, commd);
        }
    }

    /** M:AAS & SNE @ { */
    public void checkPhoneTypeArray(String accountType, ArrayList<Integer> phoneTypeArray,
            String commd) {
        Log.i(TAG, "[checkPhoneTypeArray]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().checkPhoneTypeArray(accountType, phoneTypeArray, commd);
        }
    }

    public boolean generateDataBuilder(Context context, Cursor dataCursor, Builder builder,
            String[] columnNames, String accountType, String mimeType, int slotId, int index,
            String commd) {
        Log.i(TAG, "[generateDataBuilder()]");
        if (null != mSubExtensionList) {
            for (ContactListExtension subExtension : mSubExtensionList) {
                if (subExtension.generateDataBuilder(context, dataCursor, builder, columnNames,
                        accountType, mimeType, slotId, index, commd)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String buildSimNickname(String accountType, ContentValues values,
            ArrayList<String> nicknameArray, int slotId, String defSimNickname, String cmd) {
        if (null != mSubExtensionList) {
            for (ContactListExtension subExtension : mSubExtensionList) {
                String nickName = subExtension.buildSimNickname(accountType, values, nicknameArray,
                        slotId, defSimNickname, cmd);
                if (!TextUtils.equals(nickName, defSimNickname)) {
                    return nickName;
                }
            }
        }
        return defSimNickname;
    }
    /** M: @ } */

    ///M: for SNS plugin @{
    @Override
    public Drawable getPresenceIcon(Cursor cursor, int statusResPackageColumn,
            int statusIconColumn, String commd) {
        Log.i(TAG, "[getPresenceIcon]");
        if (null == mSubExtensionList) {
            return null;
        }

        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            ContactListExtension cle = iterator.next();
            if (commd.equals(cle.getCommand())) {
                return cle.getPresenceIcon(cursor, statusResPackageColumn,
                        statusIconColumn, commd);
            }
        }

        return null;
    }

    @Override
    public String getStatusString(Cursor cursor, int statusResPackageColumn,
            int contactsStatusColumn, String commd) {
        Log.i(TAG, "[getStatusString]");
        if (null == mSubExtensionList) {
            return null;
        }

        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            ContactListExtension cle = iterator.next();
            if (commd.equals(cle.getCommand())) {
                return cle.getStatusString(cursor, statusResPackageColumn,
                        contactsStatusColumn, commd);
            }
        }

        return null;
    }
    ///@}

    @Override
    public void addOptionsMenu(Menu menu, Bundle args, String commd) {
        if (null != mSubExtensionList) {
            for (ContactListExtension subExtension : mSubExtensionList) {
                if (commd.equals(subExtension.getCommand())) {
                    subExtension.addOptionsMenu(menu, args, commd);
                    return;
                }
            }
        }
    }

    @Override
    public void registerHostContext(Context context, Bundle args, String commd) {
        if (null != mSubExtensionList) {
            for (ContactListExtension subExtension : mSubExtensionList) {
                if (commd.equals(subExtension.getCommand())) {
                    subExtension.registerHostContext(context, args, commd);
                    return;
                }
            }
        }
    }

    @Override
    public int getMultiChoiceLimitCount(String commd) {
        Log.i(TAG, "[getMultiChoiceLimitCount] commd: " + commd);
        if (null != mSubExtensionList) {
            for (ContactListExtension subExtension : mSubExtensionList) {
                if (commd.equals(subExtension.getCommand())) {
                    int result = subExtension.getMultiChoiceLimitCount(commd);
                    return result;
                }
            }
        }
        return super.getMultiChoiceLimitCount(commd);
    }
}
