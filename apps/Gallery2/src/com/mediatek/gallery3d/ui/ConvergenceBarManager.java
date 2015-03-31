package com.mediatek.gallery3d.ui;

import android.content.res.Configuration;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.animation.TranslateAnimation;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.android.gallery3d.R;

public class ConvergenceBarManager implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "ConvBarManager";
    
    private static final int ANIM_SHOWUP_DURATION = 700;
    private static final String SHARED_PREF_NAME = "convergence_tuning";
    private static final String PREFTAG_FIRSTHINT_SHOWED_IMAGE = "first_hint_showed_image";
    private static final String PREFTAG_FIRSTHINT_SHOWED_VIDEO = "first_hint_showed_video";
    
    private RelativeLayoutIgnoreTouch mConvBar;
    private SeekBarExt mSeekBar;
    private Button mBtnConvOK;
    private Button mBtnConvCancel;
    private TranslateAnimation mAnimShowUp;
    private int mValue;
    private boolean mConvBarLayoutChangeFired;
    private ViewGroup mParent;
    private Context mContext;
    private int[] mConvValues;
    private int[] mValueMasks;
    
    private ConvergenceChangeListener mListener;
    private int mConvBarHeight;
    private View.OnLayoutChangeListener mConvBarLayoutListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            Log.w(TAG, "onLayoutChange");
            if (!mConvBarLayoutChangeFired) {
                mConvBarHeight = Math.abs(bottom - top);
                mAnimShowUp = new TranslateAnimation(0, 0, mConvBarHeight, 0);
                mAnimShowUp.setDuration(ANIM_SHOWUP_DURATION);
                mConvBar.startAnimation(mAnimShowUp);
            } else {
               if (mListener != null) {
                    mListener.onEnterConvTuningMode();
               } 
            }
            mConvBarLayoutChangeFired = true;
        }
    };
    
    public interface ConvergenceChangeListener {
        public void onEnterConvTuningMode();
        public void onLeaveConvTuningMode(boolean saveValue, int value);
        public void onConvValueChanged(int value);
        public void onFirstRunHintShown();
        public void onFirstRunHintDismissed();
    }
    
    public ConvergenceBarManager(Context context, ViewGroup parent) {
        mContext = context;
        mParent = parent;
    }
    
    public void setConvergenceListener(ConvergenceChangeListener listener) {
        mListener = listener;
    }
    
    private void showConvergenceBar() {
        
    }
    
    public void enterConvTuningMode(ViewGroup parent, int[] convValues, int[] valueMasks, int defValue) {
        mParent = parent;
        mConvValues = convValues;
        mValueMasks = valueMasks;
        //for rotate case: reloadConvergenceBar, the defaut value should not be CENTER. 
        mValue = defValue;
        initConvergenceViews();
        mSeekBar.setConvergenceValues(convValues, valueMasks, defValue);
        //when setMax, the progress bar will refresh and set 0 to SF, made screen dither, 
        //put the listener set back to fix it.
        mSeekBar.setOnSeekBarChangeListener(this);
        mConvBar.setVisibility(View.VISIBLE);
        if (mListener != null) {
            mListener.onEnterConvTuningMode();
        }
    }
    
    public void enterConvTuningMode(ViewGroup parent, int[] convValues, int defValue) {
        enterConvTuningMode(parent, convValues, null, defValue);
    }
    
    public void leaveConvTuningMode(boolean saveValue) {
        leaveConvTuningMode(saveValue, true);
    }

    public void leaveConvTuningMode(boolean saveValue, boolean showAnimation) {
        if (mConvBar == null || mConvBar.getVisibility() != View.VISIBLE) {
            return;
        }

        if (showAnimation && mAnimShowUp != null) {
            if (mAnimShowUp.hasStarted() && !mAnimShowUp.hasEnded()) {
                mAnimShowUp.cancel();
            } else if (mAnimShowUp.hasEnded()) {
                TranslateAnimation anim = new TranslateAnimation(0, 0, 0, mConvBar.getHeight());
                anim.setDuration(ANIM_SHOWUP_DURATION);
                mConvBar.startAnimation(anim);
            }
        }

        mConvBar.setVisibility(View.GONE);
        if (mListener != null) {
            mListener.onLeaveConvTuningMode(saveValue, mValue);
        }
    }
    
    private void initConvergenceViews() {
        Log.d(TAG, "initConvergenceViews");
        // initialize manual convergence views here
        ViewGroup parent = mParent;
        if (mConvBar != null) {
            parent.removeView(mConvBar);
            // reset convergence bar height/convergence bar animation to prevent mis-use
            mConvBarHeight = 0;
            mConvBar = null;
        }
        mConvBar = (RelativeLayoutIgnoreTouch) View.inflate(mContext, R.layout.convergence_bar, null);
        mConvBar.addOnLayoutChangeListener(mConvBarLayoutListener);
        mConvBarLayoutChangeFired = false;
        if (parent instanceof RelativeLayout) {
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            if(mContext.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar)) {
                if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Log.d(TAG, "initConvergenceViews RelativeLayout portrait");
                    rlp.setMargins(0,0,0,mContext.getResources().getDimensionPixelSize(
                                                com.android.internal.R.dimen.navigation_bar_height));
                } else {
                    Log.d(TAG, "initConvergenceViews RelativeLayout landscape");
                    rlp.setMargins(0,0,mContext.getResources().getDimensionPixelSize(
                                                com.android.internal.R.dimen.navigation_bar_height_landscape),0);
                }
            }
            parent.addView(mConvBar, rlp);
        } else if (parent instanceof FrameLayout) {
            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            if(mContext.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar)) {
                if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Log.d(TAG, "initConvergenceViews FrameLayout portrait");
                    flp.setMargins(0,0,0,mContext.getResources().getDimensionPixelSize(
                                                com.android.internal.R.dimen.navigation_bar_height));
                } else {
                    Log.d(TAG, "initConvergenceViews FrameLayout landscape");
                    flp.setMargins(0,0,mContext.getResources().getDimensionPixelSize(
                                                com.android.internal.R.dimen.navigation_bar_height_landscape),0);
                }
            }
            parent.addView(mConvBar, flp);
        }
        Log.i(TAG, " convergence bar=" + mConvBar);
        mSeekBar = (SeekBarExt) mConvBar.findViewById(R.id.seekbar_conv);
        Log.i(TAG, " seek bar=" + mSeekBar);
        mBtnConvOK = (Button) mConvBar.findViewById(R.id.btn_conv_ok);
        Log.i(TAG, " mBtnConvOK=" + mBtnConvOK);
        mBtnConvCancel = (Button) mConvBar.findViewById(R.id.btn_conv_cancel);
        Log.i(TAG, " mBtnConvCancel=" + mBtnConvCancel);

        mBtnConvOK.setOnClickListener(this);
        mBtnConvCancel.setOnClickListener(this);
    }
    
    public void reloadConvergenceBar() {
        if (mConvBar == null || mConvBar.getVisibility() != View.VISIBLE) {
            return;
        }
        
        initConvergenceViews();
        mSeekBar.setConvergenceValues(mConvValues, mValueMasks, mValue);
        //for rotate case: in this case must set listener for seekBar as enter depth tuning
        mSeekBar.setOnSeekBarChangeListener(this);
        mConvBar.setVisibility(View.VISIBLE);
    }
    
    

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        mValue = progress;
        if (mListener != null) {
            mListener.onConvValueChanged(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onClick(View v) {
        // for "OK" and "Cancel" buttons
        switch (v.getId()) {
        case R.id.btn_conv_ok:
            /// TODO: save current value to DB
            leaveConvTuningMode(true);
            break;
        case R.id.btn_conv_cancel:
            // reset current value
            mValue = 0;
            leaveConvTuningMode(false);
            break;
        }
    }
    
    
    // M: for stereo first run
    private RelativeLayoutIgnoreTouch mFirstRunLayout;
    public void onStereoMediaOpened(boolean isImage) {
        SharedPreferences pref = mContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String tagToUse = isImage ? PREFTAG_FIRSTHINT_SHOWED_IMAGE : PREFTAG_FIRSTHINT_SHOWED_VIDEO;
        boolean needShowHint = !(pref.getBoolean(tagToUse, false));
 
        if (needShowHint) {
            showStereoFirstRunHint();
            if (mListener != null) {
                mListener.onFirstRunHintShown();
            }
            Editor ed = pref.edit();
            ed.putBoolean(tagToUse, true);
            ed.commit();
        }
    }

    private void showStereoFirstRunHint() {
        initFirstRunViews();
    }
    
    private void initFirstRunViews() {
        Log.d(TAG, "initFirstRunViews");
        if (mConvBar != null && mConvBar.getVisibility() == View.VISIBLE) {
            return;
        }
        // initialize manual convergence views here
        ViewGroup parent = mParent;
        if (mFirstRunLayout != null) {
            parent.removeView(mFirstRunLayout);
            mFirstRunLayout = null;
        }
        mFirstRunLayout = (RelativeLayoutIgnoreTouch) View.inflate(mContext, R.layout.stereo_first_run, null);
        mFirstRunLayout.addOnLayoutChangeListener(mFirstRunLayoutListener);
        mFirstRunLayoutChangeFired = false;
        if (parent instanceof RelativeLayout) {
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            if(mContext.getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar)) {
                if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Log.d(TAG, "initFirstRunViews RelativeLayout portrait");
                    rlp.setMargins(0,0,0,mContext.getResources().getDimensionPixelSize(
                                                com.android.internal.R.dimen.navigation_bar_height));
                } else {
                    Log.d(TAG, "initFirstRunViews RelativeLayout landscape");
                    rlp.setMargins(0,0,mContext.getResources().getDimensionPixelSize(
                                                com.android.internal.R.dimen.navigation_bar_height_landscape),0);
                }
            }
            parent.addView(mFirstRunLayout, rlp);
        } else if (parent instanceof FrameLayout) {
            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Log.d(TAG, "initFirstRunViews FrameLayout portrait");
                flp.setMargins(0,0,0,mContext.getResources().getDimensionPixelSize(
                                            com.android.internal.R.dimen.navigation_bar_height));
            } else {
                Log.d(TAG, "initFirstRunViews FrameLayout landscape");
                flp.setMargins(0,0,mContext.getResources().getDimensionPixelSize(
                                            com.android.internal.R.dimen.navigation_bar_height_landscape),0);
            }
            parent.addView(mFirstRunLayout, flp);
        }

        Button btnFirstRunDismiss = (Button) mFirstRunLayout.findViewById(R.id.btn_conv_firstrun_dismiss);
        btnFirstRunDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissFirstRun();
            }
        });
    }
    
    private boolean mFirstRunLayoutChangeFired = false;
    private TranslateAnimation mAnimFirstRunShowUp;
    private View.OnLayoutChangeListener mFirstRunLayoutListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            Log.w(TAG, "onLayoutChange(firstrun)");
            if (!mFirstRunLayoutChangeFired) {
                int layoutHeight = Math.abs(bottom - top);
                mAnimFirstRunShowUp = new TranslateAnimation(0, 0, layoutHeight, 0);
                mAnimFirstRunShowUp.setDuration(ANIM_SHOWUP_DURATION);
                mFirstRunLayout.startAnimation(mAnimFirstRunShowUp);
            } else {
               if (mListener != null) {
                   mListener.onFirstRunHintShown();
                }
            }
            mFirstRunLayoutChangeFired = true;
        }
    };
    
    public void dismissFirstRun() {
        if (mFirstRunLayout == null || mFirstRunLayout.getVisibility() != View.VISIBLE) {
            return;
        }
        if (mAnimFirstRunShowUp != null) {
            if (mAnimFirstRunShowUp.hasStarted() && !mAnimFirstRunShowUp.hasEnded()) {
                mAnimFirstRunShowUp.cancel();
            } else if (mAnimFirstRunShowUp.hasEnded()) {
                TranslateAnimation anim = new TranslateAnimation(0, 0, 0, mFirstRunLayout.getHeight());
                anim.setDuration(ANIM_SHOWUP_DURATION);
                mFirstRunLayout.startAnimation(anim);
            }
        }
        mFirstRunLayout.setVisibility(View.GONE);
        if (mListener != null) {
            mListener.onFirstRunHintDismissed();
        }
    }
    
    public void reloadFirstRun() {
        if (mFirstRunLayout == null || mFirstRunLayout.getVisibility() != View.VISIBLE) {
            return;
        }
        initFirstRunViews();
    }
    
    // adjust the auto-rectify value
    public void overrideAutoRectify(int rectifyValue) {
        if (mSeekBar != null) {
            mSeekBar.overrideAutoRectify(rectifyValue);
        }
    }
    
    public void pause() {
        if (mSeekBar != null) {
            mSeekBar.pause();
        }
    }
}
