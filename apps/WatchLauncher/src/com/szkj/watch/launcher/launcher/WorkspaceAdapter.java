package com.szkj.watch.launcher.launcher;

import java.util.ArrayList;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.szkj.watch.launcher.R;


public class WorkspaceAdapter  extends PagerAdapter{
	// 4 items in one page
	private static final int N = 4;
	private static final int[] VIEW_ITEM_ID = { 
		R.id.item1, R.id.item2,
		R.id.item3, R.id.item4, };
	
	private List<ActivityInfo> mActivityInfos;
	private LayoutInflater mLayoutInflater;
	private PackageManager mPM;
	private Context mContext;
	private int ICON_WIDTH;
	private int ICON_HEIGHT;


	public WorkspaceAdapter(Context context, List<String> apps) {
		mContext = context;
		resolveActivityInfo(apps);
		mLayoutInflater = LayoutInflater.from(context);
		mPM = context.getPackageManager();
		ICON_WIDTH = context.getResources().getDimensionPixelSize(R.dimen.workspace_app_icon_width);
		ICON_HEIGHT = context.getResources().getDimensionPixelSize(R.dimen.workspace_app_icon_height);
	}
	
	private void resolveActivityInfo(List<String> apps) {
		mActivityInfos = new ArrayList<ActivityInfo>();
		
		if (apps == null) {
			return;
		}
		PackageManager pm = mContext.getPackageManager();
		for (String app : apps) {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.setPackage(app);
			List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
			if (infos != null) {
				for (ResolveInfo info : infos) {
					mActivityInfos.add(new ActivityInfo(info.activityInfo));
				}
			}
		}
	}

	@Override
	public int getCount() {
		return (mActivityInfos.size() + (N - 1)) / N;
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0 == arg1;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		int startIndex = position * N;
		View pageView = mLayoutInflater.inflate(R.layout.apps_page, null);
		container.addView(pageView);
		PackageManager pm = mPM;
		int validCount = 0;
		int count = mActivityInfos.size();
		for (int i = startIndex; i < (startIndex + N) && i < count; i++) {
			ActivityInfo activityInfo = mActivityInfos.get(i);
			int id = VIEW_ITEM_ID[validCount++];
			TextView tv = (TextView)pageView.findViewById(id);
			CharSequence label = activityInfo.loadLabel(pm);
			tv.setText(label);
			Drawable drawable = activityInfo.loadIcon(pm);
			if (drawable != null) {
				drawable.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT);
				tv.setCompoundDrawables(null, drawable, null, null);
			}
			Intent intent = new Intent();
			intent.setClassName(activityInfo.packageName, activityInfo.name);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			tv.setTag(intent);
			tv.setOnClickListener(new OnClickListener() {		
				@Override
				public void onClick(View v) {
					Intent intent = (Intent)v.getTag();
					try {						
						mContext.startActivity(intent);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(mContext, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
		return pageView;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeView((View)object);
	}
}