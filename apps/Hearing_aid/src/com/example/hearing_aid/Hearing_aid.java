/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly
 * prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY
 * ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY
 * THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK
 * SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO
 * RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN
 * FORUM.
 * RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
 * LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation
 * ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.example.hearing_aid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;

import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioSystem;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Audio mode parameter settings. */
public class Hearing_aid extends Activity implements OnClickListener {

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
	private int mLevelIndex = 0;
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
	private Button mStart;
	private Button mStop;
	private Button mExit;
	private Button mMedia;
	private Button mSph;
	boolean isRecording = true;// 是否录制
	static final int frequency = 44100;
	static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	int recBufSize, playBufSize;
	private AudioRecord audioRecord;
	private AudioTrack audioTrack;
	private TelephonyManager telephonyManager;
	private AudioManager audioManager;

	private int mTypeMedia = 7;
	private int mTypeSph = 4;
	private int mCurMediaV;
	private int mCurSphV;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Resources resources = getResources();
		Xlog.v(TAG, "mCurrentMode: " + mCurrentMode + "mSupportEnhance: " + mSupportEnhance);

		setContentView(R.layout.activity_main);
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
			showDialog(DIALOG_ID_GET_DATA_ERROR);
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
		mStart = (Button) this.findViewById(R.id.start);
		mStop = (Button) this.findViewById(R.id.stop);
		mExit = (Button) this.findViewById(R.id.exit);
		mMedia = (Button) findViewById(R.id.TextView01);
		mSph = (Button) findViewById(R.id.TextView02);
		mStop.setEnabled(false);
		mStart.setOnClickListener(this);
		mStop.setOnClickListener(this);
		mExit.setOnClickListener(this);
		mMedia.setOnClickListener(this);
		mSph.setOnClickListener(this);
		audioTrack.setStereoVolume(1.0f, 1.0f);// 设置播放的声音
	
		mCurrentValue = getValue(mData, mCurrentMode, mTypeIndex, mLevelIndex);
		mCurMediaV = getValue(mData, mCurrentMode, mTypeMedia, mLevelIndex);
		mCurSphV = getValue(mData, mCurrentMode, mTypeSph, mLevelIndex);
		Xlog.i(TAG, "mCurMediaV  : " + mCurMediaV +"<>"+"mCurSphV:"+mCurSphV);
		setMaxVolEdit();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		byte editByte = (byte) (VALUE_RANGE_160 - 60);
		setMaxVolData(editByte, false);
		setAudioData();
		// 媒体
		setValue(mData, mCurrentMode, mTypeMedia, mLevelIndex, (byte)VALUE_RANGE_255);
        setAudioData();
        // 通话
        setValue(mData, mCurrentMode, mTypeSph, mLevelIndex, (byte)VALUE_RANGE_160);
        setAudioData();
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
				byte[] buffer = new byte[recBufSize];
				audioRecord.startRecording();// 开始录音
				audioTrack.play();// 开始播放

				while (isRecording) {
					// 读取Mic的声音
					int bufferReadResult = audioRecord.read(buffer, 0, recBufSize);

					byte[] tmpBuf = new byte[bufferReadResult];
					System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);
					//
					audioTrack.write(tmpBuf, 0, tmpBuf.length);
				}
				audioTrack.stop();
				audioRecord.stop();
				audioRecord.release();
			} catch (Throwable t) {
				// Toast.makeText(MainActivity.this, t.getMessage(),
				// Toast.LENGTH_SHORT).show();;
				t.printStackTrace();
			}
		}
	};

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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// float vol_max = audioTrack.getMaxVolume();
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			this.moveTaskToBack(true);
			return true;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
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
			showDialog(DIALOG_ID_SET_SUCCESS);
		} else {
			showDialog(DIALOG_ID_SET_ERROR);
			Xlog.i(TAG, "AudioModeSetting SetAudioData return value is : " + result);
		}
	}

	private boolean checkEditNumber(EditText edit, int maxValue) {
		String editStr = edit.getText().toString();
		if (null == editStr || editStr.length() == 0) {
			Toast.makeText(this, getString(R.string.input_null_tip), Toast.LENGTH_LONG).show();
			return false;
		}
		try {
			if (Integer.valueOf(editStr) > maxValue) {
				Toast.makeText(this, getString(R.string.number_arrage_tip) + maxValue,
						Toast.LENGTH_LONG).show();
				return false;
			}
		} catch (NumberFormatException e) {
			Toast.makeText(this, getString(R.string.number_arrage_tip) + maxValue,
					Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	private void showToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
	/**
	 * click the set button.
	 * 
	 * @param arg0
	 *            : click which view
	 */
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.start:
			showToast("value:"+mCurrentValue+ "max"+mCurrentMaxV);
			byte editByte = (byte) VALUE_RANGE_160;
			setMaxVolData(editByte, false);
			setAudioData();
			mStart.setEnabled(false);
			mStop.setEnabled(true);
			new RecordPlayThread().start();// 开启边录边放线程
			break;
		case R.id.stop:
			byte editByt = (byte) (VALUE_RANGE_160 - 60);
			setMaxVolData(editByt, false);
			setAudioData();
			mStart.setEnabled(true);
			mStop.setEnabled(false);
			isRecording = false;
			break;
		case R.id.exit:
			isRecording = false;
			finish();
			break;
		case R.id.TextView01:
			setValue(mData, mCurrentMode, mTypeMedia, mLevelIndex, (byte)VALUE_RANGE_255);
            setAudioData();
			break;
		case R.id.TextView02:
			setValue(mData, mCurrentMode, mTypeSph, mLevelIndex, (byte)VALUE_RANGE_160);
            setAudioData();
			break;
		default:
			break;
		}
		// if (arg0.getId() == mBtnSet.getId()) {
		//
		// if (!checkEditNumber(mValueEdit, mValueRange)) {
		// return;
		// }
		// String editString = mValueEdit.getText().toString();
		// int editInteger = Integer.valueOf(editString);
		// byte editByte = (byte) editInteger;
		// setValue(mData, mCurrentMode, mTypeIndex, mLevelIndex, editByte);
		// setAudioData();
		// } else if (arg0.getId() == mBtnSetMaxVol.getId()) {

		// String editString = mEditMaxVol.getText().toString();
		// }
	}
	

}
