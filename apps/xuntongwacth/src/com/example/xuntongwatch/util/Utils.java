package com.example.xuntongwatch.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

public class Utils {

	public static final int YEAR = 0;
	public static final int MONTH = 1;
	public static final int DAY = 2;
	public static final int HOUR = 3;
	public static final int MINUTE = 4;
	public static final int SECOND = 5;
	public static final int WEEK = 6;
	private static long lastClickTime;
	public static Calendar c = Calendar.getInstance();

	/**
	 * 调用系统的拨打电话
	 * 
	 * @param context
	 * @param phone
	 */
	public static void call(Context context, String phone) {
		Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
		context.startActivity(intent);
	}

	public synchronized static boolean isFastClick() {
		long time = System.currentTimeMillis();
		if (time - lastClickTime < 500) {
			return true;
		}
		lastClickTime = time;
		return false;

	}

	public static int[] longDateToY_M_D_H_m_S(long date) {
		int[] i = new int[7];
		c.setTimeInMillis(date);
		i[YEAR] = c.get(Calendar.YEAR);
		i[MONTH] = c.get(Calendar.MONTH);
		i[DAY] = c.get(Calendar.DAY_OF_MONTH);
		i[HOUR] = c.get(Calendar.HOUR_OF_DAY);
		i[MINUTE] = c.get(Calendar.MINUTE);
		i[SECOND] = c.get(Calendar.SECOND);
		i[WEEK] = c.get(Calendar.DAY_OF_WEEK);
		return i;
	}

	public static void sendMessage(String msg, String phone) {
		SmsManager smsManager = SmsManager.getDefault();
		if (msg.length() > 70) {
			List<String> contents = smsManager.divideMessage(msg);
			for (String sms : contents) {
				smsManager.sendTextMessage(phone, null, sms, null, null);
			}
		} else {
			smsManager.sendTextMessage(phone, null, msg, null, null);
		}
	}

	public static String weekNumberToString(int week) {
		String str = "";
		switch (week) {
		case 1:
			str = "日";
			break;
		case 2:
			str = "一";
			break;
		case 3:
			str = "二";
			break;
		case 4:
			str = "三";
			break;
		case 5:
			str = "四";
			break;
		case 6:
			str = "五";
			break;
		case 7:
			str = "六";
			break;
		}
		return str;
	}

	public static String getDoubleInt(int i) {
		StringBuffer sb = new StringBuffer("");
		if (i < 10) {
			sb.append("0");
		}
		sb.append(i);
		return sb.toString();
	}

	@SuppressLint("SimpleDateFormat")
	public static String longTimeToString(long time) {
		Date date = new Date(time);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String ti = format.format(date); // 得到发送时间
		return ti;
	}

	/**
	 * 使用系统当前日期加以调整作为照片的名称?
	 * 
	 * @return
	 */
	@SuppressLint("SimpleDateFormat")
	public static String getPhotoFileName() {
		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
		return dateFormat.format(date) + ".jpg";
	}

	/**
	 * 保存图片存放到文件中
	 * 
	 * @param path
	 *            图片路径
	 * @param mActivity
	 * @param imageName
	 *            图片名字
	 * @param bitmap
	 *            图片
	 * @return String
	 */
	public static String saveBitmapToFile(String path, Activity mActivity, String imageName,
			Bitmap bitmap) {
		String bitmapPath = null;
		File file = null;
		String real_path = "";
		FileOutputStream fos = null;
		try {

			real_path = SdUtil.getInstance().getPackagePath(mActivity)
					+ (path != null && path.startsWith("/") ? path : "/" + path);

			file = new File(real_path);
			// file = new File(real_path, imageName);

			if (!file.exists()) {
				File file2 = new File(real_path + "/");
				file2.mkdirs();
			}

			String fileUri = file.getAbsoluteFile() + "/" + imageName;
			file = new File(fileUri);
			if (!file.exists()) {
				file.createNewFile();
			}

			if (SdUtil.getInstance().hasSDCard()) {
				fos = new FileOutputStream(file);
			} else {
				fos = mActivity.openFileOutput(imageName, Context.MODE_PRIVATE);
			}

			if (imageName != null && (imageName.contains(".png") || imageName.contains(".PNG"))) {
				bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
			} else {
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
			}
			fos.flush();
			if (fos != null) {
				fos.close();
			}

			Log.e("", "real_path   === " + real_path);
			Log.e("", "imageName   === " + imageName);
			bitmapPath = real_path + "/" + imageName;
			return bitmapPath;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public static boolean isMobilePhone(String phone) {
		Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$");
		Matcher m = p.matcher(phone);
		return m.matches();
	}

}
