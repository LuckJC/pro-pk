package com.shizhongkeji.musicplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.shizhongkeji.GlobalApplication;
import com.shizhongkeji.adapter.MusicListAdapter;
import com.shizhongkeji.info.AppConstant;
import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.sqlutils.DBManager;
import com.shizhongkeji.swipemenulistview.SwipeMenu;
import com.shizhongkeji.swipemenulistview.SwipeMenuCreator;
import com.shizhongkeji.swipemenulistview.SwipeMenuItem;
import com.shizhongkeji.swipemenulistview.SwipeMenuListView;
import com.shizhongkeji.swipemenulistview.SwipeMenuListView.OnMenuItemClickListener;

public class PlayListActivity extends Activity implements android.view.View.OnClickListener {
	private SwipeMenuListView mListMusic;
	private TextView mNumberMusic;
	private Button addMusic;
	private MusicListAdapter mMisicListAdapter;
	private List<Mp3Info> mp3Infos;
	private int listPosition = 0; // 在List中的位置
	private int index = 0;

	private int currentTime; // 当前播放位置

	private MusicCompleteReceiver mMusicCompleteReceiver;
	// private int duration; // 歌曲时长

	public static final String FCR_MUSIC = "com.shizhongkeji.action.CURRENTMUSIC";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		
		super.onCreate(savedInstanceState);
		mMusicCompleteReceiver = new MusicCompleteReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(FCR_MUSIC);
		registerReceiver(mMusicCompleteReceiver, intentFilter);
		setContentView(R.layout.activity_playlist);
		Intent intent = getIntent();
		if (intent != null) {
			index = intent.getIntExtra("index", 0);
		}
		initView();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mp3Infos.clear();
		mp3Infos = DBManager.getInstance(this).queryMusic();
		mNumberMusic.setText(mp3Infos.size() + "");
		mMisicListAdapter.setData();
		mMisicListAdapter.notifyDataSetChanged();
	}

	private void initView() {
		mp3Infos = new ArrayList<Mp3Info>();
		// findViewById(R.id.add_song).setOnClickListener(this);
		mListMusic = (SwipeMenuListView) findViewById(R.id.song_list);
		addMusic = (Button) findViewById(R.id.add_song);
		addMusic.setOnClickListener(this);
		mListMusic.setOnItemClickListener(new MusicListItemClickListener());
		mListMusic.setOnCreateContextMenuListener(new MusicListItemContextMenuListener());
		mp3Infos = DBManager.getInstance(this).queryMusic();
		mNumberMusic = (TextView) findViewById(R.id.number);
		if (mp3Infos != null) {
			mNumberMusic.setText(mp3Infos.size() + "");
		}
		mMisicListAdapter = new MusicListAdapter(this, index);
		mListMusic.setAdapter(mMisicListAdapter);
		if (index < mp3Infos.size()) {
			mListMusic.setSelection(index);
		}
		// step 1. create a MenuCreator
		SwipeMenuCreator creator = new SwipeMenuCreator() {

			@Override
			public void create(SwipeMenu menu) {
				// // create "open" item
				// SwipeMenuItem openItem = new SwipeMenuItem(
				// getApplicationContext());
				// // set item background
				// openItem.setBackground(new ColorDrawable(Color.rgb(0xC9,
				// 0xC9,
				// 0xCE)));
				// // set item width
				// openItem.setWidth(dp2px(90));
				// // set item title
				// openItem.setTitle("Open");
				// // set item title fontsize
				// openItem.setTitleSize(18);
				// // set item title font color
				// openItem.setTitleColor(Color.WHITE);
				// // add to menu
				// menu.addMenuItem(openItem);

				// create "delete" item
				SwipeMenuItem deleteItem = new SwipeMenuItem(getApplicationContext());
				// set item background
				deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9, 0x3F, 0x25)));
				// set item width
				deleteItem.setWidth(dp2px(90));
				// set a icon
				deleteItem.setIcon(R.drawable.ic_delete);
				// add to menu
				menu.addMenuItem(deleteItem);
			}
		};
		// set creator
		mListMusic.setMenuCreator(creator);

		// step 2. listener item click event
		mListMusic.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(final int position, SwipeMenu menu, int index) {
				final Mp3Info mp3info = mp3Infos.get(position);
				switch (index) {
				case 0:
					Dialog dialog = new AlertDialog.Builder(PlayListActivity.this).setTitle("删除歌曲")
							.setMessage("你确定要删除" + mp3info.getTitle())
							.setPositiveButton("确定", new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									String path = mp3info.getUrl();
									deleteMusic(path);
									Mp3Info mp3info = mp3Infos.get(position);
									DBManager.getInstance(PlayListActivity.this).deleteMusic(
											String.valueOf(mp3info.getId()));
									mp3Infos.remove(position);
									mMisicListAdapter.setData();
									mMisicListAdapter.notifyDataSetChanged();
									if(GlobalApplication.current < position){
										Log.d("PlayListActivity", "GlobalApplication.current"+ GlobalApplication.current +"position"+position);
									}else if(GlobalApplication.current == position){
										Log.d("PlayListActivity", "GlobalApplication.current"+ GlobalApplication.current +"position"+position);
										Intent intent = new Intent();
										intent.setAction("com.shizhong.media.MUSIC_SERVICE");
										intent.putExtra("listPosition", position);
										intent.putExtra("MSG", AppConstant.PlayerMsg.PLAYING_DELETE);
										startService(intent);	
									}else if(GlobalApplication.current > position){
										Log.d("PlayListActivity", "GlobalApplication.current"+ GlobalApplication.current +"position"+position);
									}
									
								}
							}).setNegativeButton("取消", new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {

								}
							}).create();
					dialog.show();
					break;
				}
				return false;
			}
		});

	}

	/**
	 * 
	 * 
	 * @param listPosition
	 */
	public void playMusic(int listPosition) {
		if (mp3Infos != null) {
			Mp3Info mp3Info = mp3Infos.get(listPosition);
			// Bitmap bitmap = MediaUtil.getArtwork(this, mp3Info.getId(),
			// mp3Info.getAlbumId(), true,
			// true);// ��ȡר��λͼ����ΪСͼ
			Intent intent = new Intent(); // ����Intent������ת��PlayerActivity
			// ���һϵ��Ҫ���ݵ�����
			intent.putExtra("title", mp3Info.getTitle());
			intent.putExtra("url", mp3Info.getUrl());
			intent.putExtra("artist", mp3Info.getArtist());
			intent.putExtra("musicDuration", mp3Info.getDuration() + "");
			intent.putExtra("listPosition", listPosition);
			intent.putExtra("currentTime", currentTime);
			// intent.putExtra("repeatState", repeatState);
			// intent.putExtra("shuffleState", isShuffle);
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
	 * <br>
	 * 类描述: <br>
	 * 功能详细描述:
	 * 
	 * @author ZhouHaibo
	 * @date [2015年5月5日]
	 */
	public class MusicListItemContextMenuListener implements OnCreateContextMenuListener {

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
			vibrator.vibrate(50); // ������
			// musicListItemDialog(); // �����󵯳��ĶԻ���
			final AdapterView.AdapterContextMenuInfo menuInfo2 = (AdapterView.AdapterContextMenuInfo) menuInfo;
			listPosition = menuInfo2.position; // ����б��λ��
		}

	}

	private class MusicCompleteReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				index = intent.getIntExtra("index", 0);
				mListMusic.setSelection(index);
				mMisicListAdapter.notifyDataSetChanged();

			}

		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mMusicCompleteReceiver);
	}

	private int dp2px(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources()
				.getDisplayMetrics());
	}

	private void deleteMusic(String path) {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.add_song:
			Intent intent = new Intent(PlayListActivity.this, SystemSongActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}
}
