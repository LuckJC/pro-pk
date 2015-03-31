package com.example.xuntongwatch.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.xuntongwatch.R;

public class Call_Activity extends BaseActivity implements OnClickListener{

	private Button one,two,three,four,five,six,seven,eight,nine,xin,jin,zero;
	private TextView tv;
	private RelativeLayout back,delete;
//	private ImageView back_iv,delete_iv;
	private Button call;
	private StringBuffer sb = new StringBuffer("");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.call);
		initButton(one,R.id.keyboad_bt_one);
		initButton(two,R.id.keyboad_bt_two);
		initButton(three,R.id.keyboad_bt_three);
		initButton(four,R.id.keyboad_bt_four);
		initButton(five,R.id.keyboad_bt_five);
		initButton(six,R.id.keyboad_bt_six);
		initButton(seven,R.id.keyboad_bt_seven);
		initButton(eight,R.id.keyboad_bt_eight);
		initButton(nine,R.id.keyboad_bt_nine);
		initButton(xin,R.id.keyboad_bt_xin);
		initButton(jin,R.id.keyboad_bt_jin);
		initButton(zero,R.id.keyboad_bt_zero);
		initButton(call, R.id.keyboad_bt_call);
		
//		back_iv = (ImageView) this.findViewById(R.id.keyboad_iv_back);
//		delete_iv = (ImageView) this.findViewById(R.id.keyboad_iv_delete);
		back = (RelativeLayout) this.findViewById(R.id.keyboad_rl_back);
		delete = (RelativeLayout) this.findViewById(R.id.keyboad_rl_delete);
		tv = (TextView) this.findViewById(R.id.keyboad_tv);
		tv.setEnabled(false);
		back.setOnClickListener(this);
		delete.setOnClickListener(this);
		delete.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if(sb.length() > 0)
				{
					sb.delete(0, sb.length());
					tv.setText("");
				}
				return true;
			}
		});
	}
	
	private void initButton(Button b,int id)
	{
		b = (Button) this.findViewById(id);
		b.setOnClickListener(this);
	}
	
	public void back()
	{
		finish();
	}
	
	private void addNumber(String str)
	{
		sb.append(str);
		tv.setText(sb.toString().trim());
	}
	private void deleteNumber()
	{
		if(sb.length() <= 0)
			return;
		sb.delete(sb.length()-1, sb.length());
		tv.setText(sb.toString().trim());
	}

	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.keyboad_bt_one:
			addNumber("1");
			break;
		case R.id.keyboad_bt_two:
			addNumber("2");
			break;
		case R.id.keyboad_bt_three:
			addNumber("3");
			break;
		case R.id.keyboad_bt_four:
			addNumber("4");
			break;
		case R.id.keyboad_bt_five:
			addNumber("5");
			break;
		case R.id.keyboad_bt_six:
			addNumber("6");
			break;
		case R.id.keyboad_bt_seven:
			addNumber("7");
			break;
		case R.id.keyboad_bt_eight:
			addNumber("8");
			break;
		case R.id.keyboad_bt_nine:
			addNumber("9");
			break;
		case R.id.keyboad_bt_xin:
			addNumber("*");
			break;
		case R.id.keyboad_bt_jin:
			addNumber("#");
			break;
		case R.id.keyboad_bt_zero:
			addNumber("0");
			break;
		case R.id.keyboad_bt_call://拨打电话 
			String phone = sb.toString().trim();
			Intent phoneIntent = new Intent("android.intent.action.CALL", Uri.parse("tel:"+ phone)); 
			this.startActivity(phoneIntent);
			break;
		case R.id.keyboad_rl_delete:
			deleteNumber();
			break;
		case R.id.keyboad_rl_back:
			this.finish();
			break;
		}
	}
}
