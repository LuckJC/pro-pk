package com.mediatek.contacts.ext;

import com.android.vcard.VCardComposer;
import com.android.vcard.VCardEntryConstructor;

import android.accounts.Account;
import android.content.Context;

public class ImportExportEnhancementExtension {

    public String getCommond() {
        return "";
    }

    public VCardEntryConstructor getVCardEntryConstructorExt(int estimatedVCardType,
            Account account,
            String estimatedCharset, String commond) {
        return new VCardEntryConstructor(estimatedVCardType, account, estimatedCharset);
    }

    public VCardComposer getVCardComposerExt(final Context context, final int vcardType,
            final boolean careHandlerErrors, String commond) {
        return new VCardComposer(context, vcardType, careHandlerErrors);
    }
}
