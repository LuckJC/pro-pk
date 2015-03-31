package com.mediatek.contacts.ext;

import android.content.ContentResolver;
import android.os.Bundle;

public class SimServiceExtension {
    
    public String getCommond() {
        return "";
    }
    
    public boolean importViaReadonlyContact(Bundle bundle, ContentResolver cr, String commond) {

        return true;
    }
}
