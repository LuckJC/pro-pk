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

import android.content.Context;
import android.util.Log;

import com.mediatek.contacts.ext.CallDetailExtension;
import com.mediatek.contacts.ext.CallDetailHistoryAdapterExtension;
import com.mediatek.contacts.ext.CallListExtension;
import com.mediatek.contacts.ext.CallLogAdapterExtension;
import com.mediatek.contacts.ext.CallLogSearchResultActivityExtension;
import com.mediatek.contacts.ext.CallLogSimInfoHelperExtension;
import com.mediatek.contacts.ext.ContactAccountExtension;
import com.mediatek.contacts.ext.ContactDetailEnhancementExtension;
import com.mediatek.contacts.ext.ContactDetailExtension;
import com.mediatek.contacts.ext.ContactListExtension;
import com.mediatek.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.ext.ContactsCallOptionHandlerExtension;
import com.mediatek.contacts.ext.ContactsCallOptionHandlerFactoryExtension;
import com.mediatek.contacts.ext.DialPadExtension;
import com.mediatek.contacts.ext.DialerSearchAdapterExtension;
import com.mediatek.contacts.ext.DialtactsExtension;
import com.mediatek.contacts.ext.IContactPlugin;
import com.mediatek.contacts.ext.IccCardExtension;
import com.mediatek.contacts.ext.ImportExportEnhancementExtension;
import com.mediatek.contacts.ext.QuickContactExtension;
import com.mediatek.contacts.ext.SimPickExtension;
import com.mediatek.contacts.ext.SimServiceExtension;
import com.mediatek.contacts.ext.SpeedDialExtension;
import com.mediatek.contacts.extension.ContactPluginExtensionContainer;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.Plugin.ObjectCreationException;
import com.mediatek.pluginmanager.PluginManager;

public class ExtensionManager {
    private static final String TAG = "ExtensionManager";

    public static final String RCS_CONTACT_PRESENCE_CHANGED = "android.intent.action.RCS_CONTACT_PRESENCE_CHANGED";
    public static final String RCS_CONTACT_UNREAD_NUMBER_CHANGED = "android.intent.action.RCS_CONTACT_UNREAD_NUMBER_CHANGED";
    
    public static final String COMMD_FOR_RCS = "ExtenstionForRCS";//TDDO:need to remove
    public static final String COMMD_FOR_AAS = "ExtensionForAAS";//TDDO:need to remove
    public static final String COMMD_FOR_SNE = "ExtensionForSNE";//TDDO:need to remove
    public static final String COMMD_FOR_SNS = "ExtensionForSNS";//TDDO:need to remove

    private static ExtensionManager sInstance = null;
    private ContactPluginExtensionContainer mContactPluinContainer = null;

    private ExtensionManager() {
        refreshPlugins();
    }

    public static ExtensionManager getInstance() {
        if (sInstance == null) {
            createInstanceSynchronized();
        }
        return sInstance;
    }

    private static synchronized void createInstanceSynchronized() {
        if (sInstance == null) {
            sInstance = new ExtensionManager();
        }
    }

    private void getPlugin() {
        Context applicationContext = GlobalEnv.getApplicationContext();
        LogUtils.i(TAG, "[getPlugin] applicationContext : " + applicationContext);
        PluginManager<IContactPlugin> pm = PluginManager.<IContactPlugin> create(
                applicationContext, IContactPlugin.class.getName());
        int num = pm.getPluginCount();
        LogUtils.i(TAG, "[getPlugin]plugin num : " + num);
        if (num == 0) {
            LogUtils.e(TAG, "[getPlugin]no plugin apk");
            return;
        }
        try {
            for (int i = 0; i < num; i++) {
                Plugin<IContactPlugin> contactPlugin = pm.getPlugin(i);
                if (null != contactPlugin) {
                    IContactPlugin plugin = contactPlugin.createObject();
                    LogUtils.i(TAG, "[getPlugin] addExtension:" + plugin.getClass().getSimpleName() + ",plugin:" + plugin);
                    mContactPluinContainer.addExtensions(plugin);
                } else {
                    LogUtils.e(TAG, "[getPlugin]contactPlugin is null");
                }
            }
        } catch (ObjectCreationException e) {
            LogUtils.e(TAG, "[getPlugin]ObjectCreationException:");
            e.printStackTrace();
        }
    }

    public CallDetailExtension getCallDetailExtension() {
        return mContactPluinContainer.getCallDetailExtension();
    }

    public CallListExtension getCallListExtension() {
        return mContactPluinContainer.getCallListExtension();
    }

    public ContactAccountExtension getContactAccountExtension() {
        return mContactPluinContainer.getContactAccountExtension();
    }

    public ContactDetailExtension getContactDetailExtension() {
        return mContactPluinContainer.getContactDetailExtension();
    }

    public ContactListExtension getContactListExtension() {
        return mContactPluinContainer.getContactListExtension();
    }

    public DialPadExtension getDialPadExtension() {
        return mContactPluinContainer.getDialPadExtension();
    }

    public DialtactsExtension getDialtactsExtension() {
        return mContactPluinContainer.getDialtactsExtension();
    }

    public SpeedDialExtension getSpeedDialExtension() {
        return mContactPluinContainer.getSpeedDialExtension();
    }

    public SimPickExtension getSimPickExtension() {
        return mContactPluinContainer.getSimPickExtension();
    }

    public QuickContactExtension getQuickContactExtension() {
        return mContactPluinContainer.getQuickContactExtension();
    }

    public ContactsCallOptionHandlerExtension getContactsCallOptionHandlerExtension() {
        return mContactPluinContainer.getContactsCallOptionHandlerExtension();
    }

    public ContactsCallOptionHandlerFactoryExtension getContactsCallOptionHandlerFactoryExtension() {
        return mContactPluinContainer.getContactsCallOptionHandlerFactoryExtension();
    }

    public CallLogAdapterExtension getCallLogAdapterExtension() {
        return mContactPluinContainer.getCallLogAdapterExtension();
    }

    public CallDetailHistoryAdapterExtension getCallDetailHistoryAdapterExtension() {
        return mContactPluinContainer.getCallDetailHistoryAdapterExtension();
    }

    public DialerSearchAdapterExtension getDialerSearchAdapterExtension() {
        return mContactPluinContainer.getDialerSearchAdapterExtension();
    }

    public CallLogSearchResultActivityExtension getCallLogSearchResultActivityExtension() {
        return mContactPluinContainer.getCallLogSearchResultActivityExtension();
    }

    // for ct new feature
    public ContactDetailEnhancementExtension getContactDetailEnhancementExtension() {
        return mContactPluinContainer.getContactDetailEnhancementExtension();
    }
    
    public SimServiceExtension getSimServiceExtension() {
        return mContactPluinContainer.getSimServiceExtension();
    }
    
    public ImportExportEnhancementExtension getImportExportEnhancementExtension() {
        return mContactPluinContainer.getImportExportEnhancementExtension();
    }

    public CallLogSimInfoHelperExtension getCallLogSimInfoHelperExtension() {
        return mContactPluinContainer.getCallLogSimInfoHelperExtension();
    }

    public IccCardExtension getIccCardExtension() {
        return mContactPluinContainer.getIccCardExtension();
    }

    /**
     * in some cases(like auto-test), we don't need any plug-in
     * we can call this function to remove them all.
     */
    public void resetPlugins() {
        mContactPluinContainer = new ContactPluginExtensionContainer();
    }

    /**
     * this function to refresh all Plug-ins, it will reset an empty
     * plug-in set, and retrieve new plug-in instances then
     */
    public void refreshPlugins() {
        resetPlugins();
        getPlugin();
    }

}
