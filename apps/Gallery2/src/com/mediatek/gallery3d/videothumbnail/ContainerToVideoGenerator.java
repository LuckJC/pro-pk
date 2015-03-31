package com.mediatek.gallery3d.videothumbnail;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.mediatek.gallery3d.conshots.ContainerHelper;
import com.mediatek.gallery3d.videothumbnail.BitmapStreamToVideoGenerator;
import com.mediatek.gallery3d.videothumbnail.VideoThumbnailFeatureOption;


public class ContainerToVideoGenerator extends BitmapStreamToVideoGenerator{
    private static final String TAG = "Gallery2/ContainerToVideoGenerator";
    
    private int mTargetSize;
    private GalleryApp mApplication;
    private ArrayList<MediaItem>  mAnimationItems;
    
    public ContainerToVideoGenerator(GalleryApp app){
        mApplication = app;
    }
    
    @Override
    public void init(MediaItem item, int videoType, VideoConfig config/*in,out*/) {
        Log.d(TAG, " init");
        
        if (item != null && item.isContainer() && item.getFilePath() != null) {
            mAnimationItems = ContainerHelper.getAnimationArray(mApplication, item);
            if(mAnimationItems == null){
                config.frameCount = 0;
                return;
            }else{
                config.frameCount = mAnimationItems.size();
            }

            if (videoType == VTYPE_THUMB) {
                //mTargetSize = MediaItem.getTargetSize(MediaItem.TYPE_MICROTHUMBNAIL);
                mTargetSize = VideoThumbnailFeatureOption.CONTAINER_THUMBNAILVIDEO_TARGETSIZE;
                config.frameInterval = 1000/VideoThumbnailFeatureOption.CONTAINER_THUMBNAILVIDEO_FPS;
                config.bitRate = VideoThumbnailFeatureOption.CONTAINER_THUMBNAILVIDEO_BITRATE;
            } else {
                //mTargetSize = MediaItem.getTargetSize(MediaItem.TYPE_THUMBNAIL);
                mTargetSize = VideoThumbnailFeatureOption.CONTAINER_SHAREVIDEO_TARGETSIZE;
                config.frameInterval = 1000/VideoThumbnailFeatureOption.CONTAINER_SHAREVIDEO_FPS;
                config.bitRate = VideoThumbnailFeatureOption.CONTAINER_SHAREVIDEO_BITRATE;
            }
            Log.d(TAG, "videoType:"+videoType+" with:"+config.frameWidth+" height:"+config.frameHeight+" frameCount:"+config.frameCount);
        }
    }

    @Override
    public Bitmap getBitmapAtFrame(MediaItem item, int videoType,
            int frameIndex) {
        Bitmap bitmap;
        Bitmap outputBitmap = null;
        Log.d(TAG, "path:"+item.getPath()+" videoType:"+videoType+" frameIndex:"+frameIndex);

        try{
            bitmap = decoderBitmap(mAnimationItems.get(frameIndex).getFilePath(), videoType);
        }catch(Exception e){
            Log.d(TAG, "getBitmapAtFrame decoderBitmap exception");
            return null;
        }
        
        if(bitmap == null) return null;
        
        if (VTYPE_THUMB == videoType) {
            outputBitmap = BitmapUtils.resizeAndCropCenter(bitmap, mTargetSize, true);
        } else if(VTYPE_SHARE == videoType){
            outputBitmap = BitmapUtils.resizeDownBySideLength(bitmap, mTargetSize, true);
        }
        return outputBitmap;
    }
    
    private Bitmap decoderBitmap(String filePath, int videoType){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = BitmapUtils.computeSampleSizeLarger(
                options.outWidth, options.outHeight, mTargetSize);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }
    
    @Override
    public void deInit(MediaItem item, int videoType) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onCancelRequested(LocalMediaItem item, int videoType) {
        // TODO Auto-generated method stub
        
    }
}

