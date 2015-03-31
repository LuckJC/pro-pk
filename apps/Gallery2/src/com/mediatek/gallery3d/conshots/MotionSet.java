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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.GalleryUtils;

// MotionSet lists all same GroupID of moiton images items 
public class MotionSet extends MediaSet {
    private static final String TAG = "Gallery2/MotionSet";
    
    private final Path mItemPath;
    private final GalleryApp mApplication;
    private Path mPath;
    public File mMotionDir;
    public String mLabel = "IMG" ;
    private String mName;
    private int mRotation = 0;
    private int mId;
    private String mWorkPath;
    private static final int INDEX_DATA = 8;
    private String mFilePath;
    
    public MotionSet(Path path, GalleryApp application, String name, int id) {
        super(path, nextVersionNumber());
        mPath = path;
        mApplication = application;
        mItemPath = MotionImage.ITEM_PATH;
        mName = name;
        mId = id;
//        mLabel = name;
        loadParentInfo();
        mWorkPath = new File(mFilePath).getParent();
        mMotionDir = new File(ContainerHelper.getMotionDir(mWorkPath, mName));
        Log.d(TAG, "MotionSet, mLabel:"+mLabel);
        Log.d(TAG, "motion dir,"+mWorkPath);
        Log.d(TAG, "MotionSet, mMotionDir:"+mMotionDir);
    }
    
    private void loadParentInfo(){
        Log.d(TAG, "loadParentInfo");
        ContentResolver resolver = mApplication.getContentResolver();
        Uri uri = Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = LocalAlbum.getItemCursor(resolver, uri, LocalImage.PROJECTION, mId);
        if (cursor == null) {
            throw new RuntimeException("cannot get cursor");
        }
        try {
            if (cursor.moveToNext()) {
                mFilePath = cursor.getString(INDEX_DATA);
            } else {
                throw new RuntimeException("cannot find data");
            }
        } finally {
            cursor.close();
        }
    }
    
    public boolean isParentExist() {
        File parent = new File(mFilePath);
        return parent.exists();
    }
    
    private boolean matchLabel(String fileName){
        //return fileName.contains(mLabel);
        return true;
    }
    
    public void setRotation(int rotation){
        mRotation = rotation;
    }
    
    public class fileComparator implements Comparator<File>{

        @Override
        public int compare(File file1, File file2) {
            return file1.getName().compareTo(file2.getName());
        }
    }
    
    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        Log.d(TAG, "getMediaItem:"+start+" count:"+count);
        boolean dataDirty = false;
        int motionCount = 0;
        
        ArrayList<MediaItem> list = new ArrayList<MediaItem>();
        DataManager dataManager = mApplication.getDataManager();
        
        if(!mMotionDir.exists()) return null;
        
        File[] allFiles = mMotionDir.listFiles();
        //sort file
        ArrayList<File> fileList = new ArrayList<File>();
        for(File file:allFiles){
            fileList.add(file);
        }
        Collections.sort(fileList, new fileComparator());
        
        for (motionCount = 0; motionCount < fileList.size(); motionCount++) {
            File currFile = fileList.get(motionCount);
            if (matchLabel(currFile.getName())) {
                
                if(motionCount < start) continue;
                else if(motionCount >= (start+count)) break;
                
                Path path = mItemPath.getChild(currFile.getName().toString());
                MotionImage item = (MotionImage) dataManager.peekMediaObject(path);
                
                if (item == null) {
                    item = new MotionImage(path, mApplication, currFile.getAbsolutePath());
                    item.setRotation(mRotation);
                    dataDirty = true;
                } else {
                    item.uploadFilePath(currFile.getAbsolutePath());
                    item.setRotation(mRotation);
                }
                
                if(item.dataDirty) dataDirty = true;
                list.add(item);
            }
        }
        // add check for data updatde from database
        if (dataDirty) {
            Log.i(TAG, "getMediaItem:data changed in database.");
            mDataVersion = nextVersionNumber();
            notifyContentChanged();
        }

        return list;
    }


    @Override
    public int getMediaItemCount() {
        int count=0;
        
        if(!mMotionDir.exists()) return 0;
        
        File[] allFiles = mMotionDir.listFiles();
        for (int i = 0; i < allFiles.length; i++) {
            if (matchLabel(allFiles[i].getName())) {
                count++;
            }
        }
        //Log.i(TAG, "getMediaItemCount:"+count);
        return count;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public long reload() {
        return mDataVersion;
    }

    @Override
    public int getSupportedOperations() {
        return SUPPORT_SHARE | SUPPORT_INFO | SUPPORT_DELETE;
    }

    @Override
    public void delete() {
        if(!mMotionDir.exists()) return;
        
        File[] allFiles = mMotionDir.listFiles();
        for (int i = 0; i < allFiles.length; i++) {
            allFiles[i].delete();
        }
        GalleryUtils.assertNotInRenderThread();
        mApplication.getDataManager().broadcastUpdatePicture();
        mDataVersion = nextVersionNumber();
        notifyContentChanged();
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = new MediaDetails();
        details.addDetail(MediaDetails.INDEX_TITLE, "motion track");
        return details;
    }
    
    public String getWorkPath(){
        return mWorkPath;
    }
}

