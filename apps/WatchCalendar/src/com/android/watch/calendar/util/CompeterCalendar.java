package com.android.watch.calendar.util;

import java.util.Calendar;
import java.util.Date;

import com.watch.calendar.MyCalendar;

public class CompeterCalendar {
	/** 一天的毫秒数	 */
	public static final long DAY_MILLIS = 24 * 60 * 60 * 1000;
	private static final int YEAR_MAX = 2100;
	private static final int YEAR_MIN = 1900;
	private Calendar gregorianDate;
	/**
	 * 支持范围最大年份
	 * @return
	 */
	public static int getMaxYear(){
		return YEAR_MAX;
	}
	
	/**
	 * 支持范围最小年份
	 * @return
	 */
	public static int getMinYear(){
		return YEAR_MIN + 1;
	}
	/**
	 * 返回公历信息<br/>
	 * 参照:{@link java.util.Calendar#get(int)}
	 * @param field
	 * @return
	 */
	public int getGregorianDate(int field){
		return gregorianDate.get(field);
	}
	/**
	 * @see Calendar#getTimeInMillis()
	 * @return
	 */
	public long getTimeInMillis(){
		return gregorianDate.getTimeInMillis();
	}
	public CompeterCalendar(){
		this(new Date());
	}
	
	public CompeterCalendar(Calendar date){
		this(date.getTimeInMillis());
	}
	
	
	public CompeterCalendar(Date date){
		this(date.getTime());
	}
	public CompeterCalendar(long milliSeconds){
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(milliSeconds);
		gregorianDate = date;
	}
	/**
	 * 是否为今天
	 * @return
	 */
	public boolean isToday(){
		Calendar today = Calendar.getInstance();
		if ((gregorianDate.get(Calendar.YEAR) == today.get(Calendar.YEAR))
				&& (gregorianDate.get(Calendar.MONTH) == today.get(Calendar.MONTH))
				&& (gregorianDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)))
		{
			return true;
		}
		
		return false;
	}
}
