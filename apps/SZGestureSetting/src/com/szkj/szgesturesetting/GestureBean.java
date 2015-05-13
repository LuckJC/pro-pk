package com.szkj.szgesturesetting;

public class GestureBean {
	int gesdture_id;
	String gesture_style;
	int ismodify;
	
	public GestureBean()
	{
		
	}
	/**
	 * 手势构造函数
	 */
	public GestureBean(String style,int modify)
	{
		
		this.gesture_style=style;
		this.ismodify=modify;
	}
	
	/**
	 * @return 返回 gedture_id
	 */
	public int getGesdture_id() {
		return gesdture_id;
	}
	/**
	 * @param 对gedture_id进行赋值
	 */
	public void setGesdture_id(int gesdture_id) {
		this.gesdture_id = gesdture_id;
	}
	/**
	 * @return 返回 gesture_style
	 */
	public String getGesture_style() {
		return gesture_style;
	}
	/**
	 * @param 对gesture_style进行赋值
	 */
	public void setGesture_style(String gesture_style) {
		this.gesture_style = gesture_style;
	}
	/**
	 * @return 返回 ismodify
	 */
	public int getIsmodify() {
		return ismodify;
	}
	/**
	 * @param 对ismodify进行赋值
	 */
	public void setIsmodify(int ismodify) {
		this.ismodify = ismodify;
	}
}
