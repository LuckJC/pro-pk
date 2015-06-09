package com.shizhongkeji.service;

import com.shizhongkeji.info.AppConstant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MusicBroadCastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Intent intent = new Intent(arg0, PlayerService.class);
		intent.putExtra("action", arg1.getAction());
		intent.putExtra("type", AppConstant.TYPE_GESTURE);
		arg0.startService(intent);
	}
}
