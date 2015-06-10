package com.example.xuntongwatch.main;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.LinearLayout.LayoutParams;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.adapter.MyGridViewAdapter;
import com.example.xuntongwatch.entity.ClassicImage;
import com.example.xuntongwatch.entity.GridViewItemImageView;
import com.example.xuntongwatch.util.Constant;

public class Edit_Head_Classic_Fragment extends Fragment {
	HorizontalScrollView horizontalScrollView;
	GridView gridView;
	private int NUM = 3; // 每行显示个数
	public static int imageWidth, imageHeight;
	private Context context;
	private ArrayList<GridViewItemImageView> list;
	public static Bitmap sClassicBitmap = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.edit_head_classic_head_fragment, container, false);
		context = getActivity();
		horizontalScrollView = (HorizontalScrollView) v
				.findViewById(R.id.edit_head_classic_head_fragment_scrollView);
		initWidthOrHeight();
		gridView = (GridView) v.findViewById(R.id.edit_head_classic_head_fragment_gridView1);
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				sClassicBitmap = convertViewToBitmap(view,imageWidth,imageHeight);
			}
		});
		horizontalScrollView.setHorizontalScrollBarEnabled(false);// 隐藏滚动条

	
		return v;

	}

	public void initArrayList() {
		list = new ArrayList<GridViewItemImageView>();
		Bitmap[] bitmaps = new Bitmap[] {
				BitmapFactory.decodeResource(getResources(), R.drawable.head01),
				BitmapFactory.decodeResource(getResources(), R.drawable.head02),
				BitmapFactory.decodeResource(getResources(), R.drawable.head03),
				BitmapFactory.decodeResource(getResources(), R.drawable.head04),
				BitmapFactory.decodeResource(getResources(), R.drawable.head05),
				BitmapFactory.decodeResource(getResources(), R.drawable.head06) };
		for (int i = 0; i < bitmaps.length; i++) {
			ClassicImage image = new ClassicImage();
			image.setBitmap(bitmaps[i]);
			list.add(image);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		initArrayList();
		setValue();
	}

	private void setValue() {
		MyGridViewAdapter adapter = new MyGridViewAdapter(context, list, imageWidth, imageHeight,
				MyGridViewAdapter.CLASSIC_IMAGE);
		gridView.setAdapter(adapter);

		LayoutParams params = new LayoutParams(adapter.getCount() * (imageWidth + 3) + 6,
				LayoutParams.WRAP_CONTENT);
		gridView.setLayoutParams(params);
		gridView.setBackgroundColor(Color.parseColor("#ffffff"));
		gridView.setColumnWidth(Constant.screenWidth / NUM);
		gridView.setStretchMode(GridView.NO_STRETCH);
		gridView.setNumColumns(adapter.getCount());
	}

	public void initWidthOrHeight() {
		imageWidth = Constant.screenWidth / 3 - 3;
		imageHeight = imageWidth;
	}

	   public static Bitmap convertViewToBitmap(View view, int bitmapWidth, int bitmapHeight){
	        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
	        view.draw(new Canvas(bitmap));
	        
	        return bitmap;
	    }
}
