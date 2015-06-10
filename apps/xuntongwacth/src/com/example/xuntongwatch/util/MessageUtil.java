package com.example.xuntongwatch.util;

import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.xuntongwatch.entity.Message_;

public class MessageUtil {

	/**
	 * 发送成功的广播Intent
	 */
	public static final String SMS_SEND_ACTIOIN = "SMS_SEND_ACTIOIN";
	/**
	 * 对方接收成功的广播Intent
	 */
	public static final String SMS_DELIVERED_ACTION = "SMS_DELIVERED_ACTION";

	public static void sendMessage(Context context, Message_ message) {

		/* 建立SmsManager对象 */
		SmsManager smsManager = SmsManager.getDefault();
		String phone = message.getMessage_phone();
		String content = message.getMessage_content();
		// TODO Auto-generated method stub
		try {
			/* 建立自定义Action常数的Intent(给PendingIntent参数之用) */
			Intent itSend = new Intent(SMS_SEND_ACTIOIN);
			itSend.putExtra("message_phone", message.getMessage_phone());
			itSend.putExtra("message_content", message.getMessage_content());
			itSend.putExtra("message_time", message.getMessage_time());
			itSend.putExtra("message_state", message.getMessage_state());
			itSend.putExtra("message_see", message.getMessage_see());
			itSend.putExtra("message_id", message.getMessage_id());
			// itSend.putExtra("message_phone", phone);
			// itSend.putExtra("message_content", content);
			// long time = System.currentTimeMillis();
			// itSend.putExtra("message_time", time);
			// itSend.putExtra("message_state", Message_.SEND);
			// itSend.putExtra("message_see", Message_.SEE_HAS);

			Intent itDeliver = new Intent(SMS_DELIVERED_ACTION);

			Log.e("", "发送的message_phone == " + message.getMessage_phone());
			Log.e("", "发送的message_conten == " + message.getMessage_content());

			/*
			 * sentIntent参数为传送后接受的广播信息PendingIntent 最后一个参数 表示
			 * PendingIntent.FLAG_ONE_SHOT 表示 这个Intent只使用一次。
			 */
			PendingIntent mSendPI = PendingIntent.getBroadcast(
					context.getApplicationContext(), 0, itSend,
					PendingIntent.FLAG_ONE_SHOT);

			/* deliveryIntent参数为送达后接受的广播信息PendingIntent */
			PendingIntent mDeliverPI = PendingIntent.getBroadcast(
					context.getApplicationContext(), 0, itDeliver,
					PendingIntent.FLAG_ONE_SHOT);

			/* 发送SMS短信，注意倒数的两个PendingIntent参数 */
			if (content.length() > 70) {
				List<String> contents = smsManager.divideMessage(content);
				for (int i = 0; i < contents.size(); i++) {
					String str = contents.get(i);
					if (i == contents.size()) {
						smsManager.sendTextMessage(phone, null, str, mSendPI,mDeliverPI);
					} else {
						smsManager.sendTextMessage(phone, null, str, null, null);
					}
				}
			} else {
				smsManager.sendTextMessage(phone, null, content, mSendPI,mDeliverPI);
			}
		} catch (Exception e) {

		}
	}

}
