
package com.mediatek.encapsulation.com.android.ex.chips;

import android.content.Context;
import android.util.AttributeSet;

import com.android.ex.chips.MTKRecipientEditTextView;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedMTKRecipientEditTextView extends MTKRecipientEditTextView {

    public EncapsulatedMTKRecipientEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void appendList(EncapsulatedMTKRecipientList recipientList) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            super.appendList(recipientList);
        }
    }
}
