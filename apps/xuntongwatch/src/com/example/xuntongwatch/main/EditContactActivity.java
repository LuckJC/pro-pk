package com.example.xuntongwatch.main;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.databaseutil.PhoneDatabaseUtil;
import com.example.xuntongwatch.entity.Contact;

public class EditContactActivity extends Activity {
	private Contact contact;
	private EditText phone, name;
	private TextView sure;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_edit_contact);
		sure = (TextView) this.findViewById(R.id.edit_contact_sure);
		phone = (EditText) this.findViewById(R.id.edit_contact_phone);
		name = (EditText) this.findViewById(R.id.edit_contact_name);
		ajustUpdate();
		sure.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				updateContact(name.getText().toString(),phone.getText().toString(),null); 
				Toast.makeText(EditContactActivity.this, "edit succeed"+name.getText().toString()+phone.getText().toString()+contact.getRawContact_id(), 0).show();
				finish();
			}
		});
		super.onCreate(savedInstanceState);
	}

	private Contact updateContact(String name, String phone, Bitmap bitmap) {
		Contact contact = genarationContact(name, phone, bitmap);
		contact.setRawContact_id(this.contact.getRawContact_id());
//		initContactDbUtil();
//		contactUtil.updateByContact_id(contact);
		PhoneDatabaseUtil.updateContact(this, contact);
		return contact;
	}

	private Contact genarationContact(String name, String phone, Bitmap bitmap) {
		Contact contact = new Contact();
		if (bitmap != null) {
//			String photo_name = Utils.getPhotoFileName();
//			String photo_path = Utils.saveBitmapToFile(Constant.headUri, this,
//					photo_name, bitmap);
//			contact.setContact_head(photo_path);
//			Log.e("", "photo_path  === " + photo_path);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
//			 将Bitmap压缩成PNG编码，质量为100%存储
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
			byte[] photo = os.toByteArray();
			contact.setContact_head(photo);
		}
		contact.setContact_name(name);
		contact.setContact_phone(phone);

		return contact;
	}
	private void ajustUpdate() {
		Intent intent = this.getIntent();
		if (intent != null) {
			String contact_name = intent.getStringExtra("contact_name");
			if (TextUtils.isEmpty(contact_name)) {
				return;
			}
			String contact_phone = intent.getStringExtra("contact_phone");
//			String contact_head = intent.getStringExtra("contact_head");
			int contact_id = intent.getIntExtra("contact_id", -1);
			contact = new Contact();
//			contact.setContact_head(contact_head);
			contact.setContact_name(contact_name);
			contact.setRawContact_id(contact_id);
			contact.setContact_phone(contact_phone);
			if (!TextUtils.isEmpty(contact_phone)) {
				phone.setText(contact_phone);
			}
			if (!TextUtils.isEmpty(contact_name)) {
				name.setText(contact_name);
			}
			phone.setSelection(phone.getText().length());
			name.setSelection(name.getText().length());
		}
	}
}
