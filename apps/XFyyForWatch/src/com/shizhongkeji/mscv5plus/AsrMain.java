package com.shizhongkeji.mscv5plus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.shizhongkeji.mscv5plus.Asr_service.MyBinder;
import com.shizhongkeji.speech.util.ProgressDialogUtils;

public class AsrMain extends Activity {
	MyBinder mybinder;
	Asr_service shs;
	Handler hand = new Handler()
	{
		
		/** {@inheritDoc} */
		 
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if(msg.what == 110)
			{
				
				if(shs.speak_state == -1)//-1是录音结束
				{mic.setVisibility(View.GONE);}
				else if(shs.speak_state == 0)//0是开始录音
				{mic.setVisibility(View.VISIBLE);}
				else// 1是录音中
				{
					mic.setBackgroundResource(R.drawable.mic_1);
					if (shs.VOLUME <= 6) {
						mic.setBackgroundResource(R.drawable.mic_1);
					} else if (shs.VOLUME > 6 && shs.VOLUME <= 12) {
						mic.setBackgroundResource(R.drawable.mic_2);
					} else if (shs.VOLUME > 12 && shs.VOLUME <= 18) {
						mic.setBackgroundResource(R.drawable.mic_3);
					} else if (shs.VOLUME > 18 && shs.VOLUME <= 24) {
						mic.setBackgroundResource(R.drawable.mic_4);
					} else {
						mic.setBackgroundResource(R.drawable.mic_5);
					}
				}
				
				hand.sendEmptyMessageDelayed(110, 40);
			}
			if(msg.what == 111)
			{
				if(!shs.TTS_load_success)
				{
					ProgressDialogUtils.showProgressDialog(AsrMain.this, "中文合成引擎加载中");
					hand.sendEmptyMessageDelayed(111,100);
				}
				else
				{ProgressDialogUtils.dismissProgressDialog();hand.removeMessages(111);}
			}
			if(msg.what == 112)
			{
				
					if(shs.is_service_have_exit)
					{
						AsrMain.this.finish();
						hand.removeMessages(112);
						System.gc();
					}
					else{
					hand.sendEmptyMessageDelayed(112,50);
					}
			}
		}
		
	};
	
	ImageView mic;
	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.asr_main);
		mic = (ImageView)findViewById(R.id.mic);
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	/** {@inheritDoc} */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Intent it = new Intent(this, Asr_service.class);
		it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		startService(it);
		bindService(it, connection, BIND_AUTO_CREATE);
		
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.d("lxd","已经执行跳转:"+System.currentTimeMillis());
		Intent it = new Intent(this, Asr_service.class);
//		stopService(it);
		unbindService(connection);
	}
 
	// 创建一个 ServiceConnection 对象  
    final ServiceConnection connection = new ServiceConnection() { 
    	
    	
    	@Override
        public void onServiceDisconnected(ComponentName name) {  
            // TODO Auto-generated method stub  
    		Log.i(">>>>>>>>", "onServiceDisconnected(ComponentName name)");
        }  
  
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			mybinder = (MyBinder)service;
			shs = mybinder.getservice();
			
			hand.sendEmptyMessage(110);
			
			hand.sendEmptyMessage(111);
			
			hand.sendEmptyMessage(112);
		}  
    }; 
}
