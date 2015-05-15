package com.szkj.szgesturesetting;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class Item_Adapter extends BaseAdapter{
	private Context context;
    private List<Have_DefinedGestureBean> list;
    
    public Item_Adapter(Context ct,List<Have_DefinedGestureBean> lst)
    {
    	this.context = ct;
    	this.list = lst;
    }

	private class ViewHolder {
		//public TextView text;
		public TextView gesture;
		public TextView function;
		
	}

	@Override
	public int getCount() {
		return list.size();
	}
	
	public void setData(List<Have_DefinedGestureBean> list) {
		this.list = list;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView,
			ViewGroup parent) {
		
		View view = convertView;
		final ViewHolder holder;
		if (convertView == null) {
			view = LayoutInflater.from(context).inflate(R.layout.list_item,
					parent, false);
			holder = new ViewHolder();
		
			holder.gesture = (TextView)view.findViewById(R.id.gesture_style);
			holder.function = (TextView)view.findViewById(R.id.function);

			view.setTag(holder);
			
			
				
		} else {
			holder = (ViewHolder) view.getTag();

		}
		
		holder.gesture.setText(list.get(position).getGesture_style());
		holder.function.setText(list.get(position).getFunction_name());
		
		if(position<=3)
		{
			view.setBackgroundColor(Color.GRAY);
		}
		else
		{
			view.setBackgroundColor(Color.WHITE);
		}
		
		
		return view;
		
	}
		
		
}
