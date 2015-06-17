/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.transaction;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
// KK migration, for default MMS function.
// The changes in this project update the in-box SMS/MMS app to use the new
// intents for SMS/MMS delivery.
import static android.provider.Telephony.Sms.Intents.SMS_DELIVER_ACTION;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;

import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.ui.ClassZeroActivity;
import com.android.mms.util.Recycler;
import com.android.mms.util.SendingProgressTokenManager;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.MmsException;
import android.database.sqlite.SqliteWrapper;

import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.provider.Telephony.Sms.Outbox;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.mms.R;
import com.android.mms.LogTag;

/// M:
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.util.AndroidException;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.NotificationPreferenceActivity;
import com.android.mms.ui.SmsPreferenceActivity;
import com.android.mms.util.ThreadCountManager;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyIntents;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.common.mms.EncapsulatedIConcatenatedSmsFwkExt;
import com.mediatek.encapsulation.MmsLog;

//for Plugin
import com.android.mms.MmsPluginManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSimInfoManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsMessageEx;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
import com.mediatek.mms.ext.DisplayClassZeroMessageImpl;
import com.mediatek.mms.ext.IDisplayClassZeroMessage;
import com.mediatek.mms.ext.IMissedSmsReceiver;
import com.mediatek.mms.ext.IMissedSmsReceiverHost;
import com.mediatek.mms.ext.IMmsDialogNotify;
/// M: ALPS00440523, set service to foreground @ {
import com.mediatek.mms.ext.IMmsTransaction;
/// @}
import com.mediatek.mms.ext.ISmsReceiver;
import com.mediatek.mms.ext.MissedSmsReceiverImpl;

// add by puyong@20140915 for 预置小区广播
import android.provider.Telephony;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.PhoneConstants;
import android.content.ContentValues;
import android.database.Cursor;
import com.mediatek.telephony.SmsManagerEx;

/**
 * This service essentially plays the role of a "worker thread", allowing us to store
 * incoming messages to the database, update notifications, etc. without blocking the
 * main thread that SmsReceiver runs on.
 */
public class SmsReceiverService extends Service implements IMissedSmsReceiverHost {
    private static final String TAG = "SmsReceiverService";

    private ServiceHandler mServiceHandler;
    //private Looper mServiceLooper;
    static HandlerThread sSmsTHandler = null;

    private boolean mSending;

    public static final String MESSAGE_SENT_ACTION =
        "com.android.mms.transaction.MESSAGE_SENT";

    // Indicates next message can be picked up and sent out.
    public static final String EXTRA_MESSAGE_SENT_SEND_NEXT ="SendNextMsg";

    public static final String ACTION_SEND_MESSAGE =
        "com.android.mms.transaction.SEND_MESSAGE";

    // This must match the column IDs below.
    private static final String[] SEND_PROJECTION = new String[] {
        Sms._ID,        //0
        Sms.THREAD_ID,  //1
        Sms.ADDRESS,    //2
        Sms.BODY,       //3
        Sms.STATUS,     //4

    };

	// add by puyong@20140915 for 预置小区广播频道
	private static final int MESSAGE_SET_STATE = 33;
	private static final int MESSAGE_SET_CONFIG = 32;
	private static final String KEYID = "_id";
	private static final String NAME = "name";
	private static final String NUMBER = "number";
	private static final String ENABLE = "enable";
	private static final Uri CHANNEL_URI = Uri.parse("content://cb/channel");
	private static final Uri CHANNEL_URI1 = Uri.parse("content://cb/channel1");
	private static final Uri CHANNEL_URI2 = Uri.parse("content://cb/channel2");

    /// M:Code analyze 001, override handleMessage @{
    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RADIO_NOT_AVILABLE:
                    Toast.makeText(SmsReceiverService.this,
                        getString(R.string.message_queued), Toast.LENGTH_SHORT)
                       .show();
                    break;
                case FDN_CHECK_FAIL:
                    Toast.makeText(SmsReceiverService.this, R.string.fdn_enabled,
                         Toast.LENGTH_LONG).show();
                    break;
                default :
                    break;
            }
        }
    };
    /// @}

    // This must match SEND_PROJECTION.
    private static final int SEND_COLUMN_ID         = 0;
    private static final int SEND_COLUMN_THREAD_ID  = 1;
    private static final int SEND_COLUMN_ADDRESS    = 2;
    private static final int SEND_COLUMN_BODY       = 3;
    private static final int SEND_COLUMN_STATUS     = 4;

    private int mResultCode;
    /// M: add for ALPS00592464 begin @{
    private static HashSet<Integer> sRegSimIdSet = new HashSet<Integer>();
    /// @}

    /// M: new members
    private IDisplayClassZeroMessage mDisplayClassZeroPlugin = null;
    private IMissedSmsReceiver mMissedSmsReceiverPlugin = null;

    @Override
    public void onCreate() {
        // Temporarily removed for this duplicate message track down.
//        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
//            Log.v(TAG, "onCreate");
//        }

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        if (sSmsTHandler == null) {
            sSmsTHandler = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
            sSmsTHandler.start();
        }
        //mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(sSmsTHandler.getLooper());
        /// M: ALPS00440523, set service to foreground @ {
        IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
            .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);
        mmsTransactionPlugin.startServiceForeground(this);
        /// @}
        /// M:
        initPlugin(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Temporarily removed for this duplicate message track down.
        MmsLog.d(MmsApp.TXN_TAG, "onStartCommand SmsReceiverService");
        // KK migration, for default MMS function.
        // The changes in this project update the in-box SMS/MMS app to use the new
        // intents for SMS/MMS delivery.
        if (intent != null && SMS_DELIVER_ACTION.equals(intent.getAction())) {
            MmsLog.d(MmsApp.TXN_TAG, "Sms recevied and need not care mResultCode.");
        } else if (intent != null && TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(intent.getAction())) {
            MmsLog.d(MmsApp.TXN_TAG, "Sms service state changed and need not care mResultCode.");
        } else {
            mResultCode = intent != null ? intent.getIntExtra("result", 0) : 0;
        }
        /// M:Code analyze 007, print the log @{
        if (MESSAGE_SENT_ACTION.equals(intent.getAction())) {
            MmsLog.d(MmsApp.TXN_TAG, "Message Sent Result Code = " + mResultCode);
        }
        /// @}

        if (mResultCode != 0) {
            Log.v(TAG, "onStart: #" + startId + " mResultCode: " + mResultCode +
                    " = " + translateResultCode(mResultCode));
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
        return Service.START_NOT_STICKY;
    }

    private static String translateResultCode(int resultCode) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                return "Activity.RESULT_OK";
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                return "SmsManager.RESULT_ERROR_GENERIC_FAILURE";
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                return "SmsManager.RESULT_ERROR_RADIO_OFF";
            case SmsManager.RESULT_ERROR_NULL_PDU:
                return "SmsManager.RESULT_ERROR_NULL_PDU";
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                return "SmsManager.RESULT_ERROR_NO_SERVICE";
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                return "SmsManager.RESULT_ERROR_LIMIT_EXCEEDED";
            case SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE:
                return "SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE";
            default:
                return "Unknown error code";
        }
    }

    @Override
    public void onDestroy() {
        // Temporarily removed for this duplicate message track down.
//        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
//            Log.v(TAG, "onDestroy");
//        }
        /// M: ALPS00440523, set service to foreground @ {
        IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
            .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);
        mmsTransactionPlugin.stopServiceForeground(this);
        /// @}
        //mServiceLooper.quit();
        MmsLog.d(MmsApp.TXN_TAG, "onDestroy SmsReceiverService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        /**
         * Handle incoming transaction requests.
         * The incoming requests are initiated by the MMSC Server or by the MMS Client itself.
         */
        @Override
        public void handleMessage(Message msg) {
            /// M:
            MmsLog.d(MmsApp.TXN_TAG, "Sms handleMessage :" + msg);
            int serviceId = msg.arg1;
            Intent intent = (Intent)msg.obj;
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "handleMessage serviceId: " + serviceId + " intent: " + intent);
            }
            // KK migration, for default MMS function.
            if (intent != null && MmsConfig.isSmsEnabled(getApplicationContext())) {
                String action = intent.getAction();

                int error = intent.getIntExtra("errorCode", 0);

                if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                    Log.v(TAG, "handleMessage action: " + action + " error: " + error);
                }

                if (MESSAGE_SENT_ACTION.equals(intent.getAction())) {
                    handleSmsSent(intent, error);
                } else if (SMS_DELIVER_ACTION.equals(action)) {
                    handleSmsReceived(intent, error);
                } else if (ACTION_BOOT_COMPLETED.equals(action)) {
                    handleBootCompleted();
                } else if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                    handleServiceStateChanged(intent);
                } else if (ACTION_SEND_MESSAGE.endsWith(action)) {
                    /// M:Code analyze 008,add a parameter for this function,pass a simId
                    /// to SmsSingleRecipientSender for gemini @{
                    handleSendMessage(intent);
                    /// @}
                } else if (EncapsulatedTelephonyIntents.ACTION_HIDE_NW_STATE.equals(action)) {
                    handleHideStatus(intent);
                }
            }
            /// M:Code analyze 002, new members,add for unknown or useless @{
            sSmsSent = true;
            /// @}
            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by AlertReceiver is released.
            SmsReceiver.finishStartingService(SmsReceiverService.this, serviceId);
        }
    }

    private void handleServiceStateChanged(Intent intent) {
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "Sms handleServiceStateChanged");
        // If service just returned, start sending out the queued messages
        ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
        if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
            /// M:Code analyze 009, modify for gemini when service state changed @{
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                // convert slot id to sim id
                int slotId = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1);
                EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(this, slotId);
                if (null == si) {
                    MmsLog.e(MmsApp.TXN_TAG, "handleServiceStateChanged:SIMInfo is null for slot " + slotId);
                    return;
                }
                //sendFirstQueuedMessage((int)si.getSimId());
                /// M: add for ALPS00592464 begin @{
                MmsLog.d(MmsApp.TXN_TAG, "handleServiceStateChanged(),simID = " + (int)si.getSimId());
                if (sRegSimIdSet.contains((int)si.getSimId())) {
                    sRegSimIdSet.remove((int)si.getSimId());
                    sendFirstQueuedMessage((int) si.getSimId());
                }
                /// @}
            } else {
                sendFirstQueuedMessage(-1);
            }
            /// @}
        }
    }

    private void handleHideStatus(Intent intent) {
        int slotId = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1);
        String action = intent.getStringExtra(EncapsulatedTelephonyIntents.EXTRA_ACTION);
        if (action.equals(SmsReceiver.RADIO_HIDE_STATE_STOP)) {
            int state =  intent.getIntExtra(EncapsulatedTelephonyIntents.EXTRA_REAL_SERVICE_STATE,
                    ServiceState.STATE_IN_SERVICE);
            int simId = -1;
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                // convert slot id to sim id
                EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(this, slotId);
                if (null == si) {
                    MmsLog.e(MmsApp.TXN_TAG, "handleHideStatus:SIMInfo is null for slot " + slotId);
                    return;
                }
                simId = (int) si.getSimId();
            }
            if (state == ServiceState.STATE_IN_SERVICE) {
                sendFirstQueuedMessage(simId);
            } else {
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    sRegSimIdSet.add((int) simId);
                    MmsLog.d(MmsApp.TXN_TAG, "handleHideStatus(),regSimId = " + simId);
                }
                registerForServiceStateChanges();
            }
        }
    }

    /// M:Code analyze 008,add a parameter for this function,pass a simId
    /// to SmsSingleRecipientSender for gemini @{
    private void handleSendMessage(Intent intent) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "handleSendMessage() simId=" + intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1));
        if (!mSending) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                sendFirstQueuedMessage(intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1));
            } else {
                sendFirstQueuedMessage(-1);
            }
        }
    }
    /// @}

    /// M:Code analyze 010, extend this method to support gemini, add a parameter to pass simId @{
    public synchronized void sendFirstQueuedMessage(int simId) {
        /// M:
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        MmsLog.d(MmsApp.TXN_TAG, "sendFirstQueuedMessage(), isSmsEnabled = " + isSmsEnabled);
        if (!isSmsEnabled) {
            return;
        }
        boolean success = true;
        // get all the queued messages from the database
        final Uri uri = Uri.parse("content://sms/queued");
        ContentResolver resolver = getContentResolver();
        /// M:Codo analyze 011, modify for gemini,extend query condition with simId @{
        String where = null;
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            where = EncapsulatedTelephony.Mms.SIM_ID + "=" + simId;
        }
        Cursor c = SqliteWrapper.query(this, resolver, uri,
                        SEND_PROJECTION, where, null, "date ASC");   // date ASC so we send out in
                                                                    // same order the user tried
                                                                    // to send messages.
        /// @}
        int count = 0;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    count++;
                    String msgText = c.getString(SEND_COLUMN_BODY);
                    String address = c.getString(SEND_COLUMN_ADDRESS);
                    int threadId = c.getInt(SEND_COLUMN_THREAD_ID);
                    int status = c.getInt(SEND_COLUMN_STATUS);

                    int msgId = c.getInt(SEND_COLUMN_ID);
                    Uri msgUri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);

                    SmsMessageSender sender = new SmsSingleRecipientSender(this,
                            address, msgText, threadId, status == Sms.STATUS_PENDING,
                            msgUri);
                    /// M:Code analyze 012, add for gemini,pass simId to SmsSingleRecipientSender @{
                    if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                        sender.setSimId(simId);
                    }
                    /// @}
                    if (LogTag.DEBUG_SEND ||
                            LogTag.VERBOSE ||
                            Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                        Log.v(TAG, "sendFirstQueuedMessage " + msgUri +
                                ", address: " + address +
                                ", threadId: " + threadId +
                                /// M: print more info
                                ", body: " + msgText);
                    }

                    try {
                        sender.sendMessage(SendingProgressTokenManager.NO_TOKEN);;
                        mSending = true;
                    } catch (MmsException e) {
                        Log.e(TAG, "sendFirstQueuedMessage: failed to send message " + msgUri
                                + ", caught ", e);
                        /// M:Code analyze 013, comment the two lines,we will do this in finally branch @{
                        /*
                         * mSending = false;
                         * messageFailedToSend(msgUri, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                         */
                        /// @}
                        success = false;
                    }
                }
            } finally {
                /// M:Code analyze 014,set last sent failed and do resend operation @{
                if (!success) {
                    int msgId = c.getInt(SEND_COLUMN_ID);
                    c.close();
                    Uri msgUri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
                    messageFailedToSend(msgUri, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                    sendFirstQueuedMessage(simId);
                } else {
                    c.close();
                }
                /// @}

            }
        }
        if (success && count > 0) {
            // We successfully sent all the messages in the queue. We don't need to
            // be notified of any service changes any longer.
            unRegisterForServiceStateChangesAfterCheck();
        }
    }
    /// @}

    private void handleSmsSent(Intent intent, int error) {
        /// M:
        int resultCode = intent != null ? intent.getIntExtra("result", 0) : 0;
        MmsLog.d(MmsApp.TXN_TAG, "handleSmsSent(), errorcode=" + error + "resultCode=" + resultCode);
        Uri uri = intent.getData();
        mSending = false;
        boolean sendNextMsg = intent.getBooleanExtra(EXTRA_MESSAGE_SENT_SEND_NEXT, false);

        if (LogTag.DEBUG_SEND) {
            Log.v(TAG, "handleSmsSent uri: " + uri + " sendNextMsg: " + sendNextMsg +
                    " resultCode: " + resultCode +
                    " = " + translateResultCode(resultCode) + " error: " + error);
        }

        /// M:Code analyze 015, update message size into database @{
        updateSizeForSentMessage(intent);
        /// @}
        int slotId = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1);
        if (resultCode == Activity.RESULT_OK) {
            /// M:
            MmsLog.d(MmsApp.TXN_TAG, "handleSmsSent(), result is RESULT_OK");
            if (LogTag.DEBUG_SEND || Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "handleSmsSent move message to sent folder uri: " + uri);
            }
            /// M:Code analyze 016,change logic for just only checking one part of long sms is sent failed
            /// or not,if yes,means this long sms is sent failed,no need move it to other boxes @{
            if (sendNextMsg) { //this is the last part of a sms.a long sms's part is sent ordered.
                Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                                                    uri, new String[] {Sms.TYPE}, null, null, null);
                if (cursor != null) {
                    try {
                        if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                            int smsType = 0;
                            smsType = cursor.getInt(0);
                            //if smsType is failed, that means at least one part of this long sms is sent failed.
                            // then this long sms is sent failed.
                            //so we shouldn't move it to other boxes.just keep it in failed box.
                            if (smsType != Sms.MESSAGE_TYPE_FAILED) {
                                //move sms from out box to sent box
                                if (!Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_SENT, error)) {
                                    Log.e(TAG, "handleSmsSent: failed to move message " + uri + " to sent folder");
                                }
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
            /// @}
            if (sendNextMsg) {
                /// M:Code analyze 017, modify for gemini,try to send next message @{
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    // convert slot id to sim id
                    EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(this, slotId);
                    if (null == si) {
                        MmsLog.e(MmsApp.TXN_TAG, "SmsReceiver:SIMInfo is null for slot " + slotId);
                        return;
                    }
                    sendFirstQueuedMessage((int)si.getSimId());
                } else {
                    sendFirstQueuedMessage(-1);
                }
                /// @}
            }

            // Update the notification for failed messages since they may be deleted.
            MessagingNotification.nonBlockingUpdateSendFailedNotification(this);
        } else if ((resultCode == SmsManager.RESULT_ERROR_RADIO_OFF) ||
                (resultCode == SmsManager.RESULT_ERROR_NO_SERVICE) ||
                SmsReceiver.RADIO_HIDE_STATE_START.equals(SmsReceiver.getCurrentState(slotId))) {
            /// M:Code analyze 018,print error type @{
            if (resultCode == SmsManager.RESULT_ERROR_RADIO_OFF) {
                MmsLog.d(MmsApp.TXN_TAG, "handleSmsSent(), result is RESULT_ERROR_RADIO_OFF");
            } else if (resultCode == SmsManager.RESULT_ERROR_NO_SERVICE){
                MmsLog.d(MmsApp.TXN_TAG, "handleSmsSent(), result is RESULT_ERROR_NO_SERVICE");
            } else {
                MmsLog.d(MmsApp.TXN_TAG, "handleSmsSent(), radio state is start");
            }
            /// @}
            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                Log.v(TAG, "handleSmsSent: no service, queuing message w/ uri: " + uri);
            }
            /// M: add for ALPS00592464 begin @{
                /// M: add for ALPS00971596 fix null pointer begin @{
            EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(this, slotId);
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT && si != null) {
                /// @}
                sRegSimIdSet.add((int) si.getSimId());
                MmsLog.d(MmsApp.TXN_TAG, "handleSmsSent(),regSimId = " + (int) si.getSimId());
            }
            /// @}
            // We got an error with no service or no radio. Register for state changes so
            // when the status of the connection/radio changes, we can try to send the
            // queued up messages.
            if ((resultCode == SmsManager.RESULT_ERROR_RADIO_OFF) ||
                    (resultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
                registerForServiceStateChanges();
            }
            // We couldn't send the message, put in the queue to retry later.
            /// M:Code analyze 019, if smsType is failed, that means at least one part of this long
            /// sms is sent failed.then this long sms is sent failed. @{
            Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                                                    uri, new String[] {Sms.TYPE}, null, null, null);
            if (cursor != null) {
                try {
                    if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                        int smsType = 0;
                        smsType = cursor.getInt(0);
                        //if smsType is failed, that means at least one part of this long sms is sent failed.
                        // then this long sms is sent failed.
                        //so we shouldn't move it to other boxes.just keep it in failed box.
                        if (smsType != Sms.MESSAGE_TYPE_FAILED) {
                            Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_QUEUED, error);
                            MmsLog.d(MmsApp.TXN_TAG, "move message " + uri + " to queue folder");
                            mToastHandler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(SmsReceiverService.this, getString(R.string.message_queued),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            MmsLog.d(MmsApp.TXN_TAG, "One or more part was failed, should not move to queue folder.");
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            /// @}
        /// M:Code analyze 020, moidfy the code logic,merge two branch into one,meantime add for gemini @{
        } else {
            messageFailedToSend(uri, error);
            if (resultCode == SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE) {
                mToastHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(SmsReceiverService.this, getString(R.string.fdn_check_failure),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (sendNextMsg) {
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    // convert slot id to sim id
                    EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(this, slotId);
                    if (null == si) {
                        MmsLog.e(MmsApp.TXN_TAG, "SmsReceiver:SIMInfo is null for slot " + slotId);
                        return;
                    }
                    sendFirstQueuedMessage((int)si.getSimId());
                } else {
                    sendFirstQueuedMessage(-1);
                }
            }
        }
        /// @}
    }

    private void messageFailedToSend(Uri uri, int error) {
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "messageFailedToSend(),uri=" + uri + "\terror=" + error);
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
            Log.v(TAG, "messageFailedToSend msg failed uri: " + uri + " error: " + error);
        }
        Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_FAILED, error);
        /// M:Code analyze 021, update sms status when failed. this Sms.STATUS is used for
        /// delivery report. @{
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(Sms.STATUS, Sms.STATUS_FAILED);
        /// M: BUGFIX:560953; if user has not request delivery report,then will not update the status;
        SqliteWrapper.update(this, this.getContentResolver(), uri, contentValues, Sms.STATUS + " = ? ",
                new String[] {Sms.STATUS_PENDING + ""});
        /// @}
        MessagingNotification.notifySendFailed(getApplicationContext(), true);
        MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
    }

    private void handleSmsReceived(Intent intent, int error) {
        SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
        /// M:Code analyze 022, check null @{
        if (msgs == null) {
            MmsLog.e(MmsApp.TXN_TAG, "getMessagesFromIntent return null.");
            return;
        }
        MmsLog.d(MmsApp.TXN_TAG, "handleSmsReceived SmsReceiverService");
        /// @}
        /// M: For OP09 Storage Low @{
        if (MmsConfig.isSupportCTFeature()) {
            MessageUtils.dealCTDeviceLowNotification(getApplicationContext());
        }
        /// M: @}
        String format = intent.getStringExtra("format");
        /// M:Code analyze 023, the second argument is change for passing simId info @{
        Uri messageUri = null;
        try {
            messageUri = insertMessage(this, intent, error, format);
        } catch (IllegalArgumentException e) {
            MmsLog.e(TAG, "Save message fail:" + e.getMessage(), e);
            return;
        }
        /// @}

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
            SmsMessage sms = msgs[0];
            Log.v(TAG, "handleSmsReceived" + (sms.isReplace() ? "(replace)" : "") +
                    " messageUri: " + messageUri +
                    ", address: " + sms.getOriginatingAddress() +
                    ", body: " + sms.getMessageBody());
        }
        /// M:Code analyze 024, print log @{
        SmsMessage tmpsms = msgs[0];
        MmsLog.d(MmsApp.TXN_TAG, "handleSmsReceived" + (tmpsms.isReplace() ? "(replace)" : "") 
            + " messageUri: " + messageUri 
            + ", address: " + tmpsms.getOriginatingAddress() 
            + ", body: " + tmpsms.getMessageBody());
        /// @}

        if (messageUri != null) {
            long threadId = MessagingNotification.getSmsThreadId(this, messageUri);
            // Called off of the UI thread so ok to block.
            MessagingNotification.blockingUpdateNewMessageIndicator(this, threadId, false);
            /// M:Code analyze 025,CMCC new sms dialog @{
            if (NotificationPreferenceActivity.isPopupNotificationEnable()) {
                IMmsDialogNotify dialogPlugin =
                        (IMmsDialogNotify)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_DIALOG_NOTIFY);
                dialogPlugin.notifyNewSmsDialog(messageUri);
            }
            /// @}
        /// M:Code analyze 026, add else branch,that means this message is not saved successfully,
        /// because its type is class 0,just show on phone and no need to save @{
        } else {
            SmsMessage sms = msgs[0];
            SmsMessage msg = SmsMessage.createFromPdu(sms.getPdu(), format);
            if (msg == null) {
                MmsLog.e(MmsApp.TXN_TAG, "createFromPdu return null.");
                return;
            }
            CharSequence messageChars = msg.getMessageBody();
            if (messageChars == null) {
                MmsLog.e(MmsApp.TXN_TAG, "getMessageBody return null.");
                return;
            }
            String message = messageChars.toString();
            if (!TextUtils.isEmpty(message)) {
                MessagingNotification.notifyClassZeroMessage(this, msgs[0]
                        .getOriginatingAddress());
            }
        }
        /// @}
    }
    private void handleBootCompleted() {
        // Some messages may get stuck in the outbox or queued. At this point, they're probably irrelevant
        // to the user, so mark them as failed and notify the user, who can then decide whether to
        // resend them manually.
        int numMoved = moveOutboxMessagesToFailedBox();
        numMoved = numMoved + moveQueuedMessagesToFailedBox();
        if (numMoved > 0) {
            MessagingNotification.notifySendFailed(getApplicationContext(), true);
        }

        /// M:Code analyze 027, modify the logic for gemini,do nothing @{
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            ;
        } else {
            // Send any queued messages that were waiting from before the reboot.
            sendFirstQueuedMessage(-1);
        }
        /// @}

        // Called off of the UI thread so ok to block.
        MessagingNotification.blockingUpdateNewMessageIndicator(
                this, MessagingNotification.THREAD_ALL, false);

	/*=== add by puyong@20140915 for 预置小区广播 begin===*/
	// 预置channels 50
	if(	  queryIfChannelInDatabase(PhoneConstants.GEMINI_SIM_1,"Channel1",  50)
		&&queryIfChannelInDatabase(PhoneConstants.GEMINI_SIM_2,"Channel1",  50))
	{
		Log.d(TAG, "queryIfChannelInDatabase: return!");
		return;
	}
	
	if(SmsManagerEx.getDefault().activateCellBroadcastSms(false, 0))
	{
		Log.d(TAG, "SmsManagerEx: activateCellBroadcastSms GEMINI_SIM_1");
		
		addCustomChanneltoList(PhoneConstants.GEMINI_SIM_1,"Channel1",50);
	}
	
	if(SmsManagerEx.getDefault().activateCellBroadcastSms(false, 1))
	{
		Log.d(TAG, "SmsManagerEx: activateCellBroadcastSms GEMINI_SIM_2");
		
		addCustomChanneltoList(PhoneConstants.GEMINI_SIM_2,"Channel1",50);
	}
	
	// 预置channels 60
	if(	  queryIfChannelInDatabase(PhoneConstants.GEMINI_SIM_1,"Channel2",  60)
		&&queryIfChannelInDatabase(PhoneConstants.GEMINI_SIM_2,"Channel2",  60))
	{
		Log.d(TAG, "queryIfChannelInDatabase: return!");
		return;
	}
	
	if(SmsManagerEx.getDefault().activateCellBroadcastSms(false, 0))
	{
		Log.d(TAG, "SmsManagerEx: activateCellBroadcastSms GEMINI_SIM_1");
		
		addCustomChanneltoList(PhoneConstants.GEMINI_SIM_1,"Channel2",60);
	}
	
	if(SmsManagerEx.getDefault().activateCellBroadcastSms(false, 1))
	{
		Log.d(TAG, "SmsManagerEx: activateCellBroadcastSms GEMINI_SIM_2");
		
		addCustomChanneltoList(PhoneConstants.GEMINI_SIM_2,"Channel2",60);
	}
	
	/*=== add by puyong@20140915 for 预置小区广播 end===*/

    }


	/*=== add by puyong@20140915 for 预置小区广播 begin===*/
	private void addCustomChanneltoList(int mSimId, String channelName, int channelNum)
	{
		Log.d(TAG, "addCustomChanneltoList: mSimId=" + mSimId + ", channelName= " + channelName + ", channelNum= " + channelNum);
	
		if(queryChannelFromDatabase(mSimId, channelName, channelNum))
		{
			SmsBroadcastConfigInfo[] objectList = new SmsBroadcastConfigInfo[1];
			objectList[0] = new SmsBroadcastConfigInfo(channelNum,channelNum, -1, -1, true);
			Message msg1 = mServiceHandler.obtainMessage(MESSAGE_SET_CONFIG, 0,MESSAGE_SET_CONFIG, null);
			
			Log.d(TAG, "addCustomChanneltoList: setCellBroadcastSmsConfig");
			SmsManagerEx.getDefault().setCellBroadcastSmsConfig(objectList, objectList, mSimId);
		}
	
		Log.d(TAG, "addCustomChanneltoList: function end");
	}
	
	private boolean queryChannelFromDatabase(int mSimId,String channelName,int channelNum)
	{
		// ClearChannel();
		Log.d(TAG, "queryChannelFromDatabase: mSimId=" + mSimId + ", channelName= " + channelName + ", channelNum= " + channelNum);
	
		String[] projection = new String[] { KEYID, NAME, NUMBER, ENABLE };
		String SELECTION = "(" + NUMBER + " = " + channelNum + ")";
		Cursor cursor = null;
		
		if(mSimId==PhoneConstants.GEMINI_SIM_1)
		{
			cursor = this.getContentResolver().query(CHANNEL_URI,projection, SELECTION, null, null);
		}
		else if(mSimId==PhoneConstants.GEMINI_SIM_2)
		{
			cursor = this.getContentResolver().query(CHANNEL_URI1,projection, SELECTION, null, null);
		}
	
		Log.d(TAG, "queryChannelFromDatabase: cursor.getCount" + cursor.getCount());
	
		if (cursor.getCount() == 0)
		{				
			ContentValues values = new ContentValues();
			values.put(NAME,channelName);
			values.put(NUMBER, channelNum);
			values.put(ENABLE, false);
	
			try
			{
				if(mSimId==PhoneConstants.GEMINI_SIM_1)
				{
					this.getContentResolver().insert(CHANNEL_URI, values);
				}
				else if(mSimId==PhoneConstants.GEMINI_SIM_2)
				{
					this.getContentResolver().insert(CHANNEL_URI1, values);
				}
			}
			catch (Exception e)
			{
				cursor.close();
				return false;
			}
	
			cursor.close();
			return true;
		}
		else
		{
			cursor.close();
			return false;
		}
	}
	
	
	private boolean queryIfChannelInDatabase(int mSimId,String channelName,int channelNum)
	{
		Log.d(TAG, "queryIfChannelInDatabase: mSimId=" + mSimId + ", channelName= " + channelName + ", channelNum= " + channelNum);
	
		String[] projection = new String[] { KEYID, NAME, NUMBER, ENABLE };
		String SELECTION = "(" + NUMBER + " = " + channelNum + ")";
		Cursor cursor = null;
	
		if(mSimId==PhoneConstants.GEMINI_SIM_1)
		{
			cursor = this.getContentResolver().query(CHANNEL_URI,projection, SELECTION, null, null);
		}
		else if(mSimId==PhoneConstants.GEMINI_SIM_2)
		{
			cursor = this.getContentResolver().query(CHANNEL_URI1,projection, SELECTION, null, null);
		}
	
		if(cursor != null)
		{
			if (cursor.getCount() != 0)
			{
				Log.d(TAG, "queryIfChannelInDatabase: cursor.getCount" + cursor.getCount());
				
				cursor.close();
				return true;
			}
			
			cursor.close();
		}
		
		Log.d(TAG, "queryIfChannelInDatabase: return false");
		return false;
	}	
	/*=== add by puyong@20140915 for 预置小区广播 end  ===*/

    /**
     * Move all messages that are in the outbox to the failed state and set them to unread.
     * @return The number of messages that were actually moved
     */
    private int moveOutboxMessagesToFailedBox() {
        ContentValues values = new ContentValues(3);

        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
        values.put(Sms.ERROR_CODE, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        values.put(Sms.READ, Integer.valueOf(0));

        int messageCount = SqliteWrapper.update(
                getApplicationContext(), getContentResolver(), Outbox.CONTENT_URI,
                values, "type = " + Sms.MESSAGE_TYPE_OUTBOX, null);
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
            Log.v(TAG, "moveOutboxMessagesToFailedBox messageCount: " + messageCount);
        }
        return messageCount;
    }

    public static final String CLASS_ZERO_BODY_KEY = "CLASS_ZERO_BODY";

    // This must match the column IDs below.
    private final static String[] REPLACE_PROJECTION = new String[] {
        Sms._ID,
        Sms.ADDRESS,
        Sms.PROTOCOL
    };

    // This must match REPLACE_PROJECTION.
    private static final int REPLACE_COLUMN_ID = 0;

    /// M:Code analyze 023, the second argument is changed for passing simId info @{
    /**
     * If the message is a class-zero message, display it immediately
     * and return null.  Otherwise, store it using the
     * <code>ContentResolver</code> and return the
     * <code>Uri</code> of the thread containing this message
     * so that we can use it for notification.
     */
     private Uri insertMessage(Context context, Intent intent, int error, String format) {
        // Build the helper classes to parse the messages.
        /// M:Code analyze 028,get message info from intent @{
        SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
        if (msgs == null) {
            MmsLog.e(MmsApp.TXN_TAG, "insertMessage:getMessagesFromIntent return null.");
            return null;
        }
        /// @}
        SmsMessage sms = msgs[0];

        /// M:Code analyze 029, convert slot id to sim id @{
        int slotId = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1);
        /// M: get slotId from intent for EncapsulatedMms receive sms through SIM2 @{
        EncapsulatedSmsMessageEx.setSmsSlotId(slotId);
        /// @}
        EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(context, slotId);
        if (null == si) {
            MmsLog.e(MmsApp.TXN_TAG, "insertMessage:SIMInfo is null for slot " + slotId);
            return null;
        }
        int simId = (int)si.getSimId();
        intent.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, simId);
        MmsLog.d(MmsApp.TXN_TAG, "Sms insert message,\tslot id=" + slotId + "\tsim id=" + simId);
        /// @}

        /// M: OP09 Feature, receive long SMS. @{
        int uploadFlag = EncapsulatedIConcatenatedSmsFwkExt.UPLOAD_FLAG_NEW;
        if (mMissedSmsReceiverPlugin.isEnableMissedSmsReceiver()) {
            uploadFlag = intent.getIntExtra(EncapsulatedIConcatenatedSmsFwkExt.UPLOAD_FLAG_TAG,
                         EncapsulatedIConcatenatedSmsFwkExt.UPLOAD_FLAG_NEW);
            MmsLog.d(MmsApp.TXN_TAG, "UPLOAD_FLAG_TAG: " + uploadFlag);
        }
        /// @}

        if (sms.getMessageClass() == SmsMessage.MessageClass.CLASS_0) {
            /// M:Code analyze 030, the second argument is changed,if message type is class 0
            /// just only show it on phone but not save @{
            displayClassZeroMessage(context, intent, format);
            /// @}
            return null;
        } else if (sms.isReplace()) {
            return replaceMessage(context, msgs, error);
        /// M: For OP09 Feature, receive missing part of concatenated Sms. @{
        } else if (mMissedSmsReceiverPlugin.isEnableMissedSmsReceiver()
                && uploadFlag == EncapsulatedIConcatenatedSmsFwkExt.UPLOAD_FLAG_UPDATE) {
            return mMissedSmsReceiverPlugin.updateMissedSms(context, msgs, error);
        /// @}
        } else {
            return storeMessage(context, msgs, error);
        }
    }
    /// @}

    /**
     * This method is used if this is a "replace short message" SMS.
     * We find any existing message that matches the incoming
     * message's originating address and protocol identifier.  If
     * there is one, we replace its fields with those of the new
     * message.  Otherwise, we store the new message as usual.
     *
     * See TS 23.040 9.2.3.9.
     */
    private Uri replaceMessage(Context context, SmsMessage[] msgs, int error) {
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "Sms replaceMessage");
        SmsMessage sms = msgs[0];
        ContentValues values = extractContentValues(sms);
        values.put(Sms.ERROR_CODE, error);
        int pduCount = msgs.length;

        if (pduCount == 1) {
            // There is only one part, so grab the body directly.
            values.put(Inbox.BODY, replaceFormFeeds(sms.getDisplayMessageBody()));
        } else {
            // Build up the body from the parts.
            StringBuilder body = new StringBuilder();
            for (int i = 0; i < pduCount; i++) {
                sms = msgs[i];
                if (sms.mWrappedSmsMessage != null) {
                    body.append(sms.getDisplayMessageBody());
                }
            }
            values.put(Inbox.BODY, replaceFormFeeds(body.toString()));
        }

        ContentResolver resolver = context.getContentResolver();
        String originatingAddress = sms.getOriginatingAddress();
        int protocolIdentifier = sms.getProtocolIdentifier();
        String selection =
                Sms.ADDRESS + " = ? AND " +
                Sms.PROTOCOL + " = ?";

        /// M:Code analyze 032, modify for gemini,mainly add simId info to change query condition @{
        String[] selectionArgs = null;

        // for gemini we should care the sim id
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            selection = selection + " AND " + EncapsulatedTelephony.Sms.SIM_ID + " = ?";
            // conver slot id to sim id
            EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(context, EncapsulatedSmsMessageEx.getMessageSimId(sms));
            if (null == si) {
                MmsLog.e(MmsApp.TXN_TAG, "SmsReceiverService:SIMInfo is null for slot "
                            + EncapsulatedSmsMessageEx.getMessageSimId(sms));
                return null;
            }
            int simId = (int)si.getSimId();
            selectionArgs = new String[] {originatingAddress, 
                                          Integer.toString(protocolIdentifier), 
                                          Integer.toString(simId/*sms.getMessageSimId()*/)};
        } else {
            selectionArgs = new String[] {originatingAddress, 
                                          Integer.toString(protocolIdentifier)};
        }

        // add for gemini
        Cursor cursor = SqliteWrapper.query(context, resolver, Inbox.CONTENT_URI,
                            EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT ? REPLACE_PROJECTION_GEMINI : REPLACE_PROJECTION,
                            selection, 
                            selectionArgs, null);
        /// @}

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    long messageId = cursor.getLong(REPLACE_COLUMN_ID);
                    Uri messageUri = ContentUris.withAppendedId(
                            Sms.CONTENT_URI, messageId);

                    SqliteWrapper.update(context, resolver, messageUri,
                                        values, null, null);
                    return messageUri;
                }
            } finally {
                cursor.close();
            }
        }
        return storeMessage(context, msgs, error);
    }

    public static String replaceFormFeeds(String s) {
        // Some providers send formfeeds in their messages. Convert those formfeeds to newlines.
        /// M:Code analyze 033,add judgement if s is null or not @{
        if (null == s) {
            return "";
        }
        /// @}
        return s.replace('\f', '\n');
    }

//    private static int count = 0;

    private Uri storeMessage(Context context, SmsMessage[] msgs, int error) {
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "Sms storeMessage");
        SmsMessage sms = msgs[0];

        // Store the message in the content provider.
        ContentValues values = extractContentValues(sms);
        values.put(Sms.ERROR_CODE, error);
        int pduCount = msgs.length;

        // M:Code analyze 034, fix bug ALPS00352897,fix long messages splicing error @{
        ISmsReceiver smsReceiverPlugin = (ISmsReceiver)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_SMS_RECEIVER);
        if (smsReceiverPlugin != null) {
            smsReceiverPlugin.extractSmsBody(msgs, sms, values);
        }
        /// @}

        // Make sure we've got a thread id so after the insert we'll be able to delete
        // excess messages.
        Long threadId = values.getAsLong(Sms.THREAD_ID);
        String address = values.getAsString(Sms.ADDRESS);

        // Code for debugging and easy injection of short codes, non email addresses, etc.
        // See Contact.isAlphaNumber() for further comments and results.
//        switch (count++ % 8) {
//            case 0: address = "AB12"; break;
//            case 1: address = "12"; break;
//            case 2: address = "Jello123"; break;
//            case 3: address = "T-Mobile"; break;
//            case 4: address = "Mobile1"; break;
//            case 5: address = "Dogs77"; break;
//            case 6: address = "****1"; break;
//            case 7: address = "#4#5#6#"; break;
//        }

        if (!TextUtils.isEmpty(address)) {
            Contact cacheContact = Contact.get(address,true);
            if (cacheContact != null) {
                address = cacheContact.getNumber();
            }
        } else {
            address = getString(R.string.unknown_sender);
            values.put(Sms.ADDRESS, address);
        }

        /// M:Code analyze 035, mainly for getting simId info @{
        /// we should add sim id information
            // conver slot id to sim id
            MmsLog.e(MmsApp.TXN_TAG, "slotid = " + EncapsulatedSmsMessageEx.getMessageSimId(sms));
            EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(context, EncapsulatedSmsMessageEx.getMessageSimId(sms));
            if (null == si) {
                MmsLog.e(MmsApp.TXN_TAG, "SmsReceiverService:SIMInfo is null for slot "
                        + EncapsulatedSmsMessageEx.getMessageSimId(sms));
                return null;
            }
            values.put(EncapsulatedTelephony.Sms.SIM_ID, (int)si.getSimId()/*sms.getMessageSimId()*/);

            /* MTK note: for FTA test in the GEMINI phone
            * We need to tell SmsManager where the last incoming SMS comes from.
            * This is because the mms APP and Phone APP runs in two different process
            * and mms will use setSmsMemoryStatus to tell modem that the ME storage is full or not.
            * Since We need to dispatch the infomation about ME storage to currect SIM
            * so we should use setLastIncomingSmsSimId here 
            * to tell SmsManager this to let it dispatch the info.
            */
            EncapsulatedGeminiSmsManager.setLastIncomingSmsSimId(EncapsulatedSmsMessageEx.getMessageSimId(sms));
        /// @}

//        if (((threadId == null) || (threadId == 0)) && (address != null)) {
//            threadId = Conversation.getOrCreateThreadId(context, address);
//            values.put(Sms.THREAD_ID, threadId);
//        }

        ContentResolver resolver = context.getContentResolver();

        Uri insertedUri = SqliteWrapper.insert(context, resolver, Inbox.CONTENT_URI, values);

        /// M:Code analyze 036, store on SIM if needed @{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String storeLocation = null;

        if (MmsConfig.getSmsMultiSaveLocationEnabled()) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                final String saveLocationKey = Integer.toString(EncapsulatedSmsMessageEx.getMessageSimId(sms)) 
                        + "_" + SmsPreferenceActivity.SMS_SAVE_LOCATION;
                storeLocation = prefs.getString(saveLocationKey, "Phone");
            } else {
                storeLocation = prefs.getString(SmsPreferenceActivity.SMS_SAVE_LOCATION, "Phone");
            }
        }

        if (storeLocation == null) {
            storeLocation = prefs.getString(SmsPreferenceActivity.SMS_SAVE_LOCATION, "Phone");
        }
        if (storeLocation.equals("Sim")) {
            String sc = (null == sms.getServiceCenterAddress()) ? "" : sms.getServiceCenterAddress();
            boolean bSucceed = true;
            for (int i = 0; i < pduCount; i++) {
                sms = msgs[i];
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    bSucceed = EncapsulatedGeminiSmsManager.copyMessageToIccGemini(
                            EncapsulatedSmsMessageEx.getSmsc(sms, EncapsulatedSmsMessageEx.getMessageSimId(sms)),
                            EncapsulatedSmsMessageEx.getTpdu(sms, EncapsulatedSmsMessageEx.getMessageSimId(sms)),
                            SmsManager.STATUS_ON_ICC_READ, EncapsulatedSmsMessageEx.getMessageSimId(sms)) >= 0;
                    MmsLog.d(MmsApp.TXN_TAG, "save sms on SIM. part:" + i 
                            + "; result:" + bSucceed + "; sc:" + sc + "; slotId:"
                            + EncapsulatedSmsMessageEx.getMessageSimId(sms));
                } else {
                    bSucceed = SmsManager.getDefault().copyMessageToIcc(
                            EncapsulatedSmsMessageEx.getSmsc(sms, 0),
                            EncapsulatedSmsMessageEx.getTpdu(sms, 0),
                            SmsManager.STATUS_ON_ICC_READ);
                    MmsLog.d(MmsApp.TXN_TAG, "save sms on SIM. part:" + i + "; result:" + bSucceed + "; sc:" + sc);
                }
            }
        }
        /// @}

        /// M:Code analyze 037, set sms size @{
        if (null != insertedUri) {
            int messageSize = 0;
            if (pduCount == 1) {
                messageSize = sms.getPdu().length;
            } else {
                for (int i = 0; i < pduCount; i++) {
                    sms = msgs[i];
                    messageSize += sms.getPdu().length;
                }
            }
            ContentValues sizeValue = new ContentValues();
            sizeValue.put(Mms.MESSAGE_SIZE, messageSize);
            SqliteWrapper.update(this, getContentResolver(), insertedUri, sizeValue, null, null);
        }
        /// @}

        Cursor cur = SqliteWrapper.query(this, resolver, insertedUri,
                new String[] {"thread_id"}, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
                threadId = cur.getLong(0);
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        /// M:Code analyze 038,add for unknown @{
        ThreadCountManager.getInstance().isFull(threadId, context, ThreadCountManager.OP_FLAG_INCREASE);
        /// @}

        // Now make sure we're not over the limit in stored messages
        Recycler.getSmsRecycler().deleteOldMessagesByThreadId(context, threadId);
        MmsWidgetProvider.notifyDatasetChanged(context);

        return insertedUri;
    }

    /**
     * Extract all the content values except the body from an SMS
     * message.
     */
    private ContentValues extractContentValues(SmsMessage sms) {
        // Store the message in the content provider.
        ContentValues values = new ContentValues();

        values.put(Inbox.ADDRESS, sms.getDisplayOriginatingAddress());
        /// M:Code analyze 039, comment the code,just using local time @{
        /*
        // Use now for the timestamp to avoid confusion with clock
        // drift between the handset and the SMSC.
        // Check to make sure the system is giving us a non-bogus time.
        Calendar buildDate = new GregorianCalendar(2011, 8, 18);    // 18 Sep 2011
        Calendar nowDate = new GregorianCalendar();
        long now = System.currentTimeMillis();
        nowDate.setTimeInMillis(now);

        if (nowDate.before(buildDate)) {
            // It looks like our system clock isn't set yet because the current time right now
            // is before an arbitrary time we made this build. Instead of inserting a bogus
            // receive time in this case, use the timestamp of when the message was sent.
            now = sms.getTimestampMillis();
        }

        values.put(Inbox.DATE, new Long(now));
        */
        values.put(Inbox.DATE, Long.valueOf(System.currentTimeMillis()));
        /// @}
        values.put(Inbox.DATE_SENT, Long.valueOf(sms.getTimestampMillis()));
        values.put(Inbox.PROTOCOL, sms.getProtocolIdentifier());
        values.put(Inbox.READ, 0);
        values.put(Inbox.SEEN, 0);
        if (sms.getPseudoSubject().length() > 0) {
            values.put(Inbox.SUBJECT, sms.getPseudoSubject());
        }
        values.put(Inbox.REPLY_PATH_PRESENT, sms.isReplyPathPresent() ? 1 : 0);
        values.put(Inbox.SERVICE_CENTER, sms.getServiceCenterAddress());
        return values;
    }

    /// M:Code analyze 030, the second argument is changed,if message type is class 0
    /// just only show it on phone but not save @{
    /**
     * Displays a class-zero message immediately in a pop-up window
     * with the number from where it received the Notification with
     * the body of the message
     *
     */
     private void displayClassZeroMessage(Context context, Intent intent, String format) {
        // Using NEW_TASK here is necessary because we're calling
        // startActivity from outside an activity.
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "Sms displayClassZeroMessage");
        /// M:Code analyze 031, moidfy the logic,set component into intent,add simId info
        /// if in gemini mode @{

        intent.setComponent(new ComponentName(context, ClassZeroActivity.class))
              .putExtra("format", format)
              .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        // add for gemini, add sim id info
        intent.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1));

        /// M: add for OP09 @{
        if (mDisplayClassZeroPlugin.isEnableClassZeroFeature()) {
            intent = mDisplayClassZeroPlugin.setLaunchMode(intent);
        }
        /// @}
        context.startActivity(intent);
    }
    /// @}

    private void registerForServiceStateChanges() {
        Context context = getApplicationContext();
        unRegisterForServiceStateChanges();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
            Log.v(TAG, "registerForServiceStateChanges");
        }

        context.registerReceiver(SmsReceiver.getInstance(), intentFilter);
    }

    private void unRegisterForServiceStateChanges() {
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
            Log.v(TAG, "unRegisterForServiceStateChanges");
        }
        try {
            Context context = getApplicationContext();
            context.unregisterReceiver(SmsReceiver.getInstance());
        } catch (IllegalArgumentException e) {
            // Allow un-matched register-unregister calls
        }
    }

    private void unRegisterForServiceStateChangesAfterCheck() {
        if (sRegSimIdSet.isEmpty()) {
            unRegisterForServiceStateChanges();
        }
    }

    /// M: new members
    /// M:Code analyze 002, add for unknown or useless @{
    public static boolean sSmsSent = true;
    /// @}

    /// M:Code analyze 003, Indicates this is a concatenation sms @{
    public static final String EXTRA_MESSAGE_CONCATENATION = "ConcatenationMsg";
    /// @}
    /// M:Code analyze 004,add for unknown or useless @{
    private static final  Uri UPDATE_THREADS_URI = Uri.parse("content://mms-sms/conversations/status");
    /// @}

    /// M:Code analyze 005,add two message types for mToastHandler @{
    private static final int RADIO_NOT_AVILABLE = 1;
    private static final int FDN_CHECK_FAIL = 2;
    /// @}

    /// M:Code analyze 006, add for gemini,it is used for replaced message @{
    private static final String[] REPLACE_PROJECTION_GEMINI = new String[] {
        Sms._ID,
        Sms.ADDRESS,
        Sms.PROTOCOL,
        EncapsulatedTelephony.Sms.SIM_ID
    };
    /// @}

    /// M:Code analyze 015, new methods,update message size into database @{
    private void updateSizeForSentMessage(Intent intent){
        Uri uri = intent.getData();
        int messageSize = intent.getIntExtra("pdu_size", -1);
        MmsLog.d(MmsApp.TXN_TAG, "update size for sent sms, size=" + messageSize);
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                                            uri, null, null, null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    ContentValues sizeValue = new ContentValues();
                    sizeValue.put(Mms.MESSAGE_SIZE, messageSize);
                    SqliteWrapper.update(this, getContentResolver(),
                                        uri, sizeValue, null, null);
                }
            } finally {
                cursor.close();
            }
        }
    }
    /// @}

    /**
     * Move all messages that are in the queued to the failed state and set them to unread.
     * @return The number of messages that were actually moved
     */
    private int moveQueuedMessagesToFailedBox() {
        ContentValues values = new ContentValues(3);

        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
        values.put(Sms.ERROR_CODE, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
        values.put(Sms.READ, Integer.valueOf(0));

        final Uri uri = Uri.parse("content://sms/queued");
        int messageCount =SqliteWrapper.update(
                getApplicationContext(), getContentResolver(), uri,
                values, "type = " + Sms.MESSAGE_TYPE_QUEUED , null);

        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
            Log.v(TAG, "moveQueuedMessagesToFailedBox messageCount: " + messageCount);
        }
        return messageCount;
    }

    /// M: For OP09 feature, receive missing segment of long SMS.
    public Uri callStoreMessage(Context context, SmsMessage[] msgs, int error) {
        return storeMessage(context, msgs, error);
    }

    private void initPlugin(Context context) {
        try {
            mDisplayClassZeroPlugin = (IDisplayClassZeroMessage)EncapsulatedPluginManager.createPluginObject(
                    context.getApplicationContext(), IDisplayClassZeroMessage.class.getName());
            MmsLog.d(TAG, "operator mDisplayClassZeroPlugin = " + mDisplayClassZeroPlugin);
        } catch (AndroidException e) {
            mDisplayClassZeroPlugin = new DisplayClassZeroMessageImpl(context.getApplicationContext());
            MmsLog.d(TAG, "default mDisplayClassZeroPlugin = " + mDisplayClassZeroPlugin);
        }

        try {
            mMissedSmsReceiverPlugin = (IMissedSmsReceiver)EncapsulatedPluginManager.createPluginObject(
                    context.getApplicationContext(), IMissedSmsReceiver.class.getName());
            MmsLog.d(TAG, "operator mMissedSmsReceiverPlugin = " + mMissedSmsReceiverPlugin);
        } catch (AndroidException e) {
            mMissedSmsReceiverPlugin = new MissedSmsReceiverImpl(context.getApplicationContext());
            MmsLog.d(TAG, "default mMissedSmsReceiverPlugin = " + mMissedSmsReceiverPlugin);
        }
        mMissedSmsReceiverPlugin.init(this);
    }
}
