package com.iflytek.mscv5plus;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class MyService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	 
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		final IntentFilter filter = new IntentFilter();  
	       filter.addAction(Intent.ACTION_SCREEN_ON);  
	       filter.addAction(Intent.ACTION_SCREEN_OFF);
	       registerReceiver(mBatInfoReceiver, filter); 
	}
	private final  BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			final String action = intent.getAction();
			if(Intent.ACTION_SCREEN_ON.equals(action))
			{
				Intent it = new Intent(MyService.this,AsrDemo.class);
				 it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(it);
			}
			if(Intent.ACTION_SCREEN_OFF.equals(action))
			{
				if(SpeechApp.context!=null)
				{SpeechApp.context.finish();}
			}
		}
		};
}
