package com.mediatek.gallery3d.conshots;

import java.io.File;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore.Images;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.LocalImage.LocalImageRequest;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;
import com.mediatek.gallery3d.videothumbnail.ContainerToVideoGenerator;
import com.android.gallery3d.util.ThreadPool.Job;

public class ContainerImage extends LocalImage {

    private static final String TAG = "Gallery2/ContainerImage";

    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_BUCKET_ID = 10;
    
    private MediaSet mRelatedMediaSet;
    private boolean mIsConshot;
    private boolean mIsMotion;
    private boolean mIsContainer;
    private long mGroupId;
    private int mGroupCount;
    public long dateLastModifiedInSec;
    
    private static final boolean mIsConShotsSupported =
        MediatekFeature.isConShotsImagesSupported();
    
    private static final boolean mIsMotionTrackSupported =
        MediatekFeature.isMotionTrackSupported();
    
    public ContainerImage(Path path, GalleryApp application, Cursor cursor) {
        super(path, application, cursor);
        judgeType();
    }

    public ContainerImage(Path path, GalleryApp application,  int id) {
        super(path, application, id);
        judgeType();
    }
    protected void loadFromCursor(Cursor cursor) {
        super.loadFromCursor(cursor);
        mGroupId = cursor.getLong(INDEX_GROUP_ID);
        mGroupCount = cursor.getInt(INDEX_GROUP_COUNT);
        updateState();
    }
    
    static public boolean isContainerItem(GalleryApp application, Cursor cursor){
        if(cursor == null) return false;
        
        String title = cursor.getString(INDEX_CAPTION);
        long backetID = cursor.getLong(INDEX_BUCKET_ID);
//        if(!isCameraRoll(backetID)) return false;
        
        if(mIsConShotsSupported && title.matches("^IMG[0-9_]*CS$")) {
            return true;
        }
        if(mIsMotionTrackSupported && title.matches("^IMG[0-9_]*MT$")) {
            return true;
        }
        
        return false;
    }
    
    public void judgeType(){
        mIsContainer = false;
        mIsConshot = false;
        mIsMotion = false;
        
        if(isConShotType()){
            mRelatedMediaSet = ContainerHelper.getConShotSet(mApplication, mGroupId, bucketId); 
            if(mRelatedMediaSet == null || mRelatedMediaSet.getMediaItemCount() <=1 ){
                mRelatedMediaSet = null;
                mIsContainer = false;
                mIsConshot = false;
            }else{
                mIsContainer = true;
                mIsConshot = true;
            }
        }else if(isMotionType()){
            mRelatedMediaSet = ContainerHelper.getMotionSet(mApplication, getName(), id); 
            
            if(mRelatedMediaSet == null || (mRelatedMediaSet != null && mRelatedMediaSet.getMediaItemCount()==0)){
                mRelatedMediaSet = null;
                mIsContainer = false;
                mIsMotion = false;
            }else{
                mIsContainer = true;
                mIsMotion = true;
                ((MotionSet)mRelatedMediaSet).setRotation(rotation);
                dateLastModifiedInSec = new File(getFilePath()).lastModified();
            }
        }
        if(mIsContainer){
            initVideoGenerator();
        }
        return;
    }
    
    private boolean isConShotType(){
        if(mGroupId != 0 && caption.matches("^IMG[0-9_]*CS$")) {
            return true;
        }else{
            return false;
        }
    }
    
    private boolean isMotionType(){
        if(caption.matches("^IMG[0-9_]*MT$")){
            return true;
        }else{
            return false;
        }
    }
    
    public MediaSet getRelatedMediaSet(){
        return mRelatedMediaSet;
    }
    
    @Override
    public void updateContent(Cursor cursor) {
        super.updateContent(cursor);
        judgeType();
    }
    
    @Override
    public int getSupportedOperations() {
        if(isContainer()){
            return mRelatedMediaSet.getSupportedOperations();
        }
        return super.getSupportedOperations();
    }

    @Override
    public void delete() {
        if(isContainer()){
            mRelatedMediaSet.delete();
        }
        super.delete();
    }
    
    @Override
    public MediaDetails getDetails() {
        if(isContainer()){
            return mRelatedMediaSet.getDetails();
        }
        return super.getDetails();
    }

    public static boolean isCameraRoll(long bucketId) {
        String defaultPath = MtkUtils.getMtkDefaultPath()+"/DCIM/Camera";
        return bucketId == GalleryUtils.getBucketId(defaultPath);
    }
    
    @Override
    public boolean isMotion(){
        return mIsMotion;
    }

    @Override
    public boolean isConShot() {
        return mIsConshot;
    }

    @Override
    public boolean isContainer() {
        return mIsContainer;
    }
    
    @Override
    public long getGroupId() {
        return mGroupId;
    }
    
    @Override
    public int getSubType() {
        int subType=0;
        if(!mIsConshot && !mIsMotion && !mIsContainer){
            return super.getSubType();
        }
        
        if(mIsConshot) {
            subType |= SUBTYPE_CONSHOT;
        }
        if(mIsMotion){
            subType |= SUBTYPE_MOTION;
        }
        if(mIsContainer){
            subType |= SUBTYPE_CONTAINER;
        }
        return subType;
    }
    
    @Override
    public Uri getContentUri() {
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        return baseUri.buildUpon().appendQueryParameter("mtkInclusion", 
                String.valueOf(mPath.getMtkInclusion())).appendPath(String.valueOf(id)).build();
    }

    protected void initThumbnailPlayType() {
        if (mIsContainer) {
            thumbnailPlayType = TPT_GENERATE_PLAY;
        } else {
            // call super's version since container image may stand for a special image such as
            // mav and panorama. We let LocalImage to decide the TPT accordingly
            super.initThumbnailPlayType();
        }
    }

    protected void initVideoGenerator() {
        if (mIsContainer) {
            if (mVideoGenerator == null) {
                mVideoGenerator = new ContainerToVideoGenerator(mApplication);
                updateState();
            }
        } else {
            // call super's version since container image may stand for a special image such as
            // mav and panorama.We let LocalImage to decide the generator accordingly
            super.initVideoGenerator();
        }
    }
    
    private void reGenerateVideo(){
        MtkLog.i(TAG, "reGenerateVideo");
        if(mVideoGenerator != null){
            mVideoGenerator.prepareToRegenerate(this);
            MtkLog.i(TAG, "prepareToRegenerate");
        }
    }
    
    public void updateState(){
        if(!mIsContainer) return;
        if(mIsMotion){
            long modifiedInSec = new File(getFilePath()).lastModified();
            if(modifiedInSec != dateLastModifiedInSec){
                dateLastModifiedInSec = modifiedInSec;
                reGenerateVideo();
                // notify camera to update thumbnail
                mApplication.getDataManager().broadcastUpdatePicture();
            }
        }else if(mIsConshot){
            int itmeCount = mRelatedMediaSet.getMediaItemCount();
            if(itmeCount != mGroupCount && mVideoGenerator != null){
                setGroupCount(itmeCount);
                reGenerateVideo();
            }
        }
    }
    
    @Override
    public Job<Bitmap> requestImage(int type) {
        if(mIsContainer){
            updateState();
        }
        if(mIsContainer && mIsMotion){
            return new LocalImageRequest(mApplication, mPath, dateLastModifiedInSec, type, filePath);
        }else{
            return new LocalImageRequest(mApplication, mPath, dateModifiedInSec, type, filePath);
        }
    }
    
    private void setGroupCount(int count) {
        GalleryUtils.assertNotInRenderThread();
        mGroupCount = count;
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        if (isConShot()) {
            ContentValues cv = new ContentValues(1);
            cv.put(Images.Media.GROUP_COUNT, count);
            try{
                int result = mApplication.getContentResolver().update(baseUri, cv, "_id=?",
                        new String[] { String.valueOf(id) });
                MtkLog.i(TAG, "<setIsBestShot> update mGroupCount value of id[" + id + "] result = "
                        + result);
            }catch(Exception e){
                Log.e(TAG, "Exception when getContentResolver", e);
            }
        }
    }
}
