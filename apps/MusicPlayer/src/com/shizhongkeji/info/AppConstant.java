package com.shizhongkeji.info;

/**
 * 应用常量类
 * 
 *
 */
public class AppConstant {
	public static final int TYPE_GESTURE = 1;
	public static final int TYPE_OTHER = 0;

	public class PlayOrder {
		public static final int REPEAT_ONE = 1;
		public static final int REPEAT_ALL = 2;
		public static final int ORDER = 3;
		public static final int RANDOM = 4;
	}

	public class PlayerMsg {
		public static final int PLAY_MSG = 1; // 播放
		public static final int PAUSE_MSG = 2; // 暂停
		public static final int STOP_MSG = 3; // 停止
		public static final int CONTINUE_MSG = 4; // 继续
		public static final int PRIVIOUS_MSG = 5; // 上一首
		public static final int NEXT_MSG = 6; // 下一首
		public static final int PROGRESS_CHANGE = 7;// 进度改变
		public static final int PLAYING_MSG = 8; // 正在播放
		public static final int PLAYING_DELETE = 9; // 正在播放删除列表歌曲
		public static final int ADD_MUSIC = 10; // 增加播放列表中的歌曲
	}

	public class MusicSQL {
		public static final String ID = "_id"; // 歌曲ID 3
		public static final String TITLE = "title"; // 歌曲名称 0
		public static final String ALBUM = "album"; // 专辑 7
		public static final String ALBUMID = "albumId";// 专辑ID 6
		public static final String DISPLAYNAME = "displayName"; // 显示名称 4
		public static final String ARTIST = "artist"; // 歌手名称 2
		public static final String DURATION = "duration"; // 歌曲时长 1
		public static final String SIZE = "size"; // 歌曲大小 8
		public static final String URL = "url"; // 歌曲路径 5
		public static final String LRCTITLE = "lrcTitle"; // 歌词名称
		public static final String LRCSIZE = "lrcSize"; // 歌词大小
		public static final String TABLE_NAME = "mp3Infos";
		public static final String SQL_NAME = "mp3.db";
		public static final String SQL = "create table " + TABLE_NAME + "(" + ID + " varchar,"
				+ TITLE + " varchar," + ALBUM + " varchar," + ALBUMID + " varchar," + DISPLAYNAME
				+ " varchar," + ARTIST + " varchar," + DURATION + " varchar ," + SIZE + " varchar,"
				+ URL + " varchar," + LRCTITLE + " varchar," + LRCSIZE + " varchar" + ")";
	}
}
