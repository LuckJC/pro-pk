package com.android.deskclock;

import android.animation.ObjectAnimator;
import android.app.ActionBar.LayoutParams;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.widget.TimePicker;
import com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener;

import android.widget.Button;

public class myTimePicker extends DialogFragment {
	
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_HOUR_OF_DAY = "hour_of_day";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_IS_24_HOUR_VIEW = "is_24_hour_view";

    private int mInitialHourOfDay;
    private int mInitialMinute;
    private boolean mIs24HourMode;
    private boolean mThemeDark;
	
    private OnTimeSetListener mCallback;
    private TimePicker mTimePicker;

    public myTimePicker() {
        // Empty constructor required for dialog fragment.
    }

    public myTimePicker(Context context, int theme, OnTimeSetListener callback,
            int hourOfDay, int minute, boolean is24HourMode) {
        // Empty constructor required for dialog fragment.
    }

    public static myTimePicker newInstance(OnTimeSetListener callback,
            int hourOfDay, int minute, boolean is24HourMode) {
        myTimePicker ret = new myTimePicker();
        ret.initialize(callback, hourOfDay, minute, is24HourMode);
        return ret;
    }

    public void initialize(OnTimeSetListener callback,
            int hourOfDay, int minute, boolean is24HourMode) {
        mCallback = callback;

        mInitialHourOfDay = hourOfDay;
        mInitialMinute = minute;
        mIs24HourMode = is24HourMode;
        mThemeDark = false;
    }


    /**
     * Set a dark or light theme. NOTE: this will only take effect for the next onCreateView.
     */
    public void setThemeDark(boolean dark) {
        mThemeDark = dark;
    }

    public boolean isThemeDark() {
        return mThemeDark;
    }
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_HOUR_OF_DAY)
                    && savedInstanceState.containsKey(KEY_MINUTE)
                    && savedInstanceState.containsKey(KEY_IS_24_HOUR_VIEW)) {
            mInitialHourOfDay = savedInstanceState.getInt(KEY_HOUR_OF_DAY);
            mInitialMinute = savedInstanceState.getInt(KEY_MINUTE);
            mIs24HourMode = savedInstanceState.getBoolean(KEY_IS_24_HOUR_VIEW);
			mThemeDark = savedInstanceState.getBoolean(KEY_DARK_THEME);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mTimePicker != null) {
            outState.putInt(KEY_HOUR_OF_DAY, mTimePicker.getCurrentHour());
            outState.putInt(KEY_MINUTE, mTimePicker.getCurrentMinute());
            outState.putBoolean(KEY_IS_24_HOUR_VIEW, mIs24HourMode);

        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        View view = inflater.inflate(R.layout.time_picker, null);
        view.setBackgroundColor(0x7f000000);

              mTimePicker = (TimePicker)view.findViewById(R.id.timePicker1);
		mTimePicker.setCurrentHour(mInitialHourOfDay);
		mTimePicker.setCurrentMinute(mInitialMinute);
		mTimePicker.setIs24HourView(mIs24HourMode);

    		Button bt_ok = (Button)view.findViewById(R.id.picker_ok);
		bt_ok.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				if(mCallback != null)
				{
				mCallback.onTimeSet(null,
                            mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
   				///mCallback.onTimeSet(null,0, 0);                        
					}
				
				dismiss();
				
			}
		});


    		Button bt_cancel = (Button)view.findViewById(R.id.picker_cancel);
		bt_cancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				dismiss();
			}
		});


	  return view;
    	}

}
