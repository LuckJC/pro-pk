
package com.mediatek.providers.contacts;

public class ContactsFeatureConstants {

    public interface FeatureOption {
        boolean MTK_SEARCH_DB_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_SEARCH_DB_SUPPORT;
        boolean MTK_DIALER_SEARCH_SUPPORT = com.mediatek.common.featureoption.
                FeatureOption.MTK_DIALER_SEARCH_SUPPORT;
        boolean MTK_GEMINI_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_GEMINI_SUPPORT;
        boolean MTK_PHONE_NUMBER_GEODESCRIPTION = com.mediatek.common.featureoption.FeatureOption.
                MTK_PHONE_NUMBER_GEODESCRIPTION;
        static boolean MTK_VVM_SUPPORT = true; //[VVM] vvm is a Google default feature.
    }

    public static boolean DBG_DIALER_SEARCH = true;
}
