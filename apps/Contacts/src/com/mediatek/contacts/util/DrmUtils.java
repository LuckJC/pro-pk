package com.mediatek.contacts.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.util.Log;
import com.mediatek.drm.OmaDrmUtils;
import com.mediatek.drm.OmaDrmUtils.DrmProfile;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;

public class DrmUtils {
    private static final String TAG = DrmUtils.class.getSimpleName();

    /**
     * 
     * @param context
     * @param uri
     * @return true if the uri indicate a Drm image,false else
     */
    public static boolean isDrmImage(Context context, Uri uri) {
        if(!FeatureOption.MTK_DRM_APP){
            Log.i(TAG,"[isDrmImage] not support drm...");
            return false;
        }
        DrmProfile profile = OmaDrmUtils.getDrmProfile(context,uri,new OmaDrmClient(context));
        boolean isDrm = profile.isDrm();
        Log.i(TAG,"[isDrmImage] isDrm:" + isDrm + ",uri:" + uri);
        return isDrm;
    }
}
