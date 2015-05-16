package com.android.watch.recorder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

public class RecService extends Service {
	private static final String TAG = "RecService";
	
	private static final String SUFFIX=".amr";             //录音文件后缀
	private static final long VIBRATE_DURATION_SHORT = 500; //短震动时间
	private static final long VIBRATE_DURATION_LONG = 2000; //长震动时间
	private static final long REC_VIBRATE_INTERVAL = 1000 * 60 * 5; //录音过程中震动间隔
	private static final long[] REC_VIBRATE_PATTERN = 
			new long[] {REC_VIBRATE_INTERVAL, VIBRATE_DURATION_SHORT}; //录音过程中循环震动
	private static final int START_NOTIFICATION_ID = 1;
	
	private MediaRecorder mMediaRecorder;
	private boolean mRecStarted;
	private File mFile;
	private File mDir;
	private Vibrator mVibrator;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

		if (mVibrator == null) {
			mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!mRecStarted) {
			//开始录音
			startRec();
		} else {
			//停止录音
			stopRec();
			//退出服务
			stopSelf();
		}
		
		return START_NOT_STICKY;
	}
	
	/**
	 * 开始录音
	 */
	private void startRec() {
		Log.d(TAG, "startRec()");
		boolean sdcardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		if (!sdcardExist) return;
		String pathStr = Environment.getExternalStorageDirectory().getAbsolutePath()+"/YYT";
		//String pathStr = "/storage/sdcard1/MIUI/"+"/YY";
		mDir= new File(pathStr);
		Log.d(TAG, "mDir exist()：" + mDir.exists());
		if (mDir.exists()) {
			if (!mDir.isDirectory()) {
				Log.d(TAG, "mDir.exists but is not a directory!");
				return;
			}
		} else if (!(mDir.mkdirs())) {
			Log.d(TAG, "mDir.mkdirs() failed!");
			return;
		}
		
		mVibrator.vibrate(VIBRATE_DURATION_SHORT);
		
		String mMinute1 = getTimeString();
		mFile=new File(mDir, mMinute1+SUFFIX);
		mMediaRecorder = new MediaRecorder();
		// 设置录音为麦克风
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		//录音文件保存这里
		mMediaRecorder.setOutputFile(mFile.getAbsolutePath());
		Log.d(TAG, "before prepare");
		try {
			mMediaRecorder.prepare();
			Log.d(TAG, "after prepare");
			mMediaRecorder.start();
			Log.d(TAG, "after start");
			mRecStarted = true;
			mVibrator.vibrate(REC_VIBRATE_PATTERN, 0);
//			setNotification();
		} catch (IOException e) {
			e.printStackTrace();
			mMediaRecorder.stop();
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
	}
	
	/**
	 * 停止录音
	 */
	private void stopRec() {
		if (mMediaRecorder != null) {
			// 停止录音
			mMediaRecorder.stop();
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
		mRecStarted = false;
		if (mVibrator != null) {
			mVibrator.vibrate(VIBRATE_DURATION_LONG);
		}
	}
	
	private String getTimeString() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");      
		Date curDate=new Date(System.currentTimeMillis());//获取当前时间      
		String time = formatter.format(curDate);
		return time;
	}
	
	private void setNotification() {
		Notification.Builder builder = new Notification.Builder(this);
		builder.setContentTitle("录音");
		builder.setSmallIcon(R.drawable.notification_ic_small);
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.notification_ic_big);
		builder.setLargeIcon(bmp);
		Notification notification = builder.build();
		notification.flags |= ~Notification.FLAG_AUTO_CANCEL;
		startForeground(START_NOTIFICATION_ID, notification);
	}
}
