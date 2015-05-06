package com.android.watch.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity{
	int startCount=1;
    int second=0;
	int minute=0;
	
	/**文件存在**/
	private boolean sdcardExit;
	public static File myRecAudioFile;
	//**是否暂停标志位**/
	private boolean isPause;
	/**在暂停状态中**/
	private boolean inThePause;
	/**录音保存路径**/
	public static File myRecAudioDir;
	private  final String SUFFIX=".amr";
	/**是否停止录音**/
	private boolean isStopRecord;
	/**记录需要合成的几段amr语音文件**/
	public static ArrayList<String> lists;
	private ArrayList<String> listTimes;
	public static Map map;
	/**存放音频文件列表**/
	public static ArrayList<String> recordFiles;
	public static ArrayList<Item> recordFile;
	private ArrayAdapter<String> adapter;
	private MediaRecorder mMediaRecorder;
	MediaPlayer mediaPlayer;
	private String length1 = null;
    ImageView imageView;
    ImageView menu;
    TextView times;
    Button cancel;
    Button save;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recorder_main);
		MyClick myClick=new MyClick();
		mediaPlayer = new MediaPlayer();
		lists=new ArrayList<String>();
		map=new HashMap();
		listTimes=new ArrayList<String>();
		imageView=(ImageView) this.findViewById(R.id.recorder);
		imageView.setOnClickListener(myClick);
		times=(TextView) this.findViewById(R.id.times);
		cancel=(Button) this.findViewById(R.id.cancel);
		save=(Button) this.findViewById(R.id.save);
		menu=(ImageView) this.findViewById(R.id.menu);
		menu.setOnClickListener(myClick);
		cancel.setOnClickListener(myClick);
		save.setOnClickListener(myClick);
		isPause=false;
		inThePause=false;
		// 判断sd Card是否插入
		sdcardExit = Environment.getExternalStorageState().equals(
						android.os.Environment.MEDIA_MOUNTED);
				// 取得sd card路径作为录音文件的位置
				if (sdcardExit){
					String pathStr = Environment.getExternalStorageDirectory().getAbsolutePath()+"/YYT";
					//String pathStr = "/storage/sdcard1/MIUI/"+"/YY";
					myRecAudioDir= new File(pathStr);
					if(!myRecAudioDir.exists()){
						myRecAudioDir.mkdirs();
						Log.v("录音", "创建录音文件！" + myRecAudioDir.exists());
					}
//					Environment.getExternalStorageDirectory().getPath() + "/" + PREFIX + "/";
				}
				// 取得sd card 目录里的.arm文件
				getRecordFiles();
//				map.put("recordFiles", recordFiles);
//				map.put("listTimes", listTimes);
//				adapter = new ArrayAdapter<String>(this,
//						android.R.layout.simple_list_item_1, recordFiles);
	}

	class MyClick implements View.OnClickListener{
		@Override
		public void onClick(View arg0) {
			switch (arg0.getId()) {
			case R.id.recorder:
				if(isPause){
					//当前正在录音的文件名，全程
					imageView.setImageResource(R.drawable.startrecorder);
					lists.add(myRecAudioFile.getPath());
					recorderStop();
					//start();
					//				buttonpause.setText("继续录音");
					//计时停止
//					timer.cancel();
					isPause=false; 
				}
				//正在录音，点击暂停,现在录音状态为暂停
				else{
					imageView.setImageResource(R.drawable.endre);
					start();
					isPause=true;
				}
//				}
				startCount++;
				break;
			case R.id.save:
				//timer.cancel();
				// TODO Auto-generated method stub
				//这里写暂停处理的 文件！加上list里面 语音合成起来
				if(!isPause){
					//在暂停状态按下结束键,处理list就可以了
					getInputCollection(lists, false);
					isPause=true;
					inThePause=false;
				//	adapter.add(myRecAudioFile.getName());
				}
				else{
					lists.add(myRecAudioFile.getPath());
					recorderStop();
					getInputCollection(lists, true);
				}
				Toast.makeText(MainActivity.this, "保存成功", 3000).show();
				minute=0;
				second=0;
				times.setText(00+":"+00);
				isStopRecord = true;
				imageView.setImageResource(R.drawable.startrecorder);
				save.setEnabled(false);
				cancel.setEnabled(false);
				isPause=false;
				break;
			case R.id.cancel:
				recorderStop();
				deleteListRecord(isPause);
				minute=0;
				second=0;
				times.setText(00+":"+00);
				imageView.setImageResource(R.drawable.startrecorder);
				save.setEnabled(false);
				cancel.setEnabled(false);
				isPause=false;
				break;
			case R.id.menu:
				Intent intent=new Intent(MainActivity.this, HistoryListActivity.class);
				intent.putExtra("recordFiles", recordFiles);
				startActivity(intent);
				break;
			default:
				break;
			}
		}
	}
	/**计时器**/
	Timer timer;
	public void start() {
		 TimerTask timerTask=new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				second++;
				if(second>=60){
					second=0;
					minute++;
				}
				handler.sendEmptyMessage(0);
			}
		};
		 timer=new Timer();
		 timer.schedule(timerTask, 0,1000);
		try {
			if (!sdcardExit) {
				Toast.makeText(MainActivity.this, "请插入SD card",
						Toast.LENGTH_LONG).show();
				return;
			}
			String mMinute1=getTime();
			Toast.makeText(MainActivity.this, "当前时间是:"+mMinute1,Toast.LENGTH_LONG).show();
			// 创建音频文件
//			myRecAudioFile = File.createTempFile(mMinute1, ".amr",
//					myRecAudioDir);
			myRecAudioFile=new File(myRecAudioDir,mMinute1+SUFFIX);
			mMediaRecorder = new MediaRecorder();
			// 设置录音为麦克风
			mMediaRecorder
					.setAudioSource(MediaRecorder.AudioSource.MIC);
			mMediaRecorder
					.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			mMediaRecorder
					.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			//录音文件保存这里
			mMediaRecorder.setOutputFile(myRecAudioFile
					.getAbsolutePath());
			mMediaRecorder.prepare();
			mMediaRecorder.start();
			cancel.setEnabled(true);
			save.setEnabled(true);
			isStopRecord = false;
		} catch (IOException e) {
			e.printStackTrace();

		}
	}
	Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String minutes = null,seconds=null;
			if(minute<10){
				minutes="0"+minute;
			}
			if(second<10){
				seconds="0"+second;
			}
			times.setText(minutes+":"+seconds);
		}
	};
	private String getTime(){
		SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyyMMddHHmmss");      
		Date  curDate=new  Date(System.currentTimeMillis());//获取当前时间      
		String   time   =   formatter.format(curDate);  
		System.out.println("当前时间");
		return time;
		}
	protected void recorderStop() {
		if (mMediaRecorder != null) {
			// 停止录音
			mMediaRecorder.stop();
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
		timer.cancel();
	}
	/**
	 *  @param isAddLastRecord 是否需要添加list之外的最新录音，一起合并
	 *  @return 将合并的流用字符保存
	 */
	public  void getInputCollection(List list,boolean isAddLastRecord){
		String	mMinute1=getTime();
		Toast.makeText(MainActivity.this, "当前时间是:"+mMinute1,Toast.LENGTH_LONG).show();
		// 创建音频文件,合并的文件放这里
		File file1=new File(myRecAudioDir,mMinute1+SUFFIX);
		FileOutputStream fileOutputStream = null;
		if(!file1.exists()){
			try {
				file1.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			fileOutputStream=new FileOutputStream(file1);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//list里面为暂停录音 所产生的 几段录音文件的名字，中间几段文件的减去前面的6个字节头文件
		
		for(int i=0;i<list.size();i++){
			File file=new File((String) list.get(i));
			try {
				FileInputStream fileInputStream=new FileInputStream(file);
				byte  []myByte=new byte[fileInputStream.available()];
				//文件长度
				int length = myByte.length;
				//头文件
				if(i==0){
						while(fileInputStream.read(myByte)!=-1){
								fileOutputStream.write(myByte, 0,length);
							}
						}
				//之后的文件，去掉头文件就可以了
				else{
					while(fileInputStream.read(myByte)!=-1){
						
						fileOutputStream.write(myByte, 6, length-6);
					}
				}
				fileOutputStream.flush();
				fileInputStream.close();
				System.out.println("合成文件长度："+file1.length());
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			}
		//结束后关闭流
		try {
			fileOutputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			//合成一个文件后，删除之前暂停录音所保存的零碎合成文件
			deleteListRecord(isAddLastRecord);
			//
//			adapter.add(file1.getName());
//			listTimes.add(second+"s");
	}
	private void deleteListRecord(boolean isAddLastRecord){
		for(int i=0;i<lists.size();i++){
			File file=new File((String) lists.get(i));
			if(file.exists()){
				file.delete();
			}
		}
		//正在暂停后，继续录音的这一段音频文件
		if(isAddLastRecord){
			myRecAudioFile.delete();
		}
	}
	/**
	 * 获取目录下的所有音频文件
	 */
	private void getRecordFiles() {
		// TODO Auto-generated method stub
		recordFiles = new ArrayList<String>();
		recordFile = new ArrayList<Item>();
		if (sdcardExit) {
			File files[] = myRecAudioDir.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().indexOf(".") >= 0) { // 只取.amr 文件
						String fileS = files[i].getName().substring(
								files[i].getName().indexOf("."));
						if (fileS.toLowerCase().equals(".mp3")
								|| fileS.toLowerCase().equals(".amr")
								|| fileS.toLowerCase().equals(".mp4"))
							recordFiles.add(files[i].getName());
							try {
								mediaPlayer.setDataSource(files[i].getAbsolutePath());
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
						recordFile.add(new Item(files[i].getName(), mediaPlayer.getDuration()+""));
					}
				}
			}
		}

	}
}
