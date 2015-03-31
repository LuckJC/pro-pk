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
package com.mediatek.contacts;

import android.bluetooth.BluetoothAdapter;

public class ContactsFeatureConstants {

    public interface FeatureOption {
        boolean MTK_SEARCH_DB_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_SEARCH_DB_SUPPORT;
        boolean MTK_DIALER_SEARCH_SUPPORT =
                com.mediatek.common.featureoption.FeatureOption.MTK_DIALER_SEARCH_SUPPORT;
        boolean MTK_GEMINI_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_GEMINI_SUPPORT;
        boolean MTK_VT3G324M_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_VT3G324M_SUPPORT;
        boolean MTK_GEMINI_3G_SWITCH = com.mediatek.common.featureoption.FeatureOption.MTK_GEMINI_3G_SWITCH;
        boolean MTK_PHONE_NUMBER_GEODESCRIPTION =
                com.mediatek.common.featureoption.FeatureOption.MTK_PHONE_NUMBER_GEODESCRIPTION;
        boolean MTK_THEMEMANAGER_APP = com.mediatek.common.featureoption.FeatureOption.MTK_THEMEMANAGER_APP;
        boolean MTK_DRM_APP = com.mediatek.common.featureoption.FeatureOption.MTK_DRM_APP;
        boolean EVDO_DT_SUPPORT = com.mediatek.common.featureoption.FeatureOption.EVDO_DT_SUPPORT;
        boolean MTK_BEAM_PLUS_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_BEAM_PLUS_SUPPORT;
        boolean MTK_VVM_SUPPORT = true; //[VVM] vvm is a Google default feature.
        
        boolean MTK_ONLY_OWNER_SIM_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_ONLY_OWNER_SIM_SUPPORT;
        boolean MTK_SINGLE_IMEI = com.mediatek.common.featureoption.FeatureOption.MTK_SINGLE_IMEI;
    }

    public static boolean DBG_DIALER_SEARCH = true;
    public static boolean DBG_CONTACTS_GROUP = true;

    public static boolean isSupportBtProfileBpp() {
        return (BluetoothAdapter.getDefaultAdapter() != null)
                && android.bluetooth.ConfigHelper.checkSupportedProfiles(android.bluetooth.ProfileConfig.PROFILE_ID_BPP);
    }
}
