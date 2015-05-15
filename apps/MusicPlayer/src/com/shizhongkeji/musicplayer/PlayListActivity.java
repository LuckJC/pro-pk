package com.shizhongkeji.musicplayer;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.shizhongkeji.adapter.MusicListAdapter;
import com.shizhongkeji.info.AppConstant;
import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.utils.MediaUtil;

public class PlayListActivity extends Activity {
	private ListView mListMusic;
	private TextView mNumberMusic;
	private MusicListAdapter mMisicListAdapter;
	private List<Mp3Info> mp3Infos;
	private int listPosition = 0; // 在List中的位置

	
	private int currentTime; // 当前播放位置
//	private int duration; // 歌曲时长
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
		mp3Infos = MediaUtil.getMp3Infos(this); // 获取手机中所得mp3文件集合
		mNumberMusic = (TextView) findViewById(R.id.number);
		if(mp3Infos != null && mp3Infos.size() >= 0 ){
			mNumberMusic.setText(mp3Infos.size()+"");
		}
		mMisicListAdapter = new MusicListAdapter(this, mp3Infos);
		mListMusic.setAdapter(mMisicListAdapter);
	}


	/**
	 * 
	 * 
	 * @param listPosition
	 */
	public void playMusic(int listPosition) {
		if (mp3Infos != null) {
			Mp3Info mp3Info = mp3Infos.get(listPosition);
//			Bitmap bitmap = MediaUtil.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), true,
//					true);// ��ȡר��λͼ����ΪСͼ
			Intent intent = new Intent(); // ����Intent������ת��PlayerActivity
			// ���һϵ��Ҫ���ݵ�����
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
	 * 
	 * 
	 * @author 
	 * 
	 */
	private class MusicListItemClickListener implements OnItemClickListener {
		/**
		 * 
		 */
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			listPosition = position; // ��ȡ�б�����λ��
			playMusic(listPosition); // ��������
		}

	}

	/**
	 * 
	 * <br>类描述:
	 * <br>功能详细描述:
	 * 
	 * @author  ZhouHaibo
	 * @date  [2015年5月5日]
	 */
	public class MusicListItemContextMenuListener implements OnCreateContextMenuListener {

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
			vibrator.vibrate(50); // ������
//			musicListItemDialog(); // �����󵯳��ĶԻ���
			final AdapterView.AdapterContextMenuInfo menuInfo2 = (AdapterView.AdapterContextMenuInfo) menuInfo;
			listPosition = menuInfo2.position; // ����б��λ��
		}

	}
}
