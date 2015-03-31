
package com.mediatek.encapsulation.android.drm;

import android.content.Context;
import android.net.Uri;

import com.mediatek.drm.OmaDrmClient;

import com.mediatek.encapsulation.android.drm.EncapsulatedDrmStore;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;

/**
 * The main programming interface for the DRM framework. An application must
 * instantiate this class to access DRM agents through the DRM framework.
 */
public class EncapsulatedDrmManagerClient extends OmaDrmClient {

    private static final String TAG = "OmaDrmClient";

    /**
     * Creates a <code>DrmManagerClient</code>.
     * 
     * @param context Context of the caller.
     */

    public EncapsulatedDrmManagerClient(Context context) {
        super(context);
    }

    protected void finalize() {
        MmsLog.d(TAG, "finalize OmaDrmClient instance");
    }

    /**
     * Checks whether the given rights-protected content has valid rights for
     * the specified {@link DrmStore.Action}.
     * 
     * @param path Path to the rights-protected content.
     * @param action The {@link DrmStore.Action} to perform.
     * @return An <code>int</code> representing the
     *         {@link DrmStore.RightsStatus} of the content.
     */
    /** M: MTK ADD */
    public int checkRightsStatus(String path, int action) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return super.checkRightsStatus(path, action);
        } else {
            /**
             * M:can't complete this branch 
             */
            return EncapsulatedDrmStore.RightsStatus.RIGHTS_VALID;
        }
    }

    /**
     * Checks whether the given rights-protected content has valid rights for the specified
     * {@link DrmStore.Action}.
     *
     * @param uri URI for the rights-protected content.
     * @param action The {@link DrmStore.Action} to perform.
     *
     * @return An <code>int</code> representing the {@link DrmStore.RightsStatus} of the content.
     */
    public int checkRightsStatus(Uri uri, int action) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return super.checkRightsStatus(uri, action);
        } else {
            return EncapsulatedDrmStore.RightsStatus.RIGHTS_VALID;
        }
    }

    /**
     * Retrieves the MIME type embedded in the original content.
     *
     * @param path Path to the rights-protected content.
     *
     * @return The MIME type of the original content, such as <code>video/mpeg</code>.
     */
    public String getOriginalMimeType(String path) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return super.getOriginalMimeType(path);
        } else {
            return "";
        }
    }

    /**
     * Retrieves the MIME type embedded in the original content.
     *
     * @param uri URI of the rights-protected content.
     *
     * @return MIME type of the original content, such as <code>video/mpeg</code>.
     */
    public String getOriginalMimeType(Uri uri) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return super.getOriginalMimeType(uri);
        } else {
            return "";
        }
    }

    /**
     * Checks whether the given MIME type or path can be handled.
     *
     * @param path Path of the content to be handled.
     * @param mimeType MIME type of the object to be handled.
     *
     * @return True if the given MIME type or path can be handled; false if they cannot be handled.
     */
    public boolean canHandle(String path, String mimeType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return super.canHandle(path, mimeType);
        } else {
            return true;
        }
    }
}
