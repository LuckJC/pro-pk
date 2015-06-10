package com.example.xuntongwatch;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;

public class MyApplication extends Application {
	public static Handler handler;
	public static SharedPreferences sp;

	@Override
	public void onCreate() {
		sp = getSharedPreferences("collect", MODE_PRIVATE);
		super.onCreate();
	}
}
