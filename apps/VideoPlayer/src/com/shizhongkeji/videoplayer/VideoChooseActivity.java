package com.shizhongkeji.videoplayer;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

public class VideoChooseActivity extends Activity {

	private static int height, width;
	private LinkedList<MovieInfo> mLinkedList = new LinkedList<MovieInfo>();;
	private MovieAdapter mAdapter;
	private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

	private ImageButton mDelete;
	public class MovieInfo {
		String displayName;
		String path;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog);
		mDelete = (ImageButton) findViewById(R.id.delete);
		mDelete.setVisibility(View.GONE);
		getVideoFile(mLinkedList, Environment.getExternalStorageDirectory());
		if (android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {

			Cursor cursor = getContentResolver().query(videoListUri,
					new String[] { "_display_name", "_data" }, null, null, null);
			int n = cursor.getCount();
			cursor.moveToFirst();
			LinkedList<MovieInfo> playList2 = new LinkedList<MovieInfo>();
			for (int i = 0; i != n; ++i) {
				MovieInfo mInfo = new MovieInfo();
				mInfo.displayName = cursor.getString(cursor.getColumnIndex("_display_name"));
				mInfo.path = cursor.getString(cursor.getColumnIndex("_data"));
				playList2.add(mInfo);
				cursor.moveToNext();
			}

			if (playList2.size() > mLinkedList.size()) {
				mLinkedList = playList2;
			}
		}
		GridView myListView = (GridView) findViewById(R.id.list);
		mAdapter = new MovieAdapter(this,mLinkedList);
		myListView.setAdapter(mAdapter);
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

									@Override
									public void onClick(DialogInterface dialog, int which) {
										Iterator<Entry<Integer, Boolean>> iterator = MovieAdapter.mMap.entrySet().iterator();
										while (iterator.hasNext()) {
											Map.Entry<Integer, Boolean> entry = iterator.next();
											boolean isCheck = entry.getValue();
											if (isCheck) {
												int index = entry.getKey();
												MovieInfo movieInfo = mLinkedList.get(index);
												String path = movieInfo.path;
												deleteVideo(path);
												mLinkedList.remove(index);
												mAdapter.notifyDataSetChanged();
											}
										}	

									}
								}).setNegativeButton("取消", new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {

									}
								}).create();
						dialog.show();
					}
				});
				return true;
			}
		});
		myListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				Toast.makeText(VideoChooseActivity.this, "点击了" + position, 0).show();
			}
		});
		myListView.requestFocus();
	}

	private void getVideoFile(final LinkedList<MovieInfo> list, File file) {

		file.listFiles(new FileFilter() {

			@Override
			public boolean accept(File file) {
				String name = file.getName();
				int i = name.indexOf('.');
				if (i != -1) {
					name = name.substring(i);
					if (name.equalsIgnoreCase(".mp4") || name.equalsIgnoreCase(".3gp")
							|| name.equalsIgnoreCase(".mkv")) {
						MovieInfo mi = new MovieInfo();
						mi.displayName = file.getName();
						mi.path = file.getAbsolutePath();
						list.add(mi);
						return true;
					}
				} else if (file.isDirectory()) {
					getVideoFile(list, file);
				}
				return false;
			}
		});
	}
	private void deleteVideo(String path){
		File file = new File(path);
		file.deleteOnExit();
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
	
}
