package com.mediatek.encapsulation.android.drm;

import android.content.Context;
import android.net.Uri;

import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmUtils;
import com.mediatek.drm.OmaDrmUtils.DrmProfile;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;

public class EncapsulatedOmaDrmUtils {

    private static final String TAG = "OmaDrmUtils";

    public static boolean isDrmContent(Context context, Uri uri) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            /// M: if image is resized, the uri authority is mms
            if (uri != null && uri.getAuthority() != null && uri.getAuthority().equals("mms")) {
                return false;
            }
            try {
                DrmProfile drmProfile = OmaDrmUtils.getDrmProfile(context, uri,
                        new OmaDrmClient(context));
                MmsLog.d(TAG, "OmaDrmUtils Uri = " + uri + " isDrmContent = " + drmProfile.isDrm());
                return drmProfile.isDrm();
            } catch (IllegalArgumentException e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
