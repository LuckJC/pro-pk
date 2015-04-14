package com.example.hear_aid;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;


public class MainActivity extends Activity {
	private boolean isFirst = false; 
	private boolean isOpen = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Switch sh = (Switch) findViewById(R.id.switchs);
		SharedPreferences share = getSharedPreferences("AID",Activity.MODE_PRIVATE );
		isFirst = share.getBoolean("isFirst", true);
		final Editor edit = share.edit();
		if(isFirst){
			edit.putBoolean("isFirst", false);
			edit.putBoolean("isOpen", false);
			edit.commit();
		}
		isOpen = share.getBoolean("isOpen", false);
		sh.setChecked(isOpen);			
		
		sh.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Intent intent = new Intent(MainActivity.this,HearService.class);
				if (isChecked) {
					edit.putBoolean("isOpen", true);
					startService(intent);
				}else{
					edit.putBoolean("isOpen", false);
					stopService(intent);
				}
				edit.commit();
				
			}
		});
	}
}
