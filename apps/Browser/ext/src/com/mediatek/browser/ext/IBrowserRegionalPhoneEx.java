package com.mediatek.browser.ext;

import android.content.Context;
import android.content.SharedPreferences;

public interface IBrowserRegionalPhoneEx {

    boolean updateBookmarks(Context context);

    String getSearchEngine(SharedPreferences mPrefs, Context context);
}

