package com.shizhongkeji;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.utils.MediaUtil;

import android.app.Application;

public class GlobalApplication extends Application {
	public static HashMap<Integer, Boolean> isSelecte;
	public static List<Mp3Info> mp3InfosSystem;
	public static List<Mp3Info> mp3Infos;
	public static boolean isPlaying = false;
	public static int current = 0;
	@Override
	public void onCreate() {
		super.onCreate();
		mp3InfosSystem = MediaUtil.getMp3Infos(this); // 获取手机中所得mp3文件集合
		mp3Infos = new ArrayList<Mp3Info>();
	}
}
