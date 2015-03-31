package sim.android.mtkcit.testitem;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.SystemProperties;
import sim.android.mtkcit.CITBroadcastReceiver;
import sim.android.mtkcit.R;

import sim.android.mtkcit.cittools.CITTools;
import sim.android.mtkcit.cittools.CitBinder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.content.Context;
import com.android.internal.telephony.PhoneConstants;
//import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.common.featureoption.FeatureOption;


public class SARValueDisplay extends TestBase{
	int len = 17;
	TextView tv_Model, tv_software, tv_builddate, tv_hardware, tv_baseband, tv_sn;
	TextView tv_gsm_ft, tv_gsm_bt, tv_wcdma_bt, tv_wcdma_ft;

	private IBinder binder;
	private CitBinder citBinder;
	private static int citFlag;
	TextView tv_CitFlag, tv_Imei1, tv_Imei2;
	String imeiStr = null, imeiStr2 = null;

	private String TAG = "SARValueDisplay";
	private CITTools ct;

	private void initAllControl() {
		ct = CITTools.getInstance(this);

		tv_Model = (TextView) findViewById(R.id.sn_model);
		tv_software = (TextView) findViewById(R.id.software_title);
//		tv_builddate = (TextView) findViewById(R.id.build_date);
		tv_hardware = (TextView) findViewById(R.id.hardware_title);
		tv_baseband = (TextView) findViewById(R.id.baseband_version);
		tv_sn = (TextView) findViewById(R.id.sn_version);
		// BT FT TEST
		tv_gsm_bt = (TextView) findViewById(R.id.gsm_bt);
		tv_gsm_ft = (TextView) findViewById(R.id.gsm_ft);
		tv_wcdma_bt = (TextView) findViewById(R.id.wcdma_bt);
		tv_wcdma_ft = (TextView) findViewById(R.id.wcdma_ft);

		tv_CitFlag = (TextView) findViewById(R.id.cit_flag);
		tv_Imei1 = (TextView) findViewById(R.id.imei1);
		if(FeatureOption.MTK_GEMINI_SUPPORT)
		{
			tv_Imei2 = (TextView) findViewById(R.id.imei2);
		}


	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TelephonyManager teleMgr = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
		this.setContentView(R.layout.sar_value_display);

		initAllControl();
		//String customVerStr = getSystemproString("ro.custom.show.version");
		String customModel = getSystemproString("ro.product.model");

		String s = getString(R.string.sn_model);
		s += ":";
        s += customModel;
		tv_Model.setText(s);
		
		s = getString(R.string.sar_value_title);
		//s += ":\n";		
        //s += customVerStr;
		tv_software.setText(s);

		
		s = getString(R.string.permitted_sar_value_title);
		//s += ":\n";
    	//s += getSystemproString("ro.hardware.version");

		tv_hardware.setText(s);

		return;

	}


	// get the information use
	private static String getSystemproString(String property) {
		return SystemProperties.get(property, "unknown");
	}


	private String getSoftwareVersion() {
		String softwareTitle = getSystemproString("ro.build.display.id");
		String softwareTime = getSystemproString("ro.build.version.incremental");

		softwareTitle += "\n" + softwareTime;
		return softwareTitle;

	}

	/**
	 * get nvRam SNCit flag
	 */
	private int getCitBinder() {

		ct.CitBinderPrepare();
		citFlag = ct.getCitFlag();

		return citFlag;
	}

}
