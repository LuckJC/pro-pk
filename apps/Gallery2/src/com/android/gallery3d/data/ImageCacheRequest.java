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

package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Trace;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.BytesBufferPool.BytesBuffer;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

abstract class ImageCacheRequest implements Job<Bitmap> {
    private static final String TAG = "Gallery2/ImageCacheRequest";

    protected GalleryApp mApplication;
//    private Path mPath;
    protected Path mPath;
    private int mType;
    private int mTargetSize;
    private long mTimeModified;

    public ImageCacheRequest(GalleryApp application,
            Path path, long timeModified, int type, int targetSize) {
        mApplication = application;
        mPath = path;
        mType = type;
        mTargetSize = targetSize;
        mTimeModified = timeModified;
    }

    private String debugTag() {
        return mPath + "," + mTimeModified + "," +
                ((mType == MediaItem.TYPE_THUMBNAIL) ? "THUMB" :
                 (mType == MediaItem.TYPE_MICROTHUMBNAIL) ? "MICROTHUMB" : "?");
    }
    
    @Override
    public Bitmap run(JobContext jc) {
        MtkUtils.traceStart("ImageCacheRequest:run");
        ImageCacheService cacheService = mApplication.getImageCacheService();

        BytesBuffer buffer = MediaItem.getBytesBufferPool().get();
        try {
            //boolean found = cacheService.getImageData(mPath, mType, buffer);
            Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>ImageCacheRequest-getImageData");
            boolean found = cacheService.getImageData(mPath, mTimeModified, mType, buffer);
            Trace.traceEnd(Trace.TRACE_TAG_APP);
            // if support picture quality tuning, we decode bitmap from origin image
            // in order to apply picture quality every time
            if (MtkLog.SUPPORT_PQ) found = false;
            if (jc.isCancelled()) {
                MtkUtils.traceEnd("ImageCacheRequest:run");
                return null;
            }
            if (found) {
                Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>ImageCacheRequest-decodeFromCache");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap;
                if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                    bitmap = DecodeUtils.decodeUsingPool(jc,
                            buffer.data, buffer.offset, buffer.length, options);
                } else {
                    bitmap = DecodeUtils.decodeUsingPool(jc,
                            buffer.data, buffer.offset, buffer.length, options);
                }
                /// M: dump Skia decoded cache Bitmap for debug @{
                if (MtkLog.DBG) {
                    if (bitmap == null) {
                        MtkLog.i(TAG, "decode orig failed replace new bitmap to dump");
                        bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565);
                        dumpBitmap(bitmap, cacheBitmap);
                        bitmap = null;
                    } else {
                        dumpBitmap(bitmap, cacheBitmap);
                    }
                }
                /// @}
                if (bitmap == null && !jc.isCancelled()) {
                    Log.w(TAG, "decode cached failed " + debugTag());
                }
                Trace.traceEnd(Trace.TRACE_TAG_APP);
                MtkUtils.traceEnd("ImageCacheRequest:run");
                return bitmap;
            }
        } finally {
            MediaItem.getBytesBufferPool().recycle(buffer);
        }
        Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>ImageCacheRequest-decodeFromOriginal");
        Bitmap bitmap = onDecodeOriginal(jc, mType);
        Trace.traceEnd(Trace.TRACE_TAG_APP);
        if (jc.isCancelled()) {
            MtkUtils.traceEnd("ImageCacheRequest:run");
            return null;
        }

        /// M: dump Skia decoded origin Bitmap for debug @{
        if (MtkLog.DBG) {
            if (bitmap == null) {
                MtkLog.i(TAG, "decode orig failed replace new bitmap to dump");
                bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565);
                dumpBitmap(bitmap, originBitmap);
                bitmap = null;
            } else {
                dumpBitmap(bitmap, originBitmap);
            }
        }
        /// @}
        
        if (bitmap == null) {
            Log.w(TAG, "decode orig failed " + debugTag());
            MtkUtils.traceEnd("ImageCacheRequest:run");
            return null;
        }

        Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>ImageCacheRequest-resizeAndCrop");
        if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
            bitmap = BitmapUtils.resizeAndCropCenter(bitmap, mTargetSize, true);
            /// M: added for ConShots
        } else if (mType == MediaItem.TYPE_MOTIONTHUMBNAIL) {
            bitmap = BitmapUtils.resizeAndKeepScale(bitmap, mTargetSize, true);
        } else {
            bitmap = BitmapUtils.resizeDownBySideLength(bitmap, mTargetSize, true);
        }
        Trace.traceEnd(Trace.TRACE_TAG_APP);
        if (jc.isCancelled()) {
            MtkUtils.traceEnd("ImageCacheRequest:run");
            return null;
        }

        // M: 6592 panorama modify @{
        byte[] array = null;
        MediaObject object = mApplication.getDataManager().getMediaObject(mPath);
        if (MediatekFeature.isPanorama3DSupported() && mType == MediaItem.TYPE_MICROTHUMBNAIL
                && object != null && object instanceof MediaItem
                && ((MediaItem) object).isPanorama()) {
            // As PNG format has alpha pass, if media item is panorama, we cache its micro-thumbnail as PNG.
            array = BitmapUtils.compressToBytes(bitmap, CompressFormat.PNG);
        } else {
            Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>ImageCacheRequest-compressToBytes");
            array = BitmapUtils.compressToBytes(bitmap);
            Trace.traceEnd(Trace.TRACE_TAG_APP);
        }
        // @}

        if (jc.isCancelled()) {
            MtkUtils.traceEnd("ImageCacheRequest:run");
            return null;
        }

        //cacheService.putImageData(mPath, mType, array);
        // if support picture quality tuning, we don't write data to cache in order to improve performance
        if (!MtkLog.SUPPORT_PQ) {
            Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>ImageCacheRequest-writeToCache");
            cacheService.putImageData(mPath, mTimeModified, mType, array);
            Trace.traceEnd(Trace.TRACE_TAG_APP);
        }
        MtkUtils.traceEnd("ImageCacheRequest:run");
        return bitmap;
    }

    public abstract Bitmap onDecodeOriginal(JobContext jc, int targetSize);
    
    //M: dump skia decode bitmap
    private final String cacheBitmap = "_CacheBitmap";
    private final String originBitmap = "_OriginBitmap";
    private void dumpBitmap(Bitmap bitmap, String source) {
        long dumpStart = System.currentTimeMillis();
        String fileType;
        if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
            fileType = "MicroTNail";
        } else {
            fileType = "TNail";
        }
        MediaItem item = (MediaItem) mPath.getObject();
        if (item != null) {
            String string = item.getName() + source + fileType;
            Log.i(TAG, "string " + string);
            MtkUtils.dumpBitmap(bitmap, string);
            Log.i(TAG, " Dump Bitmap time " + (System.currentTimeMillis() - dumpStart));
        }
    }
}
