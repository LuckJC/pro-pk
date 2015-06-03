package com.infocomiot.watch.launcher.packageobserver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.infocomiot.watch.launcher.R;
import com.infocomiot.watch.launcher.provider.LauncherConfig;

/**
 * An activity let user choose the new installed app if add to sports page. 
 * @author lihj
 *
 */
public class PackageAddedPromptActivity extends Activity implements OnClickListener {
	private String mPackageName;
	private boolean mAddToSportsGroup = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Uri data = getIntent().getData();
		if (data == null) {
			finish();
			return;
		}
		
		mPackageName = data.getSchemeSpecificPart();
		PackageManager pm = getPackageManager();
		Intent intent = pm.getLaunchIntentForPackage(mPackageName);
		if (intent == null) {
			finish();
			return;
		}
		ComponentName componentName = intent.getComponent();
		ActivityInfo activityInfo;
		try {
			activityInfo = pm.getActivityInfo(componentName, 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			finish();
			return;
		}
		
		setContentView(R.layout.activity_package_added_prompt);
		
		Drawable drawable = activityInfo.loadIcon(pm);
		String name = activityInfo.loadLabel(pm).toString();
		((ImageView)findViewById(R.id.icon)).setImageDrawable(drawable);
		((TextView)findViewById(R.id.name)).setText(name);
		
		findViewById(R.id.ok).setOnClickListener(this);
		findViewById(R.id.cancel).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ok:
			mAddToSportsGroup = true;
			break;
			
		case R.id.cancel:
			mAddToSportsGroup = false;
			break;

		default:
			break;
		}
		
		finish();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ContentValues values = new ContentValues();
		values.put(LauncherConfig.PACKAGE_NAME, mPackageName);
		if (mAddToSportsGroup) {
			values.put(LauncherConfig.PACKAGE_TYPE, LauncherConfig.TYPE_SPORTS);
			getContentResolver().insert(LauncherConfig.WORKSPACE_CONTENT_URI, values);
		} else {
			values.put(LauncherConfig.PACKAGE_TYPE, LauncherConfig.TYPE_COMMON);
			getContentResolver().insert(LauncherConfig.WORKSPACE_CONTENT_URI, values);
		}
	}
}
