package com.example.xuntongwatch.main;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout.LayoutParams;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.adapter.MyGridViewAdapter;
import com.example.xuntongwatch.anima.ActivityJumpAnimation;
import com.example.xuntongwatch.databaseutil.PhoneDatabaseUtil;
import com.example.xuntongwatch.entity.Contact;
import com.example.xuntongwatch.entity.GridViewItemImageView;
import com.example.xuntongwatch.util.Constant;
import com.example.xuntongwatch.util.PhoneUtil;

public class Contact_Activity extends Activity {
	HorizontalScrollView horizontalScrollView;
	GridView gridView;
	private int NUM = 2; // 每行显示个数
	public static int imageWidth, imageHeight;
//	private ContactDbUtil contactUtil;

	private ArrayList<GridViewItemImageView> list;
	private MyGridViewAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact);
		initWidthOrHeight();
		horizontalScrollView = (HorizontalScrollView) findViewById(R.id.scrollView);
		gridView = (GridView) findViewById(R.id.gridView1);
		horizontalScrollView.setHorizontalScrollBarEnabled(false);// 隐藏滚动条
		initArrayList();
		setValue();
		initGridViewOnItemClickListener();
	}

	public void initArrayList() {
		list = PhoneDatabaseUtil.allContact_(this);
//		if (contactUtil == null) {
//			contactUtil = new ContactDbUtil(this);
//		}
//		list = contactUtil.findAllContact();
		// for (int i = 0; i < 10; i++) {
		// ClassicImage image = new ClassicImage();
		// list.add(image);
		// }
	}

	private void initGridViewOnItemClickListener() {
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.e("", "5555555555555555555555555");
				Contact contact = (Contact) list.get(position);
				String phone = contact.getContact_phone();
				PhoneUtil.callPhone(Contact_Activity.this, phone);
			}
		});

		gridView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(Contact_Activity.this,Contact_Detail_Activity.class);
				Contact contact = (Contact) list.get(position);
//				byte[] contact_head = contact.getContact_head();
//				String contact_name = contact.getContact_name();
//				String contact_phone = contact.getContact_phone();
//				int contact_id = contact.getContact_id();
//				intent.putExtra("contact_id", contact_id);
//				intent.putExtra("contact_phone", contact_phone);
//				intent.putExtra("contact_name", contact_name);
//				intent.putExtra("contact_head", contact_head);
				Bundle b = new Bundle();
				b.putSerializable("contact", contact);
				intent.putExtras(b);
				startActivityForResult(intent,Contact_Detail_Activity.RESULT_CODE);
				Log.e("", "666666666666666666666");
				return false;
			}
		});
	}

	private void setValue() {
		adapter = new MyGridViewAdapter(this, list, imageWidth, imageHeight,
				MyGridViewAdapter.CONTACT_IMAGE);
		gridView.setAdapter(adapter);
		setGridViewWidthAndHeight(adapter);
	}

	private void setGridViewWidthAndHeight(MyGridViewAdapter adapter) {
		int gridViewLie = (adapter.getCount() % 2 == 0) ? adapter.getCount() / 2
				: adapter.getCount() / 2 + 1;
		LayoutParams params = new LayoutParams(gridViewLie * (imageWidth + 3)
				+ 15, LayoutParams.WRAP_CONTENT);
		gridView.setLayoutParams(params);
		gridView.setVerticalSpacing(3);
		gridView.setColumnWidth(Constant.screenWidth / NUM);
		gridView.setStretchMode(GridView.NO_STRETCH);
		gridView.setNumColumns(gridViewLie);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.contact_activity_menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.contact_activity_menu_add:
			Intent intent = new Intent(this, Add_Contact_Activity.class);
			startActivityForResult(intent, Add_Contact_Activity.RESULT_CODE);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void initWidthOrHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		// 获取屏幕信息
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		Constant.screenWidth = dm.widthPixels;
		Constant.screenHeight = dm.heightPixels;
		imageWidth = Constant.screenWidth / 2 - 3;
		imageHeight = imageWidth;
	}

	@Override
	public void finish() {
		super.finish();
		ActivityJumpAnimation.LeftBack(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Add_Contact_Activity.RESULT_CODE) {// 从添加界面返回
			if (data != null) {
				Bundle b = data.getExtras();
				Contact contact = (Contact) b.getSerializable("contact");
				list.add(contact);
				adapter.notifyDataSetChanged();
				setGridViewWidthAndHeight(adapter);
			}
		} else if (requestCode == Contact_Detail_Activity.RESULT_CODE) {// 个人信息界面返回
			if (data != null) {
				boolean isUpdate = data.getBooleanExtra("isUpdate", false);
				boolean isDelete = data.getBooleanExtra("isDelete", false);
				if (isUpdate || isDelete) {
//					list = contactUtil.findAllContact();
					list = PhoneDatabaseUtil.allContact_(this);
					adapter = new MyGridViewAdapter(this, list, imageWidth,
							imageHeight, MyGridViewAdapter.CONTACT_IMAGE);
					gridView.setAdapter(adapter);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
