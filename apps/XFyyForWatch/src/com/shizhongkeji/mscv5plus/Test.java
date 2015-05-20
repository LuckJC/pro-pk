package com.shizhongkeji.mscv5plus;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

public class Test extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent(Test.this,Asr_service.class);
				startService(it);
			}
		});
		findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent it = new Intent(Test.this,Asr_service.class);
				stopService(it);
			}
		});
		
//		Intent it = new Intent(this,Asr_service.class);
//		startService(it);
		
	}


}
