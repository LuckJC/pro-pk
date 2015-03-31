package com.mediatek.gallery3d.videothumbnail;

import java.io.File;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;

import android.content.Intent;
import android.net.Uri;
import android.os.StatFs;
import android.util.Log;

public class VideoThumbnailHelper {
    private static final String TAG = "Gallery2/VideoThumbnailUtils";
    private static final int MIN_STORAGE_SPACE = 3 * 1024 * 1024;
    private static final String[] DYNAMIC_CACHE_FILE_POSTFIX = {".dthumb", ".mp4"};
    private static final String SUFFIX_TMP = ".tmp";

    public static boolean isStorageSafeForGenerating(String dirPath) {
        try {
            StatFs stat  = new StatFs(dirPath);
            long spaceLeft = (long)(stat.getAvailableBlocks())*stat.getBlockSize();
            Log.v(TAG, "storage available in this volume is: " + spaceLeft);
            if (spaceLeft < MIN_STORAGE_SPACE) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return false;
        }
        return true;
    }

    // out file name looks like videoxxx.dthumb
    public static String getVideoThumbnailPathFromOriginalFilePath(
            String originalFilePath, int videoType) {
        StringBuilder res = null;
        int i = originalFilePath.lastIndexOf("/");
        if (i == -1) {
            res = new StringBuilder(".dthumb/").append(
                    originalFilePath.substring(i + 1).hashCode()).append(
                    DYNAMIC_CACHE_FILE_POSTFIX[videoType]);
        } else {
            // i-1 can be -1. risk?
            res = new StringBuilder(originalFilePath.substring(0, i + 1))
                    .append(".dthumb/").append(
                            originalFilePath.substring(i + 1).hashCode())
                    .append(DYNAMIC_CACHE_FILE_POSTFIX[videoType]);
        }
        return res.toString();
    }

    public static String getTempFilePathForMediaItem(
            LocalMediaItem mediaItem, int videoType) {
        return mediaItem.mVideoGenerator.videoPath[videoType] + SUFFIX_TMP;
    }

    public static File getTempFileForMediaItem(
            LocalMediaItem mediaItem, int videoType) {
        return new File(getTempFilePathForMediaItem(mediaItem, videoType));
    }

    public static File getThumbnailFileForMediaItem(LocalMediaItem mediaItem,
            int videoType) {
        return new File(getVideoThumbnailPathFromOriginalFilePath(
                mediaItem.filePath, videoType));
    }

    public static boolean deleteThumbnailFile(LocalMediaItem mediaItem, int videoType) {
        File f = getThumbnailFileForMediaItem(mediaItem, videoType);
        if (f.exists()) {
            return f.delete();
        }
        return false;
    }
    
    public static Uri getVideoShareUriFromMediaItem(LocalMediaItem mediaItem) {
        //TODO sharePath need redefining
        int videoType = AbstractVideoGenerator.VTYPE_SHARE;
        if (VideoThumbnailMaker.needGenDynThumb(mediaItem, videoType)) {
            VideoThumbnailMaker.makeVideo(mediaItem,
                    AbstractVideoGenerator.VTYPE_SHARE);
        }
        if (mediaItem.mVideoGenerator.videoState[videoType]
            == AbstractVideoGenerator.STATE_GENERATED_FAIL) {
            return null;
        }
        String sharePath = mediaItem.mVideoGenerator.videoPath[videoType];
        Uri shareUri = Uri.fromFile(new File(sharePath));
        return shareUri;
    }
    
    public static LocalMediaItem getVideoSharableImageFromIntent(Intent intent,
            AbstractGalleryActivity activity) {
        if (null == intent) {
            return null;
        }

        Uri uri = (Uri) intent.getExtra(Intent.EXTRA_STREAM);
        if (null == uri) {
            return null;
        }

        DataManager manager = activity.getDataManager();

        Path itemPath = manager.findPathByUri(uri, intent.getType());
        if (itemPath == null) {
            return null;
        }

        MediaObject mediaObject = manager.getMediaObject(itemPath);
        if (!(mediaObject instanceof LocalImage)) {
            return null;
        }

        LocalMediaItem item = (LocalImage) mediaObject;
        item.prepareThumbnailPlay();
        if (item.mVideoGenerator == null) {
            return null;
        }

        return item;
    }
}
