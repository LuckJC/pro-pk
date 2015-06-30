package com.android.watch.recorder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class RecService extends Service {
	public static NotificationCompat.Builder builder;
	//通知栏用
	NotificationManager notificationManager;
	private static final String TAG = "RecService";
	private static final String SUFFIX=".amr";             //录音文件后缀
	private static final long VIBRATE_DURATION_SHORT = 500; //短震动时间
	private static final long VIBRATE_DURATION_LONG = 2000; //长震动时间
	private static final long REC_VIBRATE_INTERVAL = 1000 * 60 * 5; //录音过程中震动间隔
	private static final long[] REC_VIBRATE_PATTERN = 
			new long[] {REC_VIBRATE_INTERVAL, VIBRATE_DURATION_SHORT}; //录音过程中循环震动
	private static final int START_NOTIFICATION_ID = 1;
	
//	private MediaRecorder mMediaRecorder;
	private boolean mRecStarted;
	private File mFile;
	private File mDir;
	private Vibrator mVibrator;
    MainActivity me;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (mVibrator == null) {
			mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!MainActivity.isPause) {
			//开始录音
//			startRec();
			MainActivity.imageView.setImageResource(R.drawable.endre);
			start();
			MainActivity.isPause=true;
		} else {
			//停止录音
			stopRec();
			//退出服务
			stopSelf() ;
		}
		
		return START_NOT_STICKY;
	}
	
	/**
	 * 开始录音
	 */
//	private void startRec() {
//		Log.d(TAG, "startRec()");
//		boolean sdcardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
//		if (!sdcardExist) return;
//		String pathStr = Environment.getExternalStorageDirectory().getAbsolutePath()+"/YYT";
//		//String pathStr = "/storage/sdcard1/MIUI/"+"/YY";
//		mDir= new File(pathStr);
//		Log.d(TAG, "mDir exist()：" + mDir.exists());
//		if (mDir.exists()) {
//			if (!mDir.isDirectory()) {
//				Log.d(TAG, "mDir.exists but is not a directory!");
//				return;
//			}
//		} else if (!(mDir.mkdirs())) {
//			Log.d(TAG, "mDir.mkdirs() failed!");
//			return;
//		}
//		
//		mVibrator.vibrate(VIBRATE_DURATION_SHORT);
//		String mMinute1 = getTimeString();
//		mFile=new File(mDir, mMinute1+SUFFIX);
//		mMediaRecorder = new MediaRecorder();
//		// 设置录音为麦克风
//		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
//		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//		//录音文件保存这里
//		mMediaRecorder.setOutputFile(mFile.getAbsolutePath());
//		Log.d(TAG, mFile.getAbsolutePath()+"");
//		Log.d(TAG, "before prepare");
//		try {
//			mMediaRecorder.prepare();
//			Log.d(TAG, "after prepare");
//			mMediaRecorder.start();
//			Log.d(TAG, "after start");
//			mRecStarted = true;
//			mVibrator.vibrate(REC_VIBRATE_PATTERN, 0);
//			setNotification();
//		} catch (IOException e) {
//			e.printStackTrace();
//			mMediaRecorder.stop();
//			mMediaRecorder.release();
//			mMediaRecorder = null;
//		}
//	}
//	
	/**
	 * 停止录音
	 */
	private void stopRec() {
//		if (mMediaRecorder != null) {
//			// 停止录音
//			mMediaRecorder.stop();
//			mMediaRecorder.release();
//			mMediaRecorder = null;
//		}
//		mRecStarted = false;
//		if (mVibrator != null) {
//			mVibrator.vibrate(VIBRATE_DURATION_LONG);
//		}
		MainActivity.imageView.setImageResource(R.drawable.startrecorder);
		MainActivity.lists.add(MainActivity.myRecAudioFile.getPath());
		recorderStop();
	//	start();
		//buttonpause.setText("继续录音");
		//计时停止
		MainActivity.timer.cancel();
		MainActivity.isPause=false; 
		MainActivity.isReStart=false;
	}
	
	private String getTimeString() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");      
		Date curDate=new Date(System.currentTimeMillis());//获取当前时间      
		String time = formatter.format(curDate);
		return time;
	}
	
	@SuppressLint("NewApi")
	private void setNotification() {
		Notification.Builder builder = new Notification.Builder(this);
		builder.setContentTitle("录音中...");
		builder.setSmallIcon(R.drawable.notification_ic_small);
		Notification notification = builder.build();
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		startForeground(START_NOTIFICATION_ID, notification);
	}
	/**计时器**/
	public void start() {
		 TimerTask timerTask=new TimerTask() {
			@Override
			public void run() {
				MainActivity.second++;
				if(MainActivity.second>=60){
					MainActivity.second=0;
					MainActivity.minute++;
				}
				handler.sendEmptyMessage(0);
			}
		};
		 MainActivity.timer=new Timer();
		 MainActivity.timer.schedule(timerTask, 0,1000);
		try {
			if (!MainActivity.sdcardExit) {
				Toast.makeText(this, "请插入SD card",
						Toast.LENGTH_LONG).show();
				return;
			}
			String mMinute1=getTime();
			// 创建音频文件
//			myRecAudioFile = File.createTempFile(mMinute1, ".amr",
//					myRecAudioDir);
			MainActivity.myRecAudioFile=new File(MainActivity.myRecAudioDir,mMinute1+SUFFIX);
			MainActivity.mMediaRecorder = new MediaRecorder();
			// 设置录音为麦克风
			MainActivity.mMediaRecorder
					.setAudioSource(MediaRecorder.AudioSource.MIC);
			MainActivity.mMediaRecorder
					.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			MainActivity.mMediaRecorder
					.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//			mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);     
//			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			//录音文件保存这里
			MainActivity.mMediaRecorder.setOutputFile(MainActivity.myRecAudioFile
					.getAbsolutePath());
			MainActivity.mMediaRecorder.prepare();
			MainActivity.mMediaRecorder.start();
			MainActivity.save.setVisibility(View.VISIBLE);
			MainActivity.cancel.setVisibility(View.VISIBLE);
		} catch (IOException e) {
			e.printStackTrace();

		}
	}
	
	Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String minutes = null,seconds=null;
			if(MainActivity.minute<10){
				minutes="0"+MainActivity.minute;
			}else{
				minutes=MainActivity.minute+"";	
			}
			if(MainActivity.second<10){
				seconds="0"+MainActivity.second;
			}else{
				seconds=MainActivity.second+"";
			}
//			setNotification();
			panding();
			MainActivity.times.setText(minutes+":"+seconds);
		}
	};
	public void panding() {
		builder = new NotificationCompat.Builder(this);
		// builder对象在构造通知对象之前，做一些通知对象的设置
		// 小图标设置
		builder.setSmallIcon(R.drawable.recordermain);
		// 把资源转换成Bitmap对象
		Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.recordermain);
		// 设置大图标
		builder.setLargeIcon(bitmap);
		// 设置通知子描述
		//builder.setSubText("SubText");
		// 通知消息
		//builder.setNumber(9);
		// 通知标题
		builder.setContentTitle("录音机");
		// 设置通知子描述
		builder.setContentText(MainActivity.times.getText());
		// 设置进行中通知
		builder.setOngoing(true);
		//构造一个PendingIntent
//		Intent intent=new Intent(this, MainActivity.class);
		Intent intent=new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		ComponentName comp = new ComponentName("com.android.watch.recorder","com.android.watch.recorder.MainActivity");
		intent.setComponent(comp); 
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);//关键的一步，设置启动模式
		//这里需要使用PendingIntent.FLAG_UPDATE_CURRENT来覆盖以前已经存储在的PendingIntent
		PendingIntent pendingIntent=PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		//设置setContentIntent
		builder.setContentIntent(pendingIntent);
		//点击执行了PendingIntent之后，通知自动销毁
//		builder.setAutoCancel(true);
		// 构造通知对象
		Notification notification = builder.build();
		// 发布通知，通知ID为1
		notificationManager.notify(1, notification);
	}
	private String getTime(){
		SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyyMMddHHmmss");      
		Date  curDate=new  Date(System.currentTimeMillis());//获取当前时间      
		String   time   =   formatter.format(curDate);  
		System.out.println("当前时间");
		return time;
		}
	protected void recorderStop() {
		if (MainActivity.mMediaRecorder != null) {
			// 停止录音
			//mMediaRecorder.stop();
			MainActivity.mMediaRecorder.release();
			MainActivity.mMediaRecorder = null;
			MainActivity.timer.cancel();
		}
	}
}
