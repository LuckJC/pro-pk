package com.szkj.watch.launcher.clock;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.widget.TextView;

public abstract class AnalogClockFragment extends ClockFragment {
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd");
	protected AnalogClock mAnalogClock;
	protected TextView mDateTV;
	private int mYear, mMonth, mDate;
	
	@Override
	protected void doUpdateTime(int year, int month, int date, int hour,
			int minute, int second) {
		mAnalogClock.updateTime(hour, minute, second);
		updateDate(year, month, date);
	}
	
	/**
	 * Subclass can override this to update date(year, month, date)
	 * @param year
	 * @param month 1-12.
	 * @param date
	 */
	protected void updateDate(int year, int month, int date) {
		if (year != mYear || month != mMonth || date != mDate) {
			Date d = new Date(year - 1900, month - 1, date);
			mDateTV.setText(SDF.format(d));
			mYear = year;
			mMonth = month;
			mDate = date;
		}
	}
}
