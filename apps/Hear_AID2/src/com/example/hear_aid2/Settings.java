package com.example.hear_aid2;

import java.util.Arrays;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;


import com.mediatek.xlog.Xlog;

import android.media.AudioSystem;

public class Settings extends Activity {

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

	private int mTypeMedia = 7;
	private int mTypeSph = 4;
	private int mTypeMic = 2;
	private int mCurMediaV = 70;
	private int mCurSphV = 50;
	private int mCurMicV = 70;
	
	private static int VOL_70 = 70;
	private static int  VOL_50 = 50; 
	private RadioGroup mRadioGroup;
	private RadioButton mRadioFirst;
	private RadioButton mRadioSecond;
	private SharedPreferences mSharedPreferences;
	private Editor edit = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		initView();
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
		mCurrentValue = getValue(mData, mCurrentMode, mTypeIndex, mLevelIndex);

		// mCurMediaV = getValue(mData, mCurrentMode, mTypeMedia, mLevelIndex);
		// mCurSphV = getValue(mData, mCurrentMode, mTypeSph, mLevelIndex);
		// mCurMicV = getValue(mData, mCurrentMode, mTypeMic, mLevelIndex);
		showToast("mCurMediaV  : " + mCurMediaV + "mCurSphV:" + mCurSphV + "mCurMicV:" + mCurMicV);
	}

	private void initView() {
		mSharedPreferences = getSharedPreferences("status", Activity.MODE_PRIVATE);
		edit = mSharedPreferences.edit();
		mRadioGroup = (RadioGroup) findViewById(R.id.radiogroup);
		mRadioFirst = (RadioButton) findViewById(R.id.radiobutton_first);
		mRadioSecond = (RadioButton) findViewById(R.id.radiobutton_second);
		boolean isSecond = mSharedPreferences.getBoolean("isSecond", false);
		if (isSecond) {
			mRadioSecond.setChecked(true);
		} else {
			mRadioFirst.setChecked(true);
		}
		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.radiobutton_first:
					mRadioFirst.setChecked(true);
					setMaxVolEdit();
					firstVolume();
					initSecondVolume();
					edit.putBoolean("isSecond", false);
					edit.commit();
					break;
				case R.id.radiobutton_second:
					mRadioSecond.setChecked(true);
					secondVolume();
					edit.putBoolean("isSecond", true);
					edit.commit();
					break;

				default:
					break;
				}

			}
		});
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
	 * 功能简述:一级 声音放大 <br>
	 * 功能详细描述: <br>
	 * 注意:
	 */
	public void firstVolume() {
		byte editByte = (byte) VALUE_RANGE_160;
		setMaxVolData(editByte, false);
		setAudioData();
	}

	/**
	 * <br>
	 * 功能简述:把 一级 功放简绍到默认值 <br>
	 * 功能详细描述: <br>
	 * 注意:
	 */
	public void initFirstVolume() {
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
		mCurMediaV = getValue(mData, mCurrentMode, mTypeMedia, mLevelIndex);
		mCurSphV = getValue(mData, mCurrentMode, mTypeSph, mLevelIndex);
		mCurMicV = getValue(mData, mCurrentMode, mTypeMic, mLevelIndex);
		showToast("mCurMediaV  : " + mCurMediaV + "mCurSphV:" + mCurSphV + "mCurMicV:" + mCurMicV);
	}

	/**
	 * <br>
	 * 功能简述:把 二级 功放简绍到默认值 <br>
	 * 功能详细描述: <br>
	 * 注意:
	 */
	public void initSecondVolume() {
		// 媒体
		setValue(mData, mCurrentMode, mTypeMedia, mLevelIndex, (byte) VOL_70);
		setAudioData();
		// Sph
		setValue(mData, mCurrentMode, mTypeSph, mLevelIndex, (byte) VOL_50);
		setAudioData();
		// Mic
		setValue(mData, mCurrentMode, mTypeMic, mLevelIndex, (byte) VOL_70);
		setAudioData();
		mCurMediaV = getValue(mData, mCurrentMode, mTypeMedia, mLevelIndex);
		mCurSphV = getValue(mData, mCurrentMode, mTypeSph, mLevelIndex);
		mCurMicV = getValue(mData, mCurrentMode, mTypeMic, mLevelIndex);
		showToast("mCurMediaV  : " + mCurMediaV + "mCurSphV:" + mCurSphV + "mCurMicV:" + mCurMicV);
	}
}
