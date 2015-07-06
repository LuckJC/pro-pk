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

public class RecordDetails_Activity extends BaseActivity implements
		OnClickListener {

	private TextView name, msg, phone;
	private ListView lv;
	private RelativeLayout call, message, back;
	private LinearLayout delete;
	private ImageView head;
	private ArrayList<PhoneRecord> list;
	private RecordDetailsAdapter adapter;
	private RecordDbUtil recordUtil;
	private String record_phone, contact_name, contact_head, photo_uri;
	public static final int RESULT_CODE = 1114;
	private long currentTime, todayZeroTime, yesterdayZeroTime;
	private boolean isDelete = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_details);
		recordUtil = new RecordDbUtil(this);
		head = (ImageView) this.findViewById(R.id.record_details_iv);
		LinearLayout.LayoutParams params = (LayoutParams) head
				.getLayoutParams();
		params.width = Constant.screenWidth / 4;
		params.height = Constant.screenWidth / 4;
		head.setLayoutParams(params);
		name = (TextView) this.findViewById(R.id.record_details_name);
		msg = (TextView) this.findViewById(R.id.record_details_msg);
		phone = (TextView) this.findViewById(R.id.record_details_phone);
		lv = (ListView) this.findViewById(R.id.record_details_lv);
		call = (RelativeLayout) this.findViewById(R.id.record_details_call_rl);
		back = (RelativeLayout) this.findViewById(R.id.record_details_back);
		message = (RelativeLayout) this
				.findViewById(R.id.record_details_message_rl);
		delete = (LinearLayout) this
				.findViewById(R.id.record_details_delete_linear);
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

					bmp = MediaStore.Images.Media.getBitmap(
							this.getContentResolver(), uri).copy(
							Bitmap.Config.RGB_565, true);

				} catch (Exception e) {
					Log.e("", "头像的  Uri不存在 ");
				}
			} else {
				bmp = BitmapFactory.decodeResource(getResources(),
						R.drawable.image_men).copy(Bitmap.Config.RGB_565, true);

				// view.setb
			}

			ImageView iv = (ImageView) findViewById(R.id.record_details_iv);
			iv.setImageBitmap(bmp);
			// if (!TextUtils.isEmpty(contact_head)) {
			// File file = new File(contact_head);
			// if (file.exists()) {
			// Bitmap bitmap = BitmapFactory.decodeFile(contact_head);
			// if (bitmap != null) {
			// head.setImageBitmap(bitmap);
			// }
			// }
			// }
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
		list = PhoneRecordUtil.queryRecordByNumber(this, record_phone);
	}

	@Override
	public void onClick(View v) {
		if (Utils.isFastClick()) {
			return;
		}
		switch (v.getId()) {
		case R.id.record_details_back:
			Intent intent = null;
			int record_id = -1;
			if (getContactNameByPhoneNumber(RecordDetails_Activity.this,
					record_phone)) {
				intent = new Intent(RecordDetails_Activity.this,
						EditContactActivity.class);
			} else {
				intent = new Intent(RecordDetails_Activity.this,
						Add_Contact_Activity.class);
			}
			List<Contact> listC = new ArrayList<Contact>();
			String[] selection = new String[] { Phone._ID, Phone.DISPLAY_NAME,
					Phone.NUMBER, Phone.PHOTO_URI, Phone.RAW_CONTACT_ID };
			Cursor cursor = getContentResolver().query(Phone.CONTENT_URI,
					selection, null, null, null);
			while (cursor.moveToNext()) {
				int contact_id = cursor
						.getInt(cursor.getColumnIndex(Phone._ID));
				String contact_phone = cursor.getString(cursor
						.getColumnIndex(Phone.NUMBER));
				String contact_name = cursor.getString(cursor
						.getColumnIndex(Phone.DISPLAY_NAME));

				String photo_uri = cursor.getString(cursor
						.getColumnIndex(Phone.PHOTO_URI));

				int rawContact_id = cursor.getInt(cursor
						.getColumnIndex(Phone.RAW_CONTACT_ID));
				Contact contact = new Contact(contact_id, contact_phone,
						contact_name, null, rawContact_id, photo_uri);
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
			// intent.putExtra("contact_head", contact_head);
			startActivity(intent);
			break;
		case R.id.record_details_delete_linear:
			PhoneRecordUtil.deleteRecordByNumber(this, record_phone);
			list.clear();
			adapter.notifyDataSetChanged();
			isDelete = true;
			finish();
			break;
		case R.id.record_details_call_rl:// 打电话
			Intent phoneIntent = new Intent("android.intent.action.CALL",
					Uri.parse("tel:" + record_phone));
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
				convertView = LayoutInflater.from(RecordDetails_Activity.this)
						.inflate(R.layout.record_details_item, null);
				holder = new RecordDetailsHolder();
				holder.image = (ImageView) convertView
						.findViewById(R.id.record_details_item_image);
				holder.style = (TextView) convertView
						.findViewById(R.id.record_details_item_style);
				holder.time = (TextView) convertView
						.findViewById(R.id.record_details_item_time);
				convertView.setTag(holder);
			} else {
				holder = (RecordDetailsHolder) convertView.getTag();
			}

			PhoneRecord record = list.get(position);
			long record_time = record.getDate();
			long record_when = record.getDuration();
			String record_state = String.valueOf(record.getType());

			if (record_state.equals("1")) {// 呼入
				holder.image.setImageResource(R.drawable.call_in);
			} else if (record_state.equals("2")) {// 呼出
				holder.image.setImageResource(R.drawable.call_out);
			} else if (record_state.equals("3")) {// 未接
				holder.image.setImageResource(R.drawable.call_no_joint);
			}

			if (record_when > 0) {

				StringBuffer sb = new StringBuffer("");
				if (record_state.equals("1")) {// 呼入
					sb.append("呼入");
				} else if (record_state.equals("2")) {// 呼出
					sb.append("呼出");
				}
				sb.append(getFormatDuration(record_when));
				holder.style.setText(sb.toString());
			} else {
				holder.style.setText("未接通");
			}

			if (record_time != 0) {
				holder.time.setText(Record_Activity.getDetailTime(record_time));
			}

			return convertView;
		}

	}

	private void initCurrentTime() {
		Calendar c = Calendar.getInstance();
		currentTime = c.getTimeInMillis();
		c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
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

	public static boolean getContactNameByPhoneNumber(Context context,
			String address) {
		String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER };

		// 将自己添加到 msPeers 中
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				projection, // Which
							// columns
							// to
							// return.
				ContactsContract.CommonDataKinds.Phone.NUMBER + " = '"
						+ address + "'", // WHERE
											// clause.
				null, // WHERE clause value substitution
				null); // Sort order.

		if (cursor == null || cursor.getCount() <= 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 将long类型的秒数值转换为一定的格式，规则如下：
	 * <p>
	 * 1分钟之内，显示为秒值
	 * </p>
	 * <p>
	 * 1小时之内，显示为分钟和秒值
	 * </p>
	 * <p>
	 * 1小时以上，显示为小时，分钟和秒值
	 * </p>
	 * 
	 * @param duration
	 *            要被格式化的数据
	 * @return 返回特定格式的String类型值
	 */
	public static String getFormatDuration(long duration) {
		StringBuilder strBuilder = new StringBuilder("");
		if (duration < 60) {
			strBuilder.append(duration).append("秒");
		} else if (duration < 60 * 60) {
			strBuilder.append((long) (duration / 60)).append("分")
					.append((long) (duration % 60)).append("秒");
		} else {
			strBuilder.append((long) (duration / 3600)).append("时")
					.append((long) (duration % 3600 / 60)).append("分")
					.append((long) (duration % 3600 % 60)).append("秒");
		}
		return strBuilder.toString();
	}

}
