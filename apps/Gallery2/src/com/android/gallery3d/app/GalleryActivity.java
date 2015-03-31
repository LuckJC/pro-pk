/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.gallery3d.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Trace;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.Files.FileColumns;
import android.view.Menu;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.GalleryUtils;
import java.io.File;
import android.provider.MediaStore.Images.Media;
import android.database.Cursor;
import com.mediatek.gallery3d.conshots.ConShotImage;
import com.mediatek.gallery3d.conshots.ContainerHelper;
import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import android.hardware.display.WifiDisplayStatus;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import com.mediatek.gallery3d.util.MediatekMMProfile;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

public final class GalleryActivity extends AbstractGalleryActivity implements OnCancelListener {
    public static final String EXTRA_SLIDESHOW = "slideshow";
    public static final String EXTRA_DREAM = "dream";
    public static final String EXTRA_CROP = "crop";

    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    public static final String KEY_GET_CONTENT = "get-content";
    public static final String KEY_GET_ALBUM = "get-album";
    public static final String KEY_TYPE_BITS = "type-bits";
    public static final String KEY_MEDIA_TYPES = "mediaTypes";
    public static final String KEY_DISMISS_KEYGUARD = "dismiss-keyguard";
    
    private static final String TAG = "Gallery2/GalleryActivity";

    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();
    private static final boolean mIsStereoDisplaySupported = 
                                          MediatekFeature.isStereoDisplaySupported();

    private Dialog mVersionCheckDialog;

    private WifiDisplayStatus mWifiDisplayStatus;
    private DisplayManager mDisplayManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MtkUtils.traceStart("Gallery:onCreate");
        Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>Gallery-onCreate");
        MediatekMMProfile.startProfileGalleryOnCreate();
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        if (getIntent().getBooleanExtra(KEY_DISMISS_KEYGUARD, false)) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        MtkUtils.traceStart("Gallery:setContentView");
        setContentView(R.layout.main);
        MtkUtils.traceEnd("Gallery:setContentView");
        mDisplayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);

        if (savedInstanceState != null) {
            getStateManager().restoreFromState(savedInstanceState);
        } else {
            initializeByIntent();
        }
        MediatekMMProfile.stopProfileGalleryOnCreate();
        Trace.traceEnd(Trace.TRACE_TAG_APP);
        MtkUtils.traceEnd("Gallery:onCreate");
    }

    private void initializeByIntent() {
        MtkUtils.traceStart("Gallery:initializeByIntent");
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)) {
            startGetContent(intent);
        } else if (Intent.ACTION_PICK.equalsIgnoreCase(action)) {
            // We do NOT really support the PICK intent. Handle it as
            // the GET_CONTENT. However, we need to translate the type
            // in the intent here.
            Log.w(TAG, "action PICK is not supported");
            String type = Utils.ensureNotNull(intent.getType());
            if (type.startsWith("vnd.android.cursor.dir/")) {
                if (type.endsWith("/image")) intent.setType("image/*");
                if (type.endsWith("/video")) intent.setType("video/*");
            }
            startGetContent(intent);
        } else if (Intent.ACTION_VIEW.equalsIgnoreCase(action)
                || ACTION_REVIEW.equalsIgnoreCase(action)){
            startViewAction(intent);
        } else {
            startDefaultPage();
        }
        MtkUtils.traceEnd("Gallery:initializeByIntent");
    }

    public void startDefaultPage() {
        MtkUtils.traceStart("Gallery:startDefaultPage");
        PicasaSource.showSignInReminder(this);
        Bundle data = new Bundle();
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
        //add for DRM feature
        if (MediatekFeature.isDrmSupported()) {
            //when start default page, we query all drm media, any risk???
            Log.d(TAG,"startDefaultPage:we query all drm media");
            data.putInt(com.mediatek.drm.OmaDrmStore.DrmExtra.EXTRA_DRM_LEVEL,
                        com.mediatek.drm.OmaDrmStore.DrmExtra.DRM_LEVEL_ALL);
        }
        if (MediatekFeature.isStereoDisplaySupported()) {
            if (null != getIntent().getExtras()) {
                data.putBoolean(StereoHelper.STEREO_EXTRA,
                      getIntent().getExtras().getBoolean(
                                                  StereoHelper.STEREO_EXTRA,false));
            }
        }
        getStateManager().startState(AlbumSetPage.class, data);
        mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.setOnCancelListener(this);
        }
        MtkUtils.traceEnd("Gallery:startDefaultPage");
    }

    private void startGetContent(Intent intent) {
        Bundle data = intent.getExtras() != null
                ? new Bundle(intent.getExtras())
                : new Bundle();
        data.putBoolean(KEY_GET_CONTENT, true);
        int typeBits = GalleryUtils.determineTypeBits(this, intent);
        data.putInt(KEY_TYPE_BITS, typeBits);
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(typeBits));
        getStateManager().startState(AlbumSetPage.class, data);
    }

    private String getContentType(Intent intent) {
        String type = intent.getType();
        if (type != null) {
            return GalleryUtils.MIME_TYPE_PANORAMA360.equals(type)
                ? MediaItem.MIME_TYPE_JPEG : type;
        }

        Uri uri = intent.getData();
        try {
            return getContentResolver().getType(uri);
        } catch (Throwable t) {
            Log.w(TAG, "get type fail", t);
            return null;
        }
    }

    private void startViewAction(Intent intent) {
        Boolean slideshow = intent.getBooleanExtra(EXTRA_SLIDESHOW, false);
        if (slideshow) {
            getActionBar().hide();
            DataManager manager = getDataManager();
            Path path = manager.findPathByUri(intent.getData(), intent.getType());
            if (path == null || manager.getMediaObject(path)
                    instanceof MediaItem) {
                path = Path.fromString(
                        manager.getTopSetPath(DataManager.INCLUDE_IMAGE));
            }
            Bundle data = new Bundle();
            data.putString(SlideshowPage.KEY_SET_PATH, path.toString());
            data.putBoolean(SlideshowPage.KEY_RANDOM_ORDER, true);
            data.putBoolean(SlideshowPage.KEY_REPEAT, true);
            if (intent.getBooleanExtra(EXTRA_DREAM, false)) {
                data.putBoolean(SlideshowPage.KEY_DREAM, true);
            }
            getStateManager().startState(SlideshowPage.class, data);
        } else {
            Bundle data = new Bundle();
            DataManager dm = getDataManager();
            Uri uri = intent.getData();
            String contentType = getContentType(intent);
            if (contentType == null) {
                Toast.makeText(this,
                        R.string.no_such_item, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (uri == null) {
                int typeBits = GalleryUtils.determineTypeBits(this, intent);
                data.putInt(KEY_TYPE_BITS, typeBits);
                data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                        getDataManager().getTopSetPath(typeBits));
                getStateManager().startState(AlbumSetPage.class, data);
            } else if (contentType.startsWith(
                    ContentResolver.CURSOR_DIR_BASE_TYPE)) {
                int mediaType = intent.getIntExtra(KEY_MEDIA_TYPES, 0);
                if (mediaType != 0) {
                    uri = uri.buildUpon().appendQueryParameter(
                            KEY_MEDIA_TYPES, String.valueOf(mediaType))
                            .build();
                }
                Path setPath = dm.findPathByUri(uri, null);
                MediaSet mediaSet = null;
                if (setPath != null) {
                    mediaSet = (MediaSet) dm.getMediaObject(setPath);
                }
                if (mediaSet != null) {
                    if (mediaSet.isLeafAlbum()) {
                        data.putString(AlbumPage.KEY_MEDIA_PATH, setPath.toString());
                        data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                                dm.getTopSetPath(DataManager.INCLUDE_ALL));
                        if (mIsDrmSupported || mIsStereoDisplaySupported) {
                            data.putInt(DrmHelper.DRM_INCLUSION, 
                                    intent.getIntExtra(DrmHelper.DRM_INCLUSION, DrmHelper.NO_DRM_INCLUSION));
                        }
                        getStateManager().startState(AlbumPage.class, data);
                    } else {
                        data.putString(AlbumSetPage.KEY_MEDIA_PATH, setPath.toString());
                        if (mIsDrmSupported || mIsStereoDisplaySupported) {
                            data.putInt(DrmHelper.DRM_INCLUSION, 
                                    intent.getIntExtra(DrmHelper.DRM_INCLUSION, DrmHelper.NO_DRM_INCLUSION));
                        }
                        getStateManager().startState(AlbumSetPage.class, data);
                    }
                } else {
                    startDefaultPage();
                }
            } else {
                //change file:///mnt/sdcard... type uri to context://media/external...
                //if possible.
                uri = tryContentMediaUri(uri);
                
                // M: if current URI is not local, do NOT pop up the "No storage" dialog
                if (!isLocalUri(uri)) {
                    Log.d(TAG, "startViewAction: uri=" + uri + ", not local!!");
                    mShouldCheckStorageState = false;
                }
                
                //add for DRM feature
                if (mIsDrmSupported || mIsStereoDisplaySupported) {
                    //when start default page, we query all drm media, any risk???
                    Log.d(TAG,"startViewAction:we query all drm media");
                    data.putInt(com.mediatek.drm.OmaDrmStore.DrmExtra.EXTRA_DRM_LEVEL,
                                com.mediatek.drm.OmaDrmStore.DrmExtra.DRM_LEVEL_ALL);
                    //add for DRM feature: pass drm inclusio info to ActivityState
                    //int mtkInclusion = DrmHelper.getDrmInclusionFromData(data);
                    int mtkInclusion = MediatekFeature.getInclusionFromData(data);
                    data.putInt(DrmHelper.DRM_INCLUSION, mtkInclusion);
                }

                Path itemPath = dm.findPathByUri(uri, contentType);

                if (itemPath == null) {
                    Toast.makeText(this,
                            R.string.no_such_item, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                Path albumPath = null;
                try {
                    ///M:clear old mediaObject, query database again
                    itemPath.clearObject();
                    albumPath = dm.getDefaultSetOf(itemPath);
                } catch (RuntimeException e) {
                    Log.e(TAG,"got RuntimeException "+e);
                    Log.e(TAG,"can not create proper album path object!");
                    Toast.makeText(this,
                            R.string.no_such_item, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, itemPath.toString());
                data.putBoolean(PhotoPage.KEY_READONLY, true);

                // TODO: Make the parameter "SingleItemOnly" public so other
                //       activities can reference it.
                boolean singleItemOnly = (albumPath == null)
                        || intent.getBooleanExtra("SingleItemOnly", false);
                if (!singleItemOnly) {
                    data.putString(PhotoPage.KEY_MEDIA_SET_PATH, albumPath.toString());
                    // M: do not back up if KEY_TREAT_BACK_AS_UP is false(should back to the home screen),as planer design.
                    if (intent.getBooleanExtra(PhotoPage.KEY_TREAT_BACK_AS_UP, false)) {
                        data.putBoolean(PhotoPage.KEY_TREAT_BACK_AS_UP, true);
                    }
                }
                /// M: added for ConShots
                MediaObject object = dm.getMediaObject(itemPath);
                /// M:can't create object, this file don't exist @{
                if(object == null) {
                    Toast.makeText(this,
                            R.string.no_such_item, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                ///@}
                if(object instanceof LocalImage) {
                    LocalImage item = (LocalImage)object;
                    if(item.isContainer() && item.isConShot()) {
                        
                        MediaSet mediaset = ContainerHelper.getConShotSet((GalleryApp)getApplication(), item.getGroupId(), item.getBucketId());
                        data.putString(PhotoPage.KEY_MEDIA_SET_PATH, mediaset.getPath().toString());
                        data.putInt(PhotoPage.KEY_INDEX_HINT, ContainerHelper.getConShotDspIndex(mediaset,item.id));
                        data.putBoolean(PhotoPage.KEY_TREAT_BACK_AS_UP, false);
                        data.putInt(DrmHelper.DRM_INCLUSION, 0);
                        getStateManager().startState(PhotoPage.class, data);
                        return;
                    }
                }
                /// M: added for open image from local
                if (isLocalUri(uri)) {
                    data.putBoolean(PhotoPage.KEY_IS_OPEN_FROM_LOCAL, true);
                } else {
                    data.putBoolean(PhotoPage.KEY_IS_OPEN_FROM_LOCAL, false);
                }
                /// @}
                getStateManager().startState(SinglePhotoPage.class, data);
            }
        }
    }
    
    protected void onStart() {
        Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>Gallery-onStart");
        super.onStart();
        Trace.traceEnd(Trace.TRACE_TAG_APP);
    }
    @Override
    protected void onResume() {
        MtkUtils.traceStart("Gallery:onResume");
        Trace.traceBegin(Trace.TRACE_TAG_APP, ">>>>Gallery-onResume");
        MediatekMMProfile.startProfileGalleryOnResume();
        Utils.assertTrue(getStateManager().getStateCount() > 0);
        super.onResume();
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.show();
        }
        MediatekMMProfile.stopProfileGalleryOnResume();
        /// M: add for performance test case
        mStopTime = 0;
        Trace.traceEnd(Trace.TRACE_TAG_APP);
        MtkUtils.traceEnd("Gallery:onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.dismiss();
        }
        
        mStopTime = System.currentTimeMillis();
    }
    
    /// M: add for performance test case
    public long mStopTime = 0;
    
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mVersionCheckDialog) {
            mVersionCheckDialog = null;
        }
    }

    private Uri tryContentMediaUri(Uri uri) {
        if (null == uri) return null;
        
        String scheme = uri.getScheme();
        if (!ContentResolver.SCHEME_FILE.equals(scheme)) {
            return uri;
        } else {
            //ALPS00258426
            //description:Black screen is shown when opening the downloaded 
            // picture in notification bar
            //solution:
            //when opening a deleted image by gallery2,we first check whether
            // the image file exist, if not, return null
            String path = uri.getPath();
            Log.d(TAG, "tryContentMediaUri:for " + path);
            if(!new File(path).exists()) {
                return null;
            }
        }

        Cursor cursor = null;
        try {
            //for file kinds of uri, query media database
            cursor = Media.query(
                    getContentResolver(), Media.getContentUri("external"), 
                    new String[] {Media._ID, Media.BUCKET_ID},
                    "_data=(?)", new String[] {uri.getPath()},
                    null);// " bucket_id ASC, _id ASC");
            if (null != cursor && cursor.moveToNext()) {
                long id = cursor.getLong(0);
                final String imagesUri = Media.getContentUri("external").toString();
                uri = Uri.parse(imagesUri + "/"+ id);
                Log.i(TAG,"tryContentMediaUri:got " + uri);
            } else {
                Log.w(TAG,"tryContentMediaUri:fail to convert " + uri);
            }
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }

        return uri;
    }
    
    private boolean isLocalUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        boolean isLocal = ContentResolver.SCHEME_FILE.equals(uri.getScheme());
        isLocal |= ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && 
                MediaStore.AUTHORITY.equals(uri.getAuthority());
        return isLocal;
    }
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        final boolean isTouchPad = (event.getSource()
                & InputDevice.SOURCE_CLASS_POSITION) != 0;
        if (isTouchPad) {
            float maxX = event.getDevice().getMotionRange(MotionEvent.AXIS_X).getMax();
            float maxY = event.getDevice().getMotionRange(MotionEvent.AXIS_Y).getMax();
            View decor = getWindow().getDecorView();
            float scaleX = decor.getWidth() / maxX;
            float scaleY = decor.getHeight() / maxY;
            float x = event.getX() * scaleX;
            //x = decor.getWidth() - x; // invert x
            float y = event.getY() * scaleY;
            //y = decor.getHeight() - y; // invert y
            MotionEvent touchEvent = MotionEvent.obtain(event.getDownTime(),
                    event.getEventTime(), event.getAction(), x, y, event.getMetaState());
            return dispatchTouchEvent(touchEvent);
        }
        return super.onGenericMotionEvent(event);
    }
}
