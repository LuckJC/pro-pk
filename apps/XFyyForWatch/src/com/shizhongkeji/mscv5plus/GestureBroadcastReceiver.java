package com.shizhongkeji.mscv5plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.sax.StartElementListener;

public class GestureBroadcastReceiver extends BroadcastReceiver {

	public static final String GESTURE_START_VOICE ="com.shizhongkeji.action.GESTURE.START_VOICE_ASSIST";
	public static final String GESTURE_STOP_VOICE ="com.shizhongkeji.action.GESTURE.STOP_VOICE_ASSIST";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		
		if(intent.getAction().equals(GestureBroadcastReceiver.GESTURE_START_VOICE))
		{
			Intent it = new Intent(context,Asr_service.class);
			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(it);
		}
		if(intent.getAction().equals(GestureBroadcastReceiver.GESTURE_STOP_VOICE))
		{
			Intent it = new Intent(context,Asr_service.class);
			context.stopService(it);
		}
	}

}
