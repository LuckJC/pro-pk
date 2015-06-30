package com.example.xuntongwatch.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.xuntongwatch.MyApplication;
import com.example.xuntongwatch.data.MessageDbUtil;
import com.example.xuntongwatch.databaseutil.SmsUtil;
import com.example.xuntongwatch.util.MessageUtil;

public class MessageSend extends BroadcastReceiver {

	private MessageSendInterface face;
	private final int SUCCESS = 113;
	private Context context;

	public MessageSend(MessageSendInterface face) {
		this.face = face;
	}

	public MessageSend() {

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		this.context = context;

		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			SmsUtil.updateAllOutboxToFailed(context);
		}

		if (action.equals(MessageUtil.SMS_SEND_ACTIOIN)
				|| action.equals(MessageUtil.SMS_DELIVERED_ACTION)) {

			String message_phone = intent.getStringExtra("message_phone");
			String message_content = intent.getStringExtra("message_content");
			long message_time = intent.getLongExtra("message_time", -1l);
			String message_state = intent.getStringExtra("message_state");
			String message_see = intent.getStringExtra("message_see");
			String message_send_ok = "1";
			final int message_id = intent.getIntExtra("message_id", -1);

			message_phone = message_phone.replace("+86", "");

			switch (getResultCode()) {
			case Activity.RESULT_OK:
				final MessageDbUtil util = new MessageDbUtil(context);

				new Thread(new Runnable() {
					@Override
					public void run() {
						SmsUtil.updateSmsTypeToSent(MessageSend.this.context, message_id);
						Message msg = Message.obtain();
						msg.what = 113;
						MyApplication.handler.sendMessage(msg);
					}
				}).start();

				break;

			default:
				new Thread(new Runnable() {
					@Override
					public void run() {

						SmsUtil.updateSmsTypeToFailed(MessageSend.this.context, message_id);
						Message msg = Message.obtain();
						msg.what = 112;
						MyApplication.handler.sendMessage(msg);
					}
				}).start();

				break;

			}

		}
	}

	public static interface MessageSendInterface {
		public void success();
	}
}
