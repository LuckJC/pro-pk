package com.mediatek.contacts.ext;

import com.mediatek.dialer.PhoneCallDetailsEx;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;


public class CallDetailHistoryAdapterExtension {

    private static final String TAG = "CallDetailHistoryAdapterExtension";

    /**
     * @param context
     * @param phoneCallDetails
     */
    public void init(Context context, PhoneCallDetailsEx[] phoneCallDetails) {
    }

    public int getItemViewType(int position) {
        return -1;
    }

    public int getViewTypeCount(int currentViewTypeCount) {
        return currentViewTypeCount;
    }

    public View getViewPre(int position, View convertView, ViewGroup parent) {
        return null;
    }

    public View getViewPost(int position, View convertView, ViewGroup parent) {
        return convertView;
    }
}
