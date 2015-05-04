package com.example.hear_aid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class HearBroadCastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences share = context.getSharedPreferences("AID", Activity.MODE_PRIVATE);
		boolean isOpen = share.getBoolean("isOpen", false);
		if (intent != null) {
			String action = intent.getAction();
			if (action.equals("android.intent.action.BOOT_COMPLETED")) {
				Intent intent1 = new Intent(context, HearService.class);
				if (isOpen) {
					context.startService(intent1);
				}
			} else if (action.equals("com.shizhongkeji.action.GESTURE.HEAR_AID_ENABLE_SWITCH")) {
				if (isOpen) {
					context.stopService(intent);
				} else {
					context.startService(intent);
				}
			}
		}
	}

}
