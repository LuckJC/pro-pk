package com.example.musicplayer;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
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

import com.example.info.AppConstant;
import com.example.info.Mp3Info;
import com.example.service.PlayerService;
import com.example.utils.MediaUtil;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {

	private int repeatState; // 循环标识
	private final int isCurrentRepeat = 1; // 单曲循环
	private final int isAllRepeat = 2; // 全部循环
	private final int isNoneRepeat = 3; // 无重复播放
	private boolean isFirstTime = true;
	private boolean isPlaying; // 正在播放
	private boolean isPause; // 暂停
	private boolean isNoneShuffle = true; // 顺序播放
	private boolean isShuffle = false; // 随机播放
	private RelativeLayout mLinearLayoutVol;

	private String title; // 歌曲标题
	private String artist; // 歌曲艺术家
	private String url; // 歌曲路径
	private int listPosition; // 播放歌曲在mp3Infos的位置
	private int currentTime; // 当前歌曲播放时间
	private int duration; // 歌曲长度
	private int flag; // 播放标识

	private TextView mPlayCurrentTime;
	private TextView mPlayFinalTime;
	private TextView mMusicName;
	private TextView mMusicSiger;
	private SeekBar mPlayProgress;
	private SeekBar mPlayVol; // 控制音量大小
	// private CheckBox mPlay;
	private Button mLastSong;
	private Button mNextSong;
	private Button mAddVol;
	private Button mSubVol;
	private Button playBtn; // 播放（播放、暂停）
	private Button repeatBtn;
	private Button shuffleBtn;
	private List<Mp3Info> mp3Infos;

	private AudioManager am; // 音频管理引用，提供对音频的控制
	// RelativeLayout ll_player_voice; // 音量控制面板布局
	int currentVolume; // 当前音量
	int maxVolume; // 最大音量
	// ImageButton ibtn_player_voice; // 显示音量控制面板的按钮
	// 音量面板显示和隐藏动画
	private Animation showVoicePanelAnimation;
	private Animation hiddenVoicePanelAnimation;

	private ImageView musicAlbum; // 音乐专辑封面
	// private ImageView musicAblumReflection; // 倒影反射

	private SharedPreferences share;
	private Editor edit;

	private Dialog mDialog;
	
	private PlayerReceiver playerReceiver;
	public static final String UPDATE_ACTION = "com.shizhong.action.UPDATE_ACTION"; // 更新动作
	public static final String CTL_ACTION = "com.shizhong.action.CTL_ACTION"; // 控制动作
	public static final String MUSIC_CURRENT = "com.shizhong.action.MUSIC_CURRENT"; // 音乐当前时间改变动作
	public static final String MUSIC_DURATION = "com.shizhong.action.MUSIC_DURATION";// 音乐播放长度改变动作
	public static final String MUSIC_PLAYING = "com.shizhong.action.MUSIC_PLAYING"; // 音乐正在播放动作
	public static final String REPEAT_ACTION = "com.shizhong.action.REPEAT_ACTION"; // 音乐重复播放动作
	public static final String SHUFFLE_ACTION = "com.shizhong.action.SHUFFLE_ACTION";// 音乐随机播放动作
	public static final String GESTRUE_PLAYING = "com.shizhongkeji.action.GESTURE.PLAY_MUSIC"; // 手势控制自动播放

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mDialog = new Dialog(this);
		mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		share = getSharedPreferences("playInfo", Context.MODE_PRIVATE);
		edit = share.edit();
		initView();
		mp3Infos = MediaUtil.getMp3Infos(MainActivity.this); // 获取所有音乐的集合对象
		Mp3Info mp3Info = mp3Infos.get(listPosition);
		showArtwork(mp3Info);
		switch (repeatState) {
		case isCurrentRepeat: // 单曲循环
			shuffleBtn.setClickable(false);
			repeatBtn.setBackgroundResource(R.drawable.repeat_current_selector);
			break;
		case isAllRepeat: // 全部循环
			shuffleBtn.setClickable(false);
			repeatBtn.setBackgroundResource(R.drawable.repeat_all_selector);
			break;
		case isNoneRepeat: // 无重复
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
		if (flag == AppConstant.PlayerMsg.PLAYING_MSG) { // 如果播放信息是正在播放
			Toast.makeText(MainActivity.this, "正在播放--" + title, 1).show();
			Intent intent = new Intent();
			// intent.setAction(SHOW_LRC);
			intent.putExtra("listPosition", listPosition);
			sendBroadcast(intent);
		} else if (flag == AppConstant.PlayerMsg.PLAY_MSG) { // 如果是点击列表播放歌曲的话
			playBtn.setBackgroundResource(R.drawable.play_selector);
			play();
		} else if (flag == AppConstant.PlayerMsg.CONTINUE_MSG) {
			Intent intent = new Intent(MainActivity.this, PlayerService.class);
			playBtn.setBackgroundResource(R.drawable.play_selector);
			intent.setAction("com.shizhong.media.MUSIC_SERVICE");
			intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG); // 继续播放音乐
			startService(intent);
		}
		registerReceiver();
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
		repeatState = isNoneRepeat; // 初始状态为无重复播放状态
		boolean isRead = share.getBoolean("isPlaying", false);
		if (isRead) {
			currentTime = share.getInt("duration", 0);
			duration = share.getInt("currentTime", 0);
			listPosition = share.getInt("position", 0);
			repeatState = share.getInt("repeatstate", 3);
			mPlayProgress.setMax(duration);
			mPlayProgress.setProgress(currentTime);
			mPlayFinalTime.setText(MediaUtil.formatTime(currentTime));
			mPlayCurrentTime.setText(MediaUtil.formatTime(duration));
			mMusicName.setText(share.getString("title", ""));
			mMusicSiger.setText(share.getString("singer", ""));
		}
		// 添加来电监听事件
		TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE); // 获取系统服务
		telManager.listen(new MobliePhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

		// 音量调节面板显示和隐藏的动画
		showVoicePanelAnimation = AnimationUtils.loadAnimation(this, R.anim.push_up_in);
		hiddenVoicePanelAnimation = AnimationUtils.loadAnimation(this, R.anim.push_up_out);

		// 获得系统音频管理服务对象
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mPlayVol.setMax(maxVolume);
		mPlayVol.setProgress(currentVolume);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
		System.out.println("currentVolume--->" + currentVolume);
		System.out.println("maxVolume-->" + maxVolume);

	}

	private void registerReceiver() {
		// 定义和注册广播接收器
		playerReceiver = new PlayerReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_ACTION);
		filter.addAction(MUSIC_CURRENT);
		filter.addAction(MUSIC_DURATION);
		filter.addAction(REPEAT_ACTION);
		filter.addAction(SHUFFLE_ACTION);
		filter.addAction(GESTRUE_PLAYING);
		registerReceiver(playerReceiver, filter);
	}

	private class MobliePhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE: // 挂机状态
				Intent intent = new Intent(MainActivity.this, PlayerService.class);
				playBtn.setBackgroundResource(R.drawable.play_selector);
				intent.setAction("com.shizhong.media.MUSIC_SERVICE");
				intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG); // 继续播放音乐
				startService(intent);
				isPlaying = false;
				isPause = true;

				break;
			case TelephonyManager.CALL_STATE_OFFHOOK: // 通话状态
			case TelephonyManager.CALL_STATE_RINGING: // 响铃状态
				Intent intent2 = new Intent(MainActivity.this, PlayerService.class);
				playBtn.setBackgroundResource(R.drawable.pause_selector);
				intent2.setAction("com.shizhong.media.MUSIC_SERVICE");
				intent2.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
				startService(intent2);
				isPlaying = true;
				isPause = false;

				break;
			default:
				break;
			}
		}
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
			startActivityForResult(intent, 1);
			break;
		case R.id.sound:
			mHandler.removeMessages(0);
			mHandler.sendEmptyMessageDelayed(0, 4000);
			mLinearLayoutVol.setVisibility(View.VISIBLE);
			break;
		case R.id.sub_vol:
			mHandler.removeMessages(0);
			mHandler.sendEmptyMessageDelayed(0, 4000);
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
			mHandler.sendEmptyMessageDelayed(0, 4000);
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
			break;
		case R.id.next:
			volumeStatusLayout();
			next_music();
			break;
		case R.id.paly:
			volumeStatusLayout();
			if (isPlaying) {
				playBtn.setBackgroundResource(R.drawable.pause_selector);
				intent.setAction("com.shizhong.media.MUSIC_SERVICE");
				intent.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
				startService(intent);
				isPlaying = false;
				isPause = true;
			} else if (isPause) {
				playBtn.setBackgroundResource(R.drawable.play_selector);
				intent.setAction("com.shizhong.media.MUSIC_SERVICE");
				intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
				startService(intent);
				isPause = false;
				isPlaying = true;
			}
			break;
		case R.id.repeat_music: // 重复播放
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
			case isCurrentRepeat: // 单曲循环
				repeatBtn.setBackgroundResource(R.drawable.repeat_current_selector);
				Toast.makeText(MainActivity.this, R.string.repeat_current, Toast.LENGTH_SHORT)
						.show();
				break;
			case isAllRepeat: // 全部循环
				repeatBtn.setBackgroundResource(R.drawable.repeat_all_selector);
				Toast.makeText(MainActivity.this, R.string.repeat_all, Toast.LENGTH_SHORT).show();
				break;
			case isNoneRepeat: // 无重复
				repeatBtn.setBackgroundResource(R.drawable.repeat_none_selector);
				Toast.makeText(MainActivity.this, R.string.repeat_none, Toast.LENGTH_SHORT).show();
				break;
			}

			break;
		case R.id.shuffle_music: // 随机播放
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
			}
			break;
		case R.id.exit:
			mDialog.dismiss();
			unregisterReceiver(playerReceiver);
			intent.setAction("com.shizhong.media.MUSIC_SERVICE");
			stopService(intent);
			finish();
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
	 * 播放音乐
	 */
	public void play() {
		// 开始播放的时候为顺序播放
		repeat_none();
		Intent intent = new Intent();
		intent.setAction("com.shizhong.media.MUSIC_SERVICE");
		intent.putExtra("url", url);
		intent.putExtra("listPosition", listPosition);
		intent.putExtra("MSG", flag);
		startService(intent);
	}

	/**
	 * <br>
	 * 功能简述:声音布局显示或隐藏 <br>
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
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		switch (seekBar.getId()) {
		case R.id.audioTrack:
			if (fromUser) {
				audioTrackChange(progress); // 用户控制进度的改变
			}
			break;
		case R.id.seekbar_vol:
			// 设置音量
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
		// 切换播放时候专辑图片出现透明效果
		Animation albumanim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.album_replace);
		// 开始播放动画效果
		musicAlbum.startAnimation(albumanim);
		if (bm != null) {
			musicAlbum.setImageBitmap(bm); // 显示专辑封面图片
			// musicAblumReflection.setImageBitmap(ImageUtil.createReflectionBitmapForSingle(bm));
			// // 显示倒影
		} else {
			bm = MediaUtil.getDefaultArtwork(this, false);
			musicAlbum.setImageBitmap(bm); // 显示专辑封面图片
			// musicAblumReflection.setImageBitmap(ImageUtil.createReflectionBitmapForSingle(bm));
			// // 显示倒影
		}

	}

	/**
	 * 播放进度改变
	 * 
	 * @param progress
	 */
	public void audioTrackChange(int progress) {
		Intent intent = new Intent();
		intent.setAction("com.shizhong.media.MUSIC_SERVICE");
		intent.putExtra("url", url);
		intent.putExtra("listPosition", listPosition);
		intent.putExtra("MSG", AppConstant.PlayerMsg.PROGRESS_CHANGE);
		intent.putExtra("progress", progress);
		startService(intent);
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
	 * 全部循环
	 */
	public void repeat_all() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 2);
		sendBroadcast(intent);
	}

	/**
	 * 顺序播放
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
		playBtn.setBackgroundResource(R.drawable.play_selector);
		listPosition = listPosition - 1;
		if (listPosition >= 0) {
			Mp3Info mp3Info = mp3Infos.get(listPosition); // 上一首MP3
			showArtwork(mp3Info); // 显示专辑封面
			mMusicName.setText(mp3Info.getTitle());
			mMusicSiger.setText(mp3Info.getArtist());
			url = mp3Info.getUrl();
			Intent intent = new Intent();
			intent.setAction("com.shizhong.media.MUSIC_SERVICE");
			intent.putExtra("url", mp3Info.getUrl());
			intent.putExtra("listPosition", listPosition);
			intent.putExtra("MSG", AppConstant.PlayerMsg.PRIVIOUS_MSG);
			startService(intent);

		} else {
			listPosition = 0;
			Toast.makeText(MainActivity.this, "没有上一首了", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 下一首
	 */
	public void next_music() {
		playBtn.setBackgroundResource(R.drawable.play_selector);
		listPosition = listPosition + 1;
		if (listPosition <= mp3Infos.size() - 1) {
			Mp3Info mp3Info = mp3Infos.get(listPosition);
			showArtwork(mp3Info); // 显示专辑封面
			url = mp3Info.getUrl();
			mMusicName.setText(mp3Info.getTitle());
			mMusicSiger.setText(mp3Info.getArtist());
			Intent intent = new Intent();
			intent.setAction("com.shizhong.media.MUSIC_SERVICE");
			intent.putExtra("url", mp3Info.getUrl());
			intent.putExtra("listPosition", listPosition);
			intent.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
			startService(intent);

		} else {
			listPosition = mp3Infos.size() - 1;
			Toast.makeText(MainActivity.this, "没有下一首了", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 用来接收从service传回来的广播的内部类
	 * 
	 * @author 
	 * 
	 */
	public class PlayerReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MUSIC_CURRENT)) {
				currentTime = intent.getIntExtra("currentTime", -1);
				mPlayCurrentTime.setText(MediaUtil.formatTime(currentTime));
				mPlayProgress.setProgress(currentTime);
			} else if (action.equals(MUSIC_DURATION)) {
				duration = intent.getIntExtra("duration", -1);
				mPlayProgress.setMax(duration);
				mPlayFinalTime.setText(MediaUtil.formatTime(duration));
			} else if (action.equals(UPDATE_ACTION)) {
				// 获取Intent中的current消息，current代表当前正在播放的歌曲
				listPosition = intent.getIntExtra("current", -1);
				url = mp3Infos.get(listPosition).getUrl();
				if (listPosition >= 0) {
					mMusicName.setText(mp3Infos.get(listPosition).getTitle());
					mMusicSiger.setText(mp3Infos.get(listPosition).getArtist());
				}
				if (listPosition == 0) {
					mPlayFinalTime.setText(MediaUtil.formatTime(mp3Infos.get(listPosition)
							.getDuration()));
					playBtn.setBackgroundResource(R.drawable.pause_selector);
					isPause = true;
				}
			} else if (action.equals(REPEAT_ACTION)) {
				repeatState = intent.getIntExtra("repeatState", -1);
				switch (repeatState) {
				case isCurrentRepeat: // 单曲循环
					repeatBtn.setBackgroundResource(R.drawable.repeat_current_selector);
					shuffleBtn.setClickable(false);
					break;
				case isAllRepeat: // 全部循环
					repeatBtn.setBackgroundResource(R.drawable.repeat_all_selector);
					shuffleBtn.setClickable(false);
					break;
				case isNoneRepeat: // 无重复
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
			}else if(action.equals(GESTRUE_PLAYING)){
				if(share.getBoolean("isPlaying", false)){
					url = share.getString("url", "");
					listPosition = share.getInt("position", 0);
				}else{
					listPosition = 0;
					Mp3Info mp3Info = mp3Infos.get(listPosition);
					url = mp3Info.getUrl();
				}
				Intent intentService = new Intent(context, PlayerService.class);
				intentService.putExtra("url", url);
				intentService.putExtra("MSG", AppConstant.PlayerMsg.PLAYING_MSG);
				intentService.putExtra("listPosition", listPosition);
				startService(intentService);
			}

		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		edit.putBoolean("isPlaying", isPlaying);
		edit.putString("title", mMusicName.getText().toString());
		edit.putString("singer", mMusicSiger.getText().toString());
		edit.putInt("duration", duration);
		edit.putInt("currentTime", currentTime);
		edit.putInt("position", listPosition);
		edit.putInt("repeatstate", repeatState);
		edit.putString("url", url);
		edit.commit();

	}

	/**
	 * 按返回键弹出对话框确定退出
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			showExitDialog();
			return false;
		}
		return false;
	}
}
