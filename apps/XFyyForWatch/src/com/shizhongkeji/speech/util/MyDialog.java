package com.shizhongkeji.speech.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;

public class MyDialog extends AlertDialog {

	public MyDialog(Context context, int theme) {
		super(context, theme);
		// TODO Auto-generated constructor stub
	}

	public MyDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		// TODO Auto-generated constructor stub
	}

	public MyDialog(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	/** {@inheritDoc} */
	 
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	
}
