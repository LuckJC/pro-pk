package com.shizhongkeji.mscv5plus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class AsrMain extends Activity {

	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.asr_main);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	/** {@inheritDoc} */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Intent it = new Intent(this, Asr_service.class);
		it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startService(it);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();

		Intent it = new Intent(this, Asr_service.class);
		stopService(it);

	}

}
