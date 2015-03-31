package com.mediatek.contacts.simservice;

import com.mediatek.contacts.simservice.SIMProcessorManager.ProcessorCompleteListener;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.contacts.util.LogUtils;

public class SIMRemoveProcessor extends SIMProcessorBase {
    private static final String TAG = "SIMRemoveProcessor";
    private int mSlotId;
    private Context mContext;

    public SIMRemoveProcessor(Context context, int slotId, Intent intent,
            ProcessorCompleteListener listener) {
        super(intent, listener);
        mContext = context;
        mSlotId = slotId;
    }

    @Override
    public int getType() {
        return SIMServiceUtils.TYPE_REMOVE;
    }

    @Override
    public void doWork() {
        if (isCancelled()) {
            LogUtils.d(TAG, "[doWork]cancel remove work. Thread id=" + Thread.currentThread().getId());
            return;
        }
        SIMServiceUtils.deleteSimContact(mContext, mSlotId);
    }
}
