package com.example.hear_aid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class Settings extends Activity {


	private RadioGroup mRadioGroup;
	private RadioButton mRadioFirst;
	private RadioButton mRadioSecond;
	private SharedPreferences mSharedPreferences;
	private Editor edit = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		initView();
		
	}

	private void initView() {
		mSharedPreferences = getSharedPreferences("status", Activity.MODE_PRIVATE);
		edit = mSharedPreferences.edit();
		mRadioGroup = (RadioGroup) findViewById(R.id.radiogroup);
		mRadioFirst = (RadioButton) findViewById(R.id.radiobutton_first);
		mRadioSecond = (RadioButton) findViewById(R.id.radiobutton_second);
		boolean isSecond = mSharedPreferences.getBoolean("Second", false);
		if (isSecond) {
			mRadioFirst.setChecked(false);
			mRadioSecond.setChecked(true);
		} else {
			mRadioFirst.setChecked(true);
			mRadioSecond.setChecked(false);
		}
		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				Intent intent = new Intent(Settings.this,HearService.class);
				switch (checkedId) {
				case R.id.radiobutton_first:
					mRadioFirst.setChecked(true);
					edit.putBoolean("Second", false);
					edit.commit();
					intent.putExtra("MSG", MainActivity.MSG_FIRST);
					startService(intent);
					break;
				case R.id.radiobutton_second:
					mRadioSecond.setChecked(true);
					edit.putBoolean("Second", true);
					edit.commit();
					intent.putExtra("MSG", MainActivity.MSG_SECOND);
					startService(intent);
					break;

				default:
					break;
				}

			}
		});
	}
}
