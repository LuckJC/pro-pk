package com.infocomiot.watch.launcher.clock;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.Toast;

import com.infocomiot.watch.launcher.R;
import com.infocomiot.watch.launcher.launcher.LauncherActivity;
import com.infocomiot.watch.launcher.launcher.MyApplication;
import com.infocomiot.watch.launcher.provider.LauncherConfig;
import com.infocomiot.watch.launcher.provider.WatchConfig;
import com.infocomiot.watch.launcher.speechsound.SpeechsoudService;
import com.infocomiot.watch.launcher.speechsound.SpeechsoudService.MyBinder;

public class ClockActivity extends Activity {
	private static final Class<? extends ClockFragment>[] CLOCKS;
	static {
		CLOCKS = new Class[8];
		CLOCKS[0] = AnalogClockFragment1.class;
		CLOCKS[1] = AnalogClockFragment2.class;
		CLOCKS[2] = AnalogClockFragment3.class;
		CLOCKS[3] = AnalogClockFragment4.class;
		CLOCKS[4] = AnalogClockFragment5.class;
		CLOCKS[5] = AnalogClockFragment6.class;
		CLOCKS[6] = DigitClockFragment21.class;
		CLOCKS[7] = DigitClockFragment22.class;
	}
	
	
	private static final String[] PROJECTION = {
		WatchConfig._ID,
		WatchConfig.CURRENT_STYLE
	};
	
	private GestureDetector mGestureDetector;
	
	private int mCurrentStyle = -1;
	private boolean mFirstStart = true; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clock);
		
		
		
		mGestureDetector = new GestureDetector(this, mSwipeGestureListener);
		
		
//		AnalogClockFragment1 fragment = new AnalogClockFragment1();
//		AnalogClockFragment2 fragment = new AnalogClockFragment2();
//		AnalogClockFragment3 fragment = new AnalogClockFragment3();
//		AnalogClockFragment4 fragment = new AnalogClockFragment4();
//		AnalogClockFragment5 fragment = new AnalogClockFragment5();
//		AnalogClockFragment6 fragment = new AnalogClockFragment6();
//		DigitClockFragment21 fragment = new DigitClockFragment21();
//		DigitClockFragment22 fragment = new DigitClockFragment22();
//		getFragmentManager().beginTransaction().replace(R.id.root, fragment).commit();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		boolean created = false;
		Cursor cursor = getContentResolver().query(
				WatchConfig.WATCH_CONTENT_URI, PROJECTION, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			int currentStyle = cursor.getInt(1);
			if (currentStyle > 0 && currentStyle < CLOCKS.length && currentStyle != mCurrentStyle) {
				createClock(currentStyle);
				created = true;
			}
		}
		if (cursor != null) {
			cursor.close();
		}
		if (!created && mFirstStart) {
			createClock(0);
		}
		
		mFirstStart = false;
	}
	
	private void createClock(int currentStyle) {
		mCurrentStyle = currentStyle;
		try {
			ClockFragment fragment = CLOCKS[currentStyle].newInstance();
			getFragmentManager().beginTransaction().replace(R.id.root, fragment).commit();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean ret = mGestureDetector.onTouchEvent(ev); 
		if (!ret) {			
			return super.dispatchTouchEvent(ev);
		}
		
		return ret;
	}
	
	private SimpleOnGestureListener mSwipeGestureListener = new SimpleOnGestureListener() {
		private static final int SWIPE_MIN_DISTANCE = 120;
		private static final int SWIPE_THRESHOLD_VELOCITY = 100;	
		
		
		 
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			float xDistance = e2.getX() - e1.getX();
			float yDistance = e2.getY() - e1.getY();
			float abs_xDistance = Math.abs(xDistance);
			float abs_yDistance = Math.abs(yDistance);
			
			//上下或左右滑动超过一定距离
			if ((abs_xDistance > SWIPE_MIN_DISTANCE 
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
				|| (abs_yDistance > SWIPE_MIN_DISTANCE 
					&& Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY)) {
				if (abs_xDistance > abs_yDistance) {
					if (xDistance < 0) {
						//向左滑动：显示右边内容
						//Toast.makeText(LockScreenActivity.this, "从右到左", Toast.LENGTH_SHORT).show();
						//changeToNextTheme();
						gotoLauncher(LauncherConfig.TYPE_COMMON);
					} else {
						//向右滑动：显示左边内容
						//Toast.makeText(LockScreenActivity.this, "从左到右", Toast.LENGTH_SHORT).show();
						//changeToPreviousTheme();
						gotoLauncher(LauncherConfig.TYPE_COMMUNICATIONS);
					}
				} else {
					if (yDistance < 0) {
						//向上滑动：显示下面内容
						//Toast.makeText(LockScreenActivity.this, "从下到上", Toast.LENGTH_SHORT).show();
						gotoLauncher(LauncherConfig.TYPE_SETTINGS);
					} else {
						//向下滑动：显示上面内容
						//Toast.makeText(LockScreenActivity.this, "从上到下", Toast.LENGTH_SHORT).show();
						gotoLauncher(LauncherConfig.TYPE_SPORTS);
					}
				}
				
				return true;
			}

			return super.onFling(e1, e2, velocityX, velocityY);
		}
	};
	
	private void gotoLauncher(int type) {
		Intent intent;
		if (type == LauncherConfig.TYPE_SETTINGS) {
			//直接进入设置
			intent = new Intent(Settings.ACTION_SETTINGS);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		} else if (type == LauncherConfig.TYPE_COMMUNICATIONS) {
			intent = ClockFragment.getContactsIntent();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		} else {			
			intent = new Intent(this,LauncherActivity.class);
			intent.putExtra(LauncherConfig.EXTRA_WORKSPACE_TYPE, type);
		}
		
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onBackPressed() {
		//Do nothing
	}

	/** {@inheritDoc} */
	 
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.e(">>>>","onResume()");
//		Intent it=new Intent(this, SpeechsoudService.class);
//		bindService(it, connection, BIND_AUTO_CREATE);
	}

	/** {@inheritDoc} */
	 
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.e(">>>>","onDestroy()");
//		Intent it=new Intent(this, SpeechsoudService.class);
//		stopService(it);
	}

	/** {@inheritDoc} */
	 
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.e(">>>>","onPause()");
//		Intent it=new Intent(this, SpeechsoudService.class);
//		unbindService(connection);
	}
	
	
	// 创建一个 ServiceConnection 对象  
    final ServiceConnection connection = new ServiceConnection() { 
    	MyBinder mybinder;
    	SpeechsoudService shs;
    	@Override
        public void onServiceDisconnected(ComponentName name) {  
            // TODO Auto-generated method stub  
    		Log.i(">>>>>>>>", "onServiceDisconnected(ComponentName name)");
    		shs.stopListen();
        }  
  
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			
			mybinder = (MyBinder)service;
			shs = mybinder.getservice();
			shs.startListen();
			Log.i(">>>>>>>>", "onServiceConnected(ComponentName name, IBinder service)");
		}  
    }; 
}
