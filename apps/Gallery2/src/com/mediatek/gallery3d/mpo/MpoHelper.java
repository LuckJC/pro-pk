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
package com.mediatek.gallery3d.mpo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;

import com.mediatek.common.mpodecoder.IMpoDecoder;
import com.mediatek.common.MediatekClassFactory;

import com.android.gallery3d.R;

import com.android.gallery3d.app.Gallery;
import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.data.MediaItem;


import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.common.BitmapUtils;
import com.mediatek.gallery3d.data.DecodeHelper;
import com.mediatek.gallery3d.data.RegionDecoder;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public class MpoHelper {
	
    private static final String TAG = "Gallery2/MpoHelper";

    public static final String MPO_EXTENSION = "mpo";

    public static final String FILE_EXTENSION = "mpo";
    public static final String MIME_TYPE = "image/mpo";
    public static final String MPO_MIME_TYPE = "image/mpo";//this variable will be deleted later
    public static final String MPO_VIEW_ACTION = "com.mediatek.action.VIEW_MPO";

    public static final float BASE_ANGLE = 12.5f;
    public static final float NS2S = 1.0f / 1000000000.0f;
    public static final float TH = 0.001f;
    public static final float OFFSET = 0.0f;
    
    private static Drawable sMavOverlay = null;

    public static IMpoDecoder createMpoDecoder(JobContext jc, String filePath) {
        try {
            Log.i(TAG,"createMpoDecoder:filepath:"+filePath);
            if (null == filePath) return null;
            /// M: for certain mpo file, MpoDecoder.decodeFile() will return null
            // then it will cause JE when reference the returned instance. @{
            IMpoDecoder mpoDecoder = MediatekClassFactory.createInstance(IMpoDecoder.class, filePath);
            if (mpoDecoder != null && mpoDecoder.isMpoDecoderValid()) {
                return mpoDecoder;
            }
            return null;
            /// @}
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static IMpoDecoder createMpoDecoder(JobContext jc, ContentResolver cr,
                                  Uri uri) {
        try {
            Log.i(TAG,"createMpoDecoder:uri:" + uri);
            if (null == cr || null == uri) return null;
            /// M: for certain mpo file, MpoDecoder.decodeFile() will return null
            // then it will cause JE when reference the returned instance. @{
            IMpoDecoder mpoDecoder = MediatekClassFactory.createInstance(IMpoDecoder.class, cr, uri);
            if (mpoDecoder != null && mpoDecoder.isMpoDecoderValid()) {
                return mpoDecoder;
            }
            return null;
            /// @}
        } catch (Exception e)  {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String getMpoWhereClause(boolean showAllMpo) {
        String mpoFilter = null;
        if (!showAllMpo) {
            mpoFilter = FileColumns.MIME_TYPE + "!='" + MPO_MIME_TYPE + "'";
        }
        return mpoFilter;
    }

    public static String getWhereClause(int mtkInclusion) {
        if ((MediatekFeature.ALL_MPO_MEDIA & mtkInclusion) == 0) {
            return null;
        }
        String whereClause = null;
        String whereClauseEx = FileColumns.MIME_TYPE + "='" + 
                               MpoHelper.MPO_MIME_TYPE + "'";
        String whereClauseIn = FileColumns.MIME_TYPE + "='" + 
                               MpoHelper.MPO_MIME_TYPE + "'";
        String subWhereClause = null;

        if ((mtkInclusion & MediatekFeature.EXCLUDE_DEFAULT_MEDIA) != 0) {
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_MAV) != 0) {
                //Log.v(TAG,"getWhereClause:add where clause add mav");
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_MAV:
                          subWhereClause + " OR " + 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_MAV;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_3D) != 0) {
                //Log.v(TAG,"getWhereClause:add where clause add mpo 3d");
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_Stereo:
                          subWhereClause + " OR " + 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_Stereo;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_3D_PAN) != 0) {
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_3DPan:
                          subWhereClause + " OR " + 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_3DPan;
            }

            if (null != subWhereClause) {
                whereClause = whereClauseEx + " AND ( " + subWhereClause + " )";
            } //else {
                //whereClause = whereClauseEx + " AND ( " + subWhereClause + " )";
            //}
        } else {
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_MAV) == 0) {
                //Log.v(TAG,"getWhereClause2:add where clause remove mav");
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_MAV:
                          subWhereClause + " AND " + 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_MAV;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_3D) == 0) {
                //Log.v(TAG,"getWhereClause2:add where clause remove mpo 3d");
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_Stereo:
                          subWhereClause + " AND " + 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_Stereo;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_3D_PAN) == 0) {
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_3DPan:
                          subWhereClause + " AND " + 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_3DPan;
            }

            if (null != subWhereClause) {
                whereClause = whereClauseEx + " AND ( " + subWhereClause + " )";
            } else {
                whereClause = null;
            }

        }

        //if (null == subWhereClause) {
        //    Log.e(TAG,"getWhereClause:why got null subWhereClause?");
        //} else {
        //    whereClause = whereClause + " AND (" + subWhereClause + ")";
        //}
        //Log.i(TAG,"getWhereClause:whereClause="+whereClause);
        return whereClause;
    }
    
    public static void playMpo(Activity activity, Uri uri) {
        try {
            Intent i = new Intent(MPO_VIEW_ACTION);
            i.setDataAndType(uri, MPO_MIME_TYPE);
            activity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to open mpo file: ", e);
        }
    }

    public static void drawImageTypeOverlay(Context context, Bitmap bitmap) {
        if (null == sMavOverlay) {
            sMavOverlay = context.getResources().getDrawable(R.drawable.ic_mav_overlay);
        }
        int width = sMavOverlay.getIntrinsicWidth();
        int height = sMavOverlay.getIntrinsicHeight();
        float aspectRatio = (float) width / (float) height;
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();
        boolean heightSmaller = (bmpHeight < bmpWidth);
        int scaleResult = (heightSmaller ? bmpHeight : bmpWidth) / 5;
        if (heightSmaller) {
            height = scaleResult;
            width = (int)(scaleResult * aspectRatio);
        } else {
            width = scaleResult;
            height = (int)(width / aspectRatio);
        }
        int left = (bmpWidth - width) / 2;
        int top = (bmpHeight - height) / 2;
        sMavOverlay.setBounds(left, top, left + width, top + height);
        Canvas tmpCanvas = new Canvas(bitmap);
        sMavOverlay.draw(tmpCanvas);
    }
    
    public static int getInclusionFromData(Bundle data) {
        return MediatekFeature.EXCLUDE_MPO_MAV;
    }
    
    public static String getMavWhereClause(int mavInclusion) {
        String whereClause = null;
        if ((mavInclusion & MediatekFeature.EXCLUDE_MPO_MAV) !=0) {
            whereClause =  Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_MAV;
        }
        return whereClause;
    }

    private static Bitmap[] retrieveMicroMpoFrames(JobContext jc, Params params, MpoDecoderWrapper mpoDecoderWrapper) {
        if (null != jc && jc.isCancelled()) {
            Log.v(TAG, "retrieveThumbData:job cancelled");
            return null;
        }
        int frameCount = mpoDecoderWrapper.frameCount();
        Options options = new Options();
        Bitmap[] mpoFrames = new Bitmap[frameCount];
        options.inSampleSize = DecodeHelper.calculateSampleSizeByType(
                    mpoDecoderWrapper.width(), mpoDecoderWrapper.height(),
                    params.inType, params.inOriginalTargetSize);
        options.inPostProc = params.inPQEnhance;
        if (params.inMpoFrames) {
            for (int i = 0; i < frameCount; i++) {
                Bitmap mBitmap = decodeFrameSafe(jc, 
                                               mpoDecoderWrapper, i, options);
                if (mBitmap == null) {
                    return null;
                }
                mpoFrames[i] = DecodeHelper.postScaleDown(
                        mBitmap, params.inType,
                        params.inOriginalTargetSize);
            }
        }
        return mpoFrames;
    }
    
    public static Bitmap[] decodeMpoFrames(JobContext jc, Params params,
            MpoDecoderWrapper mpoDecoderWrapper, MavListener listener) {
        if (null == params || null == mpoDecoderWrapper) {
            Log.e(TAG, "decodeMpoFrames:got null decoder or params!");
            return null;
        }
          if (params.inType == MediaItem.TYPE_MICROTHUMBNAIL) {
                return retrieveMicroMpoFrames(jc, params, mpoDecoderWrapper);
            }
        int targetDisplayWidth = params.inTargetDisplayWidth;
        int targetDisplayHeight = params.inTargetDisplayHeight;
        int frameCount = mpoDecoderWrapper.frameCount();
        int frameWidth = mpoDecoderWrapper.width();
        int frameHeight = mpoDecoderWrapper.height();
        MtkLog.d(TAG, "mpo frame width: " + frameWidth + ", frame height: "
                + frameHeight);
        if (targetDisplayWidth <= 0 || targetDisplayHeight <= 0
                || MpoDecoderWrapper.INVALID_VALUE == frameCount
                || MpoDecoderWrapper.INVALID_VALUE == frameWidth
                || MpoDecoderWrapper.INVALID_VALUE == frameHeight) {
            Log.e(TAG, "decodeMpoFrames:got invalid parameters");
            return null;
        }

        // now as paramters are all valid, we start to decode mpo frames
        Bitmap[] mpoFrames = null;
        try {
            mpoFrames = tryDecodeMpoFrames(jc, mpoDecoderWrapper, params,
                    targetDisplayWidth, targetDisplayHeight, listener);
        } catch (OutOfMemoryError e) {
            Log.w(TAG, "decodeMpoFrames:out memory when decode mpo frames");
            e.printStackTrace();
            // when out of memory happend, we decode smaller mpo frames
            // we try smaller display size
            int targetDisplayPixelCount = targetDisplayWidth
                    * targetDisplayHeight;
            for (int i = 0; i < DecodeHelper.TARGET_DISPLAY_WIDTH.length; i++) {
                int pixelCount = DecodeHelper.TARGET_DISPLAY_WIDTH[i]
                        * DecodeHelper.TARGET_DISPLAY_HEIGHT[i];
                if (pixelCount >= targetDisplayPixelCount) {
                    continue;
                } else {
                    if (jc != null && jc.isCancelled()) {
                        Log.v(TAG, "decodeMpoFrames:job cancelled");
                        break;
                    }
                    Log.i(TAG, "decodeMpoFrames:try display ("
                            + DecodeHelper.TARGET_DISPLAY_WIDTH[i] + " x "
                            + DecodeHelper.TARGET_DISPLAY_HEIGHT[i] + ")");
                    try {
                        mpoFrames = tryDecodeMpoFrames(jc, mpoDecoderWrapper,
                                params, DecodeHelper.TARGET_DISPLAY_WIDTH[i],
                                DecodeHelper.TARGET_DISPLAY_HEIGHT[i], listener);
                    } catch (OutOfMemoryError oom) {
                        Log.w(TAG, "decodeMpoFrames:out of memory again:" + oom);
                        continue;
                    }
                    Log.d(TAG, "decodeMpoFrame: we finished decoding process");
                    break;
                }
            }
        }
        if (jc != null && jc.isCancelled()) {
            Log.d(TAG, "decodeMpoFrame:job cancelled, recycle decoded");
            recycleBitmapArray(mpoFrames);
            return null;
        }
        return mpoFrames;
    }

    public static Bitmap[] tryDecodeMpoFrames(JobContext jc,
            MpoDecoderWrapper mpoDecoderWrapper, Params params,
            int targetDisplayWidth, int targetDisplayHeight,
            MavListener listener) {
        // we believe all the parameters are valid
        int frameCount = mpoDecoderWrapper.frameCount();
        int frameWidth = mpoDecoderWrapper.width();
        int frameHeight = mpoDecoderWrapper.height();

        Options options = new Options();
        int initTargetSize = targetDisplayWidth > targetDisplayHeight ? targetDisplayWidth
                : targetDisplayHeight;
        float scale = (float) initTargetSize
                / Math.max(frameWidth, frameHeight);
        options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
        MediatekFeature
                .enablePictureQualityEnhance(options, params.inPQEnhance);

        Bitmap[] mpoFrames = new Bitmap[frameCount];
        boolean decodeFailed = false;
        try {
            for (int i = 0; i < frameCount; i++) {
                if (jc != null && jc.isCancelled()) {
                    Log.d(TAG, "tryDecodeMpoFrames:job cancelled");
                    break;
                }
                Bitmap bitmap = decodeFrame(jc, mpoDecoderWrapper, i, options);
                if (null == bitmap) {
                    Log.e(TAG, "tryDecodeMpoFrames:got null frame");
                    decodeFailed = true;
                    break;
                }
                float scaleDown = DecodeHelper.largerDisplayScale(bitmap.getWidth(),
                        bitmap.getHeight(), targetDisplayWidth,
                        targetDisplayHeight);
                if (scaleDown < 1.0f) {
                    mpoFrames[i] = DecodeHelper.resizeBitmap(bitmap, scaleDown, true);
                } else {
                    mpoFrames[i] = bitmap;
                }
                if (null != mpoFrames[i]) {
                    Log.v(TAG,
                            "tryDecodeMpoFrames:got mpoFrames[" + i + "]:["
                                    + mpoFrames[i].getWidth() + "x"
                                    + mpoFrames[i].getHeight() + "]");
                }
                // update progress
                if (listener != null) {
                    MtkLog.d("CGW", "update mav progress: " + i);
                    listener.setProgress(i);
                }
            }
        } catch (OutOfMemoryError e) {
            Log.w(TAG, "tryDecodeMpoFrames:out of memory, recycle decoded");
            recycleBitmapArray(mpoFrames);
            throw e;
        }
        if (jc != null && jc.isCancelled() || decodeFailed) {
            Log.d(TAG,
                    "tryDecodeMpoFrames:job cancelled or decode failed, recycle decoded");
            recycleBitmapArray(mpoFrames);
            return null;
        }
        return mpoFrames;
    }

    public static void recycleBitmapArray(Bitmap[] bitmapArray) {
        if (null == bitmapArray) {
            return;
        }
        for (int i = 0; i < bitmapArray.length; i++) {
            if (null == bitmapArray[i]) {
                continue;
            }
            //Log.v(TAG, "recycleBitmapArray:recycle bitmapArray[" + i + "]");
            bitmapArray[i].recycle();
        }
    }

    public static Bitmap decodeFrame(JobContext jc, MpoDecoderWrapper mpoDecoderWrapper,
            int frameIndex, Options options) {
        if (null == mpoDecoderWrapper || frameIndex < 0 || null == options) {
            Log.w(TAG, "decodeFrame:invalid paramters");
            return null;
        }
        Bitmap bitmap = mpoDecoderWrapper.frameBitmap(frameIndex, options);
        if (null != jc && jc.isCancelled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return bitmap;
    }

    public static Bitmap decodeFrameSafe(JobContext jc, MpoDecoderWrapper mpoDecoderWrapper,
            int frameIndex, Options options) {
        if (null == mpoDecoderWrapper || frameIndex < 0 || null == options) {
            Log.w(TAG, "decodeFrameSafe:invalid paramters");
            return null;
        }
        //As there is a chance no enough dvm memory for decoded Bitmap,
        //Skia will return a null Bitmap. In this case, we have to
        //downscale the decoded Bitmap by increase the options.inSampleSize
        Bitmap bitmap = null;
        final int maxTryNum = 8;
        for (int i=0; i < maxTryNum && (null == jc || !jc.isCancelled()); i++) {
            //we increase inSampleSize to expect a smaller Bitamp
            Log.v(TAG,"decodeFrameSafe:try for sample size " +
                      options.inSampleSize);
            try {
                bitmap = MpoHelper.decodeFrame(jc, mpoDecoderWrapper, frameIndex, options);
            } catch (OutOfMemoryError e) {
                Log.w(TAG,"decodeFrameSafe:out of memory when decoding:"+e);
            }
            if (null != bitmap) break;
            options.inSampleSize *= 2;
        }
        return bitmap;
    }

    public static RegionDecoder getRegionDecoder(JobContext jc,
            MpoDecoderWrapper mpoDecoderWrapper, int frameIndex) {
        if (null == mpoDecoderWrapper || frameIndex < 0) {
            Log.w(TAG, "getRegionDecoder:got null decoder or frameIndex!");
            return null;
        }
        Options options = new Options();
        options.inSampleSize = DecodeHelper.calculateSampleSize(DecodeHelper.MAX_BITMAP_BYTE_COUNT, -1,
                mpoDecoderWrapper.width(), mpoDecoderWrapper.height());
        //as we decode buffer for region decoder, we close PQ enhance option
        //to prevent double enhancement.

        Bitmap bitmap = decodeFrameSafe(jc, mpoDecoderWrapper,
                                        frameIndex, options);
        if (null == bitmap) return null;
        if (null != jc && jc.isCancelled()) {
            bitmap.recycle();
            return null;
        }
        return DecodeHelper.getRegionDecoder(jc, bitmap, true);
    }
}
