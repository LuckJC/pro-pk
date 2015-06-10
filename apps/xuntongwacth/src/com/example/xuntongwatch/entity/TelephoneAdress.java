package com.example.xuntongwatch.entity;

public class TelephoneAdress {

	private String telephone;// 手机前七位
	private String areaCode;// 区号
	private String city;// 城市
	private String cardType;// 手机卡类型如（湖南联通GSM卡）

	public TelephoneAdress() {
	}

	public TelephoneAdress(String telephone, String areaCode, String city,
			String cardType) {
		super();
		this.telephone = telephone;
		this.areaCode = areaCode;
		this.city = city;
		this.cardType = cardType;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public String getAreaCode() {
		return areaCode;
	}

	public void setAreaCode(String areaCode) {
		this.areaCode = areaCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCardType() {
		return cardType;
	}

	public void setCardType(String cardType) {
		this.cardType = cardType;
	}

}
