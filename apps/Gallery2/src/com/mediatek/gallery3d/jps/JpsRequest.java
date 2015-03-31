package com.mediatek.gallery3d.jps;

import java.io.FileDescriptor;
import java.io.FileInputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.data.DecodeHelper;
import com.mediatek.gallery3d.data.IMediaRequest;
import com.mediatek.gallery3d.data.RegionDecoder;
import com.mediatek.gallery3d.stereo.StereoConvergence;
import com.mediatek.gallery3d.stereo.StereoConvertor;
import com.mediatek.gallery3d.stereo.StereoEffectHandle;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkUtils;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public class JpsRequest implements IMediaRequest {
    private static final String TAG = "JpsRequest";

    public JpsRequest() {
    }

    public DataBundle request(JobContext jc, Params params, String filePath) {
        Log.i(TAG, "request(jc, parmas, filePath="+filePath+")");
        if (null == params || null == filePath) {
            Log.e(TAG,"request:invalid parameters");
            return null;
        }

        FileInputStream fis = null;
        FileDescriptor fd = null;
        try {
            fis = new FileInputStream(filePath);
            fd = fis.getFD();
            BitmapRegionDecoder regionDecoder =
                DecodeUtils.createBitmapRegionDecoder(null, fd, false);
            //assume that the jps is left-right layout
            int layout = StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT;
            return request(jc, layout, params, regionDecoder);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            Utils.closeSilently(fis);
        }
    }

    public DataBundle request(JobContext jc, Params params, byte[] data, 
                              int offset,int length) {
        Log.e(TAG, "request:no support for buffer!");
        return null;
    }

    public DataBundle request(JobContext jc, Params params,
                              ContentResolver cr, Uri uri) {
        Log.i(TAG, "request(jc, parmas, cr, uri="+uri+")");
        if (null == params || null == cr || null == uri) {
            Log.e(TAG,"request:invalid parameters");
            return null;
        }

        ParcelFileDescriptor pfd = null;
        FileDescriptor fd = null;
        try {
            pfd = cr.openFileDescriptor(uri, "r");
            fd = pfd.getFileDescriptor();
            BitmapRegionDecoder regionDecoder =
                DecodeUtils.createBitmapRegionDecoder(null, fd, false);
            //assume that the jps is left-right layout
            int layout = StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT;
            return request(jc, layout, params, regionDecoder);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            Utils.closeSilently(pfd);
        }
    }

    public DataBundle request(JobContext jc, int layout, Params params,
                              BitmapRegionDecoder regionDecoder) {
        if (null != jc && jc.isCancelled()) {
            Log.v(TAG, "request:job cancelled");
            return null;
        }

        if (null == params || null == regionDecoder) {
            Log.w(TAG, "request:got null params or decoder!");
            return null;
        }

        params.info(TAG);

        DataBundle dataBundle = new DataBundle();

        if (params.inOriginalFrame || params.inFirstFrame ||
            params.inSecondFrame) {
            retrieveThumbData(jc, layout, params, dataBundle, regionDecoder);
        }

        if (params.inStereoConvergence) {
            if (dataBundle.firstFrame == null && params.inInputDataBundle != null){
                dataBundle.firstFrame = params.inInputDataBundle.firstFrame;
            }
            if (dataBundle.secondFrame == null && params.inInputDataBundle != null){
                dataBundle.secondFrame = params.inInputDataBundle.secondFrame;
            }
            retrieveStereoConvergence(jc, layout, params, dataBundle, regionDecoder);
        }

        if (params.inOriginalFullFrame || params.inFirstFullFrame ||
            params.inSecondFullFrame) {
            retrieveLargeData(jc, layout, params, dataBundle, regionDecoder);
        }

        if (params.inGifDecoder) {
            Log.w(TAG, "request: no GifDecoder can be generated from jps");
        }

        dataBundle.info(TAG);

        return dataBundle;
    }

    private void retrieveThumbData(JobContext jc, int layout, Params params,
                     DataBundle dataBundle, BitmapRegionDecoder regionDecoder) {
        if (null != jc && jc.isCancelled()) {
            Log.v(TAG, "retrieveThumbData:job cancelled");
            return;
        }

        if (null == params || null == regionDecoder) {
            Log.e(TAG,"retrieveThumbData:invalid parameters");
            return;
        }

        Rect imageRect = new Rect(0, 0, regionDecoder.getWidth(), 
                                        regionDecoder.getHeight());
        JpsHelper.adjustRect(layout, true, imageRect);

        Options options = new Options();
        options.inSampleSize = DecodeHelper.calculateSampleSizeByType(
                                   imageRect.right - imageRect.left, 
                                   imageRect.bottom - imageRect.top, 
                                   params.inType, params.inOriginalTargetSize);
        options.inPostProc = params.inPQEnhance;

        if (params.inOriginalFrame) {
            dataBundle.originalFrame = DecodeHelper.safeDecodeImageRegion(
                jc, regionDecoder, imageRect, options);
            dataBundle.originalFrame = DecodeHelper.postScaleDown(
                dataBundle.originalFrame, params.inType,
                params.inOriginalTargetSize);
        }

        if (params.inFirstFrame) {
            Bitmap tempThumb = null;
//            if (params.inInputDataBundle != null &&
//                params.inInputDataBundle.originalFrame != null &&
//                !params.inInputDataBundle.originalFrame.isRecycled()) {
//                tempThumb = params.inInputDataBundle.originalFrame;
//            }
            if (null == tempThumb) {
                tempThumb = DecodeHelper.safeDecodeImageRegion(
                    jc, regionDecoder, imageRect, options);
                tempThumb = DecodeHelper.postScaleDown(tempThumb, params.inType,
                    params.inOriginalTargetSize);
            } else {
                tempThumb = tempThumb.copy(tempThumb.getConfig(), tempThumb.isMutable());
            }
            dataBundle.firstFrame = tempThumb;
        }

        if (params.inSecondFrame) {
            imageRect.set(0, 0, regionDecoder.getWidth(), regionDecoder.getHeight());
            JpsHelper.adjustRect(layout, false, imageRect);
            dataBundle.secondFrame = DecodeHelper.safeDecodeImageRegion(
                jc, regionDecoder, imageRect, options);
            dataBundle.secondFrame = DecodeHelper.postScaleDown(
                dataBundle.secondFrame, params.inType,
                params.inOriginalTargetSize);
        }
    }

    private void retrieveStereoConvergence(JobContext jc, int layout, Params params,
                     DataBundle dataBundle, BitmapRegionDecoder regionDecoder) {
        if (null != jc && jc.isCancelled()) {
            Log.v(TAG, "retrieveStereoConvergence:job cancelled");
            return;
        }

        if (null == params || null == regionDecoder) {
            Log.e(TAG,"retrieveStereoConvergence:invalid parameters");
            return;
        }

        Rect imageRect = new Rect(0, 0, regionDecoder.getWidth(), 
                                        regionDecoder.getHeight());
        JpsHelper.adjustRect(layout, true, imageRect);

        Options options = new Options();
        options.inSampleSize = DecodeHelper.calculateSampleSizeByType(
                                   imageRect.right - imageRect.left, 
                                   imageRect.bottom - imageRect.top, 
                                   params.inType, params.inOriginalTargetSize);
        options.inPostProc = params.inPQEnhance;

        Bitmap firstFrame = null;
        Bitmap secondFrame = null;

        if (dataBundle.firstFrame == null) {
            firstFrame = DecodeHelper.safeDecodeImageRegion(
                    jc, regionDecoder, imageRect, options);
            firstFrame = DecodeHelper.postScaleDown(firstFrame, params.inType,
                    params.inOriginalTargetSize);
        } else {
            firstFrame = dataBundle.firstFrame;
        }

        if (dataBundle.secondFrame == null) {
            imageRect.set(0, 0, regionDecoder.getWidth(), regionDecoder.getHeight());
            JpsHelper.adjustRect(layout, false, imageRect);
            secondFrame = DecodeHelper.safeDecodeImageRegion(
                    jc, regionDecoder, imageRect, options);
            secondFrame = DecodeHelper.postScaleDown(secondFrame, params.inType,
                    params.inOriginalTargetSize);
        } else {
            secondFrame = dataBundle.secondFrame;
        }

        if (null != jc && jc.isCancelled()) {
            Log.v(TAG, "retrieveStereoConvergence:job cancelled 2");
            return;
        }
        
        dataBundle.stereoConvergence = new StereoConvergence(firstFrame, secondFrame,
                params.inMtk3D);
        StereoEffectHandle.getInstance().addEffect(dataBundle.stereoConvergence);
        synchronized (dataBundle.stereoConvergence) {
            while (!dataBundle.stereoConvergence.isEffectDone()) {
                try {
                    dataBundle.stereoConvergence.wait();
                } catch (InterruptedException e) {
                    Log.i(TAG, "<retrieveStereoConvergence> InterruptedException: "
                            + e.getMessage());
                }
            }
        }
        dataBundle.secondFrameAC = dataBundle.stereoConvergence.getRightBitmapAfterWarp();
        //recycle temporarily created Bitamp
        if (dataBundle.originalFrame == null &&
            dataBundle.firstFrame == null &&
            firstFrame != null) {
            firstFrame.recycle();
        }
        if (dataBundle.secondFrame == null && secondFrame != null) {
            secondFrame.recycle();
        }
    }

    private void retrieveLargeData(JobContext jc, int layout, Params params,
                     DataBundle dataBundle, BitmapRegionDecoder regionDecoder) {
        if (null != jc && jc.isCancelled()) {
            Log.v(TAG, "retrieveLargeData:job cancelled");
            return;
        }

        if (null == params || null == regionDecoder) {
            Log.e(TAG,"retrieveLargeData:invalid parameters");
            return;
        }

        Rect imageRect = new Rect(0, 0, regionDecoder.getWidth(), 
                                        regionDecoder.getHeight());
        JpsHelper.adjustRect(layout, true, imageRect);

        Options options = new Options();
        options.inSampleSize = DecodeHelper.calculateSampleSize(
                                   DecodeHelper.MAX_BITMAP_BYTE_COUNT, -1,
                                   imageRect.right - imageRect.left,
                                   imageRect.bottom - imageRect.top);
        options.inPostProc = params.inPQEnhance;

        //decode original full frame if needed
        Bitmap firstFullBitmap = null;
        if (params.inOriginalFullFrame || params.inFirstFullFrame) {
            firstFullBitmap = DecodeHelper.safeDecodeImageRegion(
                                           jc, regionDecoder, imageRect, options);
            if (null != firstFullBitmap) {
                if (null != jc && jc.isCancelled()) {
                    Log.v(TAG, "retrieveLargeData:first:job cancelled");
                    firstFullBitmap.recycle();
                    firstFullBitmap = null;
                } else {
                    RegionDecoder tempDecoder =
                            DecodeHelper.getRegionDecoder(jc, firstFullBitmap, true);
                    if (params.inOriginalFullFrame) {
                        dataBundle.originalFullFrame = tempDecoder;
                    }
                    if (params.inFirstFullFrame) {
                        dataBundle.firstFullFrame = tempDecoder;
                    }
                }
            }
        }

        //decode second full frame if needed
        Bitmap secondFullBitmap = null;
        if (params.inSecondFullFrame) {
            //decode origin right picture
            imageRect.set(0, 0, regionDecoder.getWidth(), regionDecoder.getHeight());
            JpsHelper.adjustRect(layout, false, imageRect);
            secondFullBitmap = DecodeHelper.safeDecodeImageRegion(
                                           jc, regionDecoder, imageRect, options);
            if (null != secondFullBitmap) {
                if (null != jc && jc.isCancelled()) {
                    Log.v(TAG, "<retrieveLargeData> second full frame job cancelled");
                    secondFullBitmap.recycle();
                    secondFullBitmap = null;
                } else {
                    dataBundle.secondFullFrame =
                        DecodeHelper.getRegionDecoder(jc, secondFullBitmap, true);
                }
            }
        }
    }

    public void setMavListener(MavListener listener) {
        // TODO Auto-generated method stub
    }
}

