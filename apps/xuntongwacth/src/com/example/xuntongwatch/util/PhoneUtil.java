package com.example.xuntongwatch.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public class PhoneUtil {

	public static void callPhone(Context context, String phone_number) {
		if (!TextUtils.isEmpty(phone_number)) {
			phone_number = phone_number.trim();// 删除字符串首部和尾部的空格
			phone_number = phone_number.replace("+85", "");
			// 调用系统的拨号服务实现电话拨打功能
			// 封装一个拨打电话的intent，并且将电话号码包装成一个Uri对象传入
			Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"	+ phone_number));
			context.startActivity(intent);// 内部类
		}
	}

}
