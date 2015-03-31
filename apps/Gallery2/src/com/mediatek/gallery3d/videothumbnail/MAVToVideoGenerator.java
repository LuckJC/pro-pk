package com.mediatek.gallery3d.videothumbnail;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.mediatek.gallery3d.mpo.MpoDecoderWrapper;

public class MAVToVideoGenerator extends BitmapStreamToVideoGenerator{
    private static final String TAG = "Gallery2/MAVToVideoGenerator";
    private static MpoDecoderWrapper mpoDecoderWrapper;
    private int mTargetSize;
    private int mFrameCount;
    private static BitmapFactory.Options sDecodeOptions = new BitmapFactory.Options();

    @Override
    public void init(MediaItem item, int videoType, VideoConfig config/*in,out*/) {
        if (item != null && item.getFilePath() != null) {
            mpoDecoderWrapper = MpoDecoderWrapper.createMpoDecoderWrapper(item.getFilePath());
            if (mpoDecoderWrapper == null) {
                config.frameCount = -1;
                return;
            }
            mFrameCount = mpoDecoderWrapper.frameCount(); 
            config.frameCount = mFrameCount * 2 - 1;
            if (videoType == VTYPE_THUMB) {
                //mTargetSize = MediaItem.getTargetSize(MediaItem.TYPE_MICROTHUMBNAIL);
                mTargetSize = VideoThumbnailFeatureOption.MAV_THUMBNAILVIDEO_TARGETSIZE;
                config.frameInterval = 1000/VideoThumbnailFeatureOption.MAV_THUMBNAILVIDEO_FPS;
                config.bitRate = VideoThumbnailFeatureOption.MAV_THUMBNAILVIDEO_BITRATE;
            } else {
                //mTargetSize = MediaItem.getTargetSize(MediaItem.TYPE_THUMBNAIL);
                mTargetSize = VideoThumbnailFeatureOption.MAV_SHAREVIDEO_TARGETSIZE;
                config.frameInterval = 1000/VideoThumbnailFeatureOption.MAV_SHAREVIDEO_FPS;
                config.bitRate = VideoThumbnailFeatureOption.MAV_SHAREVIDEO_BITRATE;
            }
            sDecodeOptions.inSampleSize = BitmapUtils.computeSampleSizeLarger(mpoDecoderWrapper.width(),
                    mpoDecoderWrapper.height(), mTargetSize);
            Log.d(TAG, "init,width:" + mpoDecoderWrapper.width() + ",height:" + mpoDecoderWrapper.height() + 
                    ",targetSize:" + mTargetSize);
            Log.d(TAG, "videoType:" + videoType + ",sampleSize:" + sDecodeOptions.inSampleSize);
        }
    }

    @Override
    public Bitmap getBitmapAtFrame(MediaItem item, int videoType,
            int frameIndex) {
        Bitmap outputBitmap = null;
        int curIndex = frameIndex < mFrameCount ? frameIndex : (mFrameCount*2-2-frameIndex);
        if (mpoDecoderWrapper == null) {
            return null;
        }
        Bitmap bitmap = mpoDecoderWrapper.frameBitmap(curIndex, sDecodeOptions);
        if (bitmap == null) {
            return null;
        }
        if (VTYPE_THUMB == videoType) {
            outputBitmap = BitmapUtils.resizeAndCropCenter(bitmap, mTargetSize, true);
        } else if(VTYPE_SHARE == videoType){
            outputBitmap = BitmapUtils.resizeDownBySideLength(bitmap, mTargetSize, true);
        }
        return outputBitmap;
    }

    @Override
    public void deInit(MediaItem item, int videoType) {
        if (mpoDecoderWrapper != null) {
            mpoDecoderWrapper.close();
        }
    }

    @Override
    public void onCancelRequested(LocalMediaItem item, int videoType) {
        // TODO Auto-generated method stub
    }
}
