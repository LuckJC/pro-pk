package com.mediatek.contacts.extension;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.mediatek.contacts.ext.ImportExportEnhancementExtension;
import com.mediatek.contacts.ext.SimServiceExtension;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardEntryConstructor;

import java.util.Iterator;
import java.util.LinkedList;

public class ImportExportEnhancementExtensionContainer extends ImportExportEnhancementExtension {

    private static final String TAG = "SimServiceExtensionContainer";

    private LinkedList<ImportExportEnhancementExtension> mSubExtensionList;

    public void add(ImportExportEnhancementExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<ImportExportEnhancementExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(ImportExportEnhancementExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    @Override
    public VCardEntryConstructor getVCardEntryConstructorExt(int estimatedVCardType, Account account,
            String estimatedCharset, String commond) {
        Log.i(TAG, "[importVCardExtension] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ImportExportEnhancementExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                ImportExportEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getVCardEntryConstructorExt(estimatedVCardType, account,
                            estimatedCharset, commond);
                }
            }
        }

        return super.getVCardEntryConstructorExt(estimatedVCardType, account,
                estimatedCharset, commond);
    }

    @Override
    public VCardComposer getVCardComposerExt(final Context context, final int vcardType,
            final boolean careHandlerErrors, String commond) {
        Log.i(TAG, "[exportVCardExtension] commond : " + commond);
        if (null != mSubExtensionList) {
            Iterator<ImportExportEnhancementExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                ImportExportEnhancementExtension extension = iterator.next();
                if (extension.getCommond().equals(commond)) {
                    return extension.getVCardComposerExt(context, vcardType, careHandlerErrors,
                            commond);
                }
            }
        }

        return super.getVCardComposerExt(context, vcardType, careHandlerErrors, commond);
    }

}
