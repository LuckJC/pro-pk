package com.example.xuntongwatch.entity;

public class PhoneRecord {

	private int _id;//��ͨ����¼��Id������ɾ���ã�
	private String number;//�绰����
	private long date;//ʱ��
	private long duration;//ʱ��
	private int type;//����
	private int new_;//�Ƿ��Ѷ�
	private String name;//��ϵ������
	private String photo;//��ϵ��ͷ��
	
	public PhoneRecord() {
		
	}
	public PhoneRecord(int _id,String number, long date, long duration, int type,
			int new_, String name, String photo) {
		super();
		this._id = _id;
		this.number = number;
		this.date = date;
		this.duration = duration;
		this.type = type;
		this.new_ = new_;
		this.name = name;
		this.photo = photo;
	}
	
	public int get_id() {
		return _id;
	}
	public void set_id(int _id) {
		this._id = _id;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getNew_() {
		return new_;
	}
	public void setNew_(int new_) {
		this.new_ = new_;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPhoto() {
		return photo;
	}
	public void setPhoto(String photo) {
		this.photo = photo;
	}
	
}
