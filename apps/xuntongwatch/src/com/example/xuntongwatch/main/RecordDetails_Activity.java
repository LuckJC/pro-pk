package com.example.xuntongwatch.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.data.RecordDbUtil;
import com.example.xuntongwatch.entity.Contact;
import com.example.xuntongwatch.entity.PhoneRecord;
import com.example.xuntongwatch.entity.Record_;
import com.example.xuntongwatch.util.Constant;
import com.example.xuntongwatch.util.PhoneRecordUtil;
import com.example.xuntongwatch.util.Utils;

public class RecordDetails_Activity extends BaseActivity implements OnClickListener {

	private TextView name, msg, phone;
	private ListView lv;
	private RelativeLayout call, message, back;
	private LinearLayout delete;
	private ImageView head;
	private ArrayList<Record_> list;
	private RecordDetailsAdapter adapter;
	private RecordDbUtil recordUtil;
	private String record_phone, contact_name, contact_head,photo_uri;
	public static final int RESULT_CODE = 1114;
	private long currentTime, todayZeroTime, yesterdayZeroTime;
	private boolean isDelete = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_details);
		recordUtil = new RecordDbUtil(this);
		head = (ImageView) this.findViewById(R.id.record_details_iv);
		LinearLayout.LayoutParams params = (LayoutParams) head.getLayoutParams();
		params.width = Constant.screenWidth / 4;
		params.height = Constant.screenWidth / 4;
		head.setLayoutParams(params);
		name = (TextView) this.findViewById(R.id.record_details_name);
		msg = (TextView) this.findViewById(R.id.record_details_msg);
		phone = (TextView) this.findViewById(R.id.record_details_phone);
		lv = (ListView) this.findViewById(R.id.record_details_lv);
		call = (RelativeLayout) this.findViewById(R.id.record_details_call_rl);
		back = (RelativeLayout) this.findViewById(R.id.record_details_back);
		message = (RelativeLayout) this.findViewById(R.id.record_details_message_rl);
		delete = (LinearLayout) this.findViewById(R.id.record_details_delete_linear);
		back.setOnClickListener(this);
		message.setOnClickListener(this);
		call.setOnClickListener(this);
		delete.setOnClickListener(this);
		initCurrentTime();

		Intent intent = this.getIntent();
		if (intent != null) {
			Bitmap bmp = null;
			record_phone = intent.getStringExtra("record_phone");
			contact_name = intent.getStringExtra("contact_name");
			photo_uri = intent.getStringExtra("contact_head");
			 
			phone.setText(record_phone);
			if (!TextUtils.isEmpty(contact_name)) {
				name.setText(contact_name);
			} else {
				name.setText(record_phone);
			}
			if (!TextUtils.isEmpty(photo_uri)) {
				Uri uri = Uri.parse(photo_uri);
				try {

					bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri)
							.copy(Bitmap.Config.RGB_565, true);

				} catch (Exception e) {
					Log.e("", "头像的  Uri不存在 ");
				}
			} else {
				bmp = BitmapFactory.decodeResource(getResources(), R.drawable.image_men).copy(
						Bitmap.Config.RGB_565, true);

				// view.setb
			}
			
		 
		 
			ImageView iv = (ImageView) findViewById(R.id.record_details_iv);
			iv.setImageBitmap(bmp);
//			if (!TextUtils.isEmpty(contact_head)) {
//				File file = new File(contact_head);
//				if (file.exists()) {
//					Bitmap bitmap = BitmapFactory.decodeFile(contact_head);
//					if (bitmap != null) {
//						head.setImageBitmap(bitmap);
//					}
//				}
//			}
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		initArrayList();
		if (adapter == null) {
			adapter = new RecordDetailsAdapter();
		}
		lv.setAdapter(adapter);
	}

	private void initArrayList() {
		list = recordUtil.findRecordByPhone(record_phone);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.record_details_back:
			Intent intent = null;
			int record_id = -1;
			if (getContactNameByPhoneNumber(RecordDetails_Activity.this, record_phone)) {
				intent = new Intent(RecordDetails_Activity.this, EditContactActivity.class);
			} else {
				intent = new Intent(RecordDetails_Activity.this, Add_Contact_Activity.class);
			}
			List<Contact> listC =new ArrayList<Contact>();
			String[] selection = new String[]{
					Phone._ID,Phone.DISPLAY_NAME,Phone.NUMBER,Phone.PHOTO_URI,Phone.RAW_CONTACT_ID
			};
			Cursor cursor = getContentResolver().query(Phone.CONTENT_URI, selection, null, null, null);
			while(cursor.moveToNext())
			{
				int contact_id = cursor.getInt(cursor.getColumnIndex(Phone._ID));
				String contact_phone = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
				String contact_name = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
				
				String photo_uri = cursor.getString(cursor.getColumnIndex(Phone.PHOTO_URI));
				
				int rawContact_id = cursor.getInt(cursor.getColumnIndex(Phone.RAW_CONTACT_ID));
				Contact contact = new Contact(contact_id, contact_phone, contact_name, null, rawContact_id,photo_uri);
				listC.add(contact);
			}
			
			
			
			for (int i = 0; i < listC.size(); i++) {
				if (listC.get(i).getContact_phone().equals(record_phone)) {
					record_id = listC.get(i).getRawContact_id();
					break;
				}
			}
		 
			intent.putExtra("contact_name", contact_name);
			intent.putExtra("contact_phone", record_phone);
			intent.putExtra("contact_id", record_id);
			intent.putExtra("new_contact_phone", record_phone);
//			intent.putExtra("contact_head", contact_head);
			startActivity(intent);
			break;
		case R.id.record_details_delete_linear:
			PhoneRecordUtil.deleteRecordByNumber(this, record_phone);
			recordUtil.deleteRecordByPhone(record_phone);
			list.clear();
			adapter.notifyDataSetChanged();
			isDelete = true;
			finish();
			break;
		case R.id.record_details_call_rl:// 打电话
			Intent phoneIntent = new Intent("android.intent.action.CALL", Uri.parse("tel:"
					+ record_phone));
			startActivity(phoneIntent);
			break;
		case R.id.record_details_message_rl:// 发短信
			Intent intent1 = new Intent(this, Message_Chat_Activity.class);
			intent1.putExtra("state", Message_Chat_Activity.ONE);
			intent1.putExtra("contact_name", contact_name);
			intent1.putExtra("message_phone", record_phone);
			startActivity(intent1);
			break;
		}
	}

	public class RecordDetailsAdapter extends BaseAdapter {

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
			RecordDetailsHolder holder = null;
			if (convertView == null) {
				convertView = LayoutInflater.from(RecordDetails_Activity.this).inflate(
						R.layout.record_details_item, null);
				holder = new RecordDetailsHolder();
				holder.image = (ImageView) convertView.findViewById(R.id.record_details_item_image);
				holder.style = (TextView) convertView.findViewById(R.id.record_details_item_style);
				holder.time = (TextView) convertView.findViewById(R.id.record_details_item_time);
				convertView.setTag(holder);
			} else {
				holder = (RecordDetailsHolder) convertView.getTag();
			}

			Record_ record = list.get(position);
			long record_time = record.getRecord_time();
			long record_when = record.getRecord_when();
			String record_state = record.getRecord_state();

			if (record_state.equals("1")) {// 呼入
				holder.image.setImageResource(R.drawable.call_in);
			} else if (record_state.equals("2")) {// 呼出
				holder.image.setImageResource(R.drawable.call_out);
			} else if (record_state.equals("3")) {// 未接
				holder.image.setImageResource(R.drawable.call_no_joint);
			}

			if (record_when > 0) {
				long hourTime = (long) 1000 * (long) 60 * (long) 60;
				long minuteTime = (long) 1000 * (long) 60;
				long secondTime = (long) 1000;
				long hour = record_when / hourTime;
				record_when = record_when - hour * hourTime;
				long minute = record_when / minuteTime;
				record_when = record_when - minute * minuteTime;
				long second = record_when / secondTime;
				StringBuffer sb = new StringBuffer("");
				if (record_state.equals("1")) {// 呼入
					sb.append("呼入");
				} else if (record_state.equals("2")) {// 呼出
					sb.append("呼出");
				}
				if (hour != 0) {
					sb.append(hour + "小时");
				}
				if (minute != 0) {
					sb.append(minute + "分钟");
				}
				sb.append(second + "秒");
				holder.style.setText(sb.toString());
			} else {
				holder.style.setText("未接通");
			}

			if (record_time != 0) {
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(record_time);
				int month = c.get(Calendar.MONDAY) + 1;
				int day = c.get(Calendar.DAY_OF_MONTH);
				int hour = c.get(Calendar.HOUR_OF_DAY);
				int minute = c.get(Calendar.MINUTE);
				if (record_time > todayZeroTime || record_time == todayZeroTime) {// 今天
					Log.e("", "record_time == " + record_time);
					Log.e("", "currentTime == " + currentTime);
					long t1 = currentTime - record_time;
					if (t1 < (long) 1000 * (long) 60 * (long) 24) {// 一小时内
						Log.e("", "t1  ==  " + t1);
						long time = t1 / (long) (1000 * 60);
						if(time==0){
							holder.time.setText("刚刚");
						}else{
						holder.time.setText(time + "分钟前");}
					} else {
						holder.time.setText(Utils.getDoubleInt(hour) + ":"
								+ Utils.getDoubleInt(minute));
					}
				} else if (record_time < todayZeroTime || record_time > yesterdayZeroTime) {// 昨天
					holder.time.setText("昨天" + Utils.getDoubleInt(hour) + ":"
							+ Utils.getDoubleInt(minute));
				} else {
					holder.time.setText(Utils.getDoubleInt(month) + "." + Utils.getDoubleInt(day));
				}

			}

			return convertView;
		}

	}

	private void initCurrentTime() {
		Calendar c = Calendar.getInstance();
		currentTime = c.getTimeInMillis();
		c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		todayZeroTime = c.getTimeInMillis();
		c.set(Calendar.DAY_OF_MONTH, -1);
		yesterdayZeroTime = c.getTimeInMillis();
	}

	public class RecordDetailsHolder {
		TextView time, style;
		ImageView image;
	}

	public void finish() {
		Intent intent = new Intent();
		intent.putExtra("isDelete", isDelete);
		setResult(RESULT_CODE, intent);
		super.finish();
	}

	public static boolean getContactNameByPhoneNumber(Context context, String address) {
		String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER };

		// 将自己添加到 msPeers 中
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, // Which
																				// columns
																				// to
																				// return.
				ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + address + "'", // WHERE
																						// clause.
				null, // WHERE clause value substitution
				null); // Sort order.

		if (cursor == null || cursor.getCount() <= 0) {
			return false;
		} else {
			return true;
		}
	}
 
}
