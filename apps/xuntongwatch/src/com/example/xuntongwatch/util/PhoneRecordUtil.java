package com.example.xuntongwatch.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony.Sms;

import com.example.xuntongwatch.entity.PhoneRecord;

public class PhoneRecordUtil {

	private final static Uri uri = CallLog.Calls.CONTENT_URI;

	private final static String _id = "_id";

	/**
	 * 电话号码(String)
	 */
	private final static String number = CallLog.Calls.NUMBER;

	/**
	 * 时间(long)
	 */
	private final static String date = CallLog.Calls.DATE;

	/**
	 * 时长(long)
	 */
	private final static String duration = CallLog.Calls.DURATION;

	/**
	 * 类型(int)
	 * 
	 * INCOMING_TYPE (呼入) OUTGOING_TYPE (呼出) MISSED_TYPE (未接)
	 */
	private final static String type = CallLog.Calls.TYPE;

	/**
	 * 是否已读(int)
	 * 
	 * 1 (已读) 0 (未读)
	 */
	private final static String new_ = CallLog.Calls.NEW;

	/**
	 * 联系人名字
	 * 
	 * （空表示 没有在联系人列表中）
	 * 
	 * 注：如果是先有通话记录 然后再添加此号码到联系人列表中 此项也还是为空 所以要关联 联系人必须通过电话号码向data表中查找联系人名字
	 * 
	 */
	private final static String name = CallLog.Calls.CACHED_NAME;

	public static void sTateReplace(Context context) {
		ContentValues updateValues = new ContentValues();
		updateValues.put(Calls.NEW, 0);
		context.getContentResolver().update(uri, updateValues,
				Calls.TYPE + "=3 AND " + Calls.NEW + "=1", null);
	}

	@SuppressLint("NewApi")
	public static void sTateReplaceSmS(Context context) {
		Uri uri = Uri.parse("content://sms/inbox");
		ContentValues updateValues = new ContentValues();
		updateValues.put(Sms.SEEN, 1);
		context.getContentResolver().update(uri, updateValues,
				Calls.NEW + "=0", null);
	}

	@SuppressLint("InlinedApi")
	public static ArrayList<PhoneRecord> findAllPhoneRecordByPhone(
			Context context) {
		ArrayList<String> ss = new ArrayList<String>();
		ArrayList<PhoneRecord> list = new ArrayList<PhoneRecord>();
		Uri phone_uri = Phone.CONTENT_URI;
		String[] projections = new String[] { _id, number, date, new_, type,
				name, duration };
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = date + " desc";
		Cursor cursor = context.getContentResolver().query(uri, projections,
				selection, selectionArgs, sortOrder);
		int i = 0;
		while (cursor.moveToNext()) {
			String number = cursor.getString(cursor
					.getColumnIndex(PhoneRecordUtil.number));
			number = number.replace(" ", "");
			if (ss.contains(number)) {
				continue;
			}
			i++;
			ss.add(number);
			int _id = cursor.getInt(cursor.getColumnIndex(PhoneRecordUtil._id));
			long date = cursor.getLong(cursor
					.getColumnIndex(PhoneRecordUtil.date));
			long duration = cursor.getLong(cursor
					.getColumnIndex(PhoneRecordUtil.duration));
			int new_ = cursor.getInt(cursor
					.getColumnIndex(PhoneRecordUtil.new_));
			int type = cursor.getInt(cursor
					.getColumnIndex(PhoneRecordUtil.type));
			String name = cursor.getString(cursor
					.getColumnIndex(PhoneRecordUtil.name));
			String photo = null;
			String where = Phone.NUMBER + "=?";
			String[] args = new String[] { number };
			Cursor cursor_ = context.getContentResolver().query(phone_uri,
					new String[] { Phone.DISPLAY_NAME, Phone.PHOTO_URI },
					where, args, null);
			if (cursor_.moveToFirst()) {
				name = cursor_.getString(0);
				photo = cursor_.getString(1);
			}
			PhoneRecord phoneRecord = new PhoneRecord(_id, number, date,
					duration, type, new_, name, photo);
			list.add(phoneRecord);
			cursor_.close();
		}
		cursor.close();
		return list;
	}

	public static int maxID(Context context) {
		int id = -1;
		String[] projections = new String[] { _id };
		String sortOrder = _id + " desc";
		Cursor cursor = context.getContentResolver().query(uri, projections,
				null, null, null);
		if (cursor.moveToLast()) {
			id = cursor.getInt(cursor.getColumnIndex(PhoneRecordUtil._id));

		}
		cursor.close();
		return id;
	}

	public static void insertDate(Context context, int id) {
		Calendar c = Calendar.getInstance();
		String where = PhoneRecordUtil._id + "=?";
		ContentValues values = new ContentValues();
		values.put(date, c.getTimeInMillis());
		context.getContentResolver().update(uri, values, where,
				new String[] { String.valueOf(id) });
	}

	public static void deleteRecordByNumber(Context context, String number) {
		String where = PhoneRecordUtil.number + "=?";
		String[] selectionArgs = new String[] { number };
		if (Utils.isMobilePhone(number)) {
			String number1 = number.substring(0, 3) + " "
					+ number.substring(3, 7) + " " + number.substring(7, 11);
			where = PhoneRecordUtil.number + " in (?,?)";
			selectionArgs = new String[] { number, number1 };
		}
		context.getContentResolver().delete(uri, where, selectionArgs);
	}

	public static ArrayList<PhoneRecord> queryRecordByNumber(Context context,
			String number) {
		ArrayList<String> ss = new ArrayList<String>();
		ArrayList<PhoneRecord> list = new ArrayList<PhoneRecord>();
		Uri phone_uri = Phone.CONTENT_URI;
		String where = PhoneRecordUtil.number + " =?";
		String[] selection = new String[] { _id, PhoneRecordUtil.number, date,
				new_, type, name, duration };
		String[] selectionArgs = new String[] { number };
		String sortOrder = _id + " desc";
		if (Utils.isMobilePhone(number)) {
			String number1 = number.substring(0, 3) + " "
					+ number.substring(3, 7) + " " + number.substring(7, 11);
			where = PhoneRecordUtil.number + " in (?,?)";
			selectionArgs = new String[] { number, number1 };
		}
		Cursor cursor = context.getContentResolver().query(uri, selection,
				where, selectionArgs, sortOrder);
		while (cursor.moveToNext()) {
			String number1 = cursor.getString(cursor
					.getColumnIndex(PhoneRecordUtil.number));
			number1 = number1.replace(" ", "");
		 

			ss.add(number1);
			int _id = cursor.getInt(cursor.getColumnIndex(PhoneRecordUtil._id));
			long date = cursor.getLong(cursor
					.getColumnIndex(PhoneRecordUtil.date));
			long duration = cursor.getLong(cursor
					.getColumnIndex(PhoneRecordUtil.duration));
			int new_ = cursor.getInt(cursor
					.getColumnIndex(PhoneRecordUtil.new_));
			int type = cursor.getInt(cursor
					.getColumnIndex(PhoneRecordUtil.type));
			String name = cursor.getString(cursor
					.getColumnIndex(PhoneRecordUtil.name));
			String photo = null;
			String where1 = Phone.NUMBER + "=?";
			String[] args = new String[] { number1 };
			Cursor cursor_ = context.getContentResolver().query(phone_uri,
					new String[] { Phone.DISPLAY_NAME, Phone.PHOTO_URI },
					where1, args, null);
			if (cursor_.moveToFirst()) {
				name = cursor_.getString(0);
				photo = cursor_.getString(1);
			}
			PhoneRecord phoneRecord = new PhoneRecord(_id, number1, date,
					duration, type, new_, name, photo);
			list.add(phoneRecord);
			cursor_.close();
		}
		cursor.close();
		return list;
	}

	public static void deleteRecordByNumbers(Context context,
			ArrayList<String> numbers) {
		int size = numbers.size();
		if (size <= 0) {
			return;
		}
		StringBuffer where = new StringBuffer(PhoneRecordUtil.number + " in (");
		String[] selectionArgs = new String[size];
		for (int i = 0; i < size; i++) {
			selectionArgs[i] = numbers.get(i).replace(" ", "");
			if (i == size) {
				where.append("?)");
			} else {
				where.append("?,");
			}
		}
		context.getContentResolver().delete(uri, where.toString(),
				selectionArgs);
	}

	public static void deleteRecordBy_id(Context context, int _id) {
		String where = PhoneRecordUtil._id + "=?";
		String[] selectionArgs = new String[] { _id + "" };
		context.getContentResolver().delete(uri, where, selectionArgs);
	}

	public static void deleteRecordBy_id(Context context,
			ArrayList<Integer> _ids) {
		int size = _ids.size();
		if (size <= 0) {
			return;
		}
		StringBuffer where = new StringBuffer(PhoneRecordUtil._id + " in (");
		String[] selectionArgs = new String[size];
		for (int i = 0; i < size; i++) {
			selectionArgs[i] = _ids.get(i) + "";
			if (i == size) {
				where.append("?)");
			} else {
				where.append("?,");
			}
		}
		context.getContentResolver().delete(uri, where.toString(),
				selectionArgs);
	}

}
