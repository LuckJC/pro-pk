package com.example.musicplayer;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.adapter.MusicListAdapter;
import com.example.info.AppConstant;
import com.example.info.Mp3Info;
import com.example.utils.MediaUtil;

public class PlayListActivity extends Activity {
	private ListView mListMusic;
	private TextView mNumberMusic;
	private MusicListAdapter mMisicListAdapter;
	private List<Mp3Info> mp3Infos;
	private int listPosition = 0; // 标识列表位置

	
	private long musicDuration; // 歌曲时间
	private int currentTime; // 当前时间
	private int duration; // 时长
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);
		initView();
	}

	private void initView() {
		mp3Infos = new ArrayList<Mp3Info>();
//		findViewById(R.id.add_song).setOnClickListener(this);
		mListMusic = (ListView) findViewById(R.id.song_list);
		mListMusic.setOnItemClickListener(new MusicListItemClickListener());
		mListMusic.setOnCreateContextMenuListener(new MusicListItemContextMenuListener());
		mp3Infos = MediaUtil.getMp3Infos(this); // 获取歌曲对象集合
		mNumberMusic = (TextView) findViewById(R.id.number);
		if(mp3Infos != null && mp3Infos.size() >= 0 ){
			mNumberMusic.setText(mp3Infos.size()+"");
		}
		mMisicListAdapter = new MusicListAdapter(this, mp3Infos);
		mListMusic.setAdapter(mMisicListAdapter);
	}


	/**
	 * 此方法通过传递列表点击位置来获取mp3Info对象
	 * 
	 * @param listPosition
	 */
	public void playMusic(int listPosition) {
		if (mp3Infos != null) {
			Mp3Info mp3Info = mp3Infos.get(listPosition);
//			Bitmap bitmap = MediaUtil.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), true,
//					true);// 获取专辑位图对象，为小图
			Intent intent = new Intent(); // 定义Intent对象，跳转到PlayerActivity
			// 添加一系列要传递的数据
			intent.putExtra("title", mp3Info.getTitle());
			intent.putExtra("url", mp3Info.getUrl());
			intent.putExtra("artist", mp3Info.getArtist());
			intent.putExtra("musicDuration", mp3Info.getDuration()+"");
			intent.putExtra("listPosition", listPosition);
			intent.putExtra("currentTime", currentTime);
//			intent.putExtra("repeatState", repeatState);
//			intent.putExtra("shuffleState", isShuffle);
			intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
			setResult(1, intent);
			finish();
		}
	}

	/**
	 * 列表点击监听器
	 * 
	 * @author wwj
	 * 
	 */
	private class MusicListItemClickListener implements OnItemClickListener {
		/**
		 * 点击列表播放音乐
		 */
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			listPosition = position; // 获取列表点击的位置
			playMusic(listPosition); // 播放音乐
		}

	}

	/**
	 * 上下文菜单显示监听器
	 * 
	 * @author Administrator
	 * 
	 */
	public class MusicListItemContextMenuListener implements OnCreateContextMenuListener {

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
			vibrator.vibrate(50); // 长按振动
//			musicListItemDialog(); // 长按后弹出的对话框
			final AdapterView.AdapterContextMenuInfo menuInfo2 = (AdapterView.AdapterContextMenuInfo) menuInfo;
			listPosition = menuInfo2.position; // 点击列表的位置
		}

	}
}
