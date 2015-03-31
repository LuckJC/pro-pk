package com.mediatek.encapsulation;

import com.mediatek.encapsulation.EncapsulationConstant;
import android.provider.Telephony.Sms.Inbox;

public class EncapsulatedInbox {

    /**
     *  A switch for MTK Encapsulation API
     */
    public static final String BODY = EncapsulationConstant.USE_MTK_PLATFORM ? Inbox.BODY : "body";
}
