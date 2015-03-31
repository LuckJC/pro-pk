package com.mediatek.gallery3d.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class RelativeLayoutIgnoreTouch extends RelativeLayout {
    private static final String TAG = "ConvergenceBar";

    public RelativeLayoutIgnoreTouch(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public RelativeLayoutIgnoreTouch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeLayoutIgnoreTouch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //Log.d(TAG, "onTouchEvent: " + e.getAction());
        return true;
    }
    
}
