package com.example.service;

import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.info.AppConstant;
import com.example.info.Mp3Info;
import com.example.utils.MediaUtil;

/***
 * 2013/5/25
 * 
 * @author wwj ���ֲ��ŷ���
 */
@SuppressLint("NewApi")
public class PlayerService extends Service {
	private int mSongNum = 0; //
	private MediaPlayer mediaPlayer = null; // ý�岥��������
	private String path; // 播放的路径
	private int msg; // ������Ϣ
	private boolean isPause; // 暂停״
	private int current = 0; // ��¼��ǰ���ڲ��ŵ�����
	private List<Mp3Info> mp3Infos; // 所有歌曲的集合
	private int status = 3; // ����״̬��Ĭ��Ϊ˳�򲥷�
	private MyReceiver myReceiver; // �Զ���㲥������
	private int currentTime; // 当前播放进度
	private int duration; // 歌曲时长

	private String mSongName; // 歌名
	private String mSinger; // 歌手名

	// private LrcProcess mLrcProcess; //��ʴ���
	// private List<LrcContent> lrcList = new ArrayList<LrcContent>();
	// //��Ÿ���б����
	// private int index = 0; //��ʼ���ֵ

	// ����Ҫ���͵�һЩAction
	public static final String UPDATE_ACTION = "com.shizhong.action.UPDATE_ACTION"; // ���¶���
	public static final String CTL_ACTION = "com.shizhong.action.CTL_ACTION"; // ���ƶ���
	public static final String MUSIC_CURRENT = "com.shizhong.action.MUSIC_CURRENT"; // ��ǰ���ֲ���ʱ����¶���
	public static final String MUSIC_DURATION = "com.shizhong.action.MUSIC_DURATION";// �����ֳ��ȸ��¶���
	public static final String SHOW_LRC = "com.shizhong.action.SHOW_LRC"; // ֪ͨ��ʾ���
	public static final String LAST_SONG = "com.shizhongkeji.action.GESTURE.PLAY_MUSIC_PREVIOUS"; // ��һ�׸�
	public static final String NEXT_SONG = "com.shizhongkeji.action.GESTURE.PLAY_MUSIC_NEXT"; // ��һ�׸�
	public static final String PLAY_SONG = "com.shizhongkeji.action.GESTURE.PLAY_MUSIC"; // ���Ż���ͣ
	/**
	 * handle 更新播放进度条
	 */
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				if (mediaPlayer != null) {
					currentTime = mediaPlayer.getCurrentPosition(); // ��ȡ��ǰ���ֲ��ŵ�λ��
					Intent intent = new Intent();
					intent.setAction(MUSIC_CURRENT);
					// intent.setAction(LAST_SONG);
					// intent.setAction(NEXT_SONG);
					// intent.setAction(PLAY_SONG);
					intent.putExtra("currentTime", currentTime);
					intent.putExtra("duration", duration);
					intent.putExtra("song", mSongName);
					intent.putExtra("singer", mSinger);
					sendBroadcast(intent); // ��PlayerActivity���͹㲥
					handler.sendEmptyMessageDelayed(1, 1000);
				}
			}
		};
	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("service", "service created");
		mediaPlayer = new MediaPlayer();

		mp3Infos = MediaUtil.getMp3Infos(PlayerService.this);

		/**
		 * �������ֲ������ʱ�ļ�����
		 */
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				if (status == 1) { // ����ѭ��
					mediaPlayer.start();
				} else if (status == 2) { // ȫ��ѭ��
					current++;
					if (current > mp3Infos.size() - 1) { // ��Ϊ��һ�׵�λ�ü�������
						current = 0;
					}
					Intent sendIntent = new Intent(UPDATE_ACTION);
					sendIntent.putExtra("current", current);
					// ���͹㲥������Activity����е�BroadcastReceiver���յ�
					sendBroadcast(sendIntent);
					path = mp3Infos.get(current).getUrl();
					play(0);
				} else if (status == 3) { // ˳�򲥷�
					current++; // ��һ��λ��
					if (current <= mp3Infos.size() - 1) {
						Intent sendIntent = new Intent(UPDATE_ACTION);
						sendIntent.putExtra("current", current);
						// ���͹㲥������Activity����е�BroadcastReceiver���յ�
						sendBroadcast(sendIntent);
						path = mp3Infos.get(current).getUrl();
						play(0);
					} else {
						mediaPlayer.seekTo(0);
						current = 0;
						Intent sendIntent = new Intent(UPDATE_ACTION);
						sendIntent.putExtra("current", current);
						// ���͹㲥������Activity����е�BroadcastReceiver���յ�
						sendBroadcast(sendIntent);
					}
				} else if (status == 4) { // �������
					current = getRandomIndex(mp3Infos.size() - 1);
					System.out.println("currentIndex ->" + current);
					Intent sendIntent = new Intent(UPDATE_ACTION);
					sendIntent.putExtra("current", current);
					// ���͹㲥������Activity����е�BroadcastReceiver���յ�
					sendBroadcast(sendIntent);
					path = mp3Infos.get(current).getUrl();
					play(0);
				}
			}
		});

		myReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CTL_ACTION);
		filter.addAction(LAST_SONG);
		filter.addAction(NEXT_SONG);
		filter.addAction(PLAY_SONG);
		// filter.addAction(SHOW_LRC);
		registerReceiver(myReceiver, filter);
	}

	/**
	 * ��ȡ���λ��
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
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		String action = intent.getStringExtra("action");
		if (action != null) {
			if (action.equals(PLAY_SONG)) {
				if (isPause) {
					mediaPlayer.pause();
					isPause = false;
				} else {
					path = mp3Infos.get(mSongNum).getUrl();
					mediaPlayer.reset();
					try {
						mediaPlayer.setDataSource(path);
						mediaPlayer.prepare();
						mediaPlayer.start();
						isPause=true;
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
			if (action.equals(NEXT_SONG)) {
				if (mSongNum == mp3Infos.size() - 1) {
					mSongNum = 0;
				} else {
					mSongNum++;
				}

				path = mp3Infos.get(mSongNum).getUrl();
				mediaPlayer.reset();
				try {
					mediaPlayer.setDataSource(path);
					mediaPlayer.prepare();
					mediaPlayer.start();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (action.equals(LAST_SONG)) {
				if (mSongNum >= 1) {
					mSongNum--;
				} else {
					mSongNum = mp3Infos.size() - 1;
				}
				path = mp3Infos.get(mSongNum).getUrl();
				mediaPlayer.reset();
				try {
					mediaPlayer.setDataSource(path);
					mediaPlayer.prepare();
					mediaPlayer.start();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		path = intent.getStringExtra("url"); // ����·��
		current = intent.getIntExtra("listPosition", -1); // ��ǰ���Ÿ�������mp3Infos��λ��
		msg = intent.getIntExtra("MSG", 0); // ������Ϣ
		if (msg == AppConstant.PlayerMsg.PLAY_MSG) { // ֱ�Ӳ�������
			play(0);
		} else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) { // ��ͣ
			pause();
		} else if (msg == AppConstant.PlayerMsg.STOP_MSG) { // ֹͣ
			stop();
		} else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) { // ��������
			resume();
		} else if (msg == AppConstant.PlayerMsg.PRIVIOUS_MSG) { // ��һ��
			previous();
		} else if (msg == AppConstant.PlayerMsg.NEXT_MSG) { // ��һ��
			next();
		} else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) { // ���ȸ���
			currentTime = intent.getIntExtra("progress", -1);
			play(currentTime);
		} else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) {
			handler.sendEmptyMessage(1);
		}
		super.onStart(intent, startId);
	}

	// /**
	// * ��ʼ���������
	// */
	// public void initLrc(){
	// mLrcProcess = new LrcProcess();
	// //��ȡ����ļ�
	// mLrcProcess.readLRC(mp3Infos.get(current).getUrl());
	// //���ش����ĸ���ļ�
	// lrcList = mLrcProcess.getLrcList();
	// PlayerActivity.lrcView.setmLrcList(lrcList);
	// //�л���������ʾ���
	// PlayerActivity.lrcView.setAnimation(AnimationUtils.loadAnimation(PlayerService.this,R.anim.alpha_z));
	// handler.post(mRunnable);
	// }
	// Runnable mRunnable = new Runnable() {
	//
	// @Override
	// public void run() {
	// // PlayerActivity.lrcView.setIndex(lrcIndex());
	// // PlayerActivity.lrcView.invalidate();
	// handler.postDelayed(mRunnable, 100);
	// }
	// };

	// /**
	// * ����ʱ���ȡ�����ʾ������ֵ
	// * @return
	// */
	// public int lrcIndex() {
	// if(mediaPlayer.isPlaying()) {
	// currentTime = mediaPlayer.getCurrentPosition();
	// duration = mediaPlayer.getDuration();
	// }
	// if(currentTime < duration) {
	// for (int i = 0; i < lrcList.size(); i++) {
	// if (i < lrcList.size() - 1) {
	// if (currentTime < lrcList.get(i).getLrcTime() && i == 0) {
	// index = i;
	// }
	// if (currentTime > lrcList.get(i).getLrcTime()
	// && currentTime < lrcList.get(i + 1).getLrcTime()) {
	// index = i;
	// }
	// }
	// if (i == lrcList.size() - 1
	// && currentTime > lrcList.get(i).getLrcTime()) {
	// index = i;
	// }
	// }
	// }
	// return index;
	// }
	/**
	 * ��������
	 * 
	 * @param position
	 */
	private void play(int currentTime) {
		try {
			// initLrc();
			mediaPlayer.reset();// �Ѹ�������ָ�����ʼ״̬
			mediaPlayer.setDataSource(path);
			mediaPlayer.prepare(); // ���л���
			mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));// ע��һ��������
			handler.sendEmptyMessage(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��ͣ����
	 */
	private void pause() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			isPause = false;
		}
	}

	private void resume() {
		if (isPause) {
			mediaPlayer.start();
			isPause = true;
		}
	}

	/**
	 * ��һ��
	 */
	private void previous() {
		Intent sendIntent = new Intent(UPDATE_ACTION);
		sendIntent.putExtra("current", current);
		// ���͹㲥������Activity����е�BroadcastReceiver���յ�
		sendBroadcast(sendIntent);
		play(0);
	}

	/**
	 * ��һ��
	 */
	private void next() {
		Intent sendIntent = new Intent(UPDATE_ACTION);
		sendIntent.putExtra("current", current);
		// ���͹㲥������Activity����е�BroadcastReceiver���յ�
		sendBroadcast(sendIntent);
		play(0);
	}

	/**
	 * ֹͣ����
	 */
	private void stop() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			try {
				mediaPlayer.prepare(); // �ڵ���stop�������Ҫ�ٴ�ͨ��start���в���,��Ҫ֮ǰ����prepare����
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onDestroy() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		// handler.removeCallbacks(mRunnable);
		unregisterReceiver(myReceiver);
	}

	/**
	 * 
	 * ʵ��һ��OnPrepareLister�ӿ�,������׼���õ�ʱ��ʼ����
	 * 
	 */
	private final class PreparedListener implements OnPreparedListener {
		private int currentTime;

		public PreparedListener(int currentTime) {
			this.currentTime = currentTime;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mediaPlayer.start(); // ��ʼ����
			if (currentTime > 0) { // ������ֲ��Ǵ�ͷ����
				mediaPlayer.seekTo(currentTime);
			}
			Intent intent = new Intent();
			intent.setAction(MUSIC_DURATION);
			duration = mediaPlayer.getDuration();
			intent.putExtra("duration", duration); // ͨ��Intent�����ݸ������ܳ���
			sendBroadcast(intent);
		}
	}

	public class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int control = intent.getIntExtra("control", -1);
			switch (control) {
			case 1:
				status = 1; // ������״̬��Ϊ1��ʾ������ѭ��
				break;
			case 2:
				status = 2; // ������״̬��Ϊ2��ʾ��ȫ��ѭ��
				break;
			case 3:
				status = 3; // ������״̬��Ϊ3��ʾ��˳�򲥷�
				break;
			case 4:
				status = 4; // ������״̬��Ϊ4��ʾ���������
				break;
			}

			// String action = intent.getAction();
			// if (action.equals(LAST_SONG)) {
			// previous();
			// } else if (action.equals(NEXT_SONG)) {
			// next();
			// } else if (action.equals(PLAY_SONG)) {
			// if (isPause) {
			// isPause = false;
			// play(currentTime);
			// } else {
			// isPause = true;
			// pause();
			// }
			// }
			// if(action.equals(SHOW_LRC)){
			// current = intent.getIntExtra("listPosition", -1);
			// initLrc();
			// }
		}
	}

}
