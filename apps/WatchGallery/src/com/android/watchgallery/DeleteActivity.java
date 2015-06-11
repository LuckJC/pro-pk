package com.android.watchgallery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


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
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class DeleteActivity extends Activity {
	Button photo;
	Button picture;
	Button delete;
	Button deleteAll;
	GridView gridView;
	LinearLayout layout3;
	public ImageAdapter imageAdapter;
	public PictureAdater pictureAdater;
	DisplayMetrics dm;
	ViewHolder holder;
	public static List<String> lsmap;
	private String[] pictures;
	public static String[] imageUrls;
	DisplayImageOptions options;
	public static ArrayList<String> listImgPath;
	String path = null;
	ContentResolver resolver;
	int all = 0;
	public static int flag = 0; // 照片跟图片的标志
	public ArrayList<String> list;
	boolean isCheck = false;
	public static List<Item> imgList;
	public static List<Item> imgList2;
	public static List<Item> deleteImg; // 一个一个地选择的删除的照片数组
	public static List<Item> deletePicture; // 一个一个地选择的删除的图片数组
	public static List<Item> deleteImgAll; // 全选的删除的照片数组
	public static List<Item> deletePictureAll; // 全选的删除的图片数组
	public static List<Item> pictureList;
	public static List<Item> pictureList2;
	ArrayList<String> deleteImgDir; // 删除照片跟相片的路径

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		MyClick myClick = new MyClick();
		setContentView(R.layout.deletemain);
		imgList = new ArrayList<Item>();
		imgList2 = imgList;
		deleteImg = new ArrayList<Item>();
		deletePicture = new ArrayList<Item>();
		deleteImgAll = new ArrayList<Item>();
		deletePictureAll = new ArrayList<Item>();
		list = new ArrayList<String>();
		pictureList = new ArrayList<Item>();
		pictureList2 = pictureList;
		deleteImgDir = new ArrayList<String>();
		lsmap = new ArrayList<String>();
		imageAdapter = new ImageAdapter(DeleteActivity.this);// 照片适配
		pictureAdater = new PictureAdater(); // 图片适配
		photo = (Button) this.findViewById(R.id.delphoto);
		picture = (Button) this.findViewById(R.id.delpicture);
		delete = (Button) this.findViewById(R.id.deldelete);
		deleteAll = (Button) this.findViewById(R.id.deldeleteAll);
		photo.setOnClickListener(myClick);
		delete.setOnClickListener(myClick);
		deleteAll.setOnClickListener(myClick);
		picture.setOnClickListener(myClick);
		layout3 = (LinearLayout) this.findViewById(R.id.dellayout3);
		gridView = (GridView) this.findViewById(R.id.delgridView);
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
		if (pictureList != null) {
			pictureList.clear();
		}
		if (imgList != null) {
			imgList.clear();
		}
		if (deleteImgAll != null) {
			deleteImgAll.clear();
		}
		if (deletePictureAll != null) {
			deletePictureAll.clear();
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
			imageUrls = (String[]) listImgPath.toArray(new String[listImgPath
					.size()]);
			for (int i = 0; i < imageUrls.length; i++) {
				imageUrls[i] = "file://" + imageUrls[i];
			}
			pictures = lsmap.toArray(new String[lsmap.size()]);
			for (int i = 0; i < pictures.length; i++) {
				pictures[i] = "file://" + pictures[i];
			}
			options = new DisplayImageOptions.Builder()
					.showStubImage(R.drawable.ic_stub)
					.showImageForEmptyUri(R.drawable.ic_empty)
					.showImageOnFail(R.drawable.ic_error).cacheInMemory(true)
					.cacheOnDisc(true).displayer(new RoundedBitmapDisplayer(0))
					.build();
		}
		flag = 0;
		init();
		gridView.setAdapter(imageAdapter);
		imageAdapter.notifyDataSetChanged();
		gridView.setOnItemClickListener(new OnItemClickListener() {
			Item item;
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (flag == 0) {
					item = imgList.get(position);
				}
				if (flag == 1) {
					item = pictureList.get(position);
				}
				// holder.cb.setVisibility(View.VISIBLE);
				item.status = !item.status;// 取反
				holder.cb.setChecked(item.status);
				if (flag == 0) {
					if (item.status == true) {
						// positionList.add(arg2);
						all = 1;
						deleteImg.add(imgList.get(position));
						deleteImgDir.add(getListPic().get(position));
					} else {
						// positionList.remove(arg2);
						deleteImg.remove(MainActivity.imgList.get(position));
						deleteImgDir.remove(getListPic().get(position));
					}
				}
				if (flag == 1) {
					if (item.status == true) {
						// positionList.add(arg2);
						all = 1;
						deletePicture.add(pictureList.get(position));
						deleteImgDir.add(lsmap.get(position));
					} else {
						// positionList.remove(arg2);
						deletePicture.remove(pictureList.get(position));
						deleteImgDir.remove(lsmap.get(position));
					}
				}
			}
		});

	}

	class MyClick implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.delphoto:
				flag = 0;
				imgList.clear();
				init();
				photo.setBackgroundColor(Color.parseColor("#07D5E2"));
				picture.setBackgroundColor(Color.parseColor("#8A9AA8"));
				gridView.setAdapter(imageAdapter);
				imageAdapter.notifyDataSetChanged();
				break;
			case R.id.delpicture:
				flag = 1;
				pictureList.clear();
				init();
				picture.setBackgroundColor(Color.parseColor("#07D5E2"));
				photo.setBackgroundColor(Color.parseColor("#8A9AA8"));
				gridView.setAdapter(pictureAdater);
				pictureAdater.notifyDataSetChanged();
				break;
			case R.id.deldelete:
				if (flag == 0) {
					for (int i = 0; i < deleteImgDir.size(); i++) {
						File file = new File(deleteImgDir.get(i));
						if (file != null) {
							file.delete();
						}
					}
					MainActivity.imgList.removeAll(deleteImg);
					imageAdapter.notifyDataSetChanged();
				}
				if (flag == 1) {
					for (int i = 0; i < deleteImgDir.size(); i++) {
						File file = new File(deleteImgDir.get(i));
						if (file != null) {
							file.delete();
						}
					}
					pictureList.removeAll(deletePicture);
					pictureAdater.notifyDataSetChanged();
				}
				DeleteActivity.this.finish();
				break;
			case R.id.deldeleteAll:
				if (flag == 0) {
					if (isCheck) {
						for (int i = 0; i < imgList.size(); i++) {
							imgList.get(i).status = false;
							deleteImg.remove(imgList.get(i));
							deleteImgDir.remove(getListPic());
						}
						isCheck = false;
					} else {
						all = 2;
						for (int i = 0; i < imgList.size(); i++) {
							imgList.get(i).status = true;
						}
						deleteImg.addAll(imgList);
						deleteImgDir.addAll(getListPic());
						isCheck = true;
					}
					imageAdapter.notifyDataSetChanged();
				}
				if (flag == 1) {
					if (isCheck) {
						for (int i = 0; i < pictureList.size(); i++) {
							pictureList.get(i).status = false;
							deletePicture.remove(pictureList.get(i));
						}
						isCheck = false;
					} else {
						all = 2;
						for (int i = 0; i < pictureList.size(); i++) {
							pictureList.get(i).status = true;
							deletePicture.add(pictureList.get(i));
						}
						isCheck = true;
					}
					pictureAdater.notifyDataSetChanged();
				}
				break;
			default:
				break;
			}
		}
	}
	/* 获取的是MyCamera文件夹下面的所有图片路径 */
	private ArrayList<String> getListPic() {
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/DCIM/Camera/";
		Toast.makeText(DeleteActivity.this, path, 5000).show();
		File file = new File(path);
		if (file.exists()) {
			File[] f = file.listFiles();
			for (int i = f.length - 1; i >= 0; i--) {
				if (f[i].getName().endsWith(".jpg")) {
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
			return pictureList.size();
		}

		@Override
		public Object getItem(int position) {
			return pictureList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(DeleteActivity.this).inflate(
						R.layout.deleteitem, parent, false);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView
						.getTag();
			}
			holder.cb = (CheckBox) convertView.findViewById(R.id.cb1);
			holder.img = (ImageView) convertView.findViewById(R.id.img1);
			Item item = (Item) getItem(position);
			holder.cb.setChecked(item.status);
			try {
				imageLoader.init(ImageLoaderConfiguration
						.createDefault(DeleteActivity.this));
				imageLoader.displayImage(pictures[position], holder.img,
						options, animateFirstListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return convertView;
		}

	}

	// 照片适配
	public class ImageAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		ArrayList<ImageView> images = new ArrayList<ImageView>();
		File photos = null;
		protected ImageLoader imageLoader = ImageLoader.getInstance();
		private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

		public ImageAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return imgList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return imgList.get(position);
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
				convertView = mInflater.inflate(R.layout.deleteitem, parent,
						false);
				// holder.img = (ImageView) convertView.findViewById(R.id.img);
				holder.cb = (CheckBox) convertView.findViewById(R.id.cb1);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.img = (ImageView) convertView.findViewById(R.id.img1);
			// holder.img.setBackgroundResource((Integer)images.get(position));
			// holder.cBox.setChecked(isSelected.get(position));
			Item item = (Item) getItem(position);
			holder.cb.setChecked(item.status);
			try {
				imageLoader.init(ImageLoaderConfiguration
						.createDefault(DeleteActivity.this));
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

	class Item{
		public String img;
		public boolean status = true;

		public Item(String img, boolean b) {
			this.img = img;
			this.status = b;
		}
	}

	private void init() {
		if (flag == 0) {
			for (String s : imageUrls) {
				imgList.add(new Item(s, false));
			}
		}
		if (flag == 1) {
			for (String s : pictures) {
				pictureList.add(new Item(s, false));
			}
		}
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
		DeleteActivity.this.finish();
	}

}
