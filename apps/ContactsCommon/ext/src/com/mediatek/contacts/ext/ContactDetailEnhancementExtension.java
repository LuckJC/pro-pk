package com.mediatek.contacts.ext;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;

public class ContactDetailEnhancementExtension {
    private static final String TAG = "ContactDetailEnhancementExtension";

    protected static final int ABOUT_PAGE = 0;
    protected static final int UPDATES_PAGE = 1;
    protected static final int HISTORY_PAGE = 2;

    public String getCommond() {
        return "";
    }

    public boolean isUseOperation(String commond) {
        return false;
    }

    public boolean bindDetailEnhancementView(final Activity activity, final String number,
            View firstActionViewContainer,
            final ImageView firstActionButtonView, View vewVtCallDivider,
            ImageView btnVtCallAction, View vtcallActionViewContainer,
            int visibility, String secondaryActionDescription,int mInsertedSimCount, String commond) {

        return false;
    }
    
    public boolean bindDetailEnhancementViewForQuickContact(final Activity activity,
            final String number, ImageView firstActionViewButton,
            View firstDivider, ImageView btnVtCallAction, int mInsertedSimCount, String commond) {
        
        return false;
    }

    public Drawable getDrawableCorG(SimInfoRecord simInfo, String commond) {
        return null;
    }

    public BroadcastReceiver registeChangeDefaultSim(Activity activity,
            BroadcastReceiver broadcastReceiver, String commond) {
        return null;
    }

    public long getEnhancementPhotoId(int isSdnContact, int colorId, int slot, String commond) {
        return -1;
    }
    
    public String getEnhancementPhotoUri(int isSdnContact, int colorId, int slot, String commond) {
        return "";
    }
    
    public Drawable getEnhancementAccountSimIndicator(int i,int slot, String commond) {
        return null;
    }

    public void configActionBarExt(ActionBar bar, String commond) {

    }

    public void initActionBarExt(ActionBar bar, boolean withUpdatePage, String commond) {

    }

    public static interface DetailUIController {
        public void requestScrollFragment(int x, int y);

        public void requestScrollTab(int x, int y);

        public void requestSwitchViewPager(int index);

        public void requestSwitchFragment(int index);

        public void onFragmentPageChange(ActionBar actionBar, int index);
    }

    public void registerDetailUIController(DetailUIController detailUIController, int val,
            int mLayoutMode, String commond) {
        Log.i(TAG, "--registerDetailUIController");
    }

    public void onFragmentPageChange(int index, String commond) {

    }

    public int getDetailUpdateIndex(String commond) {
        return 1;
    }

    public void setViewPagerCurrentItemEx(ViewPager viewPager, int mCurrentPageIndex,
            boolean smoothScroll, String commond) {
        viewPager.setCurrentItem(0, false /* smooth transition */);
    }

    public int getMaxFragmentViewCountEx(int maxFragmentViewCount, String commond) {
        return maxFragmentViewCount;
    }

    public int getVisibleFragmentViewCountEx(boolean enableSwipe, String commond) {
        return enableSwipe ? 2 : 1;
    }

    public boolean onTouchEx(String commond) {
        return true;
    }

    public boolean onScrollChangedEx(String commond) {
        return true;
    }

    public class MeasureInfo {
        public int mAllowedHorizontalScrollLength = Integer.MIN_VALUE;
        public int mLowerThreshold = Integer.MIN_VALUE;
        public int mUpperThreshold = Integer.MIN_VALUE;
        public int mMinFragmentWidth = Integer.MIN_VALUE;
    }

    public MeasureInfo onMeasureEx(int widthMeasureSpec, int heightMeasureSpec,
            int fragmentViewCount, float fragmentWidthScreenWidthFraction,String commond) {
        MeasureInfo measureInfo = new MeasureInfo();
        measureInfo.mMinFragmentWidth = (int) (fragmentWidthScreenWidthFraction * widthMeasureSpec);
        measureInfo.mAllowedHorizontalScrollLength = (fragmentViewCount * measureInfo.mMinFragmentWidth)
                - widthMeasureSpec;
        measureInfo.mLowerThreshold = (widthMeasureSpec - measureInfo.mMinFragmentWidth)
                / fragmentViewCount;
        measureInfo.mUpperThreshold = measureInfo.mAllowedHorizontalScrollLength
                - measureInfo.mLowerThreshold;
        return measureInfo;
    }

    public void childMeasureEx(boolean mEnableSwipe, View child, int mMinFragmentWidth,
            int visibleFragmentViewCount, int screenHeight,
             int screenWidth, String commond) {
        if (mEnableSwipe) {
            child.measure(MeasureSpec.makeMeasureSpec(
                          mMinFragmentWidth * 2, MeasureSpec.EXACTLY),
                          MeasureSpec.makeMeasureSpec(screenHeight, MeasureSpec.EXACTLY));
        } else {
            // Otherwise, the {@link LinearLayout} child width will just
            // be the screen width because it will only have 1 child fragment.
            child.measure(MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY),
                          MeasureSpec.makeMeasureSpec(screenHeight, MeasureSpec.EXACTLY));
        }

    }

    public int getDesiredPageUpdatesEx(int mLastScrollPosition, int mUpperThreshold,
            boolean enableSwipe,
            String commond) {
        return (mLastScrollPosition < mUpperThreshold) ? ABOUT_PAGE : UPDATES_PAGE;
    }

    public int getDesiredPageHistoryEx(int mLastScrollPosition, int mUpperThreshold,
            int updatePageIndex, String commond) {
        return -1;
    }

    public View getViewPagerViewEx(ViewPager mViewPager, String commond) {

        return null;
    }

    public int getCurrentPageIndexEx(ViewPager mViewPager,
            int currentPage, String commond) {
        return 0;
    }

    public Fragment initContactDetailHistoryFragment(FragmentManager mFragmentManager, String commond) {
        return null;
    }

    public void addHistoryTransaction(FragmentManager mFragmentManager, FragmentTransaction transaction, String commond) {

    }

    public void setPhoneNumbersToFragmentEx(Uri mContactUri, List<String> mPhoneNumbers,
            long contactId, String commond) {

    }
}
