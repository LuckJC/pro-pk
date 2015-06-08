package com.example.xuntongwatch.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.database.Cursor;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
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
import android.widget.Toast;

import com.example.xuntongwatch.MyApplication;
import com.example.xuntongwatch.R;
import com.example.xuntongwatch.databaseutil.PhoneDatabaseUtil;
import com.example.xuntongwatch.entity.Contact;
import com.example.xuntongwatch.entity.GridViewItemImageView;
import com.example.xuntongwatch.util.CommonDialog;
import com.example.xuntongwatch.util.ContactInfo;
import com.example.xuntongwatch.view.CharacterParser;
import com.example.xuntongwatch.view.DraggableGridView;
import com.example.xuntongwatch.view.OnRearrangeListener;
import com.example.xuntongwatch.view.PinyinComparator;
import com.example.xuntongwatch.view.SortModel;

public class Contact_Activity extends Activity {

	/**
	 * 汉字转换成拼音的类
	 */
	private CharacterParser characterParser;
	private List<SortModel> SourceDateList;

	/**
	 * 根据拼音来排列ListView里面的数据类
	 */
	private PinyinComparator pinyinComparator;
	/** 联系人显示名称 **/
	private static final int PHONES_DISPLAY_NAME_INDEX = 0;
	/** 电话号码 **/
	private static final int PHONES_NUMBER_INDEX = 1;
	/** 获取库Phon表字段 **/
	private static final String[] PHONES_PROJECTION = new String[] { Phone.DISPLAY_NAME,
			Phone.NUMBER, Photo.PHOTO_ID, Phone.CONTACT_ID };
	ScrollView horizontalScrollView;
	GridView gridView;
	public static int imageWidth, imageHeight;
	private ArrayList<GridViewItemImageView> list;
	private List<String> listvCard;
	static Random random = new Random();
	DraggableGridView dgv;
	private List<String> listvCardPath;
	private ArrayList<HashMap<String, String>> listItem;
	ContactInfo.ContactHandler handler = ContactInfo.ContactHandler.getInstance();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact);
		// 实例化汉字转拼音类
		characterParser = CharacterParser.getInstance();
		pinyinComparator = new PinyinComparator();

		dgv = ((DraggableGridView) findViewById(R.id.vgv));
		dgv.setOnRearrangeListener(new OnRearrangeListener() {

			@Override
			public void onRearrange(int oldIndex, int newIndex) {

			}
		});
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
				if (choose.getStringExtra("tag") != null)
				// if (choose.getStringExtra("tag").equals("send"))
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
		if (MyApplication.sp.getBoolean(s, false)) {
			paint.setTextSize(50);
			paint.setColor(0xffE88F1D);
			canvas.drawText("★", 125, 38, paint);
		}
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

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, Menu.FIRST + 3, 3, "从SD卡导入联系人");
		menu.add(Menu.NONE, Menu.FIRST + 4, 4, "将联系人导出到SD卡");
		menu.add(Menu.NONE, Menu.FIRST + 1, 1, "添加联系人");
		menu.add(Menu.NONE, Menu.FIRST + 2, 2, "从sim卡导入联系人");

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case Menu.FIRST + 1:
			Intent intent = new Intent(this, Add_Contact_Activity.class);
			startActivityForResult(intent, Add_Contact_Activity.RESULT_CODE);
			break;
		case Menu.FIRST + 2:
			getSIMContacts();
			dgv.removeAllViews();
			initUI();
			break;
		case Menu.FIRST + 4:
			UpdateTextTaskContact task = new UpdateTextTaskContact();
			task.execute();
			break;
		case Menu.FIRST + 3: // 恢复
			File path = new File("/storage/sdcard0/bluetooth");
			searchFileOther(".vcf", path);
			searchFile(".vcf", Environment.getExternalStorageDirectory());

			if (listvCard.size() <= 0) {
				Toast.makeText(this, "SDCard中不存在vCard文件", Toast.LENGTH_SHORT).show();
			} else {
				String[] vCard = new String[listvCard.size()];
				for (int i = 0; i < vCard.length; i++) {
					vCard[i] = listvCard.get(i);
				}
				listItem = new ArrayList<HashMap<String, String>>();
				for (int i = 0; i < listvCard.size(); i++) {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put(listvCard.get(i), "false");
					listItem.add(map);
				}
				new AlertDialog.Builder(this).setTitle("选择要导入的文件")
						.setMultiChoiceItems(vCard, null, new OnMultiChoiceClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which, boolean isChecked) {

								if (isChecked) {
									listItem.get(which).put(listvCard.get(which), "true");
								} else {
									listItem.get(which).put(listvCard.get(which), "false");
								}
							}
						}).setPositiveButton("确定", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								boolean isEmpty = false;
								for (int i = 0; i < listItem.size(); i++) {
									if (Boolean.parseBoolean(listItem.get(i).get(listvCard.get(i)))) {
										isEmpty = true;
									}
								}

								if (isEmpty) {
									UpdateTextTask task = new UpdateTextTask();
									task.execute();
								}else{
									Toast.makeText(Contact_Activity.this, "请选择要导入的文件", 0).show();
								}
							}
						}).setNegativeButton("取消", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();

							}
						}).show();

			}

			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void toLead(ContactInfo.ContactHandler handler, String path) {
		try {
			// 获取要恢复的联系人信息
			List<ContactInfo> infoList = handler.restoreContacts(path);
			// for (ContactInfo contactInfo : infoList) {
			// // 恢复联系人
			// handler.addContacts(MainActivity.this, contactInfo);
			//
			// }
			for (int i = 0; i < infoList.size(); i++) {
				ContactInfo contactInfo = infoList.get(i);
				handler.addContacts(Contact_Activity.this, contactInfo);
				Log.i("===============================", i + "");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * searchFile 查找文件并加入到ArrayList 当中去
	 * 
	 * @String keyword 查找的关键词
	 * 
	 * @File filepath 查找的目录
	 */
	private void searchFile(String keyword, File filepath) {
		// 判断SD卡是否存在
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			File[] files = filepath.listFiles();
			if (files == null) {
				return;
			}
			if (files.length > 0) {
				for (File file : files) {
					if (file.isDirectory()) {
						// 如果目录可读就执行（一定要加，不然会挂掉）
						// if (file.canRead()) {
						// searchFile(keyword, file); // 如果是目录，递归查找
						// }
					} else {
						// 判断是文件，则进行文件名判断
						try {
							if (file.getName().indexOf(keyword) > -1
									|| file.getName().indexOf(keyword.toUpperCase()) > -1) {
								listvCard.add(file.getName());
								listvCardPath.add(file.getPath());
							}
						} catch (Exception e) {
							Toast.makeText(this, "查找发生错误", Toast.LENGTH_SHORT).show();
						}
					}
				}

			}
		}
	}

	/*
	 * searchFile 查找文件并加入到ArrayList 当中去
	 * 
	 * @String keyword 查找的关键词
	 * 
	 * @File filepath 查找的目录
	 */
	private void searchFileOther(String keyword, File filepath) {
		listvCard = new ArrayList<String>();
		listvCardPath = new ArrayList<String>();
		// 判断SD卡是否存在
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			File[] files = filepath.listFiles();
			if (files == null) {
				return;
			}
			if (files.length > 0) {
				for (File file : files) {
					if (file.isDirectory()) {
						// 如果目录可读就执行（一定要加，不然会挂掉）
						// if (file.canRead()) {
						// searchFile(keyword, file); // 如果是目录，递归查找
						// }
					} else {
						// 判断是文件，则进行文件名判断
						try {
							if (file.getName().indexOf(keyword) > -1
									|| file.getName().indexOf(keyword.toUpperCase()) > -1) {
								listvCard.add(file.getName());
								listvCardPath.add(file.getPath());
							}
						} catch (Exception e) {
						}
					}
				}
			}
		}
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
			sortContact(list);
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

	/** 得到手机SIM卡联系人人信息 **/
	private void getSIMContacts() {
		ContentResolver resolver = getContentResolver();
		// 获取Sims卡联系人
		Uri uri = Uri.parse("content://icc/adn");
		Cursor phoneCursor = resolver.query(uri, PHONES_PROJECTION, null, null, null);

		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {
				Contact contact = new Contact();
				// 得到手机号码
				String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX);
				// 当手机号码为空的或者为空字段 跳过当前循环
				if (TextUtils.isEmpty(phoneNumber))
					continue;
				// 得到联系人名称
				String contactName = phoneCursor.getString(PHONES_DISPLAY_NAME_INDEX);
				int ci_name = phoneCursor.getColumnIndex(Phone.DISPLAY_NAME);
				String name = phoneCursor.getString(ci_name);

				// Sim卡中没有联系人头像

				contact.setContact_name(contactName);
				contact.setContact_phone(phoneNumber);
				PhoneDatabaseUtil.addContact(this, contact);
			}

			phoneCursor.close();
		} else {
			Toast.makeText(this, "sim卡无联系人", Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * 为ListView填充数据
	 * 
	 * @param date
	 * @return
	 */
	private List<SortModel> filledData(String[] date) {
		List<SortModel> mSortList = new ArrayList<SortModel>();

		for (int i = 0; i < date.length; i++) {
			SortModel sortModel = new SortModel();
			sortModel.setName(date[i]);
			// 汉字转换成拼音
			String pinyin = characterParser.getSelling(date[i]);
			String sortString = pinyin.substring(0, 1).toUpperCase();

			// 正则表达式，判断首字母是否是英文字母
			if (sortString.matches("[A-Z]")) {
				sortModel.setSortLetters(sortString.toUpperCase());
			} else {
				sortModel.setSortLetters("#");
			}

			mSortList.add(sortModel);
		}
		return mSortList;

	}

	private void sortContact(ArrayList<GridViewItemImageView> old) {
		ArrayList<GridViewItemImageView> collect_c = new ArrayList<GridViewItemImageView>();
		ArrayList<GridViewItemImageView> collect_no_c = new ArrayList<GridViewItemImageView>();
		for (int i = 0; i < old.size(); i++) {
			Contact old_c = (Contact) old.get(i);
			if (MyApplication.sp.getBoolean(old_c.getContact_name(), false)) {
				collect_c.add(old_c);
			} else {
				collect_no_c.add(old_c);
			}
		}
		list.clear();
		ArrayList<GridViewItemImageView> cantacts = sortContactOther(collect_c);
		for (int i = 0; i < cantacts.size(); i++) {
			list.add(cantacts.get(i));
		}
		cantacts = sortContactOther(collect_no_c);
		for (int i = 0; i < cantacts.size(); i++) {
			list.add(cantacts.get(i));
		}

	}

	/*
	 * 排序收藏与不收藏联系人
	 */
	private ArrayList<GridViewItemImageView> sortContactOther(ArrayList<GridViewItemImageView> old) {
		String[] names = new String[old.size()];
		ArrayList<GridViewItemImageView> collect_c_new_c = new ArrayList<GridViewItemImageView>();
		for (int i = 0; i < old.size(); i++) {
			Contact contact = (Contact) old.get(i);
			names[i] = contact.getContact_name();
		}
		SourceDateList = filledData(names);
		Collections.sort(SourceDateList, pinyinComparator);
		for (int i = 0; i < SourceDateList.size(); i++) {
			for (int j = 0; j < SourceDateList.size(); j++) {
				Contact contact = (Contact) old.get(j);
				if (SourceDateList.get(i).getName().equals(contact.getContact_name())) {
					collect_c_new_c.add(contact);
					break;
				}
			}

		}
		return collect_c_new_c;
	}

	class UpdateTextTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			CommonDialog.showDialog(Contact_Activity.this);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 0; i < listItem.size(); i++) {
				if (Boolean.parseBoolean(listItem.get(i).get(listvCard.get(i)))) {
					toLead(handler, listvCardPath.get(i));
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dgv.removeAllViews();
			initUI();
			CommonDialog.closeDialog();
			Toast.makeText(Contact_Activity.this, "导入联系人信息成功!", Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
		}
	}

	class UpdateTextTaskContact extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			CommonDialog.showDialog(Contact_Activity.this);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			// 获取要备份的信息
			List<ContactInfo> _infoList = handler.getContactInfo(Contact_Activity.this);
			handler.backupContacts(Contact_Activity.this, _infoList); // 备份联系人信息
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			CommonDialog.closeDialog();
			Toast.makeText(Contact_Activity.this, "备份成功！", Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}
	}
}
