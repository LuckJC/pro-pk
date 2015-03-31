package com.mediatek.encapsulation.android.net;

import android.net.NetworkInfo;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedNetworkInfo {

    private NetworkInfo mNetworkInfo;

    private int mSlot;

    public EncapsulatedNetworkInfo(NetworkInfo info, int slot) {
        mNetworkInfo = info;
        mSlot = slot;
    }

    //MTK-START [mtk04070][111128][ALPS00093395]MTK proprietary methods
    /**
     * Return a sim IDm fir exanoke SIM1 or SIM2
     * @return the id of SIM
     */
    public int getSimId() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mNetworkInfo.getSimId();
        } else {
            return mSlot;
        }
    }
}