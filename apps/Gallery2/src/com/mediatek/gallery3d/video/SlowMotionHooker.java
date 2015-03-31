package com.mediatek.gallery3d.video;

import android.view.Menu;
import android.view.MenuItem;

import com.android.gallery3d.R;
import com.mediatek.gallery3d.ext.IMovieItem;
import com.mediatek.gallery3d.ext.MovieUtils;
import com.mediatek.gallery3d.util.MtkLog;

public class SlowMotionHooker extends MovieHooker { 
    private static final String TAG = "Gallery2/SlowMotionHooker";
    
    private static final int MENU_SLOW_MOTION = 1;
    private static final int SLOW_MOTION_QUARTER_SPEED = 4; // 1/4X
    private static final int SLOW_MOTION_HALF_SPEED = 2;    // 1/2X
    private static final int SLOW_MOTION_NORMAL_SPEED = 1;  // 1X
    
//    private static final int QUARTER_SPEED = 4;
//    private static final int HALF_SPEED = 2;
//    private static final int NORMAL_SPEED = 1;
    
    private MenuItem mMenuSlowMotion;
    private int mCurrentMode;
    private int mNextMode;
    

    
    private void updateSlowMotionIcon() {
        MtkLog.v(TAG, "updateSlowMotionIcon() mCurrentMode = " + mCurrentMode);
        if(mMenuSlowMotion != null) {
            if(mCurrentMode == SLOW_MOTION_QUARTER_SPEED) {
                mMenuSlowMotion.setIcon(R.drawable.ic_slowmotion_quarter_speed); 
//                mMenuSlowMotion.setIcon(R.drawable.ic_stereo_overlay);
                mNextMode = SLOW_MOTION_HALF_SPEED;
                getPlayer().refreshSlowMotionSpeed(mCurrentMode);
            } else if(mCurrentMode == SLOW_MOTION_HALF_SPEED){
                mMenuSlowMotion.setIcon(R.drawable.ic_slowmotion_half_speed);
                mNextMode = SLOW_MOTION_NORMAL_SPEED;
                getPlayer().refreshSlowMotionSpeed(mCurrentMode);
            } else if(mCurrentMode == SLOW_MOTION_NORMAL_SPEED) {
                mMenuSlowMotion.setIcon(R.drawable.ic_slowmotion_normal_speed);
                mNextMode = SLOW_MOTION_QUARTER_SPEED;
                getPlayer().refreshSlowMotionSpeed(mCurrentMode);
            }
        }
    }
    
    private void initialSlowMotionIcon(final int speed) {
        MtkLog.v(TAG, "initialSlowMotionIcon()");
        if(mMenuSlowMotion != null) {
            mMenuSlowMotion.setVisible(speed != 0);
            mCurrentMode = speed;
            if(mCurrentMode !=0) {
                updateSlowMotionIcon();
            }
           
        }
    }
    
    @Override
    public void onMovieItemChanged(final IMovieItem item){
        MtkLog.v(TAG, "onMovieItemChanged() " + mMenuSlowMotion);
        if(mMenuSlowMotion != null) {
            initialSlowMotionIcon(MovieUtils.isSlowMotion(getContext(),item.getUri()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        MtkLog.v(TAG, "onCreateOptionsMenu()");
        mMenuSlowMotion = menu.add(0, getMenuActivityId(MENU_SLOW_MOTION), 0, R.string.slow_motion_speed);
        mMenuSlowMotion.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        initialSlowMotionIcon(MovieUtils.isSlowMotion(getContext(),getMovieItem().getUri()));
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MtkLog.v(TAG, "onPrepareOptionsMenu()");
//        updateSlowMotionIcon();
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (getMenuOriginalId(item.getItemId())) {
        case MENU_SLOW_MOTION:
            MtkLog.v(TAG, "onOptionsItemSelected() mNextMode = " + mNextMode);
            mCurrentMode = mNextMode;
            updateSlowMotionIcon();
            return true;
        default:
            return false;
        }
    }


}
