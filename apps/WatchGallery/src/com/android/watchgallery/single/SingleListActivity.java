package com.android.watchgallery.single;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.android.watchgallery.DeleteActivity;
import com.android.watchgallery.FileList;
import com.android.watchgallery.MainActivity;
import com.android.watchgallery.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
public class SingleListActivity extends Activity {
	ListView listView1;
	ImageView del;
	ViewHolder holder;
	DisplayImageOptions options;
	MyAdater myAdater = new MyAdater();
	MainActivity me = new MainActivity();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.single_list);
		listView1 = (ListView) this.findViewById(R.id.listView1);
		del = (ImageView) this.findViewById(R.id.del);
		Bundle exBundle = getIntent().getExtras();
		del.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		listView1.setAdapter(myAdater);
		int position=exBundle.getInt("position");
		listView1.setSelection(position);
		options = new DisplayImageOptions.Builder()
				.showStubImage(R.drawable.ic_empty)        //图片加载
				.showImageForEmptyUri(R.drawable.ic_stub)    //图片为空的时候
				.showImageOnFail(R.drawable.ic_error).cacheInMemory(true)
				.cacheOnDisc(true).displayer(new RoundedBitmapDisplayer(0))
				.build();
	}
    public class MyPictureAdater extends BaseAdapter{
    	protected ImageLoader imageLoader = ImageLoader.getInstance();
		private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return MainActivity.pictures.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return MainActivity.pictures[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}
    	
    }
	public class MyAdater extends BaseAdapter {
		protected ImageLoader imageLoader = ImageLoader.getInstance();
		private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();
		@Override
		public int getCount() {
			if(MainActivity.flag==0){
				return MainActivity.imageUrls.length;
			}
			if(MainActivity.flag==1){
				return MainActivity.pictures.length;
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			if(MainActivity.flag==0){
				return MainActivity.imageUrls[position];
			}
			if(MainActivity.flag==0){
				return MainActivity.pictures[position];
			}
			return 0;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(SingleListActivity.this)
						.inflate(R.layout.single_list_item, parent, false);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.img = (ImageView) convertView.findViewById(R.id.imageView1);
			holder.del = (ImageView) convertView.findViewById(R.id.imageView2);
			holder.del.setTag(position);
			holder.del.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					 final int index = (Integer) v.getTag();
					new AlertDialog.Builder(SingleListActivity.this).setTitle("确认删除吗？") 
				     .setIcon(android.R.drawable.ic_dialog_info) 
				     .setPositiveButton("确定", new DialogInterface.OnClickListener() { 
				         @Override 
				         public void onClick(DialogInterface dialog, int which) {
				        	
								if (MainActivity.flag == 0) {
								    //MainActivity.imgList.remove(MainActivity.imgList.get(index));
									MainActivity.imageUrls[index] = null;
									 File f=new File(MainActivity.listImgPath.get(index));
									 boolean h=f.delete();
									 if(MainActivity.list!=null){
										 MainActivity.list.clear();
									 }
									MainActivity.listImgPath = me.getListPic();
									MainActivity.imageUrls = (String[]) MainActivity.listImgPath.toArray(new String[MainActivity.listImgPath.size()]);
									for (int i = MainActivity.imageUrls.length - 1; i >= 0; i--) {
										MainActivity.imageUrls[i] = "file://" + MainActivity.imageUrls[i];
									}
								}
								if (MainActivity.flag == 1) {
									//MainActivity.pictureList.remove(MainActivity.pictureList.get(index));
									MainActivity.pictures[index] = null;
									File f = new File(MainActivity.lsmap.get(index));
									f.delete();
									if(MainActivity.lsmap!=null){
										 MainActivity.lsmap.clear();
									 }
									MainActivity.lsmap = FileList.findFile(Environment.getExternalStorageDirectory().getAbsolutePath());
									MainActivity.lsmap.removeAll(MainActivity.listImgPath);
									MainActivity.pictures = MainActivity.lsmap.toArray(new String[MainActivity.lsmap.size()]);
									for (int i = 0; i < MainActivity.pictures.length; i++) {
										MainActivity.pictures[i] = "file://" + MainActivity.pictures[i];
									}
								}
								myAdater.notifyDataSetChanged();
				         } 
				     }) 
				     .setNegativeButton("返回", new DialogInterface.OnClickListener() { 
				  
				         @Override 
				         public void onClick(DialogInterface dialog, int which) { 
				         // 点击“返回”后的操作,这里不设置没有任何操作 
				         } 
				     }).show();
					
				}
			});
			try {
				imageLoader.init(ImageLoaderConfiguration
						.createDefault(SingleListActivity.this));
				if(MainActivity.flag==0){
					imageLoader.displayImage(MainActivity.imageUrls[position],
							holder.img, options, animateFirstListener);
				}
				if(MainActivity.flag==1){
					imageLoader.displayImage(MainActivity.pictures[position],
							holder.img, options, animateFirstListener);
				}
//				imageLoader.displayImage(MainActivity.imageUrls[position],
//						holder.img, options, animateFirstListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return convertView;
		}

	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (MainActivity.flag == 0) {
			   MainActivity.myPhotoAdapter.notifyDataSetChanged();
			}
	       if (MainActivity.flag == 1) {
	    	   MainActivity.pictureAdater.notifyDataSetChanged();
			}
	}
	public class ViewHolder {
		public ImageView img;
		public ImageView del;
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
}
