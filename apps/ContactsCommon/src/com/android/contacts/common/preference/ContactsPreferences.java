/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.common.preference;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.contacts.common.R;
import com.mediatek.contacts.util.LogUtils;

/**
 * Manages user preferences for contacts.
 */
public final class ContactsPreferences extends ContentObserver {

    public static final String PREF_DISPLAY_ONLY_PHONES = "only_phones";
    public static final boolean PREF_DISPLAY_ONLY_PHONES_DEFAULT = false;

    private Context mContext;
    private int mSortOrder = -1;
    private int mDisplayOrder = -1;
    private ChangeListener mListener = null;
    private Handler mHandler;

    public ContactsPreferences(Context context) {
        super(null);
        mContext = context;
        mHandler = new Handler();
    }

    public boolean isSortOrderUserChangeable() {
        return mContext.getResources().getBoolean(R.bool.config_sort_order_user_changeable);
    }

    public int getDefaultSortOrder() {
        if (mContext.getResources().getBoolean(R.bool.config_default_sort_order_primary)) {
            return ContactsContract.Preferences.SORT_ORDER_PRIMARY;
        } else {
            return ContactsContract.Preferences.SORT_ORDER_ALTERNATIVE;
        }
    }

    public int getSortOrder() {
        if (!isSortOrderUserChangeable()) {
            return getDefaultSortOrder();
        }

        if (mSortOrder == -1) {
            try {
                mSortOrder = Settings.System.getInt(mContext.getContentResolver(),
                        ContactsContract.Preferences.SORT_ORDER);
            } catch (SettingNotFoundException e) {
                mSortOrder = getDefaultSortOrder();
            }
        }
        return mSortOrder;
    }

    public void setSortOrder(int sortOrder) {
        mSortOrder = sortOrder;
        Settings.System.putInt(mContext.getContentResolver(),
                ContactsContract.Preferences.SORT_ORDER, sortOrder);
    }

    public boolean isDisplayOrderUserChangeable() {
        return mContext.getResources().getBoolean(R.bool.config_display_order_user_changeable);
    }

    public int getDefaultDisplayOrder() {
        if (mContext.getResources().getBoolean(R.bool.config_default_display_order_primary)) {
            return ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY;
        } else {
            return ContactsContract.Preferences.DISPLAY_ORDER_ALTERNATIVE;
        }
    }

    public int getDisplayOrder() {
        if (!isDisplayOrderUserChangeable()) {
            return getDefaultDisplayOrder();
        }

        if (mDisplayOrder == -1) {
            try {
                mDisplayOrder = Settings.System.getInt(mContext.getContentResolver(),
                        ContactsContract.Preferences.DISPLAY_ORDER);
            } catch (SettingNotFoundException e) {
                mDisplayOrder = getDefaultDisplayOrder();
            }
        }
        return mDisplayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        mDisplayOrder = displayOrder;
        Settings.System.putInt(mContext.getContentResolver(),
                ContactsContract.Preferences.DISPLAY_ORDER, displayOrder);
    }

    public void registerChangeListener(ChangeListener listener) {
        if (mListener != null) unregisterChangeListener();

        mListener = listener;

        // Reset preferences to "unknown" because they may have changed while the
        // observer was unregistered.
        mDisplayOrder = -1;
        mSortOrder = -1;

        final ContentResolver contentResolver = mContext.getContentResolver();
        contentResolver.registerContentObserver(
                Settings.System.getUriFor(
                        ContactsContract.Preferences.SORT_ORDER), false, this);
        contentResolver.registerContentObserver(
                Settings.System.getUriFor(
                        ContactsContract.Preferences.DISPLAY_ORDER), false, this);
    }

    public void unregisterChangeListener() {
        if (mListener != null) {
            mContext.getContentResolver().unregisterContentObserver(this);
            mListener = null;
        }
    }

    @Override
    public void onChange(boolean selfChange) {
        // This notification is not sent on the Ui thread. Use the previously created Handler
        // to switch to the Ui thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSortOrder = -1;
                mDisplayOrder = -1;
                if (mListener != null) mListener.onChange();
            }
        });
    }

    public interface ChangeListener {
        void onChange();
    }

    /**
     * M:fix ALPS01013843
     * @param cursorLoader The loader has the project and will set the display_name or sort_order by the preference.
     * @param displayNameIndex The DisplayName index
     * @param context
     * This will set the display name sort by preference:DISPLAY_NAME_PRIMARY or DISPLAY_NAME_ALTERNATIVE
     * And if the sort order if not set,it will set the sort order sort by prefence:SORT_KEY_PRIMARY or SORT_KEY_ALTERNATIVE
     */
    public static void fixSortOrderByPreference(CursorLoader cursorLoader, int displayNameIndex, Context context) {
        String[] project = cursorLoader.getProjection();
        if (project == null || project.length < displayNameIndex) {
            LogUtils.i(TAG, "[fixSortByPreference] project is null or not right.project:" + project);
            return;
        }

        ContactsPreferences preferences = new ContactsPreferences(context);

        // for display name sort order
        int displayNameSortOrder = preferences.getDisplayOrder();
        switch (displayNameSortOrder) {
        case ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY:
            project[displayNameIndex] = Contacts.DISPLAY_NAME_PRIMARY;
            break;
        case ContactsContract.Preferences.DISPLAY_ORDER_ALTERNATIVE:
            project[displayNameIndex] = Contacts.DISPLAY_NAME_ALTERNATIVE;
            break;
        default:
            LogUtils.w(TAG, "[fixSortByPreference] displayNameSortOrder is error:" + displayNameSortOrder);
        }

        // for contacts sort order
        int contactsSoryOrder = preferences.getSortOrder();
        String order = cursorLoader.getSortOrder();
        if (order != null) {
            LogUtils.w(TAG, "[fixSortByPreference] The CursorLoader already has sort order:" + order);
            return;
        }
        switch (contactsSoryOrder) {
        case ContactsContract.Preferences.SORT_ORDER_PRIMARY:
            cursorLoader.setSortOrder(Contacts.SORT_KEY_PRIMARY);
            break;
        case ContactsContract.Preferences.SORT_ORDER_ALTERNATIVE:
            cursorLoader.setSortOrder(Contacts.SORT_KEY_ALTERNATIVE);
            break;
        default:
            LogUtils.w(TAG, "[fixSortByPreference] Contacts SortOrder is error:" + contactsSoryOrder);
        }
    }

    public static final String TAG = ContactsPreferences.class.getSimpleName();
}
