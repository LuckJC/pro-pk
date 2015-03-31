package sim.android.mtkcit;

import java.io.File;
import java.util.ArrayList;

import sim.android.mtkcit.testitem.*;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageParser.Component;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import sim.android.mtkcit.cittools.CITTools;

//import com.mediatek.featureoption.FeatureOption;
import com.mediatek.common.featureoption.FeatureOption;
import com.android.internal.telephony.PhoneConstants;

public class ItemTestActivity extends ListActivity {

	private ArrayList<String> Items = new ArrayList<String>();
	private String[] AcitItemStr = null;
	CITTools ct;
	public static final String ITEM_TEST_TYPE = "item_test_type";
	public static int item_test_type = -1;
	private MediaPlayer mp;
	private BroadcastReceiver mBatteryInfoReceiver;
	private Vibrator vibrator;
	private String as[];
	private AlertDialog mDialog = null;

	boolean m_bBatteryTesting;
	boolean m_bTemperature;
	AudioManager am;
	String batteryStatus;
	int nCurrentMusicVolume;
	int bUsbDebug;
	private PowerManager.WakeLock mWakeLock;
	private KeyguardLock mLock;
	private NotificationManager nm;
	private final String WIFI_CITTEST = "wifi_bt_cittest";

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		CommonTest(as[position]);
		super.onListItemClick(l, v, position, id);
	}

	@Override
	protected void onCreate(Bundle bundle) {

		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"SimpleTimer");

		KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		mLock = mKeyGuardManager.newKeyguardLock("MachineItems");
		// /mWakeLock.acquire();

		mLock.disableKeyguard();
		super.onCreate(bundle);
		setContentView(R.layout.main);
		getListItems();
		ct = CITTools.getInstance(this);

		m_bBatteryTesting = false;
		m_bTemperature = false;
		batteryStatus = "";
		mBatteryInfoReceiver = new BatteryReceiver();
		am = (AudioManager) getSystemService("audio");
		bUsbDebug = Settings.Secure.getInt(getContentResolver(),
				Settings.Secure.ADB_ENABLED, 0);
		if (bUsbDebug == 0)
			Settings.Secure.putInt(getContentResolver(),
					Settings.Secure.ADB_ENABLED, 1);
	}

	private void getListItems() {
		item_test_type = this.getIntent().getIntExtra(ITEM_TEST_TYPE, 6);
		if (item_test_type == CITBroadcastReceiver.TEST_TYPE_MACHINE_ITEMS_TEST) {
			as = getResources().getStringArray(R.array.MachineItemStrings);

		} else if (item_test_type == CITBroadcastReceiver.TEST_TYPE_PCBA_ITEMS_TEST) {
			as = getResources().getStringArray(R.array.PCBAItemStrings);
		}

		int j = 0;
		do {
			if (j < as.length) {
				Items.add(as[j]);
				j++;
			} else {
				setListAdapter(new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, Items));
				getListView().setTextFilterEnabled(true);
				return;
			}
		} while (true);
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
//		ct.afterBluetoothtest();
//		ct.afterWifitest();
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStart() {
		getWindow().setType(WindowManager.LayoutParams.TYPE_BASE_APPLICATION);
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		if (mp != null) {
			if (mp.isPlaying()) {
				mp.stop();
				mp.release();
				mp = null;
			}
		}
		if (vibrator != null) {
			vibrator.cancel();
		}
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		if (mLock != null) {
			mLock.reenableKeyguard();
			mLock = null;
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (bUsbDebug == 0)
			Settings.Secure.putInt(getContentResolver(),
					Settings.Secure.ADB_ENABLED, 0);
		// mWakeLock.release();
		super.onDestroy();
	}

	private void speakandStorageTest() {
		String pathMusic;

		if (mp != null) {
			if (mp.isPlaying()) {
				mp.stop();
				mp.release();
				mp = null;
			}
		}
		// mod_s wangkang 2012.4.24 bug189
		ct.getSDPaths();

		// SDCard1 test
		// Unnecessary!
		if (ct.emmc) {
			// if(false) {
			if (!ct.checkSDCardMount(ct.SD2Path)) {
				Toast.makeText(this, R.string.no_sd, 1).show();

			} else {
				pathMusic = ct.SD2Path + "/CIT/test.mp3";
				File f = new File(pathMusic);
				if (!f.exists()) {
					android.app.AlertDialog.Builder builder = (new android.app.AlertDialog.Builder(
							this))
							.setTitle(R.string.speak_test_hasnot_sdcard2_file);
					String s1 = getString(R.string.no_nusic);
					android.app.AlertDialog.Builder builder1 = builder
							.setMessage(s1);
					mDialog = builder1.setPositiveButton(
							R.string.alert_dialog_ok, null).create();
					mDialog.show();
				} else {
					int i = am.getStreamMaxVolume(3);
					int j = am.getStreamVolume(3);
					nCurrentMusicVolume = j;
					am.setStreamVolume(3, i, 0);
					Uri u = Uri.parse(pathMusic);
					mp = MediaPlayer.create(this, u);
					android.app.AlertDialog.Builder builder = (new android.app.AlertDialog.Builder(
							this)).setTitle(R.string.test_item_speak);
					String s = getString(R.string.speak_test_message);
					android.app.AlertDialog.Builder builder1 = builder
							.setMessage(s);
					mDialog = builder1.setPositiveButton(
							R.string.alert_dialog_ok, new AudioLis()).create();
					mDialog.show();
					try {
						mp.setLooping(true);
						mp.start();
					} catch (Exception e) {
						return;
					}
				}
			}
		} else {
			if (!ct.checkSDCardMount(ct.SD1Path)) {
				Toast.makeText(this, R.string.no_sd, 1).show();

			} else {
				pathMusic = ct.SD1Path + "/CIT/test.mp3";
				File f = new File(pathMusic);
				if (!f.exists()) {
					android.app.AlertDialog.Builder builder = (new android.app.AlertDialog.Builder(
							this))
							.setTitle(R.string.speak_test_hasnot_sdcard2_file);
					String s1 = getString(R.string.no_nusic);
					android.app.AlertDialog.Builder builder1 = builder
							.setMessage(s1);
					mDialog = builder1.setPositiveButton(
							R.string.alert_dialog_ok, null).create();
					mDialog.show();
				} else {
					int i = am.getStreamMaxVolume(3);
					int j = am.getStreamVolume(3);
					nCurrentMusicVolume = j;
					am.setStreamVolume(3, i, 0);
					Uri u = Uri.parse(pathMusic);
					mp = MediaPlayer.create(this, u);
					android.app.AlertDialog.Builder builder = (new android.app.AlertDialog.Builder(
							this)).setTitle(R.string.test_item_speak);
					String s = getString(R.string.speak_test_message);
					android.app.AlertDialog.Builder builder1 = builder
							.setMessage(s);
					mDialog = builder1.setPositiveButton(
							R.string.alert_dialog_ok, new AudioLis()).create();
					mDialog.show();
					try {
						mp.setLooping(true);
						mp.start();
					} catch (Exception e) {
						return;
					}
				}
			}

		}
		/*
		 * if (Environment.getExternalStorageState().equals(
		 * android.os.Environment.MEDIA_REMOVED)) { // mp =
		 * MediaPlayer.create(this, R.raw.test); android.app.AlertDialog.Builder
		 * builder = (new android.app.AlertDialog.Builder(
		 * this)).setTitle(R.string.test_item_speak); String s =
		 * getString(R.string.speak_test_hasnot_sdcard);
		 * android.app.AlertDialog.Builder builder1 = builder.setMessage(s);
		 * mDialog = builder1 .setPositiveButton(R.string.alert_dialog_ok,
		 * null).create(); mDialog.show(); } else if
		 * (Environment.getExternalStorageState().equals(
		 * android.os.Environment.MEDIA_MOUNTED)) { pathMusic =
		 * Environment.getExternalStorageDirectory().toString() +
		 * "/CIT/test.mp3"; Log.v("fengxuanyang", "pathMusic~~~" + pathMusic);
		 * File f = new File(pathMusic); if (!f.exists()) { // mp =
		 * MediaPlayer.create(this, R.raw.test); android.app.AlertDialog.Builder
		 * builder = (new android.app.AlertDialog.Builder(
		 * this)).setTitle(R.string.test_item_speak); String s =
		 * getString(R.string.speak_test_hasnot_sdcard_file);
		 * android.app.AlertDialog.Builder builder1 = builder .setMessage(s);
		 * mDialog = builder1.setPositiveButton(R.string.alert_dialog_ok,
		 * null).create(); mDialog.show(); } else { int i =
		 * am.getStreamMaxVolume(3); int j = am.getStreamVolume(3);
		 * nCurrentMusicVolume = j; am.setStreamVolume(3, i, 0); Uri u =
		 * Uri.parse(pathMusic);
		 * 
		 * mp = MediaPlayer.create(this, u); if (mp == null) {
		 * Log.v("fengxuanyang", "null"); } else { Log.v("fengxuanyang",
		 * "not null"); } android.app.AlertDialog.Builder builder = (new
		 * android.app.AlertDialog.Builder(
		 * this)).setTitle(R.string.test_item_speak); String s =
		 * getString(R.string.speak_test_message);
		 * android.app.AlertDialog.Builder builder1 = builder .setMessage(s);
		 * mDialog = builder1.setPositiveButton(R.string.alert_dialog_ok, new
		 * AudioLis()).create(); mDialog.show(); mp.setLooping(true);
		 * mp.start(); } } else { return; }
		 */

	}

	private String getSimStateName(int simstate) {
		switch (simstate) {
		case TelephonyManager.SIM_STATE_UNKNOWN:
			return getString(R.string.test_sim_status_0);
		case TelephonyManager.SIM_STATE_ABSENT:
			return getString(R.string.test_sim_status_1);
		case TelephonyManager.SIM_STATE_PIN_REQUIRED:
			return getString(R.string.test_sim_status_2);
		case TelephonyManager.SIM_STATE_PUK_REQUIRED:
			return getString(R.string.test_sim_status_3);
		case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
			return getString(R.string.test_sim_status_4);
		case TelephonyManager.SIM_STATE_READY:
			return getString(R.string.test_sim_status_5);
		default:
			return "";
		}
	}

	private String getNetworkTypeName(int netType) {
		switch (netType) {
		case TelephonyManager.NETWORK_TYPE_GPRS:
			return "GPRS";
		case TelephonyManager.NETWORK_TYPE_EDGE:
			return "EDGE";
		case TelephonyManager.NETWORK_TYPE_UMTS:
			return "UMTS";
		case TelephonyManager.NETWORK_TYPE_HSDPA:
			return "HSDPA";
		case TelephonyManager.NETWORK_TYPE_HSUPA:
			return "HSUPA";
		case TelephonyManager.NETWORK_TYPE_HSPA:
			return "HSPA";
		case TelephonyManager.NETWORK_TYPE_CDMA:
			return "CDMA";
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
			return "CDMA - EvDo rev. 0";
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
			return "CDMA - EvDo rev. A";
		case TelephonyManager.NETWORK_TYPE_EVDO_B:
			return "CDMA - EvDo rev. B";
		case TelephonyManager.NETWORK_TYPE_1xRTT:
			return "CDMA - 1xRTT";
		default:
			return "UNKNOWN";
		}
	}

	private void SIMCardTest() {
		TelephonyManager tm = (TelephonyManager) (getSystemService(Context.TELEPHONY_SERVICE));
		int sim1state;
		int sim2state;
		int sim1Type;
		int sim2Type;
		String s = "";
//		FeatureOption.MTK_GEMINI_SUPPORT == true
//		if (false) {
////			tm.getSimState()
//			sim1state = tm.getSimStateGemini(PhoneConstants.GEMINI_SIM_1);
//			sim2state = tm.getSimStateGemini(PhoneConstants.GEMINI_SIM_2);
//			sim1Type = tm.getNetworkTypeGemini(PhoneConstants.GEMINI_SIM_1);
//			sim2Type = tm.getNetworkTypeGemini(PhoneConstants.GEMINI_SIM_2);
//
//			if (sim1state == TelephonyManager.SIM_STATE_UNKNOWN) {
//				s += "Sim1:\n" + getString(R.string.test_sim_status_0) + "\n";
//			} else if (sim1state == TelephonyManager.SIM_STATE_ABSENT) {
//				s += "Sim1:\n" + getString(R.string.test_sim_status_1) + "\n";
//			} else {
//				s += "Sim1: \n" // + getString(R.string.card_state)
//						+ getSimStateName(sim1state)
//						+ ","
//						+ getString(R.string.net_type)
//						+ getNetworkTypeName(sim1Type) + "\n";
//			}
//
//			if (sim2state == TelephonyManager.SIM_STATE_UNKNOWN) {
//				s += "Sim2:\n" + getString(R.string.test_sim_status_0);
//			} else if (sim2state == TelephonyManager.SIM_STATE_ABSENT) {
//				s += "Sim2:\n" + getString(R.string.test_sim_status_1);
//			} else {
//				s += "Sim2: \n" // + getString(R.string.card_state)
//						+ getSimStateName(sim2state)
//						+ ","
//						+ getString(R.string.net_type)
//						+ getNetworkTypeName(sim2Type);
//			}
//		} else {
			sim1state = tm.getSimState();
			sim1Type = tm.getNetworkType();

			if (sim1state == TelephonyManager.SIM_STATE_UNKNOWN) {
				s += "Sim:\n" + getString(R.string.test_sim_status_0);
			} else if (sim1state == TelephonyManager.SIM_STATE_ABSENT) {
				s += "Sim:\n" + getString(R.string.test_sim_status_1);
			} else {
				s += "Sim: \n" + getString(R.string.card_state)
						+ getSimStateName(sim1state) + ","
						+ getString(R.string.net_type)
						+ getNetworkTypeName(sim1Type);
			}
//		}

		AlertDialog.Builder builder = (new android.app.AlertDialog.Builder(this))
				.setTitle(R.string.test_item_simoruim).setMessage(s);
		builder.setPositiveButton(R.string.alert_dialog_ok, null).create()
				.show();
		return;
	}

	private void subCameraTest() {
		try {
			// Context otherAppsContext = null;
			// try {
			// otherAppsContext = createPackageContext("com.android.camera",
			// Context.CONTEXT_INCLUDE_CODE
			// | Context.CONTEXT_IGNORE_SECURITY);
			// Log.v("ItemTestActivity", "otherAppsContext ="
			// + otherAppsContext);
			//
			// } catch (Exception e) {
			// Log.e("ItemTestActivity", "otherAppsContext  not found");
			// }
			// SharedPreferences preferences = PreferenceManager
			// .getDefaultSharedPreferences(otherAppsContext);
			// Log.v("ItemTestActivity", "preferences =" + preferences);
			// Log.v("ItemTestActivity",
			// "mPreferences.contains = "
			// + preferences.contains("pref_camera_id_key"));
			//
			// Editor editor = preferences.edit();
			// editor.putString("pref_camera_id_key", Integer.toString(1));
			// editor.apply();
			// int cameraID = Integer.parseInt(preferences.getString(
			// "pref_camera_id_key", "5"));
			//
			// Log.v("ItemTestActivity", "cameraID =" + cameraID);

			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra("camerasensortype", 1);
			intent.putExtra("autofocus", true);
			intent.putExtra("fullScreen", false);
			intent.putExtra("showActionIcons", false);
			intent.putExtra("showActionIcons", false);
			intent.putExtra("cit_cameraid", 1);
			intent.putExtra("is_subcameratest",true);  //add by gongjh
			startActivityForResult(intent, 0);
		} catch (ActivityNotFoundException exception) {
			Log.d("CIT", "the camera activity is not exist");
			String s = getString(R.string.device_not_exist);
			AlertDialog.Builder builder = (new android.app.AlertDialog.Builder(
					this)).setTitle(R.string.test_item_camera).setMessage(s);
			builder.setPositiveButton(R.string.alert_dialog_ok, null).create()
					.show();
		}
	}

	private void powerTest() {
		AlertDialog.Builder builder = (new AlertDialog.Builder(this)).setTitle(
				R.string.test_item_power).setMessage(R.string.power_message);
		builder.setPositiveButton(R.string.alert_dialog_ok, null).create()
				.show();
	}

	private void LoopbackTest(boolean flag) {
		int i;
		String s1;
		boolean sp;
		android.app.AlertDialog.Builder builder;
		if (flag) {

			Intent intent = new Intent();
			intent.setClass(this, MikeTest.class);
			startActivity(intent);
		} else {
			AudioManager audiomanager = (AudioManager) getSystemService(AUDIO_SERVICE);
			if (!audiomanager.isWiredHeadsetOn()) {
				Toast warning = Toast.makeText(this,
						getString(R.string.headset_plug_warning),
						Toast.LENGTH_LONG);
				warning.setGravity(Gravity.CENTER, 0, 0);
				warning.show();
			} else {
				Intent intent = new Intent();
				intent.setClass(this, EarPhoneTest.class);
				startActivity(intent);
			}
		}
	}

	private class BatteryReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {

			Log.v("fengxuanyang", "package~~~~~~~~~~" + intent.getPackage()
					+ "intent.getAction()~~~~" + intent.getAction());
			String s = intent.getAction();
			if (m_bBatteryTesting) {
				m_bBatteryTesting = false;
				showChargeTestInfo(intent, s);
			}
			if (m_bTemperature) {
				m_bTemperature = false;
				showTemperatureInfo(intent, s);
			}
		}
	}

	private void showChargeTestInfo(Intent intent, String s) {
		int i = 0, j = 0, k = 0, current = 0;

		if ("android.intent.action.BATTERY_CHANGED".equals(s)) {
			i = intent.getIntExtra("plugged", 0);
			j = intent.getIntExtra("status", 1);
			k = intent.getIntExtra("temperature", 0);
			current = intent.getIntExtra("battery_current", 0);

			if (j == 2) {
				if (i > 0) {
					if (i == 1)
						batteryStatus = getString(R.string.battery_info_status_charging_ac);
					else
						batteryStatus = getString(R.string.battery_info_status_charging_usb);

				}
			} else if (j == 3) {
				batteryStatus = getString(R.string.battery_info_status_discharging);
			} else if (j == 4) {
				batteryStatus = getString(R.string.battery_info_status_notcharging);
			} else if (j == 5) {
				batteryStatus = getString(R.string.battery_info_status_full);
			} else {
				batteryStatus = getString(R.string.battery_info_status_unknow);
			}
		}
		if (j == 4) {
			Toast warning = Toast.makeText(this,
					getString(R.string.battery_info_status_notcharging),
					Toast.LENGTH_LONG);
			warning.setGravity(Gravity.CENTER, 0, 0);
			warning.show();
		} else {
			String s1 = batteryStatus;
			s1 += "\n";
			s1 += getString(R.string.charger_v) + current;
			if (current <= 200) {
				s1 += "(" + getString(R.string.abnormal_value) + ")";
			}
			// it's no need to judge temperature temporarily. keep these code
			/*
			 * s1 += "\n"; s1 += getString(R.string.test_temperature_info) + k;
			 * if(k > 45){ s1 += "("+getString(R.string.abnormal_value)+")"; }
			 */
			s1 += "\n\n";
			if (/* k > 45 || */current <= 200) {/*
												 * current <= 300 provided by
												 * driver
												 */
				s1 += getString(R.string.fail);
			} else {
				s1 += getString(R.string.success);
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setTitle(R.string.test_item_charge);
			builder.setMessage(s1);
			builder.setPositiveButton(R.string.alert_dialog_ok,
					new BatteryDilogListener()).create().show();
		}
	}

	private void showTemperatureInfo(Intent intent, String s) {
		if ("android.intent.action.BATTERY_CHANGED".equals(s)) {
			int i = intent.getIntExtra("temperature", 0);
			String s1 = (new StringBuilder())
					.append("The phone temperature is ").append(i).toString();

			AlertDialog.Builder builder = (new AlertDialog.Builder(this))
					.setTitle(R.string.test_temperature_title);
			StringBuilder stringbuilder = new StringBuilder();
			String s2 = getString(R.string.test_temperature_info);
			String s3 = stringbuilder.append(s2).append(i).toString();
			builder.setMessage(s3)
					.setPositiveButton(R.string.alert_dialog_ok, new TempList())
					.create().show();
		}
	}

	private void wifitest() {
		Intent intent = new Intent("android.intent.action.CIT_WIFI_TEST");
		Bundle mBundle = new Bundle();
		mBundle.putString(WIFI_CITTEST, WIFI_CITTEST);
		intent.putExtras(mBundle);
		startActivity(intent);
	}

	private void bluetoothtest() {
		ComponentName component = new ComponentName("com.android.settings",
				"com.android.settings.bluetooth.BluetoothSettings");
		Intent intent = new Intent();
		intent.setComponent(component);
		Bundle mBundle = new Bundle();
		mBundle.putString(WIFI_CITTEST, WIFI_CITTEST);
		intent.putExtras(mBundle);
		startActivity(intent);
	}

	private void temperatureTest() {
		m_bTemperature = true;
		BroadcastReceiver broadcastreceiver = mBatteryInfoReceiver;
		IntentFilter intentfilter = new IntentFilter(
				"android.intent.action.BATTERY_CHANGED");
		registerReceiver(broadcastreceiver, intentfilter);
	}

	private class BatteryDilogListener implements
			android.content.DialogInterface.OnClickListener {

		public void onClick(DialogInterface dialoginterface, int i) {
			unregisterReceiver(mBatteryInfoReceiver);
		}
	}

	private class TempList implements
			android.content.DialogInterface.OnClickListener {

		public void onClick(DialogInterface dialoginterface, int i) {
			unregisterReceiver(mBatteryInfoReceiver);
		}
	}

	private class LoopLis implements
			android.content.DialogInterface.OnClickListener {

		public void onClick(DialogInterface dialoginterface, int i) {
			am.setParameters("loopback=off");
		}
	}

	/*
	 * private class VibratorListener implements
	 * android.content.DialogInterface.OnClickListener { public void
	 * onClick(DialogInterface dialoginterface, int i) { ledflag = false;
	 * nm.cancelAll(); vibrator.cancel(); if(mWakeLock!=null) {
	 * mWakeLock.release(); } } }
	 */

	private class AudioLis implements
			android.content.DialogInterface.OnClickListener {

		public void onClick(DialogInterface dialoginterface, int i) {
			mp.stop();
			mp.release();
			mp = null;
			am.setStreamVolume(3, nCurrentMusicVolume, 0);
		}
	}

	/**
	 * <string-array name="CommonTestStrings"> <item>鎵０鍣ㄥ拰瀛樺偍锟?/item>
	 * <item>妫�祴UIM</item> <item>鐓х浉锟?/item> <item>鍏呯數娴嬭瘯</item> <item>鐫＄湢/鍞ら啋</item>
	 * <item>LED娴嬭瘯(绾紝锟?</item> <item>璇濈瓛鍥炶矾娴嬭瘯</item> <item>鑰虫満娴嬭瘯</item>
	 * <item>閿洏娴嬭瘯</item> <item>閲嶅姏浼犳劅锟?/item> <item>纾佸満鎰熷簲锟?/item>
	 * <item>WLAN娴嬭瘯</item> <item>钃濈墮娴嬭瘯</item> <item>LCD</item> <item>闂厜鐏祴锟?/item>
	 * <item>娓╁害</item> <item>GPS娴嬭瘯</item> <item>鏀堕煶锟?/item> <item>鎭㈠鍑哄巶璁剧疆</item>
	 * <item>鐗堟湰</item> <item>鍏変紶鎰熷櫒</item> <item>璺濈浼犳劅锟?/item>
	 * <item>SIM锟?瀛樺偍锟?鎸姩鍜屾壃澹板櫒</item> <item>鍓嶇疆鎽勫儚澶存祴锟?/item> <item>鐢靛瓙缃楃洏</item>
	 * <item>鎸囧崡锟?/item> <item>TP娴嬭瘯</item> <item>閿洏鐏祴锟?/item> <item>瑙﹀睆</item>
	 * <item>鑳屽厜娴嬭瘯</item> <item>REC娴嬭瘯</item> <item>鎸囧崡閽堟祴锟?/item> <item>鎸姩娴嬭瘯</item>
	 * <item>鎵０鍣ㄦ祴锟?/item> </string-array>
	 * 
	 * @param strAct
	 */
	private void CommonTest(String strAct/* , int type */) {
		int i = 0;
		Log.v("fengxuanyang", "commenTest____strAct=" + strAct);
		String ItemStr[] = getResources().getStringArray(
				R.array.CommonTestStrings);
		for (i = 0; i < ItemStr.length; i++) {
			if (strAct.equals(ItemStr[i])) {
				break;
			}
		}
		Intent intent = new Intent();
		switch (i) {
		case 0:
			speakandStorageTest();
			break;
		case 1:
			SIMCardTest();
			break;
		case 2:
			ct.cameraTest(this);
			break;
		case 3:
			// chargeTest();
			intent.setClass(this, ChargeTest.class);
			startActivity(intent);
			break;

		case 4:
			powerTest();
			break;
		case 5:
			intent.setClass(this, LEDVibraTest.class);
			startActivity(intent);
			break;
		case 6:
			LoopbackTest(true);
			break;
		case 7:
			LoopbackTest(false);
			break;
		case 8:
			intent.setClass(this, KeyTest.class);
			startActivity(intent);
			break;
		case 9:
			intent.setClass(this, MotionSenorTest.class);
			startActivity(intent);
			break;
		case 10:
			intent.setClass(this, MagneticTest.class);
			startActivity(intent);
			break;

		case 11:
			// wifitest();
//			ct.wifitest();
			intent.setClass(this, Wifirssitest.class);
			startActivity(intent);
			break;
		case 12:
			// bluetoothtest();
//			ct.bluetoothtest();
			intent.setClass(this, Bluetoothtest.class);
			startActivity(intent);
			break;
		case 13:
			intent.setClass(this, LCDTest.class);
			startActivity(intent);
			break;
		case 14:
			intent.setClass(this, LEDVibraTest.class);
			startActivity(intent);
			break;
			//intent.setClass(this, FlashTest.class);
			//startActivity(intent);
			//break;
		case 15:
			temperatureTest();
			break;
		case 16:
			intent.setClass(this, GPSTest.class);
			startActivity(intent);
			break;
		case 17:
//			intent.setClass(this, FMTest.class);
//			startActivity(intent);
			ct.FMTest();
			break;

		case 18:

			// intent.setClassName("com.android.settings",
			// "com.android.settings.MasterClear");
			intent.setClassName("com.android.settings",
					"com.android.settings.Settings");
			intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
					"com.android.settings.MasterClear");

			startActivity(intent);
			break;

		case 19:
			intent.setClass(this, Version.class);
			startActivity(intent);
			break;

		case 20:
			intent.setClass(this, LightSenor.class);
			startActivity(intent);
			break;

		case 21:
			intent.setClass(this, ProximitySenor.class);
			startActivity(intent);
			break;
		case 23:
			ct.subCameraTest(this);
			break;
		case 24:
			intent.setClass(this, CompassTest.class);
			startActivity(intent);
			break;

		case 26:
			intent.setClass(this, TouchTest.class);
			startActivity(intent);
			break;
		case 27:
			intent.setClass(this, KeylightTest.class);
			startActivity(intent);
			break;
		case 28:
			intent.setClass(this, MultiTouchActivity.class);
			startActivity(intent);
			break;
		case 29:
			intent.setClass(this, BrightnessTest.class);
			startActivity(intent);
			break;
		/**
		 * <item>REC娴嬭瘯</item> <item>鎸囧崡閽堟祴锟?/item> <item>鎸姩娴嬭瘯</item>
		 * <item>鎵０鍣ㄦ祴锟?/item>
		 * 
		 */
		case 30:
			intent.setClass(this, RECTest.class);
			startActivity(intent);
			break;
		case 31:
			intent.setClass(this, OrientationTest.class);
			startActivity(intent);
			break;
		case 32:
			intent.setClass(this, VibraTest.class);
			startActivity(intent);
			break;
		// case 33:
		// speakandStorageTest();
		// break;
		case 34:
			intent.setClass(this, HallTest.class);
			startActivity(intent);
			break;
		case 35:		// Twinkle Test
			intent.setClass(this, TwinkleTest.class);
			startActivity(intent);
			break;
		case 36:		// Dual MIC Test
			intent.setClass(this, DualMicTest.class);
			startActivity(intent);
			break;
		case 37:		// Dual Speaker Test
			intent.setClass(this, DualSpeakerTest.class);
			startActivity(intent);
			break;
		}
	}

	// @Override
	// public void onAttachedToWindow() {
	// getWindow().setType(WindowManager.LayoutParams.);
	// super.onAttachedToWindow();
	// }
}
