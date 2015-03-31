package com.mediatek.contacts.simservice;

import com.mediatek.contacts.simservice.SIMServiceUtils;
import com.mediatek.contacts.simservice.SIMProcessorManager.ProcessorCompleteListener;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.LogUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.contacts.ContactSaveService;
import com.android.contacts.interactions.ContactDeletionInteraction;

public class SIMDeleteProcessor extends SIMProcessorBase {
    private static final String TAG = "SIMDeleteProcessor";

    private static Listener mListener = null;

    private Uri mSimUri = null;
    private Uri mLocalContactUri = null;
    private String mSimWhere = null;
    private Context mContext;
    private Intent mIntent;
    private int mSlotId = SlotUtils.getNonSlotId();

    public final static String SIM_WHERE = "mSimWhere";
    public final static String LOCAL_CONTACT_URI = "local_contact_uri";

    public interface Listener {
        public void onSIMDeleteFailed();
        public void onSIMDeleteCompleted();
    }

    public static void registerListener(Listener listener) {
        if (listener instanceof ContactDeletionInteraction) {
            LogUtils.d(TAG, "[registerListener]listener added to SIMDeleteProcessor:" + listener);
            mListener = listener;
        }
    }

    public static void unregisterListener(Listener listener) {
        LogUtils.d(TAG, "[unregisterListener]listener removed from SIMDeleteProcessor: " + listener);
        mListener = null;
    }

    public SIMDeleteProcessor(Context context, int slotId, Intent intent,
            ProcessorCompleteListener listener) {
        super(intent, listener);
        mContext = context;
        mSlotId = slotId;
        mIntent = intent;
    }

    @Override
    public int getType() {
        return SIMServiceUtils.SERVICE_WORK_DELETE;
    }

    @Override
    public void doWork() {
        if (isCancelled()) {
            LogUtils.w(TAG, "[dowork]cancel remove work. Thread id = " + Thread.currentThread().getId());
            return;
        }
        mSimUri = mIntent.getData();
        mSimWhere = mIntent.getStringExtra(SIM_WHERE);
        mLocalContactUri = mIntent.getParcelableExtra(LOCAL_CONTACT_URI);
        if (mContext.getContentResolver().delete(mSimUri, mSimWhere, null) <= 0) {
            LogUtils.i(TAG, "[doWork] Delete SIM contact failed");
            if (mListener != null) {
                mListener.onSIMDeleteFailed();
            }
        } else {
            mContext.startService(ContactSaveService.createDeleteContactIntent(mContext, mLocalContactUri));
            if (mListener != null) {
                mListener.onSIMDeleteCompleted();
            }
        }
    }
}
