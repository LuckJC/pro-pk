package com.mediatek.gallery3d.videothumbnail;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.StatFs;
import android.util.Log;

import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.LocalVideo;
import com.android.gallery3d.ui.GLRootView;

import com.mediatek.transcode.VideoTranscode;

public class VideoToVideoGenerator extends AbstractVideoGenerator {
    private static final String TAG = "Gallery2/VideoToVideoGenerator";

    private static final int TRANSCODING_BIT_RATE = 256 * 1024;
    private static final int TRANSCODING_FRAME_RATE = 10;

    private static final int ENCODE_WIDTH = 320;
    private static final int ENCODE_HEIGHT = 240;
    private static final int MAX_THUMBNAIL_DURATION = 8 * 1000; // 8 seconds at present

    private static AtomicLong sCurrentHandle = new AtomicLong(-1);

    private static Rect getTargetRect(int srcWidth, int srcHeight, int maxWidth, int maxHeight) {
        if ((srcWidth <= maxWidth) || (srcHeight <= maxHeight)) {
            return new Rect(0, 0, srcWidth, srcHeight);
        }

        float rSrc = (float) srcWidth / srcHeight;
        float rMax = (float) maxWidth / maxHeight;

        int targetWidth;
        int targetHeight;

        // crop and scale
        if (rSrc < rMax) {
            targetWidth = maxWidth;
            targetHeight = targetWidth * srcHeight / srcWidth;
        } else {
            targetHeight = maxHeight;
            targetWidth = targetHeight * srcWidth / srcHeight;
            // width must be the factor of 16, find closest but smallest factor
            if (targetWidth % 16 != 0) {
                targetWidth = (targetWidth - 15) >> 4 << 4;
                targetHeight = targetWidth * srcHeight / srcWidth;
            }
        }

        return new Rect(0, 0, targetWidth, targetHeight);
    }

    public int generate(LocalMediaItem item, int videoType) {
        if (GLRootView.DThumbClean) {
            return GENERATE_CANCEL;
        }
        LocalVideo video = (LocalVideo)item;
//        Rect srcRect = new Rect(0, 0, video.width, video.height);
        // the width and height stored in MediaStore may be reversed
        // here we use MediaMetadataRetriever instead to get the real width and height
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int videoWidth;
        int videoHeight;
        try {
            Log.v(TAG, "doTranscode: set retriever.setDataSource begin");
            retriever.setDataSource(video.filePath);
            Log.v(TAG, "doTranscode: set retriever.setDataSource end");
            videoWidth = Integer
                    .parseInt(retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            videoHeight = Integer
                    .parseInt(retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        } catch (Exception e) {
            videoWidth = video.width;
            videoHeight = video.height;
        }
        retriever.release();
        retriever = null;
        Rect srcRect = new Rect(0, 0, videoWidth, videoHeight);
        Rect targetRect = getTargetRect(srcRect.width(), srcRect.height(), ENCODE_WIDTH,
                                        ENCODE_HEIGHT);
        Log.v(TAG, "srcRect: " + srcRect + " targetRect: " + targetRect);
        // duration is not so accurate as gotten from meta retriever,
        // but it's already enough (however, I don't know why the googlers don't save the
        // accurate duration with ms to be the unit)
        long duration = video.durationInSec * 1000;
        long startTime = duration / 3;  // eh, magic number?
        long endTime = Math.min(duration, startTime + MAX_THUMBNAIL_DURATION);
        startTime = Math.max(0, endTime - MAX_THUMBNAIL_DURATION);

        if (shouldCancel()) {
            return GENERATE_CANCEL;
        }

        String dfilepath = video.filePath;
        String tpath = video.mVideoGenerator.videoPath[videoType];//video.dynamicThumbnailPath;

        long width = (long) targetRect.width();
        long height = (long) targetRect.height();
        
        Log.v(TAG, "start transcoding: " + dfilepath + " to " + tpath + ", target width = " + width + ", target height = " + height);
        Log.v(TAG, "starttime = " + startTime + ", endtime = " + endTime);
        sCurrentHandle.set(VideoTranscode.init());
        long transcodeId = sCurrentHandle.get();
        int result = VideoTranscode.transcodeAdv(transcodeId,
                video.filePath, VideoThumbnailHelper.getTempFilePathForMediaItem(item,
                        videoType), (long) targetRect.width(),
                (long) targetRect.height(), startTime, endTime,
                TRANSCODING_BIT_RATE, TRANSCODING_FRAME_RATE);
        if (result == VideoTranscode.NO_ERROR) {
            result = GENERATE_OK;
        } else {
            result = GENERATE_ERROR;
        }
        Log.v(TAG, "end transcoding: " + dfilepath + " to " + tpath);
        sCurrentHandle.set(-1);
        VideoTranscode.deinit(transcodeId);

        if (shouldCancel()) {
            return GENERATE_CANCEL;
        }

        return result;
    }

    public void onCancelRequested(LocalMediaItem item, int videoType) {
        if (sCurrentHandle.get() != -1) {
            VideoTranscode.cancel(sCurrentHandle.get());
        }
    }
}
