package com.example.xuntongwatch.util;

import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.example.xuntongwatch.data.DbUtil;
import com.example.xuntongwatch.entity.TelephoneAdress;

public class InitDatabase {

	private Context context;
	private ArrayList<TelephoneAdress> list;
	private DbUtil util;
	private Handler handler;
	private SharedPreferences pref;

	public InitDatabase(Context context) {
		this.context = context;
	}

	public void start() {
		pref = context.getSharedPreferences("myActivityName", 0);
		boolean b = pref.getBoolean("isFirstIn", true);
		if (!b) {
			Log.e("", "不是首次进入");
			return;
		}
		// 以下是 表示 首次进入时做的处理
		Log.e("", "首次进入");
		util = new DbUtil(context);
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 1:
					Toast.makeText(InitDatabase.this.context, "解析完成",
							Toast.LENGTH_SHORT).show();
					insertData();
					break;
				case 2:
					Toast.makeText(InitDatabase.this.context, "插入完成",
							Toast.LENGTH_SHORT).show();
					Editor editor = pref.edit();
					editor.putBoolean("isFirstIn", false);
					editor.commit();
					break;
				}
				super.handleMessage(msg);
			}
		};
		jiexiXML();
	}

	public void jiexiXML() {
		final XMLSax xmlSax = new XMLSax();
		new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream inputStream = null;
				try {
					inputStream = context.getResources().getAssets()
							.open("mobilelist.xml");
					list = xmlSax.getSAXPersons(inputStream);
					inputStream.close();
					Message msg = Message.obtain();
					msg.what = 1;
					handler.sendMessage(msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void insertData() {
		if (list == null) {
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				util.insetIntoTwo(list);
				Message msg = Message.obtain();
				msg.what = 2;
				handler.sendMessage(msg);
			}
		}).start();
	}

}
