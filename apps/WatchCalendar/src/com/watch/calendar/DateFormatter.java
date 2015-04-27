package com.watch.calendar;


import java.util.Calendar;

import com.android.watch.calendar.util.CompeterCalendar;

import android.content.res.Resources;


public class DateFormatter {

	private Resources resources;

	public DateFormatter(Resources resources) {
		this.resources = resources;
	}

	private String getArrayString(int resid, int index) {
		return resources.getStringArray(resid)[index];
	}


}
