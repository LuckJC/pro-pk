package com.android.settings;

import android.os.Bundle;
import android.app.Activity;

import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;


public class Watch_UI_Activity extends Activity {
//LinearLayout lin;
//Button btn;
Watch_UI_Fragment me;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//setContentView(new Watch_UI_Fragment());
		//lin = (LinearLayout)findViewById(R.id.line1);
		 getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			     WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// getActionBar().setDisplayHomeAsUpEnabled(true);
		 
		 setContentView(R.layout.watch_ui_activity_main);
		 
		me = new Watch_UI_Fragment();
		
				FragmentManager fragmentManager =getFragmentManager();
				                  FragmentTransaction transaction = fragmentManager.beginTransaction();
				 
				                 //步骤二：用add()方法加上Fragment的对象rightFragment 
				               
				                 transaction.add(R.id.line1, me);
				
				                //步骤三：调用commit()方法使得FragmentTransaction实例的改变生效
				                transaction.commit();  
			
		
	}

	
}
