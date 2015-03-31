
package com.mediatek.encapsulation.android.provider;

import android.provider.ContactsContract;
import com.mediatek.encapsulation.EncapsulationConstant;


/**
 * The Settings provider contains global system-level device preferences.
 */
public final class EncapsulatedContactsContract {


    public static final class Data {
        public static final String SIM_ASSOCIATION_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                ContactsContract.Data.SIM_ASSOCIATION_ID : "sim_id";
    }

}