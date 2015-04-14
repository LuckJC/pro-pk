package com.infocomiot.watch.launcher.clock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.infocomiot.watch.launcher.R;

public class AnalogClockFragment6 extends AnalogClockFragment {
	private static final Bundle sBundle = new Bundle();
	static {
		sBundle.putInt(BATTERY_CHARGING_ICON, R.drawable.battery06_charging);
		sBundle.putInt(BATTERY_LEVEL_ICON, R.drawable.battery06);
		sBundle.putInt(TIME_UPDATE_PERIOD, MILLIS_PERIOD_SECOND);
	}
	
	public AnalogClockFragment6() {
		super();
		setArguments(sBundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.clock06, null);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);
		mAnalogClock = (AnalogClock)view.findViewById(R.id.clock);
		mDateTV = (TextView)view.findViewById(R.id.date);
		TextView smsView = (TextView)view.findViewById(R.id.sms);
		TextView callView = (TextView)view.findViewById(R.id.call);
		ImageView batteryView = (ImageView)view.findViewById(R.id.battery);
		setCommonView(batteryView, smsView, smsView, callView, callView);
		return view ;
	}
}
