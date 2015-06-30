package com.shizhongkeji.service;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.shizhongkeji.GlobalApplication;
import com.shizhongkeji.info.AppConstant;
import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.sqlutils.DBManager;

/***
 * 
 * 音乐播放服务
 */
@SuppressLint("NewApi")
public class PlayerService extends Service implements OnAudioFocusChangeListener {
	public MediaPlayer mediaPlayer; // 媒体播放器对象
	private String path; // 音乐文件路径
	private int msg; // 播放信息
	private boolean isPause; // 暂停状态
	public int current = 0; // 记录当前正在播放的音乐
	public List<Mp3Info> mp3Infos = new ArrayList<Mp3Info>(); // 存放Mp3Info对象的集合
	private int status = 3; // 播放状态，默认为顺序播放
	private MyReceiver myReceiver; // 自定义广播接收器
	public int currentTime; // 当前播放进度
	public int duration; // 播放长度

	// private SharedPreferences share;
	// private Editor edit;

	private boolean mPausedByTransientLossOfFocus = false;
	private boolean mPausedByCall = false;

	private AudioManager mAudioManager;
	// 服务要发送的一些Action
	public static final String UPDATE_ACTION = "com.shizhong.action.UPDATE_ACTION"; // 更新动作
	public static final String CTL_ACTION = "com.shizhong.action.CTL_ACTION"; // 控制动作
	// public static final String MUSIC_CURRENT =
	// "com.shizhong.action.MUSIC_CURRENT"; // 当前音乐播放时间更新动作
	// public static final String MUSIC_DURATION =
	// "com.shizhong.action.MUSIC_DURATION";// 新音乐长度更新动作
	// public static final String SHOW_LRC = "com.shizhong.action.SHOW_LRC"; //
	// 通知显示歌词
	public static final String GESTURE_PLAY = "com.shizhongkeji.action.GESTURE.PLAY_MUSIC"; // 手势播放
	public static final String GESTURE_NEXT = "com.shizhongkeji.action.GESTURE.PLAY_MUSIC_NEXT"; // 手势下一首
	public static final String GESTURE_PREVIOUS = "com.shizhongkeji.action.GESTURE.PLAY_MUSIC_PREVIOUS"; // 手势上一首
	public static final String FCR_MUSIC = "com.shizhongkeji.action.CURRENTMUSIC";
	// public static final String MUSIC_PLAY_OVER =
	// "com.shizhong.media.MUSIC_OVER";
	private IBinder iBinder = new MyBinder();
	private SharedPreferences shared = null;
	public class MyBinder extends Binder {
		public PlayerService getPlayerService() {
			return PlayerService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("service", "service created");
		shared =  getSharedPreferences("playInfo", Context.MODE_PRIVATE);
		current = shared.getInt("position", 0);
		TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE); // ��ȡϵͳ����
		telManager.listen(new MobliePhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mediaPlayer = new MediaPlayer();
		setData();

		/**
		 * 设置音乐播放完成时的监听器
		 */
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				if (status == 1) { // 单曲循环
					mediaPlayer.start();
				} else if (status == 2) { // 全部循环
					current++;
					if (current > mp3Infos.size() - 1) { // 变为第一首的位置继续播放
						current = 0;
					}
					path = mp3Infos.get(current).getUrl();
					play(0);
				} else if (status == 3) { // 顺序播放
					if (current == mp3Infos.size() - 1) {
//						current = 0;
						GlobalApplication.isAutoPause = true;
						stop();
					} else {
						current++; // 下一首位置
						if (current < mp3Infos.size() - 1) {
							path = mp3Infos.get(current).getUrl();
							play(0);
						}
					}
				} else if (status == 4) { // 随机播放
					current = getRandomIndex(mp3Infos.size() - 1);
					System.out.println("currentIndex ->" + current);
					path = mp3Infos.get(current).getUrl();
					play(0);
				}

			}
		});
		mediaPlayer.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.e("PlayerService", "OnError - Error code: " + what + " Extra code: " + extra);

				if (status == 1) { // 单曲循环
					mediaPlayer.start();
				} else if (status == 2) { // 全部循环
					current++;
					if (current > mp3Infos.size() - 1) { // 变为第一首的位置继续播放
						current = 0;
					}
					path = mp3Infos.get(current).getUrl();
					play(0);

				} else if (status == 3) { // 顺序播放
					current++; // 下一首位置
					if (current <= mp3Infos.size() - 1) {
						path = mp3Infos.get(current).getUrl();
						play(0);
					} else {
						stop();
					}
				} else if (status == 4) { // 随机播放
					current = getRandomIndex(mp3Infos.size() - 1);
					path = mp3Infos.get(current).getUrl();
					play(0);
				}
				return false;
			}
		});
		myReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CTL_ACTION);
		// filter.addAction(SHOW_LRC);
		registerReceiver(myReceiver, filter);
	}

	/**
	 * 获取随机位置
	 * 
	 * @param end
	 * @return
	 */
	protected int getRandomIndex(int end) {
		int index = (int) (Math.random() * end);
		return index;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return iBinder;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		String action = "";
		if (intent != null) {
			action = intent.getStringExtra("action");
			if (intent.getIntExtra("type", AppConstant.TYPE_OTHER) == AppConstant.TYPE_GESTURE) {
				if (action != null) {
					if (action.equals(GESTURE_PLAY)) {
						if (!GlobalApplication.isPlaying) {
							if (!GlobalApplication.isPlay && mp3Infos.size() > 0) {
								GlobalApplication.isPlay = true;
								path = mp3Infos.get(current).getUrl();
								play(0);
								// updateActionReceiver();
							} else {
								resume();
							}
						} else {
							pause();
						}
					}
					if (action.equals(GESTURE_NEXT)) {
						if (GlobalApplication.isPlaying) {
							if (status == 4) { // 随机播放
								current = getRandomIndex(mp3Infos.size() - 1);
								System.out.println("currentIndex ->" + current);
								play(0);

							} else if (status == 3) {
								current = current + 1;
								if (mp3Infos != null && current <= mp3Infos.size() - 1) {
									path = mp3Infos.get(current).getUrl();
									play(0);

								} else {
									current = 0;
								}
								path = mp3Infos.get(current).getUrl();
								play(0);

							}
						}

					}
					if (action.equals(GESTURE_PREVIOUS)) {
						if (GlobalApplication.isPlaying) {
							if (status == 4) { // 随机播放
								current = getRandomIndex(mp3Infos.size() - 1);
								System.out.println("currentIndex ->" + current);
								play(0);

							} else if (status == 3) {
								current--;
								if (current >= 0) {
									path = mp3Infos.get(current).getUrl(); //
									play(0);

								} else {
									current = mp3Infos.size() - 1;
									path = mp3Infos.get(current).getUrl(); //
									play(0);

								}
							}
						}

					}
				}

			} else {
				msg = intent.getIntExtra("MSG", 0); // 播放信息
				if (msg == AppConstant.PlayerMsg.PLAY_MSG) { // 直接播放音乐
					path = intent.getStringExtra("url"); // 歌曲路径
					current = intent.getIntExtra("listPosition", 0); // 当前播放歌曲的在mp3Infos的位置
					play(0);
				} else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) { // 暂停
					pause();
				} else if (msg == AppConstant.PlayerMsg.STOP_MSG) { // 停止
					stop();
				} else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) { // 继续播放
					resume();
				} else if (msg == AppConstant.PlayerMsg.PRIVIOUS_MSG) { // 上一首
					path = intent.getStringExtra("url"); // 歌曲路径
					current = intent.getIntExtra("listPosition", 0); // 当前播放歌曲的在mp3Infos的位置
					previous();
				} else if (msg == AppConstant.PlayerMsg.NEXT_MSG) { // 下一首
					path = intent.getStringExtra("url"); // 歌曲路径
					current = intent.getIntExtra("listPosition", 0); // 当前播放歌曲的在mp3Infos的位置
					next();
				} else if (msg == AppConstant.PlayerMsg.PLAYING_DELETE) {
					int delete_position = intent.getIntExtra("listPosition", 0); // 当前播放歌曲的在mp3Infos的位置
					setData();
					if (GlobalApplication.current < delete_position) {
					} else if (GlobalApplication.current > delete_position) {
						current--;
						GlobalApplication.current = current;
					}
				} else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) { // 进度更新
					currentTime = intent.getIntExtra("progress", -1);
					play(currentTime);
				} else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) {
					// handler.sendEmptyMessage(1);
				} else if (msg == AppConstant.PlayerMsg.ADD_MUSIC) {
					setData();
				}
			}

		}
		super.onStart(intent, startId);
	}

	/**
	 * 播放音乐
	 * 
	 * @param position
	 */
	private void play(int currentTime) {
		try {
			mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
					AudioManager.AUDIOFOCUS_GAIN);
			GlobalApplication.isPlay = true;
			GlobalApplication.isPlaying = true;
			GlobalApplication.current = current;
			mediaPlayer.reset();// 把各项参数恢复到初始状态
			mediaPlayer.setDataSource(path);
			mediaPlayer.prepare(); // 进行缓冲
			mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));// 注册一个监听器
			updateActionReceiver();
			// handler.sendEmptyMessage(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 暂停音乐
	 */
	private void pause() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			GlobalApplication.isPlaying = false;
			isPause = true;
			updateActionReceiver();
		}
	}

	private void resume() {
		mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		if (isPause) {
			mediaPlayer.start();
			GlobalApplication.isPlaying = true;
			isPause = false;
			updateActionReceiver();
		}
	}

	/**
	 * 上一首
	 */
	private void previous() {
		play(0);
	}

	/**
	 * 下一首
	 */
	private void next() {
		play(0);
	}

	/**
	 * 停止音乐
	 */
	private void stop() {
		if (mediaPlayer != null) {
			GlobalApplication.isPlaying = false;
			mediaPlayer.stop();
			try {
				mediaPlayer.prepare(); // 在调用stop后如果需要再次通过start进行播放,需要之前调用prepare函数
			} catch (Exception e) {
				e.printStackTrace();
			}
			updateActionReceiver();
		}
	}

	@Override
	public void onDestroy() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		Editor edit = shared.edit();
		edit.putInt("position", current);
		edit.commit();
		GlobalApplication.isPlaying = false;
		GlobalApplication.isPlay = false;
		mAudioManager.abandonAudioFocus(this);
		unregisterReceiver(myReceiver);
	}

	/**
	 * 
	 * 实现一个OnPrepareLister接口,当音乐准备好的时候开始播放
	 * 
	 */
	private final class PreparedListener implements OnPreparedListener {
		private int currentTime;

		public PreparedListener(int currentTime) {
			this.currentTime = currentTime;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			if(!GlobalApplication.isAutoPause){
				mediaPlayer.start(); // 开始播放
				if (currentTime > 0) { // 如果音乐不是从头播放
					mediaPlayer.seekTo(currentTime);
				}	
			}
		}
	}

	public class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int control = intent.getIntExtra("control", -1);
			switch (control) {
			case 1:
				status = 1; // 将播放状态置为1表示：单曲循环
				break;
			case 2:
				status = 2; // 将播放状态置为2表示：全部循环
				break;
			case 3:
				status = 3; // 将播放状态置为3表示：顺序播放
				break;
			case 4:
				status = 4; // 将播放状态置为4表示：随机播放
				break;
			}
		}
	}

	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			// resume playback
			if (!mediaPlayer.isPlaying() && mPausedByTransientLossOfFocus) {
				mPausedByTransientLossOfFocus = false;
				resume();
				updateActionReceiver();
			}
			break;

		case AudioManager.AUDIOFOCUS_LOSS:
			// Lost focus for an unbounded amount of time: stop playback and
			// release media player
			if (mediaPlayer.isPlaying()) {
				mPausedByTransientLossOfFocus = false;
				pause();
				updateActionReceiver();
			}
			break;

		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			// Lost focus for a short time, but it's ok to keep playing
			// at an attenuated level
			if (mediaPlayer.isPlaying()) {
				mPausedByTransientLossOfFocus = true;
				pause();
				updateActionReceiver();
			}

			break;
		}
	}

	private void setData() {
		mp3Infos.clear();
		DBManager.getInstance(this).queryMusic(mp3Infos);
	}

	private class MobliePhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE: // ̬
				if (!mediaPlayer.isPlaying() && mPausedByCall) {
					resume();
					mPausedByCall = false;
					updateActionReceiver();
				}
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK: // ̬
			case TelephonyManager.CALL_STATE_RINGING: // ̬
				if (mediaPlayer.isPlaying()) {
					pause();
					mPausedByCall = true;
					updateActionReceiver();
				}
				break;
			default:
				break;
			}
		}
	}

	private void updateActionReceiver() {
		Intent sendIntent = new Intent(UPDATE_ACTION);
		// 发送广播，将被Activity组件中的BroadcastReceiver接收到
		sendBroadcast(sendIntent);
	}
}
