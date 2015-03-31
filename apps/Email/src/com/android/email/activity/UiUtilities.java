/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.email.activity;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.email.R;

public class UiUtilities {

    private UiUtilities() {
    }

    /**
     * Formats the given size as a String in bytes, kB, MB or GB.  Ex: 12,315,000 = 11 MB
     */
    public static String formatSize(Context context, long size) {
        final Resources res = context.getResources();
        final long KB = 1024;
        final long MB = (KB * 1024);
        final long GB  = (MB * 1024);

        int resId;
        int value;

        if (size < KB) {
            resId = R.plurals.message_view_attachment_bytes;
            value = (int) size;
        } else if (size < MB) {
            resId = R.plurals.message_view_attachment_kilobytes;
            value = (int) (size / KB);
        } else if (size < GB) {
            resId = R.plurals.message_view_attachment_megabytes;
            value = (int) (size / MB);
        } else {
            resId = R.plurals.message_view_attachment_gigabytes;
            value = (int) (size / GB);
        }
        return res.getQuantityString(resId, value, value);
    }

    public static String getMessageCountForUi(Context context, int count,
            boolean replaceZeroWithBlank) {
        if (replaceZeroWithBlank && (count == 0)) {
            return "";
        } else if (count > 999) {
            return context.getString(R.string.more_than_999);
        } else {
            return Integer.toString(count);
        }
    }

    /** Generics version of {@link Activity#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(Activity parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /** Generics version of {@link View#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(View parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /**
     * Same as {@link Activity#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(Activity parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    /**
     * Same as {@link View#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(View parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    private static View checkView(View v) {
        if (v == null) {
            throw new IllegalArgumentException("View doesn't exist");
        }
        return v;
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View v, int visibility) {
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(Activity parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /// M: Use to constraint the max word number that user can input.
    public static void setupLengthFilter(EditText inputText, final Context context,
            final int maxLength , final boolean showToast) {
        // Create a new filter
        InputFilter.LengthFilter filter = new InputFilter.LengthFilter(
                maxLength) {
            public CharSequence filter(CharSequence source, int start, int end,
                    Spanned dest, int dstart, int dend) {
                if (source != null
                        && source.length() > 0
                        && (((dest == null ? 0 : dest.length()) + dstart - dend) == maxLength)) {
                    if (showToast) {
                        Toast.makeText(context,
                                context.getString(R.string.not_add_more_text),
                                Toast.LENGTH_SHORT).show();
                    }
                    return "";
                }
                return super.filter(source, start, end, dest, dstart, dend);
            }
        };

        // Find exist lenght filter.
        InputFilter[] filters = inputText.getFilters();
        int length = 0;
        for (int i = 0; i < filters.length; i++) {
            if (!(filters[i] instanceof InputFilter.LengthFilter)) {
                length++;
            }
        }

        //Only one length filter.
        InputFilter[] contentFilters = new InputFilter[length + 1];
        for (int i = 0; i < filters.length; i++) {
            if (!(filters[i] instanceof InputFilter.LengthFilter)) {
                contentFilters[i] = filters[i];
            }
        }
        contentFilters[length] = filter;
        inputText.setFilters(contentFilters);
    }
}
