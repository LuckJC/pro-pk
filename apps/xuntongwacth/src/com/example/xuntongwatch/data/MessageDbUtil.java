package com.example.xuntongwatch.data;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.xuntongwatch.entity.Message_;

public class MessageDbUtil {
	private MySQLiteDataBase mdb;
	private SQLiteDatabase db;

	public MessageDbUtil(Context context) {
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

	public int insertInto(Message_ msg) {
		open();
		String sql = "insert into message "
				+ "(message_phone,message_content,message_see,"
				+ "message_state,message_time,message_send_ok) values (?,?,?,?,?,?)";
		Object[] obj = new Object[] { msg.getMessage_phone(),
				msg.getMessage_content(), msg.getMessage_see(),
				msg.getMessage_state(), msg.getMessage_time(),
				msg.getMessage_send_ok() };
		db.execSQL(sql, obj);
		String sql_1 = "select message_id from message order by message_id desc limit 1";
		// String sql_1 = "select max(message_id) from message";
		Cursor cursor = db.rawQuery(sql_1, null);
		cursor.moveToFirst();
		int message_id = cursor.getInt(0);
		close(cursor);
		return message_id;
	}

	public void deleteMsg(String phone) {
		open();
		String sql = "delete from message where message_phone = ?";
		String[] str = new String[] { phone };
		db.execSQL(sql, str);
		close(null);
	}

	public void deleteMsg(ArrayList<String> phones) {
		open();
		StringBuffer sql = new StringBuffer(
				"delete from message where message_phone in (");
		Log.e("", "size == " + phones.size());
		String[] str = new String[phones.size()];
		for (int i = 0; i < phones.size(); i++) {
			if (i == phones.size() - 1) {
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

	public void deleteMsg(int message_id) {
		open();
		String sql = "delete from message where message_id = ?";
		String[] str = new String[] { message_id + "" };
		db.execSQL(sql, str);
		close(null);
	}

	public void deleteMsgByMessage_id(ArrayList<String> ids) {
		open();
		StringBuffer sql = new StringBuffer(
				"delete from message where message_id in (");
		String[] str = new String[ids.size()];
		for (int i = 0; i < ids.size(); i++) {
			if (i == ids.size() - 1) {
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

	public ArrayList<Message_> findMessageByPhone(String phone) {
		ArrayList<Message_> list = new ArrayList<Message_>();
		open();
		String sql = "select * from message where message_phone = ?";
		String[] str = new String[] { phone };
		Cursor cursor = db.rawQuery(sql, str);
		while (cursor.moveToNext()) {
			int message_id = cursor.getInt(cursor.getColumnIndex("message_id"));
			String message_phone = cursor.getString(cursor
					.getColumnIndex("message_phone"));
			String message_content = cursor.getString(cursor
					.getColumnIndex("message_content"));
			String message_see = cursor.getString(cursor
					.getColumnIndex("message_see"));
			String message_state = cursor.getString(cursor
					.getColumnIndex("message_state"));
			long message_time = cursor.getLong(cursor
					.getColumnIndex("message_time"));
			String message_send_ok = cursor.getString(cursor
					.getColumnIndex("message_send_ok"));
			Message_ msg = new Message_(message_id, message_phone,
					message_content, message_see, message_state, message_time,
					"", "", message_send_ok);
			list.add(msg);
		}
		close(cursor);
		return list;
	}

//	public ArrayList<Message_> findAllMessageGroupByPhone() {
//		ArrayList<Message_> list = new ArrayList<Message_>();
//		open();
//
//		String sql = "select * from message m left join contact c on m.message_phone = c.contact_phone group by m.message_phone order by m.message_time desc";
//		Cursor cursor = db.rawQuery(sql, null);
//		while (cursor.moveToNext()) {
//			int message_id = cursor.getInt(cursor.getColumnIndex("message_id"));
//			String message_phone = cursor.getString(cursor
//					.getColumnIndex("message_phone"));
//			String message_content = cursor.getString(cursor
//					.getColumnIndex("message_content"));
//			String message_see = cursor.getString(cursor
//					.getColumnIndex("message_see"));
//			String message_state = cursor.getString(cursor
//					.getColumnIndex("message_state"));
//			long message_time = cursor.getLong(cursor
//					.getColumnIndex("message_time"));
//			String contact_name = cursor.getString(cursor
//					.getColumnIndex("contact_name"));
//			String contact_head = cursor.getString(cursor
//					.getColumnIndex("contact_head"));
//			String message_send_ok = cursor.getString(cursor
//					.getColumnIndex("message_send_ok"));
//			Message_ msg = new Message_(message_id, message_phone,
//					message_content, message_see, message_state, message_time,
//					contact_head, contact_name, message_send_ok);
//
//			Log.e("", "contact_name == " + contact_name);
//			Log.e("", "message_phone == " + message_phone);
//
//			list.add(msg);
//		}
//		close(cursor);
//		return list;
//	}

	public void updateMessage_send_okByMessage_id(int message_id) {
		open();
		String sql = "update message set message_send_ok = ? where message_id = ?";
		Object[] str = new Object[] { "1", message_id + "" };
		db.execSQL(sql, str);
		close(null);
		// ContentValues values = new ContentValues();
		// values.put("message_send_ok", "1");
		// String where = "message_id = ?";
		// String[] strs = new String[] { message_id + "" };
		// int i = db.update("message", values, where, strs);
		// close(null);
		// boolean b = true;
		// if (i == -1) {
		// b = false;
		// }
	}
	
}
