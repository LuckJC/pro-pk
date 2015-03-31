package com.mediatek.gallery3d.videothumbnail;

import java.io.FileDescriptor;
import java.io.FileInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.mediatek.gallery3d.panorama.PanoramaConfig;
import com.mediatek.gallery3d.panorama.PanoramaHelper;
import com.mediatek.gallery3d.panorama.PanoramaHelper.PanoramaMicroThumbnailEntry;
import com.mediatek.gallery3d.panorama.PanoramaScreenNail;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MediatekFeature.Params;
import com.mediatek.gallery3d.videothumbnail.AbstractVideoPlayer;

public class PanoramaVideoPlayer extends AbstractVideoPlayer {
    private static final String TAG = "Gallery2/PanoramaVideoPlayer";
    private static long mLastRequestRenderTime;

    private PanoramaScreenNail mScreenNail;
    private Handler mHandler;

    private int mCurrentFrame;
    private float mCurrentDegree;
    private int mFrameCount;
    private int mFrameTimeGap;
    private float mFrameDegreeGap;
    private boolean mPanoramaForward = true;
    private boolean mFirstRender = true;

    public boolean prepare() {
        MtkLog.i(TAG, "<prepare>");
        PanoramaMicroThumbnailEntry entry = PanoramaHelper.getMicroThumbnailEntry(mItem);
        if (entry == null) {
            MtkLog.i(TAG, "<prepare> entry == null, return false");
            return false;
        }
        mScreenNail = new PanoramaScreenNail(entry.mBitmap, entry.mConfig);
        mFrameCount = entry.mConfig.mFrameTotalCount;
        mFrameTimeGap = entry.mConfig.mFrameTimeGap;
        mFrameDegreeGap = entry.mConfig.mFrameDegreeGap;
        mCurrentFrame = 0;

        mHandler = new MyHandler(mGalleryActivity.getAndroidContext().getMainLooper());

        return true;
    }

    class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            autoPanoramaPlayback();
        }
    }

    public void release() {
        MtkLog.i(TAG, "<release>");
        if (mScreenNail != null) {
            mScreenNail.recycle();
            mScreenNail = null;
        }
    }

    public boolean start() {
        MtkLog.i(TAG, "<start>");
        autoPanoramaPlayback();
        return true;
    }

    public boolean pause() {
        return false;
    }

    public boolean stop() {
        MtkLog.i(TAG, "<stop>");
        stopPanoramaPlayback();
        return true;
    }

    public boolean render(GLCanvas canvas, int width, int height) {
        float newHeight = (float) width / (float) mScreenNail.getWidth()
                * (float) mScreenNail.getHeight();
        float newY = (height - newHeight) / 2.f;
        mScreenNail.draw(canvas, 0, (int) newY, width, (int) newHeight, mCurrentDegree);
        return true;
    };

    private void autoPanoramaPlayback() {
        if (mPanoramaForward) {
            mCurrentFrame++;
            if (mCurrentFrame >= mFrameCount) {
                mCurrentFrame -= 2;
                mPanoramaForward = false;
            }
        } else {
            mCurrentFrame--;
            if (mCurrentFrame < 0) {
                mCurrentFrame += 2;
                mPanoramaForward = true;
            }
        }
        mCurrentDegree = mCurrentFrame * mFrameDegreeGap;
        // if request render before mFrameTimeGap, we request render again
        long now = System.currentTimeMillis();
        if (now - mLastRequestRenderTime > mFrameTimeGap) {
            requestRender();
            mLastRequestRenderTime = now;
        }
        if (mHandler != null) {
            mHandler.sendEmptyMessageDelayed(0, mFrameTimeGap);
        }
    }

    private void stopPanoramaPlayback() {
        if (mHandler != null) {
            mHandler.removeMessages(0);
        }
    }
}