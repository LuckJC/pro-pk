package com.example.xuntongwatch.data;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.xuntongwatch.entity.Record_;

public class RecordDbUtil {
	private MySQLiteDataBase mdb;
	private SQLiteDatabase db;

	public RecordDbUtil(Context context) {
		this.mdb = new MySQLiteDataBase(context);
	}

	public void open() {
		db = mdb.getWritableDatabase();
	}

	public void close(Cursor cursor)

	{
		if (cursor != null) {
			cursor.close();
		}
		db.close();
	}

	public void insertInto(Record_ record) {
		open();
		String sql = "insert into record "
				+ "(record_phone,record_time,record_when,record_state) "
				+ "values (?,?,?,?)";
		Object[] obj = new Object[] { record.getRecord_phone(),
				record.getRecord_time(), record.getRecord_when(),
				record.getRecord_state() };
		db.execSQL(sql, obj);
		close(null);
	}

	public void deleteRecordByPhone(String phone) {
		open();
		String sql = "delete from record where record_phone = ?";
		String[] str = new String[] { phone };
		db.execSQL(sql, str);
		close(null);
	}

	public void deleteRecordByPhone(ArrayList<String> phones) {
		open();
		StringBuffer sql = new StringBuffer(
				"delete from record where record_phone in (");
		String[] str = new String[phones.size()];
		for (int i = 0; i < phones.size(); i++) {
			if (i == phones.size()) {
				sql.append("?");
			} else {
				sql.append("?,");
			}
			str[i] = phones.get(i);
		}
		sql.append(")");
		db.execSQL(sql.toString(), str);
		close(null);
	}

	public void deleteRecordByRecord_id(int record_id) {
		open();
		String sql = "delete from record where record_id = ?";
		String[] str = new String[] { record_id + "" };
		db.execSQL(sql, str);
		close(null);
	}

	public void deleteRecordByRecord_id(ArrayList<String> ids) {
		open();
		StringBuffer sql = new StringBuffer(
				"delete from record where record_id in (");
		String[] str = new String[ids.size()];
		for (int i = 0; i < ids.size(); i++) {
			if (i == ids.size()) {
				sql.append("?");
			} else {
				sql.append("?,");
			}
			str[i] = ids.get(i);
		}
		sql.append(")");
		db.execSQL(sql.toString(), str);
		close(null);
	}

	public ArrayList<Record_> findRecordGroupByPhone() {
		ArrayList<Record_> list = new ArrayList<Record_>();
		open();
		String sql = "select * from record r left join contact c on r.record_phone = c.contact_phone group by r.record_phone order by r.record_time desc";
		Cursor cursor = db.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			int record_id = cursor.getInt(cursor.getColumnIndex("record_id"));
			String record_phone = cursor.getString(cursor
					.getColumnIndex("record_phone"));
			long record_time = cursor.getLong(cursor
					.getColumnIndex("record_time"));
			long record_when = cursor.getLong(cursor
					.getColumnIndex("record_when"));
			String record_state = cursor.getString(cursor
					.getColumnIndex("record_state"));
			String contact_name = cursor.getString(cursor
					.getColumnIndex("contact_name"));
			String contact_head = cursor.getString(cursor
					.getColumnIndex("contact_head"));
			Record_ record = new Record_(record_id, record_phone, record_time,
					record_when, record_state, contact_head, contact_name);
			list.add(record);
		}
		close(cursor);
		return list;
	}

	public ArrayList<Record_> findRecordByPhone(String phone) {
		ArrayList<Record_> list = new ArrayList<Record_>();
		open();
		String sql = "select * from record where record_phone = ?";
		String[] str = new String[] { phone };
		Cursor cursor = db.rawQuery(sql, str);
		while (cursor.moveToNext()) {
			int record_id = cursor.getInt(cursor.getColumnIndex("record_id"));
			String record_phone = cursor.getString(cursor
					.getColumnIndex("record_phone"));
			long record_time = cursor.getLong(cursor
					.getColumnIndex("record_time"));
			long record_when = cursor.getLong(cursor
					.getColumnIndex("record_when"));
			String record_state = cursor.getString(cursor
					.getColumnIndex("record_state"));
			Record_ record = new Record_(record_id, record_phone, record_time,
					record_when, record_state, "", "");
			list.add(record);
		}
		close(cursor);
		return list;
	}

}
