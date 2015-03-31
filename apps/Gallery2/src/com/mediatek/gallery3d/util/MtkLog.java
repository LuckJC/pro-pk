package com.mediatek.gallery3d.util;

import android.os.Environment;
import android.os.SystemProperties;

import com.mediatek.xlog.Xlog;

import java.io.File;

/**
 * Adapter for log system.
 */
public final class MtkLog {
    private static final String TAG = "Gallery2/MtkLog";
    // on/off switch to control large amount of logs
    public static final boolean DBG;
    public static final boolean SUPPORT_PQ;
    public static final boolean SUPPORT_PQ_ADV;
    public static final boolean DBG_TILE;
    public static final boolean DBG_DUMP_TILE;
    public static final boolean DBG_PERFORMANCE;
    public static final boolean DBG_LAUNCH_TIME;
    public static final boolean DBG_AC;

    static {
        File pqCfg = new File(Environment.getExternalStorageDirectory(), "SUPPORT_PQ");
        if (pqCfg.exists()) {
            SUPPORT_PQ = true;
        } else {
            SUPPORT_PQ = false;
        }
        File pqADVModeCfg = new File(Environment.getExternalStorageDirectory(), "SUPPORT_PQ_ADV");
        if (pqADVModeCfg.exists()) {
            SUPPORT_PQ_ADV = true;
        } else {
            SUPPORT_PQ_ADV = false;
        }
        Xlog.v("MtkLog", "Gallery2 support PQ " + (SUPPORT_PQ ? "ON" : "OFF")
                + " Gallery2 support PQ" + (SUPPORT_PQ_ADV ? "ON" : "OFF"));

        DBG = SystemProperties.getInt("Gallery_DBG", 0) == 1 ? true : false;
        DBG_TILE = SystemProperties.getInt("Gallery_DBG_TILE", 0) == 1 ? true : false;
        DBG_DUMP_TILE = SystemProperties.getInt("Gallery_DBG_DUMP_TILE", 0) == 1 ? true : false;
        DBG_PERFORMANCE = SystemProperties.getInt("Gallery_DBG_PERFORMANCE", 0) == 1 ? true : false;
        DBG_LAUNCH_TIME = SystemProperties.getInt("Gallery_DBG_LAUNCH_TIME", 0) == 1 ? true : false;
        DBG_AC = SystemProperties.getInt("Gallery_DBG_AC", 0) == 1 ? true : false;
        Xlog.i(TAG, "DBG = " + DBG);
        Xlog.i(TAG, "SUPPORT_PQ = " + SUPPORT_PQ);
        Xlog.i(TAG, "SUPPORT_PQ_ADV = " + SUPPORT_PQ_ADV);
        Xlog.i(TAG, "DBG_TILE = " + DBG_TILE);
        Xlog.i(TAG, "DBG_PERFORMANCE = " + DBG_PERFORMANCE);
    }
    
    private MtkLog() {
    }

    public static int v(String tag, String msg) {
        return Xlog.v(tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return Xlog.v(tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        return Xlog.d(tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return Xlog.d(tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        return Xlog.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return Xlog.i(tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        return Xlog.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return Xlog.w(tag, msg, tr);
    }

    public static int e(String tag, String msg) {
        return Xlog.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return Xlog.e(tag, msg, tr);
    }
    
    public static boolean isDebugTile() {
        return DBG_TILE;
    }
    
}
