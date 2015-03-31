package com.mediatek.cbsettings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telephony.ServiceState;
import android.view.MenuItem;

import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyIntents;
//import com.android.phone.PhoneGlobals;
import com.android.mms.R;
import com.mediatek.cbsettings.CellBroadcastCheckBox;
import com.mediatek.cbsettings.TimeConsumingPreferenceActivity;
import com.mediatek.cbsettings.CellBroadcastSettings;

import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSimInfoManager;
import com.mediatek.encapsulation.android.content.EncapsulatedAction;
import com.mediatek.encapsulation.MmsLog;

import java.util.List;

public class CellBroadcastActivity extends TimeConsumingPreferenceActivity {
    private static final String BUTTON_CB_CHECKBOX_KEY     = "enable_cellBroadcast";
    private static final String BUTTON_CB_SETTINGS_KEY     = "cbsettings";
    private static final String LOG_TAG = "Mms/CellBroadcastActivity";
    private static final String SUB_TITLE_NAME = "sub_title_name";
    public static final int UNDEFINED_SLOT_ID = -1;
    int mSlotId = EncapsulatedPhone.GEMINI_SIM_1;
    private ServiceState mServiceState;

    private CellBroadcastCheckBox mCBCheckBox = null;
    private Preference mCBSetting = null;

    private boolean mAirplaneModeEnabled = false;
    private int mDualSimMode = -1;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MmsLog.d(LOG_TAG, "[action = " + action + "]");
            if (EncapsulatedTelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)) {
                setScreenEnabled();
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                MmsLog.d(LOG_TAG, "mAirplaneModeEnabled: " +mAirplaneModeEnabled);
                /// M: ALPS00740653 @{
                // when airplane mode is on, the phone state must not be out of service,
                // but ,but when airplane mode is off, the phone state may be out of service,
                // so airplane mode is off, we do not enable screen until phone state is in service.
                if (mAirplaneModeEnabled) {
                    setScreenEnabled();
                }
                /// @}
            } else if (action.equals(EncapsulatedAction.ACTION_DUAL_SIM_MODE_CHANGED)) {
                mDualSimMode = intent.getIntExtra(EncapsulatedAction.EXTRA_DUAL_SIM_MODE, -1);
                setScreenEnabled();
            } else if (action.equals(EncapsulatedTelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                setScreenEnabled();
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.cell_broad_cast);
        mSlotId = getIntent().getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, 0);

        mCBCheckBox = (CellBroadcastCheckBox)findPreference(BUTTON_CB_CHECKBOX_KEY);
        mCBSetting = findPreference(BUTTON_CB_SETTINGS_KEY);

        if (null != getIntent().getStringExtra(SUB_TITLE_NAME)) {
            setTitle(getIntent().getStringExtra(SUB_TITLE_NAME));
        }
        if (mCBCheckBox != null) {
            mCBCheckBox.init(this, false, mSlotId);
            mCBCheckBox.setSummary(mCBCheckBox.isChecked()
                    ? R.string.sum_cell_broadcast_control_on : R.string.sum_cell_broadcast_control_off);
        }
        /// M: ALPS00670751 @{
        registerBroadcast();
        /// @}
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mCBSetting) {
            Intent intent = new Intent(this, CellBroadcastSettings.class);
            intent.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, mSlotId);
            this.startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void setScreenEnabled() {
        ///M: add for hot swap {
        handleSimHotSwap(this, mSlotId);
        ///@}

        /// M: ALPS00670751 @{
        // when no airplane , exist sim card and  have dual sim mode
        // set screen disable
        enableScreen();
        /// @}
    }

    @Override
    public void onResume() {
        super.onResume();
        mAirplaneModeEnabled = android.provider.Settings.System.getInt(getContentResolver(),
                EncapsulatedSettings.Global.AIRPLANE_MODE_ON, -1) == 1;
        if (isGeminiSupport()) {
            mDualSimMode = android.provider.Settings.System.getInt(getContentResolver(),
                    EncapsulatedSettings.System.DUAL_SIM_MODE_SETTING, -1);
            MmsLog.d(LOG_TAG, "onResume(), mDualSimMode=" + mDualSimMode);
        }
        setScreenEnabled();
    }

    private void enableScreen() {
        List<EncapsulatedSimInfoManager> insertedSimInfoList = EncapsulatedSimInfoManager.getInsertedSimInfoList(this);
        boolean isShouldEnabled = false;
        boolean isHasSimCard = ((insertedSimInfoList != null) && (insertedSimInfoList.size() > 0));
        isShouldEnabled = (!mAirplaneModeEnabled) && (mDualSimMode != 0) && isHasSimCard;
        getPreferenceScreen().setEnabled(isShouldEnabled);
    }

    private void registerBroadcast() {
        IntentFilter intentFilter = new IntentFilter(EncapsulatedTelephonyIntents.ACTION_SIM_INFO_UPDATE);
        intentFilter.addAction(EncapsulatedTelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            intentFilter.addAction(EncapsulatedAction.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        registerReceiver(mReceiver, intentFilter);
    }

    public static void handleSimHotSwap(Activity activity, int slotId) {
        List<EncapsulatedSimInfoManager> temp = EncapsulatedSimInfoManager.getInsertedSimInfoList(activity);
        MmsLog.d(LOG_TAG, "slot id = " + slotId);
        if (getSiminfoIdBySimSlotId(slotId, temp) == UNDEFINED_SLOT_ID) {
            activity.finish();
        }
    }

    public static long getSiminfoIdBySimSlotId(int slotId, List<EncapsulatedSimInfoManager> simInfoList) {
        for (EncapsulatedSimInfoManager siminfo : simInfoList) {
            if (siminfo.getSlot() == slotId) {
                return siminfo.getSlot();
            }
        }
        return UNDEFINED_SLOT_ID;
    }

    /**
     * @see FeatureOption.MTK_GEMINI_SUPPORT
     * @see FeatureOption.MTK_GEMINI_3SIM_SUPPORT
     * @see FeatureOption.MTK_GEMINI_4SIM_SUPPORT
     * @return true if the device has 2 or more slots
     */
    public static boolean isGeminiSupport() {
        return EncapsulatedPhone.GEMINI_SIM_NUM >= 2;
    }
}
