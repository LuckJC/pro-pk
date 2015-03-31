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

import java.math.BigInteger;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool.Job;

// M: mediatek import
import com.mediatek.gallery3d.panorama.PanoramaHelper;
import com.mediatek.gallery3d.util.MediatekFeature;

// MediaItem represents an image or a video item.
public abstract class MediaItem extends MediaObject {
    private static final String TAG = "Gallery2/MediaItem";
    // NOTE: These type numbers are stored in the image cache, so it should not
    // not be changed without resetting the cache.
    public static final int TYPE_THUMBNAIL = 1;
    public static final int TYPE_MICROTHUMBNAIL = 2;
    /// M: added for ConShots
    public static final int TYPE_MOTIONTHUMBNAIL = 3;
    
    public static final int THUMBNAIL_TARGET_SIZE = 640;
    public static final int MICROTHUMBNAIL_TARGET_SIZE = 200;
    public static final int CACHED_IMAGE_QUALITY = 95;

    public static final int IMAGE_READY = 0;
    public static final int IMAGE_WAIT = 1;
    public static final int IMAGE_ERROR = -1;

    /// M: added for ConShots @{
    // not mark best shot yet
    public static final int IMAGE_BEST_SHOT_NOT_MARK = 0;
    // has marked best shot, but mark as false
    public static final int IMAGE_BEST_SHOT_MARK_FALSE = 1;
    // has marked best shot, but mark as true
    public static final int IMAGE_BEST_SHOT_MARK_TRUE = 2;
    /// @}
    public static final String MIME_TYPE_JPEG = "image/jpeg";

    private static final int BYTESBUFFE_POOL_SIZE = 4;
    private static final int BYTESBUFFER_SIZE = 200 * 1024;

    private static int sMicrothumbnailTargetSize = 200;
    private static final BytesBufferPool sMicroThumbBufferPool =
            new BytesBufferPool(BYTESBUFFE_POOL_SIZE, BYTESBUFFER_SIZE);

    /// M: private -> protected (I use this var in sub class, to avoid frequent use of getTargetSize()
    protected/*private*/ static int sThumbnailTargetSize = 640;

    // TODO: fix default value for latlng and change this.
    public static final double INVALID_LATLNG = 0f;

    public abstract Job<Bitmap> requestImage(int type);
    public abstract Job<BitmapRegionDecoder> requestLargeImage();

    // M: added for mediatek feature.
    public Job<MediatekFeature.DataBundle> requestImage(int type, 
                           MediatekFeature.Params params) {return null;}

    public MediaItem(Path path, long version) {
        super(path, version);
    }

    public long getDateInMs() {
        return 0;
    }

    public String getName() {
        return null;
    }

    public void getLatLong(double[] latLong) {
        latLong[0] = INVALID_LATLNG;
        latLong[1] = INVALID_LATLNG;
    }

    public String[] getTags() {
        return null;
    }

    public Face[] getFaces() {
        return null;
    }

    // The rotation of the full-resolution image. By default, it returns the value of
    // getRotation().
    public int getFullImageRotation() {
        return getRotation();
    }

    public int getRotation() {
        return 0;
    }

    public long getSize() {
        return 0;
    }

    public abstract String getMimeType();

    public String getFilePath() {
        return "";
    }

    // Returns width and height of the media item.
    // Returns 0, 0 if the information is not available.
    public abstract int getWidth();
    public abstract int getHeight();

    // This is an alternative for requestImage() in PhotoPage. If this
    // is implemented, you don't need to implement requestImage().
    public ScreenNail getScreenNail() {
        return null;
    }

    public static int getTargetSize(int type) {
        switch (type) {
            case TYPE_THUMBNAIL:
                return sThumbnailTargetSize;
            case TYPE_MICROTHUMBNAIL:
            /// M: added for ConShots
            case TYPE_MOTIONTHUMBNAIL:
                return sMicrothumbnailTargetSize;
            default:
                throw new RuntimeException(
                    "should only request thumb/microthumb from cache");
        }
    }

    public static BytesBufferPool getBytesBufferPool() {
        return sMicroThumbBufferPool;
    }

    public static void setThumbnailSizes(int size, int microSize) {
        sThumbnailTargetSize = size;
        ///M: for 720p resolution, if thumbnail size equal 256, 
        //thumbnail will be devide into four tiles, performance will be degrade
        if (sMicrothumbnailTargetSize != microSize) {
            sMicrothumbnailTargetSize = ((microSize % 256 == 0) ? microSize - 2 * TiledTexture.BORDER_SIZE : microSize);
        }
        Log.i(TAG, "MediaItem setThumbnailSizes " + sThumbnailTargetSize + " sMicrothumbnailTargetSize " + sMicrothumbnailTargetSize);
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Mediatek added features
    ////////////////////////////////////////////////////////////////////////////

    public int getSubType() {
        return 0;
    }

    //added for Stereo Display
    public int getStereoLayout() {
        return 0;
    }

    public boolean isDrm() {
        return false;
    }

    public boolean isDrmMethod(int method) {
        return false;
    }

    public boolean hasDrmRights() {
        return false;
    }

    public int drmRights(int action) {
        //return 0 to decouple from DrmStore, be careful in the future.
        return 0;
    }

    public boolean isTimeInterval() {
        return false;
    }
    
    public abstract void setMavListener(MavListener listener);

    public int getConvergence() {
        return -1;
    }

    public boolean getIsMtkS3D() {
        return false;
    }
    /// M: added for ConShots @{
    public long getGroupId() {
        return 0;
    }

    public int getGroupIndex() {
        return 0;
    }
    
    public BigInteger getFocusValue() {
        return null;
    }
    
    public int getIsBestShot() {
        return IMAGE_BEST_SHOT_MARK_FALSE;
    }
    
    public void setIsBestShot(int isBestShort) {
        return;
    }
    
    public boolean isConShot() {
        return false;
    }
    
    public boolean isMotion() {
        return false;
    }

    public boolean isContainer(){
        return false;
    }
    
    public boolean isDisabled(){
        return false;
    }
    
    public void setDisable(boolean isDisable){
        
    }
    
    public MediaSet getRelatedMediaSet(){
        return null;
    }
    /// @}
    // M: 6592 panorama add @{
    public boolean isPanorama() {
        if (getHeight() <= 0 || getWidth() <= 0)
            return false;
        int ratio = 0;
        String mimetype = getMimeType();
        if (getRotation() == 90 || getRotation() == 270) {
            ratio = getHeight() / getWidth();
        } else {
            ratio = getWidth() / getHeight();
        }
        return (ratio >= PanoramaHelper.PANORAMA_ASPECT_RATIO_MIN
                && ratio <= PanoramaHelper.PANORAMA_ASPECT_RATIO_MAX
                && !mimetype.equals("image/gif") 
                && !mimetype.equals("image/mpo") 
                && !mimetype.equals("image/x-jps"));
    }
    
    public boolean isVerticalPanorama() {
        if (getHeight() <= 0 || getWidth() <= 0)
            return false;
        int ratio = 0;
        String mimetype = getMimeType();
        if (getRotation() == 90 || getRotation() == 270) {
            ratio = getWidth() / getHeight();
        } else {
            ratio = getHeight() / getWidth();
        }
        return (ratio >= PanoramaHelper.PANORAMA_ASPECT_RATIO_MIN
                && ratio <= PanoramaHelper.PANORAMA_ASPECT_RATIO_MAX
                && !mimetype.equals("image/gif") 
                && !mimetype.equals("image/mpo") 
                && !mimetype.equals("image/x-jps"));
    }
    // @}
}
