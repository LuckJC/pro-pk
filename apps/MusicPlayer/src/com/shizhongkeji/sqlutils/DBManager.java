package com.shizhongkeji.sqlutils;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.shizhongkeji.info.AppConstant;
import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.utils.MediaUtil;

public class DBManager {

	private static DBManager mDBManager;
	private static MusicSQLiteOpenHelper sqlHleper;
	private static SQLiteDatabase dataBase;
	public static DBManager getInstance(Context context){
		if(mDBManager == null){
			mDBManager = new DBManager(context);
		}
		return mDBManager;
	}
	private DBManager(Context context){
		sqlHleper = new MusicSQLiteOpenHelper(context);
		dataBase = sqlHleper.getWritableDatabase();
	}
	public void insertMusic(List<Mp3Info> mp3Infos){
		for (int i = 0; i < mp3Infos.size(); i++) {
			Mp3Info mp3Info = mp3Infos.get(i);
			if(!isExitMusicForID(String.valueOf(mp3Info.getId()))){
				ContentValues values = new ContentValues();
				values.put(AppConstant.MusicSQL.ID, mp3Info.getId());
				values.put(AppConstant.MusicSQL.TITLE, mp3Info.getTitle());
				values.put(AppConstant.MusicSQL.ALBUM, mp3Info.getAlbum());
				values.put(AppConstant.MusicSQL.ALBUMID, mp3Info.getAlbumId());
				values.put(AppConstant.MusicSQL.DISPLAYNAME, mp3Info.getDisplayName());
				if(mp3Info.getArtist().equals(MediaUtil.UNKNOWN)){
					values.put(AppConstant.MusicSQL.ARTIST,MediaUtil.UNKNOWN_CHINA);
				}else{
					values.put(AppConstant.MusicSQL.ARTIST, mp3Info.getArtist());	
				}
				values.put(AppConstant.MusicSQL.DURATION, mp3Info.getDuration());
				values.put(AppConstant.MusicSQL.SIZE, mp3Info.getSize());
				values.put(AppConstant.MusicSQL.URL, mp3Info.getUrl());
				values.put(AppConstant.MusicSQL.LRCTITLE, mp3Info.getLrcTitle());
				values.put(AppConstant.MusicSQL.LRCSIZE, mp3Info.getLrcSize());
				dataBase.insert(AppConstant.MusicSQL.TABLE_NAME, null, values);
			}
		}
	}
	public List<Mp3Info> queryMusic(){
		List<Mp3Info> mp3Infos = new ArrayList<Mp3Info>(); 
		Cursor cursor =  dataBase.query(AppConstant.MusicSQL.TABLE_NAME, null, null, null, null, null, null);
		while (cursor.moveToNext()) {
			Long id = Long.parseLong(cursor.getString(0)); 
			String title = cursor.getString(1);
			String album = cursor.getString(2);
			Long albumId = Long.parseLong(cursor.getString(3));
			String displayName = cursor.getString(4);
			String artist =cursor.getString(5);
			Long duration = Long.parseLong(cursor.getString(6));
			Long size = Long.parseLong(cursor.getString(7));
			String url =cursor.getString(8);
			String lrcTitle =cursor.getString(9);
			String lrcSize = cursor.getString(10);
			Mp3Info mp3Info = new Mp3Info(id, title, album, albumId, displayName, artist, 
					duration, size, url, lrcTitle, lrcSize);
			mp3Infos.add(mp3Info);
		}
		return mp3Infos;
		
	}
	public boolean isExitMusicForID(String id){
		boolean b = false;
		Cursor cursor  = dataBase.query(AppConstant.MusicSQL.TABLE_NAME,null,  AppConstant.MusicSQL.ID +"= ?", new String[]{id}, null, null, null);
		b = cursor.moveToFirst();
		cursor.close();
		return b;
		
	}

	public void deleteMusic(String id){
		dataBase.delete(AppConstant.MusicSQL.TABLE_NAME,AppConstant.MusicSQL.ID +"= ?", new String[]{id});
	}
}
