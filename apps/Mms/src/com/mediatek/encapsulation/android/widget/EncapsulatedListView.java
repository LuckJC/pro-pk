package com.mediatek.encapsulation.android.widget;

import android.widget.ListView;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;

public class EncapsulatedListView {

    private ListView mListView;

    public EncapsulatedListView(ListView listView) {
        mListView = listView;
    }

    public void clearScrapViewsIfNeeded() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mListView.clearScrapViewsIfNeeded();
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedListView -- clearScrapViewsIfNeeded()");
        }
    }
}