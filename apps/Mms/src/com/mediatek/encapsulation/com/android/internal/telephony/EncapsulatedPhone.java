package com.mediatek.encapsulation.com.android.internal.telephony;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.encapsulation.EncapsulationConstant;

/**
 * Internal interface used to control the phone; SDK developers cannot
 * obtain this interface.
 */
public interface EncapsulatedPhone extends Phone {

    /* 3G Switch start */
    /**
     * get3GCapabilitySIM
     * get SIM with 3G capability.
     *
     * @return the id (slot) with 3G capability (Phone.GEMINI_SIM_ID_1 or Phone.GEMINI_SIM_ID_2).
     */
    int get3GCapabilitySIM();

    /// M: google JB.MR1 patch,  phone's some static final variable move to PhoneConstants @{
    public static final String STATE_KEY = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        PhoneConstants.STATE_KEY : "state";

    public static final String DATA_APN_TYPE_KEY = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.DATA_APN_TYPE_KEY : "apnType";

    static final String REASON_NO_SUCH_PDP = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.REASON_NO_SUCH_PDP : "noSuchPdp";

    public static final String APN_TYPE_ALL = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_ALL : "*";
    /** APN type for MMS traffic */
    public static final String APN_TYPE_MMS = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_MMS : "mms";
    /**
     * Return codes for <code>enableApnType()</code>
     */
    public static final int APN_ALREADY_ACTIVE  = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_ALREADY_ACTIVE : 0;
    public static final int APN_REQUEST_STARTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_REQUEST_STARTED : 1;
    public static final int APN_TYPE_NOT_AVAILABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_TYPE_NOT_AVAILABLE : 2;
    public static final int APN_REQUEST_FAILED     = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_REQUEST_FAILED : 3;
    public static final int APN_ALREADY_INACTIVE   = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_ALREADY_INACTIVE : 4;

    /**
     * SIM ID for GEMINI
     */
    public static final int GEMINI_SIM_NUM = EncapsulationConstant.USE_MTK_PLATFORM ?
                                              PhoneConstants.GEMINI_SIM_NUM : 1;
    public static final int GEMINI_SIM_1 = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_1 : 0;
    public static final int GEMINI_SIM_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_2 : 1;
    public static final int GEMINI_SIM_3 = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_3 : 2;
    public static final int GEMINI_SIM_4 = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_4 : 3;

    public static final String GEMINI_SIM_ID_KEY = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_ID_KEY : "simId";


    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    /** UNKNOWN, invalid value */
    public static final int SIM_INDICATOR_UNKNOWN = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_UNKNOWN : -1;
    /** ABSENT, no SIM/USIM card inserted for this phone */
    public static final int SIM_INDICATOR_ABSENT = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_ABSENT : 0;
    /** RADIOOFF,  has SIM/USIM inserted but not in use . */
    public static final int SIM_INDICATOR_RADIOOFF = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_RADIOOFF : 1;
    /** LOCKED,  has SIM/USIM inserted and the SIM/USIM has been locked. */
    public static final int SIM_INDICATOR_LOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_LOCKED : 2;
    /** INVALID : has SIM/USIM inserted and not be locked but failed to register to the network. */
    public static final int SIM_INDICATOR_INVALID = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_INVALID : 3;
    /** SEARCHING : has SIM/USIM inserted and SIM/USIM state is Ready and is searching for network. */
    public static final int SIM_INDICATOR_SEARCHING = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_SEARCHING : 4;
    /** NORMAL = has SIM/USIM inserted and in normal service(not roaming and has no data connection). */
    public static final int SIM_INDICATOR_NORMAL = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_NORMAL : 5;
    /** ROAMING : has SIM/USIM inserted and in roaming service(has no data connection). */
    public static final int SIM_INDICATOR_ROAMING = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_ROAMING : 6;
    /** CONNECTED : has SIM/USIM inserted and in normal service(not roaming) and data connected. */
    public static final int SIM_INDICATOR_CONNECTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_CONNECTED : 7;
    /** ROAMINGCONNECTED = has SIM/USIM inserted and in roaming service(not roaming) and data connected.*/
    public static final int SIM_INDICATOR_ROAMINGCONNECTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED : 8;
    /// @}
}
