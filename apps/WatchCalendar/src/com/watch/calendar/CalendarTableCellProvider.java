package com.watch.calendar;


import java.util.Calendar;
import java.util.GregorianCalendar;

import com.android.watch.calendar.util.CompeterCalendar;
import com.example.calendar.R;

import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Camera.Parameters;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;


public class CalendarTableCellProvider {

	private long firstDayMillis = 0;
	private DateFormatter fomatter;
	
	public CalendarTableCellProvider(Resources resources, int monthIndex){
		int year = CompeterCalendar.getMinYear() + (monthIndex / 12);
		int month = monthIndex % 12;
		Calendar date = new GregorianCalendar(year, month, 1);
		int offset = 1 - date.get(Calendar.DAY_OF_WEEK);
		date.add(Calendar.DAY_OF_MONTH, offset);
		firstDayMillis = date.getTimeInMillis();
		fomatter = new DateFormatter(resources);
	}
	
	public View getView(int position, LayoutInflater inflater, ViewGroup container) {
		ViewGroup rootView;
		if(position%7==0){
			
		}
		CompeterCalendar date = new CompeterCalendar(firstDayMillis + 
				(position-1) * CompeterCalendar.DAY_MILLIS);
		// 开始日期处理
		boolean isFestival = false, isSolarTerm = false;
		rootView = (ViewGroup) inflater.inflate(R.layout.view_calendar_day_cell, container, false);
		
		TextView txtCellGregorian = (TextView)rootView.findViewById(R.id.gregorian);
		//TextView txtCellLunar = (TextView)rootView.findViewById(R.id.txtCellLunar);
		
		int gregorianDay = date.getGregorianDate(Calendar.DAY_OF_MONTH);
		// 判断是否为本月日期
		boolean isOutOfRange = ((position % 7 != 0) && 
				(position < 7 && gregorianDay > 7) || (position > 7 && gregorianDay < position - 7 - 6));
		txtCellGregorian.setText(String.valueOf(gregorianDay));
		Resources resources = container.getResources();
		if (isOutOfRange){
//			rootView.setBackgroundResource(R.drawable.selector_calendar_outrange);
			txtCellGregorian.setTextColor(resources.getColor(R.color.color_calendar_outrange));
		}
//		if (position % 7 == 0 || position % 7 == 6){ 
//			rootView.setBackgroundResource(R.drawable.selector_calendar_weekend);
//		}
		if (date.isToday()){
//			rootView.setBackgroundResource(R.drawable.shape_calendar_cell_today);
			((TextView)rootView.findViewById(R.id.gregorian)).getPaint().setFakeBoldText(true);
			((TextView)rootView.findViewById(R.id.gregorian)).setTextColor(Color.BLUE);
		}
		rootView.setTag(date);
		
		return rootView;
	}

}

