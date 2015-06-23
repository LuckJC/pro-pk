package com.example.hear_aid;

import android.app.Application;

public class GlobalApplication extends Application {
   
	public static boolean isOpenFirst = false;
	public static boolean isOpenSecond = false;
	@Override
	public void onCreate() {
		super.onCreate();
	}
}
