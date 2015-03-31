package com.mediatek.gallery3d.mpo;

import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Process;
import android.os.SystemClock;
import android.view.Display;
import android.view.Surface;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AbstractGalleryActivity.GyroPositionListener;
import com.android.gallery3d.data.ChangeNotifier;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.ui.AlbumSlotRenderer;


import com.android.gallery3d.ui.AlbumSlidingWindow.AlbumEntry;
import com.android.gallery3d.util.Log;
import com.mediatek.gallery3d.data.RequestHelper;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;
import com.mediatek.gallery3d.videothumbnail.AbstractVideoPlayer;

public class MavPlayer extends AbstractVideoPlayer implements GyroPositionListener, MavRenderThread.OnDrawMavFrameListener{
    private String TAG = "Gallery2/MavPlayer";
    private Params mParams;
    private Bitmap[] mMavBitmap;
    public BitmapTexture[] mMavcontent;
    private int mCount;
    private int mOrientation = -1;
    private float mValue = 0;
    private long timestamp = 0;
    private float angle[] = {0,0,0};
    private boolean mFirstTime = true;
    private static int mLastIndex = 0xFFFF;
    private Texture mContent = null;
    private static final int MAV_THUMBNAIL_SIZE = 256;
    private static boolean hasAddToGyroListener = false; 
    private static HashMap<MavPlayer, Object> mAllMavPlayer =
        new HashMap<MavPlayer, Object>();
    private static MavRenderThread mMavRenderThread ;

    @Override
    public boolean pause() {
        return false;
    }

    @Override
    public boolean stop() {
        if(hasAddToGyroListener) {
            hasAddToGyroListener = false;
            mGalleryActivity.removeGyroPositionListener(this);
        }
        synchronized(mAllMavPlayer) {
            Log.d(TAG,"stop  remove  path="+mItem.getFilePath());
            mAllMavPlayer.remove(this);
            if (mAllMavPlayer.size() == 0 && mMavRenderThread != null) {
                mMavRenderThread.setActive(false);
                mMavRenderThread.interrupt();
                mMavRenderThread = null;
            }
        }
        return false;
    }

    public void addTexture(int i) {
        Bitmap mBitmap = mMavBitmap[i];
        if (i >= 0 && i < mCount && mBitmap!= null) {
            BitmapTexture bitmapTexture = new BitmapTexture(mBitmap);
            if (bitmapTexture != null) {
                mMavcontent[i] = bitmapTexture;
            } else {
                Log.d(TAG,"bitmapTexture ====== null");
            }
        }
    }

    @Override
    public boolean prepare() {
        if (!mGalleryActivity.hasGyroSensor()) {
            Log.d(TAG,"hasGyroSensor==== false");
            return false;
        }
        mParams = new Params();
        mParams.inMpoTotalCount = true;
        mParams.inMpoFrames = true;//we want mpo frames
        mParams.inOriginalFrame = true;
        mParams.inType = MediaItem.TYPE_MICROTHUMBNAIL;
        mParams.inOriginalTargetSize = MediaItem.getTargetSize(MediaItem.TYPE_MICROTHUMBNAIL) > MAV_THUMBNAIL_SIZE 
                ? MAV_THUMBNAIL_SIZE : MediaItem.getTargetSize(MediaItem.TYPE_MICROTHUMBNAIL);
        DataBundle mDataBundle = RequestHelper.requestDataBundle(null, mParams, this.mPath, MpoHelper.MIME_TYPE, null);
        if (mDataBundle != null && mDataBundle.mpoFrames != null) {
            mMavBitmap = mDataBundle.mpoFrames;
            Log.d(TAG, "mMavBitmap[i].getWidth===="+mMavBitmap[0].getWidth()+ "    "+mMavBitmap[0].getHeight());
            mCount = mMavBitmap.length;
            mMavcontent = new BitmapTexture[mCount];
            for (int i = 0; i < mCount; i++) {
                addTexture(i);
            }
        } else {
            Log.d(TAG,"mBitmap============ null");
            return false;
        }
        return true;
    }

    @Override
    public void release() {
        // TODO Auto-generated method stub
        if (mMavBitmap != null) {
            int length = mMavBitmap.length;
            for (int i = 0; i < length ; i++) {
                Bitmap mBitmap = mMavBitmap[i];
                if (mBitmap != null) {
                    mBitmap.recycle();
                }
                BitmapTexture mTexture = mMavcontent[i];
                if (mTexture != null) {
                    mTexture.recycle();
                }
            }
            mContent = null;
            mMavBitmap = null;
        }
        mMavcontent = null;
    }

    @Override
    public boolean render(GLCanvas canvas, int width, int height) {
        // TODO Auto-generated method stub
        Texture content = mContent;
        if (content != null && canvas != null) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            width = height = Math.min(width, height);
            int rotation = (mItem == null) ? 0: mItem.getRotation();
            if (rotation != 0) {
                canvas.translate(width / 2, height / 2);
                canvas.rotate(rotation, 0, 0, 1);
                canvas.translate(-width / 2, -height / 2);
            }
            float scale = Math.min(
                    (float) width / content.getWidth(),
                    (float) height / content.getHeight());
            canvas.scale(scale, scale, 1);
            content.draw(canvas, 0, 0);
            canvas.restore();
            return true;
        } else {
            Log.d(TAG, "content or canvas is null");
            return false;
        }
    }

    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        synchronized(mAllMavPlayer) {
            Log.d(TAG,"start  add  path="+mItem.getFilePath());
            mAllMavPlayer.put(this, null);
            if (mAllMavPlayer.size() >= 1 && mMavRenderThread == null) {
                mMavRenderThread = new MavRenderThread(mGalleryActivity);
                mMavRenderThread.setActive(true);
                mMavRenderThread.setOnDrawMavFrameListener(this);
                mMavRenderThread.setRenderRequester(false);
                mMavRenderThread.start();
            }
        }
        if (!hasAddToGyroListener) {
            mGalleryActivity.setGyroPositionListener(this);
            hasAddToGyroListener = true;
        }
        return true;
    }

    public void onGyroPositionChanged(float angle) {
            if (mCount != 0) {
                int index = (int)(angle * mCount / (2 * MpoHelper.BASE_ANGLE));
                if (index >= 0 && index < mCount) {
                    if (mLastIndex == 0xFFFF || mLastIndex != index) {
                        mLastIndex = index;
                        refresh(mLastIndex);
                    }
                }
            }
    }

    private boolean requester(MavPlayer player ,int index) {
        if (index >= 0 && index < player.mCount 
                && player.mMavcontent != null && player.mMavcontent[index] != null) {
            BitmapTexture tile = player.mMavcontent[index];
            if (tile != null) {
                Log.d(TAG,"mLastIndex==="+mLastIndex+" index====="+index);
                player.mContent = tile;
                return true;
            }
        } else {
            Log.d(TAG, "  requester!!!!!!");
        }
        return false;
    }
    
    public float onCalculateAngle(SensorEvent event) {
        // TODO Auto-generated method stub
        // TODO Auto-generated method stub
        //workaround for Gyro sensor HW limitation.
        //As sensor continues to report small movement, wrongly
        //indicating that the phone is slowly moving, we should
        //filter the small movement.
        final float xSmallRotateTH = 0.05f;
        //xSmallRotateTH indicating the threshold of max "small
        //rotation". This varible is determined by experiments
        //based on MT6575 platform. May be adjusted on other chips.
        
        float valueToUse = 0;
        int newRotation = mGalleryActivity.getDisplay().getRotation();
        if (mOrientation != newRotation) {
            // orientation has changed, reset calculations
            mOrientation = newRotation;
            mValue = 0;
            angle[0] = 0;
            angle[1] = 0;
            angle[2] = 0;
            mFirstTime = true;
        }
        switch (mOrientation) {
        case Surface.ROTATION_0:
            valueToUse = event.values[1];
            break;
        case Surface.ROTATION_90:
            // no need to re-map
            valueToUse = event.values[0];
            break;
        case Surface.ROTATION_180:
            // we do not have this rotation on our device
            valueToUse = -event.values[1];
            break;
        case Surface.ROTATION_270:
            valueToUse = -event.values[0];
            break;
        default:
            valueToUse = event.values[0];
        }
        mValue = valueToUse + MpoHelper.OFFSET;
        if (timestamp != 0 && Math.abs(mValue) > MpoHelper.TH) {
            final float dT = (event.timestamp - timestamp) * MpoHelper.NS2S;

            angle[1] += mValue * dT * 180 / Math.PI;
            if (mFirstTime) {
                angle[0] = angle[1] - MpoHelper.BASE_ANGLE;
                angle[2] = angle[1] + MpoHelper.BASE_ANGLE;
                mFirstTime = false;
            } else if (angle[1] <= angle[0]) {
                angle[0] = angle[1];
                angle[2] = angle[0] + 2 * MpoHelper.BASE_ANGLE;
            } else if (angle[1] >= angle[2]) {
                angle[2] = angle[1];
                angle[0] = angle[2] - 2 * MpoHelper.BASE_ANGLE;
            }
        }
        float mAngle ;
        if (timestamp != 0 && mCount != 0) {
            mAngle = angle[1] - angle[0];
        } else {
            mAngle = AbstractGalleryActivity.UNUSABLE_ANGLE_VALUE;
        }
        timestamp = event.timestamp;
        return mAngle;
    }

    private void refresh(int lastIndex) {
        synchronized (mAllMavPlayer) {
            if (mMavRenderThread != null) {
                if (mMavRenderThread.animationFinished()) {
                    mMavRenderThread.initAnimation(lastIndex, 0);
                } else {
                    int distance = Math.abs(mMavRenderThread.getAnimationTagetIndex() - lastIndex);
                    if (distance >= MavRenderThread.CONTINUOUS_FRAME_ANIMATION_CHANGE_THRESHOLD) {
                        mMavRenderThread.initAnimation(lastIndex, 0);
                    }
                }

            }
        }
        long time = SystemClock.uptimeMillis();
        if (mMavRenderThread != null) {
            mMavRenderThread.setRenderRequester(true);
        }
        time = SystemClock.uptimeMillis() - time;
        Log.i(TAG, "request render consumed " + time + "ms");
    }

    public void drawMavFrame(int index) {
        // TODO Auto-generated method stub
        boolean requestrender = false;
        synchronized(mAllMavPlayer) {
            for(MavPlayer player : mAllMavPlayer.keySet()) {
                requestrender |= requester(player, index);
            }
        }
        if (requestrender) {
            requestRender();
        }
    }
}
