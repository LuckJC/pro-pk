package com.szkj.watch.launcher.clock;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.widget.TextView;

public abstract class AnalogClockFragment extends ClockFragment {
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd");
	protected AnalogClock mAnalogClock;
	protected TextView mDateTV;
	private int mYear, mMonth, mDate, mDay;
	
	@Override
	protected void doUpdateTime(int year, int month, int date, int hour,
			int minute, int second, int day) {
		mAnalogClock.updateTime(hour, minute, second);
		updateDate(year, month, date, day);
	}
	
	/**
	 * Subclass can override this to update date(year, month, date)
	 * @param year
	 * @param month 1-12.
	 * @param date
	 * @param day day of week,1~7. 1:Sunday, 2:Monday...6:Friday,7:Saturday
	 */
	protected void updateDate(int year, int month, int date, int day) {
		if (year != mYear || month != mMonth || date != mDate || day != mDay) {
			Date d = new Date(year - 1900, month - 1, date);
			mDateTV.setText(SDF.format(d));
			mYear = year;
			mMonth = month;
			mDate = date;
			mDay = day;
		}
	}
}
