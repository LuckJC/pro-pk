package com.example.xuntongwatch.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteDataBase extends SQLiteOpenHelper {

	public MySQLiteDataBase(Context context) {
		super(context, "telephoneadress.db", null, 1);

	}

	 
	@Override
	public void onCreate(SQLiteDatabase db) {

		String sql = "create table telephone"
				+ " (_id integer primary key autoincrement,telephone varchar(50),"
				+ "areaCode varchar(50),city varchar(50),cardType varchar(50))";
		db.execSQL(sql);

		String sql_contact = "create table " + DB.TABLE_CONTACT
				+ " (contact_id integer primary key autoincrement,"
				+ "contact_phone varchar(50),contact_name varchar(50),"
				+ "contact_head varchar(200))";
		db.execSQL(sql_contact);

		String sql_record = "create table " + DB.TABLE_RECORD
				+ " (record_id integer primary key autoincrement,"
				+ "record_phone varchar(50),record_time long,"
				+ "record_when long,record_state varchar(1))";
		db.execSQL(sql_record);

		String sql_message = "create table " + DB.TABLE_MESSAGE
				+ " (message_id integer primary key autoincrement,"
				+ "message_phone varchar(50),message_time long,"
				+ "message_see varchar(1),message_state varchar(1),"
				+ "message_content varchar(500),message_send_ok varchar(2))";
		db.execSQL(sql_message);
	}

	/**
	 * 更新版本时调用的方法
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

}
