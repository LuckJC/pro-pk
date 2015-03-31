package com.example.xuntongwatch.contentobserver;

import android.annotation.SuppressLint;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

public class DataBaseContentObserver extends ContentObserver{
	
	private Handler handler;
	private int what;

	public DataBaseContentObserver(Handler handler) {
		super(handler);
	}
	
	public DataBaseContentObserver(Handler handler,int what) {
		super(handler);
		this.handler = handler;
		this.what = what;
	}

	@Override
	public boolean deliverSelfNotifications() {
		return super.deliverSelfNotifications();
	}

	@SuppressLint("NewApi") @Override
	public void onChange(boolean selfChange, Uri uri) {
		super.onChange(selfChange, uri);
	}

	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Message msg = Message.obtain();
		msg.what = this.what;
		handler.sendMessage(msg);
	}

	
	
}
