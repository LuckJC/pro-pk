package com.example.xuntongwatch.main;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.adapter.Message_Chat_Adapter;
import com.example.xuntongwatch.data.MessageDbUtil;
import com.example.xuntongwatch.entity.Message_;
import com.example.xuntongwatch.util.MessageUtil;

public class Send_Message_Activity extends BaseActivity implements
		OnClickListener {

	private RelativeLayout add_person, send_message;
	private ListView lv;
	private ArrayList<Message_> list;
	private Message_Chat_Adapter adapter;
	private EditText recieverPerson,content;
	private MessageDbUtil msgUtil;
	private Handler handler;
	private final int SEND_MSG = 00;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_message);
		add_person = (RelativeLayout) this.findViewById(R.id.send_message_add_person);
		send_message = (RelativeLayout) this.findViewById(R.id.send_message_send);
		lv = (ListView) this.findViewById(R.id.send_message_lv);
		recieverPerson = (EditText) this.findViewById(R.id.send_message_person_et);
		content = (EditText) this.findViewById(R.id.send_message_content_et);
		add_person.setOnClickListener(this);
		send_message.setOnClickListener(this);
		msgUtil = new MessageDbUtil(this);
		
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SEND_MSG:
					Bundle b = msg.getData();
					Message_ message = (Message_) b.getSerializable("message");
					list.add(message);
					adapter.notifyDataSetChanged();
					break;
				}
				super.handleMessage(msg);
			}

		};
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.send_message_add_person:
			
			break;
		case R.id.send_message_send:
			String msg = content.getText().toString();
			String message_phone = recieverPerson.getText().toString().trim();
			if (!TextUtils.isEmpty(msg) && !TextUtils.isEmpty(message_phone)) {
				final Message_ message = new Message_();
				message.setMessage_content(msg);
				message.setMessage_phone(message_phone);
				message.setMessage_see(Message_.SEE_HAS);
				message.setMessage_state(Message_.SEND);
				long time = System.currentTimeMillis();
				message.setMessage_send_ok("2");
				message.setMessage_time(time);
				final int message_id = msgUtil.insertInto(message);
				new Thread(new Runnable() {
					@Override
					public void run() {
						message.setMessage_id(message_id);
						MessageUtil.sendMessage(Send_Message_Activity.this,
								message);
						sendMsg(message, SEND_MSG);
					}
				}).start();
			}
			content.setText(null);
			break;
		}
	}
	
	private void sendMsg(Message_ message, int what) {
		Message msg = Message.obtain();
		Bundle b = new Bundle();
		b.putSerializable("message", message);
		msg.setData(b);
		msg.what = what;
		handler.sendMessage(msg);
	}

	private void initListView() {
		list = new ArrayList<Message_>();
		adapter = new Message_Chat_Adapter(this, list);
		lv.setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		initListView();
	}

}
