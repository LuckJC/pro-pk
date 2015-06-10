package com.example.xuntongwatch.entity;

import java.io.Serializable;

public class Message_Thread implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int thread_id;//  短信会知的id
	
	private long date;	//最后一次短信的时间
	
	private int message_count;//短信总条数
	
	private String snippet;//最后一次短信的内容 
	
	private int read;//是否已读(0  未读   1 已读)
	
	private String name;//通讯录中的姓名
	
	private String phone;//电话号码
	
	private byte[] photo;//通讯录中的头像

	public int getThread_id() {
		return thread_id;
	}

	public void setThread_id(int thread_id) {
		this.thread_id = thread_id;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public int getMessage_count() {
		return message_count;
	}

	public void setMessage_count(int message_count) {
		this.message_count = message_count;
	}

	public String getSnippet() {
		return snippet;
	}

	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}

	public int getRead() {
		return read;
	}

	public void setRead(int read) {
		this.read = read;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public byte[] getPhoto() {
		return photo;
	}

	public void setPhoto(byte[] photo) {
		this.photo = photo;
	}

	public Message_Thread() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Message_Thread(int thread_id, long date, int message_count,
			String snippet, int read, String name, String phone, byte[] photo) {
		super();
		this.thread_id = thread_id;
		this.date = date;
		this.message_count = message_count;
		this.snippet = snippet;
		this.read = read;
		this.name = name;
		this.phone = phone;
		this.photo = photo;
	}

	
}
