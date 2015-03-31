package com.mediatek.gallery3d.mpo;

import android.os.Process;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.util.Log;
import com.mediatek.gallery3d.util.MtkLog;


public class MavRenderThread extends Thread {
    private String TAG = "Gallery2/MavPlayer";
    private AbstractGalleryActivity mGalleryActivity;
    private Animation mAnimation= null;
    private OnDrawMavFrameListener mOnDrawMavFrameListener= null;
    private Object mRenderLock = new Object();
    public boolean mRenderRequested = false;
    public boolean mIsActive = true;
    public static final int CONTINUOUS_FRAME_ANIMATION = 0;
    public static final int INTERRUPT_FRAME_ANIMATION = 1;
    public static final int CONTINUOUS_FRAME_ANIMATION_CHANGE_THRESHOLD = 2;
    public interface OnDrawMavFrameListener {
        public void drawMavFrame(int index);
    }
    public MavRenderThread(AbstractGalleryActivity galleryActivity) {
        super("MavRenderThread");
        mGalleryActivity = galleryActivity;
        mAnimation = new Animation();
    }
 
    public void  setRenderRequester (boolean request) {
        synchronized (mRenderLock) {
            mRenderRequested = request;
            mRenderLock.notifyAll();
        }
    }
    
    public void setActive (boolean isActive) {
        mIsActive = isActive;
    }
    
    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        while (true && !Thread.currentThread().isInterrupted()) {
            Log.d(TAG, "run~~~~~~~~~~~~~~~~~"+Thread.currentThread().getId()+"    mRenderRequested=="+mRenderRequested);
            if (mGalleryActivity.hasPausedActivity() || !mIsActive) {
                MtkLog.v(TAG, "MavRenderThread:run: exit MavRenderThread");
                return;
            }
            boolean isFinished = false;
            synchronized (mRenderLock) {
                if (mRenderRequested) {
                    isFinished = mAnimation.advanceAnimation();
                    mRenderRequested = (!isFinished) ? true : false;
                    mOnDrawMavFrameListener.drawMavFrame(mAnimation.getCurrentFramIndex());
                } else {
                    try {
                        mRenderLock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            if (!isFinished) {
                try {
                    Thread.sleep((long)(mAnimation.getIntervalTime()*1000));
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    public int getAnimationTagetIndex() {
        return mAnimation.getTagetFrameIndex();
    }
    
    public void initAnimation (int index ,int type) {
        mAnimation.initAnimation(index, type);
    }
    
    public boolean animationFinished () {
        return mAnimation.isFinished();
    }
    
    private class Animation {

        private int mCurrentMavFrameIndex = 0xFFFF;
        private static final double INTERVALTIME = 0.02;
        private double mIntervalTime = 0;
        
        private int mTargetMavFrameIndex = 0xFFFF;
        private int mNumberOfAnimationFrame ;
        private int mCurrenfAnimationFrame = 1;
        private double RATIO ;
        
        public int mType = 0;
        
        public void initAnimation(int lastIndex, int type) {
            mType = type;
            if (mCurrentMavFrameIndex == 0xFFFF && lastIndex != 0xFFFF) {
                mCurrentMavFrameIndex = lastIndex;
            }
            mCurrenfAnimationFrame = 0;
            mTargetMavFrameIndex = lastIndex;
            mNumberOfAnimationFrame = Math.abs(mCurrentMavFrameIndex - mTargetMavFrameIndex);
            RATIO = (Math.sqrt(mNumberOfAnimationFrame) - Math.sqrt(mNumberOfAnimationFrame - 1))/INTERVALTIME; 
        }

        private boolean advanceAnimation() {
            if (mCurrentMavFrameIndex == 0xFFFF || mTargetMavFrameIndex == 0xFFFF) {
                return true;
            }
            int DValue = mCurrentMavFrameIndex - mTargetMavFrameIndex;
            if (mType == CONTINUOUS_FRAME_ANIMATION) {
                mCurrenfAnimationFrame++;
                mCurrentMavFrameIndex = DValue > 0 ? mCurrentMavFrameIndex-1 : (DValue < 0 ? mCurrentMavFrameIndex + 1 : mCurrentMavFrameIndex);
                mIntervalTime = 0.02;//(Math.sqrt(mCurrenfAnimationFrame) - Math.sqrt(mCurrenfAnimationFrame - 1))/RATIO;
            } else if (mType == INTERRUPT_FRAME_ANIMATION) {
                mCurrentMavFrameIndex = mTargetMavFrameIndex;
            }
            Log.d(TAG, "DValue======"+DValue +"   mCurrenfAnimationFrame=="+mCurrenfAnimationFrame
                    +"  mIntervalTime==="+mIntervalTime
                    + "  mCurrentMavFrameIndex=="+mCurrentMavFrameIndex
                    +"  mTargetMavFrameIndex=="+mTargetMavFrameIndex);
            return isFinished();
        }

        private boolean isFinished() {
            return mCurrentMavFrameIndex == mTargetMavFrameIndex;
        }

        private double getIntervalTime () {
            return mIntervalTime;
        }

        private int getCurrentFramIndex () {
            return mCurrentMavFrameIndex;
        }

        private int getTagetFrameIndex () {
            return mCurrentMavFrameIndex;
        }
    }

    public void setOnDrawMavFrameListener (OnDrawMavFrameListener  lisenter) {
        mOnDrawMavFrameListener = lisenter;
    }
}

