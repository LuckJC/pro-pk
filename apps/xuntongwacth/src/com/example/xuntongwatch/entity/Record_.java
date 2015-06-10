package com.example.xuntongwatch.entity;

public class Record_ {

	private int record_id = -1;
	private String record_phone = "";
	private long record_time = 0;
	private long record_when = 0;
	private String record_state = "";
	private String contact_head = "";
	private String contact_name = "";

	public Record_() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Record_(int record_id, String record_phone, long record_time,
			long record_when, String record_state, String contact_head,
			String contact_name) {
		super();
		this.record_id = record_id;
		this.record_phone = record_phone;
		this.record_time = record_time;
		this.record_when = record_when;
		this.record_state = record_state;
		this.contact_head = contact_head;
		this.contact_name = contact_name;
	}

	public String getContact_name() {
		return contact_name;
	}

	public void setContact_name(String contact_name) {
		this.contact_name = contact_name;
	}

	public int getRecord_id() {
		return record_id;
	}

	public void setRecord_id(int record_id) {
		this.record_id = record_id;
	}

	public String getRecord_phone() {
		return record_phone;
	}

	public void setRecord_phone(String record_phone) {
		this.record_phone = record_phone;
	}

	public long getRecord_time() {
		return record_time;
	}

	public void setRecord_time(long record_time) {
		this.record_time = record_time;
	}

	public long getRecord_when() {
		return record_when;
	}

	public void setRecord_when(long record_when) {
		this.record_when = record_when;
	}

	public String getRecord_state() {
		return record_state;
	}

	public void setRecord_state(String record_state) {
		this.record_state = record_state;
	}

	public String getContact_head() {
		return contact_head;
	}

	public void setContact_head(String contact_head) {
		this.contact_head = contact_head;
	}

}
