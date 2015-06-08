package com.szkj.watch.launcher.clock;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class DigitClockFragment extends ClockFragment {
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd EEEE");
	protected TextView mDateTV, mTimeTV;
	private int mYear, mMonth, mDate;
	
	@Override
	protected void doUpdateTime(int year, int month, int date, int hour,
			int minute, int second) {
		String hourStr = hour > 9 ? String.valueOf(hour) : ("0" + hour);
		String minuteStr = minute > 9 ? String.valueOf(minute) : ("0" + minute);
		mTimeTV.setText(hourStr + ":" + minuteStr);
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
	
	/**
	 * set the contacts view and a click event.
	 */
	protected void setContactsView(View view) {
		if (view != null) {
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = ClockFragment.getContactsIntent();
					ClockFragment.startApp(getActivity(), intent);
				}
			});
		}
	}
}
