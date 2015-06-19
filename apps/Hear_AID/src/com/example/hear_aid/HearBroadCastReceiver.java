package com.example.hear_aid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class HearBroadCastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences share = context.getSharedPreferences("status", Activity.MODE_PRIVATE);
		boolean isOpen = share.getBoolean("isOpen", false);
		if (intent != null) {
			String action = intent.getAction();
			if (action.equals("android.intent.action.BOOT_COMPLETED")) {
				if (isOpen) {
					Intent intent1 = new Intent(context, HearService.class);
					intent.putExtra("MSG", MainActivity.MSG_START);
					context.startService(intent1);
				}
			} else if (action.equals("com.shizhongkeji.action.GESTURE.HEAR_AID_ENABLE_SWITCH")) {
				Log.d("HearBroadCastReceiver", "isOpen:" + isOpen);
				Intent intent1 = new Intent(context, HearService.class);
				if (isOpen) {
					context.stopService(intent1);
				} else {
					intent.putExtra("MSG", MainActivity.MSG_START);
					context.startService(intent1);
				}
			}
		}
	}

}
