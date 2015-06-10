package com.example.xuntongwatch.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PreferenceOperation {

	private static final String PREFRENCES = "preferences";
	public static final String CALL_RINGING = "CALL_RINGING";
	public static final String CALL_START_TIME = "CALL_START_TIME";
	public static final String CALL_RING_TIME = "CALL_RING_TIME";
	public static final String CALL_OFFHOOK = "CALL_OFFHOOK";
	public static final String CALL_INCOMMING_PHONE = "CALL_INCOMMING_PHONE";

	public static final int CALL_RINGIN = 1;
	public static final int CALL_OFFHOO = 3;
	public static final int CALL_DEFAULT = 4;

	public static SharedPreferences pre;

	public static void getInstant(Context context) {
		if (pre == null) {
			pre = context.getSharedPreferences(PREFRENCES, 0);
		}
	}

	/**
	 * 
	 * @param context
	 * @param call_key
	 *            (三种状态)CALL_RINGING,CALL_IDLE,CALL_OFFHOOK
	 * @param value
	 *            (与call_key相对应的三种)CALL_RINGIN,CALL_IDL,CALL_OFFHOO
	 */
	public static void putCallState(String call_key, int value) {
		Editor editor = pre.edit();
		editor.putInt(call_key, value);
		editor.commit();
	}

	/**
	 * 插入通话开始时间
	 * 
	 * @param value
	 */
	public static void putCallStartTime(long value) {
		Editor editor = pre.edit();
		editor.putLong(CALL_START_TIME, value);
		editor.commit();
	}

	/**
	 * 获取通话开始时间
	 * 
	 * @return
	 */
	public static long getCallStartTime() {
		long time = pre.getLong(CALL_START_TIME, 0);
		return time;
	}

	/**
	 * 插入响铃开始时间
	 * 
	 * @param value
	 */
	public static void putCallRingTime(long value) {
		Editor editor = pre.edit();
		editor.putLong(CALL_RING_TIME, value);
		editor.commit();
	}

	/**
	 * 插入响铃时的电话号码
	 * 
	 * @param phone
	 */
	public static void putCallIncommingPhone(String phone) {
		Editor editor = pre.edit();
		editor.putString(CALL_INCOMMING_PHONE, phone);
		editor.commit();
	}

	public static String getCallIncommingPhone() {
		String phone = pre.getString(CALL_INCOMMING_PHONE, "");
		return phone;
	}

	/**
	 * 获取响铃开始时间
	 * 
	 * @return
	 */
	public static long getCallRingTime() {
		long time = pre.getLong(CALL_RING_TIME, 0);
		return time;
	}

	/**
	 * 
	 * @param context
	 * @param call_key
	 *            (三种状态)CALL_RINGING,CALL_IDLE,CALL_OFFHOOK
	 * @return int (四种类型)CALL_RINGIN,CALL_IDL,CALL_OFFHOO,CALL_DEFAULT
	 */
	public static int getCallState(String call_key) {
		int msg = pre.getInt(call_key, CALL_DEFAULT);
		return msg;
	}

	/**
	 * @param context
	 */
	public static void clearCallState() {
		Editor editor = pre.edit();
		editor.remove(CALL_RINGING);
		editor.remove(CALL_OFFHOOK);
		editor.remove(CALL_START_TIME);
		editor.remove(CALL_RING_TIME);
		editor.remove(CALL_RING_TIME);
		editor.remove(CALL_INCOMMING_PHONE);
		editor.commit();
	}

	/**
	 * 将没发送的信息放入缓存
	 * 
	 * @param context
	 * @param phone
	 * @param text
	 */
	public static void putMessage(String phone, String text) {
		Editor editor = pre.edit();
		editor.putString(phone, text);
		editor.commit();
	}

	/**
	 * 取出缓存里的信息
	 * 
	 * @param context
	 * @param phone
	 * @return
	 */
	public static String getMessage(String phone) {
		String msg = pre.getString(phone, "");
		return msg;
	}

	/**
	 * 清空缓存
	 * 
	 * @param context
	 * @param phone
	 */
	public static void clearMessage(String phone) {
		Editor editor = pre.edit();
		editor.remove(phone);
		editor.commit();
	}

}
