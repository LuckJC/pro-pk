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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Trace;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.PanoramaMetadataSupport;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.filtershow.tools.SaveImage;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.util.UpdateHelper;

import java.io.File;
import java.io.IOException;

// M: MTK import
import java.io.FileNotFoundException;

import android.content.Context;
import com.mediatek.drm.OmaDrmStore;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Video;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel.MapMode;

import com.mediatek.common.mpodecoder.IMpoDecoder;

import com.mediatek.gallery3d.data.RequestHelper;
import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.mpo.MavPlayer;
import com.mediatek.gallery3d.mpo.MpoHelper;
import com.mediatek.gallery3d.panorama.PanoramaHelper;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.videothumbnail.AbstractVideoPlayer;
import com.mediatek.gallery3d.videothumbnail.MAVToVideoGenerator;
import com.mediatek.gallery3d.videothumbnail.OrdinaryVideoPlayer;
import com.mediatek.gallery3d.videothumbnail.PanoramaToVideoGenerator;
import com.mediatek.gallery3d.videothumbnail.PanoramaVideoPlayer;

// LocalImage represents an image in the local storage.
public class LocalImage extends LocalMediaItem {
    private static final String TAG = "Gallery2/LocalImage";

    static final Path ITEM_PATH = Path.fromString("/local/image/item");

    public static Path getItemPath(int mtkInclusion) {
        if (0 != mtkInclusion) {
            // M: retrieve path with respect to mtk inclusion
            return Path.fromString("/local/image/item", mtkInclusion);
        } else {
            return ITEM_PATH;
        }
    }

    //added to support Mediatek features
    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();
    private static final boolean mIsMpoSupported = 
                                          MediatekFeature.isMpoSupported();
    private static final boolean mIsStereoDisplaySupported = 
                                          MediatekFeature.isStereoDisplaySupported();
    private static final boolean mIsDisplay2dAs3dSupported = 
            MediatekFeature.isDisplay2dAs3dSupported();
    private static final boolean mIsGifAnimationSupported = 
            MediatekFeature.isGifAnimationSupported();
    private static final boolean mIsMavSupported = 
        MediatekFeature.isMAVSupported();
    private static final boolean mIsMavThumbnailSupported = 
        MediatekFeature.isMAVThumbnailSupport();

    //added to avoid decode bitmap many times
    private String mSniffedMimetype;
    public int mMpoSubType = -1;
    private boolean mStereoDimAdjusted = false;
    //added for manual convergence
    public int mConvergence = -1;
    public boolean mIsMtk3D = false;
    
    // Must preserve order between these indices and the order of the terms in
    // the following PROJECTION array.
    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_MIME_TYPE = 2;
    private static final int INDEX_LATITUDE = 3;
    private static final int INDEX_LONGITUDE = 4;
    private static final int INDEX_DATE_TAKEN = 5;
    private static final int INDEX_DATE_ADDED = 6;
    private static final int INDEX_DATE_MODIFIED = 7;
    private static final int INDEX_DATA = 8;
    private static final int INDEX_ORIENTATION = 9;
    private static final int INDEX_BUCKET_ID = 10;
    private static final int INDEX_SIZE = 11;
    private static final int INDEX_WIDTH = 12;
    private static final int INDEX_HEIGHT = 13;
    //added to support DRM
    private static final int INDEX_IS_DRM = 14;
    private static final int INDEX_DRM_METHOD = 15;
    //added to support Stereo display
    private static final int INDEX_MPO_SUB_TYPE = 16;
    //added to support manual convergence
    private static final int INDEX_CONVERGENCE = 17;
    //added to 3D picture captured by mtk camera
    private static final int INDEX_IS_MTK_3D = 24;
    
    /// M: added for ConShots
    protected static final int INDEX_GROUP_ID = 18;
    protected static final int INDEX_GROUP_INDEX = 19;
    protected static final int INDEX_FOCUS_VALUE_HIGH = 20;
    protected static final int INDEX_FOCUS_VALUE_LOW = 21;
    protected static final int INDEX_IS_BEST_SHOT = 22;
    protected static final int INDEX_GROUP_COUNT = 23;

    /// M: modify to public for ConShots
    public static final String[] PROJECTION =  {
            ImageColumns._ID,           // 0
            ImageColumns.TITLE,         // 1
            ImageColumns.MIME_TYPE,     // 2
            ImageColumns.LATITUDE,      // 3
            ImageColumns.LONGITUDE,     // 4
            ImageColumns.DATE_TAKEN,    // 5
            ImageColumns.DATE_ADDED,    // 6
            ImageColumns.DATE_MODIFIED, // 7
            ImageColumns.DATA,          // 8
            ImageColumns.ORIENTATION,   // 9
            ImageColumns.BUCKET_ID,     // 10
            ImageColumns.SIZE,          // 11
            "0",         // 12
            "0",         // 13
            //added to support DRM
            ImageColumns.IS_DRM,    //14
            ImageColumns.DRM_METHOD,    //15
            //added to support Stereo display
            Images.Media.MPO_TYPE,  //16
            Images.Media.CONVERGENCE,   //17
            /// M: added for ConShots
            Images.Media.GROUP_ID,      // 18
            Images.Media.GROUP_INDEX,    //19
            Images.Media.FOCUS_VALUE_HIGH,    //20
            Images.Media.FOCUS_VALUE_LOW,    //21
            Images.Media.IS_BEST_SHOT,    //22
            Images.Media.GROUP_COUNT,    //23
            Images.Media.IS_MTK_3D    //24
    };
    
    static {
        updateWidthAndHeightProjection();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void updateWidthAndHeightProjection() {
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            PROJECTION[INDEX_WIDTH] = MediaColumns.WIDTH;
            PROJECTION[INDEX_HEIGHT] = MediaColumns.HEIGHT;
        }
    }

    /// M: modify to protected for ConShots
    protected GalleryApp mApplication;

    public int rotation;
    
    private PanoramaMetadataSupport mPanoramaMetadata = new PanoramaMetadataSupport(this);

    public LocalImage(Path path, GalleryApp application, Cursor cursor) {
        super(path, nextVersionNumber());
        mApplication = application;
        loadFromCursor(cursor);
    }

    public LocalImage(Path path, GalleryApp application, int id) {
        super(path, nextVersionNumber());
        mApplication = application;
        ContentResolver resolver = mApplication.getContentResolver();
        Uri uri = Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = LocalAlbum.getItemCursor(resolver, uri, PROJECTION, id);
        if (cursor == null) {
            throw new RuntimeException("cannot get cursor for: " + path);
        }
        try {
            if (cursor.moveToNext()) {
                loadFromCursor(cursor);
            } else {
                throw new RuntimeException("cannot find data for: " + path);
            }
        } finally {
            cursor.close();
        }
    }
    /// M: Modify to protected for ConShots
    protected void loadFromCursor(Cursor cursor) {
        id = cursor.getInt(INDEX_ID);
        caption = cursor.getString(INDEX_CAPTION);
        mimeType = cursor.getString(INDEX_MIME_TYPE);
        latitude = cursor.getDouble(INDEX_LATITUDE);
        longitude = cursor.getDouble(INDEX_LONGITUDE);
        dateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        //added to avoid fake data changed judgement in
        //updateFromCursor function-begin
        dateAddedInSec = cursor.getLong(INDEX_DATE_ADDED);
        dateModifiedInSec = cursor.getLong(INDEX_DATE_MODIFIED);
        //end
        filePath = cursor.getString(INDEX_DATA);
        rotation = cursor.getInt(INDEX_ORIENTATION);
        bucketId = cursor.getInt(INDEX_BUCKET_ID);
        fileSize = cursor.getLong(INDEX_SIZE);
        width = cursor.getInt(INDEX_WIDTH);
        height = cursor.getInt(INDEX_HEIGHT);

        //added to support drm feature
        if (mIsDrmSupported) {
            is_drm = cursor.getInt(INDEX_IS_DRM);
            drm_method = cursor.getInt(INDEX_DRM_METHOD);
        }
        //added to support stereo display feature
        if (mIsStereoDisplaySupported) {
            mConvergence = cursor.getInt(INDEX_CONVERGENCE);
            mStereoDimAdjusted = false;
            mIsMtk3D = cursor.getInt(INDEX_IS_MTK_3D) == 1 ? true : false;
        }
        mMpoSubType = cursor.getInt(INDEX_MPO_SUB_TYPE);
        // M: added for stereo display
        //stereoType = cursor.getInt(INDEX_STEREO_TYPE);
    }

    @Override
    protected boolean updateFromCursor(Cursor cursor) {
        UpdateHelper uh = new UpdateHelper();
        id = uh.update(id, cursor.getInt(INDEX_ID));
        caption = uh.update(caption, cursor.getString(INDEX_CAPTION));
        mimeType = uh.update(mimeType, cursor.getString(INDEX_MIME_TYPE));
        latitude = uh.update(latitude, cursor.getDouble(INDEX_LATITUDE));
        longitude = uh.update(longitude, cursor.getDouble(INDEX_LONGITUDE));
        dateTakenInMs = uh.update(
                dateTakenInMs, cursor.getLong(INDEX_DATE_TAKEN));
        dateAddedInSec = uh.update(
                dateAddedInSec, cursor.getLong(INDEX_DATE_ADDED));
        dateModifiedInSec = uh.update(
                dateModifiedInSec, cursor.getLong(INDEX_DATE_MODIFIED));
        filePath = uh.update(filePath, cursor.getString(INDEX_DATA));
        rotation = uh.update(rotation, cursor.getInt(INDEX_ORIENTATION));
        bucketId = uh.update(bucketId, cursor.getInt(INDEX_BUCKET_ID));
        fileSize = uh.update(fileSize, cursor.getLong(INDEX_SIZE));

        if (!mIsStereoDisplaySupported ||
            !StereoHelper.JPS_MIME_TYPE.equalsIgnoreCase(mimeType)) {
            width = uh.update(width, cursor.getInt(INDEX_WIDTH));
            height = uh.update(height, cursor.getInt(INDEX_HEIGHT));
        } else {
            //for jps file, we do not need to update, cause we
            //ourselves will changeit privately
            width = cursor.getInt(INDEX_WIDTH);
            height = cursor.getInt(INDEX_HEIGHT);
        }

        //added to support drm feature
        if (mIsDrmSupported) {
            is_drm = uh.update(is_drm,cursor.getInt(INDEX_IS_DRM));
            drm_method = uh.update(drm_method,cursor.getInt(INDEX_DRM_METHOD));
        }
        //added to support stereo display feature
        if (mIsStereoDisplaySupported) {
            mConvergence = cursor.getInt(INDEX_CONVERGENCE);
            mStereoDimAdjusted = false;
        }
        mMpoSubType = cursor.getInt(INDEX_MPO_SUB_TYPE);
        // M: added for stereo display
        //stereoType = uh.update(stereoType, cursor.getInt(INDEX_STEREO_TYPE));
        /// M: vtsp - thumbnail play type may change after updating @{
        // e.g. a paranorma may become a normal image after rotation;
        // a container image may become a single image after deletion; etc.
        if (uh.isUpdated()) {
            invalidateThumbnailPlayType();
        }
        /// @}
        return uh.isUpdated();
    }

    @Override
    public Job<Bitmap> requestImage(int type) {
        return new LocalImageRequest(mApplication, mPath, dateModifiedInSec,
                type, filePath);
    }

    public static class LocalImageRequest extends ImageCacheRequest {
        private String mLocalFilePath;

        /// added public for ConShots
        public LocalImageRequest(GalleryApp application, Path path, long timeModified,
                int type, String localFilePath) {
            super(application, path, timeModified, type,
                    MediaItem.getTargetSize(type));
            mLocalFilePath = localFilePath;
        }

        @Override
        public Bitmap onDecodeOriginal(JobContext jc, final int type) {
            if (null == mLocalFilePath) {
                Log.w(TAG,"onDecodeOriginal:got null mLocalFilePath");
                return null;
            }
            Log.d(TAG, "onDecodeOriginal,type:"+type);
            Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>LocalImage-onDecodeOriginal");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            int targetSize = MediaItem.getTargetSize(type);

            // M: for picture quality enhancement
            MediatekFeature.enablePictureQualityEnhance(options, true);

            // M: 6592 panorama add @{
            if (MediatekFeature.isPanorama3DSupported()) {
                MediaItem item = (MediaItem)(mApplication.getDataManager().getMediaObject(mPath));
                if (item != null && item.isPanorama() && type == MediaItem.TYPE_MICROTHUMBNAIL) {
                    return PanoramaHelper.getPanoramaMicroThumbnail(item, jc);
                }
            }
            // @}
            
            // try to decode from JPEG EXIF
            if (type == MediaItem.TYPE_MICROTHUMBNAIL) {
                Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>LocalImage-onDecodeOriginal-decodeFromExif");
                Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>LocalImage-new ExifInterface");
                ExifInterface exif = new ExifInterface();
                Trace.traceEnd(Trace.TRACE_TAG_APP);
                byte[] thumbData = null;
                try {
                    Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>LocalImage-exif.readExif");
                    exif.readExif(mLocalFilePath);
                    Trace.traceEnd(Trace.TRACE_TAG_APP);
                    Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>LocalImage-exif.getThumbnail");
                    thumbData = exif.getThumbnail();
                    Trace.traceEnd(Trace.TRACE_TAG_APP);
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "failed to find file to read thumbnail: " + mLocalFilePath);
                } catch (IOException e) {
                    Log.w(TAG, "failed to get thumbnail from: " + mLocalFilePath);
                }
                if (thumbData != null) {
                    Bitmap bitmap = DecodeUtils.decodeIfBigEnough(
                            jc, thumbData, options, targetSize);
                    if (bitmap != null) {
                        Trace.traceEnd(Trace.TRACE_TAG_APP);
                        Trace.traceEnd(Trace.TRACE_TAG_APP);
                        return bitmap;
                    }
                }
                Trace.traceEnd(Trace.TRACE_TAG_APP);
            }

            Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>LocalImage-onDecodeOriginal-decodeOriginEx");
            Bitmap bitmap = decodeOriginEx(jc, mApplication, mLocalFilePath, 
                    type, options, targetSize);
            Trace.traceEnd(Trace.TRACE_TAG_APP);
            if (null != bitmap) {
                //replace gif background if needed
                Trace.traceEnd(Trace.TRACE_TAG_APP);
                return MediatekFeature.replaceGifBackGround(bitmap, mLocalFilePath);
            }

            Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>LocalImage-onDecodeOriginal-decodeThumbnail");
            bitmap = DecodeUtils.decodeThumbnail(
                  jc, mLocalFilePath, options, targetSize, type);
            Trace.traceEnd(Trace.TRACE_TAG_APP);
            //replace gif background if needed
            bitmap = MediatekFeature.replaceGifBackGround(bitmap, mLocalFilePath);

            Trace.traceEnd(Trace.TRACE_TAG_APP);
            return DecodeUtils.ensureGLCompatibleBitmap(bitmap);
        }
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
        return new LocalLargeImageRequest(filePath);
    }

    public static class LocalLargeImageRequest
            implements Job<BitmapRegionDecoder> {
        String mLocalFilePath;

        public LocalLargeImageRequest(String localFilePath) {
            mLocalFilePath = localFilePath;
        }

        @Override
        public BitmapRegionDecoder run(JobContext jc) {
            return DecodeUtils.createBitmapRegionDecoder(jc, mLocalFilePath, false);
        }
    }

    @Override
    public int getSupportedOperations() {
        int operation = SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_CROP
                | SUPPORT_SETAS | SUPPORT_PRINT | SUPPORT_INFO;
        /// M: For drm format image, can not get BitmapRegionDecoder Instance.
        if (BitmapUtils.isSupportedByRegionDecoder(mimeType) && !isDrm()) {
            operation |= SUPPORT_FULL_IMAGE | SUPPORT_EDIT;
            // if current item is mpo_mav, don't support full image display
            if(MediatekFeature.MIMETYPE_MPO.equals(mimeType) && 
                    getSubType() == MediaObject.SUBTYPE_MPO_MAV) {
                MtkLog.v(TAG, "current type is mpo_mav, don't support full image, path: " + filePath);
                operation &= ~SUPPORT_FULL_IMAGE; 
            } else if (mimeType.endsWith("bmp")) {
                // if current item is bmp file and it's not bigger than thumbnail size,
                // don't support full image display
                if (width <= sThumbnailTargetSize && height <= sThumbnailTargetSize) {
                    operation &= ~SUPPORT_FULL_IMAGE;
                }
            }
        }

        if (BitmapUtils.isRotationSupported(mimeType)) {
            operation |= SUPPORT_ROTATE;
        }

        if (GalleryUtils.isValidLocation(latitude, longitude)) {
            operation |= SUPPORT_SHOW_ON_MAP;
        }

        operation = getModifiedOperations(operation);
      
        return operation;
    }
    
    @Override
    public void getPanoramaSupport(PanoramaSupportCallback callback) {
        mPanoramaMetadata.getPanoramaSupport(mApplication, callback);
    }

    @Override
    public void clearCachedPanoramaSupport() {
        mPanoramaMetadata.clearCachedValues();
    }

    @Override
    public void delete() {
        GalleryUtils.assertNotInRenderThread();
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = mApplication.getContentResolver();
      ///M : delete image.
        GalleryUtils.deleteItems(contentResolver, baseUri, "_id=?", new String[]{String.valueOf(id)});
        SaveImage.deleteAuxFiles(contentResolver, getContentUri());
        
        contentResolver.delete(baseUri, "_id=?",
                new String[]{String.valueOf(id)});
        ///M:
        mApplication.getDataManager().broadcastUpdatePicture();
    }

    @Override
    public void rotate(int degrees) {
        GalleryUtils.assertNotInRenderThread();
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        ContentValues values = new ContentValues();
        int rotation = (this.rotation + degrees) % 360;
        if (rotation < 0) rotation += 360;

        if (mimeType.equalsIgnoreCase("image/jpeg")) {
            ExifInterface exifInterface = new ExifInterface();
            ExifTag tag = exifInterface.buildTag(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.getOrientationValueForRotation(rotation));
            if(tag != null) {
                exifInterface.setTag(tag);
                try {
                    exifInterface.forceRewriteExif(filePath);
                    fileSize = new File(filePath).length();
                    values.put(Images.Media.SIZE, fileSize);
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "cannot find file to set exif: " + filePath);
                } catch (IOException e) {
                    Log.w(TAG, "cannot set exif data: " + filePath);
                }
            } else {
                Log.w(TAG, "Could not build tag: " + ExifInterface.TAG_ORIENTATION);
            }
        }

        values.put(Images.Media.ORIENTATION, rotation);
        // M: 6592 panorama add @{
        // after rotate, panorama image may become non-panorama image, 
        // non-panorama image may become panorama image. For one picture, 
        // after rotate, its thumbnail may change, so we delete data in cache,
        // so that when get its image micro-thumbnail again, we will decode from origin file.  
        if (MediatekFeature.isPanorama3DSupported()) {
            boolean isPanorama = isPanorama();
            boolean isVerticalPanorama = isVerticalPanorama();
            if (isPanorama || isVerticalPanorama) {
                //values.put(ImageColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000);
                mApplication.getImageCacheService().clearImageData(mPath, dateModifiedInSec, MediaItem.TYPE_MICROTHUMBNAIL);
            }
        }
        // @}
        
        mApplication.getContentResolver().update(baseUri, values, "_id=?",
                new String[]{String.valueOf(id)});
        ///M:rotate picture, need update camera thumbnail
        mApplication.getDataManager().broadcastUpdatePicture();
    }

    @Override
    public Uri getContentUri() {
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        return baseUri.buildUpon().appendPath(String.valueOf(id)).build();
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_IMAGE;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        details.addDetail(MediaDetails.INDEX_ORIENTATION, Integer.valueOf(rotation));
        if (MIME_TYPE_JPEG.equals(mimeType)) {
            // ExifInterface returns incorrect values for photos in other format.
            // For example, the width and height of an webp images is always '0'.
            MediaDetails.extractExifInfo(details, filePath);
        }
        // M: add width/height to details if non are available from exif
        attachMtkDetails(details);
        return details;
    }

    @Override
    public int getRotation() {
        return rotation;
    }

    @Override
    public int getWidth() {
        //this is added to support stereo display, because the logical dimension of
        //jps/pns image is not what it actually is!
        adjustDimIfNeeded();

        return width;
    }

    @Override
    public int getHeight() {
        //this is added to support stereo display, because the logical dimension of
        //jps/pns image is not what it actually is!
        adjustDimIfNeeded();

        return height;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Mediatek added features
    ////////////////////////////////////////////////////////////////////////////

    private static Bitmap decodeOriginEx(JobContext jc, GalleryApp application,
            String filePath, int type, BitmapFactory.Options options, int targetSize) {
        Params params = new Params();
        params.inOriginalFrame = true;
        params.inType = type;
        params.inOriginalTargetSize = targetSize;
        params.inPQEnhance = options.inPostProc;
        DataBundle dataBundle = RequestHelper.requestDataBundle(jc, params,
                                    (Context)application, filePath, false);
        Bitmap bitmap = dataBundle != null ? dataBundle.originalFrame : null;
        return DecodeUtils.ensureGLCompatibleBitmap(bitmap);
    }

    @Override
    public Job<MediatekFeature.DataBundle> 
            requestImage(int type, Params params) {
        return new LocalImageRequestEx(
                       mApplication, mPath, type, mimeType, filePath, params);
    }

    public class LocalImageRequestEx implements 
                                         Job<MediatekFeature.DataBundle> {
        private GalleryApp mApplication;
        private Path mPath;
        private int mType;
        private int mTargetSize;
        private String mMimeType;
        private String mLocalFilePath;
        private Params mParams;

        LocalImageRequestEx(GalleryApp application, Path path, int type,
                            String mimeType, String localFilePath, Params params) {
            mApplication = application;
            mPath = path;
            mType = type;
            mTargetSize = getTargetSize(type);
            mMimeType = mimeType;
            mLocalFilePath = localFilePath;
            mParams = params;
        }

        public MediatekFeature.DataBundle run(JobContext jc) {
            if (null == mLocalFilePath || null == mParams) {
                Log.w(TAG,"LocalImageRequestEx:got null mLocalFilePath or mParams");
                return null;
            }
            MediatekFeature.DataBundle dataBundle = null;
            if((getSubType() == MediaObject.SUBTYPE_MPO_MAV) && mMavListener != null) {
                dataBundle = RequestHelper.requestDataBundle(jc, mParams, 
                        mLocalFilePath, mMimeType, mMavListener);
            } else {
                dataBundle =  RequestHelper.requestDataBundle(jc, mParams, 
                                                   mLocalFilePath, mMimeType);
            }
            if(null != dataBundle && null != dataBundle.originalFrame) {
                dataBundle.originalFrame = MediatekFeature.replaceGifBackGround(dataBundle.originalFrame, mLocalFilePath);
            }
            return dataBundle;
        }
    }

    //added for Stereo Display
    public int getStereoLayout() {
        if (MpoHelper.MPO_MIME_TYPE.equalsIgnoreCase(mimeType)
                && getSubType() != MediaObject.SUBTYPE_MPO_MAV) {
            return StereoHelper.STEREO_LAYOUT_FULL_FRAME;
        } else if (StereoHelper.JPS_MIME_TYPE.equalsIgnoreCase(mimeType)) {
            //now we ignore the possibility that the image is top and bottom layout
            return StereoHelper.STEREO_LAYOUT_LEFT_AND_RIGHT;
        } else {
            return StereoHelper.STEREO_LAYOUT_NONE;
        }
    }

    @Override
    public boolean hasDrmRights() {
         return OmaDrmStore.RightsStatus.RIGHTS_VALID == 
                DrmHelper.checkRightsStatus(mApplication.getAndroidContext(), 
                                            filePath, OmaDrmStore.Action.DISPLAY);
    }

    @Override
    public int drmRights(int action) {
         return DrmHelper.checkRightsStatus(mApplication.getAndroidContext(),
                                            filePath, action);
    }

    @Override
    public boolean isTimeInterval() {
         return DrmHelper.isTimeIntervalMedia(mApplication.getAndroidContext(), 
                                            filePath, OmaDrmStore.Action.DISPLAY);
    }

//    private void initMpoSubType() {
//        if (0 >= mMpoSubType) {
//            ContentResolver resolver = mApplication.getContentResolver();
//            MpoDecoder mpoDecoder = MpoDecoder.decodeFile(filePath);
//            if (null != mpoDecoder) {
//                mMpoSubType = mpoDecoder.suggestMtkMpoType();
//                mpoDecoder.close();
//            }
//        }
//    }

    private void adjustDimIfNeeded() {
        if (StereoHelper.JPS_MIME_TYPE.equalsIgnoreCase(mimeType) &&
            mIsStereoDisplaySupported && !mStereoDimAdjusted) {
            int layout = getStereoLayout();
            width = StereoHelper.adjustDim(true, layout, width);
            height = StereoHelper.adjustDim(false, layout, height);
            mStereoDimAdjusted = true;
        }
    }

    public int getSubType() {
        int subType = 0;
        // MPO image has the same supported operations as video
        if (MpoHelper.MPO_MIME_TYPE.equalsIgnoreCase(mimeType)) {
//            initMpoSubType();
            if (IMpoDecoder.MTK_TYPE_MAV == mMpoSubType) {
                subType |= SUBTYPE_MPO_MAV;
            } else if (IMpoDecoder.MTK_TYPE_Stereo == mMpoSubType){
                subType |= SUBTYPE_MPO_3D;
            }else if (IMpoDecoder.MTK_TYPE_3DPan == mMpoSubType){
                //subType |= MediaObject.SUBTYPE_MPO_3D_PAN;
                //as 3D panorama is currently not supported,
                //perceive all 3D panorama as 3D stereo photo
                subType |= SUBTYPE_MPO_3D;
            }
        }

        // JPS stereo image has to be treated specially
        if (StereoHelper.JPS_MIME_TYPE.equalsIgnoreCase(mimeType)) {
            if (mIsStereoDisplaySupported) {
                subType |= MediaObject.SUBTYPE_STEREO_JPS;
            }
        }

        //when show drm media whose type is not FL, show extra lock
        if (mIsDrmSupported && isDrm() &&
            OmaDrmStore.DrmMethod.METHOD_FL != drm_method) {
            if (!hasDrmRights()) {
                subType |= SUBTYPE_DRM_NO_RIGHT;
            } else {
                subType |= SUBTYPE_DRM_HAS_RIGHT;
            }
        }

        // we think of specified size image as special image
        // such as 1024 x 1, 1600 x 1
        boolean isSpecialImage = false;
        int w = getWidth();
        int h = getHeight();
        int scale = Math.max(w, h) / THUMBNAIL_TARGET_SIZE;
        if(scale != 0 && (w/scale == 0 || h/scale==0)) {
            isSpecialImage =  true;
            MtkLog.d(TAG, "is special image, w: " + w + ", h: " + h);
        }
        
        // display image at its origin size, MAV and special image 
        // is not regarded as normal image
        if (MediatekFeature.preferDisplayOriginalSize() &&
            0 == (subType & SUBTYPE_MPO_MAV) && !isSpecialImage) {
            MtkLog.d(TAG, "getWidth: " + getWidth() + ", getHeight: " + getHeight());
            subType |= SUBTYPE_ORIGIN_SIZE;
        }
        
        return subType;
    }

    public int getConvergence() {
        return mConvergence;
    }

    public boolean getIsMtkS3D() {
        return mIsMtk3D;
    }

    // M: added for all mediatek added feature
    private int getModifiedOperations(int operation) {
        //image support print by default
            operation |= SUPPORT_PRINT;
        //modify supported operation according to image capacity
        operation = modifyForImageType(operation);

        //modify supported operation according to drm protection
        operation = modifyForDRM(operation);

        return operation;
    }

    private int modifyForImageType(int operation) {
        //modify for stereo feature
        if (MpoHelper.MPO_MIME_TYPE.equalsIgnoreCase(mimeType)) {
            //for mpo file, we have to check whether it is MAV
//            initMpoSubType();
            if (MediaObject.SUBTYPE_MPO_MAV == getSubType()) {
                // MAV image has the same supported operations as video
                operation = (SUPPORT_DELETE | SUPPORT_SHARE |
                             /*SUPPORT_PLAY |*/ SUPPORT_INFO | SUPPORT_PRINT); // M: MPO_MAV type support print too
                operation |= SUPPORT_MAV_PLAYBACK;
                if(mIsStereoDisplaySupported) {
                    operation |= SUPPORT_STEREO_DISPLAY;
                    operation &= ~SUPPORT_CONV_TUNING;
                    operation |= SUPPORT_CONVERT_TO_3D;
                }
            } else {
                operation |= SUPPORT_FULL_IMAGE;
                if(mIsStereoDisplaySupported) {
                    operation |= SUPPORT_STEREO_DISPLAY;
                    operation |= SUPPORT_CONV_TUNING;
                    operation &= ~SUPPORT_CONVERT_TO_3D;
                    operation |= SUPPORT_AUTO_CONV;
                }
            }
        } else if (StereoHelper.JPS_MIME_TYPE.equalsIgnoreCase(mimeType)) {
            // JPS stereo image has to be treated specially
            operation |= SUPPORT_FULL_IMAGE;
            if (mIsStereoDisplaySupported) {
                operation |= SUPPORT_STEREO_DISPLAY;
                operation |= SUPPORT_CONV_TUNING;
                if (!mIsMtk3D) {
                    operation |= SUPPORT_AUTO_CONV;
                }
            }
        } else if (MediatekFeature.isSupportedByGifDecoder(mimeType)) {
            //added for GIF animation
            operation |= SUPPORT_GIF_ANIMATION;
        } else if (mIsDisplay2dAs3dSupported) {
            //for normal image, support stereo display if possible
            //GIF animation is not supposed to be displayed as stereo
            operation |= SUPPORT_STEREO_DISPLAY;
            operation |= SUPPORT_CONVERT_TO_3D;
        }
        return operation;
    }

    private int modifyForDRM(int operation) {
        //if image is drm kind, we have to restrict supported operations
        //according to OMA drm 1.0
        if (!MediatekFeature.isDrmSupported() || !isDrm()) {
            return operation;
        }

        //we add protection info operation
        operation |= SUPPORT_DRM_INFO;

        if (!isDrmMethod(OmaDrmStore.DrmMethod.METHOD_FL)) {
            //for local image that is drm and is not fl type, add
            //consume operation.
            //Noto: consume_drm operation has nothing to do
            //with current drm rights status
            operation |= SUPPORT_CONSUME_DRM;
        }

        if (0 != (operation & SUPPORT_SETAS)) {
            if (OmaDrmStore.RightsStatus.RIGHTS_VALID !=
                drmRights(OmaDrmStore.Action.WALLPAPER)) {
                operation &= ~ SUPPORT_SETAS;
            }
        }

        if (0 != (operation & SUPPORT_SHARE)) {
            if (OmaDrmStore.RightsStatus.RIGHTS_VALID !=
                drmRights(OmaDrmStore.Action.TRANSFER)) {
                operation &= ~ SUPPORT_SHARE;
            }
        }

        if (0 != (operation & SUPPORT_PRINT)) {
            if (OmaDrmStore.RightsStatus.RIGHTS_VALID !=
                drmRights(OmaDrmStore.Action.PRINT)) {
                operation &= ~ SUPPORT_PRINT;
            }
        }

        //As drm content is proctected,
        //no drm media supports edit, crop, rotate
        operation &= ~ SUPPORT_CROP;
        operation &= ~ SUPPORT_EDIT;
        operation &= ~ SUPPORT_ROTATE;

        return operation;
    }
    
    private void attachMtkDetails(MediaDetails details) {
        boolean dimensionAvailable = true;
        Object objW = details.getDetail(MediaDetails.INDEX_WIDTH);
        Object objH = details.getDetail(MediaDetails.INDEX_HEIGHT);
        if (objW == null || objH == null) {
            dimensionAvailable = false;
            MtkLog.w(TAG, "attachMtkDetails: no width/height found in details, will use DB data");
        }
        if (dimensionAvailable) {
            try {
                if (objW instanceof String) {
                    if (Integer.parseInt((String) objW) == 0) {
                        dimensionAvailable = false;
                        MtkLog.w(TAG, "attachMtkDetails: width is 0, use DB data");
                    }
                }
                if (objH instanceof String) {
                    if (Integer.parseInt((String) objH) == 0) {
                        dimensionAvailable = false;
                        MtkLog.w(TAG, "attachMtkDetails: height is 0, use DB data");
                    }
                }
            } catch (NumberFormatException e) {
                // M: just ignore this exception
                MtkLog.w(TAG, "attachMtkDetails: exception: ", e);
            }
        }
        if (!dimensionAvailable) {
            // M: for non-jpeg and non-mpo images,
            // width and height must be fetched from MediaProvider,
            // since there will be no available exif data for these formats
            
            // M: for stereo image, the actual dimension
            // might not be the original physical dimension of the image
            adjustDimIfNeeded();
            details.addDetail(MediaDetails.INDEX_WIDTH,String.valueOf(width));
            details.addDetail(MediaDetails.INDEX_HEIGHT,String.valueOf(height));
        }
    }

    /// M: vtsp @{
    public AbstractVideoPlayer getVideoPlayer() {
        if (MediatekFeature.isPanorama3DSupported() && isPanorama()) {
            return new PanoramaVideoPlayer();
        } else if(MediaObject.SUBTYPE_MPO_MAV == getSubType()){
            return new MavPlayer();
        } else {
            return new OrdinaryVideoPlayer();
        }
    }

    // virtual
    protected void initThumbnailPlayType() {
        if (MediatekFeature.isPanorama3DSupported() && isPanorama()) {
            thumbnailPlayType = TPT_DIRECT_PLAY;
        } else if(MediaObject.SUBTYPE_MPO_MAV == getSubType() && mIsMavSupported
                && mIsMavThumbnailSupported && !GalleryUtils.Is_Low_Ram_Device){
            thumbnailPlayType = TPT_DIRECT_PLAY;
        } else {
            thumbnailPlayType = TPT_NO_PLAY;
        }
    }

    // virtual
    protected void initVideoGenerator() {
        if (MediatekFeature.isPanorama3DSupported() && isPanorama()) {
            mVideoGenerator = new PanoramaToVideoGenerator();
        } else if(IMpoDecoder.MTK_TYPE_MAV == mMpoSubType){
            mVideoGenerator = new MAVToVideoGenerator();
        } else {
            mVideoGenerator = null;
        }
    }
    /// @}
}
