
package com.mediatek.encapsulation.android.telephony;

import android.telephony.SmsMessage;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.telephony.SmsMessageEx;

public class EncapsulatedSmsMessageEx {

    private static int sSlotId;

    public static void setSmsSlotId(int slot) {
        sSlotId = slot;
    }

    public static byte[] getTpdu(SmsMessage msg, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsMessageEx.getDefault().getTpdu(msg, slotId);
        } else {
            return null;
        }
    }

    public static byte[] getSmsc(SmsMessage msg, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsMessageEx.getDefault().getSmsc(msg, slotId);
        } else {
            return null;
        }
    }

    public static int getMessageSimId(SmsMessage sms) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sms.getMessageSimId();
        } else {
            return sSlotId;
        }
    }

    public static int[] calculateLength(CharSequence msgBody, boolean use7bitOnly,
            int encodingType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsMessage.calculateLength(msgBody, use7bitOnly, encodingType);
        } else {
            return new int[]{0,0,0,0};
        }
    }
}
