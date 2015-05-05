package com.infocomiot.watch.launcher.provider;

import android.net.Uri;
import android.provider.BaseColumns;


public class LauncherConfig implements BaseColumns {
	public static final String EXTRA_WORKSPACE_TYPE = "workspace_type";
	public static final int TYPE_COMMON = 1;
	public static final int TYPE_SPORTS = 2;
	public static final int TYPE_COMMUNICATIONS = 3;
	public static final int TYPE_SETTINGS = 4;
	
	static final String[] APPS_COMMON = {
		"com.shizhongkeji.mscv5plus",  //语音
		"com.example.hear_aid",  //助听器
		//"com.android.gallery3d",  //相机,照片
		//"com.android.music",  //音乐
		"com.example.musicplayer", //音乐
		//"com.android.soundrecorder",  //录音机
		//"com.mediatek.FMRadio",  //收音机
		"com.ss.android.article.news",  //头条新闻
	};
	
	static final String[] APPS_SPORTS = {
		"com.example.calendar",  //日历
		//"com.android.deskclock",  //时钟
		"com.codoon.gps",  //咕咚运动
		"com.baidu.BaiduMap",  //百度地图
		"com.nike.plusgpschina",  //Nike+计步器
		"com.tencent.mm",  //微信
		"com.tencent.mobileqq",  //QQ
	};
	
	static final String[] APPS_COMMUNICATIONS = {
		"com.android.contacts",  //联系人
		"com.android.dialer",  //拨号盘
		"com.android.mms",  //短信
	};
	
	static final String[] APPS_SETTINGS = {
		"com.android.settings",  //日历
	};
	
	static final String AUTORITY = "com.infocomiot.watch.launcher";
	/**
	 * Launcher provider uri.
	 */
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTORITY);
	
	/**
	 * Launcher workspace provider uri.
	 */
	public static final Uri WORKSPACE_CONTENT_URI = Uri.parse("content://" + AUTORITY + "/workspace");
	
	/**
	 * Provider column: package name
	 */
	public static final String PACKAGE_NAME = "packageName";
	
	/**
	 * Provider column: package type
	 */
	public static final String PACKAGE_TYPE = "type";
}
