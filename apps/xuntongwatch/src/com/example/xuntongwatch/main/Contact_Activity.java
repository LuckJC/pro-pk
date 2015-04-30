package com.example.xuntongwatch.main;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.databaseutil.PhoneDatabaseUtil;
import com.example.xuntongwatch.entity.Contact;
import com.example.xuntongwatch.entity.GridViewItemImageView;
import com.example.xuntongwatch.view.DraggableGridView;

public class Contact_Activity extends Activity {
	ScrollView horizontalScrollView;
	GridView gridView;
	public static int imageWidth, imageHeight;
	private ArrayList<GridViewItemImageView> list;

	static Random random = new Random();
	DraggableGridView dgv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact);
		dgv = ((DraggableGridView) findViewById(R.id.vgv));
		initUI();
		setListeners();
	}

	private void setListeners() {

		dgv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Contact contact = (Contact) list.get(arg2);
				// dgv.removeViewAt(arg2);
				Intent choose = getIntent();
				if(choose.getStringExtra("tag")!=null)
//				if (choose.getStringExtra("tag").equals("send")) 
				{
					Intent i = new Intent();
					i.putExtra("contact_phone", contact.getContact_phone());
					i.putExtra("contact_name", contact.getContact_name());
					setResult(9527, i);
					finish();
				} else {
					Intent intent = new Intent(Contact_Activity.this, Contact_Detail_Activity.class);
					Bundle b = new Bundle();
					b.putSerializable("contact", contact);
					intent.putExtras(b);
					startActivityForResult(intent, Contact_Detail_Activity.RESULT_CODE);
				}
			}
		});
		// button1.setOnClickListener(new OnClickListener() {
		// public void onClick(View arg0) {
		// // String word = words[random.nextInt(words.length)];
		// // ImageView view = new ImageView(Contact_Activity.this);
		// // view.setImageBitmap(getThumb(word));
		// // dgv.addView(view);
		// // poem.add(word);
		// }
		// });

	}

	// 画矩形and文字
	private Bitmap getThumb(Bitmap bb, String s) {
		Bitmap bmp = Bitmap.createBitmap(bb);
		Canvas canvas = new Canvas(bmp);
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setAlpha(50);
		paint.setTextSize(36);
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		canvas.drawRect(new Rect(150, 115, 0, 150), paint);
		paint.setColor(Color.WHITE);
		paint.setTextAlign(Paint.Align.CENTER);
		canvas.drawText(s, 75, 140, paint);

		return toRoundCorner(bmp, 15);
	}

	// 压缩图片
	private Bitmap compressPhoto(Bitmap rawBitmap) {
		int rawHeight = rawBitmap.getHeight();
		int rawWidth = rawBitmap.getWidth();
		int newHeight = 150;
		int newWidth = 150;
		float heightScale = ((float) newHeight) / rawHeight;
		float widthScale = ((float) newWidth) / rawWidth;
		Matrix matrix = new Matrix();
		matrix.postScale(heightScale, widthScale);
		Bitmap newBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawWidth, rawHeight, matrix, true);
		return newBitmap;
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

	@Override
	protected void onRestart() {
		dgv.removeAllViews();
		initUI();
		super.onRestart();
	}

	private void initUI() {
		LinearLayout layout = (LinearLayout) findViewById(R.id.reminder);
		list = PhoneDatabaseUtil.allContact_(this);

		if (list.size() <= 0) {

			layout.setVisibility(View.VISIBLE);
			return;
		} else {
			layout.setVisibility(View.GONE);
			Bitmap bmp = null;
			for (int i = 0; i < list.size(); i++) {
				Contact contact = (Contact) list.get(i);
				String photo_uri = contact.getPhoto_uri();

				ImageView view = new ImageView(Contact_Activity.this);
				// view.setImageBitmap(image.getBitmap());
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
				Bitmap newbp = compressPhoto(bmp);
				view.setImageBitmap(getThumb(newbp, contact.getContact_name()));
				dgv.addView(view);

			}
		}
	}

	public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {
		Bitmap output = Bitmap
				.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = pixels;
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}
}
