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

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.ext.IccCardExtension;

import android.net.Uri;
import android.os.Bundle;

import com.mediatek.contacts.util.LogUtils;

public class SimContactPhotoUtils {
    private static final String TAG = "SimContactPhotoUtils";

    public interface SimPhotoIdAndUri {
        int DEFAULT_SIM_PHOTO_ID = -1;

        int SIM_PHOTO_ID_BLUE_SDN = -5;
        int SIM_PHOTO_ID_ORANGE_SDN = -6;
        int SIM_PHOTO_ID_GREEN_SDN = -7;
        int SIM_PHOTO_ID_PURPLE_SDN = -8;

        int DEFAULT_SIM_PHOTO_ID_SDN = -9;

        int SIM_PHOTO_ID_BLUE = -10;
        int SIM_PHOTO_ID_ORANGE = -11;
        int SIM_PHOTO_ID_GREEN = -12;
        int SIM_PHOTO_ID_PURPLE = -13;

        String DEFAULT_SIM_PHOTO_URI = "content://sim";

        String SIM_PHOTO_URI_BLUE_SDN = "content://sdn-5";
        String SIM_PHOTO_URI_ORANGE_SDN = "content://sdn-6";
        String SIM_PHOTO_URI_GREEN_SDN = "content://sdn-7";
        String SIM_PHOTO_URI_PURPLE_SDN = "content://sdn-8";

        String DEFAULT_SIM_PHOTO_URI_SDN = "content://sdn";

        String SIM_PHOTO_URI_BLUE = "content://sim-10";
        String SIM_PHOTO_URI_ORANGE = "content://sim-11";
        String SIM_PHOTO_URI_GREEN = "content://sim-12";
        String SIM_PHOTO_URI_PURPLE = "content://sim-13";
    }
    
    public interface SimPhotoColors {
        int BLUE = 0;
        int ORANGE = 1;
        int GREEN = 2;
        int PURPLE = 3;
    }

    public static long getPhotoIdByPhotoUri(Uri uri) {
        long id = 0;

        if (uri == null) {
            LogUtils.e(TAG, "[getPhotoIdByPhotoUri] uri is null,return 0.");
            return id;
        }

        String photoUri = uri.toString();

        if (SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI.equals(photoUri)) {
            id = SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID;
        } else if (SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID_SDN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE_SDN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE_SDN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN_SDN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE_SDN;
        }

        LogUtils.d(TAG, "[getPhotoIdByPhotoUri]photoUri:" + photoUri +
                ",id:" + id);
        
        return id;
    }

    public static boolean isSimPhotoUri(Uri uri) {
        if (null == uri) {
            LogUtils.e(TAG, "[isSimPhotoUri] uri is null");
            return false;
        }

        String photoUri = uri.toString();
        LogUtils.d(TAG, "[isSimPhotoUri] uri : " + photoUri);

        if (SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI.equals(photoUri)
                || SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI_SDN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE_SDN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE_SDN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN_SDN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE_SDN.equals(photoUri)

                ) {
            return true;
        }
        
        return false;
    }

    public static boolean isSimPhotoId(long photoId) {
        LogUtils.d(TAG, "[isSimPhotoId] photoId : " + photoId);
        if (photoId == SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID
                || photoId == SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID_SDN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE_SDN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN_SDN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE_SDN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE_SDN) {
            return true;
        }

        return false;
    }

    public String getPhotoUri(int isSdnContact, int colorId) {
        String photoUri = null;
        boolean isSdn = (isSdnContact > 0);
        /**
         * Plug-in call @{
         */
        Bundle argsForExt = new Bundle();
        argsForExt.putBoolean(IccCardExtension.KEY_IS_ICC_CONTACT_SDN, isSdn);
        argsForExt.putInt(IccCardExtension.KEY_ICC_COLOR_ID, colorId);
        photoUri = mIccExt.getIccPhotoUriString(argsForExt, ContactPluginDefault.COMMD_FOR_OP09);
        if (photoUri != null) {
            LogUtils.i(TAG, "[getPhotoUri] from ext: " + photoUri);
            return photoUri;
        }
        /*** Plug-in call @}*/
        LogUtils.d(TAG, "[getPhotoUri] i = " + colorId + " | isSdnContact : " + isSdnContact
                + ",photoUri:" + photoUri);
        switch (colorId) {
            case SimContactPhotoUtils.SimPhotoColors.BLUE:
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.ORANGE:
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.GREEN:
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.PURPLE:
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE;
                }
                break;
            default:
                LogUtils.i(TAG, "[getPhotoUri]no match color");
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI;
                }
                break;
        }

        return photoUri;
    }

    public long getPhotoId(int isSdnContact, int colorId) {
        long photoId = 0;
        boolean isSdn = (isSdnContact > 0);
        /**
         * Plug-in call @{
         */
        Bundle argsForExt = new Bundle();
        argsForExt.putBoolean(IccCardExtension.KEY_IS_ICC_CONTACT_SDN, isSdn);
        argsForExt.putInt(IccCardExtension.KEY_ICC_COLOR_ID, colorId);
        photoId = mIccExt.getIccPhotoId(argsForExt, ContactPluginDefault.COMMD_FOR_OP09);
        if (photoId != 0) {
            LogUtils.i(TAG, "[getPhotoId] from ext: " + photoId);
            return photoId;
        }
        /**Plug-in call @}*/
        LogUtils.d(TAG, "[getPhotoId] i = " + colorId + " | isSdnContact : " + isSdnContact
                + ",photoId:" + photoId);
        switch (colorId) {
            case SimContactPhotoUtils.SimPhotoColors.BLUE:
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.ORANGE:
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.GREEN:
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.PURPLE:
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE;
                }
                break;
            default:
                LogUtils.i(TAG, "[getPhotoId]no match color.");
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID;
                }
                break;
        }
        
        return photoId;
    }

    /**
     * the extension for IccCard related photo
     */
    private IccCardExtension mIccExt = ExtensionManager.getInstance().getIccCardExtension();
}
