package com.mediatek.encapsulation.android.drm;

import com.mediatek.drm.OmaDrmStore;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedDrmStore {

    /**
     *  M: Defines status notifications for digital rights.
     */
    public static class RightsStatus {
        // M:
        // this is added for OMA DRM v1.0 implementation
        /**
         * M: Constant field signifies that the secure timer is invalid
         * @hide
         */
        public static final int SECURE_TIMER_INVALID = EncapsulationConstant.USE_MTK_PLATFORM ?
                            OmaDrmStore.RightsStatus.SECURE_TIMER_INVALID : 0x04;

        public static final int RIGHTS_VALID = EncapsulationConstant.USE_MTK_PLATFORM ?
                            OmaDrmStore.RightsStatus.RIGHTS_VALID : 0x00;
    }

    /**
     * M: defines the drm extra key & value.
     * @hide
     */
    public static class DrmExtra {
        public static final String EXTRA_DRM_LEVEL = EncapsulationConstant.USE_MTK_PLATFORM ?
                        OmaDrmStore.DrmExtra.EXTRA_DRM_LEVEL : "android.intent.extra.drm_level";
        public static final int DRM_LEVEL_SD = EncapsulationConstant.USE_MTK_PLATFORM ?
                        OmaDrmStore.DrmExtra.DRM_LEVEL_SD : 2;
    }

    /**
     * Defines actions that can be performed on rights-protected content.
     */
    public static class Action {
        /**
         * The rights-protected content can be set as a ringtone.
         */
        public static final int RINGTONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                                OmaDrmStore.Action.RINGTONE : 0x02;
        /**
         * The rights-protected content can be transferred.
         */
        public static final int TRANSFER = EncapsulationConstant.USE_MTK_PLATFORM ?
                                OmaDrmStore.Action.TRANSFER : 0x03;
        /**
         * The rights-protected content can be displayed.
         */
        public static final int DISPLAY = EncapsulationConstant.USE_MTK_PLATFORM ?
                                OmaDrmStore.Action.DISPLAY : 0x07;
    }
}
