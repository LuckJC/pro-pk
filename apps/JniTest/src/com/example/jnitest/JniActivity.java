package com.example.jnitest;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class JniActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jni);
		
		Log.v("JniTest", printJNI("I am JniActivity."));
	}
	
	static
	{
		System.loadLibrary("myjnitest");
	}
	
	private native String printJNI(String str);

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.jni, menu);
		return true;
	}

}
