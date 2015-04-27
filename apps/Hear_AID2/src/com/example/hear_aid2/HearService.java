package com.example.hear_aid2;

import java.util.Arrays;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.media.AudioSystem;
import android.media.AudioManager.OnAudioFocusChangeListener;

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
	boolean isRecording = true;// �Ƿ�¼��
	static final int frequency = 44100;
	static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	int recBufSize, playBufSize;
	private AudioRecord audioRecord;
	private AudioTrack audioTrack;
	private TelephonyManager telephonyManager;
	private AudioManager audioManager;

	private SharedPreferences mSharedPreferences;
	private boolean isFocusAudio = true;
	private int mTypeMedia = 7;
	private int mTypeSph = 4;
	private int mTypeMic = 2;
	private int mCurMediaV = 70;
	private int mCurSphV = 50;
	private int mCurMicV = 70;
	
	private static int VOL_70 = 70;
	private static int  VOL_50 = 50;
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
		int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.e(TAG, "获得焦点，AUDIOFOCUS_REQUEST_GRANTED");
			isFocusAudio = true;
		}
		recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

		playBufSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
		// -----------------------------------------
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
				channelConfiguration, audioEncoding, recBufSize);

		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, channelConfiguration,
				audioEncoding, playBufSize, AudioTrack.MODE_STREAM);
		// ------------------------------------------
		audioTrack.setStereoVolume(1.0f, 1.0f);// ���ò��ŵ�����

		mCurrentValue = getValue(mData, mCurrentMode, mTypeIndex, mLevelIndex);

		// mCurMediaV = getValue(mData, mCurrentMode, mTypeMedia, mLevelIndex);
		// mCurSphV = getValue(mData, mCurrentMode, mTypeSph, mLevelIndex);
		// mCurMicV = getValue(mData, mCurrentMode, mTypeMic, mLevelIndex);
		
		showToast("value:" + mCurrentValue + "max" + mCurrentMaxV);
		firstVolume();

		Xlog.v(TAG, "start");
		new RecordPlayThread().start();// ������¼�߷��߳�
	}

	private OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
				Log.e(TAG, "失去焦点，AUDIOFOCUS_LOSS_TRANSIENT");
				isFocusAudio = true;
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				// Stop playback
				Log.e(TAG, "失去焦点，AUDIOFOCUS_LOSS");
				isFocusAudio = true;
			} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
				Log.e(TAG, "获得焦点，AUDIOFOCUS_GAIN");
				isFocusAudio = true;
			} else  if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
				Log.e(TAG, "focus的值：" + focusChange);
			}
		}
	};

	private class MyPhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			// String stuate = "";
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				// stuate = "û��ͨ��״̬";
				// Log.e("MyPhoneStateListener", stuate);
				isRecording = true;

				break;
			case TelephonyManager.CALL_STATE_RINGING:
				// stuate = "��������";
				// Log.e("MyPhoneStateListener", stuate);
				isRecording = false;
				// audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL,
				// audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
				// 0);
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				// stuate = "ͨ����";
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
				Xlog.v(TAG, "RecordPlayThread run()");
				byte[] buffer = new byte[recBufSize];
				audioRecord.startRecording();// ��ʼ¼��
				audioTrack.play();// ��ʼ����
				Xlog.v(TAG, "isRecording:" + isRecording);
				while (isRecording && isFocusAudio) {
					// ��ȡMic������
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
		audioManager.abandonAudioFocus(afChangeListener);
		initFirstVolume();
		initSecondVolume();
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

	/**
	 * <br>
	 * 功能简述:一级  声音放大 <br>
	 * 功能详细描述: <br>
	 * 注意:
	 */
	public void firstVolume() {
		setMaxVolEdit();
		byte editByte = (byte) VALUE_RANGE_160;
		setMaxVolData(editByte, false);
		setAudioData();
	}
	/**
	 * <br>功能简述:把  一级  功放简绍到默认值
	 * <br>功能详细描述:
	 * <br>注意:
	 */
	public void initFirstVolume(){
		byte editByt = (byte) (VALUE_RANGE_160 - 60);
		setMaxVolData(editByt, false);
		setAudioData();
	}
	/**
	 * <br>
	 * 功能简述:二级 声音放大 <br>
	 * 功能详细描述: <br>
	 * 注意:
	 */
	public void secondVolume() {
		// 媒体
		setValue(mData, mCurrentMode, mTypeMedia, mLevelIndex, (byte) (VALUE_RANGE_255 - 80));
		setAudioData();
		// 通话
		setValue(mData, mCurrentMode, mTypeSph, mLevelIndex, (byte) (VALUE_RANGE_160 - 50));
		setAudioData();
		// Mic
		setValue(mData, mCurrentMode, mTypeMic, mLevelIndex, (byte) (VALUE_RANGE_255 - 80));
		setAudioData();
	}
	/**
	 * <br>功能简述:把  二级  功放简绍到默认值
	 * <br>功能详细描述:
	 * <br>注意:
	 */
	public void initSecondVolume() {
		// 媒体
		setValue(mData, mCurrentMode, mTypeMedia, mLevelIndex, (byte) VOL_70);
		setAudioData();
		//
		setValue(mData, mCurrentMode, mTypeSph, mLevelIndex, (byte) VOL_50);
		setAudioData();
		// Mic
		setValue(mData, mCurrentMode, mTypeMic, mLevelIndex, (byte) VOL_70);
		setAudioData();
	}
}
