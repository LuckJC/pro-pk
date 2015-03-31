package com.mediatek.gallery3d.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.content.res.Configuration;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.animation.TranslateAnimation;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.mediatek.gallery3d.stereo.StereoHelper;

public class StereoVideoLayout implements View.OnClickListener,
                                          View.OnLongClickListener {

    private static final String TAG = "StereoVideoLayout";
    
    private static final int ANIM_SHOWUP_DURATION = 700;
    private static final int LAYOUT_TYPE_COUNT = 5;

    private static class LayoutViewString {
        public int stereoLayout;
        public int viewId;
        public int stringId;

        public LayoutViewString(int layout, int vId, int strId) {
            stereoLayout = layout;
            viewId = vId;
            stringId = strId;
        }
    }

    private static LayoutViewString [] sLayoutViewString;

    static {
        sLayoutViewString = new LayoutViewString[LAYOUT_TYPE_COUNT];

        int k = 0;
        // for 2d layout
        sLayoutViewString[k++] = new LayoutViewString(
            StereoHelper.STEREO_TYPE_2D,
            R.id.default_layout,
            R.string.stereo3d_video_layout_normal);
        // for side by side 3d layout
        sLayoutViewString[k++] = new LayoutViewString(
            StereoHelper.STEREO_TYPE_SIDE_BY_SIDE,
            R.id.side_by_side,
            R.string.stereo3d_video_layout_sidebyside);
        // for top and bottom 3d layout
        sLayoutViewString[k++] = new LayoutViewString(
            StereoHelper.STEREO_TYPE_TOP_BOTTOM,
            R.id.top_and_bottom,
            R.string.stereo3d_video_layout_toptobottom);
        // for side by side (swapped) 3d layout
        sLayoutViewString[k++] = new LayoutViewString(
            StereoHelper.STEREO_TYPE_SWAP_LEFT_RIGHT,
            R.id.sbs_swap,
            R.string.stereo3d_video_layout_leftrightswap);
        // for top and bottom (swapped) 3d layout
        sLayoutViewString[k++] = new LayoutViewString(
            StereoHelper.STEREO_TYPE_SWAP_TOP_BOTTOM,
            R.id.tab_swap,
            R.string.stereo3d_video_layout_topbottomswap);
    }

    private static int getStringIdForView(final int vId) {
        for (int i = 0; i < sLayoutViewString.length; i++) {
            if (vId == sLayoutViewString[i].viewId) {
                return sLayoutViewString[i].stringId;
            }
        }
        return 0;
    }

    private static int getLayoutForView(final int vId) {
        for (int i = 0; i < sLayoutViewString.length; i++) {
            if (vId == sLayoutViewString[i].viewId) {
                return sLayoutViewString[i].stereoLayout;
            }
        }
        return 0;
    }

    private static int getViewForLayout(final int stereoLayout) {
        for (int i = 0; i < sLayoutViewString.length; i++) {
            if (stereoLayout == sLayoutViewString[i].stereoLayout) {
                return sLayoutViewString[i].viewId;
            }
        }
        return 0;
    }

    private final HashMap<Integer, ImageButton> mLayoutButtons =
              new HashMap<Integer, ImageButton>();
    private int mSelectedViewId = 0;

    private RelativeLayout mVideoLayout;
    private Button mBtnOK;
    private Button mBtnCancel;
    private TranslateAnimation mAnimShowUp;
    private boolean mVideoLayoutChangeFired;
    private ViewGroup mParent;
    private Context mContext;
    
    private VideoLayoutListener mListener;
    private View.OnLayoutChangeListener mVideoLayoutListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            Log.w(TAG, "onLayoutChange mVideoLayoutChangeFired" + mVideoLayoutChangeFired);
            if (!mVideoLayoutChangeFired) {
                int panelHeight = Math.abs(bottom - top);
                mAnimShowUp = new TranslateAnimation(0, 0, panelHeight, 0);
                mAnimShowUp.setDuration(ANIM_SHOWUP_DURATION);
                mVideoLayout.startAnimation(mAnimShowUp);
            } else {
                if (mListener != null) {
                   mListener.onEnterVideoLayoutMode();
                }
            }
            mVideoLayoutChangeFired = true;
        }
    };
    
    public interface VideoLayoutListener {
        public void onEnterVideoLayoutMode();
        public void onLeaveVideoLayoutMode(boolean saveValue, int stereoLayout);
        public void onVideoLayoutChanged(int stereoLayout);
    }
    
    public StereoVideoLayout(Context context, ViewGroup parent) {
        mContext = context;
        mParent = parent;
    }
    
    public void setVideoLayoutListener(VideoLayoutListener listener) {
        mListener = listener;
    }
    
    public void enterVideoLayoutMode(ViewGroup parent, int stereoLayout) {
        mParent = parent;
        mSelectedViewId = getViewForLayout(stereoLayout);
        initVideoLayout();
        mVideoLayout.setVisibility(View.VISIBLE);
        if (mListener != null) {
            mListener.onEnterVideoLayoutMode();
        }
    }
    
    public void leaveVideoLayoutMode(boolean saveValue) {
        if (mVideoLayout == null || mVideoLayout.getVisibility() != View.VISIBLE) {
            return;
        }
        if (mAnimShowUp != null) {
            if (mAnimShowUp.hasStarted() && !mAnimShowUp.hasEnded()) {
                mAnimShowUp.cancel();
            } else if (mAnimShowUp.hasEnded()) {
                TranslateAnimation anim = new TranslateAnimation(0, 0, 0,
                                                  mVideoLayout.getHeight());
                anim.setDuration(ANIM_SHOWUP_DURATION);
                mVideoLayout.startAnimation(anim);
            }
        }
        mVideoLayout.setVisibility(View.GONE);
        if (mListener != null) {
            int targetLayout = getLayoutForView(mSelectedViewId);
            mListener.onLeaveVideoLayoutMode(saveValue, targetLayout);
        }
    }
    
    private void initVideoLayout() {
        Log.d(TAG, "initVideoLayout()");
        ViewGroup parent = mParent;
        if (mVideoLayout != null) {
            parent.removeView(mVideoLayout);
            mVideoLayout = null;
        }
        mVideoLayout = (RelativeLayout)View.inflate(mContext,
            R.layout.stereo_video_layout, null);
        mVideoLayout.addOnLayoutChangeListener(mVideoLayoutListener);
        mVideoLayoutChangeFired = false;
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        if(mContext.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar)) {
            if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Log.d(TAG, "initVideoLayout portrait");
                rlp.setMargins(0,0,0,mContext.getResources().getDimensionPixelSize(
                                            com.android.internal.R.dimen.navigation_bar_height));
            } else {
                Log.d(TAG, "initVideoLayout landscape");
                rlp.setMargins(0,0,mContext.getResources().getDimensionPixelSize(
                                            com.android.internal.R.dimen.navigation_bar_height_landscape),0);
            }
        }
        parent.addView(mVideoLayout, rlp);

        mBtnOK = (Button) mVideoLayout.findViewById(R.id.btn_ok);
        mBtnCancel = (Button) mVideoLayout.findViewById(R.id.btn_cancel);

        mBtnOK.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);

        mLayoutButtons.clear();
        for (int i = 0; i < sLayoutViewString.length; i++) {
            int viewId = sLayoutViewString[i].viewId;
            ImageButton temp = (ImageButton)mVideoLayout.findViewById(viewId);
            temp.setOnClickListener(this);
            temp.setOnLongClickListener(this);
            mLayoutButtons.put(viewId, temp);
            // init selection status
            if (mSelectedViewId != 0 && mSelectedViewId == viewId) {
                temp.setSelected(true);
            }
        }
    }
    
    public void reloadVideoLayout() {
        if (mVideoLayout.getVisibility() != View.VISIBLE) {
            return;
        }
        
        initVideoLayout();
        mVideoLayout.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onClick(View v) {
        int vId = v.getId();

        // for video layout image buttons
        if (mLayoutButtons.containsKey(vId)) {
            onClickVideoLayout(vId);
            return;
        }

        // for "OK" and "Cancel" buttons
        switch (vId) {
        case R.id.btn_ok:
            leaveVideoLayoutMode(true);
            break;
        case R.id.btn_cancel:
            leaveVideoLayoutMode(false);
            break;
        }
    }
    
    private void onClickVideoLayout(final int vId) {
        ImageButton layoutButton;
        for(int id : mLayoutButtons.keySet()) {
            layoutButton = mLayoutButtons.get(id);
            if (null == layoutButton) {
                Log.e(TAG, "onClickVideoLayout: we why got null for " + vId);
                continue;
            }
            if (vId == id) {
                // for clicked button, set!
                layoutButton.setSelected(true);
                mSelectedViewId = vId;

            } else {
                // for unclicked button, reset
                layoutButton.setSelected(false);
            }
        }
        // find select video layout
        int stereoLayout = getLayoutForView(vId);
        mListener.onVideoLayoutChanged(stereoLayout);
    }

    @Override
    public boolean onLongClick(View v) {
        int vId = v.getId();

        // for video layout image buttons
        if (mLayoutButtons.containsKey(vId)) {
            onLongClickVideoLayout(vId);
            return true;
        }
        return false;
    }
    
    private void onLongClickVideoLayout(final int vId) {
        // retrieve toast string id for view
        int strId = getStringIdForView(vId);
        // show user toast
        Toast temp = Toast.makeText(mContext, strId, Toast.LENGTH_SHORT);
        // I don't know why, but the default position of toast is measured
        // from bottom up. so place the toast on top of Video Layout panel
        // mVideoLayout.getHeight() is the height of video layout panel.
        temp.setGravity(temp.getGravity(), temp.getXOffset(),
                        mVideoLayout.getHeight());
        temp.show();
    }

}
