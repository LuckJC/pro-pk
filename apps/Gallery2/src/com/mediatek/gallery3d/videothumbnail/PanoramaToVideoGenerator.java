package com.mediatek.gallery3d.videothumbnail;

import java.io.FileDescriptor;
import java.io.FileInputStream;

import com.android.gallery3d.glrenderer.GLCanvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.glrenderer.RawTexture;
import com.mediatek.gallery3d.panorama.PanoramaConfig;
import com.mediatek.gallery3d.panorama.PanoramaDrawer;
import com.mediatek.gallery3d.panorama.PanoramaHelper;
import com.mediatek.gallery3d.util.BackgroundRenderer.BackgroundGLTask;
import com.mediatek.gallery3d.util.BackgroundRenderer;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public class PanoramaToVideoGenerator extends BitmapStreamToVideoGenerator {

    private static final String TAG = "Gallery2/PanoramaToVideoGenerator";
    private static final int MAX_FRAME_COUNT = 45;
    private Bitmap mBitmap;
    private PanoramaDrawer mPanoramaDrawer;
    private RawTexture mTexture;
    private int mFrameWidth;
    private int mFrameHeight;
    private float mFrameSkip;
    private float mFrameDegreeGap;

    private PanoramaFrameTask mTask;

    public void init(MediaItem item, int videoType, VideoConfig config) {
        assert (item != null && config != null);
        // decode bitmap
        int targetSize = videoType == VTYPE_THUMB 
                ? Params.MICRO_THUMBNAIL_TARGET_SIZE_PANORAMA
                : Params.THUMBNAIL_TARGET_SIZE_PANORAMA;
        mBitmap = PanoramaHelper.decodePanoramaBitmap(item.getFilePath(), targetSize);
        if (mBitmap == null) {
            MtkLog.i(TAG, "<init> mBitmap == null, decode fail");
            return;
        }
        mBitmap = BitmapUtils.rotateBitmap(mBitmap, item.getRotation(), true);
        mBitmap = PanoramaHelper.resizeBitmapToProperRatio(mBitmap, true);
        
        // prepare PanoramaConfig
        PanoramaConfig pconfig = null;
        if (videoType == VTYPE_THUMB) {
            config.frameWidth = VideoThumbnailFeatureOption.PANORAMA_THUMBNAILVIDEO_TARGETSIZE;
            config.frameHeight = (int) ((float) PanoramaHelper.getPanoramaScreenNailHeight()
                    * (float) config.frameWidth / (float) PanoramaHelper.getPanoramaScreenNailWidth());
            pconfig = new PanoramaConfig(mBitmap.getWidth(), mBitmap.getHeight(),
                    config.frameWidth, config.frameHeight,
                    PanoramaHelper.MICRO_THUMBNAIL_ANTIALIAS_SCALE);
            config.bitRate = VideoThumbnailFeatureOption.PANORAMA_THUMBNAILVIDEO_BITRATE;
            config.frameInterval = 1000/VideoThumbnailFeatureOption.PANORAMA_THUMBNAILVIDEO_FPS;
        } else {
            config.frameWidth = VideoThumbnailFeatureOption.PANORAMA_SHAREVIDEO_TARGETSIZE;
            config.frameHeight = (int) ((float) PanoramaHelper.getPanoramaScreenNailHeight()
                    * (float) config.frameWidth / (float) PanoramaHelper.getPanoramaScreenNailWidth());
            pconfig = new PanoramaConfig(mBitmap.getWidth(), mBitmap.getHeight(),
                    config.frameWidth, config.frameHeight,
                    PanoramaHelper.SHARE_VIDEO_ANTIALIAS_SCALE);
            config.bitRate = VideoThumbnailFeatureOption.PANORAMA_SHAREVIDEO_BITRATE;
            config.frameInterval = 1000/VideoThumbnailFeatureOption.PANORAMA_SHAREVIDEO_FPS;
        }
        config.frameWidth = pconfig.mCanvasWidth;
        config.frameHeight = pconfig.mCanvasHeight;

        // init context
        mPanoramaDrawer = new PanoramaDrawer(mBitmap, pconfig);
        mFrameWidth = config.frameWidth;
        mFrameHeight = config.frameHeight;
        mFrameDegreeGap = pconfig.mFrameDegreeGap;
        if (pconfig.mFrameTotalCount > MAX_FRAME_COUNT) {
            config.frameCount = MAX_FRAME_COUNT;
            mFrameSkip = (float)pconfig.mFrameTotalCount/(float)MAX_FRAME_COUNT;
            //if (mFrameSkip > 2) {
            //    config.frameInterval *= (mFrameSkip/2.f);
            //}
        } else {
            config.frameCount = pconfig.mFrameTotalCount;
            mFrameSkip = 1.f;
        }
        MtkLog.i(TAG, "<init> [" + config.frameWidth + "," + config.frameHeight
                + "] , frameCount = " + config.frameCount);
    }

    public void deInit(MediaItem item, int videoType) {
        if (mTexture != null) {
            mTexture.recycle();
            mTexture = null;
        }
        if (mPanoramaDrawer != null) {
            mPanoramaDrawer.freeResources();
        }
    };

    public Bitmap getBitmapAtFrame(MediaItem item, int videoType, int frameIndex) {
        frameIndex = (int)(frameIndex * mFrameSkip);
        mTask = new PanoramaFrameTask(frameIndex);
        BackgroundRenderer.getInstance().addGLTask(mTask);
        BackgroundRenderer.getInstance().requestRender();
        synchronized (mTask) {
            while (!mTask.isDone()) {
                try {
                    mTask.wait();
                } catch (InterruptedException e) {
                    MtkLog.i(TAG, "<getBitmapAtFrame> InterruptedException: " + e.getMessage());
                }
            }
        }
        Bitmap bitmap = mTask.get();
        MtkLog.i(TAG, "<getBitmapAtFrame> item = " + item.getName() + ", frameIndex = "
                + frameIndex + ", bitmap = " + bitmap);
        // MtkUtils.dumpBitmap(bitmap, "frame-"+frameIndex);
        mTask = null;
        return bitmap;
    };

    public void onCancelRequested(LocalMediaItem item, int videoType) {
        if (mTask != null) {
            synchronized (mTask) {
                mTask.notifyAll();
            }
        }
        deInit(item, videoType);
    };

    class PanoramaFrameTask implements BackgroundGLTask {
        private int mFrameIndex;
        private Bitmap mFrame;
        private boolean mDone = false;

        public PanoramaFrameTask(int frameIndex) {
            mFrameIndex = frameIndex;
        }

        @Override
        public boolean run(GLCanvas canvas) {
            try {
                if (mPanoramaDrawer == null) {
                    mFrame = null;
                    mDone = true;
                    MtkLog.i(TAG, "<PanoramaFrameTask.run> mPanoramaDrawer == null, return");
                }
                if (mTexture == null) {
                    mTexture = new RawTexture(mFrameWidth, mFrameHeight, false);
                }
                mFrame = mPanoramaDrawer.drawOnBitmap(canvas, mTexture, mFrameIndex
                        * mFrameDegreeGap);
                mDone = true;
            } catch (Exception e) {
                mFrame = null;
                mDone = true;
                MtkLog.i(TAG, "<PanoramaFrameTask.run> exception occur, " + e.getMessage());
            }

            synchronized (PanoramaFrameTask.this) {
                PanoramaFrameTask.this.notifyAll();
            }
            return false;
        }

        public Bitmap get() {
            return mFrame;
        }

        public boolean isDone() {
            return mDone;
        }
    }
}
