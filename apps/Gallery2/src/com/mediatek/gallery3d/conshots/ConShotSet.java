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

package com.mediatek.gallery3d.conshots;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ChangeNotifier;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.R;

// ConShotSet lists all same GroupID ConShots media items in one group.
// The media items need to be all continuous shots images.
public class ConShotSet extends MediaSet {
    private static final String TAG = "ConShotSet";
    private static final String[] COUNT_PROJECTION = { "count(*)" };

    private static final int INVALID_COUNT = -1;
    private final String mWhereClause;
    private final String mOrderClause;
    private final Uri mBaseUri;
    private final String[] mProjection;

    private final String[] mWhereClauseArgs;

    private final GalleryApp mApplication;
    private final ContentResolver mResolver;
    private final long mGroupId;
    private final ChangeNotifier mNotifier;
    private final Path mItemPath;
    private int mCachedCount = INVALID_COUNT;
    private int mBucketId;
    
    public ConShotSet(Path path, GalleryApp application, long groupId, int bucketId) {
        super(path, nextVersionNumber());
        mApplication = application;
        mResolver = application.getContentResolver();
        mGroupId = groupId;
        mBucketId = bucketId;
        mWhereClause = Images.Media.GROUP_ID + " = ?" + " AND " + Images.Media.TITLE + " LIKE 'IMG%CS'"+ " AND " + ImageColumns.BUCKET_ID + "= ?";
        mWhereClauseArgs = new String[] { String.valueOf(mGroupId), String.valueOf(mBucketId)};

        //mOrderClause = ImageColumns.DATE_TAKEN + " DESC, " + ImageColumns._ID
        //        + " DESC";
        mOrderClause = ImageColumns.GROUP_INDEX + " ASC";
        mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
        mProjection = LocalImage.PROJECTION;
        mItemPath = ConShotImage.ITEM_PATH;
        mNotifier = new ChangeNotifier(this, mBaseUri, application);
    }

    @Override
    public Uri getContentUri() {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                .appendQueryParameter(Images.Media.GROUP_ID, String.valueOf(mGroupId))
                .build();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        DataManager dataManager = mApplication.getDataManager();
        Uri uri = mBaseUri.buildUpon().appendQueryParameter("limit",
                start + "," + count).build();
        ArrayList<MediaItem> list = new ArrayList<MediaItem>();
        GalleryUtils.assertNotInRenderThread();
        Cursor cursor = mResolver.query(uri, mProjection, mWhereClause,
                mWhereClauseArgs, // new String[]{String.valueOf(group_id)},
                mOrderClause);
        if (cursor == null) {
            Log.w(TAG, "query fail: " + uri);
            return list;
        }

        boolean dataDirty = false;
        try {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0); // _id must be in the first column
                Path childPath = null;
                childPath = mItemPath.getChild(id);
                MediaItem item = loadOrUpdateItem(childPath, cursor,
                        dataManager, mApplication, true);

                list.add(item);
                // add check for data updated from database
                if (null != item && ((LocalMediaItem) item).dataDirty) {
                    dataDirty = true;
                    ((LocalMediaItem) item).dataDirty = false;
                }
            }
        } finally {
            cursor.close();
        }

        // add check for data updatde from database
        if (dataDirty) {
            Log.i(TAG, "getMediaItem:data changed in database.");
            notifyContentChanged();
        }

        return list;
    }

    private static MediaItem loadOrUpdateItem(Path path, Cursor cursor,
            DataManager dataManager, GalleryApp app, boolean isImage) {
        LocalMediaItem item = (LocalMediaItem) dataManager
                .peekMediaObject(path);
        if (item == null) {
            item = new ConShotImage(path, app, cursor);
        } else {
            item.updateContent(cursor);
        }
        return item;
    }

    public static Cursor getItemCursor(ContentResolver resolver, Uri uri,
            String[] projection, int id) {
        return resolver.query(uri, projection, "_id=?", new String[] { String
                .valueOf(id) }, null);
    }

    @Override
    public int getMediaItemCount() {
            Cursor cursor = mResolver.query(mBaseUri, COUNT_PROJECTION,
                    mWhereClause, mWhereClauseArgs,
                    null);
            if (cursor == null) {
                Log.w(TAG, "query fail");
                return 0;
            }
            try {
                Utils.assertTrue(cursor.moveToNext());
                mCachedCount = cursor.getInt(0);
            } finally {
                cursor.close();
        }
        return mCachedCount;
    }

    @Override
    public String getName() {
        return mApplication.getAndroidContext().getString(R.string.conshots_title);
    }

    @Override
    public long reload() {
        if (mNotifier.isDirty()) {
            mDataVersion = nextVersionNumber();
            mCachedCount = INVALID_COUNT;
        }
        return mDataVersion;
    }

    @Override
    public int getSupportedOperations() {
        return SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_INFO;
    }

    @Override
    public void delete() {
        GalleryUtils.assertNotInRenderThread();
        mResolver.delete(mBaseUri, mWhereClause, mWhereClauseArgs/*
                                                                  * new
                                                                  * String[]{
                                                                  * String
                                                                  * .valueOf
                                                                  * (group_id)}
                                                                  */);
        mApplication.getDataManager().broadcastUpdatePicture();
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = new MediaDetails();
        details.addDetail(MediaDetails.INDEX_TITLE, "continuous shot");
        // query db twice, need to optimize
        //details.addDetail(MediaDetails.INDEX_FOLDER_SIZE, getConShotsFolderSize());
        //details.addDetail(MediaDetails.INDEX_PIC_NUMBER, getConShotsChindrenCount());
        //details.addDetail(MediaDetails.INDEX_PATH, deleteFileNmae(filePath));
        return details;
    }

}
