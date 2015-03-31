package com.mediatek.gallery3d.videothumbnail;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.android.gallery3d.data.LocalMediaItem;

import android.util.Log;

public final class VideoThumbnailMaker {
    private static final String TAG = "Gallery2/VideoThumbnailMaker";

    private VideoThumbnailMaker() {
        // do nothing
    }

    static synchronized void makeVideo(LocalMediaItem mediaItem, int videoType) {
        AbstractVideoGenerator videoGenerator = mediaItem.mVideoGenerator;
        
        if (videoGenerator.shouldCancel()) {
            videoGenerator.videoState[videoType]
                                      = AbstractVideoGenerator.STATE_NEED_GENERATE;
            return;
        }

        String filePath = mediaItem.filePath;
        String dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
        if (!VideoThumbnailHelper.isStorageSafeForGenerating(dirPath)) {
            Log.e(TAG, "storage available in this volume is not enough! stop generating");
            videoGenerator.videoState[videoType]
                                      = AbstractVideoGenerator.STATE_GENERATED_FAIL;
            return;
        }

        filePath = videoGenerator.videoPath[videoType];
        dirPath = filePath.substring(0, filePath.lastIndexOf('/'));
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdir()) {
            Log.e(TAG, "exception when creating cache container!");
            videoGenerator.videoState[videoType]
                                      = AbstractVideoGenerator.STATE_GENERATED_FAIL;
            return;
        }

        if (videoGenerator.shouldCancel()) {
            videoGenerator.videoState[videoType]
                                      = AbstractVideoGenerator.STATE_NEED_GENERATE;
            return;
        }

        int result = videoGenerator.generate(mediaItem, videoType);
        Log.v(TAG, "transcode result: " + result);

        if (result == AbstractVideoGenerator.GENERATE_CANCEL) {
            videoGenerator.videoState[videoType]
                                      = AbstractVideoGenerator.STATE_NEED_GENERATE;
            return;
        }

        File unCompleteDynThumb = VideoThumbnailHelper.getTempFileForMediaItem(
                mediaItem, videoType);
        boolean recrifiedResult = false;
        if (result == AbstractVideoGenerator.GENERATE_OK) {
            if (unCompleteDynThumb.exists()) {
                recrifiedResult = unCompleteDynThumb
                        .renameTo(new File(
                                videoGenerator.videoPath[videoType]));
            }
        }
        Log.v(TAG, "recrified transcode result: " + result);
        
        if (recrifiedResult) {
            videoGenerator.videoState[videoType]
                                                 = AbstractVideoGenerator.STATE_GENERATED;
            if (sDirector != null) {
                sDirector.pumpLiveThumbnails();
                sDirector.requestRender();
            }
            Log.v(TAG, "then request render: " + videoGenerator.videoPath[videoType]);
        } else {
            if (unCompleteDynThumb.exists()) {
                unCompleteDynThumb.delete();
            }
            videoGenerator.videoState[videoType]
                                                 = AbstractVideoGenerator.STATE_GENERATED_FAIL;
        }
    }

    // returns true if really need to generate dynamic thumbnail
    // and video.dynamicThumbnailPath contains the path of the thumbnail
    static boolean needGenDynThumb(LocalMediaItem mediaItem, int videoType) {
        AbstractVideoGenerator videoGenerator = mediaItem.mVideoGenerator;
        if (videoGenerator == null) {
            return false;
        }
        String inputPath = mediaItem.filePath;

        if ((mediaItem.isDrm()) || (inputPath == null)
                || (mediaItem.width == 0) || (mediaItem.height == 0)) {
            videoGenerator.videoState[videoType]
                                                 = AbstractVideoGenerator.STATE_GENERATED_FAIL;
            videoGenerator.videoPath[videoType] = null;
            return false;
        }

        String outputPath;
        // better use cache file to indicate if the dyn thumb has been generated
        outputPath = VideoThumbnailHelper
                .getVideoThumbnailPathFromOriginalFilePath(inputPath, videoType);
     File dynThumbFile = new File(outputPath);
        if (dynThumbFile.exists()) {
            videoGenerator.videoState[videoType]
                                                = AbstractVideoGenerator.STATE_GENERATED;
            videoGenerator.videoPath[videoType]
                                                = outputPath;
            return false;
        } else {
            videoGenerator.videoState[videoType]
                                                = AbstractVideoGenerator.STATE_GENERATING;
            videoGenerator.videoPath[videoType]
                                                = outputPath;
            return true;
        }
    }
    
    private static class VideoHandler extends Thread {
        // TODO: use blocking stack is more friendly, to be modified
        private final BlockingQueue<LocalMediaItem> mMediaItemQueue;
        private LocalMediaItem mCurrentItem;

        public VideoHandler(String threadName) {
            super("DynamicThumbnailRequestHandler-" + threadName);
            mMediaItemQueue = new LinkedBlockingQueue<LocalMediaItem>();
        }

        public void run() {
            try {
                LocalMediaItem currentItem;
                while(!Thread.currentThread().isInterrupted()) {
                    currentItem = mMediaItemQueue.take();                    
                    synchronized (this) {
                        mCurrentItem = currentItem;
                    }
                    Log.v(TAG, "handle transcoding request for " + mCurrentItem.filePath);
                    makeVideo(mCurrentItem, AbstractVideoGenerator.VTYPE_THUMB);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Terminating " + getName());
                this.interrupt();
            }
        }

        private void submit(LocalMediaItem mediaItem) {
            if (isAlive()) {
                if (needGenDynThumb(mediaItem, AbstractVideoGenerator.VTYPE_THUMB)) {
                    Log.v(TAG, "submit transcoding request for " + mediaItem.filePath);
                    mMediaItemQueue.add(mediaItem);
                }
            } else {
                Log.e(TAG, getName() + " should be started before submitting tasks.");
            }
        }

        private boolean cancelPendingTranscode(LocalMediaItem video) {
            video.mVideoGenerator.videoState[AbstractVideoGenerator.VTYPE_THUMB]
                                             = AbstractVideoGenerator.STATE_NEED_GENERATE;
            return mMediaItemQueue.remove(video);
        }
        
        private void cancelCurrentTranscode() {
            LocalMediaItem currentItem;
            synchronized (this) {
                currentItem = mCurrentItem;
            }
            if (currentItem != null) {
                AbstractVideoGenerator videoGenerator = currentItem.mVideoGenerator;
                videoGenerator.onCancelRequested(currentItem,
                        AbstractVideoGenerator.VTYPE_THUMB);
                if (videoGenerator.videoPath[AbstractVideoGenerator.VTYPE_THUMB] != null) {
                    File unCompleteDynThumb = VideoThumbnailHelper
                            .getTempFileForMediaItem(currentItem,
                                    AbstractVideoGenerator.VTYPE_THUMB);
                    if (unCompleteDynThumb.exists()) {
                        unCompleteDynThumb.delete();
                    }
                }
            }
        }

        private void cancelPendingTranscode() {
            // sHandler.interrupt();
            for (LocalMediaItem mediaItem : mMediaItemQueue) {
                mediaItem.mVideoGenerator.videoState[AbstractVideoGenerator.VTYPE_THUMB]
                                             = AbstractVideoGenerator.STATE_NEED_GENERATE;
            }
            mMediaItemQueue.clear();
        }

        private void cancelAllTranscode() {
            cancelPendingTranscode();
            cancelCurrentTranscode();
            sHandler.interrupt();
        }
    }
    
    private static volatile VideoHandler sHandler = null;

    public static void requestThumbnail(LocalMediaItem video) {
        if (sHandler != null) {
            sHandler.submit(video);
        }
    }
    
    private static VideoThumbnailDirector sDirector = null;
    public static void setDirector(VideoThumbnailDirector director) {
        sDirector = director;
    }
    
    public static void pause() {
        if (sHandler != null) {
            sHandler.cancelAllTranscode();
            sHandler = null;
        }
    }

    public static void cancelPendingTranscode() {
        if (sHandler != null) {
            sHandler.cancelPendingTranscode();
        }
    }

    public static void start() {
        if (sHandler == null) {
            sHandler = new VideoHandler("transcode proxy");
            sHandler.start();
        }
    }
}
