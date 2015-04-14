package com.infocomiot.watch.launcher.packageobserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.infocomiot.watch.launcher.provider.LauncherConfig;

public class PackageChangedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
		Log.d("PackageChangedReceiver", "action:" + action + ", replacing:" + replacing);
		if (! (action.equals(Intent.ACTION_PACKAGE_ADDED)
					|| action.equals(Intent.ACTION_PACKAGE_REMOVED))
				|| replacing) {
			//Only care for package added or removed, and not replacing.
			return;
		}
		
		Uri data = intent.getData();
		String packageName = data.getSchemeSpecificPart();
		if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
			//A package has been removed, so remove it from database.
			context.getContentResolver().delete(LauncherConfig.WORKSPACE_CONTENT_URI, 
					LauncherConfig.PACKAGE_NAME + "=?", 
					new String[] { packageName });
		} else {
			//Start an activity to choose if add to sport page.
			Intent intent2 = new Intent(context, PackageAddedPromptActivity.class);
			intent2.setData(data);
			intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent2);
		}
	}

}
