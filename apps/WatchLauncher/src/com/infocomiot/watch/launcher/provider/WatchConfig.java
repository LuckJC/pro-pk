package com.infocomiot.watch.launcher.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class WatchConfig implements BaseColumns {
	public static final Uri WATCH_CONTENT_URI = 
			Uri.parse("content://" + LauncherConfig.AUTORITY + "/watch");
	
	public static final String CURRENT_STYLE = "current_style";
}
