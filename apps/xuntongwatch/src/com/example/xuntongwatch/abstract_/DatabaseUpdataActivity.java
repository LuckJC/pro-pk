package com.example.xuntongwatch.abstract_;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.example.xuntongwatch.contentobserver.ObserverUtil;
import com.example.xuntongwatch.contentobserver.ObserverUtil.ObserverUtilInterface;

public abstract class DatabaseUpdataActivity extends Activity{

	private ObserverUtil observerUtil;
	private Handler handler;
	private final int UPDATA_WHAT = 192;
	@Override
	protected void onDestroy() {
		if(observerUtil != null)
		{
			observerUtil.unRegisterDabaseObserver();
		}
		super.onDestroy();
	}

	/**
	 * 初始化
	 * @param uri
	 */
	@SuppressLint({ "NewApi", "HandlerLeak" }) 
	public void handleObserverUtil(Uri uri)
	{
		handler = new Handler()
		{
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == UPDATA_WHAT)
				{
					update();
				}
				super.handleMessage(msg);
			}
			
		};
		observerUtil = new ObserverUtil(this,uri , face);
		observerUtil.registerDabaseObserver();
	}
	
	private ObserverUtilInterface face = new ObserverUtilInterface() {
		@Override
		public void hasUpdate() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					operation();
					Message msg = Message.obtain();
					msg.what = UPDATA_WHAT;
					handler.sendMessage(msg);
				}
			}).start();
		}
	};
	
	/**
	 * 进行数据更新处理
	 */
	public abstract void operation();
	
	/**
	 * 进行界面更新处理
	 */
	public abstract void update();
	
}
