package com.android.watchgallery;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.android.watchgallery.R;
import com.android.watchgallery.single.SingleListActivity;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	Button photo;
	Button picture;
	GridView gridView;
	LinearLayout layout3;
	public static MyPhotoAdapter myPhotoAdapter;
	public static PictureAdater pictureAdater;
	DisplayMetrics dm;
	ViewHolder holder;
	public static List<String> lsmap;
	public static String[] pictures;
	public static String[] imageUrls;
	DisplayImageOptions options;
	public static ArrayList<String> listImgPath;
	String path = null;
	ContentResolver resolver;
	int all = 0;
	public static int flag = 0; // 照片跟图片的标志
	public static ArrayList<String> list;
	boolean isCheck = false;
	ArrayList<String> deleteImgDir; // 删除照片跟相片的路径
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyClick myClick = new MyClick();
		setContentView(R.layout.main);
		list = new ArrayList<String>();
		deleteImgDir = new ArrayList<String>();
		lsmap = new ArrayList<String>();
		myPhotoAdapter = new MyPhotoAdapter(MainActivity.this);// 照片适配
		pictureAdater = new PictureAdater(); // 图片适配
		photo = (Button) this.findViewById(R.id.photo);
		picture = (Button) this.findViewById(R.id.picture);
		photo.setOnClickListener(myClick);
		picture.setOnClickListener(myClick);
		layout3 = (LinearLayout) this.findViewById(R.id.layout11);
		gridView = (GridView) this.findViewById(R.id.gridView1);
		if (list != null) { 
			list.clear();
		}
		if (listImgPath != null) {
			listImgPath.clear();
		}
		if (imageUrls != null) {
			imageUrls = null;
		}
		if (lsmap != null) {
			lsmap.clear();
		}
		if (pictures != null) {
			pictures = null;
		}
		listImgPath = getListPic();
		if (FileList.lsmap != null) {
			FileList.lsmap.clear();
		}
		lsmap = FileList.findFile(Environment.getExternalStorageDirectory()
				.getAbsolutePath());
		/* 判断sd卡是否存在 */
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);
		/* 获取存放的路径 */
		if (sdCardExist) {
			imageUrls = (String[]) listImgPath.toArray(new String[listImgPath.size()]);
			for (int i = imageUrls.length - 1; i >= 0; i--) {
				imageUrls[i] = "file://" + imageUrls[i];
			}
			lsmap.removeAll(listImgPath);
			pictures = lsmap.toArray(new String[lsmap.size()]);
			for (int i = 0; i < pictures.length; i++) {
				pictures[i] = "file://" + pictures[i];
			}
			options = new DisplayImageOptions.Builder()
					.showStubImage(R.drawable.ic_empty)
					.showImageForEmptyUri(R.drawable.ic_stub)
					.showImageOnFail(R.drawable.ic_error).cacheInMemory(true)
					.cacheOnDisc(true).displayer(new RoundedBitmapDisplayer(0))
					.build();
		}
		flag = 0;
		gridView.setAdapter(myPhotoAdapter);
		myPhotoAdapter.notifyDataSetChanged();
		gridView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(MainActivity.this,
						DeleteActivity.class);
				intent.putExtra("flag", flag);
				startActivityForResult(intent, 8);
				return true;
			}
		});
		gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(MainActivity.this,SingleListActivity.class);
				if (flag == 0) {
					intent.putExtra("singleImage", imageUrls[position]);
					intent.putExtra("position", position);
					startActivity(intent);
				}
				if (flag == 1) {
					intent.putExtra("singleImage", pictures[position]);
					intent.putExtra("position", position);
					startActivity(intent);
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 8) {
			myPhotoAdapter.notifyDataSetChanged();
		}

	}
	class MyClick implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.photo:
				flag = 0;
				photo.setBackgroundColor(Color.parseColor("#07D5E2"));
				picture.setBackgroundColor(Color.parseColor("#8A9AA8"));
				gridView.setAdapter(myPhotoAdapter);
				myPhotoAdapter.notifyDataSetChanged();
				break;
			case R.id.picture:
				flag = 1;
				picture.setBackgroundColor(Color.parseColor("#07D5E2"));
				photo.setBackgroundColor(Color.parseColor("#8A9AA8"));
				gridView.setAdapter(pictureAdater);
				pictureAdater.notifyDataSetChanged();
				break;
			default:
				break;
			}
		}
	}

	/* 获取的是MyCamera文件夹下面的所有图片路径 */
	public ArrayList<String> getListPic() {
		if(list!=null){
			list.clear();
		}
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/DCIM/MyCamera/";
		File file = new File(path);
		if (file.exists()) {
			File[] f = file.listFiles();
			for (int i = f.length - 1; i >= 0; i--) {
				if (f[i].getName().endsWith(".jpg")) {
					f[i].getAbsolutePath();
					list.add(f[i].getAbsolutePath());
				}
			}
		} else {
			Toast.makeText(this, path + "没有图片", 1000).show();
		}
		return list;
	}

	// 图片适配
	public class PictureAdater extends BaseAdapter {
		ImageView img;
		protected ImageLoader imageLoader = ImageLoader.getInstance();
		private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

		@Override
		public int getCount() {
			return pictures.length;
		}

		@Override
		public Object getItem(int position) {
			return pictures[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(MainActivity.this).inflate(
						R.layout.item, parent, false);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.img = (ImageView) convertView.findViewById(R.id.img);
			try {
				imageLoader.init(ImageLoaderConfiguration
						.createDefault(MainActivity.this));
				imageLoader.displayImage(pictures[position], holder.img,
						options, animateFirstListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return convertView;
		}

	}

	// 照片适配
	public class MyPhotoAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		ArrayList<ImageView> images = new ArrayList<ImageView>();
		File photos = null;
		protected ImageLoader imageLoader = ImageLoader.getInstance();
		private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

		public MyPhotoAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return imageUrls.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return imageUrls[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.item, parent, false);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.img = (ImageView) convertView.findViewById(R.id.img);
			try {
				imageLoader.init(ImageLoaderConfiguration
						.createDefault(MainActivity.this));
				imageLoader.displayImage(imageUrls[position], holder.img,
						options, animateFirstListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return convertView;
		}

	}
	public class ViewHolder {
		public ImageView img;
		public CheckBox cb;
	}
	private static class AnimateFirstDisplayListener extends
			SimpleImageLoadingListener {

		static final List<String> displayedImages = Collections
				.synchronizedList(new LinkedList<String>());

		@Override
		public void onLoadingComplete(String imageUri, View view,
				Bitmap loadedImage) {
			if (loadedImage != null) {
				ImageView imageView = (ImageView) view;
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) {
					FadeInBitmapDisplayer.animate(imageView, 500); // ����image���ض���500ms
					displayedImages.add(imageUri); // ��ͼƬuri��ӵ�������
				}
			}
		}
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		MainActivity.this.finish();
	}

}
