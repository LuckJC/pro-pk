package com.mediatek.encapsulation.com.android.internal.telephony;

import android.os.ServiceManager;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import com.android.internal.telephony.ITelephony;
import com.mediatek.common.telephony.ITelephonyEx;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;
import java.util.List;

/**
 * ITelephony used to interact with the phone.  Mostly this is used by the
 * TelephonyManager class.  A few places are still using this directly.
 * Please clean them up if possible and use TelephonyManager instead.
 */
public class EncapsulatedTelephonyService {

    /** M: MTK reference ITelephony */
    private static ITelephony sTelephony;
    private static ITelephonyEx sTelephonyEx;
    private static EncapsulatedTelephonyService sTelephonyService = new EncapsulatedTelephonyService();;

    private EncapsulatedTelephonyService() {}

    synchronized public static EncapsulatedTelephonyService getInstance() {
        if (sTelephony != null && sTelephonyEx != null) {
            return sTelephonyService;
        } else {
            sTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            sTelephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            if (null != sTelephony && null != sTelephonyEx) {
                return sTelephonyService;
            } else {
                return null;
            }
        }
    }

    /**
     * Return ture if the ICC card is a test card
     */
    public boolean isTestIccCard(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return TelephonyManagerEx.getDefault().isTestIccCard(simId);
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- isTestIccCard()");
            return false;
        }
    }

    /**
     * refer to getCallState();
     */
     public int getCallStateGemini(int simId) throws android.os.RemoteException {
         if (EncapsulationConstant.USE_MTK_PLATFORM) {
             ITelephonyEx iTelephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
             if(iTelephonyEx != null) {
                 return iTelephonyEx.getCallState(simId);
             }
         }
             return 0;
         }

    /**
     * Check to see if the radio is on or not.
     * @return returns true if the radio is on.
     */
    public boolean isRadioOn(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephonyEx.isRadioOn(simId);
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- isRadioOn(int)");
            return false;
        }
    }

    /**
     * Returns the IccCard type of Gemini phone. Return "SIM" for SIM card or "USIM" for USIM card.
     */
    public String getIccCardType(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return TelephonyManagerEx.getDefault().getIccCardType(simId);
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- getIccCardTypeGemini(int)");
            return null;
        }
    }

    /**
    * Returns true if SIM card inserted
     * This API is valid even if airplane mode is on
    */
    public boolean isSimInsert(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return TelephonyManagerEx.getDefault().hasIccCard(simId);
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- isSimInsert(int)");
            return false;
        }
    }

    /**
      * Returns the network type
      */
    public int getNetworkTypeGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getNetworkTypeGemini(simId);
        } else {
            return 0;
        }
    }

    public boolean isDataConnectivityPossibleGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephonyEx.isDataConnectivityPossibleGemini(simId);
        } else {
            return false;
        }
    }

    public int getDataStateGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephonyEx.getDataState(simId);
        } else {
            return 0;
        }
    }

    public int getDataActivityGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephonyEx.getDataActivity(simId);
        } else {
            return 0;
        }
    }

   /**
     *get the services state for specified SIM
     * @param simId Indicate which sim(slot) to query
     * @return sim indicator state.
     *
    */
   public int getSimIndicatorStateGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephonyEx.getSimIndicatorState(simId);
       } else {
           return 0;
       }
   }

   /**
     *get the network service state for default SIM
     * @return service state.
     *
    */
   public Bundle getServiceState() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getServiceState();
       } else {
           return null;
       }
   }

   /**
     * get the network service state for specified SIM
     * @param simId Indicate which sim(slot) to query
     * @return service state.
     *
    */
   public Bundle getServiceStateGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephonyEx.getServiceState(simId);
       } else {
           return null;
       }
   }

   public String getScAddressGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephonyEx.getScAddressGemini(simId);
       } else {
           return null;
       }
   }

   public void setScAddressGemini(String scAddr, int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           sTelephonyEx.setScAddressGemini(scAddr, simId);
       } else {
       }
   }

   public int get3GCapabilitySIM() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephonyEx.get3GCapabilitySIM();
       } else {
           MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- get3GCapabilitySIM()");
           return 0;
       }
   }

    public int getCallState() throws android.os.RemoteException {
        return sTelephony.getCallState();
    }

    public int getDataState() throws android.os.RemoteException {
        return sTelephony.getDataState();
    }

    /**
     * Return true if an ICC card is present
     * This API always return false if airplane mode is on.
     */
    public boolean hasIccCard() throws android.os.RemoteException {
        return sTelephony.hasIccCard();
    }

    public boolean isRadioOn() throws android.os.RemoteException {
        return sTelephony.isRadioOn();
    }
}
