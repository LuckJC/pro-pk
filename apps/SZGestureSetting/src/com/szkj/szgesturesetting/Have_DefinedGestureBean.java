package com.szkj.szgesturesetting;

/**
 * 
 * <br>类描述:已经定义的手势
 * <br>功能详细描述:
 * 
 * @author  lixianda
 * @date  [2015-5-9]
 */
public class Have_DefinedGestureBean {
	int gesture_id;
	String gesture_style;
	int ismodify;
	int function_id;
	String function_name;
	String people_name;
	String phone_number;
	
	public Have_DefinedGestureBean()
	{
		
	}
	/**
	 * gesture_id手势id，gesture_style手势样式，ismodify手势是否可以修改，function_id功能名id，function_name功能名称，people_name联系人名字，phone_number电话号码
	 *    除了联系人名字跟手机号码可以为空  其他都不为空
	 */
	public Have_DefinedGestureBean(int gesture_id,String gesture_style,int ismodify,int function_id,String function_name,String people_name,String phone_number)
	{
		this.gesture_id = gesture_id;
		this.gesture_style = gesture_style;
		this.ismodify = ismodify;
		this.function_id = function_id;
		this.function_name = function_name;
		this.people_name = people_name;
		this.phone_number = phone_number;
	}
	
	/**
	 * @return 返回 gesture_id
	 */
	public int getGesture_id() {
		return gesture_id;
	}
	/**
	 * @param 对gesture_id进行赋值
	 */
	public void setGesture_id(int gesture_id) {
		this.gesture_id = gesture_id;
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
	/**
	 * @return 返回 function_id
	 */
	public int getFunction_id() {
		return function_id;
	}
	/**
	 * @param 对function_id进行赋值
	 */
	public void setFunction_id(int function_id) {
		this.function_id = function_id;
	}
	/**
	 * @return 返回 function_name
	 */
	public String getFunction_name() {
		return function_name;
	}
	/**
	 * @param 对function_name进行赋值
	 */
	public void setFunction_name(String function_name) {
		this.function_name = function_name;
	}
	/**
	 * @return 返回 people_name
	 */
	public String getPeople_name() {
		return people_name;
	}
	/**
	 * @param 对people_name进行赋值
	 */
	public void setPeople_name(String people_name) {
		this.people_name = people_name;
	}
	/**
	 * @return 返回 phone_number
	 */
	public String getPhone_number() {
		return phone_number;
	}
	/**
	 * @param 对phone_number进行赋值
	 */
	public void setPhone_number(String phone_number) {
		this.phone_number = phone_number;
	}
}
