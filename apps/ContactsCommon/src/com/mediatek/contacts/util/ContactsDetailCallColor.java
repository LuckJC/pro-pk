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

import android.content.ContentResolver;

import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import com.mediatek.telephony.TelephonyManagerEx;



/** TODO:
 * It is an util class for CT feature only
 * because the CT requirements diaplay two call button in contact detail page, and the 
 * call icon can change with the default current SIM card.
 * so we need to know the current default sim card.
 * so this class provider some method to get default SIMINFO.
 * 
 * the GEMENI+ feature will not use this class.
 * @author mtk81249
 *
 */
public class ContactsDetailCallColor {
    private static final String TAG = "ContactsDetailCallColor";

    private static ContactsDetailCallColor sInstance = new ContactsDetailCallColor();

    private ContactsDetailCallColor() {

    }

    public static ContactsDetailCallColor getInstance() {
        if (sInstance == null) {
            return new ContactsDetailCallColor();
        }
        
        return sInstance;
    }

    public int getDefaultSlot(ContentResolver contentResolver) {
        SimInfoRecord mSimInfoOfDefaultSim = getDefaultSiminfo(contentResolver);
        
        return mSimInfoOfDefaultSim.mSimSlotId;
    }

    public int getNotDefaultSlot(ContentResolver contentResolver) {
        SimInfoRecord mSimInfoOfDefaultSim = getDefaultSiminfo(contentResolver);
        
        return mSimInfoOfDefaultSim.mSimSlotId == PhoneConstants.GEMINI_SIM_1 ? PhoneConstants.GEMINI_SIM_2
                : PhoneConstants.GEMINI_SIM_1;
    }

    public SimInfoRecord getDefaultSiminfo(ContentResolver contentResolver) {
        final long mDefaultSim = Settings.System.getLong(contentResolver,
                Settings.System.VOICE_CALL_SIM_SETTING,Settings.System.DEFAULT_SIM_NOT_SET);
        SimInfoRecord simInfoOfDefaultSim = SIMInfoWrapper.getDefault().getSimInfoById((int)mDefaultSim);
        
        return simInfoOfDefaultSim;
    }

    public SimInfoRecord getNotDefaultSiminfo(ContentResolver contentResolver) {
        SimInfoRecord defaultInfo = getDefaultSiminfo(contentResolver);
        if (defaultInfo.mSimSlotId == PhoneConstants.GEMINI_SIM_1) {
            return SIMInfoWrapper.getDefault().getSimInfoBySlot(PhoneConstants.GEMINI_SIM_2);
        } else {
            return SIMInfoWrapper.getDefault().getSimInfoBySlot(PhoneConstants.GEMINI_SIM_1);
        }
    }

    public boolean isCDMAPhoneTypeBySlot(final int slot) {
        TelephonyManagerEx telephony = TelephonyManagerEx.getDefault();
        return telephony.getPhoneType(slot) == PhoneConstants.PHONE_TYPE_CDMA;
    }

}
