package com.mediatek.mms.ext;

import java.util.HashMap;

import android.content.Context;
import android.content.ContextWrapper;

public class MmsSettingsImpl extends ContextWrapper implements IMmsSettings{
    private IMmsSettingsHost mHost = null;
    public MmsSettingsImpl(Context context) {
        super(context);
    }

    public void init(IMmsSettingsHost host) {
        mHost = host;
        return;
    }

    public String getSmsServiceCenter() {
        return "";
    }
}
