package com.shizhongkeji.videoplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shizhongkeji.videoplayer.VideoChooseActivity.PublicTools;

public class MovieAdapter extends BaseAdapter {
	private Context mContext;
//	private LinkedList<MovieInfo> mLinkedList;
	public static HashMap<Integer, Boolean> mMap = null;
//	private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
//	SimpleDateFormat   sDateFormat   =   new   SimpleDateFormat("yyyy-MM-dd   hh:mm:ss"); 

	private List<VideoObject> videoList;
	/** 默认图标 */
	private Bitmap mDefaultBitmap = null;
	/** 图标列表MAP */
	private ConcurrentHashMap<Integer, Bitmap> mThumbHash = null;
	private List<VideoItemHolder> holderList = new ArrayList<MovieAdapter.VideoItemHolder>();

	public MovieAdapter(Context mContext) {
		super();
		this.mContext = mContext;
//		initData();
	}
	public List<VideoItemHolder> getHolderList() {
		return holderList;
	}

	public void setVideoList(List<VideoObject> videoList) {
		this.videoList = videoList;
		mMap = initMap(videoList.size());
	}

	public void setmDefaultBitmap(Bitmap mDefaultBitmap) {
		this.mDefaultBitmap = mDefaultBitmap;
	}

	public void setmThumbHash(ConcurrentHashMap<Integer, Bitmap> mThumbHash) {
		this.mThumbHash = mThumbHash;
	}
	@Override
	public int getCount() {

		return videoList == null ? 0 : videoList.size();
	}

	@Override
	public Object getItem(int position) {

		return videoList.get(position);
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.list, null);
			VideoItemHolder holder = new VideoItemHolder();
			holder.imgVideoIcon = (RelativeLayout) convertView.findViewById(R.id.thumb);
			holder.txtVideoTitle = (TextView) convertView.findViewById(R.id.text);
			convertView.setTag(holder);
			holderList.add(holder);
		}
		VideoItemHolder holder = (VideoItemHolder) convertView.getTag();
		holder.refresh(position);
		ImageButton play = (ImageButton) convertView.findViewById(R.id.play);
		CheckBox box = (CheckBox) convertView.findViewById(R.id.box);
	
		if (VideoApplication.isdelete) {
			play.setOnClickListener(null);
			box.setVisibility(View.VISIBLE);
			box.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (mMap.get(position)) {
						mMap.put(position, false);
					} else {
						mMap.put(position, true);
					}
				}
			});
			box.setChecked(mMap.get(position));
		} else {
			play.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if(!VideoApplication.isdelete){
						Intent intent = new Intent();
						intent.setClass(mContext, VideoPlayerActivity.class);
						VideoObject videoObject = videoList.get(position);
						String displayName = videoObject.getTitle();
						String path = videoObject.getMediapath();
						intent.putExtra("displayName", displayName);
						intent.putExtra("path", path);
						mContext.startActivity(intent);
					}
				}
			});
			box.setVisibility(View.GONE);
		}

		return convertView;
	}

	private HashMap<Integer, Boolean> initMap(int count) {
		HashMap<Integer, Boolean> hashMap = new HashMap<Integer, Boolean>();
		for (int i = 0; i < count; i++) {
			hashMap.put(i, false);
		}
		return hashMap;
	}

//	public Bitmap getVideoThumbnail(String filePath) {
//		Bitmap bitmap = null;
//		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//		try {
//			retriever.setDataSource(filePath);
//			bitmap = retriever.getFrameAtTime();
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (RuntimeException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				retriever.release();
//			} catch (RuntimeException e) {
//				e.printStackTrace();
//			}
//		}
//		return bitmap;
//	}

//	private Bitmap compressImage(Bitmap image) {
//
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
//		int options = 100;
//		while (baos.toByteArray().length / 1024 > 100) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
//			baos.reset();// 重置baos即清空baos
//			image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
//			options -= 10;// 每次都减少10
//		}
//		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
//		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
//		return bitmap;
//	}
//	private Bitmap comp(Bitmap image) {
//		
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
//		image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//		if( baos.toByteArray().length / 1024>1024) {//判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出	
//			baos.reset();//重置baos即清空baos
//			image.compress(Bitmap.CompressFormat.JPEG, 50, baos);//这里压缩50%，把压缩后的数据存放到baos中
//		}
//		ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
//		BitmapFactory.Options newOpts = new BitmapFactory.Options();
//		//开始读入图片，此时把options.inJustDecodeBounds 设回true了
//		newOpts.inJustDecodeBounds = true;
//		Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
//		newOpts.inJustDecodeBounds = false;
//		int w = newOpts.outWidth;
//		int h = newOpts.outHeight;
//		//现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
//		float hh = 800f;//这里设置高度为800f
//		float ww = 480f;//这里设置宽度为480f
//		//缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
//		int be = 1;//be=1表示不缩放
//		if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
//			be = (int) (newOpts.outWidth / ww);
//		} else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
//			be = (int) (newOpts.outHeight / hh);
//		}
//		if (be <= 0)
//			be = 1;
//		newOpts.inSampleSize = be;//设置缩放比例
//		//重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
//		isBm = new ByteArrayInputStream(baos.toByteArray());
//		bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
//		return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
//	}
//	public void initData(){
//		videoList.clear();
//		getVideoFile(videoList, Environment.getExternalStorageDirectory());
//		if (android.os.Environment.getExternalStorageState().equals(
//				android.os.Environment.MEDIA_MOUNTED)) {
//
//			Cursor cursor = mContext.getContentResolver().query(videoListUri,
//					new String[] { "_display_name", "_data" }, null, null, null);
//			int n = cursor.getCount();
//			cursor.moveToFirst();
//			LinkedList<MovieInfo> playList2 = new LinkedList<MovieInfo>();
//			for (int i = 0; i != n; ++i) {
//				MovieInfo mInfo = new MovieInfo();
//				mInfo.displayName = cursor.getString(cursor.getColumnIndex("_display_name"));
//				mInfo.path = cursor.getString(cursor.getColumnIndex("_data"));
//				playList2.add(mInfo);
//				cursor.moveToNext();
//			}
//
//			if (playList2.size() > mLinkedList.size()) {
//				mLinkedList = playList2;
//			}
//		}
//	}
//	private void getVideoFile(final List<VideoObject> list, File file) {
//
//		file.listFiles(new FileFilter() {
//
//			@Override
//			public boolean accept(File file) {
//				String name = file.getName();
//				int i = name.indexOf('.');
//				if (i != -1) {
//					name = name.substring(i);
//					if (name.equalsIgnoreCase(".mp4") || name.equalsIgnoreCase(".3gp")
//							|| name.equalsIgnoreCase(".mkv")) {
//						MovieInfo mi = new MovieInfo();
//						mi.displayName = file.getName();
//						mi.path = file.getAbsolutePath();
//						list.add(mi);
//						return true;
//					}
//				} else if (file.isDirectory()) {
//					getVideoFile(list, file);
//				}
//				return false;
//			}
//		});
//	}
	@SuppressLint("NewApi")
	public class VideoItemHolder {

		public TextView txtVideoTitle;
//		public TextView txtVideoDuration;
//		public TextView txtVideoSize;
//		public ImageView imgVideoIcon;
		public RelativeLayout imgVideoIcon;
		public Bitmap bm;

		/** VideoObject保存视频信息 */
		private VideoObject mVideoObject;

		/**
		 * Function:加载数据<br>
		 * 
		 * @author ZYT DateTime 2014-5-16 上午10:21:42<br>
		 * @param pos
		 * <br>
		 */
		private void refresh(int pos) {
			mVideoObject = videoList.get(pos);
			txtVideoTitle.setText(mVideoObject.getTitle());
//			txtVideoDuration.setText(mVideoObject.getDuration());
//			txtVideoSize.setText(mVideoObject.getSize());

			// 动态刷新列表的图片
			if (mVideoObject.getThumbnailState() == PublicTools.THUMBNAIL_PREPARED) {
				if (mDefaultBitmap != null && mThumbHash != null) {
					bm = mVideoObject.getMiniThumbBitmap(false, mThumbHash,
							mDefaultBitmap);
				}
				imgVideoIcon.setBackground(new BitmapDrawable(bm));
			} else {
				if (mDefaultBitmap != null)
					imgVideoIcon.setBackground(new BitmapDrawable(mDefaultBitmap));
				// mUseDefault = true;
			}
		}

		/**
		 * Function:刷新缩略图<br>
		 * 
		 * @author ZYT DateTime 2014-5-16 下午1:56:57<br>
		 * <br>
		 */
		private void refeshThumbnail() {
			// 如果mVideoObject缩略图状态不是THUMBNAIL_PREPARED，要调用getMiniThumbBitmap（）方法加载缩略图
			if (mDefaultBitmap != null
					&& mThumbHash != null
					&& mVideoObject.getThumbnailState() != PublicTools.THUMBNAIL_PREPARED) {
				bm = mVideoObject.getMiniThumbBitmap(false, mThumbHash,
						mDefaultBitmap);
			}
			imgVideoIcon.setBackground(new BitmapDrawable(bm));
		}

		private Handler mHandler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				refeshThumbnail();
			}
		};

		/**
		 * Function:发送消息更新列表的图标，由Adapter的持有者获得Holder列表调用这个方法<br>
		 * 
		 * @author ZYT DateTime 2014-5-16 下午1:58:57<br>
		 * <br>
		 */
		public void sendRefreshMsg() {
			mHandler.sendMessage(mHandler.obtainMessage());
		}
	}
}
