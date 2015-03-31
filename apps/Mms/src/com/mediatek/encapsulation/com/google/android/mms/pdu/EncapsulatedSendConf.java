package com.mediatek.encapsulation.com.google.android.mms.pdu;

import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.SendConf;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedSendConf {

    private SendConf mSendConf;

    public EncapsulatedSendConf(SendConf sendConf) {
        mSendConf = sendConf;
    }

    public EncodedStringValue getResponseText() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mSendConf.getResponseText();
        } else {
            return new EncodedStringValue("");
        }
    }
}