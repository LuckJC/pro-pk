package com.example.xuntongwatch.adapter;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.entity.ClassicImage;
import com.example.xuntongwatch.entity.Contact;
import com.example.xuntongwatch.entity.GridViewItemImageView;

public class MyGridViewAdapter extends BaseAdapter {
	private Context context;
	private int imageWidth, imageHeight;
	private ArrayList<GridViewItemImageView> list;
	private int state;

	public static final int CLASSIC_IMAGE = 0;
	public static final int CONTACT_IMAGE = 1;

	/**
	 * 
	 * @param context
	 * @param list
	 * @param imageWidth
	 * @param imageHeight
	 * @param state
	 *            传入参数 （1 CLASSIC_IMAGE ，CONTACT_IMAGE）
	 */
	public MyGridViewAdapter(Context context,
			ArrayList<GridViewItemImageView> list, int imageWidth,
			int imageHeight, int state) {
		this.context = context;
		this.list = list;
		this.state = state;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
	}
 
	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (null == convertView) {
			holder = new ViewHolder();
			convertView = LayoutInflater.from(context).inflate(R.layout.contact_item, null);
			holder.tv = (TextView) convertView.findViewById(R.id.contact_item_name);
			holder.iv = (ImageView) convertView.findViewById(R.id.contact_item_image);
			RelativeLayout.LayoutParams params = (LayoutParams) holder.iv.getLayoutParams();
			params.width = imageWidth;
			params.height = imageHeight;
			holder.iv.setLayoutParams(params);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		if (state == CLASSIC_IMAGE) {
			holder.tv.setVisibility(View.GONE);
			ClassicImage image = (ClassicImage) list.get(position);
			Bitmap bitmap = image.getBitmap();
			holder.iv.setImageBitmap(bitmap);
		} else if (state == CONTACT_IMAGE) {
			Contact contact = (Contact) list.get(position);
			if (!TextUtils.isEmpty(contact.getContact_name())) {
				holder.tv.setText(contact.getContact_name());
			}
			byte[] photo = contact.getContact_head();
			Log.e("", "bitmapUri   ===  " + photo);
			if (photo != null) {
				Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
				if(bitmap != null)
				{
					holder.iv.setImageBitmap(bitmap);
				}
			}
			String photo_uri = contact.getPhoto_uri();
			if(!TextUtils.isEmpty(photo_uri))
			{
				Uri uri = Uri.parse(photo_uri);
				try {
					holder.iv.setImageURI(uri);
				} catch (Exception e) {
					Log.e("", "头像的  Uri不存在 ");
				}
			}

		}

		return convertView;
	}

	class ViewHolder {
		ImageView iv;
		TextView tv;
	}

}
