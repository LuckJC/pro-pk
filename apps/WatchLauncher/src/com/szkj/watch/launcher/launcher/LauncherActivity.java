package com.szkj.watch.launcher.launcher;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.szkj.watch.launcher.R;
import com.szkj.watch.launcher.launcher.MyApplication.PackageChangedCallback;
import com.szkj.watch.launcher.provider.LauncherConfig;

public class LauncherActivity extends Activity {
	private static final String TAG = "LauncherActivity";
	
	private static final String[] PROJECTION = {
		LauncherConfig._ID,
		LauncherConfig.PACKAGE_NAME,
		LauncherConfig.PACKAGE_TYPE
	};
	private static final int CI_PACKAGE_NAME = 1;
	private static final int CI_PACKAGE_TYPE = 2;
	
	private static List<String> sAppsCommon;
	private static List<String> sAppsSports;
	private static List<String> sAppsCommunications;
	private static List<String> sAppsSettings;
	
	private int mType;
	private ViewPager mViewPager;
	private boolean mFirstStart = true;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		mType = intent.getIntExtra(LauncherConfig.EXTRA_WORKSPACE_TYPE, LauncherConfig.TYPE_SETTINGS);
		
		setContentView(R.layout.activity_launcher);
		mViewPager = (ViewPager) findViewById(R.id.viewPager);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		boolean needInit = checkAndInitApps();
		if (mFirstStart || needInit) {
			mViewPager.setAdapter(new WorkspaceAdapter(this, getApps(mType)));
		}
		mFirstStart = false;
		
		MyApplication.getApp().registerCallback(mCallback);
	}
	
	private PackageChangedCallback mCallback = new PackageChangedCallback() {
		@Override
		public void onChange() {
			boolean needInit = checkAndInitApps();
			if (needInit) {
				mViewPager.setAdapter(new WorkspaceAdapter(LauncherActivity.this, getApps(mType)));
			}
		}
	};
	
	@Override
	protected void onPause() {
		super.onPause();
		MyApplication.getApp().unregisterCallback(mCallback);
	}
	
	//check and add apps with launcher activity
	private boolean checkAndInitApps() {
		boolean needInit = false;
		boolean dbChanged = MyApplication.getApp().isDbChanged();
		if (sAppsCommon == null) {
			needInit = true;
			sAppsCommon = new ArrayList<String>();
			sAppsCommunications = new ArrayList<String>();
			sAppsSports = new ArrayList<String>();
			sAppsSettings = new ArrayList<String>();
		} else if (dbChanged) {			
			needInit = true;
			sAppsCommon.clear();
			sAppsCommunications.clear();
			sAppsSports.clear();
			sAppsSettings.clear();
		}
		
		if (needInit) {			
			Cursor cursor = getContentResolver().query(LauncherConfig.WORKSPACE_CONTENT_URI, 
					PROJECTION, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				do {
					String packageName = cursor.getString(CI_PACKAGE_NAME);
					if (isAppExistsAndHasLauncher(packageName)) {
						int type = cursor.getInt(CI_PACKAGE_TYPE);
						getApps(type).add(packageName);
					}
				} while (cursor.moveToNext());
			}
			if (cursor != null) cursor.close();
			
			if (dbChanged) {
				MyApplication.getApp().setDbChanged(false);
			}
		}
		
		return needInit;
	}
	
	private boolean isAppExistsAndHasLauncher(String packageName) {
		if (getPackageManager().getLaunchIntentForPackage(packageName) != null) {
			return true;
		} else {
			return false;
		}
	}
	
	private List<String> getApps(int type) {
		switch (type) {
		case LauncherConfig.TYPE_SPORTS:
			return sAppsSports;
			
		case LauncherConfig.TYPE_COMMUNICATIONS:
			return sAppsCommunications;
			
		case LauncherConfig.TYPE_SETTINGS:
			return sAppsSettings;
			
		case LauncherConfig.TYPE_COMMON: 
		default:
			return sAppsCommon;
		}
	}
	
	private void logd(Object msg) {
		Log.d(TAG, String.valueOf(msg));
	}
}
