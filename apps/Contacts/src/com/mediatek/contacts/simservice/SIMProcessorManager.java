package com.mediatek.contacts.simservice;

import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.contacts.common.vcard.ProcessorBase;
import com.mediatek.contacts.simservice.SIMServiceUtils.SIMProcessorState;
import com.mediatek.contacts.util.LogUtils;

public class SIMProcessorManager implements SIMProcessorState {
    private static final String TAG = "SIMProcessorManager";

    public interface ProcessorManagerListener {
        public void addProcessor(long scheduleTime, ProcessorBase processor);
        public void onAllProcessorsFinished();
    }

    public interface ProcessorCompleteListener {
        public void onProcessorCompleted(Intent intent);
    }

    private ProcessorManagerListener mListener;
    private Handler mHandler;
    private ConcurrentHashMap<Integer, SIMProcessorBase> mImportRemoveProcessors;
    private ConcurrentHashMap<Integer, SIMProcessorBase> mOtherProcessors;

    private static final int MSG_SEND_STOP_SERVICE = 1;

    // Out of 30s hasn't new tasks and all tasks have completed, will stop service.
    private static final int DELAY_MILLIS_STOP_SEVICE = 30000;

////////////////////////////Public funtion///////////////////////////////////////////////////////

    public SIMProcessorManager(Context context, ProcessorManagerListener listener) {
        mListener = listener;
        mImportRemoveProcessors = new ConcurrentHashMap<Integer, SIMProcessorBase>();
        mOtherProcessors = new ConcurrentHashMap<Integer, SIMProcessorBase>();
        SIMServiceUtils.setSIMProcessorState(this);
        mHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_SEND_STOP_SERVICE:
                    LogUtils.d(TAG, "handleMessage MSG_SEND_STOP_SERVICE");
                    callStopService();
                    break;
                default:
                    break;
                }
            }
        };
    }

    public void handleProcessor(Context context,int slotId, int workType, Intent intent) {
        LogUtils.d(TAG, "[handleProcessor] slotId=" + slotId + ",time=" + System.currentTimeMillis());
        SIMProcessorBase processor = createProcessor(context, slotId, workType, intent);
        if (processor != null && mListener != null) {
            LogUtils.d(TAG, "[handleProcessor]Add processor [slotId=" + 
                    slotId + "] to threadPool.");
            mListener.addProcessor(/*1000 + slotId * 300*/0, processor);
        }
    }
    
    public boolean isImportRemoveRunning(int slotId) {
        if ((mImportRemoveProcessors != null) && (mImportRemoveProcessors.containsKey(slotId))) {
            SIMProcessorBase processor = mImportRemoveProcessors.get(slotId);
            if (processor == null) {
                LogUtils.i(TAG, "[isImportRemoveRunning]processor is null, return false.");
                return false;
            }
            if (processor.isRunning()) {
                LogUtils.i(TAG, "[isImportRemoveRunning]has exist running processor, return true.");
                return true;
            }
        }

        return false;
    }

////////////////////////////private funtion///////////////////////////////////////////////////////

    private SIMProcessorBase createProcessor(Context context,int slotId, int workType, Intent intent) {
        SIMProcessorBase processor = null;
        /**
         * [ALPS01224227]the mImportRemoveProcessors is likely to be accessed by main thread and sub thread
         * at the same time, we should protect the race condition
         */
        synchronized (mProcessorRemoveLock) {
            if (mImportRemoveProcessors.containsKey(slotId)) { // the rule to check whether or not create new processor 
                processor = mImportRemoveProcessors.get(slotId);
                Log.v(TAG, "[createProcessor] processor.getType() = " + processor.getType() + " workType = " + workType);
                if (processor != null && (workType == SIMServiceUtils.SERVICE_WORK_IMPORT
                        || workType == SIMServiceUtils.SERVICE_WORK_REMOVE)) {
                    if (processor.isRunning() && processor.getType() == workType) {
                        LogUtils.d(TAG, "[createProcessor]has exist running processor, return null.");
                        return null;
                    }
                    processor.cancel(false);
                    mImportRemoveProcessors.remove(slotId);
                }
            } else {
                LogUtils.d(TAG, "[createProcessor]no processor for slot " + slotId);
            }

            processor = createProcessor(context, slotId, workType, intent, mProcessoListener);

            if (workType == SIMServiceUtils.SERVICE_WORK_IMPORT
                    || workType == SIMServiceUtils.SERVICE_WORK_REMOVE) {
                mImportRemoveProcessors.put(slotId, processor);
            } else {
                mOtherProcessors.put(slotId, processor);
            }
        }

        return processor;
    }

    private SIMProcessorBase createProcessor(Context context,int slotId, int workType, Intent intent, ProcessorCompleteListener listener) {
        Log.v(TAG, "[createProcessor] create new processor for slot: " + slotId + ", workType: " + workType);
        SIMProcessorBase processor = null;

        if (workType == SIMServiceUtils.SERVICE_WORK_IMPORT) {
            processor = new SIMImportProcessor(context, slotId, intent, listener);
        } else if (workType == SIMServiceUtils.SERVICE_WORK_REMOVE) {
            processor = new SIMRemoveProcessor(context, slotId, intent, listener);
        } else if (workType == SIMServiceUtils.SERVICE_WORK_EDIT) {
            processor = new SIMEditProcessor(context, slotId, intent, listener);
        } else if (workType == SIMServiceUtils.SERVICE_WORK_DELETE) {
            processor = new SIMDeleteProcessor(context, slotId, intent, listener);
        }

        return processor;
    }

    private ProcessorCompleteListener mProcessoListener = new ProcessorCompleteListener() {

        @Override
        public void onProcessorCompleted(Intent intent) {
            if (intent != null) {
                int slotId = intent.getIntExtra(SIMServiceUtils.SERVICE_SLOT_KEY, 0);
                int workType = intent.getIntExtra(SIMServiceUtils.SERVICE_WORK_TYPE, -1);
                LogUtils.d(TAG, "[onProcessorCompleted] slotId = " + slotId + " time=" + System.currentTimeMillis() + ", workType = " + workType);
                /**
                 * [ALPS01224227]the mImportRemoveProcessors is likely to be accessed by main thread and sub thread
                 * at the same time, we should protect the race condition
                 */
                synchronized (mProcessorRemoveLock) {
                    if ((workType == SIMServiceUtils.SERVICE_WORK_IMPORT
                            || workType == SIMServiceUtils.SERVICE_WORK_REMOVE) 
                            && mImportRemoveProcessors.containsKey(slotId)) {
                        LogUtils.d(TAG, "[onProcessorCompleted] remove import/remove processor slotId=" + slotId);
                        /**
                         * [ALPS01224227]when we're going to remove the processor, in seldom condition, it might have already
                         * removed and replaced with another processor. in this case, we should not remove it any more.
                         */
                        if (mImportRemoveProcessors.get(slotId).identifyIntent(intent)) {
                            mImportRemoveProcessors.remove(slotId);
                            checkStopService();
                        } else {
                            LogUtils.w(TAG, "[onProcessorCompleted] race condition, current i/r processor has already removed by other thread(s)");
                        }
                    } else if (mOtherProcessors.containsKey(slotId)){
                        Log.d(TAG, "[onProcessorCompleted] remove other processor slotId=" + slotId);
                        /**
                         * [ALPS01224227]when we're going to remove the processor, in seldom condition, it might have already
                         * removed and replaced with another processor. in this case, we should not remove it any more.
                         */
                        if (mOtherProcessors.get(slotId).identifyIntent(intent)) {
                            mOtherProcessors.remove(slotId);
                            checkStopService();
                        } else {
                            LogUtils.w(TAG, "[onProcessorCompleted] race condition, current other processor has already removed by other thread(s)");
                        }
                    } else {
                        LogUtils.w(TAG, "[onProcessorCompleted] slotId processor not found");
                    }
                }
            }
        }
    };

    private void checkStopService() {
        Log.v(TAG, "[checkStopService]");
        if (mImportRemoveProcessors.size() == 0 && mOtherProcessors.size() == 0) {
            if (mHandler != null) {
                Log.v(TAG, "[checkStopService] send stop service message.");
                mHandler.removeMessages(MSG_SEND_STOP_SERVICE);
                mHandler.sendEmptyMessageDelayed(MSG_SEND_STOP_SERVICE, DELAY_MILLIS_STOP_SEVICE);
            }
        }
    }

    private void callStopService() {
        LogUtils.i(TAG, "[callStopService]");
        if (mListener != null && mImportRemoveProcessors.size() == 0 && mOtherProcessors.size() == 0) {
            mListener.onAllProcessorsFinished();
        }
    }

    /**
     * [ALPS01224227]the lock for synchronized
     */
    private final Object mProcessorRemoveLock = new Object();

}
