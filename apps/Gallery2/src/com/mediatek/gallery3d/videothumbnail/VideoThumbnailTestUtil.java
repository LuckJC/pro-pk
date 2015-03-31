package com.mediatek.gallery3d.videothumbnail;

import android.util.Log;

public class VideoThumbnailTestUtil {
    public static boolean IS_THUMB_PLAY_DEBUG = false;
    private static final String THUMB_PLAY_DEBUG_TAG = "Gallery2/VTSPPerformance";

    private static long singleTapTime = -1;
    private static long shareClickTime = -1;

    public static void markSingleTapTime() {
        singleTapTime = System.currentTimeMillis();
    }

    public static void markShareClickTime() {
        shareClickTime = System.currentTimeMillis();
    }

    public static void printFirstAnimateTime() {
        if (singleTapTime == -1) {
            return;
        }
        Log.v(THUMB_PLAY_DEBUG_TAG, "get animated time: " + (System.currentTimeMillis() - singleTapTime));
        singleTapTime = -1;
    }

    public static void printShareTime() {
        if (shareClickTime == -1) {
            return;
        }
        Log.v(THUMB_PLAY_DEBUG_TAG, "share time: " + (System.currentTimeMillis() - shareClickTime));
        shareClickTime = -1;
    }
}
