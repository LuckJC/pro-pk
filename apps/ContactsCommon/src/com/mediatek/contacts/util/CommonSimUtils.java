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
package com.mediatek.contacts.util;

import java.util.ArrayList;

import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountType.EditType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.mediatek.contacts.ext.Anr;
import com.mediatek.contacts.ext.ContactAccountExtension;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.extension.ContactAccountExtensionContainer;

import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class CommonSimUtils {
    private static final String TAG = "CommonSimUtils";

    public static boolean ensureAASKindExists(RawContactDelta state, AccountType accountType, String mimeType, DataKind kind) {
        ContactAccountExtension cae = ExtensionManager.getInstance().getContactAccountExtension();

        if (kind != null && cae.isFeatureAccount(accountType.accountType, ExtensionManager.COMMD_FOR_AAS)
                && cae.isPhone(mimeType, ExtensionManager.COMMD_FOR_AAS)) {
            ArrayList<ValuesDelta> values = state.getMimeEntries(mimeType);
            final int slotId = ExtensionManager.getInstance().getContactAccountExtension().getCurrentSlot(ExtensionManager.COMMD_FOR_AAS);
            final int slotAnrSize = ExtensionManager.getInstance().getContactDetailExtension().getAdditionNumberCount(
                    slotId, ExtensionManager.COMMD_FOR_AAS);
            if (values != null && values.size() == slotAnrSize + 1) {
                // primary number + slotNumber size
                LogUtils.d(TAG, "ensureAASKindExists() size=" + values.size() + " slotAnrSize=" + slotAnrSize);
                return true;
            }
            if (values == null || values.isEmpty()) {
                LogUtils.d(TAG, "ensureAASKindExists() Empty, insert primary:1 and anr:" + slotAnrSize);
                // Create child when none exists and valid kind
                final ValuesDelta child = RawContactModifier.insertChild(state, kind);
                if (kind.mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    child.setFromTemplate(true);
                }

                for (int i = 0; i < slotAnrSize; i++) {
                    final ValuesDelta slotChild = RawContactModifier.insertChild(state, kind);
                    slotChild.put(Data.IS_ADDITIONAL_NUMBER, 1);
                }
            } else {
                int pnrSize = 0;
                int anrSize = 0;
                if (values != null) {
                    for (ValuesDelta value : values) {
                        Integer isAnr = value.getAsInteger(Data.IS_ADDITIONAL_NUMBER);
                        if (isAnr != null && (isAnr.intValue() == 1)) {
                            anrSize++;
                        } else {
                            pnrSize++;
                        }
                    }
                }
                LogUtils.d(TAG, "ensureAASKindExists() pnrSize=" + pnrSize + ", anrSize=" + slotAnrSize);
                if (pnrSize < 1) {
                    // insert a empty primary number if not exists.
                    RawContactModifier.insertChild(state, kind);
                }
                for (; anrSize < slotAnrSize; anrSize++) {
                    // insert additional numbers if not full.
                    final ValuesDelta slotChild = RawContactModifier.insertChild(state, kind);
                    slotChild.put(Data.IS_ADDITIONAL_NUMBER, 1);
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isAasPhoneType(int type) {
        return Anr.TYPE_AAS == type;
    }

    public static EditType getAasEditType(ValuesDelta entry, DataKind kind, int phoneType) {
        if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureEnabled(
                ExtensionManager.COMMD_FOR_AAS)
                && phoneType == Anr.TYPE_AAS) {
            String customColumn = entry.getAsString(Data.DATA3);
            LogUtils.d(TAG, "getAasEditType() customColumn=" + customColumn);
            if (customColumn != null) {
                for (EditType type : kind.typeList) {
                    if (type.rawValue == Anr.TYPE_AAS && customColumn.equals(type.customColumn)) {
                        LogUtils.d(TAG, "getAasEditType() type");
                        return type;
                    }
                }
            }
            return null;
        }
        LogUtils.e(TAG, "getAasEditType() error Not Anr.TYPE_AAS, type=" + phoneType);
        return null;
    }

    //Force the phone number to be shown in LTR format
    public static String getPhoneNum(String num) {
        if (num != null) {
            num = "\u202a" + num + "\u202c"; 
        }
        return num;
    }
}
