package com.mediatek.contacts.ext;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

public class DialPadExtension {

    private static final String TAG = "DialPadExtension";

    public String changeChar(String string, String string2, String commd) {
        Log.i(TAG, "[changeChar] string : " + string + " | string2 : " + string2);
        return string2;
    }

    public boolean handleChars(Context context, String input, String commd) {
        return false;
    }

    public void onCreate(Fragment fragment, IDialpadFragment dialpadFragment) {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState, View resultView) {
        return resultView;
    }

    public void onDestroy() {
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    public void onPrepareOptionsMenu(Menu menu) {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public void constructPopupMenu(PopupMenu popupMenu, View anchorView, Menu menu) {
    }

    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    public boolean updateDialAndDeleteButtonEnabledState(final String lastNumberDialed) {
        return false;
    }
}
