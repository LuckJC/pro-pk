package com.example.xuntongwatch.databaseutil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.util.Log;

import com.example.xuntongwatch.entity.Message_;
import com.example.xuntongwatch.entity.Message_Thread;
import com.example.xuntongwatch.util.Utils;

public class SmsUtil {

	@SuppressLint("NewApi")
	private static final Uri sms_uri = Sms.CONTENT_URI;// 短信 Uri

	@SuppressLint("NewApi")
	private static final Uri thread_uri = Threads.CONTENT_URI;// 短信会话Uri

	private static final Uri data_uri = Data.CONTENT_URI;// 通讯录数据库表Uri

	private static final Uri phonelookup_uri = PhoneLookup.CONTENT_FILTER_URI;

	private static final String sms_address = Sms.ADDRESS; // 电话号码　　（+8613827476390）

	private static final String sms_date = Sms.DATE; // 接收的时间

	private static final String sms_thread_id = Sms.THREAD_ID; // 会话的thread_id
																// 同一个电话号码的所有短信此id都相同

	private static final String sms__id = Sms._ID; // 每一条短信的主键id

	private static final String sms_data_sent = Sms.DATE_SENT; // 发送的时间

	private static final String sms_read = Sms.READ; // 是否已读 (0 未读，1 已读)

	private static final String sms_status = Sms.STATUS;

	private static final String sms_type = Sms.TYPE; // 状态 （ 1 接收的 2 发送的）

	private static final String sms_body = Sms.BODY; // 短信内容

	@SuppressLint("InlinedApi")
	public static ArrayList<Message_Thread> allMessage_Thread(Context context) {
		ArrayList<Integer> has = new ArrayList<Integer>();
		ArrayList<Message_Thread> list = new ArrayList<Message_Thread>();
		String[] projection = new String[] { Sms.THREAD_ID, Sms.DATE, Sms.BODY, Sms.READ,
				Sms.ADDRESS };
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = Sms.DATE + " desc";
		Cursor cursor = context.getContentResolver().query(sms_uri, projection, selection,
				selectionArgs, sortOrder);
		while (cursor.moveToNext()) {
			int thread_id = cursor.getInt(cursor.getColumnIndex(Sms.THREAD_ID));
			if (has.contains(thread_id)) {
				continue;
			}
			has.add(thread_id);
			long thread_date = cursor.getLong(cursor.getColumnIndex(Sms.DATE));
			String thread_snippet = cursor.getString(cursor.getColumnIndex(Sms.BODY));
			int thread_read = cursor.getInt(cursor.getColumnIndex(Sms.READ));
			String phone = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
			String name = "";
			byte[] photo = null;
			if(phone!=null){			phone = phone.replace("+86", "");}


			// <------------ 通过phone查找头像、姓名 --------------->
			Uri uriNumber2Contacts = Uri.parse("content://com.android.contacts/data/phones/filter/"
					+ phone);
			Cursor cursorCantacts = context.getContentResolver().query(uriNumber2Contacts, null,
					null, null, null);
			if (cursorCantacts.getCount() > 0) { // 若游标不为0则说明有头像,游标指向第一条记录
				cursorCantacts.moveToFirst();
				Long contactID = cursorCantacts
						.getLong(cursorCantacts.getColumnIndex("contact_id"));
				name = cursorCantacts.getString(cursorCantacts
						.getColumnIndex(Contacts.DISPLAY_NAME));
				Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactID);
				InputStream input = Contacts.openContactPhotoInputStream(
						context.getContentResolver(), uri);
				if (input != null) {
					try {
						photo = toByteArray(input);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			Message_Thread thread = new Message_Thread(thread_id, thread_date, 0, thread_snippet,
					thread_read, name, phone, photo);
			list.add(thread);

		}
		return list;
	}

	@SuppressLint("InlinedApi")
	public static ArrayList<Message_> findMessageByThread_id(Context context, int thread_id) {
		ArrayList<Message_> list = new ArrayList<Message_>();
		String[] projection = new String[] { Sms._ID, Sms.ADDRESS, Sms.BODY, Sms.READ, Sms.TYPE,
				Sms.DATE };
		String selection = Sms.THREAD_ID + "=?";
		String[] selectionArgs = new String[] { thread_id + "" };
		String sortOrder = Sms.DATE + " asc";
		Cursor cursor = context.getContentResolver().query(sms_uri, projection, selection,
				selectionArgs, sortOrder);
		while (cursor.moveToNext()) {
			int message_id = cursor.getInt(cursor.getColumnIndex(Sms._ID));
			String message_phone = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
			String message_content = cursor.getString(cursor.getColumnIndex(Sms.BODY));
			String message_see = cursor.getString(cursor.getColumnIndex(Sms.READ));
			String message_state = cursor.getString(cursor.getColumnIndex(Sms.TYPE));
			long message_time = cursor.getLong(cursor.getColumnIndex(Sms.DATE));
			// String message_send_ok =
			// cursor.getString(cursor.getColumnIndex("message_send_ok"));/
			Message_ msg = new Message_(message_id, message_phone, message_content, message_see,
					message_state, message_time, "", "", "");
			list.add(msg);
		}

		return list;
	}

	@SuppressLint("InlinedApi")
	public static ArrayList<Message_> findMessageByPhone(Context context, String phone) {
		// if(!phone.contains("+86"))
		// {
		// phone = "+86"+phone;
		// }
		ArrayList<Message_> list = new ArrayList<Message_>();
		String[] projection = new String[] { Sms._ID, Sms.ADDRESS, Sms.BODY, Sms.READ, Sms.TYPE,
				Sms.DATE };
		String selection = Sms.ADDRESS + "=?";
		String[] selectionArgs = new String[] { phone };
		String sortOrder = Sms.DATE + " asc";
		Cursor cursor = context.getContentResolver().query(sms_uri, projection, selection,
				selectionArgs, sortOrder);
		while (cursor.moveToNext()) {
			int message_id = cursor.getInt(cursor.getColumnIndex(Sms._ID));
			String message_phone = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
			String message_content = cursor.getString(cursor.getColumnIndex(Sms.BODY));
			String message_see = cursor.getString(cursor.getColumnIndex(Sms.READ));
			String message_state = cursor.getString(cursor.getColumnIndex(Sms.TYPE));
			long message_time = cursor.getLong(cursor.getColumnIndex(Sms.DATE));
			// String message_send_ok =
			// cursor.getString(cursor.getColumnIndex("message_send_ok"));/
			Message_ msg = new Message_(message_id, message_phone, message_content, message_see,
					message_state, message_time, "", "", "");
			list.add(msg);
		}

		return list;
	}

	private static byte[] toByteArray(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		return output.toByteArray();
	}

	public static void deleteMessageByMessage_id(Context context, int message_id) {
		String where = Sms._ID + "=?";
		String[] selectionArgs = new String[] { message_id + "" };
		context.getContentResolver().delete(sms_uri, where, selectionArgs);
	}

	public static void deleteMessageByThread_id(Context context, int thread_id) {
		String where = Sms.THREAD_ID + "=?";
		String[] selectionArgs = new String[] { thread_id + "" };
		context.getContentResolver().delete(sms_uri, where, selectionArgs);
	}

	public static void deleteMessageByThread_id(Context context, ArrayList<Integer> thread_ids) {
		StringBuffer where = new StringBuffer(Sms.THREAD_ID + " in(");
		String[] selectionArgs = new String[thread_ids.size()];
		for (int i = 0; i < thread_ids.size(); i++) {
			if (i == thread_ids.size() - 1) {
				where.append("?)");
			} else {
				where.append("?,");
			}
			selectionArgs[i] = thread_ids.get(i) + "";
		}

		context.getContentResolver().delete(sms_uri, where.toString(), selectionArgs);
	}

	public static void deleteMessage(Context context, Message_ msg) {
		context.getContentResolver().delete(Uri.parse("content://sms/sent"), "body" + "in(?)",
				new String[] { msg.getMessage_content() });
	}

	/**
	 * 插入发件箱
	 * 
	 * @param context
	 * @param msg
	 */
	public static void insertMessageIntoSent(Context context, Message_ msg) {
		String phone = msg.getMessage_phone();
		phone = phone.replace("+86", "");
		if (Utils.isMobilePhone(phone)) {
			phone = "+86" + phone;
		}
		ContentValues values = new ContentValues();
		values.put("address", msg.getMessage_phone());
		// values.put("person", msg.get);
		// values.put("protocol", "0");
		values.put("read", "1");
		values.put("status", "-1");
		values.put("body", msg.getMessage_content());
		context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
		Log.e("tl3shi", "插入 发件箱 成功 ");
	}

	/**
	 * 插入 已发送短信箱
	 * 
	 * @param context
	 * @param msg
	 */
	public static void insertMessageIntoOutbox(Context context, Message_ msg) {
		String phone = msg.getMessage_phone();
		phone = phone.replace("+86", "");
		if (Utils.isMobilePhone(phone)) {
			phone = "+86" + phone;
		}
		ContentValues values = new ContentValues();
		values.put("address", msg.getMessage_phone());
		// values.put("person", msg.get);
		// values.put("protocol", "0");
		values.put("read", "1");
		values.put("status", "-1");
		values.put("body", msg.getMessage_content());
		context.getContentResolver().insert(Uri.parse("content://sms/outbox"), values);
		Log.e("tl3shi", "插入 已发短信 成功 ");
	}

	public static void deleteMessageOther(Context context, Message_ msg) {
		context.getContentResolver().delete(Uri.parse("content://sms/outbox"),
				msg.getMessage_content(), new String[] { "body" });
	}

	/**
	 * 插入收件箱
	 * 
	 * @param context
	 * @param msg
	 */
	public static void insertMessageIntoInbox(Context context, Message_ msg) {
		String phone = msg.getMessage_phone();
		phone = phone.replace("+86", "");
		if (Utils.isMobilePhone(phone)) {
			phone = "+86" + phone;
		}
		ContentValues values = new ContentValues();
		values.put("address", msg.getMessage_phone());
		values.put("body", msg.getMessage_content());
		values.put("date", msg.getMessage_time());
		values.put("read", 0);
		values.put("type", 1);
		// values.put("service_center", "+8613010776500");

		context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
	}

	/**
	 * 插入 草稿箱
	 * 
	 * @param context
	 * @param msg
	 */
	public static void insertMessageIntoDraft(Context context, Message_ msg) {
		String phone = msg.getMessage_phone();
		phone = phone.replace("+86", "");
		if (Utils.isMobilePhone(phone)) {
			phone = "+86" + phone;
		}
		ContentValues values = new ContentValues();
		values.put("address", msg.getMessage_phone());
		values.put("body", msg.getMessage_content());
		values.put("date", msg.getMessage_time());
		values.put("read", 0);
		values.put("type", 1);
		// values.put("service_center", "+8613010776500");

		context.getContentResolver().insert(Uri.parse("content://sms/draft"), values);
	}

	/**
	 * 实现粘贴功能
	 * 
	 * @param context
	 * @return
	 */
	public static String paste(Context context) {
		// 得到剪贴板管理器
		ClipboardManager cmb = (ClipboardManager) context
				.getSystemService(Context.CLIPBOARD_SERVICE);
		return cmb.getText().toString().trim();
	}
	/** 
	* 实现文本复制功能 
	* @param content 
	*/ 
	public static void copy(String content, Context context) { 
	// 得到剪贴板管理器 
	ClipboardManager cmb = (ClipboardManager) context 
	.getSystemService(Context.CLIPBOARD_SERVICE); 
	cmb.setText(content.trim()); 
	} 
}
