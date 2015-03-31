package com.example.xuntongwatch.entity;

import java.io.Serializable;

public class Contact extends GridViewItemImageView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int contact_id = -1;
	private String contact_phone;
	private String contact_name;
	private byte[] contact_head;
	private int rawContact_id;
	private String photo_uri;

	public Contact() {
		super();
	}

	public String getPhoto_uri() {
		return photo_uri;
	}

	public void setPhoto_uri(String photo_uri) {
		this.photo_uri = photo_uri;
	}

	public Contact(int contact_id, String contact_phone, String contact_name,
			byte[] contact_head, int rawContact_id, String photo_uri) {
		super();
		this.contact_id = contact_id;
		this.contact_phone = contact_phone;
		this.contact_name = contact_name;
		this.contact_head = contact_head;
		this.rawContact_id = rawContact_id;
		this.photo_uri = photo_uri;
	}

	public Contact(int contact_id, String contact_phone, String contact_name,
			byte[] contact_head,int rawContact_id) {
		super();
		this.contact_id = contact_id;
		this.contact_phone = contact_phone;
		this.contact_name = contact_name;
		this.contact_head = contact_head;
		this.rawContact_id = rawContact_id;
	}

	public int getRawContact_id() {
		return rawContact_id;
	}

	public void setRawContact_id(int rawContact_id) {
		this.rawContact_id = rawContact_id;
	}

	public int getContact_id() {
		return contact_id;
	}

	public void setContact_id(int contact_id) {
		this.contact_id = contact_id;
	}

	public String getContact_phone() {
		return contact_phone;
	}

	public void setContact_phone(String contact_phone) {
		this.contact_phone = contact_phone;
	}

	public String getContact_name() {
		return contact_name;
	}

	public void setContact_name(String contact_name) {
		this.contact_name = contact_name;
	}

	public byte[] getContact_head() {
		return contact_head;
	}

	public void setContact_head(byte[] contact_head) {
		this.contact_head = contact_head;
	}


}
