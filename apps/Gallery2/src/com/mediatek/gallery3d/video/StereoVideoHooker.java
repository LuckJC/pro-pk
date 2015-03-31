package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.SubMenu;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.android.gallery3d.R;
import com.mediatek.gallery3d.ext.IMovieItem;
import com.mediatek.gallery3d.ext.IMoviePlayer;
import com.mediatek.gallery3d.ext.MtkLog;

import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.ui.ConvergenceBarManager;
import com.mediatek.gallery3d.ui.StereoVideoLayout;

public class StereoVideoHooker extends MovieHooker {
    private static final String TAG = "Gallery2/VideoPlayer/StereoVideoHooker";
    private static final boolean LOG = true;

    private static final int UNKNOWN = -1;
    public static final int STEREO_TYPE_2D = MediaStore.Video.Media.STEREO_TYPE_2D;
    public static final int STEREO_TYPE_3D = MediaStore.Video.Media.STEREO_TYPE_SIDE_BY_SIDE;
    private static final String EXTRA_STEREO_TYPE = "mediatek.intent.extra.STEREO_TYPE";
    private static final String COLUMN_STEREO_TYPE = MediaStore.Video.Media.STEREO_TYPE;
    private static final String COLUMN_CONVERGENCE = MediaStore.Video.Media.CONVERGENCE;
    private static final String COLUMN_ID = MediaStore.Video.VideoColumns._ID;
    
    // projection when query database
    private static final String [] PROJECTION =
                new String[] { COLUMN_ID,
                               COLUMN_STEREO_TYPE,
                               COLUMN_CONVERGENCE };
    private static int INDEX_ID = 0;
    private static int INDEX_STEREO_TYPE = 1;
    private static int INDEX_CONVERGENCE = 2;

    private int mCurrentStereoType;
    private boolean mCurrentStereoMode;

    private static final int MENU_STEREO_VIDEO = 1;
    private static final int MENU_MC = 2;
    private static final int MENU_AC = 3;
    private static final int MENU_STEREO_LAYOUTS = 4;

    private MenuItem mMenuStereoVideo;
    private MenuItem mMenuAC;
    private MenuItem mMenuMC;
    private MenuItem mStereoLayouts;

    private Context mContext;
    private SurfaceView mVideoSurface;
    private ViewGroup mRootView;
    private IMoviePlayer mPlayer;
    
    @Override
    public void init(Activity context, Intent intent) {
        mContext = (Context)context;
    }
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int stereoType = null != getIntent() ?
            getIntent().getIntExtra(EXTRA_STEREO_TYPE, UNKNOWN) : UNKNOWN;
        if (stereoType == UNKNOWN) {
            enhanceStereoActionBar(getMovieItem(), stereoType);
        }
        if (stereoType != UNKNOWN) {
            getMovieItem().setStereoType(stereoType);
        }
        if (getMovieItem().getStereoType() == STEREO_TYPE_3D) {
            mVideoSurface.setFlagsEx(WindowManager.LayoutParams.FLAG_EX_S3D_3D, 
                WindowManager.LayoutParams.FLAG_EX_S3D_3D);
        }else if (getMovieItem().getStereoType() == STEREO_TYPE_2D) {
            mVideoSurface.setFlagsEx(WindowManager.LayoutParams.FLAG_EX_S3D_2D, 
                WindowManager.LayoutParams.FLAG_EX_S3D_2D);
        }
        initialStereoVideoIcon(getMovieItem().getStereoType());
        initStereoDepthTuningBar();
        initStereoVideoLayout();
        setStereoLayout(getMovieItem().getStereoType());
    }
    
    @Override
    public void onPause() {
        if (mConvBarManager != null) {
            mConvBarManager.pause();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenuStereoVideo = menu.add(0, getMenuActivityId(MENU_STEREO_VIDEO), 0, R.string.stereo3d_mode_switchto_2d);
        mMenuStereoVideo.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        initialStereoVideoIcon(getMovieItem().getStereoType());
        // M: added for video convergence feature
        mMenuMC = menu.add(0, getMenuActivityId(MENU_MC), 0, R.string.stereo3d_convergence_menu);
        mMenuAC = menu.add(0, getMenuActivityId(MENU_AC), 0, R.string.stereo3d_switch_auto_convergence);
        mMenuAC.setCheckable(true);
        mStereoLayouts = menu.add(0, getMenuActivityId(MENU_STEREO_LAYOUTS), 0, R.string.stereo3d_video_layout);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(getMenuOriginalId(item.getItemId())) {
        case MENU_STEREO_VIDEO:
            changeStereoMode();
            updateStereoVideoIcon();
            return true;
        case MENU_AC:
            case R.id.action_switch_auto_convergence:
                item.setChecked(!item.isChecked());
                StereoHelper.setACEnabled(mContext, false, item.isChecked());
                // tell SurfaceFlinger whether to do AC rendering or not
                updateConvergenceOffset();
            return true;
        case MENU_MC:
            enterDepthTuningMode();
            return true;
        case MENU_STEREO_LAYOUTS:
            enterVideoLayoutMode();
            return true;
        default:
            return false;
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateStereoVideoIcon();

        boolean supportsStereo = 
            StereoHelper.isStereo(getMovieItem().getStereoType());
        mMenuAC.setEnabled(supportsStereo);
        mMenuAC.setVisible(supportsStereo && !isMtk3DVideo());
        boolean isAcEnable = StereoHelper.getACEnabled(mContext, false);
        mMenuAC.setChecked(isAcEnable);
        mMenuMC.setEnabled(supportsStereo);
        mMenuMC.setVisible(supportsStereo);

        if (mInConvergenceTuningMode || mInVideoLayoutMode) {
            //if we are tuning depth or adjust video layout,
            //do not allow options menu
            return false;
        }
        return true;
    }
      /**
         *judge playing video from DB(MTK 3D Video or not)
         *If the video is mtk 3d video, "Auto depth tuning" option menu will be
         *not show.
         */
        private boolean isMtk3DVideo() {
            int isMtk3DVideo = 0;
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(getMovieItem().getUri(),
                        new String[]{MediaStore.Images.Media.IS_MTK_3D}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    isMtk3DVideo = cursor.getInt(0);
               }
            } catch (final SQLiteException ex) {
                ex.printStackTrace();
            } catch (IllegalArgumentException e) {
                //if this exception happen, return false.
                MtkLog.v(TAG, "ContentResolver query IllegalArgumentException");
            }finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (LOG) {
                MtkLog.v(TAG, "isMtk3DVideo() return " + isMtk3DVideo);
            }
            return (isMtk3DVideo == 1);
        }
      
    @Override
    public boolean onBackPressed() {
        if (mInConvergenceTuningMode && mConvBarManager != null) {
            mConvBarManager.dismissFirstRun();
            mConvBarManager.leaveConvTuningMode(false);
            return true;
        } else if (mInVideoLayoutMode && mVideoLayout != null) {
            mVideoLayout.leaveVideoLayoutMode(false);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MtkLog.d(TAG, "onConfigurationChanged");
        if (mInConvergenceTuningMode && mConvBarManager != null) {
            mConvBarManager.reloadFirstRun();
            mConvBarManager.reloadConvergenceBar();
        }
        if (mInVideoLayoutMode && mVideoLayout != null) {
            mVideoLayout.reloadVideoLayout();
        }
    }
    
    @Override
    public void onMovieItemChanged(final IMovieItem item) {
        super.onMovieItemChanged(item);
        setStereoLayout(item.getStereoType());
        initialStereoVideoIcon(item.getStereoType());
        
        // check and refresh AC status
        updateConvergenceOffset();
        
        //show first run
        if (mConvBarManager != null) {
            if (StereoHelper.isStereo(getMovieItem().getStereoType()) 
                    && !mInConvergenceTuningMode) {
                mConvBarManager.onStereoMediaOpened(false);
            }
        }
    }
    
    @Override
    public void setParameter(final String key, final Object value) {
        super.setParameter(key, value);
        if (value instanceof SurfaceView) {
            mVideoSurface = (SurfaceView) value;
            setStereoLayout(getMovieItem().getStereoType());
        } else if (value instanceof ViewGroup) {
            mRootView = (ViewGroup) value;
        } else if (value instanceof IMoviePlayer) {
            mPlayer = (IMoviePlayer) value;
        }
    }

    public static boolean isStereo3D(final int stereoType) {
        boolean stereo3d = true;
        if (stereoType == UNKNOWN || STEREO_TYPE_2D == stereoType) {
            stereo3d = false;
        }
        if (LOG) {
            MtkLog.v(TAG, "isStereo3D(" + stereoType + ") return " + stereo3d);
        }
        return stereo3d;
    }
    
    public void changeStereoMode() {
        setStereoLayout(mCurrentStereoType, !mCurrentStereoMode);
    }

    public void setStereoLayout(final int stereoType) {
        setStereoLayout(stereoType, StereoHelper.isStereo(stereoType));
    }
    
    public void setStereoLayout(final int stereoType, final boolean displayStereo) {
        if (LOG) {
            MtkLog.v(TAG, "setStereoLayout(" + stereoType + ",displayStereo=" + displayStereo + ")");
        }
        mCurrentStereoType = stereoType;
        mCurrentStereoMode = displayStereo;
        StereoHelper.setSfStereoLayout(mVideoSurface, stereoType, displayStereo);
    }
    
    private void updateStereoVideoIcon() {
        if (mMenuStereoVideo != null) {
            if (mCurrentStereoMode) {
                
                mMenuStereoVideo.setIcon(R.drawable.ic_switch_to_2d);
                mMenuStereoVideo.setTitle(R.string.stereo3d_mode_switchto_2d);
            } else {
                mMenuStereoVideo.setIcon(R.drawable.ic_switch_to_3d);
                mMenuStereoVideo.setTitle(R.string.stereo3d_mode_switchto_3d);
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "updateStereoVideoIcon() mMenuStereoVideoIcon=" + mMenuStereoVideo);
        }
    }
    
    private void initialStereoVideoIcon(final int stereoType) {
        if (mMenuStereoVideo != null) {
            //for disable 2D-3D conversion
            mMenuStereoVideo.setVisible(isStereo3D(stereoType));
            updateStereoVideoIcon();
        }
        if (LOG) {
            MtkLog.v(TAG, "initialStereoVideoIcon(" + stereoType + ") mSupport3DIcon=" + mMenuStereoVideo);
        }
    }
    
    private void initStereoDepthTuningBar() {
        // M: added for stereo image manual convergence tuning
        mConvBarManager = new ConvergenceBarManager(mContext, mRootView);
        mConvBarManager.setConvergenceListener(mConvChangeListener);
    }
    
    private void initStereoVideoLayout() {
        // M: added for video layout adjustment
        mVideoLayout = new StereoVideoLayout(mContext, mRootView);
        mVideoLayout.setVideoLayoutListener(mVideoLayoutListener);
    }
    
    private void updateStereoType(int stereoType, boolean updateDB) {
        MtkLog.i(TAG, "updateStereoLayout:stereoType="+stereoType);
        getMovieItem().setStereoType(stereoType);
        setStereoLayout(stereoType);
        if (updateDB) {
            StereoHelper.updateStereoLayout(mContext,
                             getMovieItem().getUri(), stereoType);
        }
        updateStereoVideoIcon();
    }
    
    private void resetStereoMode() {
        if (!StereoHelper.getACEnabled(mContext, false)) {
            mVideoSurface.setFlagsEx(WindowManager.LayoutParams.FLAG_EX_S3D_NO_GIA, 
                WindowManager.LayoutParams.FLAG_EX_S3D_NO_GIA);
        }
        else {
            setStereoLayout(getMovieItem().getStereoType());
        }
        updateStereoVideoIcon();
    }

    // M: added for stereo feature
    private int getOffsetForSF(int value) {
        return value;
    }

    public void updateConvergenceOffset() {
        if (!StereoHelper.isStereo(getMovieItem().getStereoType())) return;
        //when AC (Auto Convergence) is off/on, tell SurfaceFlinger to disable/enable AC
        if (!StereoHelper.getACEnabled(mContext, false)) {
            mVideoSurface.setFlagsEx(WindowManager.LayoutParams.FLAG_EX_S3D_NO_GIA, 
                WindowManager.LayoutParams.FLAG_EX_S3D_NO_GIA);
        }else {
            mVideoSurface.setFlagsEx(WindowManager.LayoutParams.FLAG_EX_S3D_UNKNOWN, 
                WindowManager.LayoutParams.FLAG_EX_S3D_NO_GIA);
        }
        mStoredProgress = getMovieItem().getConvergence();
        if (mStoredProgress < 0) mStoredProgress = CENTER;
        if (LOG) MtkLog.i(TAG, "updateConvergenceOffset:mStoredProgress="+mStoredProgress);
        mVideoSurface.setFlagsEx(getOffsetForSF(mStoredProgress), 
                WindowManager.LayoutParams.FLAG_EX_S3D_OFFSET_MANUAL_VALUE_MASK);
    }

    private void enterDepthTuningMode() {
        mStoredProgress = getMovieItem().getConvergence();
        if (LOG) MtkLog.i(TAG, "enterDepthTuningMode:mStoredProgress="+mStoredProgress);
        if (mStoredProgress < 0) mStoredProgress = CENTER;

        mConvBarManager.enterConvTuningMode(mRootView,
            convergenceValues, activeFlags, mStoredProgress);
    }

    private void enterVideoLayoutMode() {
        mStoredStereoType = getMovieItem().getStereoType();
        if (LOG) MtkLog.i(TAG, "enterVideoLayoutMode()");

        mVideoLayout.enterVideoLayoutMode(mRootView, mStoredStereoType);
    }

    private void updateConvergence(int progress) {
        StereoHelper.updateConvergence(mContext, 
                         false, getMovieItem().getId(), progress);
    }

    // for stereo3D phase 2: convergence manual tuning
    private int[] convergenceValues = {0, 15, 31, 47, 63, 79, 95, 111, 127, 143, 159, 175, 191, 207, 223, 239, 255};
    private int[] activeFlags = {0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0};
    private final int CENTER = 127;

    private boolean mInConvergenceTuningMode = false;
    private int mStoredProgress;
    private int mTempProgressWhenPause;
    private ConvergenceBarManager mConvBarManager;
    private ConvergenceBarManager.ConvergenceChangeListener
        mConvChangeListener = new ConvergenceBarManager.ConvergenceChangeListener() {
        
        @Override
        public void onLeaveConvTuningMode(boolean saveValue, int value) {
            mInConvergenceTuningMode = false;
            
            if (saveValue) {
                // save current value to DB
                updateConvergence(value);
                mStoredProgress = value;
                getMovieItem().setConvergence(value);
            } else {
                if (mVideoSurface == null) {
                    return;
                }
                mVideoSurface.setFlagsEx(getOffsetForSF(mStoredProgress), 
                        WindowManager.LayoutParams.FLAG_EX_S3D_OFFSET_MANUAL_VALUE_MASK);
            }
            // restore status of time bar and action bar
            updateMediaPlayerUI();
        }
        
        @Override
        public void onEnterConvTuningMode() {
            mInConvergenceTuningMode = true;
            resetStereoMode();
            // restore status of time bar and action bar
  
            if (mPlayer != null) {
                mPlayer.hideController();
            }
        }
        
        @Override
        public void onConvValueChanged(int value) {
            if (mVideoSurface == null) {
                return;
            }
            mVideoSurface.setFlagsEx(getOffsetForSF(value), 
                    WindowManager.LayoutParams.FLAG_EX_S3D_OFFSET_MANUAL_VALUE_MASK);
        }

        @Override
        public void onFirstRunHintShown() {
            // we borrow convergence tuning sequence here
            onEnterConvTuningMode();
        }

        @Override
        public void onFirstRunHintDismissed() {
            // we borrow convergence tuning sequence here
            mInConvergenceTuningMode = false;
            ((Activity)mContext).invalidateOptionsMenu();
            // unlock the timeBar
            updateMediaPlayerUI();
        }
    };

    private boolean mInVideoLayoutMode = false;
    private int mStoredStereoType;
    private StereoVideoLayout mVideoLayout;
    private StereoVideoLayout.VideoLayoutListener
        mVideoLayoutListener = new StereoVideoLayout.VideoLayoutListener() {
        
        @Override
        public void onLeaveVideoLayoutMode(boolean saveValue, int value) {
            if (LOG) MtkLog.i(TAG, "onLeaveVideoLayoutMode(saveValue=" +
                                   saveValue + ", value=" + value + ")");
            mInVideoLayoutMode = false;
            
            if (saveValue) {
                // save current value to DB
                updateStereoType(value, true);
                mStoredStereoType = value;
            } else {
                updateStereoType(mStoredStereoType, false);
            }
            // restore status of time bar and action bar
            updateMediaPlayerUI();
        }
        
        @Override
        public void onEnterVideoLayoutMode() {
            mInVideoLayoutMode = true;
            resetStereoMode();
            // restore status of time bar and action bar
          
            if (mPlayer != null) {
                mPlayer.hideController();
            }
        }
        
        @Override
        public void onVideoLayoutChanged(int stereoType) {
            if (LOG) MtkLog.i(TAG, "onVideoLayoutChanged(stereoType="+stereoType+")");
            //update stereo layout
            updateStereoType(stereoType, false);
        }
    };
    
    public boolean isPartialVisible() {
        return mInConvergenceTuningMode || mInVideoLayoutMode;
    }
    
    private void updateMediaPlayerUI() {
        MtkLog.i(TAG,"updateMediaPlayerUI");
        if (mPlayer != null) {
            mPlayer.updateUI();
        }
    }
    
    private void enhanceStereoActionBar(final IMovieItem movieItem, final int stereoType) {
        final String scheme = movieItem.getUri().getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) { //from file manager
            if (stereoType == UNKNOWN) {
                setInfoFromMediaData(movieItem, stereoType);
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            if ("media".equals(movieItem.getUri().getAuthority())) { //from media database
                if (stereoType == UNKNOWN) {
                    setInfoFromMediaUri(movieItem, stereoType);
                }
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "enhanceStereoActionBar() " + movieItem);
        }
    }
    
    private void setInfoFromMediaUri(final IMovieItem movieItem, final int stereoType) {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(movieItem.getUri(),
                    PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                if (stereoType == UNKNOWN) {
                    movieItem.setId(cursor.getInt(INDEX_ID));
                    movieItem.setStereoType(cursor.getInt(INDEX_STEREO_TYPE));
                    movieItem.setConvergence(cursor.getInt(INDEX_CONVERGENCE));
                }
           }
        } catch (final SQLiteException ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "setInfoFromMediaUri() " + movieItem);
        }
    }
    
    private void setInfoFromMediaData(final IMovieItem movieInfo, final int stereoType) {
        Cursor cursor = null;
        try {
            String data = Uri.decode(movieInfo.getUri().toString());
            data = data.replaceAll("'", "''");
            final String where = "_data LIKE '%" + data.replaceFirst("file:///", "") + "'";
            cursor = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, where, null, null);
            MtkLog.v(TAG, "setInfoFromMediaData() cursor=" + (cursor != null ? cursor.getCount() : "null"));
            if (cursor != null && cursor.moveToFirst()) {
                if (stereoType == UNKNOWN) {
                    movieInfo.setId(cursor.getInt(INDEX_ID));
                    movieInfo.setStereoType(cursor.getInt(INDEX_STEREO_TYPE));
                    movieInfo.setConvergence(cursor.getInt(INDEX_CONVERGENCE));
                }
           }
        } catch (final SQLiteException ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "setInfoFromMediaData() " + movieInfo);
        }
    }
}
