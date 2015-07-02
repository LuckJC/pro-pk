package com.shizhongkeji.adapter;

import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shizhongkeji.GlobalApplication;
import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.musicplayer.R;

public class SystemMusicAdapter extends BaseAdapter {
	private List<Mp3Info> mp3Infos;
	private Context mContext;
	@SuppressLint("UseSparseArrays")
	public SystemMusicAdapter(List<Mp3Info> mp3Infos, Context mContext) {
		super();
		this.mp3Infos = mp3Infos;
		this.mContext = mContext;
		GlobalApplication.isSelecte = new HashMap<Integer, Boolean>();
		initData();
	}

	@Override
	public int getCount() {
		return mp3Infos.size();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate( R.layout.item_systemmusic, null);
		}
		RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.music_layout);
		Mp3Info mp3Info = mp3Infos.get(position);
		TextView lineNumber = (TextView) convertView.findViewById(R.id.line_number);
		TextView musicInfo = (TextView) convertView.findViewById(R.id.music_info);
		final CheckBox box = (CheckBox) convertView.findViewById(R.id.choose);
		lineNumber.setText((position + 1) + "");
		musicInfo.setText(mp3Info.getTitle() +" - "+ mp3Info.getArtist());
		layout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (GlobalApplication.isSelecte.get(position)) {
					GlobalApplication.isSelecte.put(position, false);
					box.setChecked(false);
				} else {
					GlobalApplication.isSelecte.put(position, true);
					box.setChecked(true);
				}
			}
		});
		box.setChecked(GlobalApplication.isSelecte.get(position));
		return convertView;
	}

	private void initData() {
		for (int i = 0; i < mp3Infos.size(); i++) {
			GlobalApplication.isSelecte.put(i, false);
		}
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

}
