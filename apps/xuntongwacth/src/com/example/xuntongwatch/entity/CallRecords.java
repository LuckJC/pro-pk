package com.example.xuntongwatch.entity;

import android.net.Uri;

public class CallRecords {

	private String name;// 姓名
	private int type;// 电话 的类型 有三种 1：未接，2：打入，3：打出
	private long date;// 打电话的时间
	private String phone;// 电话号码
	private String isSee;// 是否查看过
	private int duration;// 通话时间
	private String msg;// 信息 如 广东深圳 移动
	private Uri imageUri;

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Uri getImageUri() {
		return imageUri;
	}

	public void setImageUri(Uri imageUri) {
		this.imageUri = imageUri;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getIsSee() {
		return isSee;
	}

	public void setIsSee(String isSee) {
		this.isSee = isSee;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public CallRecords() {
	}

	public CallRecords(String name, int type, long date, String phone,
			String isSee, int duration, Uri imageUri) {
		super();
		this.name = name;
		this.type = type;
		this.date = date;
		this.phone = phone;
		this.isSee = isSee;
		this.duration = duration;
		this.imageUri = imageUri;
	}

}
