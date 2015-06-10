package com.example.xuntongwatch.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.main.Edit_Head_Custom_Fragment.Custom_interface;
import com.example.xuntongwatch.util.Utils;

public class Edit_Head_Activity extends FragmentActivity implements OnClickListener,
		Custom_interface {

	private ImageView title_image;
	private Edit_Head_Custom_Fragment custom_frag;
	private Edit_Head_Classic_Fragment classic_frag;
	private TextView next, custom_tv, classic_tv;
	private final int CUSTOM = -1;
	private final int CLASSIC = -2;
	private int state;
	private Bitmap bitmap;

	private final int GALLERY = 5;
	private final int SCALE_IMAGE = 7;

	public static final int RESULT_CODE = 1111;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_head);
		init();
	}

	private void init() {
		title_image = (ImageView) this.findViewById(R.id.edit_head_title_image);
		custom_tv = (TextView) this.findViewById(R.id.edit_head_custom_tv);
		classic_tv = (TextView) this.findViewById(R.id.edit_head_classic_tv);
		next = (TextView) this.findViewById(R.id.edit_head_next_tv);
		FragmentManager manager = getSupportFragmentManager();
		custom_frag = (Edit_Head_Custom_Fragment) manager
				.findFragmentById(R.id.edit_head_custom_fragment);
		classic_frag = (Edit_Head_Classic_Fragment) manager
				.findFragmentById(R.id.edit_head_classic_fragment);
		classic_frag.onHiddenChanged(true);
		custom_frag.onHiddenChanged(true);

		custom_tv.setOnClickListener(this);
		classic_tv.setOnClickListener(this);
		next.setOnClickListener(this);
		hideFragment(classic_frag);
		state = CUSTOM;
	}

	private void showFragment(android.support.v4.app.Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.show(fragment);
		transaction.commit();
	}

	private void hideFragment(android.support.v4.app.Fragment fragment) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.hide(fragment);
		transaction.commit();
	}

	@Override
	public void onClick(View v) {
		if (Utils.isFastClick()) {
			return;
		}
		switch (v.getId()) {
		case R.id.edit_head_custom_tv:
			if (state == CUSTOM)
				return;
			showFragment(custom_frag);
			hideFragment(classic_frag);
			setTextViewBagground(custom_tv, classic_tv);
			state = CUSTOM;
			break;
		case R.id.edit_head_classic_tv:
			if (state == CLASSIC)
				return;
			showFragment(classic_frag);
			hideFragment(custom_frag);
			setTextViewBagground(classic_tv, custom_tv);
			state = CLASSIC;
			break;
		case R.id.edit_head_next_tv:
			if(state==CLASSIC){
				bitmap = Edit_Head_Classic_Fragment.sClassicBitmap;
			}
			if (bitmap != null) {
				Intent intent = new Intent();
				intent.putExtra("bitmap", bitmap);
				setResult(RESULT_CODE, intent);
				finish();
			} else {
				Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	@SuppressLint("NewApi")
	public void setTextViewBagground(TextView tv1, TextView tv2) {
		tv1.setBackgroundResource(R.drawable.edit_head_text_style);
		tv2.setBackground(null);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.e("进来相册了没有uri===", "uri===");
		if (resultCode != RESULT_CANCELED) {// 如果不进行取消点击按钮控件　的判断　　会抛异常
			switch (requestCode) {
			case GALLERY:
				Uri uri = data.getData();// 获取选中图片的Uri
				imageScale(uri, this, SCALE_IMAGE, 0, 0);// 将选中的图片进行手动的　　截取
				break;
			case SCALE_IMAGE:
				if (data == null) {
					break;
				}
				bitmap = (Bitmap) data.getExtras().getParcelable("data");
				custom_frag.setAddImageViewBitmap(bitmap);
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * 调用相册
	 * 
	 * @param activity
	 * @param resultCode
	 * 
	 */
	public void callCallery(Activity activity, int resultCode) {
		Intent intent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		activity.startActivityForResult(intent, resultCode);
	}

	public void imageScale(Uri imageUri, Activity activity, int resultCode, int width, int height) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(imageUri, "image/*");// 按照片的Uri选中图片进行手动缩放
		int i = 320;
		int j = 320;
		if (width != 0) {
			i = width;
		}
		if (height != 0) {
			j = height;
		}
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", i);
		intent.putExtra("outputY", j);
		intent.putExtra("scale", true);
		intent.putExtra("return-data", true);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		activity.startActivityForResult(intent, resultCode);
	}

	/**
	 * CustomFragment里面的添加头像事件
	 */
	@Override
	public void addImage() {
		callCallery(this, GALLERY);
	}

}
