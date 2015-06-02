package com.android.watch.recorder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class RecActionReceiver extends BroadcastReceiver {
	private static final String REC_ACTION = "com.shizhongkeji.action.GESTURE.REC";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(REC_ACTION)) {
			Intent serviceIntent = new Intent(context, RecService.class);
			serviceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			context.startService(serviceIntent);
		}
	}

}
