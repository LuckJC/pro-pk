package com.android.settings;


import android.app.Fragment;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

public class Watch_UI_Fragment extends Fragment {

	private ImageView iv;
	 int[] images= {R.drawable.watch_ui_preview_1,R.drawable.watch_ui_preview_2,R.drawable.watch_ui_preview_3,
			 R.drawable.watch_ui_preview_4,R.drawable.watch_ui_preview_5,R.drawable.watch_ui_preview_6,
			 R.drawable.watch_ui_preview_7,R.drawable.watch_ui_preview_8};
	 
	/** {@inheritDoc} */
	 
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view = inflater.inflate(R.layout.watch_ui_preview, container,false);
			ViewPager vp = (ViewPager)view.findViewById(R.id.viewPager);
			vp.setAdapter(new myAdapter());
			
		return view;
	}
	
	public class myAdapter extends PagerAdapter{

		public  final Uri WATCH_CONTENT_URI = 
				Uri.parse("content://" + "com.szkj.watch.launcher" + "/watch");
		public  final String COLUMN_CURRENT_STYLE = "current_style";
		
		private void updateWatchStyle(int style) {
			ContentValues values = new ContentValues();
			values.put(COLUMN_CURRENT_STYLE, style);
			getActivity().getContentResolver().update(WATCH_CONTENT_URI, values, null, null);
		}

		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return images.length;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			// TODO Auto-generated method stub
			return arg0 == arg1;
		}

		/** {@inheritDoc} */
		 
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			// TODO Auto-generated method stub
			((ViewPager)container).removeView((View) object);
		}

		/** {@inheritDoc} */
		 
		@Override
		public Object instantiateItem(ViewGroup container, final int position) {
			// TODO Auto-generated method stub
				
				  
				iv = new ImageView(getActivity());
				iv.setBackgroundResource(images[position]);
				iv.setOnClickListener(new OnClickListener() {
					
					int aaa=position;
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						updateWatchStyle(position);
						Toast.makeText(getActivity(), position+"", 1000).show();
						getActivity().finish();
					}
				});
				((ViewPager) container).addView(iv,0);
				
				
				return iv;
				}
				
			
			
		}
		
		
	}

