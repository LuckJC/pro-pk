package com.example.xuntongwatch.main;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.data.ContactDbUtil;
import com.example.xuntongwatch.databaseutil.PhoneDatabaseUtil;
import com.example.xuntongwatch.databaseutil.SmsUtil;
import com.example.xuntongwatch.entity.Contact;
import com.example.xuntongwatch.entity.Message_Thread;
import com.example.xuntongwatch.util.PhoneUtil;

public class Contact_Detail_Activity extends Activity implements OnClickListener {

	private ImageView call, message, delete_contact, mEditContact;
	private RelativeLayout bg;
	private String contact_name, contact_phone;
	private byte[] contact_head;
	private int raw_contact_id;

	public static final int RESULT_CODE = 1113;
	private boolean isUpdate = false;// 判断是否修改了
	private boolean isDelete = false;// 判断是否删除了
	private ContactDbUtil contactUtil;
	private ArrayList<Message_Thread> arrayList;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_detail);
		contactUtil = new ContactDbUtil(this);
		call = (ImageView) this.findViewById(R.id.contact_detail_call);
		message = (ImageView) this.findViewById(R.id.contact_detail_message);
		delete_contact = (ImageView) this.findViewById(R.id.delete_contact);
		mEditContact = (ImageView) this.findViewById(R.id.edit_contact);
		bg = (RelativeLayout) this.findViewById(R.id.contact_detail_bg);
		delete_contact.setOnClickListener(this);
		message.setOnClickListener(this);
		mEditContact.setOnClickListener(this);
		call.setOnClickListener(this);

		Intent intent = this.getIntent();
		if (intent != null) {
			Bundle b = intent.getExtras();
			if (b != null) {
				Contact contact = (Contact) b.getSerializable("contact");
				contact_name = contact.getContact_name();
				contact_phone = contact.getContact_phone();
				raw_contact_id = contact.getRawContact_id();
				byte[] photo = contact.getContact_head();
				if (photo != null) {
					Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
					if (bitmap != null) {
						Drawable drawable = new BitmapDrawable(bitmap);
						bg.setBackgroundDrawable(drawable);
					}
				}
			}
			// contact_head = intent.getStringExtra("contact_head");
			// if (!TextUtils.isEmpty(contact_head)) {
			// Bitmap bitmap = BitmapFactory.decodeFile(contact_head);
			// if (bitmap != null) {
			// Drawable drawable = new BitmapDrawable(bitmap);
			// bg.setBackgroundDrawable(drawable);
			// }
			// }
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.contact_detail_call:
			PhoneUtil.callPhone(this, contact_phone);
			break;
		case R.id.contact_detail_message:
			arrayList = SmsUtil.allMessage_Thread(this);
			int thread_id = -1;
			// for (Message_Thread iterable_element : arrayList) {
			// if (iterable_element.getPhone().equals(contact_phone)) {
			// thread_id = iterable_element.getThread_id();
			// return;
			// }
			// }
			for (int i = 0; i < arrayList.size(); i++) {
				if (arrayList.get(i).getPhone().equals(contact_phone)) {
					thread_id = arrayList.get(i).getThread_id();
					break;
				}

			}

			// Message_Thread message = arrayList.get(position);
			Intent intent = new Intent(this, Message_Chat_Activity.class);
			intent.putExtra("contact_name", contact_name);
			intent.putExtra("message_phone", contact_phone);
			intent.putExtra("state", Message_Chat_Activity.ONE);
			intent.putExtra("thread_id", thread_id);
			Log.e("=================================================================",thread_id+"");
			this.startActivity(intent);
			break;
		// case R.id.contact_detail_call_detail:
		// Intent intent_detail = new Intent(this,
		// RecordDetails_Activity.class);
		// intent_detail.putExtra("record_phone", contact_phone);
		// intent_detail.putExtra("contact_name", contact_name);
		// intent_detail.putExtra("contact_head", contact_head);
		// startActivity(intent_detail);
		// // record_phone = intent.getStringExtra("record_phone");
		// // contact_name = intent.getStringExtra("contact_name");
		// // contact_head = intent.getStringExtra("contact_head");
		// break;
		case R.id.edit_contact:
			Intent intent_edit = new Intent(this, EditContactActivity.class);
			intent_edit.putExtra("contact_name", contact_name);
			intent_edit.putExtra("contact_phone", contact_phone);
			intent_edit.putExtra("contact_id", raw_contact_id);
			this.startActivity(intent_edit);
			Toast.makeText(this, contact_name + contact_phone + raw_contact_id, 0).show();

			break;
		case R.id.delete_contact:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final AlertDialog dialog = builder.create();
			dialog.setTitle("确认删除" + contact_name + "?");
			dialog.setIcon(R.drawable.ic_launcher);
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int which) {
							contactUtil.deleteByContact_id(raw_contact_id);
							PhoneDatabaseUtil.deleteContactByRawContact_id(
									Contact_Detail_Activity.this, raw_contact_id);
							dialog.cancel();
							finish();
						}
					});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int which) {
							dialog.cancel();
						}
					});
			dialog.show();
			break;
		}
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// getMenuInflater().inflate(R.menu.contact_detail_activity_menu, menu);
	// return true;
	// }
	//
	// public boolean onOptionsItemSelected(MenuItem item) {
	// // TODO Auto-generated method stub
	// switch (item.getItemId()) {
	// case R.id.contact_detail_activity_menu_update:
	// Intent intent = new Intent(this, Add_Contact_Activity.class);
	// intent.putExtra("contact_name", contact_name);
	// intent.putExtra("contact_phone", contact_phone);
	// intent.putExtra("contact_head", contact_head);
	// intent.putExtra("contact_id", raw_contact_id);
	// this.startActivityForResult(intent,
	// Add_Contact_Activity.RESULT_CODE);
	// break;
	// case R.id.contact_detail_activity_menu_delete:
	// AlertDialog.Builder builder = new AlertDialog.Builder(this);
	// final AlertDialog dialog = builder.create();
	// dialog.setTitle("确认删除" + contact_name + "?");
	// dialog.setIcon(R.drawable.ic_launcher);
	// dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定",
	// new DialogInterface.OnClickListener() {
	// @Override
	// public void onClick(DialogInterface dialogInterface,
	// int which) {
	// // contactUtil.deleteByContact_id(raw_contact_id);
	// PhoneDatabaseUtil.deleteContactByRawContact_id(Contact_Detail_Activity.this,
	// raw_contact_id);
	// dialog.cancel();
	// }
	// });
	// dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消",
	// new DialogInterface.OnClickListener() {
	// @Override
	// public void onClick(DialogInterface dialogInterface,
	// int which) {
	// dialog.cancel();
	// }
	// });
	// dialog.show();
	// break;
	// }
	// return super.onOptionsItemSelected(item);
	// }

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Add_Contact_Activity.RESULT_CODE) {
			if (data != null) {
				Bundle b = data.getExtras();
				Contact contact = (Contact) b.getSerializable("contact");
				this.contact_head = contact.getContact_head();
				this.contact_name = contact.getContact_name();
				this.contact_phone = contact.getContact_phone();
				isUpdate = true;
			}
		}
	}

	@Override
	public void finish() {
		Intent intent = new Intent();
		intent.putExtra("isUpdate", isUpdate);
		intent.putExtra("isDelete", isDelete);
		setResult(RESULT_CODE, intent);
		super.finish();
	}

}
