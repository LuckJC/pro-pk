package com.android.watchgallery.single;

import com.android.watchgallery.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class GalleryFragment extends Fragment{
    ImageView imageView;
    ImageView imageView2;
	public static final String ARG_PAGE = "page";
	private int mPageNumber;
	private LargerMapMain mCalendar;
	private LayoutInflater inflater;
	MyAdapter myAdapter;
    GridView gridView;
    int gg[]={1,2,3,4};
    TextView textView;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mPageNumber = getArguments().getInt(ARG_PAGE);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView =inflater.inflate(
				R.layout.large_item, container, false);
//		gridView.findViewById(R.id.gridView1);
//		gridView.setAdapter(myAdapter);
		imageView=(ImageView) rootView.findViewById(R.id.imageView1);
		
		imageView2=(ImageView) rootView.findViewById(R.id.imageView2);
		return rootView;//super.onCreateView(inflater, container, savedInstanceState);
	}
	class MyAdapter extends BaseAdapter{
         TextView textView;

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return gg.length;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return gg[arg0];
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			//View view=getView(R.layout.calendar_item, arg1, arg2);
//			arg1 = inflater.inflate(R.layout.calendar_item, null);
//			textView=(TextView) arg1.findViewById(R.id.item);
//			textView.setText("1");
			return textView;
		}
		   
	   }

}
