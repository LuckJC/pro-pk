package com.mediatek.encapsulation.com.google.android.mms.pdu;

import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedMultimediaMessagePdu {

    private MultimediaMessagePdu mMsgPdu;

    public EncapsulatedMultimediaMessagePdu(MultimediaMessagePdu pdu) {
        mMsgPdu = pdu;
    }

    public long getDateSent() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mMsgPdu.getDateSent();
        } else {
            return 0;
        }
    }
}