package sim.android.mtkcit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import sim.android.mtkcit.R;

public class CITBroadcastReceiver extends BroadcastReceiver {

	private String TAG = "CITBroadcastReceiver";

	// the uri recives
	Uri citUri = Uri.parse("cit_secret_code://63863555");
	Uri deputyUri = Uri.parse("cit_secret_code://55");

	Uri SARUri = Uri.parse("cit_secret_code://07");		// add by puyong@20140917 for 查看SAR值

	// the main test item
	/**
	 * <item>PCBA测试</item> <item>整机测试</item> <item>版本</item> <item>电池信息</item>
	 */
	public static final int PCBA_TEST = 0;
	public static final int MACHINE_TEST = 1;
	public static final int VERSION_TEST = 2;
	public static final int BATTERY_INFO = 3;
	// the pcba test show on the screen
	/**
	 * the index should be Synchronous with she array <item>PCBA自动测试</item>
	 * <item>PCBA传感器自动测试</item> <item>PCBA单项测试</item>
	 */

	public static final int PCBA_AUTO_TEST = 0;
	public static final int PCBA_SENSOR_AUTO_TEST = 1;
	public static final int PCBA_ITEMS_TEST = 2;

	// the machine test show on the screen
	/**
	 * the index should be Synchronous with she array <item>整机自动测试</item>
	 * <item>整机传感器自动测试</item> <item>整机单项测试</item> <item>WBG测试</item> <item>Run
	 * In测试</item>
	 */
	public static final int MACHINE_AUTO_TEST = 0;
	public static final int MACHINE_SENSOR_AUTO_TEST = 1;
	public static final int MACHINE_ITEMS_TEST = 2;
	public static final int WBG_TEST = 3;
	public static final int RUNIN_TEST = 4;
	// if set true ,you can pass this test without test
	public static boolean PCBAPass = false;
	public static boolean MachinePass = true;

	public static int PCBA_FM_Frequency = 985;
	public static int Machine_FM_Frequency = 985;
	// the length of SN code
	public static int SN_Length = 17;
	// the length of CIT Test flag
	public static int CIT_Flag_Length = 4;
	// the location of test flag A and the test type
	public static final int TEST_TYPE_PCBA_AUTO = 0;
	public static final int TEST_TYPE_PCBA_SENSOR_AUTO = 1;
	public static final int TEST_TYPE_MACHINE_AUTO = 2;
	public static final int TEST_TYPE_MACHINE_SENSOR_AUTO = 3;
	public static final int TEST_TYPE_WBG_AUTO = 4;
	public static final int TEST_TYPE_DEPUTY = 5;

	// the item test
	public static final int TEST_TYPE_MACHINE_ITEMS_TEST = 6;
	public static final int TEST_TYPE_PCBA_ITEMS_TEST = 7;
	// for g-sensor
	public static final float G_SENSOR_CALI_THRESHOLD = 3;
	public static final boolean G_SENSOR_CALI = true;
	// for p-sensor
	public static final float P_SENSOR_CALI_THRESHOLD = 500;//5
	public static  boolean P_SENSOR_CALI = true;
	public static final String P_SENSOR_CALI_TYPE = "stk3171";

	public static final String[] P_SENSOR_TYPE_ALL = { "stk3171", "ap3216c" };
	public static int p_sensor_type = 0;
	// for l-sensor
	public static final float L_SENSOR_CALI_THRESHOLD = 20;
//for sn 
	public static boolean INSERT_SN = true;

	public CITBroadcastReceiver() {
		Log.v(TAG, "CITBroadcastReceiver");
		if (P_SENSOR_CALI_TYPE.equals("ap3216c")) {
			p_sensor_type = 2;
		} else if (P_SENSOR_CALI_TYPE.equals("stk3171")) {
			p_sensor_type = 2;
		}
		switch (CITBroadcastReceiver.p_sensor_type) {
		case 1:
		case 2:
			P_SENSOR_CALI = true;
			break;
		default:
			P_SENSOR_CALI = false;
			break;
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		try {
			Log.v(TAG, "onReceive");
			if (intent.getAction().equals("sim.android.cit")) {
				Uri uri = intent.getData();
				Intent i = new Intent(Intent.ACTION_MAIN);

				// add by puyong@20140917 for 查看SAR值
				if (uri.equals(SARUri)) 
				{
					i.setComponent(new ComponentName("sim.android.mtkcit","sim.android.mtkcit.testitem.SARValueDisplay"));
					i.putExtra(AutoTestActivity.AUTO_TEST_TYPE,CITBroadcastReceiver.TEST_TYPE_DEPUTY);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(i);
				}

				if (uri.equals(citUri)) {
					// i.setComponent(new ComponentName("sim.android.mtkcit",
					// "sim.android.mtkcit.DeputyTest"));
					//
					// Log.v(TAG, " deputyUri");
					//
					// } else {

					i.setComponent(new ComponentName("sim.android.mtkcit",
							"sim.android.mtkcit.CITActivity"));
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					context.startActivity(i);
				}
				if (uri.equals(deputyUri)) {
					i.setComponent(new ComponentName("sim.android.mtkcit",
							"sim.android.mtkcit.AutoTestActivity"));
					i.putExtra(AutoTestActivity.AUTO_TEST_TYPE,
							CITBroadcastReceiver.TEST_TYPE_DEPUTY);

					Log.v(TAG, " deputyUri");

					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					context.startActivity(i);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

}
