package com.android.videoeditor.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.android.videoeditor.R;
import com.android.videoeditor.StorageBroadcastReceiver;

import com.mediatek.storage.StorageManagerEx;

public class MtkUtils {
    private static final String TAG = "MtkUtils";
    private static final boolean LOG = true;
    private static final String DATA_SCHEME_FILE = "file";
    
    public static File getExternalFilesDir(Context context) {
        File ret = null;
        try {
            ensureStorageManager(context);
            //let framework to create related folder and .nomedia file
            ret = StorageManagerEx.getExternalCacheDir(context.getPackageName());
            if (ret != null) {
                ret = new File(ret.getParent() + "/files");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret = context.getExternalFilesDir(null);//default impl
        }
        if (LOG) MtkLog.v(TAG, "getExternalFilesDir() return " + (ret == null ? "null" : ret.getAbsolutePath())); 
        return ret;
    }
    private static final long LIMITED_SPACE_SIZE_FOR_EXPORT = 100 * 1024 * 1024;//100MB
    private static final long LIMITED_SPACE_SIZE_FOR_TRANSITION = 10 * 1024 * 1024;//10MB
    private static final long LIMITED_SPACE_SIZE = 10 * 1024;//10k 
    private static final long LIMITED_SPACE_SIZE_FOR_KENBURNS = 10 * 1024 * 1024; // 10MB
    public static String appendStorageSpaceInfo(Context context, String text) {
        if (!isLeftEnoughSpace(context)) {
            if (text == null || "".equals(text)) {
                text = context.getResources().getString(getNoEnoughSpaceResId(context));
            } else {
                text += "\n" + context.getResources().getString(getNoEnoughSpaceResId(context));
            }
        }
        return text;
    }
 
    // Return true if current space > LIMITED_SPACE_SIZE_FOR_TRANSITION(10MB)
    public static boolean isLeftEnoughSpaceForTransition(Context context) {
        return isLeftEnoughSpace(context, LIMITED_SPACE_SIZE_FOR_TRANSITION);
    }

    // Return true if current space > LIMITED_SPACE_SIZE_FOR_EXPORT(100MB)
    public static boolean isLeftEnoughSpaceForExport(Context context) {
        return isLeftEnoughSpace(context, LIMITED_SPACE_SIZE_FOR_EXPORT);
    }

    // Return true if current space > LIMITED_SPACE_SIZE_FOR_KENBURNS(1MB)
    public static boolean isLeftEnoughSpaceForKenburns(Context context) {
        return isLeftEnoughSpace(context, LIMITED_SPACE_SIZE_FOR_KENBURNS);
    }

    public static boolean isLeftEnoughSpace(Context context) {
        return isLeftEnoughSpace(context, LIMITED_SPACE_SIZE);
    }

    public static boolean isLeftEnoughSpace(Context context, long blocks) {
        boolean enough = true;
        long left = -1;
        try {
            File file = getExternalFilesDir(context);
            if (file != null) {
                String path = file.getPath();
                StatFs stat = new StatFs(path);
                left = stat.getAvailableBlocks() * (long) stat.getBlockSize();
            }
            if (left <= blocks) {
                enough = false;
            }
        } catch (Exception e) {
            MtkLog.w(TAG, "Fail to access external storage", e);
        }
        MtkLog.v(TAG, "isLeftEnoughSpace(" + blocks + ") left=" + left + ", return " + enough);
        return enough;
    }

    public static int getNoEnoughSpaceResId(Context context) {
        int resId = 0;
        if (isMultiStorage(context)) {
            if(isRemoveableStorage(context)){// EMMC only
                resId = com.mediatek.internal.R.string.storage_sd;
            } else if(haveRemoveableStorage(context)){
                resId = com.mediatek.internal.R.string.storage_withsd;
            } else {
                resId = com.mediatek.internal.R.string.storage_withoutsd;
            }
        } else {
            resId = R.string.not_enough_space;
        }
        return resId;
    }
    
    private static StorageManager sStorageManager;
    private static void ensureStorageManager(Context context) {
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        }
    }
    
    public static boolean isRemoveableStorage(Context context){
        ensureStorageManager(context);
        boolean removeable = false;
        if (sStorageManager != null) {
            String storagePath = StorageManagerEx.getDefaultPath();
            StorageVolume[] volumes = sStorageManager.getVolumeList();
            if (volumes != null) {
                for(int i = 0, len = volumes.length; i < len; i++){
                    StorageVolume volume = volumes[i];
                    if(volume != null && volume.getPath().equals(storagePath)) {
                        removeable = volumes[i].isRemovable();
                        break;
                    }
                }
            }
        }
        if (LOG) MtkLog.v(TAG, "RemoveableStorage() return " + removeable);
        return removeable;
    }

    public static boolean isMultiStorage(Context context){
        ensureStorageManager(context);
        boolean ismulti = false;
        if (sStorageManager != null) {
            StorageVolume[] volumes = sStorageManager.getVolumeList();
            if (volumes != null) {
                ismulti = volumes.length > 1;
            }
        }
        if (LOG) MtkLog.v(TAG, "isMultiStorage() return " + ismulti);
        return ismulti;
    }

    public static boolean haveRemoveableStorage(Context context){
        ensureStorageManager(context);
        boolean have = false;
        if (sStorageManager != null) {
            StorageVolume[] volumes = sStorageManager.getVolumeList();
            if (volumes != null) {
                for(int i = 0, len = volumes.length; i < len; i++){
                    StorageVolume volume = volumes[i];
                    if(volume.isRemovable()
                            && Environment.MEDIA_MOUNTED.equals(sStorageManager.getVolumeState(volumes[i].getPath()))) {
                        have = true;
                    }
                }
            }
        }
        if (LOG) MtkLog.v(TAG, "haveRemoveableStorage() return " + have);
        return have;
    }
    
    /* M: copied from packages/providers/MediaProvider/.../MediaProvider.java
     * MediaProvider will keep its original behavior,
     * here we let VideoEditor's saving folder changed according to current primary storage path.
     * JB: jelly bean just modify video's save path, so here we keep image's behavior.
     * @{
    */
    public static final String IMAGE_PREFERRED_EXTENSION = ".jpg";
    public static final String VIDEO_PREFERRED_EXTENSION =  ".3gp";
    public static final String IMAGE_DIRECTORY_NAME = "DCIM/Camera";
    public static final String VIDEO_DIRECTORY_NAME = "video"; 
    
    public static void ensureFile(final ContentValues initialValues, String preferredExtension, String directoryName) {
        String file = initialValues.getAsString(MediaStore.MediaColumns.DATA);
        if (TextUtils.isEmpty(file)) {
            file = generateFileName(preferredExtension, directoryName);
            initialValues.put(MediaStore.MediaColumns.DATA, file);
        }

        if ((file == null) || !ensureFileExists(file)) {
            throw new IllegalStateException("Unable to create new file: " + file);
        }
    }
    
    private static String generateFileName(String preferredExtension, String directoryName) {
        String filePath = null;
        // create a random file
        String name = String.valueOf(System.currentTimeMillis());
        String storagePath = StorageManagerEx.getDefaultPath();
        filePath = storagePath + "/" + directoryName + "/" + name + preferredExtension;
        if (LOG) MtkLog.v(TAG, "generateFileName() return " + filePath);
        return filePath;
    }
    
    private static boolean ensureFileExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        } else {
            // we will not attempt to create the first directory in the path
            // (for example, do not create /sdcard if the SD card is not mounted)
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash < 1) return false;
            String directoryPath = path.substring(0, secondSlash);
            File directory = new File(directoryPath);
            if (!directory.exists())
                return false;
            file.getParentFile().mkdirs();
            try {
                return file.createNewFile();
            } catch(IOException ioe) {
                MtkLog.e(TAG, "File creation failed", ioe);
            }
            return false;
        }
    }
    /// @}

    /// M: support switching sdcard for video @{
    private static final String DCIM = 
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    private static final String DIRECTORY = DCIM + "/Camera";
    
    public static String getVideoOutputMediaFilePath() {
        String videopath = null;
        if (sStorageManager != null) {
            videopath = StorageManagerEx.getDefaultPath() + "/" + Environment.DIRECTORY_DCIM + "/Camera";
        } else {
            videopath = DIRECTORY + "/" + Environment.DIRECTORY_DCIM + "/Camera";
        }
        if (LOG) MtkLog.v(TAG, "getVideoOutputMediaFilePath() return " + videopath);
        return videopath;
    }
    /// @}

    public static String getMovieExportPath() {
        String exportPath = StorageManagerEx.getDefaultPath() + "/" + Environment.DIRECTORY_MOVIES;
        if (LOG) MtkLog.v(TAG, "getMovieExportPath() return " + exportPath);
        return exportPath;
    }
    
    public static void registerStorageListener(Context context, StorageBroadcastReceiver receiver) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        // filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme(DATA_SCHEME_FILE);
        context.registerReceiver(receiver, filter);
    }
    
    public static void unregisterStorageListener(Context context, StorageBroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
    }
    
    /// M: support switching sdcard for video @{
    public static boolean isProjectCorrect(File projectDir) {
        MtkLog.d(TAG, "isProjectCorrect() file dir path:" + projectDir.getAbsolutePath());
        File[] projectFile = projectDir.listFiles();
        if (projectFile.length == 0) {
            MtkLog.e(TAG, "isProjectCorrect: no file found return false");
            return false;
        }
        for (File file : projectFile) {
            MtkLog.d(TAG, "isProjectCorrect: file name:" + file.getName() + ",lenth:" + file.length());
            if (".nomedia".equals(file.getName())) {
                MtkLog.d(TAG, "isProjectCorrect: it is .nomedia continue");
                continue;
            }
            if (0 == file.length()) {
                if ("videoeditor.xml".equalsIgnoreCase(file.getName()) ||
                        "metadata.xml".equalsIgnoreCase(file.getName()) ||
                        "thumbnail.jpg".equalsIgnoreCase(file.getName())) {
                    MtkLog.e(TAG, "isProjectCorrect: file length is 0:" + file.getName());
                    return false;
                }
            }
        }
        return true;
    }
    /// @}
    
    /**
     * Check the given uri whether exist in device.
     * 
     * @param context the context.
     * @param uri the given uri.
     * @return if exist return true, otherwise false.
     */
    public static boolean isFileExist(Context context, Uri uri) {
        if (uri == null) {
            MtkLog.e(TAG, "isFileExist: Check file exist with null uri!");
            return false;
        }
        boolean exist = false;
        try {
            AssetFileDescriptor fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            if (fd == null) {
                exist = false;
            } else {
                fd.close();
                exist = true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            exist = false;
        } catch (IOException e) {
            e.printStackTrace();
            exist = true;
        }
        MtkLog.d(TAG, "isFileExist: " + uri + " is exist " + exist);
        return exist;
    }
}
