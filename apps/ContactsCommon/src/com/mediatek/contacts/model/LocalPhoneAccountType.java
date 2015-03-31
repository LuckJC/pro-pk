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
package com.mediatek.contacts.model;

import android.content.Context;
import android.net.sip.SipManager;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;

import com.android.contacts.common.R;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountType.EditField;
import com.android.contacts.common.model.account.BaseAccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.google.android.collect.Lists;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.ext.ContactAccountExtension;
import com.mediatek.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.util.LogUtils;

import java.util.Locale;

public class LocalPhoneAccountType extends BaseAccountType {
    private static final String TAG = "LocalPhoneAccountType";
    
    private ContactAccountExtension mCAccountEx = null;
    
    public static final String ACCOUNT_TYPE = AccountType.ACCOUNT_TYPE_LOCAL_PHONE;

    public LocalPhoneAccountType(Context context, String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.resourcePackageName = null;
        this.syncAdapterPackageName = resPackageName;
        this.titleRes = R.string.account_phone_only;
        this.iconRes = R.drawable.mtk_contact_account_phone;
        
        try {
            addDataKindStructuredName(context);//overwrite
            addDataKindDisplayName(context);//overwrite
            addDataKindIm(context);//overwrite

            addDataKindPhoneticName(context);
            addDataKindNickname(context);
            addDataKindPhone(context);
            addDataKindEmail(context);
            addDataKindStructuredPostal(context);//overwrite
            addDataKindOrganization(context);
            addDataKindPhoto(context);
            addDataKindNote(context);
            addDataKindWebsite(context);
            addDataKindGroupMembership(context);
            
            if (SipManager.isVoipSupported(context)) {
                LogUtils.i(TAG, "[LocalPhoneAccountType]SipManager.isVoipSupported is true.");
                addDataKindSipAddress(context);
            }
        } catch (DefinitionException e) {
            LogUtils.e(TAG, "[LocalPhoneAccountType]DefinitionException:", e);
        }
    }

    @Override
    protected DataKind addDataKindStructuredPostal(Context context) throws DefinitionException {
        final DataKind kindForLoacalPhone = super.addDataKindStructuredPostal(context);
        final boolean useJapaneseOrder = Locale.JAPANESE.getLanguage().equals(
                Locale.getDefault().getLanguage());
        LogUtils.d(TAG, "[addDataKindStructuredPostal]useJapaneseOrder:" + useJapaneseOrder);
        
        kindForLoacalPhone.typeColumn = StructuredPostal.TYPE;
        kindForLoacalPhone.typeList = Lists.newArrayList();
        kindForLoacalPhone.typeList.add(buildPostalType(StructuredPostal.TYPE_WORK).setSpecificMax(1));
        kindForLoacalPhone.typeList.add(buildPostalType(StructuredPostal.TYPE_HOME).setSpecificMax(1));
        kindForLoacalPhone.typeList.add(buildPostalType(StructuredPostal.TYPE_OTHER).setSpecificMax(1));

        kindForLoacalPhone.fieldList = Lists.newArrayList();
        if (useJapaneseOrder) {
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.COUNTRY, R.string.postal_country,
                    FLAGS_POSTAL).setOptional(true));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.POSTCODE, R.string.postal_postcode,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.REGION, R.string.postal_region,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.CITY, R.string.postal_city,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.NEIGHBORHOOD, R.string.postal_neighborhood,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.POBOX, R.string.postal_pobox,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.STREET, R.string.postal_street,
                    FLAGS_POSTAL));
        } else {
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.STREET, R.string.postal_street,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.POBOX, R.string.postal_pobox,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.NEIGHBORHOOD, R.string.postal_neighborhood,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.CITY, R.string.postal_city,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.REGION, R.string.postal_region,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.POSTCODE, R.string.postal_postcode,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.COUNTRY, R.string.postal_country,
                    FLAGS_POSTAL).setOptional(true));
        }

        return kindForLoacalPhone;
    }
    
    @Override
    protected DataKind addDataKindStructuredName(Context context) throws DefinitionException {
        DataKind kind = super.addDataKindStructuredName(context);
        boolean result = ExtensionManager.getInstance().getContactAccountExtension()
                .needNewDataKind(ContactPluginDefault.COMMD_FOR_OP01);
        if (result) {
            String str = null;
            int displayName = 0;
            int phoneticMiddelName = 0;
            int j = 0;
            boolean a = false;
            boolean b = false;
            int count = kind.fieldList.size();
            for (int i = 0; i < count; i++) {
                str = kind.fieldList.get(i).column.toString();
                if (str != null && str.equals("data1")) {
                    displayName = i;
                } else if (str != null && str.equals("data8")) {
                    phoneticMiddelName = i;
                } else if (str != null && str.equals("data4")) {
                    j = i;
                    a = kind.fieldList.get(i).longForm;
                    kind.fieldList.get(i).setLongForm(false);
                    kind.fieldList.get(i).setOptional(true);
                    
                } else {
                    kind.fieldList.get(i).setLongForm(false);
                }
            }
            LogUtils.i(TAG, "[addDataKindStructuredName]display_name : " + displayName
                    + " | phonetic_middel_name : " + phoneticMiddelName
                    + " |a : " + a);
            
            if (displayName != phoneticMiddelName) {
                kind.fieldList.remove(displayName);
                kind.fieldList.remove(phoneticMiddelName);
            } 
        }
        
        return kind;
    }

    @Override
    protected DataKind addDataKindDisplayName(Context context) throws DefinitionException {
        DataKind kind = super.addDataKindDisplayName(context);
        boolean result = ExtensionManager.getInstance().getContactAccountExtension()
                .needNewDataKind(ContactPluginDefault.COMMD_FOR_OP01);
        if (result) {
            String str = null;
            int displayName = 0;
            int count = kind.fieldList.size();
            for (int i = 0; i < count; i++) {
                str = kind.fieldList.get(i).column.toString();
                if (str != null && str.equals("data1")) {
                    displayName = i;
                } else if (str != null && str.equals("data5")) {
                    kind.fieldList.get(i).setOptional(true);
                    kind.fieldList.get(i).setLongForm(false);
                } else if (str != null && str.equals("data4")) {
                    kind.fieldList.get(i).setOptional(true);
                    kind.fieldList.get(i).setLongForm(false);
                } else if (str != null && str.equals("data6")) {
                    kind.fieldList.get(i).setOptional(true);
                    kind.fieldList.get(i).setLongForm(false);
                } else {
                    kind.fieldList.get(i).setLongForm(false);
                }
            }
            LogUtils.i(TAG, " [addDataKindDisplayName]display_name : " + displayName);
            kind.fieldList.remove(displayName);
        }
        
        return kind;
    }

    @Override
    protected DataKind addDataKindIm(Context context) throws DefinitionException {
        DataKind kind = super.addDataKindIm(context);
        boolean result = ExtensionManager.getInstance().getContactAccountExtension()
                .needNewDataKind(ContactPluginDefault.COMMD_FOR_OP01);
        if (result) {
            kind.typeList.remove(buildImType(Im.PROTOCOL_GOOGLE_TALK));
        }
        
        return kind;
    }
    
    @Override
    public boolean isGroupMembershipEditable() {
        return true;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }
}
