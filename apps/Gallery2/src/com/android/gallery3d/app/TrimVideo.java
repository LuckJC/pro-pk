/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.StatFs;

import com.android.gallery3d.R;
import com.mediatek.gallery3d.ext.MovieUtils;
import com.android.gallery3d.util.SaveVideoFileInfo;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.video.MTKVideoView;
import com.android.gallery3d.util.SaveVideoFileUtils;
import com.android.gallery3d.common.ApiHelper;
import com.mediatek.common.featureoption.FeatureOption;

import java.io.File;
import java.io.IOException;

public class TrimVideo extends Activity implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        ControllerOverlay.Listener {

    private MTKVideoView mVideoView;

    private TextView mSaveVideoTextView;
    private TrimControllerOverlay mController;
    private Context mContext;
    private Uri mUri;
    private final Handler mHandler = new Handler();
    public static final String TRIM_ACTION = "com.android.camera.action.TRIM";

    ///M: change to static
    //public ProgressDialog mProgress;

    private int mTrimStartTime = 0;
    private int mTrimEndTime = 0;
    private int mVideoPosition = 0;
    public static final String KEY_TRIM_START = "trim_start";
    public static final String KEY_TRIM_END = "trim_end";
    public static final String KEY_VIDEO_POSITION = "video_pos";
    
    ///M: add for get duration error@{
    private static final long INVALID_DURATION = -1l;
    private static final long FILE_ERROR = -2l;
    ///@}
    
    ///M: change to static
    //private boolean mHasPaused = false;

    private String mSrcVideoPath = null;
    private String mSaveFileName = null;
    private static final String TIME_STAMP_NAME = "'TRIM'_yyyyMMdd_HHmmss";
    private File mSrcFile = null;
    ///M:change to static
    //private File mDstFile = null;
    private File mSaveDirectory = null;
    private SaveVideoFileInfo mDstFileInfo = null;

    // For showing the result.
    ///M:change to static
    //private String saveFolderName = null;
    private static final String TAG = "Gallery2/TrimVideo";
    
    // If the time bar is being dragged.
    private boolean mDragging;
    
    /// M: add for show dialog @{
    private final Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            showProgressDialog();
        }
    };
    /// @}
    /// M: add for show toast @{
    private final Runnable mShowToastRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.can_not_trim),
                    Toast.LENGTH_SHORT)
                    .show();
            /// M: modify mSaveVideoTextView to can click @{
            setSaveClickable(true);
            /// @}
        }
    };
    /// @}
    ///M: if mProgress is null, TrimVideo has stopped,
    /// set mPlayTrimVideo as true. it will play trim video again
    /// after resume TrimVideo. @{
    private static boolean mPlayTrimVideo = false;
    private static boolean mIsSaving = false;
    public static ProgressDialog mProgress;
    private static File mDstFile = null;
    private static String saveFolderName = null;
    private static boolean mHasPaused = false;
    // / M: True if mProgressChecker is called
    private boolean mIsInProgressCheck = false;

    private final Runnable mStartVideoRunnable = new Runnable() {
        @Override
        public void run() {
            // TODO: change trimming into a service to avoid
            // this progressDialog and add notification properly.
            MtkLog.v(TAG, "StartVideoRunnable,HasPaused:" + mHasPaused);
            if (!mHasPaused) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.save_into, mDstFileInfo.mFolderName),
                        Toast.LENGTH_SHORT)
                        .show();
                if(mProgress != null) {
                    mProgress.dismiss();
                    mProgress = null;
                }
                // Show the result only when the activity not stopped.
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndTypeAndNormalize(Uri.fromFile(mDstFileInfo.mFile), "video/*");
                intent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
                startActivity(intent);
                mPlayTrimVideo = false;
                mIsSaving = false;
                mDstFile = null;
                saveFolderName = null;
                finish();
            } else {
                mPlayTrimVideo = true; 
            }
        }
    };
    ///}@
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        super.onCreate(savedInstanceState);
        MtkLog.v(TAG , "onCreate()");
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        ActionBar actionBar = getActionBar();
        int displayOptions = ActionBar.DISPLAY_SHOW_HOME;
        actionBar.setDisplayOptions(0, displayOptions);
        displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM;
        actionBar.setDisplayOptions(displayOptions, displayOptions);
        actionBar.setCustomView(R.layout.trim_menu);
        //M: mIsSaving is static, 
        //here set flase avoid sometimes run mStartVideoRunnable JE happen.
        mIsSaving = false;

        /// M: modify mSaveVideoTextView to private
        mSaveVideoTextView = (TextView) findViewById(R.id.start_trim);
        mSaveVideoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                /// M: modify mSaveVideoTextView to can not click @{
                setSaveClickable(false);
                mIsSaving = true;
                /// @}
                MtkLog.v(TAG , "mSaveVideoTextView onclick");
                trimVideo();
            }
        });
	//add by matt
        mSaveVideoTextView.setEnabled(false);

        Intent intent = getIntent();
        mUri = intent.getData();
        mSrcVideoPath = intent.getStringExtra(PhotoPage.KEY_MEDIA_ITEM_PATH);
        setContentView(R.layout.trim_view);
        View rootView = findViewById(R.id.trim_view_root);

        mVideoView = (MTKVideoView) rootView.findViewById(R.id.surface_view);

        mController = new TrimControllerOverlay(mContext);
        ((ViewGroup) rootView).addView(mController.getView());
        mController.setListener(this);
        mController.setCanReplay(true);

        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setVideoURI(mUri, null);

        playVideo();
    }

    @Override
    public void onResume() {
        super.onResume();
        MtkLog.v(TAG , "onResume()");
        mDragging = false;//clear drag info
        if (mHasPaused) {
            // / M: Modified to avoid video location
            // incorrect limitation when suspend and
            // wake up in landscape mode@{
            mVideoView.setVisibility(View.VISIBLE);
            // /@
            mVideoView.seekTo(mVideoPosition);
            mVideoView.resume();
            mHasPaused = false;
        }
        mHandler.post(mProgressChecker);

        ///M: if mPlayTrimVideo is true, it need show toast 
        /// and play trim video @{
        if(mIsSaving) {
            if(mProgress == null) {
                showProgressDialog();
            }
            setSaveClickable(false);
            if(mPlayTrimVideo) {
                mHandler.post(mStartVideoRunnable);
            }
        }
        /// }@
    }

    @Override
    public void onPause() {
        MtkLog.v(TAG , "onPause()");
        mHasPaused = true;
        mHandler.removeCallbacksAndMessages(null);
        mVideoPosition = mVideoView.getCurrentPosition();
        mVideoView.suspend();
        // / M: Modified to avoid video location
        // incorrect limitation when suspend and
        // wake up in landscape mode@{
        if (!isFinishing()) {
            mVideoView.setVisibility(View.INVISIBLE);
        }
        // /@
        super.onPause();
    }

    @Override
    public void onStop() {
        MtkLog.v(TAG , "onStop()");
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        MtkLog.v(TAG , "onDestroy()");
        mVideoView.stopPlayback();
        super.onDestroy();
    }

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mIsInProgressCheck = true;
            mHandler.postDelayed(mProgressChecker, 200 - (pos % 200));
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        MtkLog.v(TAG , "onSaveInstanceState()");
        savedInstanceState.putInt(KEY_TRIM_START, mTrimStartTime);
        savedInstanceState.putInt(KEY_TRIM_END, mTrimEndTime);
        savedInstanceState.putInt(KEY_VIDEO_POSITION, mVideoPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        MtkLog.v(TAG , "onRestoreInstanceState()");
        mTrimStartTime = savedInstanceState.getInt(KEY_TRIM_START, 0);
        mTrimEndTime = savedInstanceState.getInt(KEY_TRIM_END, 0);
        mVideoPosition = savedInstanceState.getInt(KEY_VIDEO_POSITION, 0);
        MtkLog.v(TAG, "mTrimStartTime is " + mTrimStartTime
                + ", mTrimEndTime is " + mTrimEndTime + ", mVideoPosition is "
                + mVideoPosition);
    }

    // This updates the time bar display (if necessary). It is called by
    // mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        MtkLog.v(TAG , "setProgress()");
        mVideoPosition = mVideoView.getCurrentPosition();
        // If the video position is smaller than the starting point of trimming,
        // correct it.
        // / M:Under the circumstances that mProgressChecker is called,do not
        // correct the position.
        if (!mIsInProgressCheck && mVideoPosition < mTrimStartTime) {
            MtkLog.v(TAG, "setProgress() mVideoPosition < mTrimStartTime");
            mVideoView.seekTo(mTrimStartTime);
            mVideoPosition = mTrimStartTime;
        }
        // If the position is bigger than the end point of trimming, show the
        // replay button and pause.
        if (mVideoPosition >= mTrimEndTime && mTrimEndTime > 0) {
            if (mVideoPosition > mTrimEndTime) {
                mVideoView.seekTo(mTrimEndTime);
                mVideoPosition = mTrimEndTime;
            }
            mController.showEnded();
            mVideoView.pause();
        }

        int duration = mVideoView.getDuration();
        if (duration > 0 && mTrimEndTime == 0) {
            mTrimEndTime = duration;
        }
        mController.setTimes(mVideoPosition, duration, mTrimStartTime, mTrimEndTime);
        // Enable save if there's modifications
        mSaveVideoTextView.setEnabled(isModified());
        return mVideoPosition;
    }

    private void playVideo() {
        MtkLog.v(TAG , "playVideo()");
        mVideoView.start();
        mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        MtkLog.v(TAG , "pauseVideo()");
        mVideoView.pause();
        mController.showPaused();
    }

    private boolean isModified() {
        int delta = mTrimEndTime - mTrimStartTime;

        // Considering that we only trim at sync frame, we don't want to trim
        // when the time interval is too short or too close to the origin.
        if (delta < 100 || Math.abs(mVideoView.getDuration() - delta) < 100) {
            return false;
        } else {
            return true;
        }
    }
    ///M: Set mSaveVideoTextView to can click or not.@{
    public void setSaveClickable(boolean enabled) {
        mSaveVideoTextView.setClickable(enabled);
        mSaveVideoTextView.setEnabled(enabled);
    }
    ///@}

    ///M: for rename file from filemanager case, get absolute path from uri.@{
    private String getVideoPath(final Context context, Uri uri) {
            String videoPath = null;
            Cursor cursor = null;
            MtkLog.v(TAG, "getVideoPath(" + uri + ")");
            try {
                //query from "content://....."
                cursor = context.getContentResolver().query(uri,
                        new String[] { MediaStore.Video.Media.DATA }, null, null,
                        null);
                //query from "file:///......"
                if (cursor == null) {
                    String data = Uri.decode(uri.toString());
                    data = data.replaceAll("'", "''");
                    final String where = "_data LIKE '%" + data.replaceFirst("file:///", "") + "'";
                    cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Video.Media.DATA}, where, null, null);
                }
                if (cursor != null && cursor.moveToFirst()) {
                    videoPath = cursor.getString(0);
                }
            } catch (final SQLiteException ex) {
                ex.printStackTrace();
            } catch (IllegalArgumentException e) {
                // if this exception happen, return false.
                MtkLog.v(TAG, "ContentResolver query IllegalArgumentException");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return videoPath;
        }

    ///@}
    
    private void trimVideo() {
        ///M: for rename file from filemanager case, get absolute path from uri
        mSrcVideoPath = getVideoPath(this, mUri);
        
        final File mSrcFile = new File(mSrcVideoPath);
        mDstFileInfo = SaveVideoFileUtils.getDstMp4FileInfo(TIME_STAMP_NAME,
                getContentResolver(), mUri, mSrcFile.getParentFile(), true, getString(R.string.folder_download));

        /// M: show toast when there is not enough sapce for save trim video.
        if (!isSpaceEnough(mSrcFile)) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.storage_not_enough),
                    Toast.LENGTH_SHORT)
                    .show();
            /// M: modify mSaveVideoTextView to can click @{
            setSaveClickable(true);
            mIsSaving = false;
            return;
        }
        /// @}

        ///M: after check this video can trim,show dialog
        if (ApiHelper.HAS_MEDIA_MUXER) {
            showProgressDialog();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean isTrimSuccessful = VideoUtils.startTrim(mSrcFile, mDstFileInfo.mFile, mTrimStartTime, mTrimEndTime, TrimVideo.this, mProgress);
                    if(!isTrimSuccessful){
                        ///M: show can not trim to user toast
                        showToast();
                        ///M:
                        mIsSaving = false;
                        if (mDstFileInfo.mFile.exists()) {
                            mDstFileInfo.mFile.delete();
                        }
                        return;
                    }
                    // Update the database for adding a new video file.
                    SaveVideoFileUtils.insertContent(mDstFileInfo,
                            getContentResolver(), mUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // After trimming is done, trigger the UI changed.
                MtkLog.v(TAG, "save trim video succeed!");
                mHandler.post(mStartVideoRunnable);
            }
        }).start();
    }

    private void showProgressDialog() {
        // create a background thread to trim the video.
        // and show the progress.
        mProgress = new ProgressDialog(this);
        mProgress.setTitle(getString(R.string.trimming));
        mProgress.setMessage(getString(R.string.please_wait));
        // TODO: make this cancelable.
        mProgress.setCancelable(false);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
    }
    
    public void showDialogCommand(){
        mHandler.removeCallbacks(mShowDialogRunnable);
        mHandler.post(mShowDialogRunnable);
    }
    
    /**
     * Show toast when the video can't be trimmed
     */
    public void showToast() {
        mHandler.removeCallbacks(mShowToastRunnable);
        mHandler.post(mShowToastRunnable);
    }
    
    @Override
    public void onPlayPause() {
        if (mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        MtkLog.v(TAG , "onSeekStart() mDragging is " + mDragging);
        mDragging = true;
        pauseVideo();
    }

    @Override
    public void onSeekMove(int time) {
        MtkLog.v(TAG , "onSeekMove() seekto time is (" + time + ") mDragging is " + mDragging);
        if (!mDragging) {
            mVideoView.seekTo(time);
        }
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        MtkLog.v(TAG, "onSeekEnd() seekto time is " + time + ", start is "
                + start + ", end is " + end + " mDragging is " + mDragging);
        mDragging = false;
        mVideoView.seekTo(time);
        mTrimStartTime = start;
        mTrimEndTime = end;
        mIsInProgressCheck = false;
        setProgress();
        if (mVideoPosition == mTrimStartTime) {
            mController.showPaused();
        }
    }

    @Override
    public void onShown() {
    }

    @Override
    public void onHidden() {
    }

    @Override
    public boolean onIsRTSP() {
        return false;
    }
    
    @Override
    public void onReplay() {
        MtkLog.v(TAG , "onReplay()");
        mVideoView.seekTo(mTrimStartTime);
        playVideo();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        MtkLog.v(TAG , "onCompletion()");
        mController.showEnded();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
    @Override
    public boolean wfdNeedShowController() {
        return false;
    }
    /// M: need query storage left when trim,
    ///to avoid NE when native check file header@{  
    /**
    * get available space which storage source video is in.
    * @return the available sapce size, -1 means max storage size.
    */
    private long getAvailableSpace() {
        try {
            //Here just use one directory to stat fs.
            StatFs stat = new StatFs(mSrcVideoPath);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            MtkLog.v(TAG, "Fail to access external storage", e);
        }
        return -1;
    }
    
    /**
    * calculate the space for video trimed is enough or not
    * lowStorageThreshold is reserve space. LCA projec is 9M, the others is 48M.
    */
    private boolean isSpaceEnough(File Srcfile) {
        long spaceNeed;
        long lowStorageThreshold;
        if(FeatureOption.MTK_LCA_RAM_OPTIMIZE) {
            lowStorageThreshold= 9 * 1024 * 1024;
        }else {
            lowStorageThreshold= 48 * 1024 * 1024;
        }
        spaceNeed = Srcfile.length() * (mTrimEndTime - mTrimStartTime) / mVideoView.getCurrentPosition() + lowStorageThreshold;
        if (getAvailableSpace() < spaceNeed) {
            MtkLog.v(TAG, "space is not enough for save trim video");
            return false;
        }else {
            return true;
        }
    }
    /// @}
}
