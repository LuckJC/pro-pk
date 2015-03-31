package com.mediatek.encapsulation.com.android.internal.telephony;

import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedTelephonyIntents {
    public static final String ACTION_SIM_INDICATOR_STATE_CHANGED =
                            EncapsulationConstant.USE_MTK_PLATFORM ?
                            TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED
                            : "android.intent.action.SIM_INDICATOR_STATE_CHANGED";

    public static final String INTENT_KEY_ICC_SLOT =
                            EncapsulationConstant.USE_MTK_PLATFORM ?
                            TelephonyIntents.INTENT_KEY_ICC_SLOT
                            : "slotId";

    public static final String ACTION_SIM_INFO_UPDATE =
                            EncapsulationConstant.USE_MTK_PLATFORM ?
                            TelephonyIntents.ACTION_SIM_INFO_UPDATE
                            : "android.intent.action.SIM_INFO_UPDATE";

    public static final String ACTION_SERVICE_STATE_CHANGED =
                            EncapsulationConstant.USE_MTK_PLATFORM ?
                            TelephonyIntents.ACTION_SERVICE_STATE_CHANGED
                            : "android.intent.action.SERVICE_STATE";

    /* ALPS01139189 */
    public static final String ACTION_HIDE_NW_STATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                            TelephonyIntents.ACTION_HIDE_NW_STATE
                            : "android.intent.action.ACTION_HIDE_NW_STATE";

    public static final String EXTRA_ACTION = EncapsulationConstant.USE_MTK_PLATFORM ?
                            TelephonyIntents.EXTRA_ACTION : "action";

    public static final String EXTRA_REAL_SERVICE_STATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                            TelephonyIntents.EXTRA_REAL_SERVICE_STATE : "state";

}