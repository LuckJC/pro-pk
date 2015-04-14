package com.infocomiot.watch.launcher.clock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.infocomiot.watch.launcher.R;

public class AnalogClockFragment4 extends AnalogClockFragment {
	private static final Bundle sBundle = new Bundle();
	private TextView mSmsView;
	private TextView mCallView;
	static {
		sBundle.putInt(BATTERY_CHARGING_ICON, R.drawable.battery04_charging);
		sBundle.putInt(BATTERY_LEVEL_ICON, R.drawable.battery04);
		sBundle.putInt(TIME_UPDATE_PERIOD, MILLIS_PERIOD_MINUTE);
	}
	
	public AnalogClockFragment4() {
		super();
		setArguments(sBundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.clock04, null);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);
		mAnalogClock = (AnalogClock)view.findViewById(R.id.clock);
		mDateTV = (TextView)view.findViewById(R.id.date);
		mSmsView = (TextView)view.findViewById(R.id.sms);
		mCallView = (TextView)view.findViewById(R.id.call);
		ImageView batteryView = (ImageView)view.findViewById(R.id.battery);
		setCommonView(batteryView, mSmsView, mSmsView, mCallView, mCallView);
		return view ;
	}
	
	@Override
	protected int updateNewSms() {
		int newSms = super.updateNewSms();
		mSmsView.setBackgroundResource(newSms > 0 ? R.drawable.sms04_n : R.drawable.sms04);
		return newSms;
	}
	
	@Override
	protected int updateMissedCall() {
		int newMissedCall = super.updateMissedCall();
		mCallView.setBackgroundResource(newMissedCall > 0 ? R.drawable.call04_n : R.drawable.call04);
		return newMissedCall;
	}
}
