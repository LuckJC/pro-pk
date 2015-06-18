package com.shizhongkeji;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.utils.MediaUtil;

import android.app.Application;

public class GlobalApplication extends Application {
	public static HashMap<Integer, Boolean> isSelecte;
	public static List<Mp3Info> mp3Infos;
	public static boolean isPlaying = false;
	public static int current = 0;
	public static boolean isPlay = false;
	public static boolean isAutoPause = false;

	@Override
	public void onCreate() {
		super.onCreate();

		mp3Infos = new ArrayList<Mp3Info>();
	}
}
