
package com.mediatek.encapsulation.android.telephony;

import android.content.Context;
import android.telephony.TelephonyManager;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.com.mediatek.telephony.EncapsulatedTelephonyManagerEx;
import com.mediatek.telephony.TelephonyManagerEx;

public class EncapsulatedTelephonyManager {

    private static TelephonyManager mTelephonyManager;

    public EncapsulatedTelephonyManager(Context context) {
        if (null == mTelephonyManager && null != context) {
            mTelephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
        }
    }

    public EncapsulatedTelephonyManager() {
    }

    /** M: MTK ADD */
    public boolean hasIccCard(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return EncapsulatedTelephonyManagerEx.getDefault().hasIccCard(simId);
        } else {
            /** M: Can not complete for this branch. */
            return false;
        }
    }

    /** M: MTK ADD */
    public boolean isNetworkRoamingGemini(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return TelephonyManagerEx.getDefault().isNetworkRoaming(simId);
        } else {
            /** M: Can not complete for this branch. */
            return false;
        }
    }

}
