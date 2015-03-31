package com.mediatek.email.ext;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;

public interface IServerProviderPlugin {

    /**
     * M: check if need to support provider List function.
     *
     * @return true if support, the value is set in in plugin.
     */
    public boolean isSupportProviderList();

    /**
     * M: get the extension providers domains.
     *
     * @return extension provider domains.
     */
    public String[] getProviderDomains();

    /**
     * M: get the extension providers' names.
     *
     * @return extension provider names.
     */
    public String[] getProviderNames();

    /**
     * M: get the plugin context.
     *
     * @return
     */
    public Context getContext();

    /**
     * M: get the acount description, used in account setting step.
     *
     * @return
     */
    public String getAccountNameDescription();

    /**
     * M: get the extension provider xml, use this to get the provider host.
     *
     * @return
     */
    public int getProviderXml();

    /**
     * M: get the provider icons, used to show AccountSetupChooseESP listview.
     *
     * @return
     */
    public int[] getProviderIcons();

    /**
     * M: get the provider number to display in chooseESP activity.
     */
    public int getDisplayESPNum();

    /**
     * M: get the account signature, use to display in send mail content.
     *
     * @return
     */
    public String getAccountSignature();

    /** M: get the default provider domain, use to check the account whether is default.
     *
     * @return
     */
    public String getDefaultProviderDomain();
}
