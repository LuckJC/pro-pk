package com.mediatek.gallery3d.conshots;

import java.math.BigInteger;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.util.MtkLog;

public class ConShotImage extends LocalImage{
    private static final String TAG = "Gallery2/ConShotImage";
 
    public static final Path ITEM_PATH = Path.fromString("/container/conshot/item");
    
    private int mGroupIndex;
    private BigInteger mFocusValue;
    private int mIsBestShot ;
    private long mGroupId;
    
    public ConShotImage(Path path, GalleryApp application, Cursor cursor) {
        super(path, application, cursor);
    }

    protected void loadFromCursor(Cursor cursor) {
        super.loadFromCursor(cursor);
        mGroupIndex = cursor.getInt(INDEX_GROUP_INDEX);
        long high = cursor.getInt(INDEX_FOCUS_VALUE_HIGH);
        long low = cursor.getInt(INDEX_FOCUS_VALUE_LOW);
        mFocusValue = BigInteger.valueOf(high).shiftLeft(32).add(BigInteger.valueOf(low));
        Log.i(TAG, "<loadFromCursor> mFocusValue = " + mFocusValue + ", L = " + low + ", H = " + high);
        mIsBestShot = cursor.getInt(INDEX_IS_BEST_SHOT);
        mGroupId = cursor.getLong(INDEX_GROUP_ID);
    }
    
    @Override
    public int getGroupIndex() {
        return mGroupIndex;
    }

    @Override
    public long getGroupId() {
        return mGroupId;
    }
    
    @Override
    public BigInteger getFocusValue() {
        return mFocusValue;
    }
    
    @Override
    public boolean isConShot() {
        return true;
    }

    @Override
    public int getIsBestShot() {
        return mIsBestShot;
    }
    
    @Override
    public void setIsBestShot(int isBestShot) {
        GalleryUtils.assertNotInRenderThread();
        mIsBestShot = isBestShot;
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        if (isConShot()) {
            ContentValues cv = new ContentValues(1);
            cv.put(Images.Media.IS_BEST_SHOT, isBestShot);
            int result = mApplication.getContentResolver().update(baseUri, cv, "_id=?",
                    new String[] { String.valueOf(id) });
            MtkLog.i(TAG, "<setIsBestShot> update isBestShot value of id[" + id + "] result = "
                    + result);
        }
    }
}
