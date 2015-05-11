package com.watch.calendar;

import java.util.Calendar;

import com.android.watch.calendar.util.CompeterCalendar;
import com.android.watch.calendar.util.DateFormatter;
import com.example.calendar.R;

import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

public class MyCalendar extends FragmentActivity implements
OnDateSetListener, OnMenuItemClickListener, OnFocusChangeListener{
	private ViewPager mPager;
	private PagerAdapter mPagerAdapter;
	private View imgPreviousMonth, imgNextMonth;
	private DateFormatter formatter;

	private TextView showYear, showMouth;
	private int getTodayMonthIndex() {
		Calendar today = Calendar.getInstance();
		int offset = (today.get(Calendar.YEAR) - CompeterCalendar.getMinYear())
				* 12 + today.get(Calendar.MONTH);
		return offset;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
//		formatter = new DateFormatter(this.getResources());
//		txtTitleGregorian = (TextView) findViewById(R.id.gregorian);
//		txtTitleAddition = (TextView) findViewById(R.id.txtTitleAddition);
		showYear=(TextView) findViewById(R.id.showYear);
		showYear.getPaint().setFakeBoldText(true);
		showMouth=(TextView) findViewById(R.id.showMouth);
		showMouth.getPaint().setFakeBoldText(true);
		mPager = (ViewPager) findViewById(R.id.pager);

		mPagerAdapter = new CalendarPagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(mPagerAdapter);
		mPager.setOnPageChangeListener(new simplePageChangeListener());

		mPager.setCurrentItem(getTodayMonthIndex());
	}

	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onMenuItemClick(MenuItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onDateSet(DatePicker arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}
	// �·���ʾ�л��¼�
	private class simplePageChangeListener extends
			ViewPager.SimpleOnPageChangeListener {
		@Override
		public void onPageSelected(int position) {
			// set title year month
//			StringBuilder title = new StringBuilder();
//			title.append(CompeterCalendar.getMinYear() + (position / 12));
//			title.append('-');
//			int month = (position % 12) + 1;
//			if (month < 10) {
//				title.append('0');
//			}
//			title.append(month);
			
			showYear.setText(CompeterCalendar.getMinYear() + (position / 12)+"");
			
			showMouth.setText((position % 12) + 1+"月");
			
			
		//	txtTitleGregorian.setText(title);
		//	txtTitleLunar.setText("");
//			txtTitleAddition.setText("");

			// set related button's state
			/*if (position < mPagerAdapter.getCount() - 1
					&& !imgNextMonth.isEnabled()) {
				imgNextMonth.setEnabled(true);
			}
			if (position > 0 && !imgPreviousMonth.isEnabled()) {
				imgPreviousMonth.setEnabled(true);
			}*/
		}
	}

}
