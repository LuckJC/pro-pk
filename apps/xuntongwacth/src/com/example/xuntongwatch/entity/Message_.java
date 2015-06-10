package com.example.xuntongwatch.entity;

import java.io.Serializable;

public class Message_ implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String SEE_HAS = "1";
	public static final String SEE_NONE = "2";
	public static final String RECEIVE = "1";
	public static final String SEND = "2";

	private int message_id = -1;;
	private String message_phone = "";
	private String message_content = "";
	private String message_see = "";
	private String message_state = "";
	private long message_time = -1l;
	private String contact_head = "";
	private String contact_name = "";
	private String message_send_ok;

	public Message_() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getMessage_send_ok() {
		return message_send_ok;
	}

	public void setMessage_send_ok(String message_send_ok) {
		this.message_send_ok = message_send_ok;
	}

	public Message_(int message_id, String message_phone,
			String message_content, String message_see, String message_state,
			long message_time, String contact_head, String contact_name,
			String message_send_ok) {
		super();
		this.message_id = message_id;
		this.message_phone = message_phone;
		this.message_content = message_content;
		this.message_see = message_see;
		this.message_state = message_state;
		this.message_time = message_time;
		this.contact_head = contact_head;
		this.contact_name = contact_name;
		this.message_send_ok = message_send_ok;
	}

	public String getContact_name() {
		return contact_name;
	}

	public void setContact_name(String contact_name) {
		this.contact_name = contact_name;
	}

	public int getMessage_id() {
		return message_id;
	}

	public void setMessage_id(int message_id) {
		this.message_id = message_id;
	}

	public String getMessage_phone() {
		return message_phone;
	}

	public void setMessage_phone(String message_phone) {
		this.message_phone = message_phone;
	}

	public String getMessage_content() {
		return message_content;
	}

	public void setMessage_content(String message_content) {
		this.message_content = message_content;
	}

	public String getMessage_see() {
		return message_see;
	}

	public void setMessage_see(String message_see) {
		this.message_see = message_see;
	}

	public String getMessage_state() {
		return message_state;
	}

	public void setMessage_state(String message_state) {
		this.message_state = message_state;
	}

	public long getMessage_time() {
		return message_time;
	}

	public void setMessage_time(long message_time) {
		this.message_time = message_time;
	}

	public String getContact_head() {
		return contact_head;
	}

	public void setContact_head(String contact_head) {
		this.contact_head = contact_head;
	}

}
