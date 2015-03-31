package com.mediatek.contacts.simservice;

import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Message;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.contacts.simservice.SIMServiceUtils;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroup;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;

public class SIMServiceUtils {
    private static final String TAG = "SIMServiceUtils";
    private static SIMProcessorState mSIMProcessorState;
    
    public static final String ACTION_PHB_LOAD_FINISHED = "com.android.contacts.ACTION_PHB_LOAD_FINISHED";

    public static final String SERVICE_SLOT_KEY = "which_slot";
    public static final String SERVICE_WORK_TYPE = "work_type";

    public static final int SERVICE_WORK_NONE = 0;
    public static final int SERVICE_WORK_IMPORT = 1;
    public static final int SERVICE_WORK_REMOVE = 2;
    public static final int SERVICE_WORK_EDIT= 3;
    public static final int SERVICE_WORK_DELETE= 4;
    public static final int SERVICE_WORK_UNKNOWN = -1;
    public static final int SERVICE_IDLE = 0;

    public static final int SERVICE_DELETE_CONTACTS = 1;
    public static final int SERVICE_QUERY_SIM = 2;
    public static final int SERVICE_IMPORT_CONTACTS = 3;
    
    public static final int SIM_TYPE_SIM = SimCardUtils.SimType.SIM_TYPE_SIM;
    public static final int SIM_TYPE_USIM = SimCardUtils.SimType.SIM_TYPE_USIM;
    public static final int SIM_TYPE_UIM = SimCardUtils.SimType.SIM_TYPE_UIM;
    public static final int SIM_TYPE_UNKNOWN = SimCardUtils.SimType.SIM_TYPE_UNKNOWN;

    public static final int TYPE_IMPORT = 1;
    public static final int TYPE_REMOVE = 2;
    
    public static class ServiceWorkData {
        public int mSlotId = -1;
        public int mSimId = -1;
        public int mSimType = SIM_TYPE_UNKNOWN;
        public Cursor mSimCursor = null;

        ServiceWorkData() {
        }

        ServiceWorkData(int slotId, int simId, int simType, Cursor simCursor) {
            mSlotId = slotId;
            mSimId = simId;
            mSimType = simType;
            mSimCursor = simCursor;
        }
    }

    public static void deleteSimContact(Context context, int slotId) {
        // Check SIM state here to make sure whether it needs to remove all SIM or not
        int j = 10;
        boolean simInfoReady = SimCardUtils.isSimInfoReady();
        LogUtils.d(TAG,"[deleteSimContact] simInfoReady: " + simInfoReady);
        while (j > 0) {
            if (!simInfoReady) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    LogUtils.w(TAG, "[deleteSimContact]catched excepiotn: " + e);
                }
                simInfoReady = SimCardUtils.isSimInfoReady();
            } else {
                break;
            }
            j--;
        }

        int currSimId = 0;
        List<SimInfoRecord> simList = SimInfoManager.getAllSimInfoList(context);
        String selection = null;
        StringBuilder delSelection = new StringBuilder();
        for (SimInfoRecord simInfo : simList) {
            if (simInfo.mSimSlotId == slotId) {
                currSimId = (int) simInfo.mSimInfoId;
            }
            if (simInfo.mSimSlotId == -1) {
                delSelection.append(simInfo.mSimInfoId).append(",");
            }
        }

        if (delSelection.length() > 0) {
            delSelection.deleteCharAt(delSelection.length() - 1);
        }
        if (currSimId == 0 && slotId >= 0) {
            if (delSelection.length() > 0) {
                selection = delSelection.toString();
            }
        } else {
            selection = currSimId
                    + ((delSelection.length() > 0) ? ("," + delSelection.toString()) : "");
        }

        LogUtils.d(TAG,"[deleteSimContact]slotId:" + slotId + "|selection:" + selection);

        if (!TextUtils.isEmpty(selection)) {
            int count = context.getContentResolver().delete(
                    RawContacts.CONTENT_URI.buildUpon().appendQueryParameter("sim", "true")
                            .build(),
                    RawContacts.INDICATE_PHONE_SIM + " IN (" + selection + ")", null);
            LogUtils.d(TAG,"[deleteSimContact]count:" + count);
        }

        context.getContentResolver().delete(
                Groups.CONTENT_URI,
                Groups.ACCOUNT_NAME + "='" + "USIM" + slotId + "' AND "
                        + Groups.ACCOUNT_TYPE + "='USIM Account'", null);
        for (int otherSlotId : SlotUtils.getAllSlotIds()) {
            if (otherSlotId != slotId && !SimCardUtils.isSimInserted(otherSlotId)) {
                context.getContentResolver().delete(
                        Groups.CONTENT_URI,
                        Groups.ACCOUNT_NAME + "='" + "USIM" + otherSlotId + "' AND "
                                + Groups.ACCOUNT_TYPE + "='USIM Account'", null);
            }
        }
        sendFinishIntent(context, slotId);
    }

    /**
     * check PhoneBook State is ready if ready, then return true.
     * 
     * @param slotId
     * @return
     */
    static boolean checkPhoneBookState(final int slotId) {
        return SimCardUtils.isPhoneBookReady(slotId);
    }

    static void sendFinishIntent(Context context, int slotId) {
        LogUtils.i(TAG,"[sendFinishIntent]slotId:" + slotId);
        Intent intent = new Intent(ACTION_PHB_LOAD_FINISHED);
        intent.putExtra("simId", slotId);
        intent.putExtra("slotId", slotId);
        context.sendBroadcast(intent);
    }

    public static boolean isServiceRunning(int slotId) {
        if (mSIMProcessorState != null) {
            return mSIMProcessorState.isImportRemoveRunning(slotId);
        }
        
        return false;
    }

    public static int getServiceState(int slotId) {
        return 0;
    }

    public static void setSIMProcessorState(SIMProcessorState processorState) {
        mSIMProcessorState = processorState;
    }

    public interface SIMProcessorState {
        public boolean isImportRemoveRunning(int slotId);
    }
}
