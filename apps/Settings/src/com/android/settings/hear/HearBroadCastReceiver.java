package com.android.settings.hear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class HearBroadCastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent intent1 = new Intent(context,HearService.class);
		SharedPreferences share = context.getSharedPreferences("AID",Activity.MODE_PRIVATE );
		if(share.getBoolean("isOpen", false)){
			context.startService(intent1);				
		}
	}

}
