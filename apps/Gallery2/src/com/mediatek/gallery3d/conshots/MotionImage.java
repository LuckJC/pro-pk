package com.mediatek.gallery3d.conshots;

import java.io.File;
import java.io.IOException;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.ThreadPool.Job;

public class MotionImage extends LocalImage {
    private static final String TAG = "Gallery2/MotionImage";
    
    public static final Path ITEM_PATH = Path.fromString("/container/motion/item");
    
    private boolean mIsDisable = false;
    
    public MotionImage(Path path, GalleryApp application, String localFilePath) {
        super(path, application, null);
        filePath = localFilePath;
        dataDirty = true;
        loadParameter();
    }

    public void uploadFilePath(String localFilePath) {
        if (filePath.contentEquals(localFilePath)) {
            dataDirty = false;
        } else {
            filePath = localFilePath;
            mDataVersion = nextVersionNumber();
            dataDirty = true;
        }
    }
    
    private void loadParameter(){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        width = options.outWidth;
        height = options.outHeight;
    }

    @Override
    protected void loadFromCursor(Cursor cursor) {
        return;
    }
    
    @Override
    public int getSupportedOperations() {
        return 0;
    }

    @Override
    public void delete() {
    }
    
    @Override
    public boolean isMotion() {
        return true;
    }
    
    @Override
    public boolean isDisabled(){
        return mIsDisable;
    }
    
    @Override
    public void setDisable(boolean isDisable){
        mIsDisable = isDisable;
    }
    
    public void setRotation(int rotation){
        this.rotation = rotation;
    }
    
    @Override
    public Job<Bitmap> requestImage(int type) {
        dateModifiedInSec = new File(getFilePath()).lastModified();
        /// M: added for ConShots.
        //The thumbnail of motion track photos need keep scale
        if (type == TYPE_MICROTHUMBNAIL) {
            type = TYPE_MOTIONTHUMBNAIL;
        }
        return super.requestImage(type);
    }
}
