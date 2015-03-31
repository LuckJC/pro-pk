package com.example.xuntongwatch.main;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.xuntongwatch.R;

public class Edit_Head_Custom_Fragment extends Fragment {

	private ImageView add;
	private Custom_interface face;

	public interface Custom_interface {
		public void addImage();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			face = (Custom_interface) activity;
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.edit_head_custom_head_fragment,
				container, false);
		add = (ImageView) v
				.findViewById(R.id.edit_head_custom_head_fragment_add);
		add.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				face.addImage();
			}
		});
		Log.e("", "3333333333333333333");
		return v;
	}

	public void setAddImageViewBitmap(Bitmap bitmap) {
		add.setImageBitmap(bitmap);
	}

}
