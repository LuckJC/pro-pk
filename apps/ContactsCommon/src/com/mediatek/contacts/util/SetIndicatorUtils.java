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

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;

//import com.android.contacts.quickcontact.QuickContactActivity;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.util.LogUtils;

public class SetIndicatorUtils {
    private static final String TAG = "SetIndicatorUtils";
    
    private static final String PEOPLEACTIVITY = "com.android.contacts.activities.PeopleActivtiy";
    private static final String QUICKCONTACTACTIVITY = "com.android.contacts.quickcontact.QuickContactActivity";
    private static final String INDICATE_TYPE = "CONTACTS";
    
    private static SetIndicatorUtils sInstance = null;
    private StatusBarManager mStatusBarMgr = null;
    private boolean mShowSimIndicator = false;
    private BroadcastReceiver mReceiver = new MyBroadcastReceiver();

    // In PeopleActivity, if quickContact is show, quickContactIsShow = true,
    // PeopleActivity.onPause cannot hide the Indicator.
    private boolean mQuickContactIsShow = false;

    public static SetIndicatorUtils getInstance() {
        if (sInstance == null) {
            sInstance = new SetIndicatorUtils();
        }
        return sInstance;
    }

    public void showIndicator(boolean visible, Activity activity) {
        LogUtils.i(TAG, "[showIndicator]visible : " + visible);
        mShowSimIndicator = visible;
        if (visible) {
            ExtensionManager.getInstance().getContactAccountExtension().switchSimGuide(activity,
                    INDICATE_TYPE, ContactPluginDefault.COMMD_FOR_AppGuideExt);
        }
        setSimIndicatorVisibility(visible, activity);
    }
    
    public void registerReceiver(Activity activity) {
        LogUtils.i(TAG, "[registerReceiver] activity : " + activity);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_VOICE_CALL_DEFAULT_SIM_CHANGED);
        activity.registerReceiver(mReceiver, intentFilter);
    }

    public void unregisterReceiver(Activity activity) {
        LogUtils.i(TAG, "u[nregisterReceiver] activity : " + activity);
        if (null != mReceiver) {
            activity.unregisterReceiver(mReceiver);
        }
    }
    
    private SetIndicatorUtils() {
        if (mStatusBarMgr == null) {
            mStatusBarMgr = (StatusBarManager) GlobalEnv.getApplicationContext().getSystemService(
                    Context.STATUS_BAR_SERVICE);
        }
    }
    
    private void setSimIndicatorVisibility(boolean visible, Activity activity) {
        ComponentName componentName = null;
        String className = null;
        
        if (null != activity) {
            componentName = activity.getComponentName();
            if (null != componentName) {
                className = componentName.getClassName();
            }
        }
        
        LogUtils.i(TAG, "[setSimIndicatorVisibility]activity is :" + activity +
                ",visible:" + visible + ",className:" + className);
        
        if (visible) {
            mStatusBarMgr.showSimIndicator(componentName, Settings.System.VOICE_CALL_SIM_SETTING);
            if (QUICKCONTACTACTIVITY.equals(className)) {
                mQuickContactIsShow = true;
            }
        } else {
            if (QUICKCONTACTACTIVITY.equals(className)) {
                mQuickContactIsShow = false;
            }
            if (mQuickContactIsShow && PEOPLEACTIVITY.equals(className)) {
                LogUtils.d(TAG, "[setSimIndicatorVisibility]no hide PEOPLEACTIVITY=" + PEOPLEACTIVITY);
            } else {
                mStatusBarMgr.hideSimIndicator(componentName);
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.i(TAG, "[onReceive] action = " + action);

            if (Intent.ACTION_VOICE_CALL_DEFAULT_SIM_CHANGED.equals(action)) {
                LogUtils.i(TAG, "[onReceive]mShowSimIndicator : " + mShowSimIndicator);
                if (mShowSimIndicator) {
                    setSimIndicatorVisibility(true, null);
                }
            }
        }
    }
}
