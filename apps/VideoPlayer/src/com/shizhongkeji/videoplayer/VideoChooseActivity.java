package com.shizhongkeji.videoplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.shizhongkeji.videoplayer.MovieAdapter.VideoItemHolder;

public class VideoChooseActivity extends Activity {

	private static int height, width;
//	private LinkedList<MovieInfo> mLinkedList = new LinkedList<MovieInfo>();;
	private MovieAdapter mAdapter;
	private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
	
	public static Display mDisplay;
	/** 缩略图表 ，Adapter 要用到，使用参数传递 */
	private ConcurrentHashMap<Integer, Bitmap> mThumbHash = new ConcurrentHashMap<Integer, Bitmap>();
	/** 默认缩略图，Adapter 要用到，使用参数传递* */
	private Bitmap mDefaultBm;
	private List<VideoObject> mVideoObjectList = new ArrayList<VideoObject>();
	
	private static final int STATE_STOP = 0;
	private static final int STATE_IDLE = 1;
	private static final int STATE_TERMINATE = 2;
	/**
	 * STATE_STOP、STATE_IDLE、STATE_TERMINATE 三种工作状态, STOP
	 * 状态进行加载缩略图；IDEL状态进行等待用户操作；TERMINATE状态退出线程
	 */
	private int workStatus;
	
	private TextView none;
	private ImageButton mDelete;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog);
		initData();
		
		initUI();
		
//		getVideoFile(mLinkedList, Environment.getExternalStorageDirectory());
//		if (android.os.Environment.getExternalStorageState().equals(
//				android.os.Environment.MEDIA_MOUNTED)) {
//
//			Cursor cursor = getContentResolver().query(videoListUri,
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
//		if(mLinkedList.size() > 0){
//			none.setVisibility(View.GONE);
//		}
		
	}
	private void initData() {
		mVideoObjectList.clear();
		/************** 初始化数据 ***************/
		mDefaultBm = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.ic_default);
		VideoManager mVideoManager = new VideoManager();
		VideoList mVideoList = mVideoManager.getAllVideo(this,
				getContentResolver(), VideoManager.INCLUDE_VIDEOS,
				VideoManager.SORT_ASCENDING);
		int videoCount = mVideoList.getCount();
		for (int i = 0; i < videoCount; i++) {
			VideoObject mVideoObject = mVideoList.getVideoAt(i);
			String duration = mVideoObject.getDuration();
			// Bitmap bm = mVideoObject.getMiniThumbBitmap(false, mThumbHash,
			// mDefaultBm);
			String mediaPath = mVideoObject.getMediapath();
			System.out.println(mediaPath + "   " + duration);
			mVideoObjectList.add(mVideoObject);
		}
	}
	private void initUI(){
		mDelete = (ImageButton) findViewById(R.id.delete);
		mDelete.setVisibility(View.GONE);
		none = (TextView) findViewById(R.id.none);
		GridView myListView = (GridView) findViewById(R.id.list);
		mAdapter = new MovieAdapter(VideoChooseActivity.this);
		myListView.setAdapter(mAdapter);
		mAdapter.setVideoList(mVideoObjectList);
		mAdapter.setmThumbHash(mThumbHash);
		mAdapter.setmDefaultBitmap(mDefaultBm);
		myListView.setAdapter(mAdapter);
		// 列表结束

		/****************** 注册监听器 **********************/
		myListView.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// SCROLL_STATE_IDLE, SCROLL_STATE_TOUCH_SCROLL or
				// SCROLL_STATE_IDLE.
				switch (scrollState) {
				case SCROLL_STATE_FLING:
				case SCROLL_STATE_TOUCH_SCROLL:
					workStatus = STATE_IDLE;
					break;
				case SCROLL_STATE_IDLE:
					workStatus = STATE_STOP;
					break;

				default:
					break;
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

			}
		});

		/*************** 驱动线程更新缩略图 ****************/
		startLoadThumbThread();
		myListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				mDelete.setVisibility(View.VISIBLE);
				VideoApplication.isdelete = true;
				mAdapter.notifyDataSetChanged();
				mDelete.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Dialog dialog = new AlertDialog.Builder(VideoChooseActivity.this).setTitle("删除文件")
								.setMessage("你确定要删除选中的文件")
								.setPositiveButton("确定", new DialogInterface.OnClickListener() {
									List<VideoObject> list = new ArrayList<VideoObject>();
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Iterator<Entry<Integer, Boolean>> iterator = MovieAdapter.mMap.entrySet().iterator();
										while (iterator.hasNext()) {
											Map.Entry<Integer, Boolean> entry = iterator.next();
											boolean isCheck = entry.getValue();
											if (isCheck) {
												int index = entry.getKey();
												VideoObject movieInfo = mVideoObjectList.get(index);
												String path = movieInfo.getMediapath();
												deleteVideo(path);
												ContentResolver resolver = getContentResolver();
												resolver.delete(videoListUri, "_data = ?", new String[]{path});
												list.add(movieInfo);
											}
										}
										Log.e("mVideoObjectList", mVideoObjectList.size()+"");
										for (int i = 0; i < list.size(); i++) {
											mVideoObjectList.remove(list.get(i));
										}
										Log.e("mVideoObjectList", mVideoObjectList.size()+"");
										mAdapter.setVideoList(mVideoObjectList);
										VideoApplication.isdelete = false;
										mDelete.setVisibility(View.GONE);
										mAdapter.notifyDataSetChanged();
										hintNone();
									}
								}).setNegativeButton("取消", new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {

									}
								}).create();
						dialog.show();
						dialog.setCanceledOnTouchOutside(false);
					}
				});
				return true;
			}
		});
		myListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(!VideoApplication.isdelete){
					Intent intent = new Intent();
					intent.setClass(VideoChooseActivity.this, VideoPlayerActivity.class);
					VideoObject videoObject = mVideoObjectList.get(position);
					String displayName = videoObject.getTitle();
					String path = videoObject.getMediapath();
					intent.putExtra("displayName", displayName);
					intent.putExtra("path", path);
					startActivity(intent);
				}
			}
		});
		myListView.requestFocus();
		hintNone();
	}
	private void startLoadThumbThread() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				init();
				loadThumb();
				System.out
						.println("======= loadThumbThread terminate ==========");
			}

			private void loadThumb() {
				while (workStatus != STATE_TERMINATE) {
					switch (workStatus) {
					case STATE_STOP:
						stop();
						break;

					case STATE_IDLE:
						idle();
						break;

					default:
						break;
					}
				}
			}

			// 初始化
			private void init() {
				workStatus = STATE_STOP;// 一开始为STOP状态，加载缩略图
			}

			// 刷新内标操作
			private void stop() {
				List<VideoItemHolder> videoObjects = mAdapter.getHolderList();
				for (VideoItemHolder videoItemHolder : videoObjects) {
					videoItemHolder.sendRefreshMsg();
					PublicTools.sleep(PublicTools.SHORT_INTERVAL);
				}
				// 更新结束列表，把状态变为IDLE,进入idle()方法循环等待
				workStatus = STATE_IDLE;
			}

			// 空闲状态，等待用户消息
			private void idle() {
				while (true) {
					// 循环等待
					PublicTools.sleep(PublicTools.LONG_INTERVAL);
					// STOP 状态为需要刷新图标，跳出循环，进行刷新图标操作
					if (workStatus == STATE_STOP) {
						return;
					}
				}
			}

		}).start();
	}
//	private void getVideoFile(final LinkedList<MovieInfo> list, File file) {
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
	private void deleteVideo(String path){
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(VideoApplication.isdelete){
				VideoApplication.isdelete = false;
				mDelete.setVisibility(View.GONE);
				mAdapter.notifyDataSetChanged();
			}else{
				this.finish();
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	public static class PublicTools {

		/** 缩略图准备好，可以显示 */
		public static final int THUMBNAIL_PREPARED = 1;
		/** 缩略图空 ，显示默认 */
		public static final int THUMBNAIL_EMPTY = 0;
		/** 缩略图损坏 ，显示默认 */
		public static final int THUMBNAIL_CORRUPTED = -1;

		public static final int MINI_INTERVAL = 50;
		public static final int SHORT_INTERVAL = 150;
		public static final int MIDDLE_INTERVAL = 300;
		public static final int LONG_INTERVAL = 600;
		public static final int LONG_LONG_INTERVAL = 6000;

		private static final int FILENAMELENGTH = 80;

		public static long getBucketId(String path) {
			return path.toLowerCase().hashCode();
		}

		/**
		 * Function:从母串截取指定长度的子串，一个非ASCII占用两个字节<br>
		 * 
		 * @author ZYT DateTime 2014-5-15 下午2:52:07<br>
		 * @param origin
		 *            母串
		 * @param length
		 *            截取的长度
		 * @return 子串 <br>
		 */
		public static String cutString(String origin, int length) {
			char[] c = origin.toCharArray();
			int len = 0;
			int strEnd = 0;
			for (int i = 0; i < c.length; i++) {
				strEnd++;
				// Ascii 字符占用一个字节，非Ascii占用两个字节（0x00<=Ascii<0x80）
				len = (c[i] / 0x80 == 0) ? (len + 1) : (len + 2);
				if (len > length || (len == length && i != (c.length - 1))) {
					origin = origin.substring(0, strEnd) + "...";
					break;
				}
			}
			return origin;
		}

		public static String replaceFilename(String filepath, String name) {
			String newPath = "";
			int lastSlash = filepath.lastIndexOf('/');
			if (lastSlash >= 0) {
				lastSlash++;
				if (lastSlash < filepath.length()) {
					newPath = filepath.substring(0, lastSlash);
				}
			}
			newPath = newPath + name;
			int lastDot = filepath.lastIndexOf('.');
			if (lastDot > 0) {
				newPath = newPath
						+ filepath.substring(lastDot, filepath.length());
			}
			return newPath;
		}

		public static boolean isFilenameIllegal(String filename) {
			return (filename.length() <= FILENAMELENGTH);
		}

		/**
		 * Function:判断是否横屏<br>
		 * 
		 * @author ZYT DateTime 2014-5-15 下午2:36:40<br>
		 * @return true：横屏；false:竖屏 <br>
		 */
		public static boolean isLandscape() {
			// Log.v(TAG,"isLandscape : "+ mDisplay.getOrientation());
			return (1 == mDisplay.getOrientation());
		}

		public static boolean isFileExist(String filepath) {
			File file = new File(filepath);
			return file.exists();
		}

		public static void sleep(int interval) {
			try {
				Thread.sleep(interval);
			} catch (Exception e) {
			}
		}

		public static AlertDialog hint(Context context, int StringId) {
			return new AlertDialog.Builder(context)
					.setMessage(context.getString(StringId))
					.setNeutralButton("确定", null).show();
		}

		public static Cursor query(ContentResolver resolver, Uri uri,
				String[] projection, String selection, String[] selectionArgs,
				String sortOrder) {
			try {
				if (resolver == null) {
					return null;
				}
				return resolver.query(uri, projection, selection,
						selectionArgs, sortOrder);
			} catch (UnsupportedOperationException ex) {
				return null;
			}
		}

		public static boolean isMediaScannerScanning(ContentResolver cr) {
			boolean result = false;
			Cursor cursor = query(cr, MediaStore.getMediaScannerUri(),
					new String[] { MediaStore.MEDIA_SCANNER_VOLUME }, null,
					null, null);
			if (cursor != null) {
				if (cursor.getCount() == 1) {
					cursor.moveToFirst();
					result = "external".equals(cursor.getString(0));
				}
				cursor.close();
			}

			return result;
		}

		public static boolean isVideoStreaming(Uri uri) {
			return ("http".equalsIgnoreCase(uri.getScheme()) || "rtsp"
					.equalsIgnoreCase(uri.getScheme()));
		}
	}
	private void hintNone(){
		if(mVideoObjectList.size() >0){
			none.setVisibility(View.GONE);
		}else{
			none.setVisibility(View.VISIBLE);
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();

		workStatus = STATE_TERMINATE;

	}
	
}
