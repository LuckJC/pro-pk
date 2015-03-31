package com.example.xuntongwatch.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.xuntongwatch.data.MessageDbUtil;
import com.example.xuntongwatch.util.MessageUtil;

public class MessageSend extends BroadcastReceiver {
	
	private MessageSendInterface face;
	private final int SUCCESS = 113;
	
	public MessageSend(MessageSendInterface face){
		this.face = face;
	}
	
	public MessageSend(){
		
	}
	
	private Handler handler = new Handler()
	{

		@Override
		public void handleMessage(Message msg) {
			if(msg.what == SUCCESS){
				if(face != null)
				{
					face.success();
				}
			}
			super.handleMessage(msg);
		}
		
	};
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(MessageUtil.SMS_SEND_ACTIOIN)) {
			
//			if(face != null)
//			{
//				return;
//			}
			
			Log.e("", "短信发送成功~~~~~~~~~~~~~~");
			String message_phone = intent.getStringExtra("message_phone");
			String message_content = intent.getStringExtra("message_content");
			long message_time = intent.getLongExtra("message_time", -1l);
			String message_state = intent.getStringExtra("message_state");
			String message_see = intent.getStringExtra("message_see");
			String message_send_ok = "1";
			final int message_id = intent.getIntExtra("message_id", -1);

			message_phone = message_phone.replace("+86", "");

			// final Message_ msg = new Message_();
			// msg.setMessage_phone(message_phone);
			// msg.setMessage_content(message_content);
			// msg.setMessage_time(message_time);
			// msg.setMessage_see(message_see);
			// msg.setMessage_state(message_state);
			// msg.setMessage_send_ok(message_send_ok);
			// Log.e("", "发送的成功返回的message_conten == " + message_content);
			// Log.e("", "发送的成功返回的message_phone == " + message_phone);

			final MessageDbUtil util = new MessageDbUtil(context);

			new Thread(new Runnable() {
				@Override
				public void run() {
					// Log.e("", "发送的短信插入数据库成功");
					// util.insertInto(msg);
					util.updateMessage_send_okByMessage_id(message_id);
					Message msg = Message.obtain();
					msg.what = SUCCESS;
					handler.sendMessage(msg);
				}
			}).start();

		}
	}
	
	public interface MessageSendInterface{
		public void success();
	}
}
