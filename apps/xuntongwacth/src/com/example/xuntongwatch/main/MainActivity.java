package com.example.xuntongwatch.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.data.MessageDbUtil;
import com.example.xuntongwatch.util.Constant;
import com.example.xuntongwatch.util.InitDatabase;
import com.example.xuntongwatch.util.Utils;

public class MainActivity extends BaseActivity implements OnClickListener {

	private Button call, message, contact, record;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.watch_main);
		initConstant();
		new InitDatabase(this).start();
		call = (Button) this.findViewById(R.id.main_call);
		message = (Button) this.findViewById(R.id.main_message);
		contact = (Button) this.findViewById(R.id.main_contact);
		record = (Button) this.findViewById(R.id.main_record);
		call.setOnClickListener(this);
		message.setOnClickListener(this);
		contact.setOnClickListener(this);
		record.setOnClickListener(this);

		MessageDbUtil util = new MessageDbUtil(this);
		util.deleteMsg("13827477105");

	}

	@Override
	public void onClick(View v) {
		if (Utils.isFastClick()) {
			return;
		}
		switch (v.getId()) {
		case R.id.main_call:
			startMyActivity(Call_Activity.class);
			break;
		case R.id.main_message:
			startMyActivity(Message_Activity.class);
			break;
		case R.id.main_contact:
			startMyActivity(Contact_Activity.class);
			break;
		case R.id.main_record:
			startMyActivity(Record_Activity.class);
			break;
		}
	}

	private void startMyActivity(Class<?> c) {
		Intent intent = new Intent(this, c);
		startActivity(intent);
	}

	private void initConstant() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		Constant.screenWidth = dm.widthPixels;
		Constant.screenHeight = dm.heightPixels;
	}

}
