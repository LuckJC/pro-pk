
package com.mediatek.contacts.list.service;

import android.content.BroadcastReceiver;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.util.CallerInfoCacheUtils;
import com.android.contacts.common.vcard.ProcessorBase;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.list.ContactsMultiDeletionFragment;
import com.mediatek.contacts.simservice.SIMServiceUtils;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.contacts.util.TimingStatistics;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DeleteProcessor extends ProcessorBase {
    private static final String TAG = "DeleteProcessor";

    private final MultiChoiceService mService;
    private final ContentResolver mResolver;
    private final List<MultiChoiceRequest> mRequests;
    private final int mJobId;
    private final MultiChoiceHandlerListener mListener;

    private PowerManager.WakeLock mWakeLock;

    private volatile boolean mCanceled;
    private volatile boolean mDone;
    private volatile boolean mIsRunning;

    private static final int MAX_OP_COUNT_IN_ONE_BATCH = 100;

    // change max count and max count in one batch for special operator 
    private static final int MAX_COUNT = 1551;
    private static final int MAX_COUNT_IN_ONE_BATCH = 50;

    public DeleteProcessor(final MultiChoiceService service,
            final MultiChoiceHandlerListener listener, final List<MultiChoiceRequest> requests,
            final int jobId) {
        mService = service;
        mResolver = mService.getContentResolver();
        mListener = listener;

        mRequests = requests;
        mJobId = jobId;

        final PowerManager powerManager = (PowerManager) mService.getApplicationContext()
                .getSystemService("power");
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        LogUtils.d(TAG, "[cancel]received cancel request,mDone = " + mDone
                + ",mCanceled = " + mCanceled + ",mIsRunning = " + mIsRunning);
        if (mDone || mCanceled) {
            return false;
        }
        
        mCanceled = true;
        if (!mIsRunning) {
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceled(MultiChoiceService.TYPE_DELETE, mJobId, -1, -1, -1);
        } else {
            /*
             * Bug Fix by Mediatek Begin.
             *   Original Android's code:
             *     xxx
             *   CR ID: ALPS00249590
             *   Descriptions: 
             */
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceling(MultiChoiceService.TYPE_DELETE, mJobId);
            /*
             * Bug Fix by Mediatek End.
             */
        }

        return true;
    }

    @Override
    public int getType() {
        return MultiChoiceService.TYPE_DELETE;
    }

    @Override
    public synchronized boolean isCancelled() {
        return mCanceled;
    }

    @Override
    public synchronized boolean isDone() {
        return mDone;
    }

    @Override
    public void run() {
        try {
            mIsRunning = true;
            mWakeLock.acquire();
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            registerReceiver();
            runInternal();
            unregisterReceiver();
        } finally {
            synchronized (this) {
                mDone = true;
            }
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    private void runInternal() {
        if (isCancelled()) {
            LogUtils.i(TAG, "[runInternal]Canceled before actually handling");
            return;
        }

        boolean succeessful = true;
        int totalItems = mRequests.size();
        int successfulItems = 0;
        int currentCount = 0;
        int iBatchDel = MAX_OP_COUNT_IN_ONE_BATCH;
        if (totalItems > MAX_COUNT) {
            iBatchDel = MAX_COUNT_IN_ONE_BATCH;
            LogUtils.i(TAG, "[runInternal]iBatchDel = " + iBatchDel);
        }
        long startTime = System.currentTimeMillis();
        final ArrayList<Long> contactIdsList = new ArrayList<Long>();
        int times = 0;
        /** M: Add idToSlotHashMap to save old request indicator. CR: 568004 @{ */
        HashMap<Long, Integer> idToSlotHashMap = new HashMap<Long, Integer>();
        int slot = -1;
        /** @} */
        boolean simServiceStarted = false;

        HashMap<Integer, Uri> delSimUriMap = new HashMap<Integer, Uri>();
        TimingStatistics iccProviderTiming = new TimingStatistics(DeleteProcessor.class.getSimpleName());
        TimingStatistics contactsProviderTiming = new TimingStatistics(DeleteProcessor.class.getSimpleName());
        for (MultiChoiceRequest request : mRequests) {
            if (mCanceled) {
                LogUtils.d(TAG, "[runInternal] run: mCanceled = true, break looper");
                break;
            }
            currentCount++;

            mListener.onProcessed(MultiChoiceService.TYPE_DELETE, mJobId, currentCount, totalItems,
                    request.mContactName);
            // delete contacts from sim card
            if (request.mIndicator > RawContacts.INDICATE_PHONE) {
                /** M: Just reset slot value when indicator gets changed. { */
                if (!idToSlotHashMap.containsKey(Long.valueOf(request.mIndicator))) {
                    ///M:fix CR ALPS01065879,sim Info Manager API Remove
                    SimInfoRecord simInfo = SimInfoManager.getSimInfoById(mService.getApplicationContext(), request.mIndicator);
                    if (simInfo == null) {
                        slot = -1;
                    } else {
                        slot = simInfo.mSimSlotId;
                    }
                    idToSlotHashMap.put(Long.valueOf(request.mIndicator), Integer.valueOf(slot));
                    LogUtils.d(TAG, "[runInternal]Indicator: " + request.mIndicator + ",slot: " + slot);
                } else {
                    slot = idToSlotHashMap.get(Long.valueOf(request.mIndicator)).intValue();
                }
                /** @} */
                if (mReveiced3GSwitch || !isReadyForDelete(slot)) {
                    LogUtils.d(TAG, "[runInternal] run: isReadyForDelete(" + slot + ") = false");
                    succeessful = false;
                    continue;
                }

                /// M: change for SIM Service refactoring
                if (simServiceStarted || !simServiceStarted && SIMServiceUtils.isServiceRunning(slot)) {
                    LogUtils.d(TAG, "[runInternal]run: sim service is running, we should skip all of sim contacts");
                    simServiceStarted = true;
                    succeessful = false;
                    continue;
                }

                Uri delSimUri = null;
                if (delSimUriMap.containsKey(slot)) {
                    delSimUri = delSimUriMap.get(slot);
                } else {
                    delSimUri = SimCardUtils.SimUri.getSimUri(slot);
                    delSimUriMap.put(slot, delSimUri);
                }

                String where = ("index = " + request.mSimIndex);
                
                iccProviderTiming.timingStart();
                int deleteCount = mResolver.delete(delSimUri, where, null);
                iccProviderTiming.timingEnd();
                if (deleteCount <= 0) {
                    LogUtils.d(TAG, "[runInternal] run: delete the sim contact failed");
                    succeessful = false;
                } else {
                    successfulItems++;
                    contactIdsList.add(Long.valueOf(request.mContactId));
                }
            } else {
                successfulItems++;
                contactIdsList.add(Long.valueOf(request.mContactId));
            }

            // delete contacts from database
            if (contactIdsList.size() >= iBatchDel) {
                contactsProviderTiming.timingStart();
                actualBatchDelete(contactIdsList);
                contactsProviderTiming.timingEnd();
                LogUtils.i(TAG, "[runInternal]the " + (++times) + " times iBatchDel = " + iBatchDel);
                contactIdsList.clear();
                if ((totalItems - currentCount) <= MAX_COUNT) {
                    iBatchDel = MAX_OP_COUNT_IN_ONE_BATCH;
                }
            }
        }

        if (contactIdsList.size() > 0) {
            contactsProviderTiming.timingStart();
            actualBatchDelete(contactIdsList);
            contactsProviderTiming.timingEnd();
            contactIdsList.clear();
        }

        LogUtils.i(TAG, "[runInternal]totaltime: " + (System.currentTimeMillis() - startTime));

        if (mCanceled) {
            LogUtils.d(TAG, "[runInternal]run: mCanceled = true, return");
            succeessful = false;
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceled(MultiChoiceService.TYPE_DELETE, mJobId, totalItems,
                    successfulItems, totalItems - successfulItems);
            return;
        }
        mService.handleFinishNotification(mJobId, succeessful);
        /** M: Sends an Intent, notifying CallerInfo cache should be updated  @{ */
        CallerInfoCacheUtils.sendUpdateCallerInfoCacheIntent(mService.getApplicationContext());
        /** @} */
        if (succeessful) {
            mListener.onFinished(MultiChoiceService.TYPE_DELETE, mJobId, totalItems);
        } else {
            mListener.onFailed(MultiChoiceService.TYPE_DELETE, mJobId, totalItems,
                    successfulItems, totalItems - successfulItems);
        }
        
        iccProviderTiming.log("runInternal():IccProviderTiming");
        contactsProviderTiming.log("runInternal():ContactsProviderTiming");
    }

    private int actualBatchDelete(ArrayList<Long> contactIdList) {
        LogUtils.d(TAG, "[actualBatchDelete]");
        if (contactIdList == null || contactIdList.size() == 0) {
            LogUtils.w(TAG, "[actualBatchDelete]input error,contactIdList = " + contactIdList);
            return 0;
        }
        
        final StringBuilder whereBuilder = new StringBuilder();
        final ArrayList<String> whereArgs = new ArrayList<String>();
        final String[] questionMarks = new String[contactIdList.size()];
        for (long contactId : contactIdList) {
            whereArgs.add(String.valueOf(contactId));
        }
        Arrays.fill(questionMarks, "?");
        whereBuilder.append(Contacts._ID + " IN (").
                append(TextUtils.join(",", questionMarks)).
                append(")");

        int deleteCount = mResolver.delete(Contacts.CONTENT_URI.buildUpon().appendQueryParameter(
                "batch", "true").build(), whereBuilder.toString(), whereArgs.toArray(new String[0]));
        LogUtils.d(TAG, "[actualBatchDelete]deleteCount:" + deleteCount + " Contacts");
        return deleteCount;
    }

    /**
     * -1 -- for single SIM
     * 0  -- for gemini SIM 1
     * 1  -- for gemini SIM 2
     */
    private boolean isReadyForDelete(int slotId) {
        return SimCardUtils.isSimStateIdle(slotId);
    }

    private void registerReceiver() {
        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(TelephonyIntents.EVENT_PRE_3G_SWITCH);
            mService.getApplicationContext().registerReceiver(mModemSwitchListener, intentFilter);
        }
    }
    
    private void unregisterReceiver() {
        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            mService.getApplicationContext().unregisterReceiver(mModemSwitchListener);
        }
    }
    
    private Boolean mReveiced3GSwitch = false;
    
    private BroadcastReceiver mModemSwitchListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TelephonyIntents.EVENT_PRE_3G_SWITCH)) {
                LogUtils.i(TAG, "[onReceive]receive 3G Switch ...");
                mReveiced3GSwitch = true;
            }
        }
    };
}
