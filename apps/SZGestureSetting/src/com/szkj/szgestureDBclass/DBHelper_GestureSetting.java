package com.szkj.szgestureDBclass;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class DBHelper_GestureSetting extends ContentProvider {
	//ContentProvider的uri  
    public static final String CONTENT_URI = "content://com.szkj.szgestureDBclass.DBHelper_GestureSetting";  
	
    static final String QUERY_DATA="select a.[gesture_style],b.[function_name],c.[gesture_function_phonenumber] from gesture_function c ,gesture a,function b where c.[gesture_function_gid]=a.[gesture_id] and c.[gesture_function_fid]=b.[function_id]";
    
    private static SQLiteDatabase database;
    
    @Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		if (database == null) { 
			database = new Query_Defined_Util(getContext()).open();
		}
		return database != null; 
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		// TODO Auto-generated method stub
		
	String querySql = QUERY_DATA;
		if (selectionArgs != null) {
			querySql += " and a.[gesture_style]=?";
		}
		Cursor cs = database.rawQuery(querySql, selectionArgs);
		return cs;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
