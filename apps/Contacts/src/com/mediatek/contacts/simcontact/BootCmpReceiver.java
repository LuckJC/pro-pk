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

package com.mediatek.contacts.simcontact;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
/** M: Bug Fix for CR ALPS01328816 @{ */
import android.content.pm.PackageManager;
import android.os.UserHandle;
/** @} */
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
/** M: Bug Fix for CR ALPS01328816 @{ */
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
/** @} */
import com.mediatek.contacts.simservice.SIMProcessorService;
import com.mediatek.contacts.simservice.SIMServiceUtils;
import com.mediatek.contacts.util.LogUtils;

public class BootCmpReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCmpReceiver";
    private static final int DEFAULT_DUAL_SIM_MODE = (1 << SlotUtils.getSlotCount()) - 1;
    private static Context sContext = null;
    private static HashMap<Integer, Boolean> mImportRecord = new HashMap<Integer, Boolean>();
    

    ///M: [Gemini+]TODO: should change to SIMRecords.xxx
    private static final String ACTION_SIM_FILE_CHANGED = "android.intent.action.sim.SIM_FILES_CHANGED";
    private static final String KEY_SLOT_ID = "SIM_ID";

    public void onReceive(Context context, Intent intent) {
        sContext = context;
        LogUtils.i(TAG, "In onReceive ");
        final String action = intent.getAction();
        LogUtils.i(TAG, "action is " + action);

        if (action.equals(TelephonyIntents.ACTION_PHB_STATE_CHANGED)) {
            processPhoneBookChanged(intent);
        }else if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
            // fix ALPS01003520,when boot complete,remove the contacts if the
            // card of a slot has been removed
            processBootComplete();
        }
        /// M:change for PHB Status Refatoring, remove other unused status.

        /** M: Bug Fix for CR ALPS01328816: when other owner, do not show sms when share contact @{ */
        if (action.equals("android.intent.action.USER_SWITCHED_FOR_MULTIUSER_APP")
                && FeatureOption.MTK_ONLY_OWNER_SIM_SUPPORT) {
            if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
                sContext.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.contacts", "com.mediatek.contacts.ShareContactViaSMSActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
            } else {
                sContext.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.contacts", "com.mediatek.contacts.ShareContactViaSMSActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }
        /** @} */
    }

    public void startSimService(int slotId, int workType) {
        Intent intent = null;
        /* M: change for SIM Service refactoring @{*/
        intent = new Intent(sContext, SIMProcessorService.class);
        /* @}*/
        intent.putExtra(SIMServiceUtils.SERVICE_SLOT_KEY, slotId);
        intent.putExtra(SIMServiceUtils.SERVICE_WORK_TYPE, workType);
        LogUtils.i(TAG, "[startSimService]slotId:" + slotId + "|workType:" + workType);
        sContext.startService(intent);
    }

    private void processPhoneBookChanged(Intent intent) {
        LogUtils.i(TAG, "processPhoneBookChanged");
        boolean phbReady = intent.getBooleanExtra("ready", false);
        int slotId = intent.getIntExtra("simId", -10);
        LogUtils.i(TAG, "[processPhoneBookChanged]phbReady:" + phbReady + "|slotId:" + slotId);
        //if the PHB state has been changed,reset the phone book info
        //Only update the info when first used to avoid ANR in onReceiver when Boot Complete
        SlotUtils.resetPhbInfoBySlot(slotId);
        if (phbReady && slotId >= 0) {
            mImportRecord.put(slotId, true);
            startSimService(slotId, SIMServiceUtils.SERVICE_WORK_IMPORT);
            /*SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getSimWrapperInstanceUnCheck();
            if (simInfoWrapper != null) {
                simInfoWrapper.updateSimInfoCache();
            }*/
        } else if(slotId >= 0 && !phbReady) {
            startSimService(slotId, SIMServiceUtils.SERVICE_WORK_REMOVE);
        }
    }

    /**
     * fix for [PHB Status Refatoring] ALPS01003520
     * when boot complete,remove the contacts if the card of a slot had been removed
     */
    private void processBootComplete() {
        LogUtils.i(TAG, "[processBootComplete],slot count:" + SlotUtils.getSlotCount());
        for (int slotId : SlotUtils.getAllSlotIds()) {
            if (mImportRecord.get(slotId) != null && mImportRecord.get(slotId) == true) {
                LogUtils.i(TAG, "[processBootComplete],slot :" + slotId + " has impoered,ignore..");
                continue;
            }
            LogUtils.i(TAG, "[processBootComplete],process slot:" + slotId);
            // reset the phone book when boot complete
            SlotUtils.resetPhbInfoBySlot(slotId);
            startSimService(slotId, SIMServiceUtils.SERVICE_WORK_REMOVE);
        }
    }
}
