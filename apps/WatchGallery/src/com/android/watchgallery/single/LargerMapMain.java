package com.android.watchgallery.single;

import java.util.ArrayList;

import com.android.watchgallery.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;

public class LargerMapMain extends FragmentActivity{
	MyFragmentAdapter myFragmentAdapter;
	ViewPager mPager;
	GalleryFragment calendarFragment;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		final ArrayList<Fragment> fragments = new ArrayList<Fragment>();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.large_main);
		fragments.add(new GalleryFragment());
		myFragmentAdapter = new MyFragmentAdapter(getSupportFragmentManager(),fragments);
		mPager=(ViewPager)this.findViewById(R.id.vPager);
		mPager.setAdapter(myFragmentAdapter);
	}
   

}
