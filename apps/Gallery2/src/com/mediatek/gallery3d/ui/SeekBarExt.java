package com.mediatek.gallery3d.ui;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.SeekBar;

import com.android.gallery3d.R;

public class SeekBarExt extends SeekBar {
    
    private static final String TAG = "Seekbarext";
    
    private int mAutoRectifyRange = 5;
    private Drawable mConvergenceIndicator = null;
    private boolean mSqueezed = false;
    private int mConvOffset = 0;
    private int[] mConvArray;
    private int[] mMaskedArray;
    
    private Context mContext;
    private Vibrator mVibrator;
    
    private int mTouchSlopSquare = 0;
    private MotionEvent mCurrentDownEvent = null;
    private boolean mAlwaysInTapRegion = false;
    
    private OnSeekBarChangeListener mSelfListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mClientListener.onStopTrackingTouch(seekBar);
        }
        
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mClientListener.onStartTrackingTouch(seekBar);
        }
        
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser) {
            Log.i(TAG,"onProgressChanged(progress="+progress+",fromUser="+fromUser+")");
            // vibrate the device if we hit a convergence point
            int[] arrayToUse = (mMaskedArray != null && mMaskedArray.length > 0) ? 
                    mMaskedArray : mConvArray; 
            if (arrayToUse != null && arrayToUse.length > 0) {
                for (int i = 0; i < arrayToUse.length; ++i) {
                    if (progress == arrayToUse[i] && fromUser) {
                        mVibrator.vibrate(50);
                    }
                }
            }
            // the output progress value should be compensated according to offset
            mClientListener.onProgressChanged(seekBar, progress + mConvOffset, fromUser);
        }
    };
    
    private OnSeekBarChangeListener mClientListener;
    
    private int mMaxConvergence = 0;

    public SeekBarExt(Context context) {
        super(context);
        mContext = context;
        Log.v(TAG, "constructor #1 called");
        setEnabled(false);
        initializeDrawables();
        initViewConfigurations(context);
    }
    
    public SeekBarExt(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        Log.v(TAG, "constructor #2 called");
        setEnabled(false);
        initializeDrawables();
        initViewConfigurations(context);
    }
    
    public SeekBarExt(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        Log.v(TAG, "constructor #3 called");
        Log.v(TAG, "original thumb offset=" + getThumbOffset());
        setEnabled(false);
        initializeDrawables();
        initViewConfigurations(context);
    }
    
    protected synchronized void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw");
        
        Drawable progressDrawable = getProgressDrawable();
        Rect progressRect = progressDrawable.copyBounds();
        int intrinsicHeight = progressDrawable.getIntrinsicHeight();
        Log.i(TAG, "  orig drawable height=" + progressRect.height());
        Log.i(TAG, "  intrinsic drawable height=" + intrinsicHeight);
        
        
        int progressWidth = progressRect.width();
        // draw convergence indicators below the progress background
        int measuredHeight = getMeasuredHeight();
        // the actual top should be just below the progress drawable
        int indicatorW = mConvergenceIndicator.getIntrinsicWidth();
        int indicatorH = mConvergenceIndicator.getIntrinsicHeight();
        mConvergenceIndicator.setBounds(0, 0, indicatorW, indicatorH);
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int curProgress = getProgress();
        
        // draw all points from the convergence array, including starting and ending points
        boolean canDrawIndicators = 
                (mConvArray != null && mConvArray.length > 1 && mMaxConvergence > 0);
        if (canDrawIndicators) {
            int[] arrayToUse = ((mMaskedArray != null 
                    && mMaskedArray.length > 0) ? mMaskedArray : mConvArray);
            for (int i : arrayToUse) {
                // calculate horizontal offset according to point value/max value
                float scale = (float) i / (float) mMaxConvergence;
                if (scale > 1) {
                    continue;
                }
                int offset = (int) ((float) progressWidth * scale);
                canvas.save();
                canvas.translate(paddingLeft + offset - (indicatorW / 2), (measuredHeight - intrinsicHeight) / 2 - indicatorH);
                mConvergenceIndicator.draw(canvas);
                canvas.restore();
            }
        }
        
        progressRect = getProgressDrawable().copyBounds();
        int progressHeight = getProgressDrawable().getBounds().height();
        // we need to do this AFTER setThumb to avoid progress bar flicker
        mSqueezed = (progressHeight == intrinsicHeight);
        // squeeze to intrinsic height (real height of the original image)
        if (!mSqueezed && progressHeight > 0) {
            int heightDiff = progressHeight - intrinsicHeight;
            if (heightDiff > 0) {
                progressRect.set(progressRect.left, progressRect.top + heightDiff / 2, progressRect.right, progressRect.bottom - heightDiff / 2);
                progressDrawable.setBounds(progressRect);
                Log.i(TAG, "  after squeeze bounds=" + progressDrawable.getBounds());
                Log.i(TAG, "  after squeeze height=" + progressDrawable.getBounds().height());
            }
        }

        
        Log.e(TAG, " final drawing progress h=" + getProgressDrawable().getBounds().height());
        super.onDraw(canvas);
    }
    
    // added for supporting meaningful picked convergence points
    public void setConvergenceValues(int[] points, int[] valueMasks, int defaultValue) {
        if (points == null || points.length <= 0) {
            Log.e(TAG, "convergence array is not valid");
            return;
        }
        // find smallest and largest number in points array to be the '0' and 'max' values
        // we don't want to operate directly on the original array, so duplicate it
        int[] array = Arrays.copyOf(points, points.length);
        Arrays.sort(array);
        
        if (array == null || array.length <= 0) {
            return;
        }
        
        // the first is the smallest, and is the offset value of convergence
        mConvOffset  = array[0];
        
        
        // clip all convergence values according to offset
        for (int i = 0; i < array.length; ++i) {
            array[i] -= mConvOffset;
        }
        
        // get the max value and set as progress max
        mMaxConvergence  = array[array.length - 1];
        setMax(mMaxConvergence);

        mConvArray = array;
        
        // prepare drawing values according to mask array provided
        // 1. make sure masks are valid
        boolean isMaskValid = valueMasks != null && valueMasks.length == array.length;
        Log.d(TAG, "mask is " + (isMaskValid ? "valid" : "invalid"));
        
        // 2. filter out all masked values into mMaskedArray
        if (isMaskValid) {
            int mask = 0;
            ArrayList<Integer> maskArray = new ArrayList<Integer>();
            for (int i = 0; i < valueMasks.length; ++i) {
                mask = valueMasks[i];
                if (mask != 0) {
                    maskArray.add(array[i]);
                }
            }
            int maskSize = maskArray.size();
            if (maskSize > 0) {
                mMaskedArray = new int[maskSize];
                for (int i = 0; i < maskSize; ++i) {
                    mMaskedArray[i] = maskArray.get(i);
                }
            }
        }
        
        if (defaultValue >= array[0] && defaultValue <= array[array.length - 1]) {
            // this default value is valid
            setProgress(defaultValue - mConvOffset);
        }
        
        // 9 points, half-half division
        mAutoRectifyRange = mMaxConvergence / 8 / 2;
        Log.d(TAG, "setConvergenceValues: auto rectify radius=" + mAutoRectifyRange);
        // this bar is enabled only after the convergence points are set
        setEnabled(true);
    }
    
    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        super.setOnSeekBarChangeListener(mSelfListener);
        mClientListener = l;
    }
    
    private void initializeDrawables() {
        mConvergenceIndicator = getResources().getDrawable(R.drawable.ic_triangle);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) {
            return false;
        }
        
        // analyse user touch events for possible single tap gesture
        boolean singleTapFired = false;
        switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mCurrentDownEvent = MotionEvent.obtain(e);
            mAlwaysInTapRegion = true;
            mIsDown = true;
            break;
        case MotionEvent.ACTION_MOVE:
            if (!mIsDown) {
                // no move without down
                return true;
            }
            int deltaX = (int) (e.getX() - mCurrentDownEvent.getX());
            int deltaY = (int) (e.getY() - mCurrentDownEvent.getY());
            int distance = (deltaX * deltaX) + (deltaY * deltaY);
            if (distance > mTouchSlopSquare) {
                mAlwaysInTapRegion = false;
            }
            break;
        case MotionEvent.ACTION_UP:
            mIsDown = false;
            if (mAlwaysInTapRegion) {
                // call single tap listener/callback
                Log.e(TAG, "onSingleTapUp!!!");
                onSingleTapUp(getProgress());
                singleTapFired = true;
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            mIsDown = false;
            break;
        }
        if (singleTapFired) {
            return true;
        } else {
            return super.onTouchEvent(e);
        }
    }
    
    private void initViewConfigurations(Context context) {
        int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTouchSlopSquare = touchSlop * touchSlop;
    }
    
    private void onSingleTapUp(int progress) {
        Log.i(TAG, "onSingleTapUp: " + progress);
        // rectify this progress value to the closest convergence point
        if (mConvArray == null || mConvArray.length <= 0 
                || mAutoRectifyRange <= 0) {
            return;
        }
        int[] arrayToUse = (mMaskedArray != null && mMaskedArray.length > 0) ? 
              mMaskedArray : mConvArray;
        for (int i = 0; i < arrayToUse.length; ++i) {
            Log.i(TAG, " onSingleTapUp: array[" + i + "]=" + arrayToUse[i]);
            if (Math.abs(arrayToUse[i] - progress) <= mAutoRectifyRange) {
                // if progress is within allowed offset
                // we regard this progress as the closest one in convergence array
                setProgress(arrayToUse[i]);
                // Although this is not from user, we still want the vibrate
                mVibrator.vibrate(50);
            }
        }
    }
    
    public void overrideAutoRectify(int rectifyValue) {
        mAutoRectifyRange = rectifyValue;
    }
    
    private boolean mIsDown = false;
    public void pause() {
        // disable further move events
        // to prevent seekbar jumping to undesired positions
        mIsDown = false;
    }
}
