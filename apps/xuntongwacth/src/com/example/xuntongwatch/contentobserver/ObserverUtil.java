package com.example.xuntongwatch.contentobserver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

public class ObserverUtil {

	private ObserverUtilInterface face;
	private DataBaseContentObserver observer;
	private Uri uri;
	private Context context;
	private Handler handler;
	private final int WHAT = 100;
	
	@SuppressLint("HandlerLeak") 
	public ObserverUtil(Context context,Uri uri,ObserverUtilInterface face)
	{
		this.uri = uri;
		this.context = context;
		this.face = face;
		handler = new Handler()
		{
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == WHAT)
				{
					ObserverUtil.this.face.hasUpdate();
				}
				super.handleMessage(msg);
			}
			
		};
		observer = new DataBaseContentObserver(handler, WHAT);
	}
	
	public void registerDabaseObserver()
	{
		context.getContentResolver().registerContentObserver(uri, true, observer);
	}
	
	public void unRegisterDabaseObserver()
	{
		if(observer != null)
		{
			context.getContentResolver().unregisterContentObserver(observer);
		}
	}
	
	public interface ObserverUtilInterface
	{
		public void hasUpdate();
	}
	
}
