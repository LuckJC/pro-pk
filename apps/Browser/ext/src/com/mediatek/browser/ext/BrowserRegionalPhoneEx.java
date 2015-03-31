package com.mediatek.browser.ext;

import android.content.Context;
import android.content.SharedPreferences;

import com.mediatek.xlog.Xlog;

public class BrowserRegionalPhoneEx implements IBrowserRegionalPhoneEx {
    private static final String TAG = "BrowserRegionalPhoneEx";

    @Override
    public boolean updateBookmarks(Context context) {
        Xlog.i(TAG, "Enter: " + "updateBookmarks" + " --default implement");
        return false;
    }

    public String getSearchEngine(SharedPreferences mPrefs, Context context) {
        Xlog.i(TAG, "Enter: " + "updateSearchEngine" + " --default implement");
        return null;
    }
}

