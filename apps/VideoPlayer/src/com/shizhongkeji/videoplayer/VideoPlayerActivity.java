package com.shizhongkeji.videoplayer;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.shizhongkeji.videoplayer.SoundView.OnVolumeChangedListener;
import com.shizhongkeji.videoplayer.VideoView.MySizeChangeLinstener;

public class VideoPlayerActivity extends Activity {

	private final static String TAG = "VideoPlayerActivity";
	private boolean isChangedVideo = false;

	private int playedTime;

	private VideoView vv = null;
	private SeekBar seekBar = null;
	private TextView durationTextView = null;
	private TextView playedTextView = null;
	private GestureDetector mGestureDetector = null;
	private AudioManager mAudioManager = null;

	private int maxVolume = 0;
	private int currentVolume = 0;
	private ImageButton play_Btn = null;
	private ImageButton sound_Btn = null;
	private TextView title;
	private ImageButton delete;
	private View controlView = null;
	private PopupWindow controler = null;

	private SoundView mSoundView = null;
	private PopupWindow mSoundWindow = null;

	private View titleView = null;
	private PopupWindow titleWindow = null;

	private static int screenWidth = 0;
	private static int screenHeight = 0;
	private static int controlHeight = 0;

	private final static int TIME = 4000;

	private boolean isControllerShow = true;
	private boolean isPaused = false;
	private boolean isFullScreen = false;
	private boolean isSilent = false;
	private boolean isSoundShow = false;

	private String path = "";
	private boolean mPausedByTransientLossOfFocus = false;

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.e("OnCreate", getIntent().toString());

		Looper.myQueue().addIdleHandler(new IdleHandler() {

			@Override
			public boolean queueIdle() {

				if (controler != null && vv.isShown()) {
					controler.showAtLocation(vv, Gravity.BOTTOM, 0, 0);
					controler.update(0, 0, screenWidth, controlHeight);
				}
				if (titleWindow != null && vv.isShown()) {
					titleWindow.showAtLocation(vv, Gravity.TOP, 0, 0);
					titleWindow.update(0, 25, screenWidth, 60);
				}
				return false;
			}
		});

		controlView = getLayoutInflater().inflate(R.layout.controler, null);
		controler = new PopupWindow(controlView);
		durationTextView = (TextView) controlView.findViewById(R.id.duration);
		playedTextView = (TextView) controlView.findViewById(R.id.has_played);

		mSoundView = new SoundView(this);
		mSoundView.setOnVolumeChangeListener(new OnVolumeChangedListener() {

			@Override
			public void setYourVolume(int index) {

				cancelDelayHide();
				updateVolume(index);
				hideControllerDelay();
			}
		});

		mSoundWindow = new PopupWindow(mSoundView);

		titleView = getLayoutInflater().inflate(R.layout.extral, null);
		titleWindow = new PopupWindow(this);

		title = (TextView) titleView.findViewById(R.id.title);
		delete = (ImageButton) titleView.findViewById(R.id.delete);
		play_Btn = (ImageButton) controlView.findViewById(R.id.play);
		sound_Btn = (ImageButton) controlView.findViewById(R.id.sound);

		vv = (VideoView) findViewById(R.id.vv);

		vv.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {

				vv.stopPlayback();

				new AlertDialog.Builder(VideoPlayerActivity.this).setTitle("对不起")
						.setMessage("你所播放的视频格式错误，请重新选择")
						.setPositiveButton("知道了", new AlertDialog.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {

								vv.stopPlayback();

							}

						}).setCancelable(false).show();

				return false;
			}

		});
		vv.setMySizeChangeLinstener(new MySizeChangeLinstener() {

			@Override
			public void doMyThings() {
				setVideoScale(SCREEN_DEFAULT);
			}

		});
		play_Btn.setAlpha(0xBB);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		sound_Btn.setAlpha(findAlphaFromSound());

		play_Btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cancelDelayHide();
				if (isPaused) {
					vv.start();
					play_Btn.setImageResource(R.drawable.pause);
					hideControllerDelay();
				} else {
					vv.pause();
					play_Btn.setImageResource(R.drawable.play);
				}
				isPaused = !isPaused;

			}

		});
		sound_Btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				cancelDelayHide();
				if (isSoundShow) {
					mSoundWindow.dismiss();
				} else {
					if (mSoundWindow.isShowing()) {
						mSoundWindow.update(15, 0, SoundView.MY_WIDTH, SoundView.MY_HEIGHT);
					} else {
						mSoundWindow.showAtLocation(vv, Gravity.RIGHT | Gravity.CENTER_VERTICAL,
								15, 0);
						mSoundWindow.update(15, 0, SoundView.MY_WIDTH, SoundView.MY_HEIGHT);
					}
				}
				isSoundShow = !isSoundShow;
				hideControllerDelay();
			}
		});

		sound_Btn.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				if (isSilent) {
					sound_Btn.setImageResource(R.drawable.soundenable);
				} else {
					sound_Btn.setImageResource(R.drawable.sounddisable);
				}
				isSilent = !isSilent;
				updateVolume(currentVolume);
				cancelDelayHide();
				hideControllerDelay();
				return true;
			}

		});

		seekBar = (SeekBar) controlView.findViewById(R.id.seekbar);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {

				if (fromUser) {

					vv.seekTo(progress);

				}

			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {

				myHandler.removeMessages(HIDE_CONTROLER);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

				myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
			}
		});

		getScreenSize();

		mGestureDetector = new GestureDetector(new SimpleOnGestureListener() {

			@Override
			public boolean onDoubleTap(MotionEvent e) {

				if (isFullScreen) {
					setVideoScale(SCREEN_DEFAULT);
				} else {
					setVideoScale(SCREEN_FULL);
				}
				isFullScreen = !isFullScreen;
				Log.d(TAG, "onDoubleTap");

				if (isControllerShow) {
					showController();
				}

				return true;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {

				if (!isControllerShow) {
					showController();
					hideControllerDelay();
				} else {
					cancelDelayHide();
					hideController();
				}

				return true;
			}

			@Override
			public void onLongPress(MotionEvent e) {

				if (isPaused) {
					vv.start();
					play_Btn.setImageResource(R.drawable.pause);
					cancelDelayHide();
					hideControllerDelay();
				} else {
					vv.pause();
					play_Btn.setImageResource(R.drawable.play);
					cancelDelayHide();
					showController();
				}
				isPaused = !isPaused;

			}
		});
		vv.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer arg0) {

				setVideoScale(SCREEN_DEFAULT);
				isFullScreen = false;
				if (isControllerShow) {
					showController();
				}

				int i = vv.getDuration();
				Log.d("onCompletion", "" + i);
				seekBar.setMax(i);
				i /= 1000;
				int minute = i / 60;
				int hour = minute / 60;
				int second = i % 60;
				minute %= 60;
				durationTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));

				/*
				 * controler.showAtLocation(vv, Gravity.BOTTOM, 0, 0);
				 * controler.update(screenWidth, controlHeight);
				 * myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
				 */

				vv.start();
				play_Btn.setImageResource(R.drawable.pause);
				hideControllerDelay();
				myHandler.sendEmptyMessage(PROGRESS_CHANGED);
			}
		});

		vv.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer arg0) {

				VideoPlayerActivity.this.finish();
			}

		});
		Intent intent = getIntent();
		if (intent != null) {
			String displayName = intent.getStringExtra("displayName");
			title.setText(displayName);
			path = intent.getStringExtra("path");
			vv.setVideoPath(path);
			isChangedVideo = true;
			mAudioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
					AudioManager.AUDIOFOCUS_GAIN);
		}
		delete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				deleteVideo(path);
				finish();
			}
		});
	}

	private final static int PROGRESS_CHANGED = 0;
	private final static int HIDE_CONTROLER = 1;

	Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

			case PROGRESS_CHANGED:

				int i = vv.getCurrentPosition();
				seekBar.setProgress(i);
				seekBar.setSecondaryProgress(0);

				i /= 1000;
				int minute = i / 60;
				int hour = minute / 60;
				int second = i % 60;
				minute %= 60;
				playedTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
				sendEmptyMessageDelayed(PROGRESS_CHANGED, 100);
				break;

			case HIDE_CONTROLER:
				hideController();
				break;
			}

			super.handleMessage(msg);
		}
	};
	private OnAudioFocusChangeListener focusListener = new OnAudioFocusChangeListener() {

		@Override
		public void onAudioFocusChange(int focusChange) {
			switch (focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS:
				if (!isPaused) {
					// we do not need get focus back in this situation

					// 会长时间失去，所以告知下面的判断，获得焦点后不要自动播放
					mPausedByTransientLossOfFocus = false;

					vv.pause();
					play_Btn.setImageResource(R.drawable.play);// 因为会长时间失去，所以直接暂停
				}

				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				if (!isPaused) {
					vv.pause();
					play_Btn.setImageResource(R.drawable.play);
					mPausedByTransientLossOfFocus = true;
					cancelDelayHide();
					hideControllerDelay();
				}
				break;
			case AudioManager.AUDIOFOCUS_GAIN:
				if (isPaused && mPausedByTransientLossOfFocus) {
					vv.start();
					play_Btn.setImageResource(R.drawable.pause);
					cancelDelayHide();
					hideControllerDelay();
				}
				break;

			default:
				break;
			}

		}

	};

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		boolean result = mGestureDetector.onTouchEvent(event);

		if (!result) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
					cancelDelayHide();
					hideControllerDelay();
			}
			result = super.onTouchEvent(event);
		}

		return result;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		getScreenSize();
		if (isControllerShow) {
			cancelDelayHide();
			showController();
			hideControllerDelay();
		}

		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
		Log.e(TAG, "onPause()");
		playedTime = vv.getCurrentPosition();
		vv.pause();
		play_Btn.setImageResource(R.drawable.play);
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.e(TAG, "onResume()");
		if (isChangedVideo) {
			vv.seekTo(playedTime);
			vv.start();
		}
		if (vv.isPlaying()) {
			play_Btn.setImageResource(R.drawable.pause);
			hideControllerDelay();
		}
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		Log.e(TAG, "onDestroy()");
		if (controler.isShowing()) {
			controler.dismiss();
			titleWindow.dismiss();
		}
		if (mSoundWindow.isShowing()) {
			mSoundWindow.dismiss();
		}

		myHandler.removeMessages(PROGRESS_CHANGED);
		myHandler.removeMessages(HIDE_CONTROLER);

		if (vv.isPlaying()) {
			vv.stopPlayback();
		}
		mAudioManager.abandonAudioFocus(focusListener);
		super.onDestroy();
	}

	private void getScreenSize() {
		Display display = getWindowManager().getDefaultDisplay();
		screenHeight = display.getHeight();
		screenWidth = display.getWidth();
		controlHeight = screenHeight / 4;

	}

	private void hideController() {
		if (controler.isShowing()) {
			controler.update(0, 0, 0, 0);
			titleWindow.update(0, 0, screenWidth, 0);
		}
		if (mSoundWindow.isShowing()) {
			mSoundWindow.dismiss();
			isSoundShow = false;
		}
	}

	private void hideControllerDelay() {
		myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
	}

	private void showController() {
		controler.update(0, 0, screenWidth, controlHeight);
		if (isFullScreen) {
			titleWindow.update(0, 0, screenWidth, 60);
		} else {
			titleWindow.update(0, 25, screenWidth, 60);
		}
		isControllerShow = true;
	}

	private void cancelDelayHide() {
		myHandler.removeMessages(HIDE_CONTROLER);
	}

	private final static int SCREEN_FULL = 0;
	private final static int SCREEN_DEFAULT = 1;

	private void setVideoScale(int flag) {

		LayoutParams lp = vv.getLayoutParams();

		switch (flag) {
		case SCREEN_FULL:

			Log.d(TAG, "screenWidth: " + screenWidth + " screenHeight: " + screenHeight);
			vv.setVideoScale(screenWidth, screenHeight);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

			break;

		case SCREEN_DEFAULT:

			int videoWidth = vv.getVideoWidth();
			int videoHeight = vv.getVideoHeight();
			int mWidth = screenWidth;
			int mHeight = screenHeight - 25;

			if (videoWidth > 0 && videoHeight > 0) {
				if (videoWidth * mHeight > mWidth * videoHeight) {
					// Log.i("@@@", "image too tall, correcting");
					mHeight = mWidth * videoHeight / videoWidth;
				} else if (videoWidth * mHeight < mWidth * videoHeight) {
					// Log.i("@@@", "image too wide, correcting");
					mWidth = mHeight * videoWidth / videoHeight;
				} else {

				}
			}

			vv.setVideoScale(mWidth, mHeight);

			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

			break;
		}
	}

	private int findAlphaFromSound() {
		if (mAudioManager != null) {
			// int currentVolume =
			// mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			int alpha = currentVolume * (0xCC - 0x55) / maxVolume + 0x55;
			return alpha;
		} else {
			return 0xCC;
		}
	}

	private void updateVolume(int index) {
		if (mAudioManager != null) {
			if (isSilent) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
			} else {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
			}
			currentVolume = index;
			sound_Btn.setAlpha(findAlphaFromSound());
		}
	}

	private void deleteVideo(String path) {
		File file = new File(path);
		file.deleteOnExit();
	}

}