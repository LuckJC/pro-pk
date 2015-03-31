package com.mediatek.encapsulation.android.media;

import android.media.RingtoneManager;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedRingtoneManager {

    public static final String EXTRA_RINGTONE_PICKED_POSITION = EncapsulationConstant.USE_MTK_PLATFORM ?
            RingtoneManager.EXTRA_RINGTONE_PICKED_POSITION : "android.intent.extra.ringtone.PICKED_POSITION";

}