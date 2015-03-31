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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.TrackInfo;
import android.media.TimedText.Style; 
import android.media.TimedText;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Virtualizer;

import android.media.Metadata;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.provider.Settings;

import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.BlobCache;
import com.android.gallery3d.util.CacheManager;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.ext.IContrllerOverlayExt;
import com.mediatek.gallery3d.ext.IMovieDrmExtension;
import com.mediatek.gallery3d.ext.IMovieDrmExtension.IMovieDrmCallback;
import com.mediatek.gallery3d.ext.IMovieItem;
import com.mediatek.gallery3d.ext.IMoviePlayer;
import com.mediatek.gallery3d.ext.MovieUtils;
import com.mediatek.gallery3d.ext.MtkLog;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkUtils;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallery3d.video.DetailDialog;
import com.mediatek.gallery3d.video.ErrorDialogFragment;
import com.mediatek.gallery3d.video.ExtensionHelper;
import com.mediatek.gallery3d.video.MTKVideoView;
import com.mediatek.gallery3d.video.ScreenModeManager;
import com.mediatek.gallery3d.video.StopVideoHooker;
import com.mediatek.gallery3d.video.ScreenModeManager.ScreenModeListener;
import com.mediatek.gallery3d.video.IControllerRewindAndForward;
import com.mediatek.gallery3d.video.IControllerRewindAndForward.IRewindAndForwardListener;
import com.mediatek.gallery3d.video.WfdPowerSaving;

// M: FOR MTK_SUBTITLE_SUPPORT
///@{
import com.mediatek.gallery3d.video.SubTitleView;
import com.mediatek.gallery3d.video.SubtitleSettingDialog;
//@}

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MoviePlayer implements
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        ControllerOverlay.Listener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnTimedTextListener{   //M: ADD FOR MTK_SUBTITLE_SUPPORT
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/MoviePlayer";
    private static final String TEST_CASE_TAG = "Gallery2PerformanceTestCase2";

    private static final String KEY_VIDEO_POSITION = "video-position";
    private static final String KEY_RESUMEABLE_TIME = "resumeable-timeout";
    // Copied from MediaPlaybackService in the Music Player app.
    private static final String SERVICECMD = "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDPAUSE = "pause";	
    private static final String ASYNC_PAUSE_PLAY = "MTK-ASYNC-RTSP-PAUSE-PLAY";
	/// M: for more detail in been killed case @{
    private static final String KEY_CONSUMED_DRM_RIGHT = "consumed_drm_right";
    private static final String KEY_POSITION_WHEN_PAUSED = "video_position_when_paused";
    private static final String KEY_VIDEO_CAN_SEEK = "video_can_seek";
    private static final String KEY_VIDEO_CAN_PAUSE = "video_can_pause";
    private static final String KEY_VIDEO_LAST_DURATION = "video_last_duration";
    private static final String KEY_VIDEO_LAST_DISCONNECT_TIME = "last_disconnect_time";
    private static final String KEY_VIDEO_STATE = "video_state";
    private static final String KEY_VIDEO_STREAMING_TYPE = "video_streaming_type";
    private static final String KEY_VIDEO_CURRENT_URI= "video_current_uri";

	private static final int RETURN_ERROR = -1;
	private static final int NONE_TRACK_INFO = -1;
    private static final int TYPE_TRACK_INFO_BOTH = -1;
    private static final int ERROR_CANNOT_CONNECT = -1003;
    private static final int ERROR_FORBIDDEN = -1100;
    private static final int ERROR_INVALID_OPERATION = -38;
    private static final int ERROR_ALREADY_EXISTS = -35;
    // These are constants in KeyEvent, appearing on API level 11.
    private static final int KEYCODE_MEDIA_PLAY = 126;
    private static final int KEYCODE_MEDIA_PAUSE = 127;
    private static final long BLACK_TIMEOUT = 500;
    // If we resume the acitivty with in RESUMEABLE_TIMEOUT, we will keep playing.
    // Otherwise, we pause the player.
    private static final long RESUMEABLE_TIMEOUT = 3 * 60 * 1000; // 3 mins
    private long mResumeableTime = Long.MAX_VALUE;
    	
	/// M: for log flag, if set this false, will improve run speed.
    private static final boolean LOG = true;
    private MovieActivity mActivityContext;//for dialog and toast context
    /// M: add for streaming cookie
    private String mCookie = null;
    private Context mContext;
    private final MTKVideoView mVideoView;
    private final View mRootView;
    private final Bookmarker mBookmarker;
    //private Uri mUri;
    private final Handler mHandler = new Handler();
    private final AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver;
    private final MovieControllerOverlay mController;

    private int mVideoPosition = 0;
    private int mLastSystemUiVis = 0;
    //M: MTK_AUDIO_CHANGE_SUPPORT & MTK_SUBTITLE_SUPPORT//@{
    private int mSelectAudioIndex = 0; //single choice index in dialog
    private int mSelcetAudioTrackIdx = 0; //index in all track info
    private int mSelectSubTitleIndex = 0; //single choice index in dialog
    private int mSelectSubTitleTrackIdx = 0;  //index in all track info
    private AlertDialog mSubtitleSetDialog;     
    private SubTitleView mSubTitleView;
    private ImageView mSubTitleViewBm;
    //@}

    private int mVideoLastDuration;//for duration displayed in init state  

    private boolean mHideController = false;
	
    private PlayPauseProcessExt mPlayPauseProcessExt = new PlayPauseProcessExt();
    private boolean mFirstBePlayed = false;//for toast more info 
    private boolean mVideoCanPause = false;
    private boolean mVideoCanSeek = false;
	private boolean mCanReplay;
    private boolean mHasPaused = false;
    // If the time bar is being dragged.
    private boolean mDragging;
    // If the time bar is visible.
    private boolean mShowing;
    /// M: add for control Action bar first open show 500ms @{
    private boolean mIsDelayFinish = false;
    private static final long SHOW_ACTIONBAR_TIME = 500;
    /// @}
    // M: the type of the video
    private int mVideoType = MovieUtils.VIDEO_TYPE_LOCAL;
    
    ///M: for wfd power saving.
    private WfdPowerSaving mWfdPowerSaving;
    ///M: for slowmotion
    private int mSlowMotionSpeed;
    private MovieActivity mMovieActivity;

    private boolean is4kBluetoothOff;
    private boolean DisableHdmiAndMhl;
    private AlertDialog m4kDilog;
    private boolean is4kDilogShow;
    private boolean notifyHdmiManager4kPlaying;
    
    private TState mTState = TState.PLAYING;
    private IMovieItem mMovieItem;
    private RetryExtension mRetryExt = new RetryExtension();
    private ScreenModeExt mScreenModeExt = new ScreenModeExt();
    private ServerTimeoutExtension mServerTimeoutExt = new ServerTimeoutExtension();
    private MoviePlayerExtension mPlayerExt = new MoviePlayerExtension();
    private IContrllerOverlayExt mOverlayExt;
    private IControllerRewindAndForward mControllerRewindAndForwardExt;
    private static final String VIRTUALIZE_EXTRA = "virtualize";
    private Virtualizer mVirtualizer;

    private final Runnable mPlayingChecker = new Runnable() {
        @Override
        public void run() {
            boolean isplaying = mVideoView.isPlaying();
            if (LOG) {
                MtkLog.v(TAG, "mPlayingChecker.run() isplaying=" + isplaying);
            }
            if (isplaying) {
                mController.showPlaying();
            } else {
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };

//    private final Runnable mRemoveBackground = new Runnable() {
//        @Override
//        public void run() {
//            if (LOG) {
//                MtkLog.v(TAG, "mRemoveBackground.run()");
//            }
//            mRootView.setBackground(null);
//        }
//    };

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            int updatetime =  1000 / 2 * mSlowMotionSpeed;
            if(updatetime == 0) {
                mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
            } else {
                mHandler.postDelayed(mProgressChecker, updatetime - (pos % updatetime));
            }
        }
    };
    
    public MoviePlayer(View rootView, final MovieActivity movieActivity, IMovieItem info,
            Bundle savedInstance, boolean canReplay, String cookie) {
        mContext = movieActivity.getApplicationContext();
        mRootView = rootView;
        mVideoView = (MTKVideoView) rootView.findViewById(R.id.surface_view);
        mBookmarker = new Bookmarker(movieActivity);
        mMovieActivity = movieActivity;
        //FOR MTK_SUBTITLE_SUPPORT
        ///@{       
        if(MediatekFeature.MTK_SUBTITLE_SUPPORT){
            mSubTitleView = (SubTitleView) rootView.findViewById(R.id.subtitle_view);
            mSubTitleViewBm = (ImageView)rootView.findViewById(R.id.subtitle_bitmap);
            mSubTitleView.InitFirst(mContext,mSubTitleViewBm);
        }        
        ///@}

        mCookie = cookie;
        //mUri = videoUri;
        
        
        mController = new MovieControllerOverlay(movieActivity);
        ((ViewGroup)rootView).addView(mController.getView());
        mController.setListener(this);
        mController.setCanReplay(canReplay);
        
        init(movieActivity, info, canReplay);
        
        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        //we move this behavior to startVideo()
        //mVideoView.setVideoURI(mUri, null);
        
        Intent ai = movieActivity.getIntent();
        boolean virtualize = ai.getBooleanExtra(VIRTUALIZE_EXTRA, false);
        if (virtualize) {
            int session = mVideoView.getAudioSessionId();
            if (session != 0) {
                mVirtualizer = new Virtualizer(0, session);
                mVirtualizer.setEnabled(true);
            } else {
                Log.w(TAG, "no audio session to virtualize");
            }
        }
        mVideoView.setSurfaceListener(new MTKVideoView.SurfaceListener() {
            // surface listener to watch surface changes
            @Override
            public void surfaceCreated() {
                Log.d(TAG, "surfaceCreated, refreshMovieInfo");
                //for stereo display feature
                if (mActivityContext != null) {
                    mActivityContext.refreshMovieInfo(mMovieItem);
                }
            }
        });
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
               if(!mHideController) {
                    updateDisplayElement();
                    mController.show();
                }
                return true;
            }
        });

        /// M: remove it for seekable is handled in onPrepred. @{ 
        /*mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                if (!mVideoView.canSeekForward() || !mVideoView.canSeekBackward()) {
                    mController.setSeekable(false);
                } else {
                    mController.setSeekable(true);
                }
                setProgress();
            }
        });*/
        /// @} 

        // The SurfaceView is transparent before drawing the first frame.
        // This makes the UI flashing when open a video. (black -> old screen
        // -> video) However, we have no way to know the timing of the first
        // frame. So, we hide the VideoView for a while to make sure the
        // video has been drawn on it.
        /// M: remove it for performance issue. @{
        /*mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoView.setVisibility(View.VISIBLE);
            }
        }, BLACK_TIMEOUT);*/
        /// @}
        enableWfdPowerSavingIfNeed();
        
        setOnSystemUiVisibilityChangeListener();
        mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
        mAudioBecomingNoisyReceiver.register();

        if (savedInstance != null) { // this is a resumed activity
            /// M: add for action bar don't dismiss
            mIsDelayFinish = true;
            mVideoPosition = savedInstance.getInt(KEY_VIDEO_POSITION, 0);
            mResumeableTime = savedInstance.getLong(KEY_RESUMEABLE_TIME, Long.MAX_VALUE);
            onRestoreInstanceState(savedInstance);
            mHasPaused = true;
        } else {
            // Hide system UI by default
            /// M:first open need to show action bar 500ms.
            showSystemUi(false, true);
            mTState = TState.PLAYING;
            mFirstBePlayed = true;
            final BookmarkerInfo bookmark = mBookmarker.getBookmark(mMovieItem.getUri());
            if (is4kVideoLimitation()) {
                show4kVideoDialog(movieActivity, bookmark);
            }else if (bookmark != null) {
                showResumeDialog(movieActivity, bookmark);
            } else {
                doStartVideoCareDrm(false, 0, 0);
            }
        }
        mScreenModeExt.setScreenMode();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setOnSystemUiVisibilityChangeListener() {
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) return;

        // When the user touches the screen or uses some hard key, the framework
        // will change system ui visibility from invisible to visible. We show
        // the media control and enable system UI (e.g. ActionBar) to be visible at this point
        mVideoView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
            	boolean finish = (mActivityContext == null ? true : mActivityContext.isFinishing());
                int diff = mLastSystemUiVis ^ visibility;
                mLastSystemUiVis = visibility;
                if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                        && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    if(!mHideController) {
                        mController.show();
                        mRootView.setBackgroundColor(Color.BLACK);
                    }
                }
                if (LOG) {
                    MtkLog.v(TAG, "onSystemUiVisibilityChange(" + visibility + ") finishing()=" + finish);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showSystemUi(boolean visible, boolean isFirstOpen) {
        /// M:isFirstOpen mark for first open
        MtkLog.v(TAG, "showSystemUi() visible " + visible + " isFirstOpen " + isFirstOpen);
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) return;
        int flag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!visible) {
            //We used the deprecated "STATUS_BAR_HIDDEN" for unbundling
            /// M: if first open, need to show action bar 500ms @{
            final int flagx = flag | View.STATUS_BAR_HIDDEN | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            flag = flagx;
            // /M:Delay hidden the action bar when play a streaming video
            boolean isLocalFile =
                    MovieUtils.isLocalFile(mMovieItem.getUri(), mMovieItem.getMimeType());
            if (!isLocalFile) {
                mIsDelayFinish = true;
            }
            if (isFirstOpen && isLocalFile) {
                mVideoView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mVideoView.setSystemUiVisibility(flagx);
                        mIsDelayFinish = true;
                        if(mWfdPowerSaving != null) {
                            mWfdPowerSaving.setSystemUiVisibility(flagx);
                        }
                    }
                }, SHOW_ACTIONBAR_TIME);
                MtkLog.v(TAG, "first open showSystemUi() flag = " + flagx);
                return;
            }
            mVideoView.setSystemUiVisibility(flag);
            if(mWfdPowerSaving != null) {
                mWfdPowerSaving.setSystemUiVisibility(flag);
            }
            MtkLog.v(TAG, "not first open showSystemUi() flag = " + flag);
            return;
            /// @}
        }
        mVideoView.setSystemUiVisibility(flag);
        if(mWfdPowerSaving != null) {
            mWfdPowerSaving.setSystemUiVisibility(flag);
        }
        MtkLog.v(TAG, "visiable showSystemUi() flag = " + flag);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_VIDEO_POSITION, mVideoPosition);
        outState.putLong(KEY_RESUMEABLE_TIME, mResumeableTime);
        onSaveInstanceStateMore(outState);
    }

    private void showResumeDialog(Context context, final BookmarkerInfo bookmark) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.resume_playing_title);
        builder.setMessage(String.format(
                context.getString(R.string.resume_playing_message),
                GalleryUtils.formatDuration(context, bookmark.mBookmark / 1000)));
        builder.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onCompletion();
            }
        });
        builder.setPositiveButton(
                R.string.resume_playing_resume, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //here try to seek for bookmark
                //Note: if current video can not be sought, it will not has any bookmark.
            	///M: MTK_AUDIO_CHANGE_SUPPORT & MTK_SUBTITLE_SUPPORT
            	///@{
            	  if(MediatekFeature.MTK_AUDIO_CHANGE_SUPPORT){
                      mSelectAudioIndex = bookmark.mSelectAudioIndexBmk; 
                      mSelcetAudioTrackIdx = bookmark.mSelcetAudioTrackIdxBmk; 
                  }
                  if(MediatekFeature.MTK_SUBTITLE_SUPPORT){
                      mSelectSubTitleIndex = bookmark.mSelectSubTitleIndexBmk;
                      mSelectSubTitleTrackIdx = bookmark.mSelectSubTitleTrackIdxBmk;
                  }
                  ///@}
                mVideoCanSeek = true;
                doStartVideoCareDrm(true, bookmark.mBookmark, bookmark.mDuration);
            }
        });
        builder.setNegativeButton(
                R.string.resume_playing_restart, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                doStartVideoCareDrm(true, 0, bookmark.mDuration);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                mIsShowResumingDialog = true;
            }
        });
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                mIsShowResumingDialog = false;
            }
        });
        dialog.show();
    }
    private boolean is4kVideoLimitation() {
        return (MovieUtils.is4kVideo(mActivityContext, mMovieItem.getUri()) && 
                ((MovieUtils.isWfdEnabled(mContext) || MovieUtils.isWfdConnecting(mContext)) || 
                MovieUtils.isBluetoothAudioOn(mContext) ||
                MovieUtils.isHdmiOn() ||
                MovieUtils.isMhlOn()));
    }

    private void show4kVideoDialog(final Context context, final BookmarkerInfo bookmark) {
        if (is4kDilogShow) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.Warning_WFD_Dialog);
        if (MovieUtils.isBluetoothAudioOn(mContext)) {
            if (MovieUtils.isHdmiOn() || MovieUtils.isMhlOn() || 
                MovieUtils.isWfdEnabled(mContext) || MovieUtils.isWfdConnecting(mContext)) {
                builder.setMessage(String.format(
                        context.getString(R.string.Not_supported_4k_video)));
            }else {
                builder.setMessage(String.format(
                        context.getString(R.string.Not_supported_4k_video_in_bluetooth)));
            }
        }else if (MovieUtils.isWfdEnabled(mContext) || MovieUtils.isWfdConnecting(mContext)) {
            builder.setMessage(String.format(
                    context.getString(R.string.Not_supported_4k_video_in_wfd)));
        }else if (MovieUtils.isHdmiOn()) {
            builder.setMessage(String.format(
                    context.getString(R.string.Not_supported_4k_video_in_hdmi)));
        }else if (MovieUtils.isMhlOn()) {
            builder.setMessage(String.format(
                    context.getString(R.string.Not_supported_4k_video_in_mhl)));
        }
        builder.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onCompletion();
                is4kDilogShow = false;
            }
        });
        builder.setPositiveButton(
                R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (MovieUtils.isWfdEnabled(mContext) || MovieUtils.isWfdConnecting(mContext)) {
                    //here disconnect WFD, continue play 4k video in phone.
                    MovieUtils.disconnectWfd(mContext);
                    }
                //case: hdmi&mhl is enable but not connetting.
                // SMB is support connect when play 4k video.
                if ((Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.HDMI_ENABLE_STATUS, 0) == 1) && !MovieUtils.isSmbOn()){
                    DisableHdmiAndMhl = true;
                    MovieUtils.disconnectHdmiOrMhl();
                }
                if (MovieUtils.isBluetoothAudioOn(mContext)) {
                    is4kBluetoothOff = true;
                    MovieUtils.disconnectBluetooth();
                }
                if (bookmark != null) {
                    showResumeDialog(context, bookmark);
                }else {
                    doStartVideoCareDrm(false, 0, 0);
                }
                is4kDilogShow = false;
            }
        });
        builder.setNegativeButton(
                R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //here cancel play 4k video, keep WFD connected.
                onCompletion();
            }
        });
        m4kDilog = builder.create();
        m4kDilog.show();
        is4kDilogShow = true;
    }

    public boolean onPause() {
        if (LOG) {
            MtkLog.v(TAG, "onPause()");
         }
        
        if(mWfdPowerSaving != null) {
            mWfdPowerSaving.cancelCountDown();
            mWfdPowerSaving.unregisterReceiver();
        }
        if (is4kBluetoothOff) {
            MovieUtils.reconnectBlutooth();
            is4kBluetoothOff = false;
        }
        if (DisableHdmiAndMhl) {
            MovieUtils.reconnectHdmiOrMhl();
            DisableHdmiAndMhl = false;
        }
        //case: play video end, notify HdmiManager 4k video is play end.
        if (notifyHdmiManager4kPlaying) {
            MovieUtils.notifyHdmiManager4kPlaying(false);
            notifyHdmiManager4kPlaying = false;
        }
        boolean pause = false;
        if (MovieUtils.isLiveStreaming(mVideoType)) {
            pause = false;
        } else {
            doOnPause();
            pause = true;
        }
        if (LOG) {
            MtkLog.v(TAG, "onPause() , return " + pause);
        }
        return pause;
    }
    
    //we should stop video anyway after this function called.
    public void onStop() {
        ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE))
                .abandonAudioFocus(mAudioFocusChangeListener);
        if (LOG) {
            MtkLog.v(TAG, "onStop() mHasPaused=" + mHasPaused);
        }
        if (!mHasPaused) {
            doOnPause();
        }
    }
    
    private void doOnPause() {
        long start = System.currentTimeMillis();
        mHasPaused = true;
        mHandler.removeCallbacksAndMessages(null);
        ///M: set background black here for avoid screen maybe flash when exit MovieActivity
        mHandler.removeCallbacks(mRemoveBackground);
        mRootView.setBackgroundColor(Color.BLACK);
        int position = mVideoView.getCurrentPosition();
        mVideoPosition = position >= 0 ? position : mVideoPosition;
        MtkLog.v(TAG, "mVideoPosition is " + mVideoPosition);
        int duration = mVideoView.getDuration();
        mVideoLastDuration = duration > 0 ? duration : mVideoLastDuration;
        ///M: MTK_SUBTITLE_SUPPORT & MTK_AUDIO_CHANGE_SUPPORT
        ///@{
        if(MediatekFeature.MTK_AUDIO_CHANGE_SUPPORT || MediatekFeature.MTK_SUBTITLE_SUPPORT){
        	mBookmarker.setBookmark(mMovieItem.getUri(), mVideoPosition, mVideoLastDuration,
            mSelcetAudioTrackIdx, mSelectSubTitleTrackIdx, mSelectAudioIndex, mSelectSubTitleIndex);
            if(MediatekFeature.MTK_SUBTITLE_SUPPORT){
	        if (null != mSubTitleView) {
		    mSubTitleView.setTextOrBitmap(null);
	            mSubTitleView.saveSettingPara();
	        }
            }        	
        //@}
        }else{
        	mBookmarker.setBookmark(mMovieItem.getUri(), mVideoPosition, mVideoLastDuration);
        }
        
        long end1 = System.currentTimeMillis();
        mVideoView.stopPlayback();//change suspend to release for sync paused and killed case
        mResumeableTime = System.currentTimeMillis() + RESUMEABLE_TIMEOUT;
        mVideoView.setResumed(false);//avoid start after surface created
        ///M: when stop play set enable in case  the pause complete callback not come.
        mController.setPlayPauseReplayResume();
        /// if activity will be finished, will not set movie view invisible @{
        if (!mActivityContext.isFinishing()){
            mVideoView.setVisibility(View.INVISIBLE);//Workaround for last-seek frame difference
        }
        /// @}
        
        long end2 = System.currentTimeMillis();
        mOverlayExt.clearBuffering();//to end buffer state
        mServerTimeoutExt.recordDisconnectTime();
        if (LOG) {
            MtkLog.v(TAG, "doOnPause() save video info consume:" + (end1 - start));
            MtkLog.v(TAG, "doOnPause() suspend video consume:" + (end2 - end1));
            MtkLog.v(TAG, "doOnPause() mVideoPosition=" + mVideoPosition + ", mResumeableTime=" + mResumeableTime
                + ", mVideoLastDuration=" + mVideoLastDuration + ", mIsShowResumingDialog="
                + mIsShowResumingDialog);
        }
    }

    public void onResume() {
        dump();
        mDragging = false;//clear drag info
        if (mHasPaused) {
            /// M: same as launch case to delay transparent. @{
            mVideoView.removeCallbacks(mDelayVideoRunnable);
            mVideoView.postDelayed(mDelayVideoRunnable, BLACK_TIMEOUT);
            /// @}
            
            ///M: reset notification related variable. @{
            mPlayPauseProcessExt.mNeedCheckPauseSuccess = false;
            mPlayPauseProcessExt.mPauseSuccess = false;
            mPlayPauseProcessExt.mPlayVideoWhenPaused = false;
            ///@}
            if (mServerTimeoutExt.handleOnResume() || mIsShowResumingDialog) {
                mHasPaused = false;
                return;
            }
            enableWfdPowerSavingIfNeed();
            if (is4kVideoLimitation()) {
                final BookmarkerInfo bookmark = mBookmarker.getBookmark(mMovieItem.getUri());
                show4kVideoDialog(mMovieActivity, bookmark);
            }else {
                switch(mTState) {
                case RETRY_ERROR:
                    mRetryExt.showRetry();
                    break;
                case STOPED:
                    mPlayerExt.stopVideo();
                    break;
                case COMPELTED:
                    mController.showEnded();
                    if (mVideoCanSeek || mVideoView.canSeekForward()) { 
                        mVideoView.seekTo(mVideoPosition);
                    }
                    mVideoView.setDuration(mVideoLastDuration);
                    break;
                case PAUSED:
                    //if video was paused, so it should be started.
                    doStartVideo(true, mVideoPosition, mVideoLastDuration, false);
                    pauseVideo();
                    break;
                default:
                    if(is4kDilogShow) {
                        m4kDilog.dismiss();
                        is4kDilogShow = false;
                    }
                    if (mConsumedDrmRight) {
                        doStartVideo(true, mVideoPosition, mVideoLastDuration);
                    } else {
                        doStartVideoCareDrm(true, mVideoPosition, mVideoLastDuration);
                    }
                    pauseVideoMoreThanThreeMinutes();
                    break;
                }
            }
            mVideoView.dump();
            mHasPaused = false;
        }
        mHandler.post(mProgressChecker);
    }
    
  ///M: enable wfd power saving mode when wfd is connected.
    private void enableWfdPowerSavingIfNeed() {
        if(MovieUtils.isWfdEnabled(mContext)) {
            if(mWfdPowerSaving != null) {
                mWfdPowerSaving.refreshPowerSavingPara();
            } else {
                mWfdPowerSaving = new WfdPowerSaving(mRootView,mActivityContext,mVideoView,mHandler) {
                    @Override
                    public void stopPowerSaving() {
                        if(mWfdPowerSaving != null) {
                            mWfdPowerSaving.release();
                            mWfdPowerSaving = null;
                        }
                    }
                    @Override
                    public void showController() {
                        mController.show();
                        mRootView.setBackgroundColor(Color.BLACK);
                    }
                };
            }
            mWfdPowerSaving.registerReceiver();
        } else {
            mWfdPowerSaving = null;
        }
    }
  /// @}

    private void pauseVideoMoreThanThreeMinutes() {
        // If we have slept for too long, pause the play
        // If is live streaming, do not pause it too
        long now = System.currentTimeMillis();
        if (now > mResumeableTime && !MovieUtils.isLiveStreaming(mVideoType)
                && ExtensionHelper.getMovieStrategy(mActivityContext).shouldEnableCheckLongSleep()) {
            if (mVideoCanPause || mVideoView.canPause()) {
                pauseVideo();
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "pauseVideoMoreThanThreeMinutes() now=" + now);
        }
    }

    public void onDestroy() {
        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
        }
        if(mWfdPowerSaving != null) {
            mWfdPowerSaving = null;
        }

        mVideoView.stopPlayback();
        mAudioBecomingNoisyReceiver.unregister();
        mServerTimeoutExt.clearTimeoutDialog();
    }

    // This updates the time bar display (if necessary). It is called every
    // second by mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        if (LOG) {
            MtkLog.v(TAG, "setProgress() mDragging=" + mDragging + ", mShowing=" + mShowing
                + ", mIsOnlyAudio=" + mIsOnlyAudio);
        }
        if (mDragging || (!mShowing && !mIsOnlyAudio)) {
            return 0;
        }
        int position = mVideoView.getCurrentPosition();
        int duration = mVideoView.getDuration();
        mController.setTimes(position, duration, 0, 0);
        if (mControllerRewindAndForwardExt != null && mControllerRewindAndForwardExt.getPlayPauseEanbled()) {
            updateRewindAndForwardUI();
        }
        return position;
    }

    private void doStartVideo(final boolean enableFasten, final int position, final int duration, boolean start) {
        if (LOG) {
            MtkLog.v(TAG, "doStartVideo(" + enableFasten + ", " + position + ", " + duration + ", " + start + ")");
        }
        ///M:dismiss some error dialog and if error still it will show again
        mVideoView.dismissAllowingStateLoss();
        Uri uri = mMovieItem.getUri();
        String mimeType = mMovieItem.getMimeType();
        if (!MovieUtils.isLocalFile(uri, mimeType)) {
            Map<String, String> header = new HashMap<String, String>(2);
            mController.showLoading(false);
            mOverlayExt.setPlayingInfo(MovieUtils.isLiveStreaming(mVideoType));
            mHandler.removeCallbacks(mPlayingChecker);
            mHandler.postDelayed(mPlayingChecker, 250);
            MtkLog.v(TAG, "doStartVideo() mCookie is " + mCookie);
            // / M: add play/pause asynchronous processing @{
            if (onIsRTSP()) {
                header.put(ASYNC_PAUSE_PLAY, String.valueOf(true));
                // /M: add for streaming cookie
                if (mCookie != null) {
                    header.put(MovieActivity.COOKIE, mCookie);
                }
                mVideoView.setVideoURI(mMovieItem.getUri(), header);
                // / @}
            } else {
                if (mCookie != null) {
                    // /M: add for streaming cookie
                    header.put(MovieActivity.COOKIE, mCookie);
                    mVideoView.setVideoURI(mMovieItem.getUri(), header);
                } else {
                    mVideoView.setVideoURI(mMovieItem.getUri(), null);
                }
            }
        } else {
            if(mWfdPowerSaving != null) {
                mWfdPowerSaving.disableWfdPowerSaving();
                mController.showPlaying();
                if(!mWfdPowerSaving.needShowController()) {
                    mController.hide();
                }
                mWfdPowerSaving.enableWfdPowerSaving();
            } else {
                mController.showPlaying();
                mController.hide();
            }
            ///M: set background to null to avoid lower power after start playing, 
            // because GPU is always runing, if not set null.
            //can not set too early, for onShown() will set backgound black.
            mHandler.removeCallbacks(mRemoveBackground);
            mHandler.postDelayed(mRemoveBackground, BLACK_TIMEOUT);
            mVideoView.setVideoURI(mMovieItem.getUri(), null);
        }
        if (start) {
            mVideoView.start();
        }
        /// @}
        //we may start video from stopVideo,
        //this case, we should reset canReplay flag according canReplay and loop
        boolean loop = mPlayerExt.getLoop();
        boolean canReplay = loop ? loop : mCanReplay;
        mController.setCanReplay(canReplay);
        if (position > 0 && (mVideoCanSeek || mVideoView.canSeekForward())) {
            mVideoView.seekTo(position);
        }
        if (enableFasten) {
            mVideoView.setDuration(duration);
        }
        setProgress();
    }

    private void doStartVideo(boolean enableFasten, int position, int duration) {
        ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(
                mAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        doStartVideo(enableFasten, position, duration, true);
    }
    
    private void playVideo() {
        if (LOG) {
            MtkLog.v(TAG, "playVideo()");
        }
        /// M: resume mPauseBuffering to false for show buffering info to user.
        mPlayerExt.mPauseBuffering = false;
        mTState = TState.PLAYING;
        mVideoView.start();
        mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        if (LOG) {
            MtkLog.v(TAG, "pauseVideo()");
        }
        mTState = TState.PAUSED;
        mVideoView.pause();
        mController.showPaused();
        setProgress();
    }

    // Below are notifications from VideoView
    @Override
    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        if (LOG) {
            MtkLog.v(TAG, "onError(" + player + ", " + arg1 + ", " + arg2 + ")");
        }
        mMovieItem.setError();
        if (mServerTimeoutExt.onError(player, arg1, arg2)) {
            return true;
        }
        if (mRetryExt.onError(player, arg1, arg2)) {
            return true;
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler.post(mProgressChecker);//always show progress
        // VideoView will show an error dialog if we return false, so no need
        // to show more message.
        //M:resume controller
        mController.setViewEnabled(true);
        mController.showErrorMessage("");
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (LOG) {
            MtkLog.v(TAG, "onCompletion() mCanReplay=" + mCanReplay);
        }
	
        // SetProgress when receive EOS to avoid that sometimes the progress
        // bar is not right because the position got from media player is
        // not in time.Notice that even if the position got again when receive
        // EOS,the progress bar may not exactly right for native may return
        // an inaccurate position.
        setProgress();
        if (mMovieItem.getError()) {
            MtkLog.w(TAG, "error occured, exit the video player!");
            mActivityContext.finish();
            return;
        }
        if (mPlayerExt.getLoop()) {
            onReplay();
        } else { //original logic
            mTState = TState.COMPELTED;
            if (mCanReplay) {
                mController.showEnded();
            }
            onCompletion();
        }
    }
    
    public void onCompletion() {
    }

    // Below are notifications from ControllerOverlay
    @Override
    public void onPlayPause() {
        if (mVideoView.isPlaying()) {
            if (mVideoView.canPause()) {
                pauseVideo();
                //set view disabled(play/pause asynchronous processing)
                mController.setViewEnabled(false);
            }
        } else {
            playVideo();
            //set view disabled(play/pause asynchronous processing)
            mController.setViewEnabled(false);
        }
    }
    
    public boolean isPlaying(){
        return mVideoView.isPlaying();
    }

    @Override
    public void onSeekStart() {
        if (LOG) {
            MtkLog.v(TAG, "onSeekStart() mDragging=" + mDragging);
        }
        mDragging = true;
    }

    @Override
    public void onSeekMove(int time) {
        if (LOG) {
            MtkLog.v(TAG, "onSeekMove(" + time + ") mDragging=" + mDragging);
        }
        if (!mDragging) {
            mVideoView.seekTo(time);
        }
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        if (LOG) {
            MtkLog.v(TAG, "onSeekEnd(" + time + ") mDragging=" + mDragging);
        }
        mDragging = false;
        mVideoView.seekTo(time);
        //setProgress();
    }

    @Override
    public void onShown() {
        mShowing = true;
        if (isMoviePartialVisible()) {
            onHidden();
            return;
        } else {
            if (LOG) {
                MtkLog.v(TAG, "onShown");
        }
        mHandler.removeCallbacks(mRemoveBackground);
        mRootView.setBackgroundColor(Color.BLACK);
        mShowing = true;
        setProgress();
        /// M:if it isn't first open, no need to show action bar 500ms.
        showSystemUi(true, false);
        if(mWfdPowerSaving != null && mWfdPowerSaving.isPowerSavingEnable()) {
           mWfdPowerSaving.cancelCountDown();
        }
        }
    }
    @Override
    public void onHidden() {
        if (LOG) {
            MtkLog.v(TAG, "onHidden");
        }
        if(mWfdPowerSaving != null) {
            /// M: In WFD Extension mode, Time will always update.
            if(!mWfdPowerSaving.needShowController()) {
                mShowing = false;
            }
        } else {
            mShowing = false;
        }
        /// M: if show action bar is not finish, can not to hidden it. @{
        if (mIsDelayFinish) {
            MtkLog.v(TAG, "mIsDelayFinish " + mIsDelayFinish);
            if (isMoviePartialVisible()) {
                // when video is partially visible, do not create
                // a temp window
                showSystemUi(true, false);
                mRootView.setBackgroundColor(Color.BLACK);
            } else {
                showSystemUi(false, false);
                ///M: set background to null avoid lower power, 
                // because GPU is always running, if not set null.
                //delay 1000ms is to avoid ghost image when action bar do slide animation, 
                mHandler.removeCallbacks(mRemoveBackground);
                mHandler.postDelayed(mRemoveBackground, 2 * BLACK_TIMEOUT);
                if (mWfdPowerSaving != null && !mHasPaused && mWfdPowerSaving.isPowerSavingEnable()) {
                    mWfdPowerSaving.startCountDown();
                }
            }
        }
        /// @}
    }
    @Override
    public boolean onIsRTSP() {
        return MovieUtils.isRTSP(mVideoType);
    }
    
    @Override
    public boolean wfdNeedShowController() {
      if(mWfdPowerSaving != null) {
    	  return  mWfdPowerSaving.needShowController();
      } else {
    	  return false;
      }
    }
    
    
    
    public void updateRewindAndForwardUI(){
        if(mControllerRewindAndForwardExt != null){
            mControllerRewindAndForwardExt.showControllerButtonsView(mPlayerExt
                    .canStop(), mVideoView.canSeekBackward()
                    && mVideoView.getCurrentPosition() > 0 && mControllerRewindAndForwardExt.getTimeBarEanbled(), mVideoView
                    .canSeekForward()
                    && (mVideoView.getCurrentPosition() < mVideoView
                            .getDuration()) && mControllerRewindAndForwardExt.getTimeBarEanbled());
        }
    }

    @Override
    public void onReplay() {
        if (LOG) {
            MtkLog.v(TAG, "onReplay()");
        }
        mFirstBePlayed = true;
        if (mRetryExt.handleOnReplay()) {
            return;
        }
        //M: FOR MTK_SUBTITLE_SUPPORT//@{
        if(MediatekFeature.MTK_SUBTITLE_SUPPORT){
            if (null != mSubTitleView) {
	        mSubTitleView.setTextOrBitmap(null);
	    }
        }
        //@}
        doStartVideoCareDrm(false, 0, 0);
    }

    // Below are key events passed from MovieActivity.
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (LOG) {
            MtkLog.v(TAG, "onKeyDown keyCode = " +keyCode);
        }
        // Some headsets will fire off 7-10 events on a single click
        if (event.getRepeatCount() > 0) {
            return isMediaKey(keyCode);
        }
        
        if (!mController.getTimeBarEnabled()){
            MtkLog.v(TAG, "onKeyDown, can not play or pause");
            return false;
        }
        
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mVideoView.isPlaying() && mVideoView.canPause()) {
                    pauseVideo();
                } else {
                    playVideo();
                }
                //set view disabled(play/pause asynchronous processing)
                mController.setViewEnabled(false);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (mVideoView.isPlaying() && mVideoView.canPause()) {
                    pauseVideo();
                    //set view disabled(play/pause asynchronous processing)
                    mController.setViewEnabled(false);
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (!mVideoView.isPlaying()) {
                    playVideo();
                    //set view disabled(play/pause asynchronous processing)
                    mController.setViewEnabled(false);
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if(mActivityContext.mMovieList == null) {
                    return false;
                }
                mPlayerExt.startNextVideo(mActivityContext.mMovieList.getPrevious(mMovieItem));
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if(mActivityContext.mMovieList == null) {
                    return false;
                }
                mPlayerExt.startNextVideo(mActivityContext.mMovieList.getNext(mMovieItem));	
                return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return isMediaKey(keyCode);
    }

    private static boolean isMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE;
    }

    // We want to pause when the headset is unplugged.
    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {

        public void register() {
            mContext.registerReceiver(this,
                    new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }

        public void unregister() {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MtkLog.v(TAG, "AudioBecomingNoisyReceiver onReceive");
            if (!mController.getTimeBarEnabled()){
                MtkLog.v(TAG, "AudioBecomingNoisyReceiver, can not play or pause");
                return;
            }
            if (mVideoView.isPlaying() && mVideoView.canPause()) { 
                pauseVideo();
                //set view disabled(play/pause asynchronous processing)
                mController.setViewEnabled(false);
            }
        }
    }



    private void init(final MovieActivity movieActivity, IMovieItem info, boolean canReplay) {
        mActivityContext = movieActivity;
        mCanReplay = canReplay;
        mMovieItem = info;
        mVideoType = MovieUtils.judgeStreamingType(info.getUri(), info.getMimeType());
        
        //for toast more info and live streaming
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnBufferingUpdateListener(this);
        mVideoView.setOnVideoSizeChangedListener(this);
        // M: FOR MTK_SUBTITLE_SUPPORT
        //@{
        if(MediatekFeature.MTK_SUBTITLE_SUPPORT) {
            mVideoView.setOnTimedTextListener(this); 
        } 
        //@}      
        
        mRootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
               if(!mHideController) {
                    mController.show();
                }
                return true;
            }
        });
        mOverlayExt = mController.getOverlayExt();
        mControllerRewindAndForwardExt = mController.getControllerRewindAndForwardExt();
        if(mControllerRewindAndForwardExt != null){
            mControllerRewindAndForwardExt.setIListener(mRewindAndForwardListener);
        }
    }
    
    public IMoviePlayer getMoviePlayerExt() {
        return mPlayerExt;
    }
    
    public SurfaceView getVideoSurface() {
        return mVideoView;
    }
    
    private void onSaveInstanceStateMore(Bundle outState) {
        //for more details
        mServerTimeoutExt.onSaveInstanceState(outState);
        outState.putInt(KEY_VIDEO_LAST_DURATION, mVideoLastDuration);
        outState.putBoolean(KEY_VIDEO_CAN_PAUSE, mVideoView.canPause());
        /// M: add this for deal with change language or other case which cause activity destory but not save right state
        /// @{
        if (mVideoCanSeek || mVideoView.canSeekForward()) { 
            outState.putBoolean(KEY_VIDEO_CAN_SEEK, true);
        } else {
            outState.putBoolean(KEY_VIDEO_CAN_SEEK, false);
        }
        /// @}
        outState.putBoolean(KEY_CONSUMED_DRM_RIGHT, mConsumedDrmRight);
        outState.putInt(KEY_VIDEO_STREAMING_TYPE, mVideoType);
        outState.putString(KEY_VIDEO_STATE, String.valueOf(mTState));
        outState.putString(KEY_VIDEO_CURRENT_URI, mMovieItem.getUri().toString());
        mScreenModeExt.onSaveInstanceState(outState);
        mRetryExt.onSaveInstanceState(outState);
        mPlayerExt.onSaveInstanceState(outState);
        if (LOG) {
            MtkLog.v(TAG, "onSaveInstanceState(" + outState + ")");
        }
    }

    private void onRestoreInstanceState(Bundle icicle) {
        mVideoLastDuration = icicle.getInt(KEY_VIDEO_LAST_DURATION);
        mVideoCanPause = icicle.getBoolean(KEY_VIDEO_CAN_PAUSE);
        mVideoCanSeek = icicle.getBoolean(KEY_VIDEO_CAN_SEEK);
        mConsumedDrmRight = icicle.getBoolean(KEY_CONSUMED_DRM_RIGHT);
        mVideoType = icicle.getInt(KEY_VIDEO_STREAMING_TYPE);
        mTState = TState.valueOf(icicle.getString(KEY_VIDEO_STATE));
        mMovieItem.setUri(Uri.parse(icicle.getString(KEY_VIDEO_CURRENT_URI)));
        mScreenModeExt.onRestoreInstanceState(icicle);
        mServerTimeoutExt.onRestoreInstanceState(icicle);
        mRetryExt.onRestoreInstanceState(icicle);
        mPlayerExt.onRestoreInstanceState(icicle);
        if (LOG) {
            MtkLog.v(TAG, "onRestoreInstanceState(" + icicle + ")");
        }
    }
    /// @}
    
    private void clearVideoInfo() {
        mVideoPosition = 0;
        mVideoLastDuration = 0;
        mIsOnlyAudio = false;
        mConsumedDrmRight = false;
        if (mServerTimeoutExt != null) {
            mServerTimeoutExt.clearServerInfo();
        }
    }

    private void getVideoInfo(MediaPlayer mp) {
        if (!MovieUtils.isLocalFile(mMovieItem.getUri(), mMovieItem.getMimeType())) {
            Metadata data = mp.getMetadata(MediaPlayer.METADATA_ALL,
                    MediaPlayer.BYPASS_METADATA_FILTER);
            if (data != null) {
                mServerTimeoutExt.setVideoInfo(data);
                mPlayerExt.setVideoInfo(data);
            } else {
                MtkLog.w(TAG, "Metadata is null!");
            }
            int duration = mp.getDuration();
            if (duration <= 0) {
                mVideoType = MovieUtils.VIDEO_TYPE_SDP;// correct it
            } else {
                //correct sdp to rtsp
                if (mVideoType == MovieUtils.VIDEO_TYPE_SDP) {
                    mVideoType = MovieUtils.VIDEO_TYPE_RTSP;
                }
            }
            
            if (LOG) {
                MtkLog.v(TAG, "getVideoInfo() duration=" + duration);
            }
        }
    }
    
    /// M: FOR MTK_SUBTITLE_SUPPORT @{
    @Override
    public void onTimedText(MediaPlayer mp, TimedText text) {
        if (LOG) {
            MtkLog.v(TAG," AudioAndSubtitle mOnTimedTextListener.onTimedTextListener("
							+ mp + ")" + " text = " + text);
        }
        if (text != null ) {
            mSubTitleView.setTextOrBitmap(text);
        } else {
            mSubTitleView.setTextOrBitmap(null);
        }
    }
    ///@}

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (LOG) {
            MtkLog.v(TAG, "onPrepared(" + mp + ")");
        }
        getVideoInfo(mp);
        if (mVideoType != MovieUtils.VIDEO_TYPE_LOCAL) { //here we get the correct streaming type.
            mOverlayExt.setPlayingInfo(MovieUtils.isLiveStreaming(mVideoType));
        }
        boolean canPause = mVideoView.canPause();
        boolean canSeek = mVideoView.canSeekBackward() && mVideoView.canSeekForward();
        mOverlayExt.setCanPause(canPause);
        mOverlayExt.setCanScrubbing(canSeek);
        //resume play pause button (play/pause asynchronous processing)
        mController.setPlayPauseReplayResume();
        if (!canPause && !mVideoView.isTargetPlaying()) {
            mVideoView.start();
        }
        updateRewindAndForwardUI();
        if (LOG) {
            MtkLog.v(TAG, "onPrepared() canPause=" + canPause + ", canSeek=" + canSeek);
        }
        //FOR MTK_SUBTITLE_SUPPORT
        //@{ TODO set menu visible or invisible
        final int minTrackNumber = 1;
        if(MediatekFeature.MTK_SUBTITLE_SUPPORT){
            addExternalSubtitle();
            if(getTrackNumFromVideoView(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) < minTrackNumber){
        	mActivityContext.setSubtitleMenuItemVisible(false);
            } else {
                 mActivityContext.setSubtitleMenuItemVisible(true);
            }
        }
        //@}
        //FOR MTK_AUDIO_CHANGE_SUPPORT 
        //@{ TODO set menu visible or invisible
        if (MediatekFeature.MTK_AUDIO_CHANGE_SUPPORT && mVideoType == MovieUtils.VIDEO_TYPE_LOCAL) {
        	if(getTrackNumFromVideoView(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) < minTrackNumber){
        		mActivityContext.setAudioMenuItemVisible(false);
        	} else {
        		mActivityContext.setAudioMenuItemVisible(true);
        	}
	        if(mSelcetAudioTrackIdx != 0 ){
	        	mVideoView.selectTrack(mSelcetAudioTrackIdx);           
	        }
        }
        ///@}
        //for stereo display feature
        if (mActivityContext != null) {
            mActivityContext.refreshMovieInfo(mMovieItem);
        }
    }
    
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (LOG) {
            MtkLog.v(TAG, "onInfo() what:" + what + " extra:" + extra);
        }
        if (mRetryExt.onInfo(mp, what, extra)) {
            return true;
        }
        if(mPlayPauseProcessExt.onInfo(mp, what, extra)){
            return true;
        }
        isPlaySupported(what, extra);
        // / M: add log for performance auto test @{
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            long endTime = System.currentTimeMillis();
            MtkLog.i(TEST_CASE_TAG,
                    "[Performance Auto Test][VideoPlayback] The duration of open a video end ["
                            + endTime + "]");
            MtkLog.i(TAG, "[CMCC Performance test][Gallery2][Video Playback] open mp4 file end ["
                    + endTime + "]");

            if(mWfdPowerSaving != null && mTState == TState.PLAYING 
                    && MovieUtils.isLocalFile(mMovieItem.getUri(), mMovieItem.getMimeType())) {
                mWfdPowerSaving.startCountDown();
            }
        }
        // / @}
        // /M:For http streaming, show spinner while seek to a new
        // position.
        handleSeeking(what, extra);

        // /M:HLS_audio-only_02 The mediaplayer shall support metadata
        // embedded in the MPEG2-TS file
        if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
            handleMetadataUpdate(mp, what, extra);
        }
        return false;
    }
    
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (!mPlayerExt.pauseBuffering()) {
            mOverlayExt.showBuffering(!MovieUtils.isRtspOrSdp(mVideoType), percent);
        }
        if (LOG) {
            MtkLog.v(TAG, "onBufferingUpdate(" + percent + ") pauseBuffering=" + mPlayerExt.pauseBuffering());
        }
    }
    
    // / M: Check whether video or audio is supported @{
    private boolean isPlaySupported(int what, int extra) {
        if (mFirstBePlayed) {
            int messageId = 0;
            if (extra == ERROR_CANNOT_CONNECT || extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED
                    || extra == ERROR_FORBIDDEN) {
                messageId = R.string.VideoView_info_text_network_interrupt;
            } else {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_NOT_SUPPORTED) {
                    messageId = R.string.VideoView_info_text_video_not_supported;
                } else if (what == MediaPlayer.MEDIA_INFO_AUDIO_NOT_SUPPORTED) {
                    messageId = R.string.audio_not_supported;
                }
            }
            if (messageId != 0) {
                String message = mActivityContext.getString(messageId);
                Toast.makeText(mActivityContext, message, Toast.LENGTH_SHORT).show();
                mFirstBePlayed = false;
                return true;
            }
        }
        return false;
    }
    // / @}
    
    // /M:HLS_audio-only_02 The mediaplayer shall support metadata
    // embedded in the MPEG2-TS file @{
    private void handleMetadataUpdate(MediaPlayer mp, int what, int extra) {
        MtkLog.v(TAG, "handleMetadataUpdate entry");
        Metadata data =
                mp.getMetadata(MediaPlayer.METADATA_ALL, MediaPlayer.BYPASS_METADATA_FILTER);
        MtkLog.v(TAG, "handleMetadataUpdate data is " + data);
        String mimeType = new String();
        byte[] album = null;
        if (data.has(Metadata.MIME_TYPE)) {
            mimeType = data.getString(Metadata.MIME_TYPE);
            MtkLog.v(TAG, "handleMetadataUpdate mimeType is " + mimeType);
        }
        if (data.has(Metadata.ALBUM_ART)) {
            album = data.getByteArray(Metadata.ALBUM_ART);
            if (album != null) {
                mOverlayExt.setLogoPic(album);
                MtkLog.v(TAG, "handleMetadataUpdate album size is " + album.length);
            } else {
                mOverlayExt.setBottomPanel(true, true);
                MtkLog.v(TAG, "handleMetadataUpdate album is null");
            }
        }
    }// / @}

    // /M:For http streaming, show spinner while seek to a new position.
    private void handleSeeking(int what, int extra) {
        if (mVideoType == MovieUtils.VIDEO_TYPE_HTTP) {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                mController.showLoading(true);
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                // /M: when stream is pasue, after seek, the state is still pause
                if (mTState == TState.PAUSED) {
                    mController.showPaused();
                }
                else {
                    mController.showPlaying();
                }
            }
        }
    } // / @}


    /// @}
    
    /// M: for drm feature @{
    private boolean mConsumedDrmRight = false;
    private IMovieDrmExtension mDrmExt = ExtensionHelper.getMovieDrmExtension(mActivityContext);
    private void doStartVideoCareDrm(final boolean enableFasten, final int position, final int duration) {
        if (LOG) {
            MtkLog.v(TAG, "doStartVideoCareDrm(" + enableFasten + ", " + position + ", " + duration + ")");
        }
        if (MovieUtils.isLivePhoto(mActivityContext, mMovieItem.getUri())) {
            mPlayerExt.setLoop(true);
        }
        //case: play 4k video first, and then plug in hdmi cable,
        //notify HdmiManager 4k video is playing now.
        if (MovieUtils.is4kVideo(mActivityContext, mMovieItem.getUri())) {
            MovieUtils.notifyHdmiManager4kPlaying(true);
            notifyHdmiManager4kPlaying = true;
        }
        mTState = TState.PLAYING;
        if (!mDrmExt.handleDrmFile(mActivityContext, mMovieItem, new IMovieDrmCallback() {
            @Override
            public void onContinue() {
                doStartVideo(enableFasten, position, duration);
                mConsumedDrmRight = true;
                ///M: set background to null to avoid lower power after start playing, 
                // because GPU is always runing, if not set null.
                //can not set too early, for onShown() will set backgound black.
                mRootView.setBackground(null);
            }
            @Override
            public void onStop() {
                mPlayerExt.setLoop(false);
                onCompletion(null);
            }
        })) {
            doStartVideo(enableFasten, position, duration);
            ///M: set background to null to avoid lower power after start playing, 
            // because GPU is always runing, if not set null.
            //can not set too early, for onShown() will set backgound black.
            mRootView.setBackground(null);
        }
    }
    
    /// M: for dynamic change video size(http live streaming) @{
    private boolean mIsOnlyAudio = false;
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        //reget the audio type
        if (width != 0 && height != 0) {
            mIsOnlyAudio = false;
        } else {
            mIsOnlyAudio = true;
        }
        mOverlayExt.setBottomPanel(mIsOnlyAudio, true);
        if (LOG) {
            MtkLog.v(TAG, "onVideoSizeChanged(" + width + ", " + height + ") mIsOnlyAudio=" + mIsOnlyAudio);
        }
    }
    /// @}
    
    private void dump() {
        if (LOG) {
            MtkLog.v(TAG, "dump() mHasPaused=" + mHasPaused
                + ", mVideoPosition=" + mVideoPosition + ", mResumeableTime=" + mResumeableTime
                + ", mVideoLastDuration=" + mVideoLastDuration + ", mDragging=" + mDragging
                + ", mConsumedDrmRight=" + mConsumedDrmRight + ", mVideoCanSeek=" + mVideoCanSeek
                + ", mVideoCanPause=" + mVideoCanPause + ", mTState=" + mTState
                + ", mIsShowResumingDialog=" + mIsShowResumingDialog);
        }
    }
    
    //for more killed case, same as videoview's state and controller's state.
    //will use it to sync player's state.
    //here doesn't use videoview's state and controller's state for that
    //videoview's state doesn't have reconnecting state and controller's state has temporary state.
    private enum TState {
        PLAYING,
        PAUSED,
        STOPED,
        COMPELTED,
        RETRY_ERROR
    }
    

    private IRewindAndForwardListener mRewindAndForwardListener = new ControllerRewindAndForwardExt();
    
    interface Restorable {
        void onRestoreInstanceState(Bundle icicle);
        void onSaveInstanceState(Bundle outState);
    }

    private class RetryExtension implements Restorable, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener {
        private static final String KEY_VIDEO_RETRY_COUNT = "video_retry_count";
        private int mRetryDuration;
        private int mRetryPosition;
        private int mRetryCount;
        
        public void retry() {
            doStartVideoCareDrm(true, mRetryPosition, mRetryDuration);
            if (LOG) {
                MtkLog.v(TAG, "retry() mRetryCount=" + mRetryCount + ", mRetryPosition=" + mRetryPosition);
            }
        }
        
        public void clearRetry() {
            if (LOG) {
                MtkLog.v(TAG, "clearRetry() mRetryCount=" + mRetryCount);
            }
            mRetryCount = 0;
        }
        
        public boolean reachRetryCount() {
            if (LOG) {
                MtkLog.v(TAG, "reachRetryCount() mRetryCount=" + mRetryCount);
            }
            if (mRetryCount > 3) {
                return true;
            }
            return false;
        }
        
        public int getRetryCount() {
            if (LOG) {
                MtkLog.v(TAG, "getRetryCount() return " + mRetryCount);
            }
            return mRetryCount;
        }
        
        public boolean isRetrying() {
            boolean retry = false;
            if (mRetryCount > 0) {
                retry = true;
            }
            if (LOG) {
                MtkLog.v(TAG, "isRetrying() mRetryCount=" + mRetryCount);
            }
            return retry;
        }
        
        @Override
        public void onRestoreInstanceState(Bundle icicle) {
            mRetryCount = icicle.getInt(KEY_VIDEO_RETRY_COUNT);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putInt(KEY_VIDEO_RETRY_COUNT, mRetryCount);
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_ERROR_CANNOT_CONNECT_TO_SERVER) {
                //get the last position for retry
                mRetryPosition = mVideoView.getCurrentPosition();
                mRetryDuration = mVideoView.getDuration();
                mRetryCount++;
                if (reachRetryCount()) {
                    mTState = TState.RETRY_ERROR;
                    mOverlayExt.showReconnectingError();
                    /// M: set replay is true for user can reload video when 
                    //media error can not connect to server
                    mController.setCanReplay(true);
                } else {
                    mOverlayExt.showReconnecting(mRetryCount);
                    retry();
                }
                return true;
            }
            return false;
        }
        
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_GET_BUFFER_DATA) {
                //this means streaming player has got the display data
                //so we can retry connect server if it has connection error.
                clearRetry();
                return true;
            }
            if ((what == StereoHelper.MEDIA_INFO_3D ||
                what == StereoHelper.STEREO_TYPE_2D) &&
                StereoHelper.needAutoFormatDection(mMovieItem.getStereoType())) {
                MtkLog.d(TAG,"onInfo:setStereoType:" + extra);
                mMovieItem.setStereoType(extra);
                mActivityContext.refreshMovieInfo(mMovieItem);
                // invalidate and refresh options menu
                mActivityContext.invalidateOptionsMenu();
                StereoHelper.updateStereoLayout(mContext,
                              mMovieItem.getUri(), mMovieItem.getStereoType());
                if (StereoHelper.isStereo(mMovieItem.getStereoType()) && what == StereoHelper.MEDIA_INFO_3D) {
                    //tell user mode is auto matically converted
                    Toast.makeText((Context) mActivityContext,
                            R.string.stereo3d_auto_switch_to_3d_mode,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        }
        
        public boolean handleOnReplay() {
            if (isRetrying()) { //from connecting error
                clearRetry();
                int errorPosition = mVideoView.getCurrentPosition();
                int errorDuration = mVideoView.getDuration();
                doStartVideoCareDrm(errorPosition > 0, errorPosition, errorDuration);
                if (LOG) {
                    MtkLog.v(TAG, "onReplay() errorPosition=" + errorPosition + ", errorDuration=" + errorDuration);
                }
                return true;
            }
            return false;
        }
        
        public void showRetry() {
            mOverlayExt.showReconnectingError();
            if (mVideoCanSeek || mVideoView.canSeekForward()) { 
                mVideoView.seekTo(mVideoPosition);
            }
            mVideoView.setDuration(mVideoLastDuration);
            mRetryPosition = mVideoPosition;
            mRetryDuration = mVideoLastDuration;
        }
    }
    
    private class ScreenModeExt implements Restorable, ScreenModeListener {
        private static final String KEY_VIDEO_SCREEN_MODE = "video_screen_mode";
        private int mScreenMode = ScreenModeManager.SCREENMODE_BIGSCREEN;
        private ScreenModeManager mScreenModeManager = new ScreenModeManager();
        
        public void setScreenMode() {
            mVideoView.setScreenModeManager(mScreenModeManager);
            mController.setScreenModeManager(mScreenModeManager);
            mScreenModeManager.addListener(this);
            mScreenModeManager.setScreenMode(mScreenMode);//notify all listener to change screen mode
            if (LOG) {
                MtkLog.v(TAG, "setScreenMode() mScreenMode=" + mScreenMode);
            }
        }
        
        @Override
        public void onScreenModeChanged(int newMode) {
            mScreenMode = newMode;//changed from controller
            if (LOG) {
                MtkLog.v(TAG, "OnScreenModeClicked(" + newMode + ")");
            }
        }
        
        @Override
        public void onRestoreInstanceState(Bundle icicle) {
            mScreenMode = icicle.getInt(KEY_VIDEO_SCREEN_MODE, ScreenModeManager.SCREENMODE_BIGSCREEN);
        }
        
        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putInt(KEY_VIDEO_SCREEN_MODE, mScreenMode);
        }
    }
    
    ///M: for CU 4.0 add rewind and forward function
    private class ControllerRewindAndForwardExt implements IRewindAndForwardListener {
        @Override
        public void onPlayPause() {
            onPlayPause();
        }
        @Override
        public void onSeekStart() {
            onSeekStart();
        }
        @Override
        public void onSeekMove(int time) {
            onSeekMove(time);
        }
        @Override
        public void onSeekEnd(int time, int trimStartTime, int trimEndTime) {
            onSeekEnd(time,trimStartTime,trimEndTime);
        }
        @Override
        public void onShown() {
            onShown();
        }
        @Override
        public void onHidden() {
            onHidden();
        }
        @Override
        public void onReplay() {
            onReplay();
        }
        
        @Override
        public boolean onIsRTSP() {
            return false;
        }
        @Override
        public void onStopVideo() {
            MtkLog.v(TAG, "ControllerRewindAndForwardExt onStopVideo()");
            if (mPlayerExt.canStop()) {
                mPlayerExt.stopVideo();
                mControllerRewindAndForwardExt.showControllerButtonsView(false,
                        false, false);
            }
        }
        @Override
        public void onRewind() {
            MtkLog.v(TAG, "ControllerRewindAndForwardExt onRewind()");
            if (mVideoView != null && mVideoView.canSeekBackward()) {
                mControllerRewindAndForwardExt
                        .showControllerButtonsView(
                                mPlayerExt.canStop(),
                                false,
                                mVideoView.canSeekForward()
                                        && (mVideoView.getCurrentPosition() < mVideoView
                                                .getDuration())
                                        && mControllerRewindAndForwardExt
                                                .getTimeBarEanbled());
                int stepValue = getStepOptionValue();
                int targetDuration = mVideoView.getCurrentPosition()
                        - stepValue < 0 ? 0 : mVideoView.getCurrentPosition()
                        - stepValue;
                MtkLog.v(TAG, "onRewind targetDuration " + targetDuration);
                mVideoView.seekTo(targetDuration);
            } else {
                mControllerRewindAndForwardExt
                        .showControllerButtonsView(
                                mPlayerExt.canStop(),
                                false,
                                mVideoView.canSeekForward()
                                        && (mVideoView.getCurrentPosition() < mVideoView
                                                .getDuration())
                                        && mControllerRewindAndForwardExt
                                                .getTimeBarEanbled());
            }
        }
        @Override
        public void onForward() {
            MtkLog.v(TAG, "ControllerRewindAndForwardExt onForward()");
            if (mVideoView != null && mVideoView.canSeekForward()) {
                mControllerRewindAndForwardExt.showControllerButtonsView(
                        mPlayerExt.canStop(), mVideoView.canSeekBackward()
                                && mVideoView.getCurrentPosition() > 0
                                && mControllerRewindAndForwardExt
                                        .getTimeBarEanbled(), false);
                int stepValue = getStepOptionValue();
                int targetDuration = mVideoView.getCurrentPosition()
                        + stepValue > mVideoView.getDuration() ? mVideoView
                        .getDuration() : mVideoView.getCurrentPosition()
                        + stepValue;
                MtkLog.v(TAG, "onForward targetDuration " + targetDuration);
                mVideoView.seekTo(targetDuration);
            } else {
                mControllerRewindAndForwardExt.showControllerButtonsView(
                        mPlayerExt.canStop(), mVideoView.canSeekBackward()
                                && mVideoView.getCurrentPosition() > 0
                                && mControllerRewindAndForwardExt
                                        .getTimeBarEanbled(), false);
            }
        }
        @Override
        public boolean wfdNeedShowController() {
            return wfdNeedShowController();
        }
    }
    /// M: FOR MTK_AUDIO_CHANGE_SUPPORT & MTK_SUBTITLE_SUPPORT
    /// @{
    /** get the track number
    * @param type can be MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO 
     *                  or 
     *                  MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT
     *                  or
     *                  TYPE_TRACK_INFO_BOTH for both type
    */

    public int getTrackNumFromVideoView(int type){
    	TrackInfo trackInfo[] = mVideoView.getTrackInfo();
    	int AudioNum = 0;
    	int SubtilteNum = 0;
		if (trackInfo == null) {
			MtkLog.v(TAG,
					"---AudioAndSubtitle getTrackInfoFromVideoView: NULL ");
			return NONE_TRACK_INFO;
		}
		int trackLength = trackInfo.length;
		for (int i = 0; i < trackLength; i++) {
			if (trackInfo[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
				AudioNum++;
				continue;
			} else if (trackInfo[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
				SubtilteNum++;
				continue;
			}
		}
		MtkLog.v(TAG,
				"---AudioAndSubtitle getTrackNumFromVideoView: trackInfo.length = "
						+ trackLength + ", AudioNum= " + AudioNum
						+ ", SubtilteNum=" + SubtilteNum);
		if (type == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
			return AudioNum;
		} else if (type == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
			return SubtilteNum;
		} else {
			return trackLength;
		}
    	 
    }
    
    private void addExternalSubtitle(){
    	File[] externalSubTitlefiles = mActivityContext.listExtSubTitleFileNameWithPath();
    	if(null != externalSubTitlefiles){
    		
            for(File file : externalSubTitlefiles){
    	        String filePath = file.getPath();
                if(filePath.endsWith(MtkUtils.SUBTITLE_SUPPORT_WITH_SUFFIX_SRT)) {
                    mVideoView.addExtTimedTextSource(file.getPath(),MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
                    continue;
                } else if(filePath.endsWith(MtkUtils.SUBTITLE_SUPPORT_WITH_SUFFIX_SMI)) {
                    mVideoView.addExtTimedTextSource(file.getPath(),MtkUtils.MEDIA_MIMETYPE_TEXT_SUBSMI);
                    continue;
                } else if(filePath.endsWith(MtkUtils.SUBTITLE_SUPPORT_WITH_SUFFIX_SUB)) {
                    mVideoView.addExtTimedTextSource(file.getPath(),MtkUtils.MEDIA_MIMETYPE_TEXT_SUB);
                    continue;
                } else if(filePath.endsWith(MtkUtils.SUBTITLE_SUPPORT_WITH_SUFFIX_IDX)) {
                    mVideoView.addExtTimedTextSource(file.getPath(),MtkUtils.MEDIA_MIMETYPE_TEXT_IDX);
                    continue;
                } else if(filePath.endsWith(MtkUtils.SUBTITLE_SUPPORT_WITH_SUFFIX_TXT)) {
                    mVideoView.addExtTimedTextSource(file.getPath(),MtkUtils.MEDIA_MIMETYPE_TEXT_SUBTXT);
                    continue;
                } else if(filePath.endsWith(MtkUtils.SUBTITLE_SUPPORT_WITH_SUFFIX_ASS)) {
                    mVideoView.addExtTimedTextSource(file.getPath(),MtkUtils.MEDIA_MIMETYPE_TEXT_SUBASS);
                    continue;
                } else if(filePath.endsWith(MtkUtils.SUBTITLE_SUPPORT_WITH_SUFFIX_SSA)) {
                    mVideoView.addExtTimedTextSource(file.getPath(),MtkUtils.MEDIA_MIMETYPE_TEXT_SUBSSA);
                    continue;
                } else if(filePath.endsWith(MtkUtils.SUBTITLE_SUPPORT_WITH_SUFFIX_MPL)) {
                    mVideoView.addExtTimedTextSource(file.getPath(),MtkUtils.MEDIA_MIMETYPE_TEXT_SUBMPL);
                    continue;
                }
            }
    	}    	
    	//selsect the bookMark saved track. if mSelectSubTitleTrackIdx > hasTrackNum do not call this
        if (mSelectSubTitleTrackIdx != 0 
        		&& (mSelectSubTitleTrackIdx < getTrackNumFromVideoView(TYPE_TRACK_INFO_BOTH)) ){
        	 mVideoView.selectTrack(mSelectSubTitleTrackIdx);
        }
    }

    /**
     * show the dialog for user to select audio track or subtitle
     * @param TrackType can be MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO 
     *                  or 
     *                  MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT
     */
    private static final int MINI_TRACK_INFO_NUM= 1;
	public void showDialogForTrack(int TrackType) {
		TrackInfo trackInfo[] = mVideoView.getTrackInfo();
		if (trackInfo == null || trackInfo.length < 1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					mActivityContext);
			builder.setTitle(R.string.noAvailableeTrack);
			builder.show();
			return;
		}
		int trackInfoNum = 0;
		trackInfoNum = trackInfo.length;

		if (MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO == TrackType) {
			ArrayList<String> ListAudio = new ArrayList();
			ArrayList<Integer> ListAudioIdx = new ArrayList();
			int idx = 1;
			for (int i = 0; i < trackInfoNum; i++) {
				String at = null;
				if (trackInfo[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
					/*
					 * at = "Language-" +trackInfo[i].getLanguage() + "-" +
					 * mContext.getString(R.string.audioTrack)+ "# " + idx;
					 */
					at = mContext.getString(R.string.audioTrack) + "# " + idx;
					idx++;
				} else {
					continue;
				}
				ListAudio.add(at);
				ListAudioIdx.add(i);
			}
			MtkLog.v(TAG,
					"---AudioAndSubtitle showDialogForTrack: Audio.TrackInfo.size = "
							+ ListAudio.size());
			if (ListAudio.size() < MINI_TRACK_INFO_NUM) {
				ListAudio.add(mContext
						.getString(R.string.noAvailableeAudioTrack));
				ListAudioIdx.add(0);
			}
			String[] at1 = new String[ListAudio.size()];
			ListAudio.toArray(at1);
			Integer[] AudioIdx = new Integer[ListAudioIdx.size()];
			ListAudioIdx.toArray(AudioIdx);
			showDialog2Disp(at1, mSelectAudioIndex, AudioIdx,
					MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO);
		} else if (MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT == TrackType) {
			ArrayList<String> ListSubTitle = new ArrayList();
			ArrayList<Integer> ListSubTitleIdx = new ArrayList();
			ListSubTitle.add(mContext.getString(R.string.closeSubtitle));
			ListSubTitleIdx.add(0);
			int idx = 1;
			for (int i = 0; i < trackInfoNum; i++) {
				String at = null;
				if (trackInfo[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
					/*
					 * at = "Language-" +trackInfo[i].getLanguage() + "-" +
					 * mContext.getString(R.string.SubtitleSetting)+ "# " + idx;
					 */
					at = mContext.getString(R.string.SubtitleSetting) + "# "
							+ idx;
					idx++;
				} else {
					continue;
				}
				ListSubTitle.add(at);
				ListSubTitleIdx.add(i);
			}
			MtkLog.v(TAG, "---AudioAndSubtitle showDialogForTrack: list.size ="
					+ ListSubTitle.size());
			String[] at1 = new String[ListSubTitle.size()];
			ListSubTitle.toArray(at1);
			Integer[] SubTitleIdx = new Integer[ListSubTitleIdx.size()];
			ListSubTitleIdx.toArray(SubTitleIdx);
			if (mSelectSubTitleIndex >= ListSubTitleIdx.size()) {
				// if external subtitle has been deleted			
				mSelectSubTitleIndex = 0;
			}
			showDialog2Disp(at1, mSelectSubTitleIndex, SubTitleIdx,
					MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT);
		}
	}
  

	private void showDialog2Disp(String[] Track, int index,
			final Integer[] TrackIdx, final int TrackType) {	  

		Log.i(TAG, "AudioAndSubTitleChange showDialog2Disp: showSeclectDialog ");
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivityContext);
		if (MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO == TrackType) {
			builder.setTitle(R.string.audioTrackChange);
			builder.setSingleChoiceItems(Track, index,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// TODO
   						MtkLog.v(TAG, "AudioAndSubTitleChange  onClick whichButton = " + whichButton);    					
							if (mSelectAudioIndex == whichButton) {
								MtkLog.v(TAG, "AudioAndSubTitleChange  onClick whichButton SameChoice");
								dialog.dismiss();
								return;
							}
							if (RETURN_ERROR == mVideoView.selectTrack(TrackIdx[whichButton].intValue())) {
								if (mTState == TState.PAUSED) {
									MtkLog.v(TAG, "AudioAndSubTitleChange --- onClick if has error after selectTrack");
									playVideo();
									mVideoView.selectTrack(TrackIdx[whichButton].intValue());
									pauseVideo();
								}
							}
							mSelectAudioIndex = whichButton;
							mSelcetAudioTrackIdx = TrackIdx[whichButton]
									.intValue();
							dialog.dismiss();
						}
					});
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {

				}
			});
		} else if (MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT == TrackType) {
			builder.setTitle(R.string.subtitleTrackChange);
			builder.setSingleChoiceItems(Track, index,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// TODO
							MtkLog.v(TAG,
									"AudioAndSubTitleChange --- onClick whichButton = "
											+ whichButton);
							if (mSelectSubTitleIndex == whichButton) {
								MtkLog.v(TAG,"AudioAndSubTitleChange --- onClick whichButton SameChoice");
								dialog.dismiss();
							} else {
								if (whichButton == 0) {
									mVideoView.deselectTrack(TrackIdx[mSelectSubTitleIndex].intValue());
									mSubTitleView.setTextOrBitmap(null);
									mSelectSubTitleIndex = whichButton;
									mSelectSubTitleTrackIdx = TrackIdx[whichButton].intValue();
									dialog.dismiss();
								} else {
									if (mSelectSubTitleIndex != 0) {
										mVideoView.deselectTrack(TrackIdx[mSelectSubTitleIndex].intValue());
										mSubTitleView.setTextOrBitmap(null);
									}
									mVideoView.selectTrack(TrackIdx[whichButton].intValue());
									mSelectSubTitleIndex = whichButton;
									mSelectSubTitleTrackIdx = TrackIdx[whichButton].intValue();
							dialog.dismiss();
						}
		} 
						}
					});
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {

				}
			});
		} 
   		
   		final AlertDialog dialog = builder.create();
   		dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
               
                mPlayerExt.pauseIfNeed();
            }
        });
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
               
                mPlayerExt.resumeIfNeed();
            }
        });
   		dialog.show();
   }   
   ///@}
   
    public int getStepOptionValue(){
        final String slectedStepOption = "selected_step_option";
        final String videoPlayerData = "video_player_data";
        final int stepBase = 3000;
        final String stepOptionThreeSeconds = "0";
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return (Integer.parseInt(mPrefs.getString(slectedStepOption, stepOptionThreeSeconds)) + 1)
                * stepBase;
    }
    
    private class MoviePlayerExtension implements IMoviePlayer, Restorable {
        private static final String KEY_VIDEO_IS_LOOP = "video_is_loop";
        
        private BookmarkEnhance mBookmark;//for bookmark
        private String mAuthor;//for detail
        private String mTitle;//for detail
        private String mCopyRight;//for detail
        private boolean mIsLoop;
        private boolean mLastPlaying;
        private boolean mLastCanPaused;
        private boolean mPauseBuffering;
        private boolean mResumeNeed = false;
        
        @Override
        public void stopVideo() {
            if (LOG) {
                MtkLog.v(TAG, "stopVideo()");
            }
            mTState = TState.STOPED;
            mVideoView.clearSeek();
            mVideoView.clearDuration();
            mVideoView.stopPlayback();
            mVideoView.setResumed(false);
            mVideoView.setVisibility(View.INVISIBLE);
            mVideoView.setVisibility(View.VISIBLE);
            clearVideoInfo();
            mFirstBePlayed = false;
            mController.setCanReplay(true);
            mController.showEnded();
            mController.setViewEnabled(true);
            //FOR MTK_AUDIO_CHANGE_SUPPORT 
            //@{ 
            if(MediatekFeature.MTK_AUDIO_CHANGE_SUPPORT){
            	mActivityContext.setAudioMenuItemVisible(false);
            }
            ///@}
            // FOR MTK_SUBTITLE_SUPPORT
            if (MediatekFeature.MTK_SUBTITLE_SUPPORT) {
                if (null != mSubTitleView) { // M: FOR MTK_SUBTITLE_CHANGE_SUPPORT
                    mSubTitleView.setTextOrBitmap(null);
                }
                mActivityContext.setSubtitleMenuItemVisible(false);
            }
            //@}
            setProgress();
        }
        
        @Override
        public boolean canStop() {
            boolean stopped = false;
            if (mController != null) {
                stopped = mOverlayExt.isPlayingEnd();
            }
            if (LOG) {
                MtkLog.v(TAG, "canStop() stopped=" + stopped);
            }
            return !stopped;
        }

        @Override
        public void addBookmark() {
            if (mBookmark == null) {
                mBookmark = new BookmarkEnhance(mActivityContext);
            }
            String uri = String.valueOf(mMovieItem.getUri());
            if (mBookmark.exists(uri)) {
                Toast.makeText(mActivityContext, R.string.bookmark_exist, Toast.LENGTH_SHORT).show();
            } else {
                mBookmark.insert(mTitle, uri, mMovieItem.getMimeType(), 0);
                Toast.makeText(mActivityContext, R.string.bookmark_add_success, Toast.LENGTH_SHORT).show();
            }
            if (LOG) {
                MtkLog.v(TAG, "addBookmark() mTitle=" + mTitle + ", mUri=" + mMovieItem.getUri());
            }
        }

        @Override
        public boolean getLoop() {
            if (LOG) {
                MtkLog.v(TAG, "getLoop() return " + mIsLoop);
            }
            return mIsLoop;
        }
        /// M: FOR  MTK_SUBTITLE_SUPPORT
        /// @{
        @Override
        public void showSubtitleViewSetDialog(){
        	 
            if (mSubtitleSetDialog != null) {
                mSubtitleSetDialog.dismiss();
            }
            mSubtitleSetDialog = new SubtitleSettingDialog(mActivityContext,mSubTitleView);
            mSubtitleSetDialog.setOnShowListener(new OnShowListener() {
                
                @Override
                public void onShow(DialogInterface dialog) {
                    if (LOG) {
                        MtkLog.v(TAG, "mSubtitleSetDialog.onShow()");
                    }
                    pauseIfNeed();
                }
            });
            mSubtitleSetDialog.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (LOG) {
                        MtkLog.v(TAG, "mSubtitleSetDialog.onDismiss()");
                    }
                    resumeIfNeed();
                }
            });
        	mSubtitleSetDialog.show();
        }
        ///@}
        @Override
        public void setLoop(boolean loop) {
            if (LOG) {
                MtkLog.v(TAG, "setLoop(" + loop + ") mIsLoop=" + mIsLoop);
            }
            if (mVideoType == MovieUtils.VIDEO_TYPE_LOCAL) {
                mIsLoop = loop;
                if(mTState != TState.STOPED)
                {
                	mController.setCanReplay(loop);
                }
            }
        }

        @Override
        public void showDetail() {
            DetailDialog detailDialog = new DetailDialog(mActivityContext, mTitle, mAuthor, mCopyRight);
            detailDialog.setTitle(R.string.media_detail);
            detailDialog.setOnShowListener(new OnShowListener() {
                
                @Override
                public void onShow(DialogInterface dialog) {
                    if (LOG) {
                        MtkLog.v(TAG, "showDetail.onShow()");
                    }
                    pauseIfNeed();
                }
            });
            detailDialog.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (LOG) {
                        MtkLog.v(TAG, "showDetail.onDismiss()");
                    }
                    resumeIfNeed();
                }
            });
            detailDialog.show();
        }

        @Override
        public void startNextVideo(IMovieItem item) {
            IMovieItem next = item;
            if (next != null && next != mMovieItem) {
                int position = mVideoView.getCurrentPosition();
                int duration = mVideoView.getDuration();
                if(MediatekFeature.MTK_AUDIO_CHANGE_SUPPORT || MediatekFeature.MTK_SUBTITLE_SUPPORT){
                	mBookmarker.setBookmark(mMovieItem.getUri(), position, duration,
                    mSelcetAudioTrackIdx, mSelectSubTitleTrackIdx, mSelectAudioIndex, mSelectSubTitleIndex);
                }else{
                	mBookmarker.setBookmark(mMovieItem.getUri(), position, duration);
                }
                //mBookmarker.setBookmark(mMovieItem.getUri(), position, duration);
                mVideoView.stopPlayback();
                mVideoView.setVisibility(View.INVISIBLE);
                clearVideoInfo();
                mMovieItem = next;
                mActivityContext.refreshMovieInfo(mMovieItem);
                if (is4kVideoLimitation()) {
                    show4kVideoDialog(mMovieActivity, null);
                }else {
                    doStartVideoCareDrm(false, 0, 0);
                }
                mVideoView.setVisibility(View.VISIBLE);
            } else {
                MtkLog.e(TAG, "Cannot play the next video! " + item);
            }
            mActivityContext.closeOptionsMenu();
        }

        @Override
        public void onRestoreInstanceState(Bundle icicle) {
            mIsLoop = icicle.getBoolean(KEY_VIDEO_IS_LOOP, false);
            if (mIsLoop) {
                mController.setCanReplay(true);
            } // else  will get can replay from intent.
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putBoolean(KEY_VIDEO_IS_LOOP, mIsLoop);
        }
        
        private void pauseIfNeed() {
            mLastCanPaused = canStop() && mVideoView.canPause();
            if (mLastCanPaused) {
                MtkLog.v(TAG, "pauseIfNeed mTState= " + mTState);
                mLastPlaying = (mTState == TState.PLAYING);
                mOverlayExt.clearBuffering();
                mPauseBuffering = true;
                ///M: Reset flag , we don't want use the last result.
                mPlayPauseProcessExt.mPlayVideoWhenPaused = false;
                if(mVideoView.isCurrentPlaying()&&onIsRTSP()){
                   mPlayPauseProcessExt.mPauseSuccess = false;
                   mPlayPauseProcessExt.mNeedCheckPauseSuccess = true;
                }
                pauseVideo();
                
            }
            if (LOG) {
                MtkLog.v(TAG, "pauseIfNeed() mLastPlaying=" + mLastPlaying + ", mLastCanPaused=" + mLastCanPaused
                    + ", mPauseBuffering=" + mPauseBuffering + "mTState=" + mTState);
            }
        }
        
        private void resumeIfNeed() {
            if (mLastCanPaused) {
                if (mLastPlaying) {
                    mPauseBuffering = false;
                    ///M: restore mTsate firstly. Because playvideo() maybe happened in onInfo().
                    mTState = TState.PLAYING;
                    mPlayPauseProcessExt.CheckPauseSuccess();
                }
            }
            if (LOG) {
                MtkLog.v(TAG, "resumeIfNeed() mLastPlaying=" + mLastPlaying + ", mLastCanPaused=" + mLastCanPaused
                    + ", mPauseBuffering=" + mPauseBuffering);
            }
        }

        
        
        public boolean pauseBuffering() {
            return mPauseBuffering;
        }
        
        public void setVideoInfo(Metadata data) {
            if (data.has(Metadata.TITLE)) {
                mTitle = data.getString(Metadata.TITLE);
            }
            if (data.has(Metadata.AUTHOR)) {
                mAuthor = data.getString(Metadata.AUTHOR);
            }
            if (data.has(Metadata.COPYRIGHT)) {
                mCopyRight = data.getString(Metadata.COPYRIGHT);
            }
        }
        public boolean setParameter(int key, int value) {
            return mVideoView.setParameter(key, value);
        }

         /**
          *hide controller and action bar for showing stereo 3d ui.
          */
        public void hideController() {
            //hide controller overlay.
            mController.hide();
            //set hide controller flag for ignoring touch event. 
            mHideController = true;
            //set hide controller flag for ignoring touch event. 
            mController.setInterceptFlag(mHideController);
            //hide action bar.
            mVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

       public void updateUI() {
            updateDisplayElement();
            mHideController = false;
            mController.setInterceptFlag(mHideController);
            if (mVideoView.isPlaying()) {
                if (mController != null) {
                    mController.hide();
                    //hide navigation bar
                    showSystemUi(false, false);
                }
            } else {
                if (mController != null) {
                    mController.show();
                }
            }
        }
        ///M: for slow motion
        public void refreshSlowMotionSpeed(int speed) {
            Log.i(TAG,"refreshSlowMotionSpeed speed " + speed);
            mSlowMotionSpeed = speed;
            mVideoView.setSlowMotionSpeed(mSlowMotionSpeed);
            
            ContentValues values = new ContentValues(1);
            values.put(Video.Media.SLOW_MOTION_SPEED,mSlowMotionSpeed);
            if(mMovieItem.getUri().toString().toLowerCase(Locale.ENGLISH).contains("file:///")) {
                Uri uri = mMovieItem.getUri();
                Cursor cursor = null;
                String data = Uri.decode(uri.toString());
                data = data.replaceAll("'", "''");
                int id = 0;
                final String where = "_data LIKE '%" + data.replaceFirst("file:///", "") + "'";
                
                cursor = mActivityContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Video.Media._ID}, where, null, null);
            
                if (cursor != null && cursor.moveToFirst()) {
                      id = cursor.getInt(0);
                }
                Log.i(TAG,"refreshSlowMotionSpeed id " + id);
                uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "" + id);
                mActivityContext.getContentResolver().update(uri, values, null, null);
                
            } else {
                mActivityContext.getContentResolver().update(mMovieItem.getUri(), values, null, null);
            }
            
        }
    };

    /**
     * Play/pause asynchronous processing.
     */
    private class PlayPauseProcessExt implements MediaPlayer.OnInfoListener {
        public boolean mPauseSuccess = false;
        public boolean mNeedCheckPauseSuccess = false;
        public boolean mPlayVideoWhenPaused = false;

        /**
         * Check Pause is success or not. if success, it will start play, or
         * will start play when success is come in onInfo().
         */
        private void CheckPauseSuccess() {
            MtkLog.v(TAG, "CheckPauseSuccess() mNeedCheckPauseSuccess=" + mNeedCheckPauseSuccess
                    + ", mPauseSuccess=" + mPauseSuccess);
            if (mNeedCheckPauseSuccess == true) {
                if (mPauseSuccess == true) {
                    playVideo();
                    mPauseSuccess = false;
                    mNeedCheckPauseSuccess = false;
                } else {
                    mPlayVideoWhenPaused = true;
                    mController.setViewEnabled(false);
                }
            } else {
                playVideo();
            }
        }

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_PAUSE_COMPLETED
                    || what == MediaPlayer.MEDIA_INFO_PLAY_COMPLETED) {
                MtkLog.v(TAG, "onInfo is PAUSE PLAY COMPLETED");
                if (extra == MediaPlayer.PAUSE_PLAY_SUCCEED) {
                    if (what == MediaPlayer.MEDIA_INFO_PAUSE_COMPLETED) {
                        handlePauseComplete();
                    }
                } else {
                    if (extra != ERROR_INVALID_OPERATION && extra != ERROR_ALREADY_EXISTS) {
                        showNetWorkErrorDialog();
                    }
                }
                if (mVideoView.canPause()) {
                    mController.setViewEnabled(true);
                    updateRewindAndForwardUI();
                }
                return true;
            }
            return false;
        }

        /**
         * Judge if need play video in onInfo.
         */
        private void handlePauseComplete() {
            Log.v(TAG, "handlePauseComplete() mNeedCheckPauseSuccess=" + mNeedCheckPauseSuccess
                    + ", mPlayVideoWhenPaused=" + mPlayVideoWhenPaused);
            if (mNeedCheckPauseSuccess == true) {
                mPauseSuccess = true;
            }
            if (mPlayVideoWhenPaused == true) {
                mVideoView.start();
                mController.showPlaying();
                mPauseSuccess = false;
                mNeedCheckPauseSuccess = false;
                mPlayVideoWhenPaused = false;
            }
        }

        /**
         * Show dialog to user if play/pause is failed.Notify that only socket
         * error(except invalid operation and already exists error) will cause
         * network connection failed and should show the dialog.
         */
        private void showNetWorkErrorDialog() {
            final String errorDialogTag = "ERROR_DIALOG_TAG";
            FragmentManager fragmentManager = ((Activity) mActivityContext).getFragmentManager();
            DialogFragment fragment =
                    ErrorDialogFragment
                            .newInstance(R.string.VideoView_error_text_connection_failed);
            fragment.show(fragmentManager, errorDialogTag);
            fragmentManager.executePendingTransactions();
        }
    }
    
    private class ServerTimeoutExtension implements Restorable, MediaPlayer.OnErrorListener {
        //for cmcc server timeout case 
        //please remember to clear this value when changed video.
        private int mServerTimeout = -1;
        private long mLastDisconnectTime;
        private boolean mIsShowDialog = false;
        private AlertDialog mServerTimeoutDialog;
        
        //check whether disconnect from server timeout or not.
        //if timeout, return false. otherwise, return true.
        private boolean passDisconnectCheck() {
            if (ExtensionHelper.getMovieStrategy(mActivityContext).shouldEnableServerTimeout()
                    && MovieUtils.isRtspOrSdp(mVideoType)) {
                //record the time disconnect from server
                long now = System.currentTimeMillis();
                if (LOG) {
                    MtkLog.v(TAG, "passDisconnectCheck() now=" + now + ", mLastDisconnectTime=" + mLastDisconnectTime
                            + ", mServerTimeout=" + mServerTimeout);
                }
                if (mServerTimeout > 0 && (now - mLastDisconnectTime) > mServerTimeout) {
                    //disconnect time more than server timeout, notify user
                    notifyServerTimeout();
                    return false;
                }
            }
            return true;
        }
        
        private void recordDisconnectTime() {
            if (ExtensionHelper.getMovieStrategy(mActivityContext).shouldEnableServerTimeout()
                    && MovieUtils.isRtspOrSdp(mVideoType)) {
                //record the time disconnect from server
                mLastDisconnectTime = System.currentTimeMillis();
            }
            if (LOG) {
                MtkLog.v(TAG, "recordDisconnectTime() mLastDisconnectTime=" + mLastDisconnectTime);
            }
        }
        
        private void clearServerInfo() {
            mServerTimeout = -1;
        }
        
        private void notifyServerTimeout() {
            if (mServerTimeoutDialog == null) {
                //for updating last position and duration.
                if (mVideoCanSeek || mVideoView.canSeekForward()) { 
                    mVideoView.seekTo(mVideoPosition);
                }
                mVideoView.setDuration(mVideoLastDuration);
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivityContext);
                mServerTimeoutDialog = builder.setTitle(R.string.server_timeout_title)
                    .setMessage(R.string.server_timeout_message)
                    .setNegativeButton(android.R.string.cancel, new OnClickListener() {
    
                        public void onClick(DialogInterface dialog, int which) {
                            if (LOG) {
                                MtkLog.v(TAG, "NegativeButton.onClick() mIsShowDialog=" + mIsShowDialog);
                            }
                            mController.showEnded();
                            onCompletion();
                        }
                        
                    })
                    .setPositiveButton(R.string.resume_playing_resume, new OnClickListener() {
        
                        public void onClick(DialogInterface dialog, int which) {
                            if (LOG) {
                                MtkLog.v(TAG, "PositiveButton.onClick() mIsShowDialog=" + mIsShowDialog);
                            }
                            doStartVideoCareDrm(true, mVideoPosition, mVideoLastDuration);
                            // / M: Add mProgressChecker
                            // to the message queue so that the
                            // progress bar will update every
                            // seconds.
                            mHandler.post(mProgressChecker);
                        }
                        
                    })
                    .create();
                mServerTimeoutDialog.setOnDismissListener(new OnDismissListener() {
                        
                        public void onDismiss(DialogInterface dialog) {
                            if (LOG) {
                                MtkLog.v(TAG, "mServerTimeoutDialog.onDismiss()");
                            }
                            mIsShowDialog = false;
                        }
                        
                    });
                mServerTimeoutDialog.setOnShowListener(new OnShowListener() {
    
                        public void onShow(DialogInterface dialog) {
                            if (LOG) {
                                MtkLog.v(TAG, "mServerTimeoutDialog.onShow()");
                            }
                            mIsShowDialog = true;
                        }
                        
                    });
            }
            mServerTimeoutDialog.show();
        }
        
        private void clearTimeoutDialog() {
            if (mServerTimeoutDialog != null && mServerTimeoutDialog.isShowing()) {
                mServerTimeoutDialog.dismiss();
            }
            mServerTimeoutDialog = null;
        }

        @Override
        public void onRestoreInstanceState(Bundle icicle) {
            mLastDisconnectTime = icicle.getLong(KEY_VIDEO_LAST_DISCONNECT_TIME);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putLong(KEY_VIDEO_LAST_DISCONNECT_TIME, mLastDisconnectTime);
        }

        public boolean handleOnResume() {
            if (mIsShowDialog && !MovieUtils.isLiveStreaming(mVideoType)) {
                //wait for user's operation
                return true;
            }
            if (!passDisconnectCheck()) {
                return true;
            }
            return false;
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            //if we are showing a dialog, cancel the error dialog
            if (mIsShowDialog) {
                return true;
            }
            return false;
        }
        
        public void setVideoInfo(Metadata data) {
            if (data.has(Metadata.SERVER_TIMEOUT)) {
                mServerTimeout = data.getInt(Metadata.SERVER_TIMEOUT);
                if (LOG) {
                    MtkLog.v(TAG, "get server timeout from metadata. mServerTimeout=" + mServerTimeout);
                }
            }
        }
    }
    
    private final Runnable mRemoveBackground = new Runnable() {
        @Override
        public void run() {
            if (LOG) {
                MtkLog.v(TAG, "mRemoveBackground.run()");
            }
            if(mWfdPowerSaving != null) { 
                /// M: In WFD Extension mode, UI and Background will always show.
                if(!mWfdPowerSaving.needShowController()) {
                    mRootView.setBackground(null);
                }
            } else {
                mRootView.setBackground(null);
            }
        }
    };
    
    /// M: same as launch case to delay transparent. @{
    private Runnable mDelayVideoRunnable = new Runnable() {
        @Override
        public void run() {
            if (LOG) {
                MtkLog.v(TAG, "mDelayVideoRunnable.run()");
            }
            mVideoView.setVisibility(View.VISIBLE);
        }
    };
    /// @}
    
    /// M: when show resming dialog, suspend->wakeup, will play video. @{
    private boolean mIsShowResumingDialog;
    /// @}

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        setProgress();
    }
    
    /// M: for update MediaPlayer layout in stereo feature
    private boolean isMoviePartialVisible() {
        return mActivityContext != null && mActivityContext.isPartialVisible();
    }

    private void updateDisplayElement() {
        // this function is added for stereo video playback feature that
        // update info of how to display time bar
        if (mController != null) {
            mController.displayTimeBar(!isMoviePartialVisible());
        }
    }
    /// @}

    // / M:Pause the player when meet a transient loss focus(eg:An incoming
    // phone call)
    // @{
    private OnAudioFocusChangeListener mAudioFocusChangeListener =
            new OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    MtkLog.v(TAG, "onAudioFocusChange() focusChange is " + focusChange);
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        mVideoView.pause();
                    }
                }
            };// /@}
}

class Bookmarker {
    private static final String TAG = "Gallery2/Bookmarker";

    private static final String BOOKMARK_CACHE_FILE = "bookmark";
    private static final int BOOKMARK_CACHE_MAX_ENTRIES = 100;
    private static final int BOOKMARK_CACHE_MAX_BYTES = 10 * 1024;
    private static final int BOOKMARK_CACHE_VERSION = 1;

    private static final int HALF_MINUTE = 30 * 1000;
    private static final int TWO_MINUTES = 4 * HALF_MINUTE;

    private final Context mContext;

    public Bookmarker(Context context) {
        mContext = context;
    }

    public void setBookmark(Uri uri, int bookmark, int duration) {
        if (LOG) {
            MtkLog.v(TAG, "setBookmark(" + uri + ", " + bookmark + ", " + duration + ")");
        }
        try {
            //do not record or override bookmark if duration is not valid.
            if (duration <= 0) {
                return;
            }
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri.toString());
            dos.writeInt(bookmark);
            dos.writeInt(Math.abs(duration));
            dos.flush();
            cache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
            Log.w(TAG, "setBookmark failed", t);
        }
    }
    /// M: FOR MTK_AUDIO_CHANGE_SUPPORT & MTK_SUBTITLE_SUPPORT
    ///@{
    public void setBookmark(Uri uri, int bookmark, int duration, 
    						int audioIdx, int subtitleIdx, int audioDlgListIdx, int  subtitleDlgListIdx) {
        if (LOG) {
            MtkLog.v(TAG, "setBookmark(" + uri + ", " + bookmark + ", " + duration + ", "  
            		+ audioIdx + ", " + subtitleIdx + ", " + audioDlgListIdx + ", "+ subtitleDlgListIdx+")");
        }
        try {
            //do not record or override bookmark if duration is not valid.
            if (duration <= 0) {
                return;
            }
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri.toString());
            dos.writeInt(bookmark);
            dos.writeInt(Math.abs(duration));
            dos.writeInt(audioIdx);
            dos.writeInt(subtitleIdx);
            dos.writeInt(audioDlgListIdx);
            dos.writeInt(subtitleDlgListIdx);
            dos.flush();
            cache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
            Log.w(TAG, "setBookmark failed", t);
        }
    }
    ///@}

    public BookmarkerInfo getBookmark(Uri uri) {
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);

            byte[] data = cache.lookup(uri.hashCode());
            if (data == null) {
                if (LOG) {
                    MtkLog.v(TAG, "getBookmark(" + uri + ") data=null. uri.hashCode()=" + uri.hashCode());
                }
                return null;
            }

            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));

            String uriString = DataInputStream.readUTF(dis);
            int bookmark = dis.readInt();
            int duration = dis.readInt();
            if (LOG) {
                MtkLog.v(TAG, "getBookmark(" + uri + ") uriString=" + uriString + ", bookmark=" + bookmark
                        + ", duration=" + duration);
            }
            if (!uriString.equals(uri.toString())) {
                return null;
            }

            if ((bookmark < HALF_MINUTE) || (duration < TWO_MINUTES)
                    || (bookmark > (duration - HALF_MINUTE))) {
                return null;
            }
            if(MediatekFeature.MTK_AUDIO_CHANGE_SUPPORT || MediatekFeature.MTK_SUBTITLE_SUPPORT){
            	int audioIdx = dis.readInt();
            	int subtitleIdx = dis.readInt();
            	int audioDlgListIdx = dis.readInt();
            	int subtitleDlgListIdx = dis.readInt();
				return new BookmarkerInfo(bookmark, duration, audioIdx, subtitleIdx, audioDlgListIdx, subtitleDlgListIdx);
            }            	
            else {
            	return new BookmarkerInfo(bookmark, duration);
            }
            
        } catch (Throwable t) {
            Log.w(TAG, "getBookmark failed", t);
        }
        return null;
    }
    
    private static final boolean LOG = true;
}

class BookmarkerInfo {
    public final int mBookmark;
    public final int mDuration;
	
    /// M: FOR MTK_AUDIO_CHANGE_SUPPORT & MTK_SUBTITLE_SUPPORT
    ///@{
    public int mSelectAudioIndexBmk; 
    public int mSelcetAudioTrackIdxBmk; 
    public int mSelectSubTitleIndexBmk;
    public int mSelectSubTitleTrackIdxBmk;
    ///@}
    public BookmarkerInfo(int bookmark, int duration) {
        this.mBookmark = bookmark;
        this.mDuration = duration;
    }
    /// M: FOR MTK_AUDIO_CHANGE_SUPPORT & MTK_SUBTITLE_SUPPORT
    ///@{
    public BookmarkerInfo(int bookmark, int duration, 
    		int audioIdx, int subtitleIdx, int audioDlgListIdx, int  subtitleDlgListIdx) {
        this.mBookmark = bookmark;
        this.mDuration = duration;
        this.mSelcetAudioTrackIdxBmk = audioIdx;
        this.mSelectAudioIndexBmk = audioDlgListIdx;
        this.mSelectSubTitleTrackIdxBmk = subtitleIdx;
        this.mSelectSubTitleIndexBmk = subtitleDlgListIdx;
    }
    ///@}
    
    @Override
    public String toString() {
        return new StringBuilder()
        .append("BookmarkInfo(bookmark=")
        .append(mBookmark)
        .append(", duration=")
        .append(mDuration)
        .append(")")
        .toString();
    }
}
