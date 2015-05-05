package com.android.settings.hear;

import com.android.settings.R;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class HearNullFragment extends Activity implements OnCheckedChangeListener {

	private boolean isFirst = false;
	private boolean isOpen = false;
	private Editor edit = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hear_aid_main);
		Switch sh = (Switch) findViewById(R.id.switchs);
		SharedPreferences share = getSharedPreferences("AID", Activity.MODE_PRIVATE);
		isFirst = share.getBoolean("isFirst", true);
		edit = share.edit();
		if (isFirst) {
			edit.putBoolean("isFirst", false);
			edit.putBoolean("isOpen", false);
			edit.commit();
		}
		isOpen = share.getBoolean("isOpen", false);
		sh.setChecked(isOpen);
		sh.setOnCheckedChangeListener(this);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Intent intent = new Intent(this, HearService.class);
		if (isChecked) {
			edit.putBoolean("isOpen", true);
			startService(intent);
		} else {
			edit.putBoolean("isOpen", false);
			stopService(intent);
		}
		edit.commit();
	}

//	private ServiceConnection conn = new ServiceConnection() {
//
//		@Override
//		public void onServiceDisconnected(ComponentName name) {
//
//		}
//
//		@Override
//		public void onServiceConnected(ComponentName name, IBinder service) {
//
//		}
//	};
//
//	private void bindService() {
//		Intent intent = new Intent(HearNullFragment.this, HearService.class);
//		bindService(intent, conn, Context.BIND_AUTO_CREATE);
//	}
//
//	private void unBind() {
//		unbindService(conn);
//	}

}
