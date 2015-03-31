/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.photos.data.GalleryBitmapPool;
import com.android.gallery3d.ui.ScreenNail;

import com.mediatek.gallery3d.stereo.StereoConvergence;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

public class TileImageViewAdapter implements TileImageView.TileSource {
    private static final String TAG = "Gallery2/TileImageViewAdapter";
    protected ScreenNail mScreenNail;
    protected boolean mOwnScreenNail;
    protected BitmapRegionDecoder mRegionDecoder;
    protected int mImageWidth;
    protected int mImageHeight;
    protected int mLevelCount;

    //added for stereo display
    protected final ScreenNail [] mStereoScreenNail = new ScreenNail[3];
    protected ScreenNail mStereoSecondScreenNailAC = null;
    protected final boolean [] mOwnStereoScreenNail = new boolean[3];
    protected final BitmapRegionDecoder [] mStereoRegionDecoder =
                        new BitmapRegionDecoder[3];
    protected final int [] mStereoImageWidth = new int[3];
    protected final int [] mStereoImageHeight = new int[3];
    protected final int [] mStereoLevelCount = new int[3];
    protected StereoConvergence mStereoConvergence;
    // M: 6592 panorama add @{
    protected ScreenNail mPanoramaScreenNail = null;
    // @}

    private final Rect mIntersectRect = new Rect();
    private final Rect mRegionRect = new Rect();

    private static int sTileDumpNum = 0;
    public TileImageViewAdapter() {
    }

    public TileImageViewAdapter(
            Bitmap bitmap, BitmapRegionDecoder regionDecoder) {
        Utils.checkNotNull(bitmap);
        updateScreenNail(new BitmapScreenNail(bitmap), true);
        mRegionDecoder = regionDecoder;
        mImageWidth = regionDecoder.getWidth();
        mImageHeight = regionDecoder.getHeight();
        mLevelCount = calculateLevelCount();
    }

    public synchronized void clear() {
        Log.v(TAG,"clear()");
        mScreenNail = null;
        mImageWidth = 0;
        mImageHeight = 0;
        mLevelCount = 0;
        mRegionDecoder = null;
        //added for stereo display
        updateStereoScreenNail(1, null, false);
        updateStereoScreenNail(2, null, false);
        setStereoRegionDecoder(1, null);
        setStereoRegionDecoder(2, null);
//        mStereoConvergence = null;
    }

    public synchronized void setScreenNail(Bitmap bitmap, int width, int height) {
        Utils.checkNotNull(bitmap);
        updateScreenNail(new BitmapScreenNail(bitmap), true);
        mImageWidth = width;
        mImageHeight = height;
        mRegionDecoder = null;
        mLevelCount = 0;
    }

    public synchronized void setScreenNail(
            ScreenNail screenNail, int width, int height) {
        Utils.checkNotNull(screenNail);
        mScreenNail = screenNail;
        mImageWidth = width;
        mImageHeight = height;
        mRegionDecoder = null;
        mLevelCount = 0;
    }

    private void updateScreenNail(ScreenNail screenNail, boolean own) {
        if (mScreenNail != null && mOwnScreenNail) {
            mScreenNail.recycle();
        }
        mScreenNail = screenNail;
        mOwnScreenNail = own;
    }

    public synchronized void setRegionDecoder(BitmapRegionDecoder decoder) {
        mRegionDecoder = Utils.checkNotNull(decoder);
        mImageWidth = decoder.getWidth();
        mImageHeight = decoder.getHeight();
        mLevelCount = calculateLevelCount();
    }

    private int calculateLevelCount() {
        return Math.max(0, Utils.ceilLog2(
                (float) mImageWidth / mScreenNail.getWidth()));
    }

    // Gets a sub image on a rectangle of the current photo. For example,
    // getTile(1, 50, 50, 100, 3, pool) means to get the region located
    // at (50, 50) with sample level 1 (ie, down sampled by 2^1) and the
    // target tile size (after sampling) 100 with border 3.
    //
    // From this spec, we can infer the actual tile size to be
    // 100 + 3x2 = 106, and the size of the region to be extracted from the
    // photo to be 200 with border 6.
    //
    // As a result, we should decode region (50-6, 50-6, 250+6, 250+6) or
    // (44, 44, 256, 256) from the original photo and down sample it to 106.
    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    @Override
    public Bitmap getTile(int level, int x, int y, int tileSize) {
        if (!ApiHelper.HAS_REUSING_BITMAP_IN_BITMAP_REGION_DECODER) {
            return getTileWithoutReusingBitmap(level, x, y, tileSize);
        }

        int t = tileSize << level;

        Rect wantRegion = new Rect(x, y, x + t, y + t);

        boolean needClear;
        BitmapRegionDecoder regionDecoder = null;

        synchronized (this) {
            regionDecoder = mRegionDecoder;
            if (regionDecoder == null) return null;

            // We need to clear a reused bitmap, if wantRegion is not fully
            // within the image.
            needClear = !new Rect(0, 0, mImageWidth, mImageHeight)
                    .contains(wantRegion);
        }

        Bitmap bitmap = GalleryBitmapPool.getInstance().get(tileSize, tileSize);
        if (bitmap != null) {
            if (needClear) bitmap.eraseColor(0);
        } else {
            bitmap = Bitmap.createBitmap(tileSize, tileSize, Config.ARGB_8888);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        options.inSampleSize =  (1 << level);
        options.inBitmap = bitmap;

        // M: for picture quality enhancement
        MediatekFeature.enablePictureQualityEnhance(options, mEnablePQ);

        try {
            // In CropImage, we may call the decodeRegion() concurrently.
            // M: Do region decode in multi-thread, so delete synchronized
            // synchronized (regionDecoder) {
                bitmap = regionDecoder.decodeRegion(wantRegion, options);
                if (MtkLog.DBG_DUMP_TILE) {
                    if (bitmap == null) {
                        MtkLog.i(TAG, "<getTile1> decodeRegion l" + level + "-x" + x
                                + "-y" + y + "-size" + tileSize + ", return null");
                    } else {
                        MtkUtils.dumpBitmap(bitmap, "Tile-l" + level + "-x" + x
                                + "-y" + y + "-size" + tileSize + "-"
                                + sTileDumpNum);
                        sTileDumpNum++;
                    }
                }
            // }
        } finally {
            if (options.inBitmap != bitmap && options.inBitmap != null) {
                GalleryBitmapPool.getInstance().put(options.inBitmap);
                options.inBitmap = null;
            }
        }

        if (bitmap == null) {
            Log.w(TAG, "fail in decoding region");
        }
        return bitmap;
    }

    private Bitmap getTileWithoutReusingBitmap(
            int level, int x, int y, int tileSize) {
        int t = tileSize << level;
        Rect wantRegion = new Rect(x, y, x + t, y + t);

        BitmapRegionDecoder regionDecoder;
        Rect overlapRegion;

        synchronized (this) {
            regionDecoder = mRegionDecoder;
            if (regionDecoder == null) return null;
            overlapRegion = new Rect(0, 0, mImageWidth, mImageHeight);
            Utils.assertTrue(overlapRegion.intersect(wantRegion));
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        options.inSampleSize =  (1 << level);
        Bitmap bitmap = null;

        // In CropImage, we may call the decodeRegion() concurrently.
        // M: Do region decode in multi-thread, so delete synchronized
        // synchronized (regionDecoder) {
            bitmap = regionDecoder.decodeRegion(overlapRegion, options);
            if (MtkLog.DBG_DUMP_TILE) {
                if (bitmap == null) {
                    MtkLog.i(TAG, "<getTileWithoutReusingBitmap> decodeRegion l" + level + "-x" + x
                            + "-y" + y + "-size" + tileSize + ", return null");
                } else {
                    MtkUtils.dumpBitmap(bitmap, "Tile-l" + level + "-x" + x
                            + "-y" + y + "-size" + tileSize + "-"
                            + sTileDumpNum);
                    sTileDumpNum++;
                }
            }
        // }

        if (bitmap == null) {
            Log.w(TAG, "fail in decoding region");
        }

        if (wantRegion.equals(overlapRegion)) return bitmap;

        Bitmap result = Bitmap.createBitmap(tileSize, tileSize, Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bitmap,
                (overlapRegion.left - wantRegion.left) >> level,
                (overlapRegion.top - wantRegion.top) >> level, null);
        return result;
    }


    @Override
    public ScreenNail getScreenNail() {
        return mScreenNail;
    }

    @Override
    public int getImageHeight() {
        return mImageHeight;
    }

    @Override
    public int getImageWidth() {
        return mImageWidth;
    }

    @Override
    public int getLevelCount() {
        return mLevelCount;
    }


    ////////////////////////////////////////////////////////////////////////////
    //  Mediatek added features
    ////////////////////////////////////////////////////////////////////////////

    private int calculateLevelCount(int fullWidth, int screennailWidth) {
        return Math.max(0, Utils.ceilLog2((float) fullWidth / screennailWidth));
    }

    public synchronized void setRegionDecoder(BitmapRegionDecoder decoder,
                 ScreenNail screenNail, int width, int height) {
        updateScreenNail(screenNail, false);
        mRegionDecoder = decoder;//the decoder may be null for bmp format
        mImageWidth = width;
        mImageHeight = height;
        mLevelCount = calculateLevelCount();
    }

    public synchronized void setRegionDecoder(BitmapRegionDecoder decoder,
                 Bitmap bitmap, int width, int height) {
        updateScreenNail(new BitmapScreenNail(bitmap), true);
        mRegionDecoder = decoder;//the decoder may be null for bmp format
        mImageWidth = width;
        mImageHeight = height;
        mLevelCount = calculateLevelCount();
    }

    private boolean mEnablePQ = true;
    public void setEnablePQ(boolean enablePQ) {
        mEnablePQ = enablePQ;
    }

    public synchronized void setStereoScreenNail(int stereoIndex, ScreenNail s) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        updateStereoScreenNail(stereoIndex, s, false);
    }

    public synchronized void setStereoScreenNail(int stereoIndex, Bitmap bitmap) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        if (null != bitmap) {
            updateStereoScreenNail(stereoIndex, 
                                   new BitmapScreenNail(bitmap), true);
        } else {
            updateStereoScreenNail(stereoIndex, null, true);
        }
    }

    // M: 6592 panorama add @{
    public synchronized void setPanoramaScreenNail(ScreenNail s) {
        mPanoramaScreenNail = s;
    }
    
    @Override
    public ScreenNail getPanoramaScreenNail() {
        return mPanoramaScreenNail;
    }
    // @}
    public synchronized void setStereoSecondScreenNailAC(ScreenNail s) {
        mStereoSecondScreenNailAC = s;
    }

    private void updateStereoScreenNail(int stereoIndex,
                                    ScreenNail screenNail, boolean own) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        if (mStereoScreenNail[stereoIndex] != null && 
            mOwnStereoScreenNail[stereoIndex]) {
            mStereoScreenNail[stereoIndex].recycle();
        }
        mStereoScreenNail[stereoIndex] = screenNail;
        mOwnStereoScreenNail[stereoIndex] = own;
        // update stereo info
        if (screenNail != null) {
            mStereoImageWidth[stereoIndex] = screenNail.getWidth();
            mStereoImageHeight[stereoIndex] = screenNail.getHeight();
            mStereoLevelCount[stereoIndex] = 0;
        } else {
            mStereoImageWidth[stereoIndex] = 0;
            mStereoImageHeight[stereoIndex] = 0;
            mStereoLevelCount[stereoIndex] = 0;
        }
    }
    
    public synchronized void setStereoRegionDecoder(int stereoIndex,
                                 BitmapRegionDecoder decoder) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        mStereoRegionDecoder[stereoIndex] = decoder;
        if (decoder != null) {
            mStereoImageWidth[stereoIndex] = decoder.getWidth();
            mStereoImageHeight[stereoIndex] = decoder.getHeight();
            mStereoLevelCount[stereoIndex] =
                calculateLevelCount(mStereoImageWidth[stereoIndex],
                                    mStereoScreenNail[stereoIndex].getWidth());
        }
    }

    public BitmapRegionDecoder getStereoRegionDecoder(int stereoIndex, boolean acEnabled) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        if (stereoIndex == 0) {
            return mRegionDecoder;
        } else if (stereoIndex == StereoHelper.STEREO_INDEX_SECOND && acEnabled) {
            return null;
        }
        return mStereoRegionDecoder[stereoIndex];
    }

    @Override
    public ScreenNail getStereoScreenNail(int stereoIndex, boolean acEnabled) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        if (stereoIndex == 0) {
            return mScreenNail;
        }
        // if acEnabled == true, we do not return mStereoScreenNail[2] when mStereoSecondScreenNailAC is not ready
        if (stereoIndex == StereoHelper.STEREO_INDEX_SECOND && acEnabled) {
            return mStereoSecondScreenNailAC;
        }
        return mStereoScreenNail[stereoIndex];
    }

    @Override
    public int getStereoImageWidth(int stereoIndex) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        if (StereoHelper.STEREO_INDEX_NONE == stereoIndex) {
            return mImageWidth;
        } else {
            return mStereoImageWidth[stereoIndex];
        }
    }

    @Override
    public int getStereoImageHeight(int stereoIndex) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        if (StereoHelper.STEREO_INDEX_NONE == stereoIndex) {
            return mImageHeight;
        } else {
            return mStereoImageHeight[stereoIndex];
        }
    }

    @Override
    public int getStereoLevelCount(int stereoIndex) {
        Utils.assertTrue(stereoIndex < 3 && stereoIndex >= 0);
        if (StereoHelper.STEREO_INDEX_NONE == stereoIndex) {
            return mLevelCount;
        } else {
            return mStereoLevelCount[stereoIndex];
        }
    }

    // This function is actually modified from getTile()
    // Please be very carefull when origin getTile() is modified.
    @Override
    public Bitmap getTile(int level, int x, int y, int tileSize, int stereoIndex, boolean acEnabled) {
        int t = tileSize << level;

        Rect wantRegion = new Rect(x, y, x + t, y + t);

        boolean needClear;
        BitmapRegionDecoder regionDecoder = null;

        synchronized (this) {
            regionDecoder = getStereoRegionDecoder(stereoIndex, acEnabled);
            if (regionDecoder == null) return null;

            // We need to clear a reused bitmap, if wantRegion is not fully
            // within the image.
            needClear = !new Rect(0, 0, getStereoImageWidth(stereoIndex),
                    getStereoImageHeight(stereoIndex)).contains(wantRegion);
        }

        Bitmap bitmap = GalleryBitmapPool.getInstance().get(tileSize, tileSize);
        if (bitmap != null) {
            if (needClear) bitmap.eraseColor(0);
        } else {
            bitmap = Bitmap.createBitmap(tileSize, tileSize, Config.ARGB_8888);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        options.inSampleSize =  (1 << level);
        options.inBitmap = bitmap;

        if (StereoHelper.STEREO_INDEX_NONE == stereoIndex) {
            // M: for picture quality enhancement
            MediatekFeature.enablePictureQualityEnhance(options, mEnablePQ);
        }

        try {
            // In CropImage, we may call the decodeRegion() concurrently.
            // M: Do region decode in multi-thread, so delete synchronized
            // synchronized (regionDecoder) {
                bitmap = regionDecoder.decodeRegion(wantRegion, options);
                if (MtkLog.DBG_DUMP_TILE) {
                    if (bitmap == null) {
                        MtkLog.i(TAG, "<getTile2> decodeRegion l" + level + "-x" + x
                                + "-y" + y + "-size" + tileSize + ", return null");
                    } else {
                        MtkUtils.dumpBitmap(bitmap, "Tile-l" + level + "-x" + x
                                + "-y" + y + "-size" + tileSize + "-"
                                + sTileDumpNum);
                        sTileDumpNum++;
                    }
                }
            // }
        } finally {
            if (options.inBitmap != bitmap && options.inBitmap != null) {
                GalleryBitmapPool.getInstance().put(options.inBitmap);
                options.inBitmap = null;
            }
        }

        if (bitmap == null) {
            Log.w(TAG, "fail in decoding region");
        }
        Log.v(TAG,"getTile:bitmap="+bitmap+" //stereoIndex="+stereoIndex);
        return bitmap;
    }

    @Override
    public StereoConvergence getStereoConvergence() {
        return mStereoConvergence;
    }

    public synchronized void setStereoConvergence(
                                    StereoConvergence stereoConvergence) {
        mStereoConvergence = stereoConvergence;
    }

    public synchronized void clearRegionDecoder() {
        Log.d(TAG, "[" + this + "] clearRegionDecoder");
        mRegionDecoder = null;
        mImageWidth = (mScreenNail != null ? mScreenNail.getWidth() : 0);
        mImageHeight = (mScreenNail != null ? mScreenNail.getHeight() : 0);
        mLevelCount = 0;
    }


}
