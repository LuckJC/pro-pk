package com.example.hear_aid;



import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {
	public static String TAG = "ZhuTingQi";
	private SeekBar mSeekBar;
	private Button mStartButton;

	private boolean isFirst = false;
	private boolean isOpen = false;
	private SharedPreferences mSharedPreferences;
	private Editor edit = null;

	private AudioManager mAudioManager;
	private int mVolume;
	private int startVol;
	private int endVol;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
		mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
		mSeekBar.setProgress(mVolume);
	}

	private void initView() {
		findViewById(R.id.setting).setOnClickListener(this);
		findViewById(R.id.main_sub).setOnClickListener(this);
		findViewById(R.id.main_add).setOnClickListener(this);
		mStartButton = (Button) findViewById(R.id.start);
		mStartButton.setOnClickListener(this);
		mSeekBar = (SeekBar) findViewById(R.id.progress_main_vol);
		mSeekBar.setOnSeekBarChangeListener(this);
		mSharedPreferences = getSharedPreferences("status", Activity.MODE_PRIVATE);
		isFirst = mSharedPreferences.getBoolean("isFirst", true);
		edit = mSharedPreferences.edit();
		if (isFirst) {
			edit.putBoolean("isFirst", false);
			edit.putBoolean("isOpen", false);
			edit.putBoolean("isSecond", false);
			edit.commit();
		}
		isOpen = mSharedPreferences.getBoolean("isOpen", false);
		if (isOpen) {
			mStartButton.setText(R.string.stop);
		} else {
			mStartButton.setText(R.string.start);
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;
		int curSeeBar = mSeekBar.getProgress();
		mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
		switch (v.getId()) {
		case R.id.setting:
			intent = new Intent(this, Settings.class);
			startActivity(intent);
			break;
		case R.id.start:
			intent = new Intent(this, HearService.class);
			isOpen = mSharedPreferences.getBoolean("isOpen", false);
			if (isOpen) {
				edit.putBoolean("isOpen", false);
				stopService(intent);
				mStartButton.setText(R.string.start);
			} else {
				edit.putBoolean("isOpen", true);
				startService(intent);
				mStartButton.setText(R.string.stop);
			}
			edit.commit();
			break;
		case R.id.main_sub:
			Toast.makeText(this, mVolume + "", 0).show();
			if (curSeeBar > 0) {
				mSeekBar.setProgress(curSeeBar - 1);
				mAudioManager.adjustVolume(AudioManager.ADJUST_LOWER, 0);
				mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
			}
			break;
		case R.id.main_add:
			Toast.makeText(this, mVolume + "", 0).show();
			if (curSeeBar < 15) {
				mSeekBar.setProgress(curSeeBar + 1);
				mAudioManager.adjustVolume(AudioManager.ADJUST_RAISE, 0);
				mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
			}
			break;
		default:
			break;
		}

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		startVol = seekBar.getProgress();
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		endVol = seekBar.getProgress();
		if (startVol > endVol) {
			int count = startVol - endVol;
			for (int i = 0; i < count; i++) {
				mAudioManager.adjustVolume(AudioManager.ADJUST_LOWER, 0);
				Log.e(TAG, "onStopTrackingTouch ADJUST_LOWER  :"+mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
			}
		} else if (startVol < endVol) {
			int count = endVol - startVol;
			for (int i = 0; i < count; i++) {
				mAudioManager.adjustVolume(AudioManager.ADJUST_RAISE, 0);
				Log.e(TAG, "onStopTrackingTouch  ADJUST_RAISE :"+mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
			}
		}
	}
}
