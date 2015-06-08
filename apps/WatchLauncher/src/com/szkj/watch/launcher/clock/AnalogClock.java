package com.szkj.watch.launcher.clock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.szkj.watch.launcher.R;

public class AnalogClock extends View {
	private Drawable mDial, mHourHand, mMinuteHand, mSecondHand, mPin;
	private int mWidth, mHeight;
	
	private int mHour, mMinute, mSecond;
	
	public AnalogClock(Context context) {
		this(context, null);
	}

	public AnalogClock(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AnalogClock(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnalogClock,
                defStyle, 0);
		final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.AnalogClock_dial:
                    mDial = a.getDrawable(attr);
                    break;
                    
                case R.styleable.AnalogClock_hour:
                	mHourHand = a.getDrawable(attr);
                	break;
                	
                case R.styleable.AnalogClock_minute:
                	mMinuteHand = a.getDrawable(attr);
                	break;
                	
                case R.styleable.AnalogClock_second:
                	mSecondHand = a.getDrawable(attr);
                	break;
                	
                case R.styleable.AnalogClock_pin:
                	mPin = a.getDrawable(attr);
                	break;
            }
        }
        a.recycle();
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		int width = getWidth();
		int height = getHeight();
		mWidth = width;
		mHeight = height;
		
		if (mDial != null) {
			mDial.setBounds(0, 0, width, height);
		}
		if (mHourHand != null) {
			mHourHand.setBounds(0, 0, width, height);
		}
		if (mMinuteHand != null) {
			mMinuteHand.setBounds(0, 0, width, height);
		}
		if (mSecondHand != null) {
			mSecondHand.setBounds(0, 0, width, height);
		}
		if (mPin != null) {
			mPin.setBounds(0, 0, width, height);
		}
	}
	
	public void updateTime(int hour, int minute, int second) {
		mHour = hour;
		mMinute = minute;
		mSecond = second;
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mDial != null) {
			mDial.draw(canvas);
		}
		float minuteAngle = mMinute / 60.0f * 360;
		if (mMinuteHand != null) {
			drawHand(canvas, mMinuteHand, minuteAngle);
		}
		if (mHourHand != null) {
			drawHand(canvas, mHourHand, mHour / 12.0f * 360 + minuteAngle / 12);
		}
		if (mSecondHand != null) {
			drawHand(canvas, mSecondHand, mSecond / 60.0f * 360);
		}
		if (mPin != null) {
			mPin.draw(canvas);
		}
	}
	
	private void drawHand(Canvas canvas, Drawable hand, float angle) {
		canvas.save();
		canvas.rotate(angle, mWidth / 2, mHeight / 2);
		hand.setBounds(0, 0, mWidth, mHeight);
		hand.draw(canvas);
		canvas.restore();
	}
}
