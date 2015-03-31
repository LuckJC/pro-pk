package com.example.xuntongwatch.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.xuntongwatch.data.RecordDbUtil;
import com.example.xuntongwatch.entity.Record_;
import com.example.xuntongwatch.util.PreferenceOperation;

public class PhoneReceiver extends BroadcastReceiver {

	public static boolean isComing = true;

	@Override
	public void onReceive(Context context, Intent intent) {
		PreferenceOperation.getInstant(context);
		if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
			isComing = false;
		}
		String record_phone = intent
				.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		Log.e("", "record_phone  === " + record_phone);
		TelephonyManager telephony = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		int state = telephony.getCallState();
		switch (state) {
		case TelephonyManager.CALL_STATE_RINGING:// 响铃
			PreferenceOperation.putCallState(PreferenceOperation.CALL_RINGING,
					PreferenceOperation.CALL_RINGIN);
			PreferenceOperation.putCallRingTime(System.currentTimeMillis());
			PreferenceOperation.putCallIncommingPhone(record_phone);
			break;
		case TelephonyManager.CALL_STATE_IDLE:// 空闲
			int ring = PreferenceOperation
					.getCallState(PreferenceOperation.CALL_RINGING);
			int offhook = PreferenceOperation
					.getCallState(PreferenceOperation.CALL_OFFHOOK);
			if (ring == PreferenceOperation.CALL_RINGIN
					&& offhook == PreferenceOperation.CALL_OFFHOO) {
				// 表示接电话成功
				long ringTime = PreferenceOperation.getCallRingTime();
				long stopTime = System.currentTimeMillis();
				long startTime = PreferenceOperation.getCallStartTime();
				long when = stopTime - startTime;
				String phone = PreferenceOperation.getCallIncommingPhone();
				// Record_ record = new Record_(record_id, record_phone,
				// record_time, record_when, record_state, contact_head,
				// contact_name)
				Record_ record = new Record_();
				record.setRecord_phone(phone);
				record.setRecord_state("1");
				record.setRecord_time(ringTime);
				record.setRecord_when(when);
				RecordDbUtil recordUtil = new RecordDbUtil(context);
				recordUtil.insertInto(record);

			} else if (ring == PreferenceOperation.CALL_RINGIN
					&& offhook == PreferenceOperation.CALL_DEFAULT) {
				// 表示打入电话没有接
				long ringTime = PreferenceOperation.getCallRingTime();
				String phone = PreferenceOperation.getCallIncommingPhone();
				Log.e("", "挂电话了  挂电话时   响铃的时间是  ====  " + ringTime);

				Record_ record = new Record_();
				record.setRecord_time(ringTime);
				record.setRecord_state("3");
				record.setRecord_phone(phone);
				record.setRecord_when(-1);
				RecordDbUtil recordUtil = new RecordDbUtil(context);
				recordUtil.insertInto(record);
			} else if (ring == PreferenceOperation.CALL_DEFAULT
					&& offhook == PreferenceOperation.CALL_OFFHOO) {
				// 表示拨出电话

			}
			// 清空所有状态
			PreferenceOperation.clearCallState();
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:// 摘机
			PreferenceOperation.putCallState(PreferenceOperation.CALL_OFFHOOK,
					PreferenceOperation.CALL_OFFHOO);
			int ring_ = PreferenceOperation
					.getCallState(PreferenceOperation.CALL_RINGING);
			if (ring_ == PreferenceOperation.CALL_RINGIN) {
				// 表示开始接听电话 了。
				long startTime = System.currentTimeMillis();
				PreferenceOperation.putCallStartTime(startTime);
			}
			break;
		}
	}
}
