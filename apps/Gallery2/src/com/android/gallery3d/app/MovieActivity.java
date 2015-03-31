/*
 * Copyright (C) 2007 The Android Open Source Project
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

import java.io.File;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.TrackInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ActivityChooserView;
import android.widget.TextView;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import android.hardware.display.DisplayManager;

import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;

import com.mediatek.gallery3d.ext.IActivityHooker;
import com.mediatek.gallery3d.ext.IMovieItem;
import com.mediatek.gallery3d.ext.MovieItem;
import com.mediatek.gallery3d.ext.MovieUtils;
import com.mediatek.gallery3d.ext.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.video.ExtensionHelper;
import com.mediatek.gallery3d.video.MovieTitleHelper;
import com.mediatek.gallery3d.ext.IMovieList;
import com.mediatek.gallery3d.ext.IMovieListLoader;
import com.mediatek.gallery3d.ext.MovieList;
import com.mediatek.gallery3d.ext.IMovieListLoader.LoaderListener;
import com.mediatek.gallery3d.ext.MovieListLoader;
import com.mediatek.common.featureoption.FeatureOption;
import android.os.PowerManager;

/**
 * This activity plays a video from a specified URI.
 * 
 * The client of this activity can pass a logo bitmap in the intent (KEY_LOGO_BITMAP)
 * to set the action bar logo so the playback process looks more seamlessly integrated with
 * the original activity.
 */
public class MovieActivity extends Activity implements LoaderListener ,CreateBeamUrisCallback {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/MovieActivity";
    public static final String KEY_LOGO_BITMAP = "logo-bitmap";
    public static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
    private static final String VIDEO_CONTENT_SCHEMA = "content";
    public static final String KEY_COME_FROM_CAMERA = "come-from-camera";

    /// M: add for letv @{
    private static final String SCREEN_ORIENTATION_LANDSCAPE = "SCREEN_ORIENTATION_LANDSCAPE";
    private static final String STREAMING_VIDEO_TITLE = "STREAMING_VIDEO_TITLE";
    private boolean mIsLandscape = false;
    private String mVideoTitle = null;
    /// @}
    private PowerManager mPowerManager;
    public DisplayManager mDisplayManager;

    private boolean mFinishOnCompletion;
    //private Uri mUri;
    private boolean mTreatUpAsBack;
    ///M: add for NFC
    private boolean mBeamVideoIsPlaying = false;
    ///M: add for streaming cookie
    public static final String COOKIE = "Cookie";
	private static final boolean LOG = true;
    /// M: add for change video to previous/next this is make movielist and use in movieplayer @{
    private IMovieListLoader mMovieLoader;
	private MoviePlayer mPlayer;
    public IMovieList mMovieList;
    /// @}
    private IMovieItem mMovieItem;
    /// M: resume bug fix @{
    private boolean mResumed = false;
    private boolean mControlResumed = false;
    /// @}   
    /// M: for sdp over http @{
    private static final String VIDEO_SDP_MIME_TYPE = "application/sdp";
    private static final String VIDEO_SDP_TITLE = "rtsp://";
    private static final String VIDEO_FILE_SCHEMA = "file";
    private static final String VIDEO_MIME_TYPE = "video/*";
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setSystemUiVisibility(View rootView) {
        if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
    
    /// M: NFC feature
    NfcAdapter mNfcAdapter;
    private final Handler mHandler = new Handler();
    private final Runnable mPlayVideoRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null && mBeamVideoIsPlaying) {
                MtkLog.i(TAG, "NFC call play video");
                mPlayer.onPlayPause();
            }
        }
    };
    
    @Override
    public void onListLoaded(IMovieList movieList) {
        mMovieList = movieList;
        if (LOG) {
            MtkLog.v(TAG, "onListLoaded() " + (mMovieList != null ? mMovieList.size() : "null"));
        }
    }
    private final Runnable mPauseVideoRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                MtkLog.i(TAG, "NFC call pause video");
                mBeamVideoIsPlaying = true;
                mPlayer.onPlayPause();
            } else {
                mBeamVideoIsPlaying = false;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MtkLog.v(TAG, "onCreate()");
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.movie_view);
        View rootView = findViewById(R.id.movie_view_root);

        setSystemUiVisibility(rootView);
        mDisplayManager = (DisplayManager)this.getSystemService(Context.DISPLAY_SERVICE);
        Intent intent = getIntent();
        mMovieHooker = ExtensionHelper.getHooker(this);
        initMovieInfo(intent);
        initializeActionBar(intent);
        mFinishOnCompletion = intent.getBooleanExtra(
                MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        mTreatUpAsBack = intent.getBooleanExtra(KEY_TREAT_UP_AS_BACK, false);
        ///M:add for make movielist will be used in moviePlayer to change previous/next
        mMovieLoader = new MovieListLoader();
        mMovieLoader.fillVideoList(this, intent, this, mMovieItem);
        /// M: for letv landscape playback and show title from intent@{
        mIsLandscape = intent.getBooleanExtra(SCREEN_ORIENTATION_LANDSCAPE, false);
        if (mIsLandscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        mVideoTitle = intent.getStringExtra(STREAMING_VIDEO_TITLE);
        /// @}
        mPlayer =
                new MoviePlayer(rootView, this, mMovieItem, savedInstanceState,
                        !mFinishOnCompletion, intent.getStringExtra(COOKIE)) {
            @Override
            public void onCompletion() {
                if (LOG) {
                    MtkLog.v(TAG, "onCompletion() mFinishOnCompletion=" + mFinishOnCompletion);
                }
                if (mFinishOnCompletion) {
                    finish();
                }
            }
        };
        if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = intent.getIntExtra(
                    MediaStore.EXTRA_SCREEN_ORIENTATION,
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (orientation != getRequestedOrientation()) {
                setRequestedOrientation(orientation);
            }
        }
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(winParams);

        // We set the background in the theme to have the launching animation.
        // But for the performance (and battery), we remove the background here.
        win.setBackgroundDrawable(null);
        mMovieHooker.init(this, intent);
        mMovieHooker.setParameter(null, mPlayer.getMoviePlayerExt());
        mMovieHooker.setParameter(null, mMovieItem);
        mMovieHooker.setParameter(null, mPlayer.getVideoSurface());
        mMovieHooker.setParameter(null, rootView);
        mMovieHooker.onCreate(savedInstanceState);
    }

    private void setActionBarLogoFromIntent(Intent intent) {
        Bitmap logo = intent.getParcelableExtra(KEY_LOGO_BITMAP);
        if (logo != null) {
            getActionBar().setLogo(
                    new BitmapDrawable(getResources(), logo));
        }
        /// M: Get Nfc adapter and set callback available. @{
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (mNfcAdapter == null) {
            MtkLog.e(TAG, "NFC not available!");
            return;
        }
        mNfcAdapter.setMtkBeamPushUrisCallback(this, this);
        OnNdefPushCompleteCallback completeCallBack = new OnNdefPushCompleteCallback(){
            @Override
            public void onNdefPushComplete(NfcEvent event) {
                mHandler.removeCallbacks(mPlayVideoRunnable);
                mHandler.post(mPlayVideoRunnable);
            }
        };
        mNfcAdapter.setOnNdefPushCompleteCallback(completeCallBack, this, this);
        /// @}
    }
    
    private void initializeActionBar(Intent intent) {
        //mUri = intent.getData();
        final ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        setActionBarLogoFromIntent(intent);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_HOME_AS_UP);
        /// M: show title for video playback
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_TITLE);

//        String title = intent.getStringExtra(Intent.EXTRA_TITLE);
//        if (title != null) {
//            actionBar.setTitle(title);
//        } else {
//            enhanceActionBar();
            /*// Displays the filename as title, reading the filename from the
            // interface: {@link android.provider.OpenableColumns#DISPLAY_NAME}.
            AsyncQueryHandler queryHandler =
                    new AsyncQueryHandler(getContentResolver()) {
                @Override
                protected void onQueryComplete(int token, Object cookie,
                        Cursor cursor) {
                    try {
                        if ((cursor != null) && cursor.moveToFirst()) {
                            String displayName = cursor.getString(0);

                            // Just show empty title if other apps don't set
                            // DISPLAY_NAME
                            actionBar.setTitle((displayName == null) ? "" :
                                    displayName);
                        }
                    } finally {
                        Utils.closeSilently(cursor);
                    }
                }
            };
            queryHandler.startQuery(0, null, mUri,
                    new String[] {OpenableColumns.DISPLAY_NAME}, null, null,
                    null);*/
//        }
        if (LOG) {
            MtkLog.v(TAG, "initializeActionBar() mMovieInfo=" + mMovieItem);
        }
    }
    // M: FOR MTK_AUDIO_CHANGE_SUPPORT 
    ///@{
    private MenuItem mChangeAudioTrackMenu;
    
    private void showDialogFromPlayer(int Type){
    	mPlayer.showDialogForTrack(Type);
    }
    
    /**
     * set the audio track setting icon visible or not
     * @param visible true to be visible false to be invisible
     */
 	public void setAudioMenuItemVisible(boolean visible){
            if (mChangeAudioTrackMenu != null) {
                mChangeAudioTrackMenu.setVisible(visible);
            }
    }
 	//@}
    
    // M: FOR MTK_SUBTITLE_SUPPORT
    private MenuItem mChangeSubTitleTrackMenu;
    private boolean mSubtitleTrackMenuVisible = false;
    private String mVideoFilePath = null;
    /**
     * set the audio track setting icon visible or not
     * @param visible true to be visible false to be invisible
     */
    public void setSubtitleMenuItemVisible(boolean visible){

        mSubtitleTrackMenuVisible = visible;
            
        if (mChangeSubTitleTrackMenu != null) {
            mChangeSubTitleTrackMenu.setVisible(visible);
        }
    }
 	
    /**
     * list the external subtitle files
     * @return files array
   */
    public File[] listExtSubTitleFileNameWithPath(){    	
        return MtkUtils.listTheSameNameOfVideo(mVideoFilePath);
    }
    
    private  String getVideoRealPathFromURI(Uri uri){
    	String[] proj = { MediaStore.Video.Media.DATA}; 
    	Cursor cursor = null;
    	String nameFromURI = null;
    	try{
            cursor = getContentResolver().query(uri,proj,null,null,null);
            if (cursor == null) {
                return null;
            }
            int colummIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            nameFromURI = cursor.getString(colummIndex);
            MtkLog.v(TAG, "getVideoRealPathFromURI colummIndex= " + colummIndex); 
    	} catch (Exception e) {
            MtkLog.e(TAG, "getVideoRealPathFromURI Exception", e); 
    	} finally {
            if(cursor != null){
                cursor.close();
        }
    }
    	return nameFromURI;
    }
    /////@}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        boolean local = MovieUtils.isLocalFile(mMovieItem.getOriginalUri(), mMovieItem.getMimeType());
        /// M: don't show share if intent from le tv
        if (!MtkUtils.canShare(getIntent().getExtras()) || (local && 
                !ExtensionHelper.getMovieDrmExtension(this).canShare(this, mMovieItem)) || mIsLandscape) {
            //do not show share
        } else {
            getMenuInflater().inflate(R.menu.movie, menu);
            mShareMenu = menu.findItem(R.id.action_share);
            ShareActionProvider provider = (ShareActionProvider) mShareMenu.getActionProvider();
            mShareProvider = provider;

            refreshShareProvider(mMovieItem);
        }
        // M: FOR MTK_AUDIO_CHANGE_SUPPORT
        //@{
		if(MediatekFeature.MTK_AUDIO_CHANGE_SUPPORT || MediatekFeature.MTK_SUBTITLE_SUPPORT){
        	getMenuInflater().inflate(R.menu.movie_track_info, menu);
		}
		if (MediatekFeature.MTK_AUDIO_CHANGE_SUPPORT) {
			mChangeAudioTrackMenu = menu.findItem(R.id.action_change_audio_track);
			mChangeAudioTrackMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						public boolean onMenuItemClick(MenuItem item) {
							showDialogFromPlayer(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO);
							return true;
						}
					});
		}
 	    ///@}
        // M: FOR MTK_SUBTITLE_SUPPORT
        if(MediatekFeature.MTK_SUBTITLE_SUPPORT){
            mChangeSubTitleTrackMenu = menu.findItem(R.id.action_change_subTitle_track);
            mChangeSubTitleTrackMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(){
            	public boolean onMenuItemClick(MenuItem item)
            	{
                    MtkLog.v(TAG, "AudioAndSubtitle --- mDeleteMenu.setOnClickListener(new OnClickListener()");
                    showDialogFromPlayer(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT);
                    return true;
            	}
            });
        }            
        //@}
        return mMovieHooker.onCreateOptionsMenu(menu);
        /*getMenuInflater().inflate(R.menu.movie, menu);
        ShareActionProvider provider = GalleryActionBar.initializeShareActionProvider(menu);

        // Document says EXTRA_STREAM should be a content: Uri
        // So, we only share the video if it's "content:".
        if (provider != null && ContentResolver.SCHEME_CONTENT
                .equals(mUri.getScheme())) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, mUri);
            provider.setShareIntent(intent);
        }

        return true;*/
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        if(MediatekFeature.MTK_SUBTITLE_SUPPORT) {
            setSubtitleMenuItemVisible(mSubtitleTrackMenuVisible);
        }
        
        if (isPartialVisible()) {
            // if video is partially visible, we done allow options menu
            // to show up
            return false;
        }
        return mMovieHooker.onPrepareOptionsMenu(menu);
    }
    
    private Intent createShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, mMovieItem.getUri());
        return intent;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (mTreatUpAsBack) {
                finish();
            } else {
                startActivity(new Intent(this, GalleryActivity.class));
                finish();
            }
            return true;
        } else if (id == R.id.action_share) {
            startActivity(Intent.createChooser(createShareIntent(),
                    getString(R.string.share)));
            return true;
        }
        return mMovieHooker.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        mMovieHooker.onStart();
        registerScreenOff();
        if (LOG) {
            MtkLog.v(TAG, "onStart()");
        }
    }

    @Override
    protected void onStop() {
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                .abandonAudioFocus(null);
        super.onStop();
        if (mControlResumed && mPlayer != null) {
            mPlayer.onStop();
            mControlResumed = false;
        }
        mMovieHooker.onStop();
        unregisterScreenOff();
        if (LOG) {
            MtkLog.v(TAG, "onStop() "
                + ", mResumed=" + mResumed + ", mControlResumed=" + mControlResumed);
        }
    }

    @Override
    public void onPause() {
        if (LOG) {
            MtkLog.v(TAG, "onPause() " 
                + ", mResumed=" + mResumed + ", mControlResumed=" + mControlResumed);
        }
        if(FeatureOption.MTK_SMARTBOOK_SUPPORT && getIntent().getBooleanExtra(KEY_COME_FROM_CAMERA, false)) {
            if (!(mFinishOnCompletion || onbackkeypressde)) {
                screenOffForSmartBook();
            } 
        }
        mResumed = false;
        if (mControlResumed && mPlayer != null) {
            mControlResumed = !mPlayer.onPause();
        }
        unregisterReceiver(mSmartBookReceiver);
        super.onPause();
        collapseShareMenu();
        mMovieHooker.onPause();
    }
    /// M: add for smartbook @ {
    private void screenOnForSmartBook() {
        if (LOG) {
            Log.d(TAG, "prepareSmartBookScreenOn,FO is :"+ FeatureOption.MTK_SMARTBOOK_SUPPORT);
        }
        
        // add for SMARTBOOK
        if (FeatureOption.MTK_SMARTBOOK_SUPPORT && getIntent().getBooleanExtra(KEY_COME_FROM_CAMERA, false)) {
            int flags = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            WindowManager.LayoutParams pl = getWindow().getAttributes();
            pl.flags |= flags;
            getWindow().setAttributes(pl);
        }
    }
    public void screenOffForSmartBook() {
        if (LOG) {
            Log.d(TAG, "screenOffForSmartBook, mIsSmartBookPlugged = "+mIsSmartBookPlugged +",mPowerManager = "+mPowerManager);
        }
        if(mIsSmartBookPlugged && mPowerManager != null) {
            mPowerManager.sbGoToSleep(SystemClock.uptimeMillis());
        }
    }
    private boolean onbackkeypressde = false;
    
    @Override
    public void onBackPressed() {
       Log.d(TAG, "onBackPressed");
       if(FeatureOption.MTK_SMARTBOOK_SUPPORT && getIntent().getBooleanExtra(KEY_COME_FROM_CAMERA, false)) {
           onbackkeypressde = true;
       }
       if (!mMovieHooker.onBackPressed()) {
           super.onBackPressed();
       }
    }
    
    /// M: For SmartBook @{
    private final BroadcastReceiver mSmartBookReceiver = new SmartBookBroadcastReceiver();
    public boolean mIsSmartBookPlugged = false;

    private void registerSmartBookReceiver() {
        if (LOG) {
            Log.d(TAG, "registerSmartBookReceiver ");
        }
        IntentFilter mSmartBookIntentFilter = new IntentFilter();
        mSmartBookIntentFilter.addAction(Intent.ACTION_SMARTBOOK_PLUG);
        registerReceiver(mSmartBookReceiver, mSmartBookIntentFilter);
    }
///M: add for smartbook. if plug in smartbook when playing drm video then finish movieActivity and show notification and toast
    private class SmartBookBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (LOG) {
                Log.d(TAG, "SmartBookBR,action = " + action);
            }
            if (Intent.ACTION_SMARTBOOK_PLUG.equals(action)) {
                mIsSmartBookPlugged = intent.getBooleanExtra(Intent.EXTRA_SMARTBOOK_PLUG_STATE, false);
                if (LOG) {
                    Log.d(TAG, "SmartBookBR,state = " + mIsSmartBookPlugged);
                }
            }
        }
    } 
    /// M:end  } @
    @Override
    public void onResume() {
        if (LOG) {
            MtkLog.v(TAG, "onResume() " 
                + ", mResumed=" + mResumed + ", mControlResumed=" + mControlResumed);
        }
        screenOnForSmartBook();
        registerSmartBookReceiver();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (LOG) {
            Log.v(TAG, "getSystemService,mPowerManager =" + mPowerManager);
        }
         
        mResumed = true;
        if (mResumed && !mControlResumed && mPlayer != null) {
            mPlayer.onResume();
            mControlResumed = true;
        }
        if (mShareProvider != null) {
            mShareProvider.setOnShareTargetSelectedListener(null);
        }
        enhanceActionBar();
        super.onResume();
        mMovieHooker.onResume();
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (LOG) {
            MtkLog.v(TAG, "onWindowFocusChanged(" + hasFocus + ")"
                + ", mResumed=" + mResumed + ", mControlResumed=" + mControlResumed);
        }
        if (hasFocus && mResumed && !mControlResumed && mPlayer != null) {
            mPlayer.onResume();
            mControlResumed = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPlayer.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        mPlayer.onDestroy();
        super.onDestroy();
        mMovieHooker.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mPlayer.onKeyDown(keyCode, event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mPlayer.onKeyUp(keyCode, event)
                || super.onKeyUp(keyCode, event);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mMovieHooker.onConfigurationChanged(newConfig);
    }
    
    
    private void initMovieInfo(Intent intent) {
        Uri original = intent.getData();
        String mimeType = intent.getType();
        if (VIDEO_SDP_MIME_TYPE.equalsIgnoreCase(mimeType)
                && VIDEO_FILE_SCHEMA.equalsIgnoreCase(original.getScheme())) {
            mMovieItem = new MovieItem(VIDEO_SDP_TITLE + original, mimeType, null);
        } else {
            mMovieItem = new MovieItem(original, mimeType, null);
        }
        mMovieItem.setOriginalUri(original);
        
        // M: FOR MTK_SUBTITLE_SUPPORT
        ///@{
        if(MediatekFeature.MTK_SUBTITLE_SUPPORT){
            if(VIDEO_FILE_SCHEMA.equalsIgnoreCase(original.getScheme())){
                mVideoFilePath = original.getPath();
            }
	    else if (VIDEO_CONTENT_SCHEMA.equalsIgnoreCase(original.getScheme())){
                mVideoFilePath = getVideoRealPathFromURI(original);
            }
        }
        ///@}
        if (LOG) {
            MtkLog.v(TAG, "initMovieInfo(" + original + ") mMovieInfo=" + mMovieItem);
        }
    }
    /// @}
    
    /// M:for live streaming. @{
    //we do not stop live streaming when other dialog overlays it.
    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOG) {
                MtkLog.v(TAG, "onReceive(" + intent.getAction() + ") mControlResumed=" + mControlResumed);
            }
            /// M:add receive shut down broadcast, stop video.
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())
                    || Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                // Only stop video.
                if (mControlResumed) {
                    mPlayer.onStop();
                    mControlResumed = false;
                }
            }
        }
        
    };
    
    private void registerScreenOff() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        /// M:add receive shut down broadcast
        filter.addAction(Intent.ACTION_SHUTDOWN);
        registerReceiver(mScreenOffReceiver, filter);
    }
    
    private void unregisterScreenOff() {
        unregisterReceiver(mScreenOffReceiver);
    }
    /// @}
    
    /// M: enhance the title feature @{
    private void enhanceActionBar() {
        final IMovieItem movieItem = mMovieItem;//remember original item
        final Uri uri = mMovieItem.getUri();
        final String scheme = mMovieItem.getUri().getScheme();
        final String authority = mMovieItem.getUri().getAuthority();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String title = null;
                /// M: get title from letv @{
                if (mVideoTitle != null) {
                    title = mVideoTitle;
                    MtkLog.v(TAG, "enhanceActionBar() task return mVideoTitle " + title);
                    return title;
                }
                /// @}
                // / M: Get title from uri if the title can not find in
                // database @{
                if (ContentResolver.SCHEME_FILE.equals(scheme)) { // from file
                    // manager
                    title = MovieTitleHelper.getTitleFromMediaData(MovieActivity.this, uri);
                    if (title == null) {
                        title = MovieTitleHelper.getTitleFromUri(uri);
                    }
                } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                    title = MovieTitleHelper.getTitleFromDisplayName(MovieActivity.this, uri);
                    if (title == null) {
                        title = MovieTitleHelper.getTitleFromData(MovieActivity.this, uri);
                    }
                } else {
                    title = MovieTitleHelper.getTitleFromUri(uri);
                } // / @}
                if (LOG) {
                    MtkLog.v(TAG, "enhanceActionBar() task return " + title);
                }
                return title;
            }
            @Override
            protected void onPostExecute(String result) {
                if (LOG) {
                    MtkLog.v(TAG, "onPostExecute(" + result + ") movieItem=" + movieItem + ", mMovieItem=" + mMovieItem);
                }
                movieItem.setTitle(result);
                if (movieItem == mMovieItem) {
                    setActionBarTitle(result);
                }
            };
        }.execute();
        if (LOG) {
            MtkLog.v(TAG, "enhanceActionBar() " + mMovieItem);
        }
    }
    
    public void setActionBarTitle(String title) {
        if (LOG) {
            MtkLog.v(TAG, "setActionBarTitle(" + title + ")");
        }
        ActionBar actionBar = getActionBar();
        if (title != null) {
            actionBar.setTitle(title);
        }
    }
    /// @}

    public void refreshMovieInfo(IMovieItem info) {
        mMovieItem = info;
        setActionBarTitle(info.getTitle());
        refreshShareProvider(info);
        mMovieHooker.setParameter(null, mMovieItem);
        if (LOG) {
            MtkLog.v(TAG, "refreshMovieInfo(" + info + ")");
        }
    }

    private ShareActionProvider mShareProvider;
    private void refreshShareProvider(IMovieItem info) {
        // Document says EXTRA_STREAM should be a content: Uri
        // So, we only share the video if it's "content:".
        /// M: the upper is JellyBean's comment, here we enhance the share action.
        if (mShareProvider != null) {
            mShareProvider.setOnShareTargetSelectedListener(null);
            Intent intent = new Intent(Intent.ACTION_SEND);
            if (MovieUtils.isLocalFile(info.getUri(), info.getMimeType())) {
                intent.setType("video/*");
                intent.putExtra(Intent.EXTRA_STREAM, info.getUri());
            } else {
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, String.valueOf(info.getUri()));
            }
            mShareProvider.setShareIntent(intent);
        }
        if (LOG) {
            MtkLog.v(TAG, "refreshShareProvider() mShareProvider=" + mShareProvider);
        }
    }
    
    /* M: ActivityChooseView's popup window will not dismiss
     * when user press power key off and on quickly.
     * Here dismiss the popup window if need.
     * Note: dismissPopup() will check isShowingPopup().
     * @{
     */
    private MenuItem mShareMenu;
    private void collapseShareMenu() {
        if (mShareMenu != null &&  mShareMenu.getActionView() instanceof ActivityChooserView) {
            ActivityChooserView chooserView = (ActivityChooserView)mShareMenu.getActionView();
            if (LOG) {
                MtkLog.v(TAG, "collapseShareMenu() chooserView.isShowingPopup()=" + chooserView.isShowingPopup());
            }
            chooserView.dismissPopup();
        }
    }
    /* @} */
    
   
    private IActivityHooker mMovieHooker;
    /**
     * M: Add NFC callback to provide the uri.
     */
    @Override
    public Uri[] createBeamUris(NfcEvent event) {
        mHandler.removeCallbacks(mPauseVideoRunnable);
        mHandler.post(mPauseVideoRunnable);
        Uri currentUri = mMovieItem.getOriginalUri();
        MtkLog.i(TAG, "NFC call for uri " + currentUri);
        return new Uri[]{currentUri};
    }

    /// M:for stereo video playback feature.
    public boolean isPartialVisible() {
        return mMovieHooker.isPartialVisible();
    }
    /// @}
    
}
