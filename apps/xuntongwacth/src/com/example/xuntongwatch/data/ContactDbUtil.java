package com.example.xuntongwatch.data;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.xuntongwatch.entity.Contact;
import com.example.xuntongwatch.entity.GridViewItemImageView;

public class ContactDbUtil {
	private MySQLiteDataBase mdb;
	private SQLiteDatabase db;

	public ContactDbUtil(Context context) {
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

	public void insertInto(Contact contact) {
		open();
		String sql = "insert into contact "
				+ "(contact_phone,contact_name,contact_head) values(?,?,?)";

		Object[] obj = new Object[] { contact.getContact_phone(),
				contact.getContact_name(), contact.getContact_head() };
		db.execSQL(sql, obj);
		// ContentValues values = new ContentValues();
		// values.put("contact_phone", contact.getContact_phone());
		// db.insert("contact", null, values);
		close(null);
	}

	public void deleteByPhone(String phone) {
		open();
		String sql = "delete from contact where contact_phone = ?";
		String[] str = new String[] { phone };
		db.execSQL(sql, str);
		// db.delete(table, whereClause, whereArgs)
		close(null);
	}

	public void deleteByContact_id(int contact_id) {
		open();
		String sql = "delete from contact where contact_id = ?";
		String[] str = new String[] { contact_id + "" };
		db.execSQL(sql, str);
		close(null);
	}
//
//	public void updateByContact_id(Contact contact) {
//		open();
//		String sql = "update contact set contact_phone=?,contact_name=?,contact_head=? "
//				+ "where contact_id=?";
//		String[] str = new String[] { contact.getContact_phone(),
//				contact.getContact_name(), contact.getContact_head(),
//				contact.getContact_id() + "" };
//		db.execSQL(sql, str);
//		close(null);
//	}

//	public ArrayList<GridViewItemImageView> findAllContact() {
//		ArrayList<GridViewItemImageView> list = new ArrayList<GridViewItemImageView>();
//		open();
//		String sql = "select * from contact";
//		Cursor cursor = db.rawQuery(sql, null);
//		while (cursor.moveToNext()) {
//			int contact_id = cursor.getInt(cursor.getColumnIndex("contact_id"));
//			String contact_phone = cursor.getString(cursor
//					.getColumnIndex("contact_phone"));
//			String contact_name = cursor.getString(cursor
//					.getColumnIndex("contact_name"));
//			String contact_head = cursor.getString(cursor
//					.getColumnIndex("contact_head"));
//			Contact contact = new Contact(contact_id, contact_phone,
//					contact_name, contact_head);
//			list.add(contact);
//		}
//		close(cursor);
//		return list;
//	}
}
