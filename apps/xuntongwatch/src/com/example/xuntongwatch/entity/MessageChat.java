package com.example.xuntongwatch.entity;

public class MessageChat {

	public static final String SEND = "2";
	public static final String RECEIVER = "1";

	private String _id;
	private String type;
	private String msg_content;
	private long date;

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMsg_content() {
		return msg_content;
	}

	public void setMsg_content(String msg_content) {
		this.msg_content = msg_content;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public MessageChat(String _id, String type, String msg_content,
			long date) {
		super();
		this._id = _id;
		this.type = type;
		this.msg_content = msg_content;
		this.date = date;
	}

}
