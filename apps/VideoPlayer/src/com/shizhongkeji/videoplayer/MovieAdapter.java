package com.shizhongkeji.videoplayer;

import java.util.HashMap;
import java.util.LinkedList;

import com.shizhongkeji.videoplayer.VideoChooseActivity.MovieInfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MovieAdapter extends BaseAdapter {
	private Context mContext;
	private LinkedList<MovieInfo> mLinkedList;
	public  static HashMap<Integer, Boolean> mMap = null;
	public MovieAdapter(Context mContext, LinkedList<MovieInfo> mLinkedList) {
		super();
		this.mContext = mContext;
		
		this.mLinkedList = mLinkedList;
		mMap = initMap(mLinkedList.size());
	}

	@Override
	public int getCount() {
		
		return mLinkedList.size();
	}

	@Override
	public Object getItem(int position) {
		
		return position;
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
		}
		MovieInfo movieInfo = mLinkedList.get(position);
		TextView text = (TextView) convertView.findViewById(R.id.text);
		RelativeLayout image = (RelativeLayout) convertView.findViewById(R.id.thumb);
		ImageButton play = (ImageButton) convertView.findViewById(R.id.play);
		CheckBox box = (CheckBox) convertView.findViewById(R.id.box);
		image.setBackground(new BitmapDrawable(getVideoThumbnail(movieInfo.path)));
		text.setText(movieInfo.displayName);
		if(VideoApplication.isdelete){
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
		}else{
			play.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
					intent.setClass(mContext, VideoPlayerActivity.class);
					MovieInfo movieInfo = mLinkedList.get(position);
					String displayName = movieInfo.displayName;
					String path = movieInfo.path;
					intent.putExtra("displayName", displayName);
					intent.putExtra("path", path);
					mContext.startActivity(intent);
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
	public Bitmap getVideoThumbnail(String filePath) {
		Bitmap bitmap = null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			retriever.setDataSource(filePath);
			bitmap = retriever.getFrameAtTime();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} finally {
			try {
				retriever.release();
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		return bitmap;
	}

}
