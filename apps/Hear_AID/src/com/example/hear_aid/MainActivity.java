package com.example.hear_aid;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {
	public static String TAG = "ZhuTingQi";

	private Button mSubBtn;
	private Button mAddBtn;
	private SeekBar mSeekBar;
	private Button mStartButton;
	private LinearLayout mLinearVol;

	private SharedPreferences mSharedPreferences;
	private Editor edit = null;

	private AudioManager mAudioManager;
	private int mVolume;

	
	
	public static final String MSG_START = "start";
	public static final String MSG_FIRST = "first";
	public static final String MSG_SECOND = "second";
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
		mSubBtn = (Button) findViewById(R.id.main_sub);
		mSubBtn.setOnClickListener(this);
		mAddBtn = (Button) findViewById(R.id.main_add);
		mAddBtn.setOnClickListener(this);
		mLinearVol = (LinearLayout) findViewById(R.id.volume);
		mStartButton = (Button) findViewById(R.id.start);
		mStartButton.setOnClickListener(this);
		mSeekBar = (SeekBar) findViewById(R.id.progress_main_vol);
		mSeekBar.setOnSeekBarChangeListener(this);
		mSharedPreferences = getSharedPreferences("status", Activity.MODE_PRIVATE);
		boolean isNoneOpened = mSharedPreferences.getBoolean("isOpened", true);
		edit = mSharedPreferences.edit();
		if (isNoneOpened) {
			edit.putBoolean("isOpened", false);
			edit.putBoolean("isOpen", false);
			edit.putBoolean("First", false);
			edit.putBoolean("Second", false);
			edit.commit();
			
		}
//		boolean isFirst = mSharedPreferences.getBoolean("First", false);
		
		
	}
	@Override
	protected void onStart() {
		if(GlobalApplication.isOpen){
			mStartButton.setBackgroundResource(R.drawable.button_background_on);
			setEnable(true);
		}else{
			mStartButton.setBackgroundResource(R.drawable.button_background);
			setEnable(false);
		}
		super.onResume();
	}
	
	private void setEnable(boolean b) {
		mSubBtn.setEnabled(b);
		mAddBtn.setEnabled(b);
		mSeekBar.setEnabled(b);
	}

	@Override
	public void onClick(View v) {
		Intent intent = null;

		switch (v.getId()) {
		case R.id.setting:
			if (GlobalApplication.isOpen) {
				intent = new Intent(this, Settings.class);
				startActivity(intent);
			} else {
				Toast.makeText(MainActivity.this, "请先开启助听器", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.start:
			intent = new Intent(this, HearService.class);
			if (GlobalApplication.isOpen) {
				GlobalApplication.isOpen = false;
				GlobalApplication.isOpenFirst = false;
				mLinearVol.setFocusable(false);
				edit.putBoolean("First", false);
				edit.putBoolean("isOpen", false);
				stopService(intent);
				mStartButton.setBackgroundResource(R.drawable.button_background);
				setEnable(false);
			} else {
				GlobalApplication.isOpen = true;
				GlobalApplication.isOpenFirst = true;
				mLinearVol.setFocusable(true);
				edit.putBoolean("First", true);
				edit.putBoolean("isOpen", true);
				intent.putExtra("MSG", MSG_START);
				startService(intent);
				mStartButton.setBackgroundResource(R.drawable.button_background_on);
				setEnable(true);
			}
			edit.commit();
			break;
		case R.id.main_sub:
			if (mVolume > 0) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, mVolume - 1, 0);
				mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
				mSeekBar.setProgress(mVolume);
			}
			
			break;
		case R.id.main_add:
			if (mVolume < 15) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, mVolume + 1, 0);
				mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
				mSeekBar.setProgress(mVolume);
			}
			break;
		default:
			break;
		}

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(fromUser){
			mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, seekBar.getProgress(), 0);
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
