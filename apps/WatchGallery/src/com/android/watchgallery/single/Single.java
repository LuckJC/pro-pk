package com.android.watchgallery.single;

import com.android.watchgallery.MainActivity;
import com.android.watchgallery.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.ImageView;

public class Single extends Activity implements OnPageChangeListener {
	ViewPager viewPager;
	ImageView singledel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_image);
		viewPager = (ViewPager) this.findViewById(R.id.vPager);
		singledel = (ImageView) this.findViewById(R.id.singledel);
		viewPager.setAdapter(new MyAdater());

	}

	public class MyAdater extends PagerAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			// TODO Auto-generated method stub
			return arg0 == arg1;
		}

		@Override
		public Object instantiateItem(View container, int position) {
			//((ViewPager) container).addView(MainActivity.imageUrls[position], 0);
			return MainActivity.imageUrls[position];
		}

	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageSelected(int arg0) {
		// TODO Auto-generated method stub

	}

}
