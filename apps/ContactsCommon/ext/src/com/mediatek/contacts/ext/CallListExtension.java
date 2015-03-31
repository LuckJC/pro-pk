package com.mediatek.contacts.ext;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

public class CallListExtension {

    public int layoutExtentionIcon(int leftBound, int topBound, int bottomBound, int rightBound,
            int mGapBetweenImageAndText, ImageView mExtentionIcon, String commd) {
        return rightBound;
    }

    public void measureExtention(ImageView mExtentionIcon, String commd) {
    }

    public void setExtentionImageView(ImageView view, String commd) {
        // do nothing
    }

    public boolean setExtentionIcon(String number, String commd) {
        return false;
    }

    public boolean checkPluginSupport(String commd) {
        return false;
    }

    public void onCreate(ListFragment fragment) {
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    public void onDestroy() {
    }

    public boolean onListItemClick(ListView l, View v, int position, long id) {
        return false;
    }

    /*public boolean onItemLongClick(ListView l, View v, int position, long id) {
        return false;
    }*/

    public boolean onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        return false;
    }

    public boolean onContextItemSelected(MenuItem item) {
        return false;
    }
}
