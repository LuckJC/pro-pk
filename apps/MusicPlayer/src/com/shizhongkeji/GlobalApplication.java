package com.shizhongkeji;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Application;

import com.shizhongkeji.info.Mp3Info;

public class GlobalApplication extends Application {
	public static HashMap<Integer, Boolean> isSelecte;
	public static List<Mp3Info> mp3Infos;
	public static boolean isPlaying = false;  //是否正在播放歌曲
	public static int current = 0; 
	public static boolean isPlay = false;   // 进入播放器，是否播放过歌曲
	public static boolean isAutoPause = false; // 是否自动暂停播放过

	@Override
	public void onCreate() {
		super.onCreate();

		mp3Infos = new ArrayList<Mp3Info>();
	}
}
