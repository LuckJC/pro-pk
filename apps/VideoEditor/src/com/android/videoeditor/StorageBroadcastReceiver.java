/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
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

package com.android.videoeditor;

import com.android.videoeditor.util.MtkLog;

import android.os.storage.StorageVolume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// The receiver is to get sdcard MOUNT and UNMOUNT broadcast to 
// notify listeners.
public class StorageBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "StorageBroadcastReceiver";
    // private static final ArrayList<SdcardBroadcastReceiverListener> mListeners = new ArrayList<SdcardBroadcastReceiverListener>();

    /**
     * Broase listener
     */
    /*
    public interface SdcardBroadcastReceiverListener {
        // When sdcard is mounted call this
        public void onMounted();
        // When sdcard is unmounted call this
        public void onUnMounted();
    }
    */
    
    protected void onMounted(String storagePath) {
        // Sub class override this function
    }
    
    protected void onUnMounted(String storagePath) {
        // Sub class override this function
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        MtkLog.d(TAG, "onReceive() action:" + action);
        
        String storagePath = null;
        if (intent.hasExtra(StorageVolume.EXTRA_STORAGE_VOLUME)) {
            final StorageVolume storage = (StorageVolume)intent.getParcelableExtra(
                    StorageVolume.EXTRA_STORAGE_VOLUME);
            storagePath = storage.getPath();
            MtkLog.d(TAG, "onReceive() storage:" + storage.getPath());
        }
        
        if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            MtkLog.d(TAG, "onReceive() mounted");
            onMounted(storagePath);
        } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {//|| Intent.ACTION_MEDIA_EJECT.equals(action)
            MtkLog.d(TAG, "onReceive() unmounted");
            onUnMounted(storagePath);
        }
    }
    
    /*
    private void notifyUnmounted() {
        for (SdcardBroadcastReceiverListener listener : mListeners) {
            listener.onUnMounted();
        }
    }
    
    private void notifyMounted() {
        for (SdcardBroadcastReceiverListener listener : mListeners) {
            listener.onMounted();
        }
    }
    */

    /**
     * Register a listener
     *
     * @param listener The listener
     */
    /*
    public static void registerListener(SdcardBroadcastReceiverListener listener) {
        MtkLog.d(TAG, "registerListener() " + listener);
        mListeners.add(listener);
    }
    */
    /**
     * Unregister a listener
     *
     * @param listener The listener
     */
    /*
    public static void unregisterListener(SdcardBroadcastReceiverListener listener) {
        MtkLog.d(TAG, "unregisterListener() " + listener);
        mListeners.remove(listener);
    }
    */
}
