package com.mediatek.gallery3d.panorama;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.glrenderer.NinePatchTexture;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.ui.GLView.OnClickListener;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.util.MediatekFeature;

public class PanoramaSwitchBarView extends GLView {
    private static final String TAG = "Gallery2/PanoramaSwitchBarView";
    private static final int GRAY = 0xFFAAAAAA;
    public static final int SWITCH_BUTTON_LENG = 2;
    public static final int SWITCH_BAR_TOP_GAP = 100;
    public static final int SWITCH_BUTTON_GAP = (int) GalleryUtils.dpToPixel(10);
    public static final int INVILED_BUTTON = 0;
    public static final int BUTTON_NORMAL = 1;
    public static final int BUTTON_3D = 2;

    private ColorTexture mBackGround;
    private SwitchButton mSwitchButtons[];
    private int mLength;
    private int mContentWidth;
    private int mContentHight;
    private int mMeasureWidth;
    private int mMeasureHight;
    private int mFocusButtion;
    private OnClickListener mOnClickListener;
    private boolean mEnable;
    
    public PanoramaSwitchBarView(Context context) {
        mSwitchButtons = new SwitchButton[SWITCH_BUTTON_LENG];
        mEnable = true;
        addSwitchButton(new SwitchButton(BUTTON_NORMAL, context, R.drawable.panorama_pressed,
                R.drawable.panorama_normal, SWITCH_BUTTON_GAP));
        addSwitchButton(new SwitchButton(BUTTON_3D, context, R.drawable.panorama_3d_pressed,
                R.drawable.panorama_3d_normal,SWITCH_BUTTON_GAP));
        adjustButtonsPosition();
    }

    public void addSwitchButton(SwitchButton button) {
        mSwitchButtons[mLength] = button;
        mMeasureWidth = mMeasureWidth + button.getWidth();
        mMeasureHight = Math.max(mMeasureHight, button.getHeight());
        mLength++;
    }

    public void adjustButtonsPosition() {
        int begin = 0;
        mContentWidth = 0;
        for (int i = 0; i < mLength; i++) {
            if (mSwitchButtons[i].mVisible) {
                mContentWidth = mContentWidth + mSwitchButtons[i].getWidth();
            }
        }
        begin = (mMeasureWidth - mContentWidth) / 2;
        for (int i = 0; i < mLength; i++) {
            if (mSwitchButtons[i].mVisible) {
                mSwitchButtons[i].setPosition(begin, 0);
                begin = begin + mSwitchButtons[i].getWidth();
                mContentHight = mSwitchButtons[i].getHeight();
            }
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    @Override
    protected void render(GLCanvas canvas) {
        if(!mEnable) return;
        draw(canvas);
    }

    public void draw(GLCanvas canvas, int x, int y) {
        int begin = (mMeasureWidth-mContentWidth)/2;
        for (int i = 0; i < mLength; i++) {
            mSwitchButtons[i].draw(canvas, x, y);
        }
    }

    public void draw(GLCanvas canvas) {
        draw(canvas, 0, 0);
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredSize(mMeasureWidth, mMeasureHight);
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        Log.d(TAG, "onTouch, x:" + x);
        Log.d(TAG, "onTouch, y:" + y);
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mFocusButtion = pressDown((int) x, (int) y);
            break;
        case MotionEvent.ACTION_UP:
            if (mFocusButtion > INVILED_BUTTON && pressDown((int) x, (int) y) > INVILED_BUTTON) {
                setFocusButton(mFocusButtion, true);
                invalidate();
            }
            mFocusButtion = INVILED_BUTTON;
            break;
        case MotionEvent.ACTION_CANCEL:
            mFocusButtion = INVILED_BUTTON;
            break;
        default:
            break;
        }
        return true;
    }

    public void setFocusButton(int button, boolean fromUser) {
        if (button == INVILED_BUTTON)
            return;

        for (int i = 0; i < mLength; i++) {
            if (mSwitchButtons[i].mName == button) {
                mSwitchButtons[i].setPress(true);
            } else {
                mSwitchButtons[i].setPress(false);
            }
        }

        if (mOnClickListener != null && fromUser)
            mOnClickListener.onClick(this);
    }

    public void setEnable(boolean enable){
        mEnable = enable;
    }
    
    public void setButtonVisible(int button, boolean visible) {
        SwitchButton b = getButton(button);
        if (b != null) {
            b.setVisible(visible);
            adjustButtonsPosition();
        }
    }

    private SwitchButton getButton(int button) {
        for (int i = 0; i < mLength; i++) {
            if (mSwitchButtons[i].mName == button) {
                return mSwitchButtons[i];
            }
        }
        return null;
    }

    public int getFocusButtion() {
        return mFocusButtion;
    }

    private int pressDown(int x, int y) {
        for (int i = 0; i < mLength; i++) {
            if (mSwitchButtons[i].pressed(x, y)) {
                return mSwitchButtons[i].mName;
            }
        }
        return -1;
    }

    private static class SwitchButton {
        public boolean mVisible;
        public boolean mFocus;
        private int mFocusResID;
        private int mNormalResID;
        private int mGap;
        public int mName;
        private Rect mContentRect;
        private Texture mForcusTexture;
        private Texture mNormalTexture;
        private Texture mCurrTexture;

        public SwitchButton(int name, Context context, int focusResID, int normalResID, int gap) {
            this.mName = name;
            this.mFocusResID = focusResID;
            this.mNormalResID = normalResID;
            this.mGap = gap;
            this.mForcusTexture = new ResourceTexture(context, focusResID);
            this.mNormalTexture = new ResourceTexture(context, normalResID);
            this.mVisible = true;
            this.mContentRect = new Rect(0, 0, mForcusTexture.getWidth() + gap * 2, mForcusTexture.getHeight() + gap * 2);
        }

        public void draw(GLCanvas canvas, int x, int y) {
            if (!mVisible)
                return;
            if (mFocus) {
                mCurrTexture = mForcusTexture;
            } else {
                mCurrTexture = mNormalTexture;
            }
            mCurrTexture.draw(canvas, mContentRect.left + mGap + x, mContentRect.top + mGap + y);
        }
        
        public void drawSplit(GLCanvas canvas, Texture texture, int x, int y){
            if(texture == null) return;
            int h = mContentRect.height();
            texture.draw(canvas, mContentRect.right+x, mContentRect.top+y+h/4, 1, h/2);
        }
        
        public boolean isNeedSplit(int left, int right){
            if(mContentRect.right >= right) return false;
            else return true;
        }
        
        public void draw(GLCanvas canvas) {
            draw(canvas, 0, 0);
        }

        public void setVisible(boolean visible) {
            this.mVisible = visible;
        }

        public void setPosition(int x, int y) {
            mContentRect.offsetTo(x, y);
        }

        public int getWidth() {
            return mContentRect.width();
        }

        public int getHeight() {
            return mContentRect.height();
        }

        public boolean pressed(int x, int y) {
            return mContentRect.contains(x, y);
        }

        public void setPress(boolean focus) {
            this.mFocus = focus;
        }
    }
}
