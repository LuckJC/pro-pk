package com.shizhongkeji.musicplayer;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.shizhongkeji.GlobalApplication;
import com.shizhongkeji.info.AppConstant;
import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.service.PlayerService;
import com.shizhongkeji.service.PlayerService.MyBinder;
import com.shizhongkeji.sqlutils.DBManager;
import com.shizhongkeji.utils.MediaUtil;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {

	private int repeatState; // 重复状态
	private final int isCurrentRepeat = 1; // ����ѭ��
	private final int isAllRepeat = 2; // ȫ��ѭ��
	private final int isNoneRepeat = 3; // ���ظ�����
	private boolean isFirstTime = true;
	// private boolean isPlaying; //
	// private boolean isPause; // ��ͣ
	private boolean isNoneShuffle = true; // ˳�򲥷�
	private boolean isShuffle = false; // 随机
	private RelativeLayout mLinearLayoutVol;

	private String title; // 歌名
	private String artist; // 歌手名
	private String url; // 歌曲路径
	private int listPosition = 0; // 当前歌曲位置
	private int currentTime; // 当前播放时间
	private int duration; // 音乐时长

	private TextView mPlayCurrentTime;
	private TextView mPlayFinalTime;
	private TextView mMusicName;
	private TextView mMusicSiger;
	private SeekBar mPlayProgress;
	private SeekBar mPlayVol; // 声音控制
	// private CheckBox mPlay;
	private Button mLastSong;
	private Button mNextSong;
	private Button mAddVol;
	private Button mSubVol;
	private Button playBtn; // 播放按钮
	private Button repeatBtn;
	private Button shuffleBtn;
	private List<Mp3Info> mp3Infos = new ArrayList<Mp3Info>();

	private AudioManager am;

	private int currentVolume; // 当前音量
	private int maxVolume; // 最大音量

	private ImageView musicAlbum; // 专辑的封面

	private SharedPreferences share;
	private Editor edit;

	private Dialog mDialog;

	private PlayerReceiver playerReceiver;
	private PlayerService playerService;
	public static final String UPDATE_ACTION = "com.shizhong.action.UPDATE_ACTION"; // 更新动作
	public static final String CTL_ACTION = "com.shizhong.action.CTL_ACTION"; // 控制动作
	// public static final String MUSIC_CURRENT =
	// "com.shizhong.action.MUSIC_CURRENT"; // 当前音乐改变动作
	// public static final String MUSIC_DURATION =
	// "com.shizhong.action.MUSIC_DURATION";// 音乐时长改变动作
	public static final String MUSIC_PLAYING = "com.shizhong.action.MUSIC_PLAYING"; // 播放音乐动作
	public static final String REPEAT_ACTION = "com.shizhong.action.REPEAT_ACTION"; // 音乐重复改变动作
	public static final String SHUFFLE_ACTION = "com.shizhong.action.SHUFFLE_ACTION";// 音乐随机播放动作
	public static final String GESTRUE_PLAYING = "com.shizhongkeji.action.GESTURE.PLAY_MUSIC"; // 手势播放音乐动作
	public static final String MUSIC_SERVICE = "com.shizhong.media.MUSIC_SERVICE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mDialog = new Dialog(this);
		mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		share = getSharedPreferences("playInfo", Context.MODE_PRIVATE);
		edit = share.edit();
		initView();
		if (mp3Infos != null && mp3Infos.size() > 0 && mp3Infos.size() > listPosition
				&& listPosition >= 0) {
			Mp3Info mp3Info = mp3Infos.get(listPosition);
			showArtwork(mp3Info);
		}
		switch (repeatState) {
		case isCurrentRepeat: //
			shuffleBtn.setClickable(false);
			repeatBtn.setBackgroundResource(R.drawable.repeat_current_selector);
			break;
		case isAllRepeat: //
			shuffleBtn.setClickable(false);
			repeatBtn.setBackgroundResource(R.drawable.repeat_all_selector);
			break;
		case isNoneRepeat: //
			shuffleBtn.setClickable(true);
			repeatBtn.setBackgroundResource(R.drawable.repeat_none_selector);
			break;
		}
		if (isShuffle) {
			isNoneShuffle = false;
			shuffleBtn.setBackgroundResource(R.drawable.shuffle_selector);
			repeatBtn.setClickable(false);
		} else {
			isNoneShuffle = true;
			shuffleBtn.setBackgroundResource(R.drawable.shuffle_none_selector);
			repeatBtn.setClickable(true);
		}
		Intent intentService = new Intent(MUSIC_SERVICE);
		bindService(intentService, sc, Context.BIND_AUTO_CREATE);
		registerReceiver();
	}

	@Override
	protected void onResume() {
		setData();
		super.onResume();
	}

	private void initView() {
		findViewById(R.id.menu).setOnClickListener(this);
		findViewById(R.id.sound).setOnClickListener(this);
		mLinearLayoutVol = (RelativeLayout) findViewById(R.id.volume);
		mPlayCurrentTime = (TextView) findViewById(R.id.current_progress);
		mPlayFinalTime = (TextView) findViewById(R.id.final_progress);
		mMusicName = (TextView) findViewById(R.id.song);
		mMusicSiger = (TextView) findViewById(R.id.singer);
		mPlayProgress = (SeekBar) findViewById(R.id.audioTrack);
		mPlayVol = (SeekBar) findViewById(R.id.seekbar_vol);
		playBtn = (Button) findViewById(R.id.paly);
		musicAlbum = (ImageView) findViewById(R.id.music_image);
		mLastSong = (Button) findViewById(R.id.last);
		mNextSong = (Button) findViewById(R.id.next);
		mAddVol = (Button) findViewById(R.id.add_vol);
		mSubVol = (Button) findViewById(R.id.sub_vol);
		repeatBtn = (Button) findViewById(R.id.repeat_music);
		shuffleBtn = (Button) findViewById(R.id.shuffle_music);
		mPlayProgress.setOnSeekBarChangeListener(this);
		mPlayVol.setOnSeekBarChangeListener(this);
		mLastSong.setOnClickListener(this);
		mNextSong.setOnClickListener(this);
		mAddVol.setOnClickListener(this);
		mSubVol.setOnClickListener(this);
		playBtn.setOnClickListener(this);
		repeatBtn.setOnClickListener(this);
		shuffleBtn.setOnClickListener(this);
		boolean isRead = share.getBoolean("isPlaying", false);
		repeatState = isNoneRepeat;
		if (isRead) {
			url = share.getString("url", "");
			currentTime = share.getInt("duration", 0);
			duration = share.getInt("currentTime", 0);
			listPosition = share.getInt("position", 0);
			repeatState = share.getInt("repeatstate", 3);
			mPlayProgress.setMax(duration);
			mPlayProgress.setProgress(currentTime);
			mPlayCurrentTime.setText(MediaUtil.formatTime(duration));
			mPlayFinalTime.setText(MediaUtil.formatTime(currentTime));
			mMusicName.setText(share.getString("title", ""));
			mMusicSiger.setText(share.getString("singer", ""));
		} else {
			if (mp3Infos != null && mp3Infos.size() > 0) {
				Mp3Info mp3Info = mp3Infos.get(listPosition);
				url = mp3Info.getUrl();
			}
		}

		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mPlayVol.setMax(maxVolume);
		mPlayVol.setProgress(currentVolume);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
		System.out.println("currentVolume--->" + currentVolume);
		System.out.println("maxVolume-->" + maxVolume);
		setPlayButtonStatus();
		setData();
	}

	private void registerReceiver() {

		playerReceiver = new PlayerReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_ACTION);
		filter.addAction(REPEAT_ACTION);
		filter.addAction(SHUFFLE_ACTION);
		filter.addAction(GESTRUE_PLAYING);
		registerReceiver(playerReceiver, filter);
	}

	private void showExitDialog() {
		View view = LayoutInflater.from(this).inflate(R.layout.dialog_exit, null);
		view.findViewById(R.id.exit).setOnClickListener(this);
		view.findViewById(R.id.moveback).setOnClickListener(this);
		mDialog.setContentView(view);
		mDialog.show();
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		switch (v.getId()) {
		case R.id.menu:
			intent.setClass(this, PlayListActivity.class);
			intent.putExtra("index", listPosition);
			startActivityForResult(intent, 1);
			break;
		case R.id.sound:
			mHandler.removeMessages(0);
			mHandler.sendEmptyMessageDelayed(0, 3000);
			mLinearLayoutVol.setVisibility(View.VISIBLE);
			break;
		case R.id.sub_vol:
			mHandler.removeMessages(0);
			mHandler.sendEmptyMessageDelayed(0, 3000);
			if (currentVolume > 0) {
				currentVolume = currentVolume - 1;
				mPlayVol.setProgress(currentVolume);
				am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
			} else {
				am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
			}

			break;
		case R.id.add_vol:
			mHandler.removeMessages(0);
			mHandler.sendEmptyMessageDelayed(0, 3000);
			if (currentVolume < maxVolume) {
				currentVolume = currentVolume + 1;
				mPlayVol.setProgress(currentVolume);
				am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
			} else {
				am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
			}
			break;
		case R.id.last:
			volumeStatusLayout();
			previous_music();
			GlobalApplication.isPlaying = true;
			setPlayButtonStatus();
			break;
		case R.id.next:
			volumeStatusLayout();
			next_music();
			GlobalApplication.isPlaying = true;
			setPlayButtonStatus();
			break;
		case R.id.paly:
			volumeStatusLayout();
			if (GlobalApplication.isPlaying) {
				intent.setAction(MUSIC_SERVICE);
				intent.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
				startService(intent);
				GlobalApplication.isPlaying = false;
				setPlayButtonStatus();
			} else {
				if (!GlobalApplication.isPlay) {
					if (mp3Infos != null && mp3Infos.size() > 0) {
						if (listPosition < mp3Infos.size()) {
							Mp3Info mp3Info = mp3Infos.get(listPosition);
							showArtwork(mp3Info);
							intent.setAction(MUSIC_SERVICE);
							intent.putExtra("url", mp3Info.getUrl());
							intent.putExtra("listPosition", listPosition);
							intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
							startService(intent);
							GlobalApplication.isPlaying = true;
							setPlayButtonStatus();
						}
					} else {
						Toast.makeText(MainActivity.this, "音乐列表无歌曲文件", Toast.LENGTH_SHORT).show();
					}
				} else {
					intent.setAction(MUSIC_SERVICE);
					intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
					startService(intent);
					GlobalApplication.isPlaying = true;
					setPlayButtonStatus();
				}
			}

			break;
		case R.id.repeat_music: //
			if (repeatState == isNoneRepeat) {
				repeat_one();
				shuffleBtn.setClickable(false);
				repeatState = isCurrentRepeat;
			} else if (repeatState == isCurrentRepeat) {
				repeat_all();
				shuffleBtn.setClickable(false);
				repeatState = isAllRepeat;
			} else if (repeatState == isAllRepeat) {
				repeat_none();
				shuffleBtn.setClickable(true);
				repeatState = isNoneRepeat;
			}
			switch (repeatState) {
			case isCurrentRepeat: // ����ѭ��
				repeatBtn.setBackgroundResource(R.drawable.repeat_current_selector);
				Toast.makeText(MainActivity.this, R.string.repeat_current, Toast.LENGTH_SHORT)
						.show();
				break;
			case isAllRepeat: // ȫ��ѭ��
				repeatBtn.setBackgroundResource(R.drawable.repeat_all_selector);
				Toast.makeText(MainActivity.this, R.string.repeat_all, Toast.LENGTH_SHORT).show();
				break;
			case isNoneRepeat: // ���ظ�
				repeatBtn.setBackgroundResource(R.drawable.repeat_none_selector);
				Toast.makeText(MainActivity.this, R.string.repeat_none, Toast.LENGTH_SHORT).show();
				break;
			}

			break;
		case R.id.shuffle_music: // �������
			if (isNoneShuffle) {
				shuffleBtn.setBackgroundResource(R.drawable.shuffle_selector);
				Toast.makeText(MainActivity.this, R.string.shuffle, Toast.LENGTH_SHORT).show();
				isNoneShuffle = false;
				isShuffle = true;
				shuffleMusic();
				repeatBtn.setClickable(false);
			} else if (isShuffle) {
				shuffleBtn.setBackgroundResource(R.drawable.shuffle_none_selector);
				Toast.makeText(MainActivity.this, R.string.shuffle_none, Toast.LENGTH_SHORT).show();
				isShuffle = false;
				isNoneShuffle = true;
				repeatBtn.setClickable(true);
				repeat_all();
			}
			break;
		case R.id.exit:
			mDialog.dismiss();
			unregisterReceiver(playerReceiver);
			intent.setAction(MUSIC_SERVICE);
			unbindService(sc);
			stopService(intent);
			this.finish();
			break;
		case R.id.moveback:
			mDialog.dismiss();
			this.moveTaskToBack(true);
			break;
		default:
			break;
		}

	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			mLinearLayoutVol.setVisibility(View.GONE);
		};
	};

	/**
	 * 播放
	 */
	public void play() {
		// 默认不循环
		repeat_none();
		Intent intent = new Intent();
		intent.setAction(MUSIC_SERVICE);
		intent.putExtra("url", url);
		intent.putExtra("listPosition", listPosition);
		intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
		startService(intent);
		// bindService(intent, sc, Context.BIND_AUTO_CREATE);
	}

	/**
	 * <br>
	 * 功能简述: <br>
	 * 功能详细描述: <br>
	 * 注意:
	 */

	private void volumeStatusLayout() {
		if (mLinearLayoutVol.isShown()) {
			mLinearLayoutVol.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null) {
			setData();
			title = data.getStringExtra("title");
			url = data.getStringExtra("url");
			artist = data.getStringExtra("artist");
			listPosition = data.getIntExtra("listPosition", 0);
			Mp3Info mp3Info = mp3Infos.get(listPosition);
			showArtwork(mp3Info);
			Long musicDuration = Long.parseLong(data.getStringExtra("musicDuration"));
			int msg = data.getIntExtra("MSG", 0);
			mPlayFinalTime.setText(MediaUtil.formatTime(musicDuration));
			mMusicName.setText(title);
			mMusicSiger.setText(artist);
			Intent intentService = new Intent(this, PlayerService.class);
			intentService.putExtra("url", url);
			intentService.putExtra("MSG", msg);
			intentService.putExtra("listPosition", listPosition);
			startService(intentService);
			GlobalApplication.isPlaying = true;
			setPlayButtonStatus();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		switch (seekBar.getId()) {
		case R.id.audioTrack:
			if (fromUser) {
				audioTrackChange(progress); //
			}
			break;
		case R.id.seekbar_vol:
			am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
			System.out.println("am--->" + progress);
			break;
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	/**
	 * 显示专辑封面
	 */
	private void showArtwork(Mp3Info mp3Info) {
		Bitmap bm = MediaUtil.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), true, false);
		Animation albumanim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.album_replace);
		musicAlbum.startAnimation(albumanim);
		if (bm != null) {
			musicAlbum.setImageBitmap(bm);
			// musicAblumReflection.setImageBitmap(ImageUtil.createReflectionBitmapForSingle(bm));
		} else {
			bm = MediaUtil.getDefaultArtwork(this, false);
			musicAlbum.setImageBitmap(bm);
			// musicAblumReflection.setImageBitmap(ImageUtil.createReflectionBitmapForSingle(bm));
		}

	}

	/**
	 *
	 * 
	 * @param progress
	 */
	public void audioTrackChange(int progress) {
		Intent intent = new Intent();
		intent.setAction(MUSIC_SERVICE);
		intent.putExtra("url", url);
		intent.putExtra("listPosition", listPosition);
		intent.putExtra("MSG", AppConstant.PlayerMsg.PROGRESS_CHANGE);
		intent.putExtra("progress", progress);
		startService(intent);
		// bindService(intent, sc, Context.BIND_AUTO_CREATE);
	}

	/**
	 * 单曲循环
	 */
	public void repeat_one() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 1);
		sendBroadcast(intent);
	}

	/**
	 * 列表循环
	 */
	public void repeat_all() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 2);
		sendBroadcast(intent);
	}

	/**
	 * 不循环
	 */
	public void repeat_none() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 3);
		sendBroadcast(intent);
	}

	/**
	 * 随机播放
	 */
	public void shuffleMusic() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 4);
		sendBroadcast(intent);
	}

	/**
	 * 上一首
	 */
	public void previous_music() {
		// playBtn.setBackgroundResource(R.drawable.pause_selector);
		listPosition = listPosition - 1;
		if (listPosition >= 0) {
			if (mp3Infos != null && mp3Infos.size() > 0) {
				Mp3Info mp3Info = mp3Infos.get(listPosition); //
				showArtwork(mp3Info); //
				mMusicName.setText(mp3Info.getTitle());
				mMusicSiger.setText(mp3Info.getArtist());
				url = mp3Info.getUrl();
				Intent intent = new Intent();
				intent.setAction(MUSIC_SERVICE);
				intent.putExtra("url", mp3Info.getUrl());
				intent.putExtra("listPosition", listPosition);
				intent.putExtra("MSG", AppConstant.PlayerMsg.PRIVIOUS_MSG);
				startService(intent);

			}

		} else {
			listPosition = 0;
			Toast.makeText(MainActivity.this, "没有上一首了", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 下一首
	 */
	public void next_music() {
		// playBtn.setBackgroundResource(R.drawable.pause_selector);
		listPosition = listPosition + 1;
		if (mp3Infos != null && mp3Infos.size() > 2) {
			if (listPosition <= mp3Infos.size() - 1) {
				Mp3Info mp3Info = mp3Infos.get(listPosition);
				showArtwork(mp3Info); //
				url = mp3Info.getUrl();
				mMusicName.setText(mp3Info.getTitle());
				mMusicSiger.setText(mp3Info.getArtist());
				Intent intent = new Intent();
				intent.setAction(MUSIC_SERVICE);
				intent.putExtra("url", mp3Info.getUrl());
				intent.putExtra("listPosition", listPosition);
				intent.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
				startService(intent);
				// bindService(intent, sc, Context.BIND_AUTO_CREATE);
			}

		} else {
			listPosition = mp3Infos.size() - 1;
			Toast.makeText(MainActivity.this, "已经是最后一首歌了", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 
	 * 
	 * @author
	 * 
	 */
	public class PlayerReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(UPDATE_ACTION)) {
				setPlayButtonStatus();
			} else if (action.equals(REPEAT_ACTION)) {
				repeatState = intent.getIntExtra("repeatState", -1);
				switch (repeatState) {
				case isCurrentRepeat: // ����ѭ��
					repeatBtn.setBackgroundResource(R.drawable.repeat_current_selector);
					shuffleBtn.setClickable(false);
					break;
				case isAllRepeat: // ȫ��ѭ��
					repeatBtn.setBackgroundResource(R.drawable.repeat_all_selector);
					shuffleBtn.setClickable(false);
					break;
				case isNoneRepeat: // ���ظ�
					repeatBtn.setBackgroundResource(R.drawable.repeat_none_selector);
					shuffleBtn.setClickable(true);
					break;
				}
			} else if (action.equals(SHUFFLE_ACTION)) {
				isShuffle = intent.getBooleanExtra("shuffleState", false);
				if (isShuffle) {
					isNoneShuffle = false;
					shuffleBtn.setBackgroundResource(R.drawable.shuffle_selector);
					repeatBtn.setClickable(false);
				} else {
					isNoneShuffle = true;
					shuffleBtn.setBackgroundResource(R.drawable.shuffle_none_selector);
					repeatBtn.setClickable(true);
				}
			} else if (action.equals(GESTRUE_PLAYING)) {
				if (mp3Infos != null && mp3Infos.size() > 0) {
					if (share.getBoolean("isPlaying", false)) {
						url = share.getString("url", "");
						listPosition = share.getInt("position", 0);
					} else {
						listPosition = 0;
						Mp3Info mp3Info = mp3Infos.get(listPosition);
						url = mp3Info.getUrl();
					}
					Intent intentService = new Intent(context, PlayerService.class);
					intentService.putExtra("url", url);
					intentService.putExtra("MSG", AppConstant.PlayerMsg.PLAYING_MSG);
					intentService.putExtra("listPosition", listPosition);
					startService(intentService);
					// bindService(intentService, sc, Context.BIND_AUTO_CREATE);
				}
			}

		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		edit.putBoolean("isPlaying", isFirstTime);
		edit.putString("title", mMusicName.getText().toString());
		edit.putString("singer", mMusicSiger.getText().toString());
		edit.putInt("duration", duration);
		edit.putInt("currentTime", currentTime);
		if (listPosition < 0) {
			edit.putInt("position", 0);
		} else {
			edit.putInt("position", listPosition);
		}
		edit.putInt("repeatstate", repeatState);
		edit.putString("url", url);
		edit.commit();
		mhandler.removeMessages(0);
	}

	/**
	 * 拦截退出键
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			showExitDialog();
			return false;
		}
		return false;
	}

	private ServiceConnection sc = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MyBinder myBinder = (MyBinder) service;
			playerService = myBinder.getPlayerService();
			mhandler.sendEmptyMessage(0);
		}
	};
	private Handler mhandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				listPosition = playerService.current;
				Log.e("Main--listPosition", listPosition + "");
				if (mp3Infos.size() > 0) {
					Mp3Info m = mp3Infos.get(listPosition < 0 ? 0 : listPosition);
					// showArtwork(m);
					if (playerService.mediaPlayer != null && GlobalApplication.isPlaying) {
						currentTime = playerService.mediaPlayer.getCurrentPosition(); // 获取当前音乐播放的位置
						mPlayProgress.setMax((int) m.getDuration());
						mPlayProgress.setProgress(currentTime);
						mPlayCurrentTime.setText(MediaUtil.formatTime(currentTime));
						Log.e("Main--currentTime", MediaUtil.formatTime(currentTime));
						mPlayFinalTime.setText(MediaUtil.formatTime(m.getDuration()));
						Log.e("Main--duration", MediaUtil.formatTime(m.getDuration()));
						mMusicName.setText(m.getTitle());
						Log.e("Main--Title", m.getTitle());
						mMusicSiger.setText(m.getArtist());
					}
					mhandler.sendEmptyMessageDelayed(0, 1000);
				}
				break;

			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	private void setPlayButtonStatus() {
		if (GlobalApplication.isPlaying) {
			playBtn.setBackgroundResource(R.drawable.pause);
		} else {
			playBtn.setBackgroundResource(R.drawable.play);
		}
	}

	private void setData() {
		mp3Infos.clear();
		DBManager.getInstance(this).queryMusic(mp3Infos);
	}
}
