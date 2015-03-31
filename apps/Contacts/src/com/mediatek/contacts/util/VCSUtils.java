package com.mediatek.contacts.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;

public class VCSUtils {
    private static boolean IS_ANIMATOR_ENABLE = false;
    private static boolean MTK_VOICE_CONTACT_SEARCH_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_VOICE_CONTACT_SEARCH_SUPPORT;
    public static final String KEY_DISABLE_VCS_BY_USER = "disable_vcs_by_user";
    public static final String PREFERENCE_NAME = "vcs_preference";
    /**
     * [VCS] whether VCS feature enabled on this device
     * 
     * @return ture if allowed to enable
     */
    public static boolean isVCSFeatureEnabled() {
        return MTK_VOICE_CONTACT_SEARCH_SUPPORT;
    }

    /**
     * 
     * @param context
     * @return true if vcs if enable by user,false else.default will return true.
     */
    public static boolean isVcsEnableByUser(Context context) {
        SharedPreferences sp = context.getSharedPreferences(VCSUtils.PREFERENCE_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(VCSUtils.KEY_DISABLE_VCS_BY_USER, true);
    }

    /**
     * 
     * @param enable true to enable the vcs,false to disable.
     * @param context
     */
    public static void setVcsEnableByUser(boolean enable, Context context) {
        SharedPreferences sp = context.getSharedPreferences(VCSUtils.PREFERENCE_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_DISABLE_VCS_BY_USER, enable).commit();
    }
    
    public static boolean isAnimatorEnable(){
        return IS_ANIMATOR_ENABLE;
    }

}
