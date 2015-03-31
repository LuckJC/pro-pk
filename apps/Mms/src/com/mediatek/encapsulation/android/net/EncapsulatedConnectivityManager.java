package com.mediatek.encapsulation.android.net;

import android.net.ConnectivityManager;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedConnectivityManager {

    private ConnectivityManager mConnectivityManager;

    public EncapsulatedConnectivityManager(ConnectivityManager cm) {
        mConnectivityManager = cm;
    }

    /**
     * Gemini
     * Tells the underlying networking system that the caller wants to
     * begin using the named feature. The interpretation of {@code feature}
     * is completely up to each networking implementation.
     * @param networkType specifies which network the request pertains to
     * @param feature the name of the feature to be used
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is specific to each networking
     * implementation+feature combination, except that the value {@code -1}
     * always indicates failure.
     */
    public int startUsingNetworkFeatureGemini(int networkType, String feature, int radioNum) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mConnectivityManager.startUsingNetworkFeatureGemini(networkType, feature, radioNum);
        } else {
            return mConnectivityManager.startUsingNetworkFeature(networkType, feature);
        }
    }

    /**
     * Gemini
     * Tells the underlying networking system that the caller is finished
     * using the named feature. The interpretation of {@code feature}
     * is completely up to each networking implementation.
     * @param networkType specifies which network the request pertains to
     * @param feature the name of the feature that is no longer needed
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is specific to each networking
     * implementation+feature combination, except that the value {@code -1}
     * always indicates failure.
     */
    public int stopUsingNetworkFeatureGemini(int networkType, String feature, int radioNum) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mConnectivityManager.stopUsingNetworkFeatureGemini(networkType, feature, radioNum);
        } else {
            return mConnectivityManager.stopUsingNetworkFeature(networkType, feature);
        }
    }
}