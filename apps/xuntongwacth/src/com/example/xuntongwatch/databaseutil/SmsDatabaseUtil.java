package com.example.xuntongwatch.databaseutil;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony.Sms;
import android.text.TextUtils;

import com.example.xuntongwatch.entity.MessageChat;
import com.example.xuntongwatch.util.ServiceMsgUtil;

public class SmsDatabaseUtil {
	@SuppressLint("NewApi")
	// public static ArrayList<MessageAll> readMsmAll(Context context) {
	// // Sms._ID 短信序号
	// // Sms.BODY;短信的内容
	// // Sms.ADDRESS;发送人号码
	// // Sms.THREAD_ID;对话的序号，如手机号码
	// // Sms.PERSON;发件人，返回一个数字就是联系人列表里的序号，陌生人为null
	// // Sms.DATE;日期 long型
	// // Sms.DATE_SENT;发送日期 long型
	// // Sms.READ;是否阅读 0未读，1已读
	// // Sms.STATUS 状态 -1接收，0 complete,64 pending, 128 failed
	// // Sms.TYPE;类型 1是接收到的，2是已发出的
	//
	// ArrayList<MessageAll> list = new ArrayList<MessageAll>();
	// Cursor cursor = context.getContentResolver().query(
	// Sms.CONTENT_URI,
	// new String[] { Sms.BODY, Sms.ADDRESS, Sms.THREAD_ID,
	// Sms.PERSON, Sms.DATE, Sms.READ }, null, null,
	// Sms.DATE + " desc");
	// ArrayList<String> l = new ArrayList<String>();
	// while (cursor.moveToNext()) {
	// String thread_id = cursor.getString(cursor
	// .getColumnIndex(Sms.THREAD_ID));
	// if (l.contains(thread_id))
	// continue;
	// l.add(thread_id);
	// String address = cursor.getString(cursor
	// .getColumnIndex(Sms.ADDRESS));
	// String content = cursor.getString(cursor.getColumnIndex(Sms.BODY));
	// String person = cursor.getString(cursor.getColumnIndex(Sms.PERSON));
	// String date = cursor.getString(cursor.getColumnIndex(Sms.DATE));
	// String read = cursor.getString(cursor.getColumnIndex(Sms.READ));
	// String name = getPeopleNameFromPerson(context, address);
	// MessageAll msg = new MessageAll(name, content, thread_id, date,
	// read, person, address);
	// list.add(msg);
	// }
	// cursor.close();
	// return list;
	// }
	/**
	 * 通过手机号 查找通讯录中的联系人姓名
	 * 
	 * @param context
	 * @param address
	 *            手机号码
	 * @return
	 */
	public static String getPeopleNameFromPerson(Context context, String address) {
		if (TextUtils.isEmpty(address)) {
			return "";
		}
		String str = ServiceMsgUtil.getString(address);
		if (!TextUtils.isEmpty(str))
			return str;
		String strPerson = address;
		address = address.replace("+86", "");
		String[] projection = new String[] { Phone.DISPLAY_NAME, Phone.NUMBER };
		Uri uri_Person = Uri.withAppendedPath(
				ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
				address); // address 手机号过滤
		Cursor cursor = context.getContentResolver().query(uri_Person,
				projection, null, null, null);

		if (cursor.moveToFirst()) {
			int index_PeopleName = cursor.getColumnIndex(Phone.DISPLAY_NAME);
			String strPeopleName = cursor.getString(index_PeopleName);
			strPerson = strPeopleName;
		}
		cursor.close();

		return strPerson;
	}

	/**
	 * 短信聊天记录
	 * 
	 * @param context
	 * @param thread_id
	 * @return
	 */
	@SuppressLint("NewApi")
	public static ArrayList<MessageChat> getMessageChatByThread_id(
			Context context, String thread_id) {
		ArrayList<MessageChat> list = new ArrayList<MessageChat>();
		Cursor cursor = context.getContentResolver().query(Sms.CONTENT_URI,
				new String[] { Sms._ID, Sms.BODY, Sms.TYPE, Sms.DATE },
				Sms.THREAD_ID + "=?", new String[] { thread_id },
				Sms.DATE + " asc");
		while (cursor.moveToNext()) {
			String _id = cursor.getString(cursor.getColumnIndex(Sms._ID));
			String content = cursor.getString(cursor.getColumnIndex(Sms.BODY));
			String type = cursor.getString(cursor.getColumnIndex(Sms.TYPE));
			long date = cursor.getLong(cursor.getColumnIndex(Sms.DATE));
			MessageChat chat = new MessageChat(_id, type, content, date);
			list.add(chat);
		}
		cursor.close();
		return list;
	}

	@SuppressLint("NewApi")
	public static void deleteMessageByThread_id(Context context,
			String thread_id) {
		int i = context.getContentResolver().delete(Sms.CONTENT_URI,
				Sms.THREAD_ID + " = ? ", new String[] { thread_id });

	}

	@SuppressLint("NewApi")
	public static void deleteMessageByThread_id(Context context,
			ArrayList<String> thread_ids) {

		StringBuffer where = new StringBuffer(Sms.THREAD_ID + " in (");
		String[] selectionArgs = new String[thread_ids.size()];

		for (int i = 0; i < selectionArgs.length; i++) {
			if (i == selectionArgs.length - 1) {
				where.append("?)");
			} else {
				where.append("?,");
			}
			selectionArgs[i] = thread_ids.get(i);
		}

		context.getContentResolver().delete(Sms.CONTENT_URI, where.toString(),
				selectionArgs);

	}

	@SuppressLint("NewApi")
	public static void deleteMessageBy_id(Context context, String _id) {
		int i = context.getContentResolver().delete(Sms.CONTENT_URI,
				Sms._ID + " = ? ", new String[] { _id });

	}

	@SuppressLint("NewApi")
	public static void deleteMessageBy_id(Context context,
			ArrayList<String> _ids) {
		StringBuffer where = new StringBuffer(Sms.THREAD_ID + " in (");
		String[] selectionArgs = new String[_ids.size()];

		for (int i = 0; i < selectionArgs.length; i++) {
			if (i == selectionArgs.length - 1) {
				where.append("?)");
			} else {
				where.append("?,");
			}
			selectionArgs[i] = _ids.get(i);
		}
		int i = context.getContentResolver().delete(Sms.CONTENT_URI,
				where.toString(), selectionArgs);

	}

}
