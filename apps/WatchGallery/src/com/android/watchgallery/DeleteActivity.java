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
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
	Button deldelete;
	Button deldeleteAll;
	MainActivity me=new MainActivity();
	GridView gridView;
	LinearLayout layout3;
	public ImageAdapter imageAdapter;
	public PictureAdater pictureAdater;
	DisplayMetrics dm;
	ViewHolder holder;
	public static List<String> lsmap;
	DisplayImageOptions options;
	String path = null;
	ContentResolver resolver;
	int all = 0;
//	public static int flag = 0; // 照片跟图片的标志
	public ArrayList<String> list;
	boolean isCheck = false;
	public static List<Item> imgList;
//	public static List<Item> deleteImg; // 一个一个地选择的删除的照片数组
	public static ArrayList<String> deleteImg;
	public static List<Item> deletePicture; // 一个一个地选择的删除的图片数组
	public static List<Item> deleteImgAll; // 全选的删除的照片数组
	public static List<Item> deletePictureAll; // 全选的删除的图片数组
	public static List<Item> pictureList;
	ArrayList<String> deleteImgDir; // 删除照片跟相片的路径
	Item item=null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		MyClick myClick = new MyClick();
		setContentView(R.layout.deletemain);
		imgList = new ArrayList<Item>();
//		deleteImg = new ArrayList<Item>();
		deleteImg = new ArrayList<String>();
		deletePicture = new ArrayList<Item>();
		deleteImgAll = new ArrayList<Item>();
		deletePictureAll = new ArrayList<Item>();
		list = new ArrayList<String>();
		pictureList = new ArrayList<Item>();
		deleteImgDir = new ArrayList<String>();
		lsmap = new ArrayList<String>();
		imageAdapter = new ImageAdapter(DeleteActivity.this);// 照片适配
		pictureAdater = new PictureAdater(); // 图片适配
		photo = (Button) this.findViewById(R.id.delphoto);
		picture = (Button) this.findViewById(R.id.delpicture);
		deldelete = (Button) this.findViewById(R.id.deldelete);
		deldeleteAll = (Button) this.findViewById(R.id.deldeleteAll);
		photo.setOnClickListener(myClick);
		deldelete.setOnClickListener(myClick);
		deldeleteAll.setOnClickListener(myClick);
		picture.setOnClickListener(myClick);
		layout3 = (LinearLayout) this.findViewById(R.id.dellayout3);
		gridView = (GridView) this.findViewById(R.id.delgridView);
		if (list != null) {
			list.clear();
		}
		if (lsmap != null) {
			lsmap.clear();
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
			options = new DisplayImageOptions.Builder()
					.showStubImage(R.drawable.ic_empty)          //图片缓冲的时候
					.showImageForEmptyUri(R.drawable.ic_stub)   //图片为空的时候
					.showImageOnFail(R.drawable.ic_error).cacheInMemory(true)
					.cacheOnDisc(true).displayer(new RoundedBitmapDisplayer(0))
					.build();
		}
//		flag = 0;
		init();
		if(MainActivity.flag==0){
			photo.setBackgroundColor(Color.parseColor("#07D5E2"));
			picture.setBackgroundColor(Color.parseColor("#8A9AA8"));
			gridView.setAdapter(imageAdapter);
			imageAdapter.notifyDataSetChanged();
		}
		if(MainActivity.flag==1){
			photo.setBackgroundColor(Color.parseColor("#8A9AA8"));
			picture.setBackgroundColor(Color.parseColor("#07D5E2"));
			gridView.setAdapter(pictureAdater);
			pictureAdater.notifyDataSetChanged();
		}
		gridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (MainActivity.flag == 0) {
					item = imgList.get(position);
				}
				if (MainActivity.flag == 1) {
					item = pictureList.get(position);
				}
				item.status = !item.status;// 取反
				holder=(ViewHolder)view.getTag();
				holder.cb.setChecked(item.status);
				if (MainActivity.flag == 0) {
					if (item.status == true) {
						// positionList.add(arg2);
						all = 1;
//						deleteImg.add(imgList.get(position));
						deleteImg.add(MainActivity.imageUrls[position]);
						deleteImgDir.add(me.getListPic().get(position));
					} else {
						// positionList.remove(arg2);
						deleteImg.remove(MainActivity.imageUrls[position]);
						deleteImgDir.remove(me.getListPic().get(position));
					}
				}
				if (MainActivity.flag == 1) {
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
				MainActivity.flag = 0;
				imgList.clear();
				init();
				photo.setBackgroundColor(Color.parseColor("#07D5E2"));
				picture.setBackgroundColor(Color.parseColor("#8A9AA8"));
				gridView.setAdapter(imageAdapter);
				imageAdapter.notifyDataSetChanged();
				break;
			case R.id.delpicture:
				MainActivity.flag = 1;
				pictureList.clear();
				init();
				picture.setBackgroundColor(Color.parseColor("#07D5E2"));
				photo.setBackgroundColor(Color.parseColor("#8A9AA8"));
				gridView.setAdapter(pictureAdater);
				pictureAdater.notifyDataSetChanged();
				break;
			case R.id.deldelete:
				if(deleteImgDir.size()==0){
					//无删除选项内容时不进行操作
				}else{
					new AlertDialog.Builder(DeleteActivity.this).setTitle("确认删除吗？") 
				     .setIcon(android.R.drawable.ic_dialog_info) 
				     .setPositiveButton("确定", new DialogInterface.OnClickListener() { 
				         @Override 
				         public void onClick(DialogInterface dialog, int which) {
				        	 if (MainActivity.flag == 0) {
									for (int i = 0; i < deleteImgDir.size(); i++) {
										File file = new File(deleteImgDir.get(i));
										if (file != null) {
											file.delete();
										}
									}
									if(MainActivity.list!=null){
										 MainActivity.list.clear();
									 }
									MainActivity.listImgPath = me.getListPic();
									MainActivity.imageUrls = (String[]) MainActivity.listImgPath.toArray(new String[MainActivity.listImgPath.size()]);
									for (int i = MainActivity.imageUrls.length - 1; i >= 0; i--) {
										MainActivity.imageUrls[i] = "file://" + MainActivity.imageUrls[i];
									}
//									imgList.removeAll(deleteImg);
									MainActivity.myPhotoAdapter.notifyDataSetChanged();
								}
								if (MainActivity.flag == 1) {
									for (int i = 0; i < deleteImgDir.size(); i++) {
										File file = new File(deleteImgDir.get(i));
										if (file != null) {
											file.delete();
										}
									}
									if(MainActivity.lsmap!=null){
										 MainActivity.lsmap.clear();
									 }
									MainActivity.lsmap = FileList.findFile(Environment.getExternalStorageDirectory().getAbsolutePath());
									MainActivity.lsmap.removeAll(MainActivity.listImgPath);
									MainActivity.pictures = MainActivity.lsmap.toArray(new String[MainActivity.lsmap.size()]);
									for (int i = 0; i < MainActivity.pictures.length; i++) {
										MainActivity.pictures[i] = "file://" + MainActivity.pictures[i];
									}
//									pictureList.removeAll(deletePicture);
									MainActivity.pictureAdater.notifyDataSetChanged();
								}
								DeleteActivity.this.finish(); 
				         } 
				     }) 
				     .setNegativeButton("返回", new DialogInterface.OnClickListener() { 
				  
				         @Override 
				         public void onClick(DialogInterface dialog, int which) { 
				         // 点击“返回”后的操作,这里不设置没有任何操作 
				         } 
				     }).show();
				}
				
				
				break;
			case R.id.deldeleteAll:
				if (MainActivity.flag == 0) {
					if (isCheck) {
						for (int i = 0; i < imgList.size(); i++) {
							imgList.get(i).status = false;
						}
						deleteImgDir.removeAll(me.getListPic());
						deleteImgDir.size();
						isCheck = false;
					} else {
						all = 2;
						for (int i = 0; i < imgList.size(); i++) {
							imgList.get(i).status = true;
							
						}
//						deleteImg.addAll(MainActivity.imageUrls);
						deleteImgDir.addAll(me.getListPic());
						deleteImgDir.size();
						isCheck = true;
					}
					imageAdapter.notifyDataSetChanged();
				}
				if (MainActivity.flag == 1) {
					if (isCheck) {
						for (int i = 0; i < pictureList.size(); i++) {
							pictureList.get(i).status = false;
						}
						deleteImgDir.removeAll(lsmap); 
						isCheck = false;
					} else {
						all = 2;
						for (int i = 0; i < pictureList.size(); i++) {
							pictureList.get(i).status = true;
						}
						deleteImgDir.addAll(lsmap);
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
			if (convertView == null||(holder = (ViewHolder) convertView.getTag()) == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(DeleteActivity.this).inflate(
						R.layout.deleteitem, parent, false);
				holder.cb = (CheckBox) convertView.findViewById(R.id.cb1);
				holder.img = (ImageView) convertView.findViewById(R.id.img1);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			Item item = (Item) getItem(position);
			holder.cb.setChecked(item.status);
			try {
				imageLoader.init(ImageLoaderConfiguration
						.createDefault(DeleteActivity.this));
				imageLoader.displayImage(MainActivity.pictures[position], holder.img,
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
			if (convertView == null||(holder = (ViewHolder) convertView.getTag()) == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.deleteitem, parent,
						false);
				holder.cb = (CheckBox) convertView.findViewById(R.id.cb1);
				holder.img = (ImageView) convertView.findViewById(R.id.img1);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			Item item = (Item) getItem(position);
			holder.cb.setChecked(item.status);
			try {
				imageLoader.init(ImageLoaderConfiguration
						.createDefault(DeleteActivity.this));
				imageLoader.displayImage(MainActivity.imageUrls[position], holder.img,
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
		public boolean status = false;

		public Item(String img, boolean b) {
			this.img = img;
			this.status = b;
		}
	}

	private void init() {
		if (MainActivity.flag == 0) {
			for (String s : MainActivity.imageUrls) {
				 imgList.add(new Item(s, false));
			}
		}
		if (MainActivity.flag == 1) {
			for (String s :  MainActivity.pictures) {
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
					FadeInBitmapDisplayer.animate(imageView, 500); 
					displayedImages.add(imageUri); 
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
