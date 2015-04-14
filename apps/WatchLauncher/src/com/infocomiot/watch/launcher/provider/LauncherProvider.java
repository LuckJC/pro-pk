package com.infocomiot.watch.launcher.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class LauncherProvider extends ContentProvider {
	private static final String DB_NAME = "launcher.db";
	private static final int DB_VERSION = 1;
	private static final String TABLE_WORKSPACE = "workspace";
	private static final String TABLE_WATCH = "watch";
	
	private static final String CREATE_TABLE_WORKSPACE =
			  "create table " + TABLE_WORKSPACE + " ("
			+ LauncherConfig._ID + " integer primary key, "
			+ LauncherConfig.PACKAGE_NAME + " text not null, "
			+ LauncherConfig.PACKAGE_TYPE + " integer"
			+ " )";
	private static final String CREATE_TABLE_WATCH = 
			  "create table " + TABLE_WATCH + " ("
			+ WatchConfig._ID + " integer primary key, "
			+ WatchConfig.CURRENT_STYLE + " integer"
			+ " )";
	
	private static final UriMatcher sUriMatcher = new UriMatcher(0);
	private static final int WORKSPACE = 1;
	private static final int WORKSPACE_ITEM = 2;
	private static final int WATCH = 3;
	private static final int WATCH_ITEM = 4;
	static {
		sUriMatcher.addURI(LauncherConfig.AUTORITY, "workspace", WORKSPACE);
		sUriMatcher.addURI(LauncherConfig.AUTORITY, "workspace#", WORKSPACE_ITEM);
		sUriMatcher.addURI(LauncherConfig.AUTORITY, "watch", WATCH);
		sUriMatcher.addURI(LauncherConfig.AUTORITY, "watch#", WATCH_ITEM);
	}
	
	private SQLiteHelper mOpenHelper;
	
	
	@Override
	public boolean onCreate() {
		mOpenHelper = new SQLiteHelper(getContext(), DB_NAME, null, DB_VERSION);
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		long id;
		String newSelection;
		switch (sUriMatcher.match(uri)) {
		case WORKSPACE:
			return db.query(TABLE_WORKSPACE, projection, selection, selectionArgs, null, null, sortOrder);
			
		case WORKSPACE_ITEM:
			id = Long.parseLong(uri.getPathSegments().get(1));
			newSelection = LauncherConfig._ID + "=" + id
					+ (selection == null ? "" : " AND (" + selection + ") ");
			return db.query(TABLE_WORKSPACE, projection, newSelection, selectionArgs, null, null, sortOrder);
			
		case WATCH:
			return db.query(TABLE_WATCH, projection, selection, selectionArgs, null, null, sortOrder);
			
		case WATCH_ITEM:
			id = Long.parseLong(uri.getPathSegments().get(1));
			newSelection = LauncherConfig._ID + "=" + id
					+ (selection == null ? "" : " AND (" + selection + ") ");
			return db.query(TABLE_WATCH, projection, newSelection, selectionArgs, null, null, sortOrder);
			
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long id;
		Uri newUri;
		switch (sUriMatcher.match(uri)) {
		case WORKSPACE:
			id = db.insert(TABLE_WORKSPACE, null, values);
			newUri = Uri.withAppendedPath(LauncherConfig.WORKSPACE_CONTENT_URI, String.valueOf(id));
			break;
			
		case WATCH:
			id = db.insert(TABLE_WATCH, null, values);
			newUri = Uri.withAppendedPath(WatchConfig.WATCH_CONTENT_URI, String.valueOf(id));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return newUri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int num;
		switch (sUriMatcher.match(uri)) {
		case WORKSPACE:
			num = db.delete(TABLE_WORKSPACE, selection, selectionArgs);
			break;
			
		case WORKSPACE_ITEM:
			long id = Long.parseLong(uri.getPathSegments().get(1));
			String newSelection = LauncherConfig._ID + "=" + id
					+ (selection == null ? "" : " AND (" + selection + ") ");
			num = db.delete(TABLE_WORKSPACE, newSelection, selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return num;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int num;
		long id;
		String newSelection;
		switch (sUriMatcher.match(uri)) {
		case WORKSPACE:
			num = db.update(TABLE_WORKSPACE, values, selection, selectionArgs);
			break;
			
		case WORKSPACE_ITEM:
			id = Long.parseLong(uri.getPathSegments().get(1));
			newSelection = LauncherConfig._ID + "=" + id
					+ (selection == null ? "" : " AND (" + selection + ") ");
			num = db.update(TABLE_WORKSPACE, values, newSelection, selectionArgs);
			break;
			
		case WATCH:
			num = db.update(TABLE_WATCH, values, selection, selectionArgs);
			break;
			
		case WATCH_ITEM:
			id = Long.parseLong(uri.getPathSegments().get(1));
			newSelection = WatchConfig._ID + "=" + id
					+ (selection == null ? "" : " AND (" + selection + ") ");
			num = db.update(TABLE_WATCH, values, newSelection, selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return num;
	}
	
	private class SQLiteHelper extends SQLiteOpenHelper {
		public SQLiteHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_WORKSPACE);
			db.execSQL(CREATE_TABLE_WATCH);
			initTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
		}
		
		private void initTable(SQLiteDatabase db) {
			ContentValues values = new ContentValues();
			
			//table workspace
			for (String app : LauncherConfig.APPS_COMMON) {
				values.clear();
				values.put(LauncherConfig.PACKAGE_NAME, app);
				values.put(LauncherConfig.PACKAGE_TYPE, LauncherConfig.TYPE_COMMON);
				db.insert(TABLE_WORKSPACE, null, values);
			}
			
			for (String app : LauncherConfig.APPS_SPORTS) {
				values.clear();
				values.put(LauncherConfig.PACKAGE_NAME, app);
				values.put(LauncherConfig.PACKAGE_TYPE, LauncherConfig.TYPE_SPORTS);
				db.insert(TABLE_WORKSPACE, null, values);
			}
			
			for (String app : LauncherConfig.APPS_COMMUNICATIONS) {
				values.clear();
				values.put(LauncherConfig.PACKAGE_NAME, app);
				values.put(LauncherConfig.PACKAGE_TYPE, LauncherConfig.TYPE_COMMUNICATIONS);
				db.insert(TABLE_WORKSPACE, null, values);
			}
			
			//table watch
			values.clear();
			values.put(WatchConfig.CURRENT_STYLE, 0);
			db.insert(TABLE_WATCH, null, values);
		}
	}

}
