package com.mediatek.mail.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.NetworkInfo.DetailedState;
import android.os.SystemProperties;

import com.android.mail.utils.LogUtils;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmUtils;

public class Utility {
    /**
     * M:Check if device has a network connection (wifi or data)
     * @param context
     * @return true if network connected
     */
    public static boolean hasConnectivity(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            DetailedState state = info.getDetailedState();
            if (state == DetailedState.CONNECTED) {
                return true;
            }
        }
        return false;
    }

    /**
     * M: Get the original MimeType of DRM file.
     * @param context the context to create DrmClient
     * @param uri the content URI
     * @return The original MimeType of DRM file if it was DRM file, otherwise return null.
     */
    public static String getDRMOriginalMimeType(Context context, Uri uri) {
        String type = null;
        if (FeatureOption.MTK_DRM_APP) {
            OmaDrmClient drmClient = new OmaDrmClient(context);
            OmaDrmUtils.DrmProfile profile = OmaDrmUtils.getDrmProfile(context, uri, drmClient);
            if (profile.isDrm()) {
                type = drmClient.getOriginalMimeType(uri);
                LogUtils.d(LogUtils.TAG, "The original type of [%s] is %s.", uri, type);
            } else {
                LogUtils.d(LogUtils.TAG, "[%s] is not DRM.", uri);
            }
            drmClient.release();
        }
        return type;
    }
}
