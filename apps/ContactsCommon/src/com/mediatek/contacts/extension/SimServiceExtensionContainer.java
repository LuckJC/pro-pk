package com.mediatek.contacts.extension;

import android.content.ContentResolver;
import android.os.Bundle;
import android.util.Log;

import com.mediatek.contacts.ext.SimServiceExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class SimServiceExtensionContainer extends SimServiceExtension {

    private static final String TAG = "SimServiceExtensionContainer";

    private LinkedList<SimServiceExtension> mSubExtensionList;

    public void add(SimServiceExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<SimServiceExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(SimServiceExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    @Override
    public boolean importViaReadonlyContact(Bundle bundle, ContentResolver cr, String commond) {
        Log.i(TAG, "[importViaReadonlyContact] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<SimServiceExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                SimServiceExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.importViaReadonlyContact(bundle, cr, commond);
                }
            }
        }
        
        return super.importViaReadonlyContact(bundle, cr, commond);
    }

}
