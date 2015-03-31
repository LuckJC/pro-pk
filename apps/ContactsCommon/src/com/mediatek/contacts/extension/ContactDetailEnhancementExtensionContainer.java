/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.extension;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;

import com.mediatek.contacts.ext.ContactDetailEnhancementExtension;
import com.mediatek.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.ext.ContactDetailEnhancementExtension.DetailUIController;
import com.mediatek.contacts.ext.ContactDetailEnhancementExtension.MeasureInfo;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ContactDetailEnhancementExtensionContainer extends ContactDetailEnhancementExtension {

    private static final String TAG = "ContactDetailEnhancementExtension";

    private LinkedList<ContactDetailEnhancementExtension> mSubExtensionList;

    public void add(ContactDetailEnhancementExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<ContactDetailEnhancementExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(ContactDetailEnhancementExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public boolean isUseOperation(String commond) {
        Log.i(TAG, "[isUseOperation] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.isUseOperation(commond);
                }
            }
        }
        return super.isUseOperation(commond);
    }

    public boolean bindDetailEnhancementView(final Activity activity, final String number,
            View firstActionViewContainer,
            final ImageView firstActionButtonView, View vewVtCallDivider,
            ImageView btnVtCallAction, View vtcallActionViewContainer,
            int visibility, String secondaryActionDescription, int mInsertedSimCount, String commond) {
        Log.i(TAG, "[bindDetailEnhancementView] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.bindDetailEnhancementView(activity,
                            number, firstActionViewContainer,
                            firstActionButtonView, vewVtCallDivider,
                            btnVtCallAction, vtcallActionViewContainer,
                            visibility, secondaryActionDescription,
                            mInsertedSimCount, commond);
                }
            }
        }
        return super.bindDetailEnhancementView(activity, number,
                firstActionViewContainer, firstActionButtonView,
                vewVtCallDivider, btnVtCallAction, vtcallActionViewContainer,
                visibility, secondaryActionDescription, mInsertedSimCount,
                commond);
    }

    public boolean bindDetailEnhancementViewForQuickContact(final Activity activity,
            final String number, ImageView firstActionViewButton,
            View firstDivider, ImageView btnVtCallAction, int mInsertedSimCount, String commond) {
        Log.i(TAG, "[bindDetailEnhancementViewForQuickContact] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.bindDetailEnhancementViewForQuickContact(
                            activity, number, firstActionViewButton,
                            firstDivider, btnVtCallAction, mInsertedSimCount,
                            commond);
                }
            }
        }
        return super.bindDetailEnhancementViewForQuickContact(activity, number,
                firstActionViewButton, firstDivider, btnVtCallAction,
                mInsertedSimCount, commond);
    }

    public Drawable getDrawableCorG(SimInfoRecord simInfo, String commond) {
        Log.i(TAG, "[getDrawableCorG] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getDrawableCorG(simInfo, commond);
                }
            }
        }
        return super.getDrawableCorG(simInfo, commond);

    }

    public BroadcastReceiver registeChangeDefaultSim(Activity activity, BroadcastReceiver broadcastReceiver,
            String commond) {
        Log.i(TAG, "[registeChangeDefaultSim] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.registeChangeDefaultSim(activity, broadcastReceiver, commond);
                }
            }
        }
        return super.registeChangeDefaultSim(activity, broadcastReceiver, commond);
    }

    public long getEnhancementPhotoId(int isSdnContact, int colorId, int slot, String commond) {
        Log.i(TAG, "[getEnhancementPhotoId] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getEnhancementPhotoId(isSdnContact, colorId, slot, commond);
                }
            }
        }
        return super.getEnhancementPhotoId(isSdnContact, colorId, slot, commond);
    }

    public String getEnhancementPhotoUri(int isSdnContact, int colorId, int slot, String commond) {
        Log.i(TAG, "[getEnhancementPhotoUri] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getEnhancementPhotoUri(isSdnContact, colorId, slot, commond);
                }
            }
        }
        return super.getEnhancementPhotoUri(isSdnContact, colorId, slot, commond);
    }

    public Drawable getEnhancementAccountSimIndicator(int i, int slot, String commond) {
        Log.i(TAG, "[getEnhancementAccountSimIndicator] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getEnhancementAccountSimIndicator(i, slot, commond);
                }
            }
        }
        return super.getEnhancementAccountSimIndicator(i, slot, commond);
    }
    
    public void configActionBarExt(ActionBar bar, String commond) {
        Log.i(TAG, "[configActionBarExt] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    extension.configActionBarExt(bar, commond);
                    return;
                }
            }
        }
        super.configActionBarExt(bar, commond);
    }
    
    public void initActionBarExt(ActionBar bar, boolean withUpdatePage, String commond) {
        Log.i(TAG, "[initActionBarExt] commond : " + commond); 
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    extension.initActionBarExt(bar, withUpdatePage, commond);
                    return;
                }
            }
        }
        super.initActionBarExt(bar, withUpdatePage, commond);
    }
    
    public void onFragmentPageChange(int index, String commond){
        Log.i(TAG, "[onFragmentPageChange] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    extension.onFragmentPageChange(index, commond);
                    return;
                }
            }
        }
        super.onFragmentPageChange(index, commond);
    }
    
    public void registerDetailUIController(DetailUIController detailUIController, int val,
            int mLayoutMode, String commond) {
        Log.i(TAG, "[registerDetailUIController] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    extension.registerDetailUIController(detailUIController, val, mLayoutMode,
                            commond);
                    return;
                }
            }
        }
        super.registerDetailUIController(detailUIController, val, mLayoutMode, commond);
    }
    
    public int getDetailUpdateIndex(String commond) {
        Log.i(TAG, "[getDetailUpdateIndex] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getDetailUpdateIndex(commond);
                }
            }
        }
        return super.getDetailUpdateIndex(commond);
    }
    
    public void setViewPagerCurrentItemEx(ViewPager viewPager, int mCurrentPageIndex,
            boolean smoothScroll, String commond) {
        Log.i(TAG, "[setViewPagerCurrentItemEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    extension.setViewPagerCurrentItemEx(viewPager, mCurrentPageIndex, smoothScroll,
                            commond);
                    return;
                }
            }
        }
        super.setViewPagerCurrentItemEx(viewPager, mCurrentPageIndex, smoothScroll, commond);

    }

    public int getMaxFragmentViewCountEx(int maxFragmentViewCount, String commond) {
        Log.i(TAG, "[getMaxFragmentViewCountEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getMaxFragmentViewCountEx(maxFragmentViewCount, commond);
                }
            }
        }
        return super.getMaxFragmentViewCountEx(maxFragmentViewCount, commond);
    }

    public int getVisibleFragmentViewCountEx(boolean enableSwipe, String commond) {
        Log.i(TAG, "[getVisibleFragmentViewCountEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getVisibleFragmentViewCountEx(enableSwipe, commond);
                }
            }
        }
        return super.getVisibleFragmentViewCountEx(enableSwipe, commond);
    }
    
    public boolean onTouchEx(String commond) {
        Log.i(TAG, "[onTouchEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.onTouchEx(commond);
                }
            }
        }
        return super.onTouchEx(commond);
    }

    public boolean onScrollChangedEx(String commond) {
        Log.i(TAG, "[onScrollChangedEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.onScrollChangedEx(commond);
                }
            }
        }
        return super.onScrollChangedEx(commond);
    }
    
    public MeasureInfo onMeasureEx(int widthMeasureSpec, int heightMeasureSpec,
            int fragmentViewCount, float fragmentWidthScreenWidthFraction,String commond) {
        Log.i(TAG, "[onMeasureEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.onMeasureEx(widthMeasureSpec, heightMeasureSpec,
                            fragmentViewCount, fragmentWidthScreenWidthFraction, commond);
                }
            }
        }
        return super.onMeasureEx(widthMeasureSpec, heightMeasureSpec, fragmentViewCount,
                fragmentWidthScreenWidthFraction, commond);
    }

    public void childMeasureEx(boolean mEnableSwipe, View child, int mMinFragmentWidth,
            int visibleFragmentViewCount, int screenHeight,
             int screenWidth, String commond) {
        Log.i(TAG, "[childMeasureEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    extension.childMeasureEx(mEnableSwipe, child, mMinFragmentWidth,
                            visibleFragmentViewCount, screenHeight, screenWidth, commond);
                    return;
                }
            }
        }
        super.childMeasureEx(mEnableSwipe, child, mMinFragmentWidth, visibleFragmentViewCount,
                screenHeight, screenWidth, commond);

    }

    public int getDesiredPageUpdatesEx(int mLastScrollPosition, int mUpperThreshold, boolean enableSwipe,
            String commond) {
        Log.i(TAG, "[getDesiredPageUpdatesEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getDesiredPageUpdatesEx(mLastScrollPosition, mUpperThreshold,
                            enableSwipe, commond);
                }
            }
        }
        return super.getDesiredPageUpdatesEx(mLastScrollPosition, mUpperThreshold, enableSwipe,
                commond);
    }

    public int getDesiredPageHistoryEx(int mLastScrollPosition, int mUpperThreshold,
            int updatePageIndex, String commond) {
        Log.i(TAG, "[getDesiredPageHistoryEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getDesiredPageHistoryEx(mLastScrollPosition, mUpperThreshold,
                            updatePageIndex, commond);
                }
            }
        }
        return super.getDesiredPageHistoryEx(mLastScrollPosition, mUpperThreshold, updatePageIndex,
                commond);
    }
    
    public View getViewPagerViewEx(ViewPager mViewPager, String commond) {
        Log.i(TAG, "[getViewPagerViewEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getViewPagerViewEx(mViewPager, commond);
                }
            }
        }
        return super.getViewPagerViewEx(mViewPager, commond);
    }

    public int getCurrentPageIndexEx(ViewPager mViewPager, boolean fragmentCarouselIsNull,
            int currentPage, String commond) {
        Log.i(TAG, "[getCurrentPageIndexEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getCurrentPageIndexEx(mViewPager,
                            currentPage, commond);
                }
            }
        }
        return super.getCurrentPageIndexEx(mViewPager, currentPage, commond);
    }
    
    public Fragment initContactDetailHistoryFragment(FragmentManager mFragmentManager, String commond) {
        Log.i(TAG, "[initContactDetailHistoryFragment] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.initContactDetailHistoryFragment(mFragmentManager, commond);
                }
            }
        }
        return super.initContactDetailHistoryFragment(mFragmentManager, commond);
    }

    public void addHistoryTransaction(FragmentManager mFragmentManager,
            FragmentTransaction transaction, String commond) {
        Log.i(TAG, "[addHistoryTransaction] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    extension.addHistoryTransaction(mFragmentManager, transaction, commond);
                    return;
                }
            }
        }
        super.addHistoryTransaction(mFragmentManager, transaction, commond);
    }

    public void setPhoneNumbersToFragmentEx(Uri mContactUri, List<String> mPhoneNumbers,
            long contactId, String commond) {
        Log.i(TAG, "[setPhoneNumbersToFragmentEx] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ContactDetailEnhancementExtension> iterator = mSubExtensionList
                    .iterator();
            while (iterator.hasNext()) {
                ContactDetailEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    extension.setPhoneNumbersToFragmentEx(mContactUri, mPhoneNumbers, contactId, commond);
                    return;
                }
            }
        }
        super.setPhoneNumbersToFragmentEx(mContactUri, mPhoneNumbers, contactId, commond);
    }
}
