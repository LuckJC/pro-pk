package com.example.xuntongwatch.main;

import java.util.ArrayList;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.abstract_.DatabaseUpdataActivity;
import com.example.xuntongwatch.contentobserver.ObserverUtil;
import com.example.xuntongwatch.contentobserver.ObserverUtil.ObserverUtilInterface;
import com.example.xuntongwatch.databaseutil.SmsUtil;
import com.example.xuntongwatch.entity.Message_Thread;
import com.example.xuntongwatch.util.Utils;

public class Message_Activity extends DatabaseUpdataActivity implements OnClickListener {

	private ListView lv;
	private ArrayList<Message_Thread> arrayList;
	private MessageAdapter adapter;
	private LinearLayout delete_title;
	private TextView delete_cancle, delete_number, delete_select_all;
	private ImageView bottomImage;
	private int today, yesterday;
	private ArrayList<Integer> selectList;
	private boolean isEnterDeleteMode = false;// 是否进入删除模式
	private final int WHAT = 11111111;
	
	@SuppressLint("NewApi") @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message);
		lv = (ListView) this.findViewById(R.id.message_lv);
		delete_title = (LinearLayout) this
				.findViewById(R.id.message_delete_title_ll);
		delete_cancle = (TextView) this
				.findViewById(R.id.message_delete_cancle);
		delete_select_all = (TextView) this
				.findViewById(R.id.message_delete_select_all);
		delete_number = (TextView) this
				.findViewById(R.id.message_delete_number);
		bottomImage = (ImageView) this.findViewById(R.id.message_bottom_image);
		bottomImage.setOnClickListener(this);
		delete_select_all.setOnClickListener(this);
		delete_cancle.setOnClickListener(this);

		Calendar c = Calendar.getInstance();
		today = c.get(Calendar.DAY_OF_MONTH);
		long ll = (long) 24 * (long) 60 * (long) 60 * (long) 1000;
		c.setTimeInMillis(c.getTimeInMillis() - ll);
		yesterday = c.get(Calendar.DAY_OF_MONTH);

		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (isEnterDeleteMode)
					return false;// 判断是否已经 是删除模式，如果是就不作处理。
				if (selectList == null) {
					selectList = new ArrayList<Integer>();
				}
				selectList.clear();
				Message_Thread msg = arrayList.get(position);
				selectList.add(msg.getThread_id());
				enterDeleteMode();// 进入删除模式
				delete_number.setText("已选择" + selectList.size() + "项");
				return false;
			}
		});

		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (isEnterDeleteMode)// 删除模式
				{
					Message_Thread msg = arrayList.get(position);
					int thread_id = msg.getThread_id();
					if (selectList.contains(msg))// 表示已选中了。
					{
						selectList.remove(thread_id);
						selectOff(view);
						delete_number.setText("已选择" + selectList.size() + "项");
					} else {
						selectList.add(thread_id);
						selectOn(view);
						delete_number.setText("已选择" + selectList.size() + "项");
					}
				} else// 正常模式
				{
					Message_Thread message = arrayList.get(position);
					Intent intent = new Intent(Message_Activity.this,
							Message_Chat_Activity.class);
//					intent.putExtra("message_id", message.getThread_id());
//					intent.putExtra("state", Message_Chat_Activity.ONE);
//					intent.putExtra("contact_name", message.getContact_name());
//					intent.putExtra("message_phone", message.getMessage_phone());
					
					intent.putExtra("thread_id", message.getThread_id());
					intent.putExtra("state", Message_Chat_Activity.ONE);
					intent.putExtra("contact_name", message.getName());
					intent.putExtra("message_phone", message.getPhone());
					Message_Activity.this.startActivity(intent);
				}
			}
		});
		
		super.handleObserverUtil(Sms.CONTENT_URI);//注册监听器 （数据库改变监听器）

	}

	private void selectOn(View view)// 选 中
	{
		ImageView select_v = (ImageView) view
				.findViewById(R.id.message_item_select_iv);
		select_v.setImageResource(R.drawable.delete_select_on);
	}

	private void selectOff(View view)// 没选中
	{
		ImageView select_v = (ImageView) view
				.findViewById(R.id.message_item_select_iv);
		select_v.setImageResource(R.drawable.delete_select_off);
	}

	/**
	 * 进入删除模式
	 */
	private void enterDeleteMode() {
		isEnterDeleteMode = true;
		adapter.notifyDataSetChanged();
		showDeleteTitle();

	}

	/**
	 * 进入正常模式
	 */
	private void enterNormalMode() {
		isEnterDeleteMode = false;
		adapter.notifyDataSetChanged();
		hideDeleteTitle();
	}

	public void onResume() {
		super.onResume();
		if (arrayList != null) {
			return;
		}
//		initMessageDbUtil();
//		arrayList = msgUtil.findAllMessageGroupByPhone();
		arrayList = SmsUtil.allMessage_Thread(this);
//		contactUtil.findAllContact();
		adapter = new MessageAdapter();
		lv.setAdapter(adapter);
			
	}

	public void back() {
		finish();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.message_bottom_image:
			if (delete_title.getVisibility() == View.VISIBLE)// 删除信息
			{
//				ArrayList<String> phones = new ArrayList<String>();
//				for (int i = 0; i < selectList.size(); i++) {
//					Message_Thread msg = selectList.get(i);
//					String phone = msg.getMessage_phone();
//					String phone = msg.getPhone();
//					int thread_id = selectList.get(i);
//					phones.add(phone);
//					arrayList.remove(msg);
//				}
//				msgUtil.deleteMsg(phones);
				SmsUtil.deleteMessageByThread_id(this, selectList);
				enterNormalMode();
			} else if (delete_title.getVisibility() == View.GONE)// 写信息
			{
				Intent intent = new Intent(this, Message_Chat_Activity.class);
				intent.putExtra("state", Message_Chat_Activity.TWO);
				this.startActivity(intent);
			}

			break;
		case R.id.message_delete_select_all:// 全选
			selectList.clear();
			for (int i = 0; i < arrayList.size(); i++) {
				Message_Thread msg = arrayList.get(i);
				int thread_id = msg.getThread_id();
				selectList.add(thread_id);
			}
			adapter.notifyDataSetChanged();
			delete_number.setText("已选择" + selectList.size() + "项");
			break;
		case R.id.message_delete_cancle:// 取消
			enterNormalMode();
			break;
		}
	}

	public class MessageAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return arrayList.size();
		}

		@Override
		public Object getItem(int position) {
			return arrayList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MessageHolder holder = null;
			if (convertView == null) {
				holder = new MessageHolder();
				convertView = LayoutInflater.from(Message_Activity.this).inflate(R.layout.message_item, null);
				holder.name = (TextView) convertView.findViewById(R.id.message_item_name_tv);
				holder.head = (ImageView) convertView.findViewById(R.id.message_item_head_iv);
				holder.time = (TextView) convertView.findViewById(R.id.message_item_time_tv);
				holder.content = (TextView) convertView.findViewById(R.id.message_item_content_tv);
				holder.select = (ImageView) convertView.findViewById(R.id.message_item_select_iv);
				convertView.setTag(holder);
			} else {
				holder = (MessageHolder) convertView.getTag();
			}

			final Message_Thread message = arrayList.get(position);
			if (isEnterDeleteMode) {
				holder.select.setImageResource(R.drawable.delete_select_off);
				if (selectList.contains(message.getThread_id())) {
					holder.select.setImageResource(R.drawable.delete_select_on);
				}
				holder.select.setVisibility(View.VISIBLE);
				holder.time.setVisibility(View.GONE);
			} else {
				holder.select.setVisibility(View.GONE);
				holder.time.setVisibility(View.VISIBLE);
			}

//			holder.name.setText(message.getContact_name());
//			holder.content.setText(message.getMessage_content());
//			long date = message.getMessage_time();
			if(TextUtils.isEmpty(message.getName()))
			{
				holder.name.setText(message.getPhone());
			}else
			{
				holder.name.setText(message.getName());
			}
			holder.content.setText(message.getSnippet());
			long date = message.getDate();
			int[] i = Utils.longDateToY_M_D_H_m_S(date);
			String time = "";
			if (i[Utils.DAY] == today) {
				time = i[Utils.HOUR] + ":"
						+ Utils.getDoubleInt(i[Utils.MINUTE]);
			} else if (i[2] == yesterday) {
				time = "昨天  " + i[Utils.HOUR] + ":"
						+ Utils.getDoubleInt(i[Utils.MINUTE]);
			} else {
				time = (i[Utils.MONTH] + 1) + "月"
						+ Utils.getDoubleInt(i[Utils.DAY]) + "号";
			}
			holder.time.setText(time);
			
			byte[] photo = message.getPhoto();
			if(photo != null)
			{
				Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
				if(bitmap != null)
				{
					holder.head.setImageBitmap(bitmap);
				}
			}else
			{
				holder.head.setImageResource(R.drawable.image_men);
			}
			
			return convertView;
		}

	}

	public class MessageHolder {
		ImageView head, select;
		TextView name, time, content;
	}

	public void showDeleteTitle() {
		delete_title.setVisibility(View.VISIBLE);
		Animation anim = AnimationUtils.loadAnimation(this,
				R.anim.up_to_down_show);
		delete_title.setAnimation(anim);
		bottomImage.setImageResource(R.drawable.message_delete);
	}

	public void hideDeleteTitle() {
		delete_title.setVisibility(View.GONE);
		Animation anim = AnimationUtils.loadAnimation(this,
				R.anim.down_to_up_hide);
		delete_title.setAnimation(anim);
		bottomImage.setImageResource(R.drawable.message_edit);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isEnterDeleteMode) {
				enterNormalMode();
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void operation() {
		arrayList = SmsUtil.allMessage_Thread(Message_Activity.this);
	}

	@Override
	public void update() {
		adapter.notifyDataSetChanged();
	}
	
	
}
