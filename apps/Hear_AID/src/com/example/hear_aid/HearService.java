package com.example.hear_aid;

import java.util.Arrays;


import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.media.AudioSystem;
import com.mediatek.xlog.Xlog;

public class HearService extends Service {
	private static final String TAG = "EM/Audio_modesetting";

	/** normal, headset, handfree. */
	private static int sMaxVolMode = 3;
	/** 7 level. */
	private static int sMaxVolLevel = 7;
	/** 8 level. */
	private static int sMaxVolType = 8;
	/** 8 types. */
	private static final int MAX_VOL_SIZE = 6;
	private static int[] sOffSet = { sMaxVolMode * sMaxVolLevel * 0,
			sMaxVolMode * sMaxVolLevel * 1, sMaxVolMode * sMaxVolLevel * 2,
			sMaxVolMode * sMaxVolLevel * 3, sMaxVolMode * sMaxVolLevel * 4,
			sMaxVolMode * sMaxVolLevel * 5, sMaxVolMode * sMaxVolLevel * 6,
			sMaxVolMode * sMaxVolLevel * 7 };
	/** Media type number. */
	private static int sTypeMedia = 6;

	private static final int TYPE_MAX_NORMAL = 6;
	private static final int TYPE_MAX_HEADSET = 6;
	private static final int TYPE_MAX_SPEAKER = 6;
	private static final int TYPE_MAX_HEADSPEAKER = 8;
	private static final int TYPE_MAX_EXTAMP = 6;
	/** Data struct size. */
	private static int sStructSize = sMaxVolMode * sMaxVolLevel * sMaxVolType;
	/** Get custom data size. */
	private static final int GET_CUSTOMD_DATASIZE = 5;
	/** Get custom data. */
	private static int sSetCustomerData = 6;
	/** Set custom data size. */
	private static int sGetCustomerData = 7;
	/** Get data error dialog id. */
	private static final int DIALOG_ID_GET_DATA_ERROR = 1;
	/** set audio mode parameter value success dialog id. */
	private static final int DIALOG_ID_SET_SUCCESS = 2;
	/** set audio mode parameter value failed dialog id. */
	private static final int DIALOG_ID_SET_ERROR = 3;
	private static final int VALUE_RANGE_255 = 255;
	private static final int VALUE_RANGE_160 = 160;
	private static int sModeMicIndex = 2;
	private static int sModeSphIndex = 4;
	private static int sModeSph2Index = 4;
	private static int sModeSidIndex = 5;
	private static final int AUDIO_COMMAND_PARAM0 = 0x20;
	private static final int AUDIO_COMMAND_PARAM1 = 0x21;
	private static final int AUDIO_COMMAND_PARAM2 = 0x22;
	private static final int CONSTANT_256 = 256;
	private static final int CONSTANT_0XFF = 0xFF;

	/** Selected category: normal, headset, loudspeaker or headset_loudspeaker. */
	private int mCurrentMode = 1;
	/** Selected mode index. */
	private int mTypeIndex;
	/** Selected level index. */
	private int mLevelIndex;
	/** value range 255. */
	private int mValueRange = VALUE_RANGE_255;
	/** Audio data byte array. */
	private byte[] mData = null;
	/** Is first fir set?. */
	private boolean mIsFirstFirSet = true;
	private boolean mSupportEnhance = true;

	/** Current Max Vol */
	private int mCurrentMaxV;
	private int mCurrentValue;
	boolean isRecording = true;// 是否录制
	static final int frequency = 44100;
	static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	int recBufSize, playBufSize;
	private AudioRecord audioRecord;
	private AudioTrack audioTrack;
	private TelephonyManager telephonyManager;
	private AudioManager audioManager;
	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}
	@Override
	public void onCreate() {
		super.onCreate();
		Resources resources = getResources();
		Xlog.v(TAG, "mCurrentMode: " + mCurrentMode + "mSupportEnhance: " + mSupportEnhance);
		sMaxVolMode = 4;
		sMaxVolLevel = 15;
		sMaxVolType = 9;
		sTypeMedia = 7;
		sStructSize = sMaxVolMode * sMaxVolLevel * sMaxVolType + TYPE_MAX_NORMAL + TYPE_MAX_HEADSET
				+ TYPE_MAX_SPEAKER + TYPE_MAX_HEADSPEAKER + TYPE_MAX_EXTAMP + sMaxVolType;
		sSetCustomerData = 0x101;
		sGetCustomerData = 0x100;
		sModeSph2Index = 5;
		sModeSidIndex = 6;
		sOffSet = new int[] { sMaxVolMode * sMaxVolLevel * 0, sMaxVolMode * sMaxVolLevel * 1,
				sMaxVolMode * sMaxVolLevel * 2, sMaxVolMode * sMaxVolLevel * 3,
				sMaxVolMode * sMaxVolLevel * 4, sMaxVolMode * sMaxVolLevel * 5,
				sMaxVolMode * sMaxVolLevel * 6, sMaxVolMode * sMaxVolLevel * 7,
				sMaxVolMode * sMaxVolLevel * 8, sMaxVolMode * sMaxVolLevel * 9 };

		mData = new byte[sStructSize];
		Arrays.fill(mData, 0, sStructSize, (byte) 0);
		// get the current data
		int ret = AudioSystem.getAudioData(sGetCustomerData, sStructSize, mData);
		if (ret != 0) {
			Xlog.i(TAG, "AudioModeSetting GetAudioData return value is : " + ret);
		}
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		telephonyManager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

		audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);

		recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

		playBufSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
		// -----------------------------------------
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
				channelConfiguration, audioEncoding, recBufSize);

		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, channelConfiguration,
				audioEncoding, playBufSize, AudioTrack.MODE_STREAM);
		// ------------------------------------------
		audioTrack.setStereoVolume(1.0f, 1.0f);// 设置播放的声音
		
		mCurrentValue = getValue(mData, mCurrentMode, mTypeIndex, mLevelIndex);
		
		setMaxVolEdit();
		showToast("value:"+mCurrentValue+ "max"+mCurrentMaxV);
		byte editByte = (byte) VALUE_RANGE_160;
		setMaxVolData(editByte, false);
		setAudioData();
		Xlog.v(TAG,"start");
		new RecordPlayThread().start();// 开启边录边放线程
	}
	private class MyPhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			// String stuate = "";
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				// stuate = "没有通话状态";
				// Log.e("MyPhoneStateListener", stuate);
				isRecording = true;

				break;
			case TelephonyManager.CALL_STATE_RINGING:
				// stuate = "被呼叫中";
				// Log.e("MyPhoneStateListener", stuate);
				isRecording = false;
				// audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL,
				// audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
				// 0);
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				// stuate = "通话中";
				// Log.e("MyPhoneStateListener", stuate);
				isRecording = true;
				break;

			default:
				break;
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	}

	class RecordPlayThread extends Thread {
		public void run() {
			try {
				Xlog.v(TAG,"RecordPlayThread run()");
				byte[] buffer = new byte[recBufSize];
				audioRecord.startRecording();// 开始录音
				audioTrack.play();// 开始播放
				Xlog.v(TAG,"isRecording:"+isRecording);
				while (isRecording) {
					// 读取Mic的声音
					Xlog.v(TAG,"isRecording:"+isRecording);
					int bufferReadResult = audioRecord.read(buffer, 0, recBufSize);

					byte[] tmpBuf = new byte[bufferReadResult];
					System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);
					//
					audioTrack.write(tmpBuf, 0, tmpBuf.length);
				}
				audioTrack.stop();
				audioRecord.stop();
				audioRecord.release();
				audioRecord = null;
			} catch (Throwable t) {
				// Toast.makeText(MainActivity.this, t.getMessage(),
				// Toast.LENGTH_SHORT).show();;
				t.printStackTrace();
			}
		}
	};
	@Override
	public void onDestroy() {
		super.onDestroy();
		byte editByt = (byte) (VALUE_RANGE_160 - 60);
		setMaxVolData(editByt, false);
		setAudioData();
		isRecording = false;
	}
	private void setValue(byte[] dataPara, int mode, int type, int level, byte val) {
		if (dataPara == null || mode >= sMaxVolMode || type >= sMaxVolType || level >= sMaxVolLevel) {
			Xlog.d(TAG, "assert! Check the setting value.");
		}
		Xlog.d(TAG, "setValue() mode:" + mode + ", type:" + type + "level:" + level);
		dataPara[mode * sMaxVolLevel + level + sOffSet[type]] = val;
	}

	private int getValue(byte[] dataPara, int mode, int type, int level) {
		if (dataPara == null || mode >= sMaxVolMode || type >= sMaxVolType || level >= sMaxVolLevel) {
			Xlog.d(TAG, "assert! Check the setting value.");
		}
		return CONSTANT_0XFF & (dataPara[mode * sMaxVolLevel + level + sOffSet[type]]);
	}
	private void setMaxVolEdit() {
		Xlog.i(TAG, "Set max vol Edit.");
		mCurrentMaxV = getMaxValue(mData, mCurrentMode);
		// mEditMaxVol.setText(String.valueOf(mCurrentMaxV));

	}

	private void setMaxValue(byte[] dataPara, int mode, byte val, boolean dual) {
		if (dataPara == null || mode >= sMaxVolMode) {
			Xlog.d(TAG, "assert! Check the setting value.");
		}
		if (dual && (mode == 3)) {
			dataPara[sOffSet[sMaxVolType] + mode * MAX_VOL_SIZE + 1] = val;
		} else {
			dataPara[sOffSet[sMaxVolType] + mode * MAX_VOL_SIZE] = val;
		}
	}

	private int getMaxValue(byte[] dataPara, int mode) {
		if (dataPara == null || mode >= sMaxVolMode) {
			Xlog.d(TAG, "assert! Check the setting value.");
		}
		return CONSTANT_0XFF & (dataPara[sOffSet[sMaxVolType] + mode * MAX_VOL_SIZE]);

	}

	private void setMaxVolData(byte val) {
		// headset mode
		setValue(mData, 0, sTypeMedia, sModeSidIndex, val);

	}

	private void setMaxVolData(byte val, boolean dual) {
		setMaxValue(mData, mCurrentMode, val, dual);
	}

	private void setAudioData() {
		int result = AudioSystem.setAudioData(sSetCustomerData, sStructSize, mData);
		if (0 == result) {
			
		} else {
			
			Xlog.i(TAG, "AudioModeSetting SetAudioData return value is : " + result);
		}
	}
	private void showToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
}
