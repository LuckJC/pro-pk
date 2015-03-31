package com.example.xuntongwatch.main;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.abstract_.DatabaseUpdataActivity;
import com.example.xuntongwatch.adapter.Message_Chat_Adapter;
import com.example.xuntongwatch.contentobserver.ObserverUtil;
import com.example.xuntongwatch.contentobserver.ObserverUtil.ObserverUtilInterface;
import com.example.xuntongwatch.data.MessageDbUtil;
import com.example.xuntongwatch.databaseutil.SmsUtil;
import com.example.xuntongwatch.entity.Message_;
import com.example.xuntongwatch.receiver.MessageSend;
import com.example.xuntongwatch.receiver.MessageSend.MessageSendInterface;
import com.example.xuntongwatch.util.MessageUtil;
import com.example.xuntongwatch.util.PreferenceOperation;

public class Message_Chat_Activity extends DatabaseUpdataActivity implements
		OnClickListener {

	private LinearLayout title_one,title_two;
	private TextView name;
	private ListView lv;
	private ArrayList<Message_> list;
	private String contact_name, message_phone;
	private Message_Chat_Adapter adapter;
	private RelativeLayout send;
	private Button edit;
	private EditText et,et_phone;
	private final int SEND_MSG = 111;
	private int thread_id = -1;
	
	public static final int ONE = 1;
	public static final int TWO = 2;;
	
	@SuppressLint({ "HandlerLeak", "NewApi" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceOperation.getInstant(this);
		setContentView(R.layout.message_chat);
		name = (TextView) this.findViewById(R.id.message_chat_name);
		et = (EditText) this.findViewById(R.id.message_chat_et);
		lv = (ListView) this.findViewById(R.id.message_chat_lv);
		send = (RelativeLayout) this.findViewById(R.id.message_chat_send_rl);
		edit = (Button) this.findViewById(R.id.message_chat_edit);
		title_one = (LinearLayout) this.findViewById(R.id.message_chat_title);
		title_two = (LinearLayout) this.findViewById(R.id.message_chat_top_ll);
		et_phone = (EditText) this.findViewById(R.id.message_chat_person_et);
		edit.setOnClickListener(this);
		send.setOnClickListener(this);
		Intent intent = this.getIntent();
		if (intent != null) {
			int state = intent.getIntExtra("state", 0);
			if(state == ONE){
				title_two.setVisibility(View.GONE);
				contact_name = intent.getStringExtra("contact_name");
				message_phone = intent.getStringExtra("message_phone");
				thread_id = intent.getIntExtra("thread_id", -1);
				if (!TextUtils.isEmpty(contact_name)) {
					name.setText(contact_name);
				} else {
					name.setText(message_phone);
				}
			}else if(state == TWO)
			{
				title_one.setVisibility(View.GONE);
			}
		}
		String msg = PreferenceOperation.getMessage(message_phone);
		if (!TextUtils.isEmpty(msg)) {
			et.setText(msg);
		}
		super.handleObserverUtil(Sms.CONTENT_URI);//注册 数据库改变监听器
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (list == null) {
			if(thread_id == -1)
			{
				list = SmsUtil.findMessageByPhone(this, message_phone);
			}else
			{
				list = SmsUtil.findMessageByThread_id(this, thread_id);
			}
			adapter = new Message_Chat_Adapter(this, list);
			lv.setAdapter(adapter);
			lv.setSelection(list.size() - 1);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.message_chat_send_rl:
			String msg = et.getText().toString();
			if (!TextUtils.isEmpty(msg)) {
				final Message_ message = new Message_();
				message.setMessage_content(msg);
				message.setMessage_phone(message_phone);
				message.setMessage_see("1");
				message.setMessage_state("-1");
				long time = System.currentTimeMillis();
				message.setMessage_time(time);
				SmsUtil.insertMessageIntoSent(this, message);//插入发件箱
				MessageUtil.sendMessage(Message_Chat_Activity.this,message);//发送短信
			}
			et.setText(null);
			break;
		case R.id.message_chat_edit:

			break;
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			String text = et.getText().toString().trim();
			if (!TextUtils.isEmpty(text)) {
				PreferenceOperation.putMessage(message_phone, text);
			} else {
				PreferenceOperation.clearMessage(message_phone);
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void operation() {
		if(thread_id == -1)
		{
			list = SmsUtil.findMessageByPhone(Message_Chat_Activity.this, message_phone);
		}else
		{
			list = SmsUtil.findMessageByThread_id(Message_Chat_Activity.this, thread_id);
		}
	}

	@Override
	public void update() {
		adapter.notifyDataSetChanged();
	}
	
}
