package com.example.xuntongwatch.receiver;

import com.example.xuntongwatch.util.PhoneRecordUtil;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class CallStateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// 如果是来电
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Service.TELEPHONY_SERVICE);
		if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
			switch (tm.getCallState()) {
			case TelephonyManager.CALL_STATE_RINGING:
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				int maxid = PhoneRecordUtil.maxID(context);
				PhoneRecordUtil.insertDate(context, maxid);
				break;
			case TelephonyManager.CALL_STATE_IDLE:
				break;
			}
		} else {

			switch (tm.getCallState()) {
			case TelephonyManager.CALL_STATE_RINGING:
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				int maxid = PhoneRecordUtil.maxID(context);
				PhoneRecordUtil.insertDate(context, maxid);
				break;
			case TelephonyManager.CALL_STATE_IDLE:
				break;
			}
		}

	}

}
