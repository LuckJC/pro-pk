package com.mediatek.encapsulation.android.content.res;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.SystemProperties;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedResources {

    private static final String TAG = "EncapsulatedResources";

    private Resources mResources;

    public EncapsulatedResources(Resources res) {
        mResources = res;
    }

    private static final String DEFAULT_THEME_PATH = "/system/framework/framework-res.apk";
    private static final String THEME_COLOR_PATH = "assets/color/colors.xml";
    private static final String STR_COLOR = "color";

    // For getThemeColor add cache.
    private static HashMap<String, Integer> mMtkColorCache = new HashMap<String, Integer>();
}
