package com.example.hear_aid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class HearBroadCastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences share = context.getSharedPreferences("status", Activity.MODE_PRIVATE);
		Editor edit = share.edit();
		boolean isOpen = share.getBoolean("isOpen", false);
		if (intent != null) {
			String action = intent.getAction();
			Intent intentService = new Intent(context, HearService.class);
			if (action.equals("android.intent.action.BOOT_COMPLETED")) {
				if (isOpen) {
					GlobalApplication.isOpen = true;
					edit.putBoolean("isOpen", true);
					intentService.putExtra("MSG", MainActivity.MSG_START);
					Log.e("HearBroadCastReceiver", MainActivity.MSG_START);
					context.startService(intentService);
				}
			} else if (action.equals("com.shizhongkeji.action.GESTURE.HEAR_AID_ENABLE_SWITCH")) {
				Log.d("HearBroadCastReceiver", "isOpen:" + isOpen);
				if (GlobalApplication.isOpenFirst) {
					GlobalApplication.isOpen = false;
					context.stopService(intentService);
					edit.putBoolean("isOpen", false);
				} else {
					GlobalApplication.isOpen = true;
					edit.putBoolean("isOpen", true);
					intentService.putExtra("MSG", MainActivity.MSG_START);
					Log.e("HearBroadCastReceiver", MainActivity.MSG_START);
					context.startService(intentService);
				}
			}
		}
		edit.commit();
	}

}
