package com.example.xuntongwatch.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.entity.Message_;
import com.example.xuntongwatch.util.Utils;

public class Message_Chat_Adapter extends BaseAdapter {

	private ArrayList<Message_> list;
	private Context context;
	private boolean isSucceed = false;
	private boolean isFail = false;

	public Message_Chat_Adapter(Context context, ArrayList<Message_> list) {
		this.list = list;
		this.context = context;

	}

	public Message_Chat_Adapter(Context context, ArrayList<Message_> list, boolean isSucceed) {
		this.list = list;
		this.context = context;
		this.isSucceed = isSucceed;

	}

	public Message_Chat_Adapter(boolean isFail, Context context, ArrayList<Message_> list) {
		this.list = list;
		this.context = context;
		this.isFail = isFail;

	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Message_Chat_Holder holder = null;
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.message_chat_item, null);
			holder = new Message_Chat_Holder();
			holder.me_ll = (LinearLayout) convertView.findViewById(R.id.message_chat_item_me_ll);
			holder.other_ll = (LinearLayout) convertView
					.findViewById(R.id.message_chat_item_other_ll);
			holder.other_content = (TextView) convertView
					.findViewById(R.id.message_chat_item_other_content);

			holder.me_content = (TextView) convertView
					.findViewById(R.id.message_chat_item_me_content);
			holder.time = (TextView) convertView.findViewById(R.id.message_chat_item_time);
			holder.sending = (TextView) convertView.findViewById(R.id.sending);
			convertView.setTag(holder);
		} else {
			holder = (Message_Chat_Holder) convertView.getTag();
		}
		Message_ msg = list.get(position);
		String type = msg.getMessage_state();
		// 1月6号，周二，20：45"
		String content = msg.getMessage_content();
		long date = msg.getMessage_time();
		int[] timeI = Utils.longDateToY_M_D_H_m_S(date);
		String week = Utils.weekNumberToString(timeI[Utils.WEEK]);
		String time = (timeI[Utils.MONTH] + 1) + "月" + Utils.getDoubleInt(timeI[Utils.DAY]) + "号，周"
				+ week + "，" + Utils.getDoubleInt(timeI[Utils.HOUR]) + "："
				+ Utils.getDoubleInt(timeI[Utils.MINUTE]);
		Log.e("", "type == " + type);
		if (type.equals(Message_.RECEIVE)) {
			holder.other_ll.setVisibility(View.VISIBLE);
			holder.me_ll.setVisibility(View.GONE);
			holder.other_content.setText(content);
			holder.time.setText(time);
		} else if (type.equals(Message_.SEND)) {
			convertView.findViewById(R.id.hint).setVisibility(View.GONE);
			convertView.findViewById(R.id.sending).setVisibility(View.GONE);
			if (isSucceed&&position==list.size()-1) {
				
				convertView.findViewById(R.id.sending).setVisibility(View.VISIBLE);
			}
			holder.other_ll.setVisibility(View.GONE);
			holder.me_ll.setVisibility(View.VISIBLE);
			holder.me_content.setText(content);
			holder.time.setText(time);
		} else if (type.equals("5")) {

			convertView.findViewById(R.id.hint).setVisibility(View.VISIBLE);

			holder.other_ll.setVisibility(View.GONE);
			holder.me_ll.setVisibility(View.VISIBLE);
			holder.me_content.setText(content);
			holder.time.setText(time);
		}
		return convertView;
	}

	public class Message_Chat_Holder {
		LinearLayout other_ll, me_ll;
		TextView other_content, me_content, time, sending;
	}

}
