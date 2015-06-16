package com.example.xuntongwatch.main;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony.Sms;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xuntongwatch.MyApplication;
import com.example.xuntongwatch.R;
import com.example.xuntongwatch.abstract_.DatabaseUpdataActivity;
import com.example.xuntongwatch.adapter.Message_Chat_Adapter;
import com.example.xuntongwatch.databaseutil.SmsUtil;
import com.example.xuntongwatch.entity.Message_;
import com.example.xuntongwatch.entity.Message_Thread;
import com.example.xuntongwatch.util.MessageUtil;
import com.example.xuntongwatch.util.PreferenceOperation;
import com.example.xuntongwatch.util.Utils;

public class Message_Chat_Activity extends DatabaseUpdataActivity implements OnClickListener {
	private String mFailSend;
	private LinearLayout title_one, title_two;
	private TextView name;
	private ListView lv;
	private ArrayList<Message_> list;
	private String contact_name, message_phone;
	private Message_Chat_Adapter adapter;
	private RelativeLayout send, mChooseContact;
	private String addname = null;
	private EditText et, et_phone;
	private final int SEND_MSG = 111;
	private int thread_id = -1;
	private int mListSize;
	public static final int ONE = 1;
	public static final int TWO = 2;;
	private int state;
	private ArrayList<Message_Thread> arrayList;
	private boolean isClear = true;
	private TextView viewadd;
	private TextView tv = null;
	private boolean delete = true;

	@SuppressLint({ "HandlerLeak", "NewApi" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceOperation.getInstant(this);
		setContentView(R.layout.message_chat);
		viewadd = (TextView) findViewById(R.id.send_contact_name);
		name = (TextView) this.findViewById(R.id.message_chat_name);
		et_phone = (EditText) this.findViewById(R.id.message_chat_person_et);
		et = (EditText) this.findViewById(R.id.message_chat_et);
		et_phone.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				if (arg3 == 0) {
					viewadd.setText("");
				}
			}

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub

			}
		});
		lv = (ListView) this.findViewById(R.id.message_chat_lv);
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(final AdapterView<?> parent, final View view,
					final int position, long id) {

				if (list.get(position).getMessage_state().equals(Message_.RECEIVE)) {
					tv = (TextView) view.findViewById(R.id.message_chat_item_other_content);
				} else if (list.get(position).getMessage_state().equals(Message_.SEND)) {
					tv = (TextView) view.findViewById(R.id.message_chat_item_me_content);
				}

				tv.setBackgroundColor(Color.RED);
				final String[] mItems = { "复制", "转发", "删除" };
				AlertDialog.Builder builder = new AlertDialog.Builder(Message_Chat_Activity.this);
				builder.setOnDismissListener(new OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						if (delete) {
							if (list.get(position).getMessage_state().equals(Message_.RECEIVE)) {
								tv.setBackgroundResource(R.drawable.message_chat_other);
							} else if (list.get(position).getMessage_state().equals(Message_.SEND)) {
								tv.setBackgroundResource(R.drawable.message_chat_me);
							}

						}
					}
				});
				builder.setItems(mItems, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							SmsUtil.copy(tv.getText().toString(), Message_Chat_Activity.this);
							break;
						case 1:
							Intent intent = new Intent(Message_Chat_Activity.this,
									Message_Chat_Activity.class);
							intent.putExtra("state", Message_Chat_Activity.TWO);
							intent.putExtra("content", tv.getText().toString());
							Message_Chat_Activity.this.startActivity(intent);

							break;
						case 2:
							// SmsUtil.deleteMessage(Message_Chat_Activity.this,
							// list.get(position));
							delete = false;
							list.remove(position);
							lv.setAdapter(adapter);
							break;
						default:
							break;
						}

					}
				});
				builder.create().show();

				return false;
			}
		});
		send = (RelativeLayout) this.findViewById(R.id.message_chat_send_rl);
		mChooseContact = (RelativeLayout) this.findViewById(R.id.message_chat_add_person);
		mChooseContact.setOnClickListener(this);
		title_one = (LinearLayout) this.findViewById(R.id.message_chat_title);
		title_two = (LinearLayout) this.findViewById(R.id.message_chat_top_ll);

		MyApplication.handler = handler;

		send.setOnClickListener(this);
		Intent intent = this.getIntent();
		if (intent != null) {
			state = intent.getIntExtra("state", 0);
			if (state == ONE) {
				title_two.setVisibility(View.GONE);
				contact_name = intent.getStringExtra("contact_name");
				message_phone = intent.getStringExtra("message_phone");
				thread_id = intent.getIntExtra("thread_id", -1);
				if (!TextUtils.isEmpty(contact_name)) {
					name.setText(contact_name);
				} else {
					name.setText(message_phone);
				}
			} else if (state == TWO) {
				et.setText(intent.getStringExtra("content"));
				title_one.setVisibility(View.GONE);
				title_two.setVisibility(View.VISIBLE);
				// et_phone.setOnClickListener(new OnClickListener() {
				//
				// @Override
				// public void onClick(View arg0) {
				// et_phone.setFocusable(true);
				// et_phone.setFocusableInTouchMode(true);
				// et_phone.requestFocus();
				//
				// }
				// });
				et_phone.setOnFocusChangeListener(new android.view.View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							// 此处为得到焦点时的处理内容
						} else {
							updataUI();
						}
					}
				});
			}
		}
		String msg = PreferenceOperation.getMessage(message_phone);
		if (!TextUtils.isEmpty(msg)) {
			et.setText(msg);
		}

		super.handleObserverUtil(Sms.CONTENT_URI);// 注册 数据库改变监听器
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (state == ONE) {
			if (list == null) {
				if (thread_id == -1) {
					list = SmsUtil.findMessageByPhone(this, message_phone);
				} else {
					list = SmsUtil.findMessageByThread_id(this, thread_id);
				}
				adapter = new Message_Chat_Adapter(this, list);
				lv.setAdapter(adapter);
				lv.setSelection(list.size() - 1);
				mListSize = list.size();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 10086) {
			if (resultCode == 9527) {
				String phone = data.getStringExtra("contact_phone");
				addname = data.getStringExtra("contact_name");
				et_phone.setText(phone);
				et.setFocusable(true);
				et.setFocusableInTouchMode(true);
				et.requestFocus();
				viewadd.setText("(" + addname + ")");
				updataUI();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		if (Utils.isFastClick()) {
			return;
		}
		switch (v.getId()) {
		case R.id.message_chat_add_person:
			Intent intent = new Intent(this, Contact_Activity.class);
			intent.putExtra("tag", "send");
			startActivityForResult(intent, 10086);
			break;
		case R.id.message_chat_send_rl:
			if (state == ONE) {
				String msg = et.getText().toString();
				if (!TextUtils.isEmpty(msg)) {
					final Message_ message = new Message_();
					message.setMessage_content(msg);
					message.setMessage_phone(message_phone);
					message.setMessage_see("1");
					message.setMessage_state("-1");
					long time = System.currentTimeMillis();
					message.setMessage_time(time);
					SmsUtil.insertMessageIntoSent(this, message);// 插入发件箱
					MessageUtil.sendMessage(Message_Chat_Activity.this, message);// 发送短信
				} else {
					Toast.makeText(this, "请填写短信内容", Toast.LENGTH_SHORT).show();
					return;
				}
				mFailSend = et.getText().toString();
				et.setText(null);
			} else {
				String msg = et.getText().toString();
				String phone = et_phone.getText().toString();
				if (TextUtils.isEmpty(phone)) {
					Toast.makeText(this, "请填写收件人", Toast.LENGTH_SHORT).show();
					return;
				}

				if (!TextUtils.isEmpty(msg)) {
					final Message_ message = new Message_();
					message.setMessage_content(msg);
					message.setMessage_phone(et_phone.getText().toString());
					message.setMessage_see("1");
					message.setMessage_state("-1");
					long time = System.currentTimeMillis();
					message.setMessage_time(time);
					SmsUtil.insertMessageIntoSent(this, message);// 插入发件箱
					MessageUtil.sendMessage(Message_Chat_Activity.this, message);// 发送短信
				} else {
					Toast.makeText(this, "请填写短信内容", Toast.LENGTH_SHORT).show();
					return;
				}
				mFailSend = et.getText().toString();
				et.setText(null);
			}

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
		if (state == ONE) {
			if (thread_id == -1) {
				if (state == ONE) {
					list = SmsUtil.findMessageByPhone(Message_Chat_Activity.this, message_phone);
				} else {
					list = SmsUtil.findMessageByPhone(Message_Chat_Activity.this, et_phone
							.getText().toString());
				}
			} else {
				list = SmsUtil.findMessageByThread_id(Message_Chat_Activity.this, thread_id);
			}
			mListSize = list.size();
		}
	}

	@Override
	public void update() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 110) {

				final Timer mQRTimer = new Timer();
				mQRTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						if (state == ONE) {
							list = SmsUtil.findMessageByThread_id(Message_Chat_Activity.this,
									thread_id);
						} else {
							list = SmsUtil.findMessageByPhone(Message_Chat_Activity.this, et_phone
									.getText().toString());
						}
						if (mListSize < list.size()) {
							Message receiver = Message.obtain();
							receiver.what = 119;
							MyApplication.handler.sendMessage(receiver);
							mQRTimer.cancel();
							mListSize = list.size();
						}

					}
				}, 0, 100);
			}
			if (msg.what == 119) {
				adapter = new Message_Chat_Adapter(Message_Chat_Activity.this, list);
				lv.setAdapter(adapter);
				lv.setSelection(list.size() - 1);
			}
			if (msg.what == 113) {

				if (thread_id == -1) {
					if (state == ONE) {
						list = SmsUtil
								.findMessageByPhone(Message_Chat_Activity.this, message_phone);
					} else {
						list = SmsUtil.findMessageByPhone(Message_Chat_Activity.this, et_phone
								.getText().toString());
					}

				} else {
					list = SmsUtil.findMessageByThread_id(Message_Chat_Activity.this, thread_id);
				}
				adapter = new Message_Chat_Adapter(Message_Chat_Activity.this, list);
				lv.setAdapter(adapter);
				lv.setSelection(list.size() - 1);
				mListSize = list.size();
			}
			if (msg.what == 112) {
				Toast.makeText(Message_Chat_Activity.this, "Failed,try again.", Toast.LENGTH_LONG)
						.show();
				et.setText(mFailSend);
			}

			super.handleMessage(msg);
		}

	};

	private void updataUI() {

		// 此处为失去焦点时的处理内容
		arrayList = SmsUtil.allMessage_Thread(Message_Chat_Activity.this);
		int thread_id = -1;
		String name = null;
		// for (Message_Thread iterable_element : arrayList)
		// {
		// if
		// (iterable_element.getPhone().equals(contact_phone))
		// {
		// thread_id = iterable_element.getThread_id();
		// return;
		// }
		// }
		String phone = et_phone.getText().toString();
		for (int i = 0; i < arrayList.size(); i++) {
			if (arrayList.get(i).getPhone().equals(phone)) {
				thread_id = arrayList.get(i).getThread_id();
				name = arrayList.get(i).getName();
				if (thread_id == -1) {
					list = SmsUtil.findMessageByPhone(Message_Chat_Activity.this, phone);
				} else {
					list = SmsUtil.findMessageByThread_id(Message_Chat_Activity.this, thread_id);
				}
				adapter = new Message_Chat_Adapter(Message_Chat_Activity.this, list);
				lv.setAdapter(adapter);
				lv.setSelection(list.size() - 1);
				isClear = false;
				mListSize = list.size();
				break;
			}

		}
		if (isClear) {
			list = new ArrayList<Message_>();
			list.clear();
			adapter = new Message_Chat_Adapter(Message_Chat_Activity.this, list);
			lv.setAdapter(adapter);
			lv.setSelection(list.size() - 1);

		}
		isClear = true;
	}

}
