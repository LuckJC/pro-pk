package com.android.watchgallery.single;

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MyFragmentAdapter extends FragmentPagerAdapter{
	ArrayList<Fragment> fragments;
	private final FragmentManager mFragmentManager;
	public MyFragmentAdapter(FragmentManager fm,ArrayList<Fragment> fragments) {
		super(fm);
		// TODO Auto-generated constructor stub
		this.mFragmentManager=fm;
		this.fragments=fragments;
	}

	@Override
	public Fragment getItem(int arg0) {
		// TODO Auto-generated method stub
		return fragments.get(arg0);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return fragments.size();
	}


}
