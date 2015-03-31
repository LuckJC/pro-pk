package com.mediatek.gallery3d.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Debug.MemoryInfo;
import android.os.storage.StorageManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import com.mediatek.gallery3d.ext.MtkLog;

import com.mediatek.storage.StorageManagerEx;
public class MtkUtils {
    private static final String TAG = "Gallery2/MtkUtils";
    private static final boolean LOG = true;
    
    private static final String PERFORMANCE_DEBUG = "ap.performance.debug";
    private static Boolean Debug = true;
    private static long TRACE_TAG = 0L;

    public static final String URI_FOR_SAVING = "UriForSaving";
    public static final String SUPPORT_CLEARMOTION = "SUPPORT_CLEARMOTION";
    
    private MtkUtils() {}
    
    public static void logMemory(String title) {
        MemoryInfo mi = new MemoryInfo();
        android.os.Debug.getMemoryInfo(mi);
        String tagtitle = "logMemory() " + title;
        MtkLog.v(TAG, tagtitle + "         PrivateDirty    Pss     SharedDirty");
        MtkLog.v(TAG, tagtitle + " dalvik: " + mi.dalvikPrivateDirty + ", " + mi.dalvikPss
                + ", " + mi.dalvikSharedDirty + ".");
        MtkLog.v(TAG, tagtitle + " native: " + mi.nativePrivateDirty + ", " + mi.nativePss
                + ", " + mi.nativeSharedDirty + ".");
        MtkLog.v(TAG, tagtitle + " other: " + mi.otherPrivateDirty + ", " + mi.otherPss
                + ", " + mi.otherSharedDirty + ".");
        MtkLog.v(TAG, tagtitle + " total: " + mi.getTotalPrivateDirty() + ", " + mi.getTotalPss()
                + ", " + mi.getTotalSharedDirty() + ".");
    }
    
    private static final String EXTRA_CAN_SHARE = "CanShare";
    public static boolean canShare(Bundle extra) {
        boolean canshare = true;
        if (extra != null) {
            canshare = extra.getBoolean(EXTRA_CAN_SHARE, true);
        }
        if (LOG) {
            MtkLog.v(TAG, "canShare(" + extra + ") return " + canshare);
        }
        return canshare;
    }

    private static StorageManager sStorageManager = null;
    public static File getMTKExternalCacheDir(Context context) {
        if (context == null) {
            return null;
        }
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        }
        File ret = StorageManagerEx.getExternalCacheDir(context.getPackageName());
        if (ret == null)
            return null;
        String internalStoragePath = StorageManagerEx.getInternalStoragePath();
        if (internalStoragePath == null)
            return null;
        String cachePath = ret.getAbsolutePath();
        cachePath = internalStoragePath
                + cachePath.substring(internalStoragePath.length(), cachePath.length());
        ret = new File(cachePath);
        if (ret.exists())
            return ret;
        if (ret.mkdirs())
            return ret;
        MtkLog.v(TAG, "<getMTKExternalCacheDir> can not create external cache dir");
        return null;
    }

    public static String getMtkDefaultPath() {
        String path = StorageManagerEx.getDefaultPath();
        /// M: too much log, not print 
        //if (LOG) {
        //    MtkLog.v(TAG, "getMtkDefaultPath() return " + path);
        //}
        return path;
    }
    
    public static String getMtkDefaultStorageState(Context context) {
        if (sStorageManager == null && context == null) {
            return null;
        }
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        }
        String path = StorageManagerEx.getDefaultPath();
        if (path == null) {
            return null;
        }
        String volumeState = sStorageManager.getVolumeState(path);
        if (LOG) {
            MtkLog.v(TAG, "getMtkDefaultStorageState: default path=" + path + ", state=" + volumeState);
        }
        return volumeState;
    }
    
    public static boolean isSupport3d() {
        boolean support = com.mediatek.common.featureoption.FeatureOption.MTK_S3D_SUPPORT;
        MtkLog.w(TAG, "isSupport3d() return " + support);
        return support;
    }    
    public static final int UNKNOWN = -1;

    private static final String GALLERY_ISSUE = "/.GalleryIssue/";
    public static final String BITMAP_DUMP_PATH = Environment.getExternalStorageDirectory().toString() + GALLERY_ISSUE;
    public static void dumpBitmap(Bitmap bitmap, String string) {
        String fileName = string + ".png";
        try {
            File galleryIssueFilePath = new File(BITMAP_DUMP_PATH);
            if (!galleryIssueFilePath.exists()) {
                Log.i(TAG, " create  galleryIssueFilePath");
                galleryIssueFilePath.mkdir();
            }
        } catch (Exception e) {
            Log.i(TAG, " create galleryIssueFilePath exception");
        }
        File file = new File(BITMAP_DUMP_PATH, fileName);
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "cannot create fos");
        }
        bitmap.compress(CompressFormat.PNG, 100, fos);
    }
    
    /**
     * Convert byteArry to Drawable
     * 
     * @param byteArry The picture in byte array.
     * @return A Drawable.
     */
    public static Drawable bytesToDrawable(byte[] byteArray) {
        Bitmap bitmap = null;
        Drawable drawable = null;
        int length = byteArray.length;
        if (length != 0) {
            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, length);
            drawable = new BitmapDrawable(bitmap);
        }
        Log.v(TAG, "bytesToDrawable() exit with the drawable is " + drawable);
        return drawable;
    }
    // //FOR MTK_SUBTITLE_SUPPORT
    // @{
    public static final String SUBTITLE_SUPPORT_WITH_SUFFIX_SRT = ".srt";
    public static final String SUBTITLE_SUPPORT_WITH_SUFFIX_MPL = ".mpl";
    public static final String SUBTITLE_SUPPORT_WITH_SUFFIX_SMI = ".smi";
    public static final String SUBTITLE_SUPPORT_WITH_SUFFIX_SUB = ".sub";
    public static final String SUBTITLE_SUPPORT_WITH_SUFFIX_IDX = ".idx";
    public static final String SUBTITLE_SUPPORT_WITH_SUFFIX_TXT = ".txt";
    public static final String SUBTITLE_SUPPORT_WITH_SUFFIX_SSA = ".ssa";
    public static final String SUBTITLE_SUPPORT_WITH_SUFFIX_ASS = ".ass";

    public static final String MEDIA_MIMETYPE_TEXT_SUBASS = "application/x-subtitle-ass";
    public static final String MEDIA_MIMETYPE_TEXT_SUBSSA = "application/x-subtitle-ssa";
    public static final String MEDIA_MIMETYPE_TEXT_SUBTXT = "application/x-subtitle-txt";
    public static final String MEDIA_MIMETYPE_TEXT_SUBMPL = "application/x-subtitle-mpl";
    public static final String MEDIA_MIMETYPE_TEXT_SUBSMI = "application/x-subtitle-smi";
    public static final String MEDIA_MIMETYPE_TEXT_SUB = "application/x-subtitle-sub";
    public static final String MEDIA_MIMETYPE_TEXT_IDX = "application/x-subtitle-idx";

    static private class ScriptFileNameFilter implements FilenameFilter {
        private String mNameToFilter;
		
        /**
         * construct 
         * @param nameGetToFilter
         */
        public ScriptFileNameFilter(String nameGetToFilter) {
	    this.mNameToFilter = nameGetToFilter;
        }
		
        /**
         * Override
         * @param dir
         * @param name
         * @return
         */
        public boolean accept(File dir, String name) {
            if (name.indexOf(mNameToFilter) != -1) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * list external subtitle files with the same name
     * 
     * @param VideoPath the path of a video
     * @return files array
     */
    public static File[] listTheSameNameOfVideo(String VideoPath) {
        if (null == VideoPath) {
            return null;
        }
        File dir = new File(VideoPath);
        if (!dir.exists()) {
            return null;
        }
        File parentFileDir = dir.getParentFile();
        if (parentFileDir == null) {
            return null;
        }
        String VideoName = dir.getName();
        MtkLog.i(TAG, "AudioAndSubtitle getExtSubTitleFileName parentFileDir ="
                 + parentFileDir.getName() + "  VideoName=" + VideoName);
        int index = VideoName.lastIndexOf(".");
        if (index == -1) {
            return null;
        }
        String nameToFiliter = VideoName.substring(0, index) + ".";
        File[] sameNameFiles = parentFileDir.listFiles(new ScriptFileNameFilter(nameToFiliter));
        if (LOG) {
            for (File file : sameNameFiles) {
                MtkLog.i(TAG,"AudioAndSubtitle getExtSubTitleFileName sameNameFiles.path ="							+ file.getPath());
            }
        }
        return sameNameFiles;
    }
    
    ///@}
    /**
     * Whether clear motion menu should be shown.
     * 
     * @return True if clear motion menu should be shown,false otherwise.
     */
    public static boolean isClearMotionSupport() {
        boolean isClearMotionSupport = false;
        String[] path =
                ((StorageManager) MediatekFeature.sContext
                        .getSystemService(Context.STORAGE_SERVICE)).getVolumePaths();
        int length = path.length;
        for (int i = 0; i < length; i++) {
            if (path != null) {
                File clearMotionFile = new File(path[i], SUPPORT_CLEARMOTION);
                if (clearMotionFile.exists()) {
                    Log.v(TAG,
                            "isClearMotionSupport() clearMotion file exists with clearMotionFile is "
                                    + clearMotionFile);
                    isClearMotionSupport = true;
                }
            }
        }
        Log.v(TAG, "isClearMotionSupport() exit with isClearMotionSupport is "
                + isClearMotionSupport);
        return isClearMotionSupport;
    }

    // The following methods are used for performance trace
    static {
        Debug = !SystemProperties.get("ap.performance.debug", "0").equals("0");
        if (Debug){
            TRACE_TAG = 1L << Long.parseLong(SystemProperties.get("ap.performance.debug"));
        }
    }

    public static void traceStart(String msg) {
        if (Debug) {
            Trace.traceCounter(TRACE_TAG, "P$" + msg, 1);
        }
    }

    public static void traceEnd(String msg) {
        if (Debug) {
            Trace.traceCounter(TRACE_TAG, "P$" + msg, 0);
        }
    }

    public static void traceEndAndStart(String msg1, String msg2) {
        if (Debug) {
            Trace.traceCounter(TRACE_TAG, "P$" + msg1, 0);
            Trace.traceCounter(TRACE_TAG, "P$" + msg2, 1);
        }
    }
}
