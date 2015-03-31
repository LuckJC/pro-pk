package com.mediatek.encapsulation.com.android.ex.chips;

import com.android.ex.chips.MTKRecipientList;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedMTKRecipientList extends MTKRecipientList {

    public EncapsulatedMTKRecipientList() {
        super();
    }

    public void addRecipient(EncapsulatedMTKRecipient recipient) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            super.addRecipient(recipient);
        } else {

        }
    }
}
