package com.android.settings.hear;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.mediatek.xlog.Xlog;
public class HearEnabler implements OnCheckedChangeListener{

	private static final String TAG = "HearEnable";
	private Context mContext;
	private Switch mSwitch;
	private boolean isFirst = false; 
	private boolean isOpen = false;
	private Editor edit = null;
	public HearEnabler(Context mContext, Switch mSwitch) {
		super();
		this.mContext = mContext;
		this.mSwitch = mSwitch;
		init();
	}
	
	private void init(){
		SharedPreferences share = mContext.getSharedPreferences("AID",Activity.MODE_PRIVATE );
		isFirst = share.getBoolean("isFirst", true);
		edit = share.edit();
		if(isFirst){
			edit.putBoolean("isFirst", false);
			edit.putBoolean("isOpen", false);
			edit.commit();
		}
//		isOpen = share.getBoolean("isOpen", false);
//		Xlog.v(TAG, "init() " + isOpen);
//		mSwitch.setChecked(isOpen);
		mSwitch.setOnCheckedChangeListener(this);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Xlog.v(TAG,"onCheckedChanged:"+isChecked);
		Intent intent = new Intent(mContext.getApplicationContext(),HearService.class);
		if (isChecked) {
			edit.putBoolean("isOpen", true);
			mContext.startService(intent);
		}else{
			edit.putBoolean("isOpen", false);
			mContext.stopService(intent);
		}
		edit.commit();
		
	}
	public void setSwitch(Switch switch_){
		if (mSwitch == switch_){
			return;			
		}
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        SharedPreferences share = mContext.getSharedPreferences("AID",Activity.MODE_PRIVATE );
		isOpen = share.getBoolean("isOpen", false);
		mSwitch.setChecked(isOpen);
        mSwitch.setOnCheckedChangeListener(this);
        Xlog.v(TAG, "setSwitch() "+mSwitch.isChecked());
	}
	public void resume(){
		SharedPreferences share = mContext.getSharedPreferences("AID",Activity.MODE_PRIVATE );
		isOpen = share.getBoolean("isOpen", false);
		mSwitch.setChecked(isOpen);
		Xlog.v(TAG, "resume() " + isOpen);
		mSwitch.setOnCheckedChangeListener(this);
	}
	public void pause(){
		Xlog.v(TAG, "pause() " + isOpen);
		mSwitch.setOnCheckedChangeListener(null);
	}
}
