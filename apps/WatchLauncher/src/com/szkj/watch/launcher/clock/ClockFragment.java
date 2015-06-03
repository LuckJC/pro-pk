package com.szkj.watch.launcher.clock;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.szkj.watch.launcher.R;

public abstract class ClockFragment extends Fragment {
	private static final int MSG_INBOX_CHANGED = 1;
	private static final int MSG_CALLLOG_CHANGED = 2;
	private static final int MSG_TIME_CHANGED = 3;
	
	public static final String BATTERY_CHARGING_ICON = "battery_charging_icon";
	public static final String BATTERY_LEVEL_ICON = "battery_level_icon";
	public static final String TIME_UPDATE_PERIOD = "time_update_period";
	
	private boolean mHasNewMissedCalls;
	
	/**
	 * Time update period.
	 * For analog clock with second pointer and digit clock with second display: this should be 1000.
	 */
	public static int MILLIS_PERIOD_SECOND = 1000;
	
	/**
	 * Time update period.
	 * For analog clock without second pointer and digit clock without second display: this should be 60*1000.
	 */
	public static int MILLIS_PERIOD_MINUTE = 60 * 1000;
	
	
	private int mTimeUpdatePeriod;
	private Drawable mBatteryCharingDrawable;
	private Drawable mBatteryLevelDrawable;
	private int mBatteryLevel;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle arguments = getArguments();
		int batteryChargingRes = arguments.getInt(BATTERY_CHARGING_ICON, R.drawable.battery01_charging);
		int batteryLevelRes = arguments.getInt(BATTERY_LEVEL_ICON, R.drawable.battery01);
		mBatteryCharingDrawable = getResources().getDrawable(batteryChargingRes);
		mBatteryLevelDrawable = getResources().getDrawable(batteryLevelRes);

		mTimeUpdatePeriod = arguments.getInt(TIME_UPDATE_PERIOD, -1);
		if (mTimeUpdatePeriod != MILLIS_PERIOD_MINUTE
				&& mTimeUpdatePeriod != MILLIS_PERIOD_SECOND) {
			mTimeUpdatePeriod = MILLIS_PERIOD_MINUTE;
		}
	}
	
	private Handler mHandler = new Handler(new Handler.Callback() {	
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_INBOX_CHANGED:
				updateNewSms();
				break;
				
			case MSG_CALLLOG_CHANGED:
				updateMissedCall();
				break;
				
			case MSG_TIME_CHANGED:
				updateTime();
				break;

			default:
				break;
			}
			
			return true;
		}
	});
	
	/**
	 * new sms observer
	 */
	private ContentObserver mNewSmsObserver = new NewSmsObserver(mHandler);
	/**
	 * new call observer
	 */
	private ContentObserver mMissedCallObserver = new NewMissedCallObserver(mHandler);
	
	//battery filter
	private static final IntentFilter sBatteryFilter = new IntentFilter();
	static {
		sBatteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
		sBatteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
		sBatteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
	}
	/**
	 * battery receiver
	 */
	private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateBattery(intent);
		}
	};
	
	//time observer
	private Timer mTimer;
	
	private TextView mNewSmsTV;
	private TextView mMissedCallTV;
	private ImageView mBatteryIV;
	private View mSmsView;
	private View mCallView;
	
	protected long mDelay = 1000, mPeriod = 1000;
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		updateTime();
		updateBattery(null);
		updateNewSms();
		updateMissedCall();
		
		ContentResolver resolver = getActivity().getContentResolver();
		//register observer to inbox for new sms
		resolver.registerContentObserver(Sms.Inbox.CONTENT_URI, true, mNewSmsObserver);
		//register observer to calllog for new missed call
		resolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, mMissedCallObserver);
		
		//register battery changed receiver
		getActivity().registerReceiver(mBatteryReceiver, sBatteryFilter);
		
		//schedule time update
		mTimer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				mHandler.sendEmptyMessage(MSG_TIME_CHANGED);
			}
		};
		long delay = 1000;
		if (mTimeUpdatePeriod == MILLIS_PERIOD_MINUTE) {
			//compute the interval between now and the next minute arrival.
			long now = System.currentTimeMillis();
			long minute = now / MILLIS_PERIOD_MINUTE;
			long nextMinute = minute + 1;
			long nextMinuteInMillis = nextMinute * MILLIS_PERIOD_MINUTE;
			delay = nextMinuteInMillis - now;
		}
		mTimer.schedule(task, delay, mTimeUpdatePeriod);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		ContentResolver resolver = getActivity().getContentResolver();
		resolver.unregisterContentObserver(mNewSmsObserver);
		resolver.unregisterContentObserver(mMissedCallObserver);
		
		getActivity().unregisterReceiver(mBatteryReceiver);
		
		mTimer.cancel();
	}
	
	private OnClickListener mSmsViewOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			//go to new sms
//			Toast.makeText(getActivity(), "sms click", Toast.LENGTH_SHORT).show();
			Intent intent = getSmsIntent();
			startApp(getActivity(), intent);
		}
	};
	
	private OnClickListener mCallViewOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			//go to call log
//			Toast.makeText(getActivity(), "call click", Toast.LENGTH_SHORT).show();
			Intent intent = getCallIntent();
			startApp(getActivity(), intent);
		}
	};
	
	/**
	 * update time
	 */
	private void updateTime() {
		Calendar calendar = Calendar.getInstance();
		doUpdateTime(calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DATE),
				calendar.get(Calendar.HOUR),
				calendar.get(Calendar.MINUTE), 
				calendar.get(Calendar.SECOND));
	}
	
	/**
	 * Subclass implement this method to update time display.
	 * @param year
	 * @param month
	 * @param date
	 * @param hour
	 * @param minute
	 * @param second
	 */
	protected abstract void doUpdateTime(int year, int month, int date, int hour, int minute, int second);
	
	/**
	 * update battery
	 */
	private void updateBattery(Intent intent) {
		if (intent == null) {
			return;
		}
		
		String action = intent.getAction();
		if (action == null) {
			return;
		}
		
		if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
			//Nothing to do. Note: this does not mean "charging" because maybe the battery is full.  
		} else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
			mBatteryIV.setImageDrawable(mBatteryLevelDrawable);
			mBatteryIV.setImageLevel(mBatteryLevel);
		} else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			mBatteryLevel = (int)((float)level / scale * 100);
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean charing = (status == BatteryManager.BATTERY_STATUS_CHARGING);
			if (charing) {
				mBatteryIV.setImageDrawable(mBatteryCharingDrawable);
			} else {
				mBatteryIV.setImageDrawable(mBatteryLevelDrawable);
				mBatteryIV.setImageLevel(mBatteryLevel);
			}
		}
	}
	
	private static final String[] NEW_SMS_PROJECTION = {Inbox._ID};
	/**
	 * update new sms(not seen)
	 * @return new sms count.
	 */
	protected int updateNewSms() {
		int count = 0;
		//query unseen sms count and display
		Cursor cursor = getActivity().getContentResolver().query(
				Inbox.CONTENT_URI, NEW_SMS_PROJECTION, 
				Inbox.SEEN + "=0", null, null);
		boolean hasNewSms = false;
		if (cursor != null && cursor.moveToFirst()) {
			count = cursor.getCount();
			if (count > 0) {
				hasNewSms = true;
				mNewSmsTV.setText(count <= 99 ? String.valueOf(count) : "99+");
			}
		}
		if (cursor != null) cursor.close();
		if (!hasNewSms) {
			mNewSmsTV.setText("");
		}
		
		return count;
	}
	
	private static final String[] NEW_MISSED_CALL_PROJECTION = {Calls._ID};
	/**
	 * update new missed call
	 * @return new missed call count.
	 */
	protected int updateMissedCall() {
		int count = 0;
		mHasNewMissedCalls = false;
		//query unseen missed call count and display
		Cursor cursor = getActivity().getContentResolver().query(
				CallLog.Calls.CONTENT_URI, NEW_MISSED_CALL_PROJECTION, 
				Calls.TYPE + "=? AND " + Calls.NEW + "=?", 
				new String[] {String.valueOf(Calls.MISSED_TYPE), "1"}, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			count = cursor.getCount();
			if (count > 0) {
				mHasNewMissedCalls = true;
				mMissedCallTV.setText(count <= 99 ? String.valueOf(count) : "99+");
			}
		}
		if (cursor != null) cursor.close();
		if (!mHasNewMissedCalls) {
			mMissedCallTV.setText("");
		}
		
		return count;
	}
	
	/**
	 * The subclass must call this method to set the common views for new sms, 
	 * missed call, and battery.
	 * @param batteryIV Battery imageview to set battery status icon.
	 * @param newSmsTV New sms indicator.
	 * @param smsView A view layout to click: go to sms.
	 * @param missedCallTV New missed call indicator
	 * @param callView A view layout to click: go to call.
	 */
	protected void setCommonView(ImageView batteryIV, TextView newSmsTV, View smsView,
			TextView missedCallTV, View callView) {
		mBatteryIV = batteryIV;
		mNewSmsTV = newSmsTV;
		mSmsView = smsView;
		mMissedCallTV = missedCallTV;
		mCallView = callView;
		mSmsView.setOnClickListener(mSmsViewOnClickListener);
		mCallView.setOnClickListener(mCallViewOnClickListener);
	}
	
	public static Bundle myArguments() {
		return null;
	}
	
	private class NewSmsObserver extends ContentObserver {
		public NewSmsObserver(Handler handler) {
			super(handler);
		}
	}
	
	private class NewMissedCallObserver extends ContentObserver {
		public NewMissedCallObserver(Handler handler) {
			super(handler);
		}
	}
	
	/**
	 * 获取联系人Intent
	 * @return 联系人Intent
	 */
	protected final static Intent getContactsIntent() {
		Intent intent = new Intent("com.example.xuntongwatch.main.Contact_Activity");
		return intent;
	}
	
	/**
	 * 获取信息Intent
	 * @return 信息Intent
	 */
	protected final static Intent getSmsIntent() {
		Intent intent = new Intent("com.example.xuntongwatch.main.Message_Activity");
		return intent;
	}
	
	/**
	 * 获取电话Intent
	 * @return 电话Intent
	 */
	protected Intent getCallIntent() {
		Intent intent;
		if (!mHasNewMissedCalls) {			
			intent = new Intent("com.example.xuntongwatch.main.Call_Activity");
		} else {
			intent = new Intent();
			intent.setClassName("com.example.xuntongwatch", "com.example.xuntongwatch.main.Record_Activity");
		}
//		Intent intent = new Intent(Intent.ACTION_DIAL);
//		intent.setData(Uri.parse("tel:*#3646633#"));
		return intent;
	}
	
	public static void startApp(Context context, Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
			Toast.makeText(context, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
		}
	}
}
