package com.shizhongkeji.mscv5plus;


import java.text.SimpleDateFormat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.sax.StartElementListener;
import android.util.Log;

public class GestureBroadcastReceiver extends BroadcastReceiver {

	public static final String GESTURE_START_VOICE ="com.shizhongkeji.action.GESTURE.START_VOICE_ASSIST";
	public static final String GESTURE_STOP_VOICE ="com.shizhongkeji.action.GESTURE.STOP_VOICE_ASSIST";
	
	SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
	String s = date.format(System.currentTimeMillis());
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		
		if(intent.getAction().equals(GestureBroadcastReceiver.GESTURE_START_VOICE))
		{
			Log.d("lxd","收到广播 "+s);
			Intent it = new Intent(context,Asr_service.class);
			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(it);
			Log.d("lxd","已经启动 "+s);
//			Intent it = new Intent(context,Asr_service.class);
//			context.bindService(it, connection, context.BIND_AUTO_CREATE);
		}
		if(intent.getAction().equals(GestureBroadcastReceiver.GESTURE_STOP_VOICE))
		{
			Intent it = new Intent(context,Asr_service.class);
			context.stopService(it);
//			Intent it = new Intent(context,Asr_service.class);
//			context.unbindService(connection);
		}
		
	}
	 
	final ServiceConnection connection = new ServiceConnection() { 
    	com.shizhongkeji.mscv5plus.Asr_service.MyBinder mybinder;
    	Asr_service shs;
    	@Override
        public void onServiceDisconnected(ComponentName name) {  
            // TODO Auto-generated method stub  
    		Log.i(">>>>>>>>", "onServiceDisconnected(ComponentName name)");
    		shs.destoryListen();
        }  
  
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			
			mybinder = (com.shizhongkeji.mscv5plus.Asr_service.MyBinder)service;
			shs = mybinder.getservice();
			shs.grammar();
			Log.i(">>>>>>>>", "onServiceConnected(ComponentName name, IBinder service)");
		}  
    }; 
}
