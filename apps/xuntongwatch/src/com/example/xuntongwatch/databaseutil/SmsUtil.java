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
	public static final int SMS_SEND_TYPE_OUTBOX = 4;// 短信在发件箱的type值
	@SuppressLint("NewApi")
	private static final Uri sms_uri = Sms.CONTENT_URI;// 短信 Uri

	@SuppressLint("NewApi")
	private static final Uri thread_uri = Threads.CONTENT_URI;// 短信会话Uri

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
			if (phone != null) {
				phone = phone.replace("+86", "");
			}

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
			cursorCantacts.close();
		}
		
		cursor.close();
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
		cursor.close();
		return list;
	}

	public static int findMessageIdByPhone(Context context, long time, String number) {
		String[] projection = new String[] { Sms._ID };
		String selection = Sms.ADDRESS + "=? and " + Sms.DATE + "=?";
		String sortOrder = Sms.DATE + " asc";
		String[] selectionArgs = new String[] { number, String.valueOf(time) };
		Cursor cursor = context.getContentResolver().query(sms_uri, projection, selection,
				selectionArgs, sortOrder);
		int message_id = -1;
		while (cursor.moveToNext()) {
			message_id = cursor.getInt(cursor.getColumnIndex(Sms._ID));
		}
		cursor.close();
		return message_id;
	}

	@SuppressLint("InlinedApi")
	public static ArrayList<Message_> findMessageByThread_id01(Context context, int thread_id) {
		ArrayList<Message_> list = new ArrayList<Message_>();
		String[] projection = new String[] { Sms._ID, Sms.ADDRESS, Sms.BODY, Sms.READ, Sms.TYPE,
				Sms.DATE };
		String selection = Sms.THREAD_ID + " =?";
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
		cursor.close();
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
		cursor.close();
		return list;
	}
	@SuppressLint("InlinedApi")
	public static ArrayList<Message_> findMessage(Context context, String phone) {
		ArrayList<Message_> list = new ArrayList<Message_>();
		String[] projection = new String[] { Sms._ID, Sms.ADDRESS, Sms.BODY, Sms.READ, Sms.TYPE,
				Sms.DATE };
		String selection = Sms.ADDRESS + " =?" ;

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
		cursor.close();
		return list;
	}
	@SuppressLint("InlinedApi")
	public static ArrayList<Message_> findMessageByPhone01(Context context, String phone) {
		ArrayList<Message_> list = new ArrayList<Message_>();
		String[] projection = new String[] { Sms._ID, Sms.ADDRESS, Sms.BODY, Sms.READ, Sms.TYPE,
				Sms.DATE };
		String selection = Sms.ADDRESS + "=? and " + Sms.TYPE + "=?";

		String[] selectionArgs = new String[] { phone, "5" };
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
		cursor.close();
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
		values.put("date", msg.getMessage_time());
		values.put("read", "1");
		values.put("status", "-1");
		values.put("body", msg.getMessage_content());
		Uri u = context.getContentResolver().insert(Uri.parse("content://sms/outbox"), values);
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
	 * 
	 * @param content
	 */
	public static void copy(String content, Context context) {
		// 得到剪贴板管理器
		ClipboardManager cmb = (ClipboardManager) context
				.getSystemService(Context.CLIPBOARD_SERVICE);
		cmb.setText(content.trim());
	}

	public static final int SMS_SEND_TYPE_FAILED = 5;// 短信发送失败时的type值
	public static final int SMS_SEND_TYPE_SEND = 2;// 短信发送成功即已发送的type值

	/**
	 * 将短信从发件箱的状态更改为已发送
	 * <p>
	 * 发件箱的type值为4，已发送的type值为2
	 * </p>
	 * 
	 * @param context
	 * @param msg
	 *            直接根据Message_对象的_id字段进行更新
	 */
	public static void updateSmsTypeToSent(Context context, int messageId) {
		String where = "_id=?";
		String[] selectionArgs = new String[] { String.valueOf(messageId) };
		ContentValues values = new ContentValues();
		values.put("type", SMS_SEND_TYPE_SEND);
		context.getContentResolver().update(Uri.parse("content://sms/outbox"), values, where,
				selectionArgs);
	}

	/**
	 * 将短信从发件箱的状态更改为已发送
	 * <p>
	 * 发件箱的type值为4，已发送的type值为2
	 * </p>
	 * 
	 * @param context
	 * @param msg
	 *            Message_对象，使用其_id字段进行更新
	 */
	public static void updateSmsTypeToSent(Context context, Message_ msg) {
		updateSmsTypeToSent(context, msg.getMessage_id());
	}

	/**
	 * 将发送中的短信的状态更改为失败
	 * <p>
	 * 发件箱的type值为4，已发送的type值为5
	 * </p>
	 * 
	 * @param context
	 * @param msg
	 *            Message_对象，使用其_id字段进行更新
	 */
	public static void updateSmsTypeToFailed(Context context, Message_ msg) {
		updateSmsTypeToFailed(context, msg.getMessage_id());
	}

	/**
	 * 将发送中的短信的状态更改为失败
	 * <p>
	 * 发件箱的type值为4，已发送的type值为5
	 * </p>
	 * 
	 * @param context
	 * @param messageId
	 *            直接根据Message_对象的_id字段进行更新
	 */
	public static void updateSmsTypeToFailed(Context context, int messageId) {
		String where = "_id=?";
		String[] selectionArgs = new String[] { String.valueOf(messageId) };
		ContentValues values = new ContentValues();
		values.put("type", SMS_SEND_TYPE_FAILED);
		context.getContentResolver().update(Uri.parse("content://sms/outbox"), values, where,
				selectionArgs);
	}

	/**
	 * 将已发送的所有短信的状态更改为失败
	 * <p>
	 * 发件箱的type值为4，发送失败的type值为5
	 * </p>
	 * 
	 * @param context
	 * @param messageId
	 *            直接根据Message_对象的_id字段进行更新
	 */
	public static void updateAllOutboxToFailed(Context context) {
		String where = "type=?";
		String[] selectionArgs = new String[] { String.valueOf(SMS_SEND_TYPE_OUTBOX) };
		ContentValues values = new ContentValues();
		values.put("type", SMS_SEND_TYPE_FAILED);
		context.getContentResolver().update(Uri.parse("content://sms/outbox"), values, where,
				selectionArgs);
	}

}
