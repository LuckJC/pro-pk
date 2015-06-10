package com.example.xuntongwatch.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.xuntongwatch.anima.ActivityJumpAnimation;

public class BaseActivity extends Activity {

	@Override
	public void finish() {
		super.finish();
		ActivityJumpAnimation.LeftBack(this);
	}

	@SuppressLint("NewApi")
	@Override
	public void startActivity(Intent intent, Bundle options) {
		super.startActivity(intent, options);
		ActivityJumpAnimation.RightInto(this);

	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		ActivityJumpAnimation.RightInto(this);
	}

}
