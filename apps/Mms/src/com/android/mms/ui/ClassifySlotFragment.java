/*
 * Copyright Statement:
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/*
 * MediaTek Inc. (C) 2010. All rights reserved.
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
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mms.ui;

import com.android.mms.R;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.R;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSimInfoManager;
import com.mediatek.encapsulation.android.widget.EncapsulatedListView;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyIntents;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.mms.ext.IMmsFailedNotify;
import com.mediatek.mms.ext.IStringReplacement;

/**
 * This Messsge settings activity is only for op09.
 */
public class ClassifySlotFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "ClassifySlotFragment";

    private Activity mActivity;

    private int mSlotId = 0;

    // sms preferebce.
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";

    public static final String SMS_SERVICE_CENTER = "pref_key_sms_service_center";

    public static final String SMS_MANAGE_SIM_MESSAGES = "pref_key_manage_sim_messages";

    public static final String SMS_SAVE_LOCATION = "pref_key_sms_save_location";

    public static final String SMS_SETTINGS = "pref_key_sms_settings";

    // mms preference.
    public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";

    public static final String READ_REPORT_MODE = "pref_key_mms_read_reports";

    // M: add this for read report
    public static final String READ_REPORT_AUTO_REPLY = "pref_key_mms_auto_reply_read_reports";

    public static final String AUTO_RETRIEVAL = "pref_key_mms_auto_retrieval";

    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";

    public static final String MMS_SETTINGS = "pref_key_mms_settings";

    // general preference.
    public static final String CELL_BROADCAST = "pref_key_cell_broadcast";

    private Preference mManageSimPref;

    private Preference mSmsServiceCenterPref;

    // all preferences need change key for single sim card
    private CheckBoxPreference mSmsDeliveryReport;

    private ListPreference mSmsLocation;

    // all preferences need change key for single sim card
    private CheckBoxPreference mMmsDeliveryReport;

    private CheckBoxPreference mMmsReadReport;

    // M: add this for read report
    private CheckBoxPreference mMmsAutoReplyReadReport;

    private CheckBoxPreference mMmsAutoRetrieval;

    private CheckBoxPreference mMmsRetrievalDuringRoaming;

    private Preference mCBsettingPref;

    public static final String SETTING_SAVE_LOCATION = "Phone";

    public static final String SETTING_SAVE_LOCATION_TABLET = "Device";

    private static final String LOCATION_PHONE = "Phone";

    private static final String LOCATION_SIM = "Sim";

    // / M: fix bug ALPS00455172, add tablet "device" support
    private static final String DEVICE_TYPE = "pref_key_device_type";

    public String SUB_TITLE_NAME = "sub_title_name";

    private EditText mNumberText;

    private AlertDialog mNumberTextDialog;

    private static final String MMS_PREFERENCE = "com.android.mms_preferences";

    private static final int MAX_EDITABLE_LENGTH = 20;

    // / M: Plug-in for op09.
    private IStringReplacement mStringReplacementPlugin;

    private IMmsFailedNotify mMmsFailedNotifyPlugin;

    private boolean mIsSmsEnabled = true;

    private static final String SMS_SETTING_GENERAL = "pref_key_sms_settings";

    private static final String MMS_SETTING_GENERAL = "pref_key_mms_settings";

    private static final String CB_SETTING_GENERAL = "pref_cell_broadcast_settings";

    private static final String SLOT_ID = "slotId";

    // public ClassifySlotFragment(int slotId, Activity activity) {
    // mSlotId = slotId;
    // mActivity = activity;
    // }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.v(TAG, "onCreate()");
        icicle = getArguments();
        if (icicle != null) {
            mSlotId = icicle.getInt(SLOT_ID, 0);
        }
        mStringReplacementPlugin = (IStringReplacement) MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_STRING_REPLACEMENT);
        mMmsFailedNotifyPlugin = (IMmsFailedNotify) MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_FAILED_NOTIFY);
        setMessagePreferences();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        setListPrefSummary();
        // / For KK, Default sms.
        setAllSettings();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.v(TAG, "onStop()");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        Log.d(TAG, "onAttach");
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        mActivity = null;
        super.onDetach();
    }

    public static ClassifySlotFragment newInstance(int slotId) {
        ClassifySlotFragment cf = new ClassifySlotFragment();
        Bundle b = new Bundle();
        b.putInt(SLOT_ID, slotId);
        cf.setArguments(b);
        return cf;
    }

    private void setMessagePreferences() {
        // sms preference.
        addPreferencesFromResource(R.xml.firstslotpreference);
        mSmsLocation = (ListPreference) findPreference(SMS_SAVE_LOCATION);
        mSmsServiceCenterPref = findPreference(SMS_SERVICE_CENTER);
        mSmsLocation.setOnPreferenceChangeListener(this);
        mSmsDeliveryReport = (CheckBoxPreference) findPreference(SMS_DELIVERY_REPORT_MODE);
        mManageSimPref = findPreference(SMS_MANAGE_SIM_MESSAGES);
        // mms preference.
        mMmsDeliveryReport = (CheckBoxPreference) findPreference(MMS_DELIVERY_REPORT_MODE);
        mMmsReadReport = (CheckBoxPreference) findPreference(READ_REPORT_MODE);
        mMmsAutoReplyReadReport = (CheckBoxPreference) findPreference(READ_REPORT_AUTO_REPLY);
        mMmsAutoRetrieval = (CheckBoxPreference) findPreference(AUTO_RETRIEVAL);
        mMmsRetrievalDuringRoaming = (CheckBoxPreference) findPreference(RETRIEVAL_DURING_ROAMING);
        // cell broadcast preference.
        mCBsettingPref = findPreference(CELL_BROADCAST);
        if (EncapsulatedFeatureOption.EVDO_DT_SUPPORT && MessageUtils.isUSimType(mSlotId)) {
            mMmsReadReport.setEnabled(false);
            mMmsAutoReplyReadReport.setEnabled(false);
        }
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            Log.d(TAG, "MTK_GEMINI_SUPPORT is true");
            changeSingleCardKeyToSimRelated();
            // add for op09 feature, replace string "SIM" with "UIM". @{
            String[] location = mStringReplacementPlugin.getSaveLocationString();
            if (mStringReplacementPlugin.isEnableStringReplacement() && location != null
                && MessageUtils.isUSimType(mSlotId)) {
                mSmsLocation.setEntries(location);
            }
            // @}
        }
    }

    private void changeSingleCardKeyToSimRelated() {
        EncapsulatedSimInfoManager simInfoManager = EncapsulatedSimInfoManager.getSimInfoBySlot(mActivity, mSlotId);
        Long simId = simInfoManager.getSimId();
        Log.d(TAG, "changeSingleCardKeyToSimRelated Got simId = " + simId);
        SharedPreferences spr = mActivity.getSharedPreferences("com.android.mms_preferences",
            Context.MODE_WORLD_READABLE);
        // translate all key to SIM-related key;
        if (mSmsLocation != null && MmsConfig.getSmsMultiSaveLocationEnabled()) {
            mSmsLocation.setKey(Integer.toString(mSlotId) + "_" + SMS_SAVE_LOCATION);
            mSmsLocation.setValue(spr.getString((Integer.toString(mSlotId) + "_" + SMS_SAVE_LOCATION), "Phone"));
        }
        if (mSmsDeliveryReport != null) {
            mSmsDeliveryReport.setKey(Long.toString(simId) + "_" + SMS_DELIVERY_REPORT_MODE);
            mSmsDeliveryReport.setChecked(spr
                    .getBoolean((Long.toString(simId) + "_" + SMS_DELIVERY_REPORT_MODE), false));
        }
        if (mMmsDeliveryReport != null) {
            mMmsDeliveryReport.setKey(Long.toString(simId) + "_" + MMS_DELIVERY_REPORT_MODE);
            mMmsDeliveryReport.setChecked(spr
                    .getBoolean((Long.toString(simId) + "_" + MMS_DELIVERY_REPORT_MODE), false));
        }
        if (mMmsReadReport != null) {
            mMmsReadReport.setKey(Long.toString(simId) + "_" + READ_REPORT_MODE);
            mMmsReadReport.setChecked(spr.getBoolean((Long.toString(simId) + "_" + READ_REPORT_MODE), false));
        }
        if (mMmsAutoReplyReadReport != null) {
            mMmsAutoReplyReadReport.setKey(Long.toString(simId) + "_" + READ_REPORT_AUTO_REPLY);
            mMmsAutoReplyReadReport.setChecked(spr.getBoolean((Long.toString(simId) + "_" + READ_REPORT_AUTO_REPLY),
                false));
        }
        if (mMmsAutoRetrieval != null) {
            mMmsAutoRetrieval.setKey(Long.toString(simId) + "_" + AUTO_RETRIEVAL);
            mMmsAutoRetrieval.setChecked(spr.getBoolean((Long.toString(simId) + "_" + AUTO_RETRIEVAL), true));
        }
        if (mMmsRetrievalDuringRoaming != null) {
            mMmsRetrievalDuringRoaming.setDependency(Long.toString(simId) + "_" + AUTO_RETRIEVAL);
            mMmsRetrievalDuringRoaming.setKey(Long.toString(simId) + "_" + RETRIEVAL_DURING_ROAMING);
            mMmsRetrievalDuringRoaming.setChecked(spr.getBoolean(
                (Long.toString(simId) + "_" + RETRIEVAL_DURING_ROAMING), false));
        }
        // / M: For op09 Feature, replace "SIM" with "UIM". @{
        String ctString = mStringReplacementPlugin.getCTStrings(IStringReplacement.MANAGE_CARD_MSG_TITLE);
        if (mStringReplacementPlugin.isEnableStringReplacement() && ctString != null && mSlotId == 0) {
            mManageSimPref.setTitle(ctString);
            mManageSimPref
                    .setSummary(mStringReplacementPlugin.getCTStrings(IStringReplacement.MANAGE_CARD_MSG_SUMMARY));
        }
        // / @}
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mManageSimPref) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                Log.d(TAG, "slotId is : " + mSlotId);
                if (mSlotId != -1) {
                    Intent it = new Intent();
                    it.setClass(mActivity, ManageSimMessages.class);
                    it.putExtra("SlotId", mSlotId);
                    startActivity(it);
                }
            } else {
                startActivity(new Intent(mActivity, ManageSimMessages.class));
            }
        } else if (preference == mSmsDeliveryReport && MmsConfig.isSupportCTFeature()) {
            // / M: for CT feature.
            if (mSmsDeliveryReport.isChecked() && !MmsConfig.isAllowDRWhenRoaming(mActivity, mSlotId)) {
                mSmsDeliveryReport.setChecked(false);
                if (mMmsFailedNotifyPlugin != null) {
                    mMmsFailedNotifyPlugin.popupToast(IMmsFailedNotify.DISABLE_DELIVERY_REPORT, null);
                }
            }
        } else if (preference == mSmsServiceCenterPref) {
            if (EncapsulatedFeatureOption.EVDO_DT_SUPPORT && MessageUtils.isUSimType(mSlotId)) {
                showToast(R.string.cdma_not_support);
            } else {
                if (!EncapsulatedGeminiSmsManager.isSmsReady(mSlotId)) {
                    showToast(R.string.sms_not_ready);
                    return true;
                }
                AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
                mNumberText = new EditText(dialog.getContext());
                mNumberText.setHint(R.string.type_to_compose_text_enter_to_send);
                mNumberText.computeScroll();
                mNumberText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(MAX_EDITABLE_LENGTH)});
                mNumberText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_CLASS_PHONE);
                EncapsulatedTelephonyService teleService = EncapsulatedTelephonyService.getInstance();
                String gotScNumber;
                try {
                    if (teleService == null) {
                        gotScNumber = null;
                        Log.e(MmsApp.TXN_TAG, "teleService == null.\n");
                    } else {
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                            gotScNumber = teleService.getScAddressGemini(mSlotId);
                        } else {
                            gotScNumber = teleService.getScAddressGemini(0);
                        }
                    }
                } catch (RemoteException e) {
                    gotScNumber = null;
                    Log.e(MmsApp.TXN_TAG, "getScAddressGemini is failed.\n" + e.toString());
                }
                Log.d(TAG, "gotScNumber is: " + gotScNumber);
                mNumberText.setText(gotScNumber);
                mNumberTextDialog = dialog.setIcon(R.drawable.ic_dialog_info_holo_light).setTitle(
                    R.string.sms_service_center).setView(mNumberText).setPositiveButton(R.string.OK,
                    new PositiveButtonListener()).setNegativeButton(R.string.Cancel, new NegativeButtonListener())
                        .show();
            }
        } else if (preference == mCBsettingPref) {
            Log.d(TAG, "mCBsettingPref slotId is : " + mSlotId);
            if (EncapsulatedFeatureOption.EVDO_DT_SUPPORT && MessageUtils.isUSimType(mSlotId)) {
                showToast(R.string.cdma_not_support);
            } else {
                Intent it = new Intent();
                it.setClassName("com.android.phone", "com.mediatek.settings.CellBroadcastActivity");
                it.setAction(Intent.ACTION_MAIN);
                it.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, mSlotId);
                it.putExtra(SUB_TITLE_NAME, EncapsulatedSimInfoManager.getSimInfoBySlot(mActivity, mSlotId)
                        .getDisplayName());
                startActivity(it);
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void showToast(int id) {
        Toast t = Toast.makeText(mActivity, mActivity.getString(id), Toast.LENGTH_SHORT);
        t.show();
    }

    private class PositiveButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // write to the SIM Card.
            // / M: added for bug ALPS00314789 begin
            if (!isValidAddr(mNumberText.getText().toString())) {
                String num = mNumberText.getText().toString();
                String strUnSpFormat = mActivity.getResources().getString(R.string.unsupported_media_format, "");
                Toast.makeText(mActivity, strUnSpFormat, Toast.LENGTH_SHORT).show();
                return;
            }
            // / M: added for bug ALPS00314789 end
            final EncapsulatedTelephonyService teleService = EncapsulatedTelephonyService.getInstance();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        teleService.setScAddressGemini(mNumberText.getText().toString(), mSlotId);
                    } catch (RemoteException e1) {
                        Log.e(TAG, "setScAddressGemini is failed.\n" + e1.toString());
                    } catch (NullPointerException e2) {
                        Log.e(TAG, "setScAddressGemini is failed.\n" + e2.toString());
                    }
                }
            }).start();
        }
    }

    private class NegativeButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // cancel
            dialog.dismiss();
        }
    }

    private boolean isValidAddr(String address) {
        boolean ret = true;
        if (address.isEmpty()) {
            return ret;
        }
        if (address.charAt(0) == '+') {
            for (int i = 1, count = address.length(); i < count; i++) {
                if (address.charAt(i) < '0' || address.charAt(i) > '9') {
                    ret = false;
                    break;
                }
            }
        } else {
            for (int i = 0, count = address.length(); i < count; i++) {
                if (address.charAt(i) < '0' || address.charAt(i) > '9') {
                    ret = false;
                    break;
                }
            }
        }
        return ret;
    }

    private CharSequence getVisualTextName(int slotId, Context context, String enumName, int choiceNameResId,
            int choiceValueResId) {
        /// M: Modify for CT, replace "SIM" with "UIM" @{
        CharSequence[] visualNames = null;
        if (mStringReplacementPlugin.isEnableStringReplacement()) {
            if (MessageUtils.isUSimType(slotId) && choiceNameResId == R.array.pref_sms_save_location_choices) {
                String[] location = mStringReplacementPlugin.getSaveLocationString();
                visualNames = location;
            } else {
                visualNames = context.getResources().getTextArray(choiceNameResId);
            }
        } else { /// M: @}
            visualNames = context.getResources().getTextArray(choiceNameResId);
        }
        CharSequence[] enumNames = context.getResources().getTextArray(choiceValueResId);
        // Sanity check
        if (visualNames.length != enumNames.length) {
            return "";
        }
        for (int i = 0; i < enumNames.length; i++) {
            if (enumNames[i].equals(enumName)) {
                return visualNames[i];
            }
        }
        return "";
    }

    @Override
    public boolean onPreferenceChange(Preference arg0, Object arg1) {
        final String key = arg0.getKey();
        Log.d(TAG, "key: " + key);
        String stored = (String) arg1;
        Log.d(TAG, "" + mSlotId);
        Log.d(TAG, Integer.toString(mSlotId) + "_" + SMS_SAVE_LOCATION);
        if ((Integer.toString(mSlotId) + "_" + SMS_SAVE_LOCATION).equals(key)
            || ((SMS_SAVE_LOCATION.equals(key) && MmsConfig.getSmsMultiSaveLocationEnabled()))) {
            if (!mActivity.getResources().getBoolean(R.bool.isTablet)) {
                Log.d(TAG, "enter");
                mSmsLocation.setSummary(getVisualTextName(mSlotId, mActivity, stored,
                    R.array.pref_sms_save_location_choices, R.array.pref_sms_save_location_values));
            } else {
                mSmsLocation.setSummary(MessageUtils.getVisualTextName(mActivity, stored,
                    R.array.pref_tablet_sms_save_location_choices, R.array.pref_tablet_sms_save_location_values));
            }
        }
        return true;
    }

    private void setListPrefSummary() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mActivity);
        // / M: fix bug ALPS00455172, add tablet "device" support
        SharedPreferences.Editor editor = sp.edit();
        if (!getResources().getBoolean(R.bool.isTablet)) {
            editor.putString(DEVICE_TYPE, "Phone");
        } else {
            editor.putString(DEVICE_TYPE, "Device");
        }
        editor.commit();
        // For mSmsLocation;
        String saveLocation = null;
        if (MmsConfig.getSmsMultiSaveLocationEnabled()) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                saveLocation = sp.getString(mSlotId + "_" + SMS_SAVE_LOCATION, "Phone");
            } else {
                saveLocation = sp.getString(SMS_SAVE_LOCATION, "Phone");
            }
        }
        if (saveLocation == null) {
            // / M: fix bug ALPS00429244, change "Phone" to "Device"
            if (!getResources().getBoolean(R.bool.isTablet)) {
                saveLocation = sp.getString(SMS_SAVE_LOCATION, "Phone");
            } else {
                saveLocation = sp.getString(SMS_SAVE_LOCATION, "Device");
            }
        }
        if (!mActivity.getResources().getBoolean(R.bool.isTablet)) {
            mSmsLocation.setSummary(getVisualTextName(mSlotId, mActivity, saveLocation,
                R.array.pref_sms_save_location_choices, R.array.pref_sms_save_location_values));
        } else {
            mSmsLocation.setSummary(MessageUtils.getVisualTextName(mActivity, saveLocation,
                R.array.pref_tablet_sms_save_location_choices, R.array.pref_tablet_sms_save_location_values));
        }
    }

    public void setCategoryDisable() {
        PreferenceCategory smsSettings = (PreferenceCategory) findPreference(SMS_SETTING_GENERAL);
        if (smsSettings != null) {
            smsSettings.setEnabled(false);
        }
        PreferenceCategory mmsSettings = (PreferenceCategory) findPreference(MMS_SETTING_GENERAL);
        if (mmsSettings != null) {
            mmsSettings.setEnabled(false);
        }
        PreferenceCategory notificationSettins = (PreferenceCategory) findPreference(CB_SETTING_GENERAL);
        if (notificationSettins != null) {
            notificationSettins.setEnabled(false);
        }
    }

    public void setCategoryEnable() {
        PreferenceCategory smsSettings = (PreferenceCategory) findPreference(SMS_SETTING_GENERAL);
        if (smsSettings != null) {
            smsSettings.setEnabled(true);
        }
        PreferenceCategory mmsSettings = (PreferenceCategory) findPreference(MMS_SETTING_GENERAL);
        if (mmsSettings != null) {
            mmsSettings.setEnabled(true);
        }
        PreferenceCategory cbSettins = (PreferenceCategory) findPreference(CB_SETTING_GENERAL);
        if (cbSettins != null) {
            cbSettins.setEnabled(true);
        }
    }

    // / M: For KK new feature,Default sms. @{
    private void setAllSettings() {
        mIsSmsEnabled = MmsConfig.isSmsEnabled(mActivity);
        if (mIsSmsEnabled) {
            setCategoryEnable();
        } else {
            setCategoryDisable();
        }
    }
    // / M: @}
}
