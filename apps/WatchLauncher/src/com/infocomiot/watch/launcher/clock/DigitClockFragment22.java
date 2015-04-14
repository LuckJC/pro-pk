package com.infocomiot.watch.launcher.clock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.infocomiot.watch.launcher.R;

public class DigitClockFragment22 extends DigitClockFragment {
	private static final Bundle sBundle = new Bundle();
	static {
		sBundle.putInt(BATTERY_CHARGING_ICON, R.drawable.battery22_charging);
		sBundle.putInt(BATTERY_LEVEL_ICON, R.drawable.battery22);
		sBundle.putInt(TIME_UPDATE_PERIOD, MILLIS_PERIOD_MINUTE);
	}
	
	public DigitClockFragment22() {
		super();
		setArguments(sBundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.clock22, null);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);
		mDateTV = (TextView)view.findViewById(R.id.date);
		mTimeTV = (TextView)view.findViewById(R.id.time);
		TextView smsView = (TextView)view.findViewById(R.id.sms);
		TextView callView = (TextView)view.findViewById(R.id.call);
		ImageView batteryView = (ImageView)view.findViewById(R.id.battery);
		setCommonView(batteryView, smsView, smsView, callView, callView);
		setContactsView(view.findViewById(R.id.contacts));
		return view ;
	}
}
