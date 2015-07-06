package com.example.xuntongwatch.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.xuntongwatch.MyApplication;
import com.example.xuntongwatch.data.MessageDbUtil;
import com.example.xuntongwatch.databaseutil.SmsUtil;
import com.example.xuntongwatch.entity.Message_;

public class MessageReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {

		// android.provider.Telephony.SMS_RECEIVED
		Bundle bundle = intent.getExtras();
		if (bundle == null)
			return;
		Log.e("", "有短信来了");
		Object[] pduses = (Object[]) bundle.get("pdus");
		for (Object pdus : pduses) {
			byte[] pdusmessage = (byte[]) pdus;
			SmsMessage sms = SmsMessage.createFromPdu(pdusmessage);
			String mobile = sms.getOriginatingAddress();// 发送短信的手机号码
			String content = sms.getMessageBody(); // 短信内容
			long time = sms.getTimestampMillis();// 发送的时间

			mobile = mobile.replace("+86", "");

			final Message_ msg = new Message_();
			msg.setMessage_content(content);
			msg.setMessage_phone(mobile);
			msg.setMessage_time(time);
			msg.setMessage_state(Message_.RECEIVE);
			msg.setMessage_see(Message_.SEE_NONE);
			SmsUtil.insertMessageIntoInbox(context, msg);
			// final MessageDbUtil util = new MessageDbUtil(context);
			Log.e("", "接收的message_conten == " + content);
			Log.e("", "接收的message_phone == " + mobile);
			// new Thread(new Runnable() {
			// @Override
			// public void run() {
			// Log.e("", "接收的短信插入数据库成功");
			// util.insertInto(msg);
			// }
			// }).start();
			Message receiver = Message.obtain();
			receiver.what = 110;
			Bundle bundles = new Bundle();
			bundles.putSerializable("msg", msg);
			receiver.setData(bundles);
			if (MyApplication.handler != null) {
				MyApplication.handler.sendMessage(receiver);
			}
		}

	}
}
