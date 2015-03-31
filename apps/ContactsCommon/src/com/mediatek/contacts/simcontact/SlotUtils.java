package com.mediatek.contacts.simcontact;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.PhoneConstants;
import com.android.contacts.common.R;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * M: [Gemini+] slot helper class. all slot related method placed here.
 */
public final class SlotUtils {
    private static final String TAG = "SlotUtils";
    
    private static final int PHONE_SLOT_NUM = PhoneConstants.GEMINI_SIM_NUM;
    private static final int FIRST_SLOT_ID = PhoneConstants.GEMINI_SIM_1;

    private SlotUtils() {
    }

    /**
     * M: [Gemini+] each slot information defined in this class
     */
    private static final class SlotInfo {

        private static final String SIM_PHONE_BOOK_SERVICE_NAME_FOR_SINGLE_SLOT = "simphonebook";
        private static final String ICC_SDN_URI_FOR_SINGLE_SLOT = "content://icc/sdn";
        private static final String ICC_ADN_URI_FOR_SINGLE_SLOT = "content://icc/adn";
        private static final String ICC_PBR_URI_FOR_SINGLE_SLOT = "content://icc/pbr";

        int mSlotId;
        Uri mIccUri;
        Uri mIccUsimUri;
        Uri mSdnUri;
        String mVoiceMailNumber;
        String mSimPhoneBookServiceName;
        boolean mIsSlotServiceRunning = false;
        int mResId;
        PhbInfoWrapper mPhbInfo;

        public SlotInfo(int slotId) {
            mSlotId = slotId;
            generateIccUri();
            generateIccUsimUri();
            generateSdnUri();
            generateSimPhoneBook();
            updateVoiceMailNumber();
            generateResId();
            mPhbInfo = new PhbInfoWrapper(slotId);
        }

        /**
         * TODO: the resource should be limited to only one string
         */
        private void generateResId() {
            switch (mSlotId) {
            case 0:
                mResId = R.string.sim1;
                break;
            case 1:
                mResId = R.string.sim2;
                break;
            case 2:
                mResId = R.string.sim3;
                break;
            case 3:
                mResId = R.string.sim4;
                break;
            default:
                LogUtils.e(TAG, "[generateResId]no res for slot:" + mSlotId);
            }
        }

        /**
         * slot 0 ==> simphonebook slot 1 ==> simphonebook2
         */
        private void generateSimPhoneBook() {
            mSimPhoneBookServiceName = SIM_PHONE_BOOK_SERVICE_NAME_FOR_SINGLE_SLOT;
            if (mSlotId > 0) {
                mSimPhoneBookServiceName = mSimPhoneBookServiceName + (mSlotId + 1);
            }
        }

        public String getSimPhoneBookServiceName() {
            return mSimPhoneBookServiceName;
        }

        public void updateVoiceMailNumber() {
            if (SlotUtils.isGeminiEnabled()) {
                mVoiceMailNumber = TelephonyManagerEx.getDefault().getVoiceMailNumber(mSlotId);
            } else {
                mVoiceMailNumber = TelephonyManager.getDefault().getVoiceMailNumber();
            }
        }

        public String getVoiceMailNumber() {
            return mVoiceMailNumber;
        }

        private void generateSdnUri() {
            String str = ICC_SDN_URI_FOR_SINGLE_SLOT;
            if (isGeminiEnabled()) {
                // like:"content://icc/sdn2"
                str += (mSlotId + 1);
            }
            mSdnUri = Uri.parse(str);
        }

        private void generateIccUri() {
            String str = ICC_ADN_URI_FOR_SINGLE_SLOT;
            if (isGeminiEnabled()) {
                // like:"content://icc/adn2"
                str += (mSlotId + 1);
            }
            mIccUri = Uri.parse(str);
        }

        private void generateIccUsimUri() {
            String str = ICC_PBR_URI_FOR_SINGLE_SLOT;
            if (isGeminiEnabled()) {
                // like:"content://icc/pbr2"
                str += (mSlotId + 1);
            }
            mIccUsimUri = Uri.parse(str);
        }

        public void updateSimServiceRunningState(boolean isRunning) {
            LogUtils.i(TAG, "[updateSimServiceRunningState]slotid: " + mSlotId + 
                    ",service running state changed from " + mIsSlotServiceRunning + " to "
                    + isRunning);
            mIsSlotServiceRunning = isRunning;
        }

        public boolean isSimServiceRunning() {
            return mIsSlotServiceRunning;
        }

        public Uri getIccUri() {
            return SimCardUtils.isSimUsimType(mSlotId) ? mIccUsimUri : mIccUri;
        }

        public Uri getSdnUri() {
            return mSdnUri;
        }

        public int getResId() {
            return mResId;
        }
    }

    private final static class PhbInfoWrapper {
        private int mSlotId = getNonSlotId();
        private int mUsimGroupMaxNameLength;
        private int mUsimGroupMaxCount;
        private int mUsimAnrCount;
        private int mUsimEmailCount;
        private boolean mInitialized;

        public PhbInfoWrapper(int slotId) {
            mSlotId = slotId;
            resetPhbInfo();
        }

        public void resetPhbInfo() {
            mUsimGroupMaxNameLength = -1;
            mUsimGroupMaxCount = 0;
            mUsimAnrCount = 0;
            mUsimEmailCount = 0;
            mInitialized = false;
        }

        public void refreshPhbInfo() {
            LogUtils.i(TAG, "[refreshPhbInfo]refreshing phb info for slot: " + mSlotId);
            if (!SimCardUtils.isSimInserted(mSlotId)) {
                LogUtils.i(TAG, "[refreshPhbInfo]sim not insert, refresh aborted. slot: " + mSlotId);
                mInitialized = true;
                return;
            }
            if (!SimCardUtils.isPhoneBookReady(mSlotId)) {
                LogUtils.e(TAG, "[refreshPhbInfo]phb not ready, refresh aborted. slot: " + mSlotId);
                mInitialized = false;
                return;
            }
            ///TODO: currently, only Usim is necessary for phb infos.
            if (!SimCardUtils.isSimUsimType(mSlotId)) {
                LogUtils.i(TAG, "[refreshPhbInfo]not usim phb, nothing to refresh, keep default, slot: " + mSlotId);
                mInitialized = true;
                return;
            }

            String serviceName = getSimPhoneBookServiceNameForSlot(mSlotId);
            try {
                final IIccPhoneBook iIccPhb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService(serviceName));
                mUsimGroupMaxNameLength = iIccPhb.getUsimGrpMaxNameLen();
                mUsimGroupMaxCount = iIccPhb.getUsimGrpMaxCount();
                mUsimAnrCount = iIccPhb.getAnrCount();
                mUsimEmailCount = iIccPhb.getEmailCount();
            } catch (RemoteException e) {
                LogUtils.e(TAG, "[refreshPhbInfo]Exception happened when refreshing phb info");
                e.printStackTrace();
                mInitialized = false;
                return;
            }
            mInitialized = true;
            LogUtils.i(TAG, "[refreshPhbInfo]refreshing done, UsimGroupMaxNameLenght = " + mUsimGroupMaxNameLength
                    + ", UsimGroupMaxCount = " + mUsimGroupMaxCount + ", UsimAnrCount = " + mUsimAnrCount
                    + ", UsimEmailCount = " + mUsimEmailCount);
        }

        public int getUsimGroupMaxNameLength() {
            if (!mInitialized) {
                refreshPhbInfo();
            }
            LogUtils.d(TAG, "[getUsimGroupMaxNameLength] slotId = " + mSlotId + ", length = " + mUsimGroupMaxNameLength);
            return mUsimGroupMaxNameLength;
        }

        public int getUsimGroupMaxCount() {
            if (!mInitialized) {
                refreshPhbInfo();
            }
            LogUtils.d(TAG, "[getUsimGroupMaxCount] slotId = " + mSlotId + ", count = " + mUsimGroupMaxCount);
            return mUsimGroupMaxCount;
        }

        public int getUsimAnrCount() {
            if (!mInitialized) {
                refreshPhbInfo();
            }
            LogUtils.d(TAG, "[getUsimAnrCount] slotId = " + mSlotId + ", count = " + mUsimAnrCount);
            return mUsimAnrCount;
        }

        public int getUsimEmailCount() {
            if (!mInitialized) {
                refreshPhbInfo();
            }
            LogUtils.d(TAG, "[getUsimEmailCount] slotId = " + mSlotId + ", count = " + mUsimEmailCount);
            return mUsimEmailCount;
        }
    }

    @SuppressLint("UseSparseArrays")
    private static Map<Integer, SlotInfo> sSlotInfoMap = new HashMap<Integer, SlotInfo>();
    static {
        for (int i = 0; i < PHONE_SLOT_NUM; i++) {
            int slotId = FIRST_SLOT_ID + i;
            sSlotInfoMap.put(slotId, new SlotInfo(slotId));
        }
    }

    /**
     * M: [Gemini+] get the icc uri of the slot id
     * @param slotId
     * @return
     */
    public static Uri getSlotIccUri(int slotId) {
        SlotInfo slotInfo = sSlotInfoMap.get(slotId);
        if (slotInfo != null){
            return slotInfo.getIccUri();
        } else {
            LogUtils.w(TAG,"[getSlotIccUri],slotId:" + slotId);
            return null;
        }
    }

    /**
     * M: [Gemini+] get slot sdn uri
     * @param slotId
     * @return
     */
    public static Uri getSlotSdnUri(int slotId) {
        SlotInfo slotInfo = sSlotInfoMap.get(slotId);
        if (slotInfo != null){
            return slotInfo.getSdnUri();
        } else {
            LogUtils.w(TAG,"[getSlotSdnUri],slotId:" + slotId);
            return null;
        }
    }

    /**
     * M: get all slot Ids
     * @return the list contains all slot ids
     */
    public static List<Integer> getAllSlotIds() {
        return new ArrayList<Integer>(sSlotInfoMap.keySet());
    }

    /**
     * M: [Gemini+] get voice mail number for slot
     * @param slotId
     * @return string
     */
    public static String getVoiceMailNumberForSlot(int slotId) {
        if (isSlotValid(slotId)) {
            SlotInfo slotInfo = sSlotInfoMap.get(slotId);
            if (slotInfo != null){
                return slotInfo.getVoiceMailNumber();
            } else {
                LogUtils.w(TAG,"[getVoiceMailNumberForSlot],slotId:" + slotId);
                return null;
            }
        }
        
        LogUtils.d(TAG, "[getVoiceMailNumberForSlot] slot " + slotId + " is not valid!");
        return null;
    }

    /**
     * M: [Gemini+] update the saved voice mail number
     */
    public static void updateVoiceMailNumber() {
        for (SlotInfo slot : sSlotInfoMap.values()) {
            slot.updateVoiceMailNumber();
        }
    }

    /**
     * M: listen to all slots including Gemini+ a wrapper for
     * TelephonyManager.listen
     * 
     * @param context
     *            the context to get system service
     * @param listener
     *            {@link} PhoneStateListener
     * @param events
     *            {@link} TelephonyManager.listen events
     */
    public static void listenAllSlots(Context context, PhoneStateListener listener, int events) {
        if (SlotUtils.isGeminiEnabled()) {
            for (int slotId : getAllSlotIds()) {
                TelephonyManagerEx.getDefault().listen(listener, events, slotId);
            }
        } else {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(listener, events);
        }
    }

    /**
     * M: [Gemini+] get current device total slot count
     * @return count
     */
    public static int getSlotCount() {
        return sSlotInfoMap.size();
    }

    /**
     * M: [Gemini+] phone book service name by slotId
     * @param slotId
     * @return string
     */
    public static String getSimPhoneBookServiceNameForSlot(int slotId) {
        SlotInfo slotInfo = sSlotInfoMap.get(slotId);
        if (slotInfo != null){
            return slotInfo.getSimPhoneBookServiceName();
        } else {
            LogUtils.w(TAG,"[getSimPhoneBookServiceNameForSlot],slotId:" + slotId);
            return null;
        }
    }

    /**
     * M: [Gemini+] check whether the slot is valid
     * @param slotId
     * @return true if valid
     */
    public static boolean isSlotValid(int slotId) {
        boolean isValid = sSlotInfoMap.containsKey(slotId);
        if (!isValid) {
            LogUtils.w(TAG, "[isSlotValid]slot " + slotId + " is invalid!");
        }
        return isValid;
    }

    /**
     * M: [Gemini+] slot ids are defined in array like 0, 1, 2, ...
     * @return the first id of all slotIds
     */
    public static int getFirstSlotId() {
        return FIRST_SLOT_ID;
    }

    /**
     * M: [Gemini+] get an invalid slot id, to indicate that this is not a sim slot.
     * 
     * @return negative value
     */
    public static int getNonSlotId() {
        return -1;
    }

    /**
     * M: [Gemini+] in single card phone, the only slot has a slot id this method to
     * retrieve the id.
     * 
     * @return the only slot id of a single card phone
     */
    public static int getSingleSlotId() {
        return FIRST_SLOT_ID;
    }

    /**
     * M: [Gemini+] get string resource id for the corresponding slot id
     * @param slotId
     * @return
     */
    public static int getResIdForSlot(int slotId) {
        SlotInfo slotInfo = sSlotInfoMap.get(slotId);
        if (slotInfo != null){
            return slotInfo.getResId();
        } else {
            LogUtils.w(TAG,"[getResIdForSlot],slotId:" + slotId);
            return -1;
        }
    }

    /**
     * M: [Gemini+] resource is just string like "SIM1", "SIM2"
     * @param resId
     * @return if no slot matches, return NonSlotId
     */
    public static int getSlotIdFromSimResId(int resId) {
        for (int slotId : getAllSlotIds()) {
            if (sSlotInfoMap.get(slotId).mResId == resId) {
                return slotId;
            }
        }
        return getNonSlotId();
    }

    /**
     * M: [Gemini+] if gemini feature enabled on this device
     * @return
     */
    public static boolean isGeminiEnabled() {
        return FeatureOption.MTK_GEMINI_SUPPORT;
    }
    
    public static int getUsimGroupMaxNameLengthBySlot(int slotId) {
        return sSlotInfoMap.get(slotId).mPhbInfo.getUsimGroupMaxNameLength();
    }
    
    public static int getUsimGroupMaxCountBySlot(int slotId) {
        return sSlotInfoMap.get(slotId).mPhbInfo.getUsimGroupMaxCount();
    }
    
    public static int getUsimAnrCountBySlot(int slotId) {
        return sSlotInfoMap.get(slotId).mPhbInfo.getUsimAnrCount();
    }
    
    public static int getUsimEmailCountBySlot(int slotId) {
        return sSlotInfoMap.get(slotId).mPhbInfo.getUsimEmailCount();
    }
    
    /**
     * Time Consuming, run in background
     * to refresh the PHB info, read from IccPhb, might access Modem, so, would be time consuming.
     * this must be called, once PHB state changed.
     * 
     * @param slotId
     */
    public static void refreshPhbInfoBySlot(int slotId) {
        sSlotInfoMap.get(slotId).mPhbInfo.refreshPhbInfo();
    }
    
    /**
     * reset the PHB info cache to the un-init state.
     * this state means, any requirement trying to access the phb info, it would re-init
     * immediately.
     * 
     * @param slotId
     */
    public static void resetPhbInfoBySlot(int slotId) {
        sSlotInfoMap.get(slotId).mPhbInfo.resetPhbInfo();
    }
}
