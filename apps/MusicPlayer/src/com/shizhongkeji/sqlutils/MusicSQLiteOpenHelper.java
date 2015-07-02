package com.shizhongkeji.sqlutils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.shizhongkeji.info.AppConstant;

public class MusicSQLiteOpenHelper extends SQLiteOpenHelper {
	public static final int VERSION = 1;
	
	public MusicSQLiteOpenHelper(Context context) {
		super(context, AppConstant.MusicSQL.SQL_NAME, null, VERSION);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(AppConstant.MusicSQL.SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

}
