/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;

import com.android.mms.R;
import android.database.sqlite.SqliteWrapper;
import com.android.mms.transaction.MessagingNotification;
import java.util.HashSet;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

/// M:
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AndroidException;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.Toast;

import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyIntents;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.ui.CustomMenu.DropDownMenu;
import com.android.mms.util.Recycler;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.internal.EncapsulatedR;
import com.mediatek.mms.ext.IMmsTextSizeAdjust;
import com.mediatek.mms.ext.IMmsTextSizeAdjustHost;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephonyManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsMemoryStatus;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSimInfoManager;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
import com.mediatek.mms.ext.IMmsManageSimMessage;
import com.mediatek.mms.ext.IStringReplacement;
import com.mediatek.mms.ext.MmsManageSimMessageImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Displays a list of the SMS messages stored on the ICC.
 */
public class ManageSimMessages extends Activity
        implements View.OnCreateContextMenuListener,
                   IMmsTextSizeAdjustHost, Contact.UpdateListener {
    private static final String TAG = "ManageSimMessages";
    private static final int MENU_COPY_TO_PHONE_MEMORY = 0;
    private static final int MENU_DELETE_FROM_SIM = 1;
    
    private static final int OPTION_MENU_DELETE = 0;

    private static final int SHOW_LIST = 0;
    private static final int SHOW_EMPTY = 1;
    private static final int SHOW_BUSY = 2;
    private int mState;

    private ContentResolver mContentResolver;
    private Cursor mCursor = null;
    private ListView mSimList;
    private TextView mMessage;
    private MessageListAdapter mMsgListAdapter = null;
    private AsyncQueryHandler mQueryHandler = null;

    public static final int SIM_FULL_NOTIFICATION_ID = 234;

    /// M:
    private static final int DIALOG_REFRESH = 1;        
    private static Uri sSimMessageUri;

    // the key used to deliver a flag to provider, indicate show long sms in one bubble or not.
    private static final String SHOW_IN_ONE = "showInOne";
    
    private static final int MENU_FORWARD = 2;
    private static final int MENU_REPLY = 3;
    private static final int MENU_ADD_TO_BOOKMARK      = 4;
    private static final int MENU_CALL_BACK            = 5;
    private static final int MENU_SEND_EMAIL           = 6;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 7;
    private static final int MENU_SEND_SMS              = 9;
    private static final int MENU_ADD_CONTACT           = 10;
    private static final int OPTION_MENU_SIM_CAPACITY = 1;

    ///M: Sim message column index. @{
    private static int COLUMN_SERVICE_CENTER_ADDRESS = 0;
    private static int COLUMN_ADDRESS = 1;
    private static int COLUMN_MESSAGE_CLASS = 2;
    private static int COLUMN_BODY = 3;
    private static int COLUMN_DATE = 4;
    private static int COLUMN_STATUS = 5;
    private static int COLUMN_INDEX_ON_ICC = 6;
    private static int COLUMN_IS_STATUS_REPORT = 7;
    private static int COLUMN_TRANSPORT_TYPE = 8;
    private static int COLUMN_TYPE = 9;
    private static int COLUMN_LOCKED = 10;
    private static int COLUMN_ERROR_CODE = 11;
    private static int COLUMN_ID = 12;
    private static int COLUMN_SIM_ID = 13;
    /// @}

    ProgressDialog mDialog;
    private static final String ALL_SMS = "999999"; 
    private static final String FOR_MULTIDELETE = "ForMultiDelete";
    private int mCurrentSlotId = 0;
    public boolean isQuerying = false;
    public boolean isDeleting = false;
    /// M: extract telephony number ...
    private ArrayList<String> mURLs = new ArrayList<String>();
    private ContactList mContactList;
    ///M: add for plugin
    private IMmsTextSizeAdjust mMmsTextSizeAdjustPlugin = null;
    ///M: fix bug ALPS00850867.
    private boolean mIsCurrentSimFull;

    /// M: fix bug ALPS00448222, posting UI update Runnables for Contact update
    private Handler mHandler = new Handler();

    /// M: Add for OP09 @{
    private IMmsManageSimMessage mMmsManageSimMessagePlugin = null;
    private IStringReplacement mStringReplacePlugin = (IStringReplacement) MmsPluginManager
            .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_STRING_REPLACEMENT);
    /// @}

    /// M: add to fix costing time when delete message.
    private HashSet<Long> mDeletedMessageSet = new HashSet<Long>();

    ///M: add for avoid ANR when check sim state
    private QueryCapacityAndMsgTask mQueryCapacityAndMsgTask = new QueryCapacityAndMsgTask();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        /// M: fix bug ALPS00414035
        IntentFilter intentFilter = new IntentFilter(EncapsulatedTelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        registerReceiver(mSimReceiver, intentFilter);
        ///M: for 'finish activity when airplane mode on' function.
        registerReceiver(airPlaneReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        registerReceiver(mSimReceiver, new IntentFilter(Telephony.Sms.Intents.SIM_FULL_ACTION));

        /// M: @{
        initPlugin(this);

        Intent it = getIntent();
        mCurrentSlotId = it.getIntExtra("SlotId", 0);
        MmsLog.i(TAG, "onCreate Got slot id is : " + mCurrentSlotId);
        if (mCurrentSlotId == EncapsulatedPhone.GEMINI_SIM_1) {
            sSimMessageUri = Uri.parse("content://sms/icc");
        } else {
            String contentUri = "content://sms/icc" + (mCurrentSlotId + 1);
            sSimMessageUri = Uri.parse(contentUri);
        }
        /// @}
        setContentView(R.layout.sim_list);
        mContentResolver = getContentResolver();
        mQueryHandler = new QueryHandler(mContentResolver, this);
        mSimList = (ListView) findViewById(R.id.messages);
        mMessage = (TextView) findViewById(R.id.empty_message);

        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);

        /// M: fix bug ALPS00448222, update ListView when contact update
        Contact.addListener(this);
        mQueryCapacityAndMsgTask.execute();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        /// M: @{
        MmsLog.d(TAG, "onNewIntent .....");
        mCurrentSlotId = intent.getIntExtra("SlotId", 0);
        MmsLog.d(TAG, "onNewIntent Got slot id is : " + mCurrentSlotId);
        if (mCurrentSlotId == EncapsulatedPhone.GEMINI_SIM_1) {
            sSimMessageUri = Uri.parse("content://sms/icc");
        } else {
            String contentUri = "content://sms/icc" + (mCurrentSlotId + 1);
            sSimMessageUri = Uri.parse(contentUri);
        }
        /// @}
        MessagingNotification.cancelNotification(getApplicationContext(), SIM_FULL_NOTIFICATION_ID);
        mQueryCapacityAndMsgTask.cancel(true);
        updateState(SHOW_BUSY);
        startQueryIcc();
    }

    /**
     * M: Put checking SIM capacity action to background, because sometimes it cost long and
     * caused ANR.
     * And AP must execute query capacity and ICC message sync, can't asynchronous.
     */
    private class QueryCapacityAndMsgTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            MmsLog.d(TAG, "QueryCapacityAndMsgTask doInBackground.");
            isQuerying = true;
            ///M: fix bug ALPS00850867, check whether current SIM is full on NewIntent.
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                mIsCurrentSimFull = isCurrentSimFullGemini();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ///M: fix bug ALPS00850867, check whether current SIM is full before cancel the sim full
            // notification.
            MmsLog.d(TAG, "QueryCapacityAndMsgTask onPostExecute simFull: " + mIsCurrentSimFull);
            if (mIsCurrentSimFull || !EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                MessagingNotification.cancelNotification(getApplicationContext(),
                        SIM_FULL_NOTIFICATION_ID);
            }
            startQueryIcc();
        }
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(
                ContentResolver contentResolver, ManageSimMessages parent) {
            super(contentResolver);
        }

        @Override
        protected void onDeleteComplete(int token, Object expectDeleteNum, int ActualDeletedNum) {
            super.onDeleteComplete(token, expectDeleteNum, ActualDeletedNum);
            if(ManageSimMessages.this.isFinishing()) {
                return;
            }
            ///M: change for improve deleting and re-querying message flow performance.
            // 1. set isDeleting flag as false when delete completely.
            // 2. update the list manually if deleted successful.
            // 3. show failure toast if deleted fail. @{
            MmsLog.d(TAG, "onDeleteComplete expectDeleteNum " + expectDeleteNum +
                    " ActualDeletedNum " + ActualDeletedNum);
            isDeleting = false;
            if(ActualDeletedNum <= 0) {
                Toast.makeText(ManageSimMessages.this, getString(R.string.delete_unsuccessful), Toast.LENGTH_SHORT).show();
                updateState(SHOW_LIST);
            } else if (ActualDeletedNum == (Integer) expectDeleteNum) {
                updateListAfterDelete();
            } else {
                updateState(SHOW_BUSY);
                startQueryIcc();
            }
            mDeletedMessageSet.clear();
            /// @}
            /// M: cancel the SIM FULL notification if deleted successful.
            if (ActualDeletedNum > 0 && mIsCurrentSimFull) {
                MessagingNotification.cancelNotification(getApplicationContext(),
                        SIM_FULL_NOTIFICATION_ID);
            }
            /// @}
        }

        @Override
        protected void onQueryComplete(
                int token, Object cookie, Cursor cursor) {
            MmsLog.d(TAG, "onQueryComplete ");
            if(ManageSimMessages.this.isFinishing()) {
                return;
            }
            mQueryHandler.removeCallbacksAndMessages(null);
            isQuerying = false;
            ///M: change for improve deleting and re-querying message flow performance.
            // this query result data is not clearllt newest if is deleting, a newest
            // call back will come after delete compelete.
            if (isDeleting) {
                MmsLog.e(TAG, "onQueryComplete will return cause is deleting");
                return;
            }
            updateListWithCursor(cursor);
        }
    }

    private void updateListWithCursor(Cursor cursor){
        if (mCursor != null && !mCursor.isClosed()) {
            stopManagingCursor(mCursor);
        }
        mCursor = cursor;
        if (mCursor != null) {
            MmsLog.d(TAG, "updateListWithCursor cursor size is " + mCursor.getCount());
            if (!mCursor.moveToFirst()) {
                // Let user know the SIM is empty
                updateState(SHOW_EMPTY);
            } else if (mMsgListAdapter == null) {
                MmsLog.d(TAG, "updateListWithCursor mMsgListAdapter is null");
                // Note that the MessageListAdapter doesn't support auto-requeries. If we
                // want to respond to changes we'd need to add a line like:
                //   mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
                // See ComposeMessageActivity for an example.
                mMsgListAdapter = new MessageListAdapter(
                        this, mCursor, mSimList, false, null);
                mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
                mSimList.setAdapter(mMsgListAdapter);
                if (MmsConfig.getAdjustFontSizeEnabled()) {
                    float textSize = MessageUtils.getPreferenceValueFloat(ManageSimMessages.this,
                            SettingListActivity.TEXT_SIZE, 18);
                    setTextSize(textSize);
                }
                if (mMmsTextSizeAdjustPlugin != null) {
                    mMmsTextSizeAdjustPlugin.refresh();
                }
                mSimList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (view != null) {
                            MessageListItem mli = (MessageListItem)view;
                            /// M: add for multi-delete
                            if (mli.mSelectedBox != null && mli.mSelectedBox.getVisibility() == View.VISIBLE) {
                                if (!mli.mSelectedBox.isChecked()) {
                                    mli.setSelectedBackGroud(true);
                                } else {
                                    mli.setSelectedBackGroud(false);
                                }
                                Cursor cursor = (Cursor)mMsgListAdapter.getCursor();
                                String msgIndex = cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
                                MmsLog.d(MmsApp.TXN_TAG, "simMsg msgIndex = " + msgIndex);
                                String[] index = msgIndex.split(";");
                                mMsgListAdapter.changeSelectedState(index[0]);
                                updateActionBarText();
                                return;
                            }
                            mli.onMessageListItemClick();
                        }
                    }
                });
                mSimList.setOnCreateContextMenuListener(this);
                updateState(SHOW_LIST);
            } else {
                mMsgListAdapter.changeCursor(mCursor);
                updateState(SHOW_LIST);
            }
            startManagingCursor(mCursor);
        } else {
            MmsLog.d(TAG, "updateListWithCursor cursor is null");
            // Let user know the SIM is empty
            updateState(SHOW_EMPTY);
        }
        /// M: invoke this, so onPrepareOptionsMenu will be invoked. refresh the menu.
        invalidateOptionsMenu();
        checkDeleteMode();
        if (mMsgListAdapter != null) {
            mMsgListAdapter.initListMap(cursor);
        }
    }

    private void startQueryIcc() {
        try {
            isQuerying = true;
            String flag = (MmsConfig.getSIMLongSmsConcatenateEnabled()) ? "1" : "0";
            Uri queryUri = sSimMessageUri.buildUpon().appendQueryParameter(SHOW_IN_ONE, flag).build();
            /// M: add for OP09 @{
            if (MmsConfig.isSupportCTFeature() && mMmsManageSimMessagePlugin.isInternationalCard(mCurrentSlotId)) {
                queryUri = mMmsManageSimMessagePlugin.getAllContentUriForInternationalCard(mCurrentSlotId).buildUpon()
                        .appendQueryParameter(SHOW_IN_ONE, flag).build();
            }
            /// @}
            mQueryHandler.startQuery(0, null, queryUri, null, null, null, null);
            MmsLog.d(TAG, "startQueryIcc " + queryUri);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

/*    private void refreshMessageList() {
        /// M: fix bug ALPS00526071, remove "SimCapacity" menuitem
        invalidateOptionsMenu();
        /// @}
        startQuery();
    }*/

    @Override
    public void onCreateContextMenu(
            ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        if (mMsgListAdapter != null && mMsgListAdapter.mIsDeleteMode) {
            return;
        }

        /// M: Add for OP09 feature: display all messages in GSM mode and CDMA mode for international SIM card. The message
        /// which be stored in no activated mode folder can not be operated. @{
        if (MmsConfig.isSupportCTFeature() && !mMmsManageSimMessagePlugin.canBeOperated(mMsgListAdapter.getCursor())) {
            Toast.makeText(this, mStringReplacePlugin.getCTStrings(IStringReplacement.MESSAGE_CANNOT_BE_OPERATED),
                Toast.LENGTH_LONG).show();
            return;
        }
        /// @}
        menu.setHeaderTitle(R.string.message_options);
        AdapterView.AdapterContextMenuInfo info = null;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException exception) {
            Log.e(TAG, "Bad menuInfo.", exception);
        }
        if (info != null) {
            final Cursor cursor = (Cursor) mMsgListAdapter.getItem(info.position);
            addCallAndContactMenuItems(menu, cursor);
        }
        menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);
        menu.add(0, MENU_REPLY, 0, R.string.menu_reply);
        menu.add(0, MENU_COPY_TO_PHONE_MEMORY, 0,
                R.string.sim_copy_to_phone_memory);
        if ((null != mCursor) && (mCursor.getCount()) > 0) {
            menu.add(0, MENU_DELETE_FROM_SIM, 0, R.string.sim_delete);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        MmsLog.d(TAG, "onContextItemSelected " + item.getItemId());
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException exception) {
            Log.e(TAG, "Bad menuInfo.", exception);
            return false;
        }

        final Cursor cursor = (Cursor) mMsgListAdapter.getItem(info.position);
        if (cursor == null) {
            MmsLog.e(TAG, "Bad menuInfo, cursor is null");
            return false;
        }
        switch (item.getItemId()) {
        case MENU_COPY_TO_PHONE_MEMORY:
            copyToPhoneMemory(cursor);
            return true;
        case MENU_DELETE_FROM_SIM:
            final String msgIndex = getMsgIndexByCursor(cursor);
            confirmDeleteDialog(new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    updateState(SHOW_BUSY);
                    new Thread(new Runnable() {
                        public void run() {
                            mDeletedMessageSet.add(cursor.getLong(cursor
                                    .getColumnIndexOrThrow("_id")));
                            // deleteFromSim(msgIndex);
                            String[] index = msgIndex.split(";");
                            // Uri simUri =
                            // sDeleteAllContentUri.buildUpon().build();
                            Log.d(TAG, "onContextItemSelected startDelete length:" + index.length);
                            mQueryHandler.startDelete(/* DELETE_MESSAGE_TOKEN */0,
                                    index.length, sSimMessageUri, FOR_MULTIDELETE,
                                    index);
                            isDeleting = true;
                        }
                    }, "ManageSimMessages").start();
                    dialog.dismiss();
                }
            }, R.string.confirm_delete_SIM_message);
            return true;
        case MENU_FORWARD:
            forwardMessage(cursor);
            return true;
        case MENU_REPLY:
            replyMessage(cursor);
            return true;
        case MENU_ADD_TO_BOOKMARK:
            if (mURLs.size() == 1) {
                Browser.saveBookmark(ManageSimMessages.this, null, mURLs.get(0));
            } else if (mURLs.size() > 1) {
                CharSequence[] items = new CharSequence[mURLs.size()];
                for (int i = 0; i < mURLs.size(); i++) {
                    items[i] = mURLs.get(i);
                }
                new AlertDialog.Builder(ManageSimMessages.this)
                    .setTitle(R.string.menu_add_to_bookmark)
                    .setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Browser.saveBookmark(ManageSimMessages.this, null, mURLs.get(which));
                            }
                        })
                    .show();
            }
            return true;

        case MENU_ADD_CONTACT:
            String number = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            startActivity(ConversationList.createAddContactIntent(number));
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onResume() {
        MmsLog.d(TAG, "onResume");
        super.onResume();

        ///M: do not show sim messages when sim is off. @{
        if (!EncapsulatedSimInfoManager.isSimMessageAccessable(this, mCurrentSlotId)) {
            if (!MmsConfig.isSmsEnabled(this)) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.pref_title_sms_disabled), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(EncapsulatedR.string.sim_close),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
            return;
        }
        /// @}

        if (isQuerying && mCursor == null) {
            // This means app is querying SIM SMS when left activity last time
            updateState(SHOW_BUSY);
        }
    }

    @Override
    public void onPause() {
        MmsLog.d(TAG, "onPause");
        super.onPause();
        //invalidate cache to refresh contact data
        Contact.invalidateCache();
    }

    private void copyToPhoneMemory(Cursor cursor) {
        final String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        final String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        final Long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
        final String serviceCenter = cursor.getString(cursor.getColumnIndexOrThrow("service_center_address"));
        final boolean isIncomingMessage = isIncomingMessage(cursor);
        MmsLog.d(MmsApp.TXN_TAG, "\t address \t=" + address);
        MmsLog.d(MmsApp.TXN_TAG, "\t body \t=" + body);
        MmsLog.d(MmsApp.TXN_TAG, "\t date \t=" + date);
        MmsLog.d(MmsApp.TXN_TAG, "\t sc \t=" + serviceCenter);
        MmsLog.d(MmsApp.TXN_TAG, "\t isIncoming \t=" + isIncomingMessage);

        new Thread(new Runnable() {
            public void run() {
                try {
                    if (isIncomingMessage) {
                        MmsLog.d(MmsApp.TXN_TAG, "Copy incoming sms to phone");
                        EncapsulatedSimInfoManager simInfo = EncapsulatedSimInfoManager.getSimInfoBySlot(getApplicationContext(), mCurrentSlotId);
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                            if (simInfo != null) {
                                EncapsulatedTelephony.Sms.Inbox.addMessage(mContentResolver, address, body, null, serviceCenter, date, true,
                                        (int)simInfo.getSimId());
                            } else {
                                EncapsulatedTelephony.Sms.Inbox.addMessage(mContentResolver, address, body, null, serviceCenter, date, true, -1);
                            }
                        } else {
                            if (simInfo != null) {
                                EncapsulatedTelephony.Sms.Inbox.addMessage(mContentResolver, address, body, null, serviceCenter, date, true,
                                    (int)simInfo.getSimId());
                            } else {
                                EncapsulatedTelephony.Sms.Inbox.addMessage(mContentResolver, address, body, null, serviceCenter, date, true,-1);
                            }
                        }
                    } else {
                        /// M: outgoing sms has not date info
                        Long currentTime = System.currentTimeMillis();
                        MmsLog.d(MmsApp.TXN_TAG, "Copy outgoing sms to phone");
                        EncapsulatedSimInfoManager simInfo = EncapsulatedSimInfoManager.getSimInfoBySlot(getApplicationContext(), mCurrentSlotId);
                        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                            if (simInfo != null) {
                                EncapsulatedTelephony.Sms.Sent.addMessage(mContentResolver, address, body, null, serviceCenter, currentTime,
                                        (int)simInfo.getSimId());
                            } else {
                                EncapsulatedTelephony.Sms.Sent.addMessage(mContentResolver, address, body, null, serviceCenter, currentTime, -1);
                            }
                        } else {
                            if (simInfo != null) {
                                EncapsulatedTelephony.Sms.Sent.addMessage(mContentResolver, address, body, null, serviceCenter, currentTime,
                                    (int)simInfo.getSimId());
                            } else {
                                EncapsulatedTelephony.Sms.Sent.addMessage(mContentResolver, address, body, null, serviceCenter, currentTime,-1);
                            }
                        }
                    }
                    Recycler.getSmsRecycler().deleteOldMessages(getApplicationContext());
                    MmsApp.getToastHandler().sendEmptyMessage(MmsApp.MSG_DONE);
                } catch (SQLiteException e) {
                    SqliteWrapper.checkSQLiteException(getApplicationContext(), e);
                }
            }
        }, "copyToPhoneMemory").start();
    }

    private boolean isIncomingMessage(Cursor cursor) {
        int messageStatus = cursor.getInt(
                cursor.getColumnIndexOrThrow("status"));
        MmsLog.d(MmsApp.TXN_TAG, "message status:" + messageStatus);
        return (messageStatus == SmsManager.STATUS_ON_ICC_READ) ||
               (messageStatus == SmsManager.STATUS_ON_ICC_UNREAD);
    }

    private String getMsgIndexByCursor(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
    }

    private Menu mOptionMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionMenu = menu;
        menu.add(0, OPTION_MENU_DELETE, 0, R.string.menu_delete_messages)
                .setIcon(android.R.drawable.ic_menu_delete);
        menu.add(0, OPTION_MENU_SIM_CAPACITY, 0,
                R.string.menu_show_icc_sms_capacity).setIcon(R.drawable.ic_menu_sim_capacity);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        /// M: fix bug ALPS00526071, remove "SimCapacity" menuitem in Sim Sms Selection Mode
        boolean isShowMenu = setOptionMenu();
        return isShowMenu;
    }

    private boolean setOptionMenu() {
        if (mOptionMenu == null) {
            return false;
        }

        /// M: fix bug ALPS00526071, remove "SimCapacity" menuitem in Sim Sms Selection Mode @{
        boolean isShowDelectAll = (null != mCursor) && (mCursor.getCount() > 0)
                                && mState == SHOW_LIST && !mMsgListAdapter.mIsDeleteMode;

        boolean isShowCapacity = (mState == SHOW_LIST || mState == SHOW_EMPTY)
                                && (mMsgListAdapter == null || !mMsgListAdapter.mIsDeleteMode);
        /// @}

        MenuItem miDeleteAll = mOptionMenu.findItem(OPTION_MENU_DELETE);
        if (miDeleteAll != null) {
            miDeleteAll.setVisible(isShowDelectAll);
        }

        MenuItem miSimCapacity = mOptionMenu.findItem(OPTION_MENU_SIM_CAPACITY);
        if (miSimCapacity != null) {
            miSimCapacity.setVisible(isShowCapacity);
        }

        return isShowDelectAll || isShowCapacity;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPTION_MENU_DELETE:
                mMsgListAdapter.mIsDeleteMode = true;
                item.setVisible(false);
                checkDeleteMode();
                /// M: fix bug ALPS00526071, remove "SimCapacity" menuitem
                invalidateOptionsMenu();
                /// @}
                /*
                confirmDeleteDialog(new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updateState(SHOW_BUSY);
                        //deleteAllFromSim();
                        new Thread(new Runnable() {
                            public void run() {
                                deleteAllFromSim();
                            }
                        }, "ManageSimMessages").start();
                        dialog.dismiss();
                    }
                }, R.string.confirm_delete_all_SIM_messages);
                */
                break;
            case OPTION_MENU_SIM_CAPACITY:
                ///M: Put checking SIM capacity action to background, because sometimes it cost long and
                // caused ANR.
                new CheckSimCapacityTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                break;
        }

        return true;
    }

    private void confirmDeleteDialog(OnClickListener listener, int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);

        /// M: For OP09 Feature, replace "SIM" with "UIM". @{
        String ctString = mStringReplacePlugin.getCTStrings(IStringReplacement.CONFIRM_DELETE_MSG);
        if (ctString != null && MessageUtils.isUSimType(mCurrentSlotId)) {
            builder.setMessage(ctString);
        } else {
            builder.setMessage(messageId);
        } /// @}

        builder.show();
    }

    private void updateState(int state) {
        MmsLog.d(TAG, "updateState, state = " + state);
        if (mState == state) {
            return;
        }

        mState = state;
        switch (state) {
            case SHOW_LIST:
                removeDialog(DIALOG_REFRESH);
                mSimList.setVisibility(View.VISIBLE);
                mSimList.requestFocus();
                mSimList.setSelection(mSimList.getCount() - 1);
                mMessage.setVisibility(View.GONE);

                /// M: For OP09 Feature, replace "SIM" with "UIM". @{
                String ctString = mStringReplacePlugin.getCTStrings(IStringReplacement.MANAGE_UIM_MESSAGE);
                if (ctString != null && MessageUtils.isUSimType(mCurrentSlotId)) {
                    setTitle(ctString);
                } else {
                    setTitle(getString(R.string.sim_manage_messages_title));
                } /// @}
                break;
            case SHOW_EMPTY:
                removeDialog(DIALOG_REFRESH);
                mSimList.setVisibility(View.GONE);

                /// M: For OP09 Feature, replace "SIM" with "UIM". @{
                String string = mStringReplacePlugin.getCTStrings(IStringReplacement.MANAGE_UIM_MESSAGE);
                if (string != null && MessageUtils.isUSimType(mCurrentSlotId)) {
                    setTitle(string);
                    mMessage.setText(mStringReplacePlugin.getCTStrings(IStringReplacement.UIM_EMPTY));
                } else {
                    setTitle(getString(R.string.sim_manage_messages_title));
                } /// @}

                mMessage.setVisibility(View.VISIBLE);
                break;
            case SHOW_BUSY:
                mSimList.setVisibility(View.GONE);
                mMessage.setVisibility(View.GONE);
                setTitle(getString(R.string.refreshing));
                showDialog(DIALOG_REFRESH);
                break;
            default:
                Log.e(TAG, "Invalid State");
        }
    }

    private void viewMessage(Cursor cursor) {
        // TODO: Add this.
    }

    @Override
    protected void onDestroy() {
        MmsLog.d(TAG,"onDestroy");
        super.onDestroy();
        /// M: fix bug ALPS00414035
        unregisterReceiver(mSimReceiver);
        ///M: for 'finish activity when airplane mode on' function.
        unregisterReceiver(airPlaneReceiver);
        mQueryHandler.removeCallbacksAndMessages(null);

        /// M: fix bug ALPS00448222, update ListView when contact update
        Contact.removeListener(this);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (null != intent && null != intent.getData()
                && intent.getData().getScheme().equals("mailto")) {
            try {
                super.startActivityForResult(intent, requestCode);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Failed to startActivityForResult: " + intent);
                Intent i = new Intent().setClassName("com.android.email",
                        "com.android.email.activity.setup.AccountSetupBasics");
                try {
                    this.startActivity(i);
                } catch (ActivityNotFoundException ex) {
                    Log.e(TAG, "Failed to startActivityForResult: " + intent);
                    Toast.makeText(this,getString(R.string.message_open_email_fail),
                          Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        try {
            super.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent mChooserIntent = Intent.createChooser(intent, null);
            super.startActivity(mChooserIntent);
        }
    }

    private void forwardMessage(Cursor cursor) {
        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        Intent intent = new Intent();
        intent.setClassName(this, "com.android.mms.ui.ForwardMessageActivity");
        intent.putExtra(ComposeMessageActivity.FORWARD_MESSAGE, true);
        if (body != null) {
            if (mMmsTextSizeAdjustPlugin.isAppendSender()) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ManageSimMessages.this);
                boolean forwardWithSender = prefs.getBoolean(SmsPreferenceActivity.SMS_FORWARD_WITH_SENDER, true);
                MmsLog.d(TAG, "forwardMessage(): SMS Forward With Sender ?= " + forwardWithSender);
                if (forwardWithSender) {
                    long mStatus = cursor.getLong(cursor.getColumnIndexOrThrow("status"));
                    if (mStatus == SmsManager.STATUS_ON_ICC_SENT
                            || mStatus == SmsManager.STATUS_ON_ICC_UNSENT) {
                        MmsLog.d(TAG, "forwardMessage(): BoxId = Sms.MESSAGE_TYPE_SENT mStatus ="+mStatus);
                    } else {
                        MmsLog.d(TAG, "forwardMessage(): BoxId = Sms.MESSAGE_TYPE_INBOX mStatus ="+mStatus);
                        String mAddr = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                        StringBuffer strBodybuf = new StringBuffer();
                        strBodybuf.append(body);
                        strBodybuf.append("\n");
                        strBodybuf.append(getString(R.string.forward_from));
                        Contact contact = Contact.get(mAddr, false);
                        String number = Contact.formatNameAndNumber(contact.getName(), contact.getNumber(),"");
                        MmsLog.d(TAG, "forwardMessage(): Contact's name and number=" + number);
                        strBodybuf.append(number);
                        body = strBodybuf.toString();
                    }
                }
            }
            intent.putExtra(ComposeMessageActivity.SMS_BODY, body);
        }
        startActivity(intent);
    }
    
    private void replyMessage(Cursor cursor) {
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("sms", address, null));
        startActivity(intent);
    }

    private void addCallAndContactMenuItems(ContextMenu menu, Cursor cursor) {
        /// M: Add all possible links in the address & message
        StringBuilder textToSpannify = new StringBuilder();  
        String reciBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        String reciNumber = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        textToSpannify.append(reciNumber + ": ");
        textToSpannify.append(reciBody);
        SpannableString msg = new SpannableString(textToSpannify.toString());
        Linkify.addLinks(msg, Linkify.ALL);
        ArrayList<String> uris =
            MessageUtils.extractUris(msg.getSpans(0, msg.length(), URLSpan.class));
        mURLs.clear();
        Log.d(TAG, "addCallAndContactMenuItems uris.size() = " + uris.size());
        while (uris.size() > 0) {
            String uriString = uris.remove(0);
            // Remove any dupes so they don't get added to the menu multiple times
            while (uris.contains(uriString)) {
                uris.remove(uriString);
            }

            int sep = uriString.indexOf(":");
            String prefix = "";
            if (sep >= 0) {
                prefix = uriString.substring(0, sep);
                if ("mailto".equalsIgnoreCase(prefix) || "tel".equalsIgnoreCase(prefix)) {
                    uriString = uriString.substring(sep + 1);
                }
            }
            boolean addToContacts = false;
            if ("mailto".equalsIgnoreCase(prefix)) {
                String sendEmailString = getString(R.string.menu_send_email).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("mailto:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//                menu.add(0, MENU_SEND_EMAIL, 0, sendEmailString).setIntent(intent);
                addToContacts = !MessageUtils.haveEmailContact(uriString, this);
            } else if ("tel".equalsIgnoreCase(prefix)) {
                addToContacts = !isNumberInContacts(uriString);
                MmsLog.d(TAG, "addCallAndContactMenuItems  addToContacts2 = " + addToContacts);
            } else {
                //add URL to book mark
                if (mURLs.size() <= 0) {
                    menu.add(0, MENU_ADD_TO_BOOKMARK, 0, R.string.menu_add_to_bookmark);
                }
                mURLs.add(uriString);
            }
            if (addToContacts) {
                 Intent intent = ConversationList.createAddContactIntent(uriString);
                //Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                //intent.putExtra(ContactsContract.Intents.Insert.PHONE, uriString);
                String addContactString = getString(
                        R.string.menu_add_address_to_contacts).replace("%s", uriString);
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, addContactString)
                    .setIntent(intent);
            }
        }
    }
    
    private boolean addRecipientToContact(ContextMenu menu, Cursor cursor) {
        boolean showAddContact = false;
        String reciNumber = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        Log.d(TAG, "addRecipientToContact reciNumber = " + reciNumber);
        // if there is at least one number not exist in contact db, should show add.
        mContactList = ContactList.getByNumbers(reciNumber, false, true);
        for (Contact contact : mContactList) {
            if (!contact.existsInDatabase()) {
                 showAddContact = true;
                 Log.d(TAG, "not in contact[number:" + contact.getNumber() + ",name:" + contact.getName());
                 break;
             }
         }
        boolean menuAddExist = (menu.findItem(MENU_ADD_CONTACT) != null);
        if (showAddContact) {
            if (!menuAddExist) {
                menu.add(0, MENU_ADD_CONTACT, 1, R.string.menu_add_to_contacts).setIcon(R.drawable.ic_menu_contact);
            }
        } else {
             menu.removeItem(MENU_ADD_CONTACT);
        }
        return true;
    }
    private boolean isNumberInContacts(String phoneNumber) {
        return Contact.get(phoneNumber, true).existsInDatabase();
    }

    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {             
            case MessageListItem.ITEM_CLICK:
                break;

            default:                
                return;
            }
        }
    };


    @Override 
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_REFRESH) {
            if (mDialog != null && mDialog.getContext()!= this) {
                removeDialog(DIALOG_REFRESH);
                MmsLog.d(TAG, "onCreateDialog mDialog is not null");
            }
            /// M: use QueryProcessDialog for function: can finish ManageSimMessage activity
            // when querying if user click back key
            mDialog = new QueryProcessDialog(this);
            mDialog.setIndeterminate(true);
            mDialog.setCancelable(false);
            mDialog.setMessage(getString(R.string.refreshing));
            return mDialog;
        }

        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mMsgListAdapter != null && mMsgListAdapter.mIsDeleteMode) {
                mMsgListAdapter.mIsDeleteMode = false;
                checkDeleteMode();
                /// M: fix bug ALPS00526071, remove "SimCapacity" menuitem
                invalidateOptionsMenu();
                /// @}
               return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void confirmMultiDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setMessage(R.string.confirm_delete_selected_messages);
        /// M: Add for OP09; change the notice message for cannot delete unacitivated message. @{
        if (MmsConfig.isSupportCTFeature()
            && mMmsManageSimMessagePlugin.hasIncludeUnoperatedMessage(mMsgListAdapter.getSimMsgItemList().entrySet()
                    .iterator())) {
            builder.setMessage(mStringReplacePlugin.getCTStrings(IStringReplacement.CONFIRM_DELETE_SELECTED_MESSAGES));
        }
        /// @}
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int which) {
                 mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP,
                        ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                                | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
                 updateState(SHOW_BUSY);
                 new Thread(new Runnable() {
                     public void run() {
                         int count = mMsgListAdapter.getCount();
                         Map<String, Boolean> simMsgList = mMsgListAdapter.getSimMsgItemList();
                         ArrayList<String> selectedSimIds = new ArrayList<String>();
                         for(int position = 0; position < count; position++){
                             Cursor cursor = (Cursor) mMsgListAdapter.getItem(position);
                             String msgIndex = getMsgIndexByCursor(cursor);
                             String[] index = msgIndex.split(";");
                             if ((simMsgList.get(index[0]) != null) && simMsgList.get(index[0])) {
                                 for (int n = 0; n < index.length; n++) {
                                     selectedSimIds.add(index[n]);
                                 }
                             mDeletedMessageSet.add(cursor.getLong(cursor
                                    .getColumnIndexOrThrow("_id")));
                            }
                         }
                         String[] argsSimMsg = selectedSimIds.toArray(new String[selectedSimIds.size()]);
                         /// M: Add for OP09; remove the messages' id which can not deleted. @{
                         if (MmsConfig.isSupportCTFeature()) {
                             argsSimMsg = mMmsManageSimMessagePlugin.filterUnoperatedMsgs(argsSimMsg);
                         }
                         /// @}
                         Log.d(TAG, "confirmMultiDelete startDelete length:" + argsSimMsg.length);
                         mQueryHandler.startDelete(/*DELETE_MESSAGE_TOKEN*/0,
                                 argsSimMsg.length, sSimMessageUri, FOR_MULTIDELETE, argsSimMsg);
                         isDeleting = true;
                     }
                 }).start();
                 mMsgListAdapter.mIsDeleteMode = false;
             }
         });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
     }

    private void checkDeleteMode() {
        if (mMsgListAdapter == null) {
            return;
        }
        markCheckedState(false);
        if (mMsgListAdapter.mIsDeleteMode) {
            setUpActionBar();
        } else {
            mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        }
        mSimList.invalidateViews();
    }

    private void markCheckedState(boolean checkedState) {
        mMsgListAdapter.setSimItemsValue(checkedState, null);
//        mDeleteButton.setEnabled(checkedState);
        int count = mSimList.getChildCount();
        MessageListItem item = null;
        for (int i = 0; i < count; i++) {
            item = (MessageListItem)mSimList.getChildAt(i);
            if (null != item) {
                item.setSelectedBackGroud(checkedState);
            }
        }
        updateActionBarText();
    }

    public void setTextSize(float size) {
        if (mMsgListAdapter != null) {
            mMsgListAdapter.setTextSize(size);
        }
        
        if (mSimList != null && mSimList.getVisibility() == View.VISIBLE) {
            int count = mSimList.getChildCount();
            for (int i = 0; i < count; i++) {
                MessageListItem item =  (MessageListItem)mSimList.getChildAt(i);
                if (item != null) {
                    item.setBodyTextSize(size);
                }
            }
        }
    }

    private void initPlugin(Context context) {
        mMmsTextSizeAdjustPlugin = (IMmsTextSizeAdjust)MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_TEXT_SIZE_ADJUST);
        if (mMmsTextSizeAdjustPlugin != null) {
            mMmsTextSizeAdjustPlugin.init(this, this);
        }
        try {
            mMmsManageSimMessagePlugin = (IMmsManageSimMessage) EncapsulatedPluginManager.createPluginObject(context,
                IMmsManageSimMessage.class.getName());
            MmsLog.d(TAG, "operator mMmsManageSimMessagePlugin = " + mMmsManageSimMessagePlugin);
        } catch (AndroidException e) {
            mMmsManageSimMessagePlugin = new MmsManageSimMessageImpl(context);
            MmsLog.d(TAG, "default mMmsManageSimMessagePlugin = " + mMmsManageSimMessagePlugin);
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean ret = false;
        if (mMmsTextSizeAdjustPlugin != null) {
            ret = mMmsTextSizeAdjustPlugin.dispatchTouchEvent(ev);
        }
        if (!ret) {
            ret = super.dispatchTouchEvent(ev);
        }
        return ret;
    }

/*    public void showAirPlaneToast(){
        String airPlaneString = getString(EncapsulatedR.string.sim_close) + "," + getString(R.string.delete_unsuccessful);
        Toast.makeText(this, airPlaneString, Toast.LENGTH_SHORT).show();
    }*/

    /// M: update sim state dynamically. @{
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MmsLog.d(TAG, "onReceive mSimReceiver action: " + action);
            if (action.equals(EncapsulatedTelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                int slotId = intent.getIntExtra(EncapsulatedTelephonyIntents.INTENT_KEY_ICC_SLOT, -1);
                if ((EncapsulatedSimInfoManager.getSimInfoBySlot(getApplicationContext(), slotId) == null) && mCurrentSlotId == slotId) {
                    finish();
                }
            } else if(action.equals(Telephony.Sms.Intents.SIM_FULL_ACTION)){
                int slotId = 0;
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    slotId = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, EncapsulatedPhone.GEMINI_SIM_1);
                }
                if(mCurrentSlotId == slotId) {
                    mIsCurrentSimFull = true;
                }
            }
        }
    };

    /// M: fix bug ALPS00448222, update ListView when contact update
    public void onUpdate(Contact updated) {
        mHandler.post(new Runnable() {
            public void run() {
                // TODO Auto-generated method stub
                if (mMsgListAdapter != null) {
                    mMsgListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /// M: redesign select all action. @{

    private Button mActionBarText;
    private MenuItem mSelectionItem;
    private DropDownMenu mSelectionMenu;
    private ActionBar mActionBar;
    private void setUpActionBar() {
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);

        CustomMenu customMenu = new CustomMenu(this);
        View customView = LayoutInflater.from(this).inflate(
                R.layout.multi_select_simsms_actionbar, null);

        /// M: fix bug ALPS00441681, re-layout for landscape
        mActionBar.setCustomView(customView,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                        ActionBar.LayoutParams.MATCH_PARENT,
                        Gravity.FILL));

        mActionBarText = (Button) customView.findViewById(R.id.selection_menu);
        mSelectionMenu = customMenu.addDropDownMenu(mActionBarText, R.menu.selection);
        mSelectionItem = mSelectionMenu.findItem(R.id.action_select_all);

        customMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if ((mMsgListAdapter.getSelectedNumber() > 0)
                        && (mMsgListAdapter.getSelectedNumber() >= mMsgListAdapter.getCount())) {
                    markCheckedState(false);
                } else {
                    markCheckedState(true);
                }
                return false;
            }
        });

        Button cancelSelection = (Button) customView.findViewById(R.id.selection_cancel);
        cancelSelection.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                /// M: fix bug ALPS00526071, remove "SimCapacity" menuitem
                invalidateOptionsMenu();
                /// @}
                mMsgListAdapter.mIsDeleteMode = false;
                checkDeleteMode();
                mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP
                        | ActionBar.DISPLAY_SHOW_TITLE,
                        ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
            }
        });

        Button deleteSelection = (Button) customView.findViewById(R.id.selection_done);
        deleteSelection.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /// M: fix bug ALPS00526071, remove "SimCapacity" menuitem
                invalidateOptionsMenu();
                /// @}
                if (mMsgListAdapter.getSelectedNumber() > 0) {
                    confirmMultiDelete();
                }
            }
        });
        updateActionBarText();
    }

    private void updateActionBarText() {
        if (mMsgListAdapter != null && mActionBarText != null) {
            mActionBarText.setText(getResources().getQuantityString(
                    R.plurals.message_view_selected_message_count,
                    mMsgListAdapter.getSelectedNumber(),
                    mMsgListAdapter.getSelectedNumber()));
        }

        if (mSelectionItem != null && mMsgListAdapter != null) {
            if ((mMsgListAdapter.getSelectedNumber() > 0)
                    && (mMsgListAdapter.getSelectedNumber() >= mMsgListAdapter.getCount())) {
                mSelectionItem.setChecked(true);
                mSelectionItem.setTitle(R.string.unselect_all);
            } else {
                mSelectionItem.setChecked(false);
                mSelectionItem.setTitle(R.string.select_all);
            }
        }
    }

    /// @}

    /**
     *  M: listen this for finishing activity if airplane mode on.
     */
    private final BroadcastReceiver airPlaneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean airplaneOn = intent.getBooleanExtra("state", false);
            MmsLog.d(TAG, "onReceive airPlaneReceiver airplaneOn " + airplaneOn);
            if (airplaneOn) {
                Toast.makeText(getApplicationContext(), getString(EncapsulatedR.string.sim_close),
                        Toast.LENGTH_SHORT).show();
                ManageSimMessages.this.finish();
            }
        }
    };

    /**
     * M: check whether current SIM memory is full.
     * @return
     */
    private boolean isCurrentSimFullGemini() {
        boolean isFull = false;
        EncapsulatedSmsMemoryStatus simMemStatus = null;
        simMemStatus = EncapsulatedGeminiSmsManager.getSmsSimMemoryStatusGemini(mCurrentSlotId);
        if (null != simMemStatus) {
            isFull = simMemStatus.getUsed() == simMemStatus.getTotal();
            MmsLog.d(TAG, "isCurrentSimFull isFull= " + isFull);
        }
        return isFull;
    }

    public void updateListAfterDelete() {
        if (mCursor == null || mCursor.isClosed()) {
            return;
        }
        Cursor filertedMessges = null;
        filertedMessges = buildFilteredCursor(mMsgListAdapter.getCursor());
        updateListWithCursor(filertedMessges);
    }

    private Cursor buildFilteredCursor(Cursor messages) {
        MatrixCursor cursor = new MatrixCursor(mMsgListAdapter.getCursor().getColumnNames(), 1);
        messages.moveToPosition(-1);
        while (messages.moveToNext()) {
            Long messageId = messages.getLong(cursor.getColumnIndexOrThrow("_id"));
            if (!mDeletedMessageSet.contains(messageId)) {
                addSMRow(cursor, messages);
            }
        }
        return cursor;
    }

    private void addSMRow(MatrixCursor targetCursor, Cursor sourceCursor) {
        Object[] row = new Object[14];
        row[0] = sourceCursor.getLong(COLUMN_SERVICE_CENTER_ADDRESS);
        row[1] = sourceCursor.getString(COLUMN_ADDRESS);
        row[2] = sourceCursor.getString(COLUMN_MESSAGE_CLASS);
        row[3] = sourceCursor.getString(COLUMN_BODY);
        row[4] = sourceCursor.getLong(COLUMN_DATE);
        row[5] = sourceCursor.getInt(COLUMN_STATUS);
        row[6] = sourceCursor.getString(COLUMN_INDEX_ON_ICC);
        row[7] = Boolean.getBoolean(sourceCursor.getString(COLUMN_IS_STATUS_REPORT));
        row[8] = sourceCursor.getString(COLUMN_TRANSPORT_TYPE);
        row[9] = sourceCursor.getInt(COLUMN_TYPE);
        row[10] = sourceCursor.getInt(COLUMN_LOCKED);
        row[11] = sourceCursor.getInt(COLUMN_ERROR_CODE);
        row[12] = sourceCursor.getInt(COLUMN_ID);
        row[13] = sourceCursor.getInt(COLUMN_SIM_ID);
        targetCursor.addRow(row);
    }

    /**
     * M: Put checking SIM capacity action to background, because sometimes it cost long and
     * caused ANR.
     */
    private class CheckSimCapacityTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MmsLog.d(TAG, "CheckSimCapacityTask onPreExecute");
            // / M: fix bug ALPS00526071, remove "SimCapacity" menuitem
            invalidateOptionsMenu();
            // / @}
            showDialog(DIALOG_REFRESH);
        }

        @Override
        protected String doInBackground(Void... params) {
            return checkSimCapacity();
        }

        @Override
        protected void onPostExecute(String message) {
            MmsLog.d(TAG, "CheckSimCapacityTask onPostExecute");
            removeDialog(DIALOG_REFRESH);
            new AlertDialog.Builder(ManageSimMessages.this).setIconAttribute(
                    android.R.attr.alertDialogIcon).setTitle(R.string.show_icc_sms_capacity_title)
                    .setMessage(message).setPositiveButton(android.R.string.ok, null)
                    .setCancelable(true).show();
        }
    }

    public String checkSimCapacity() {
        String message = null;
        EncapsulatedSmsMemoryStatus simMemStatus = null;
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            simMemStatus = EncapsulatedGeminiSmsManager.getSmsSimMemoryStatusGemini(mCurrentSlotId);
        } else {
            simMemStatus = EncapsulatedSmsManager.getSmsSimMemoryStatus();
        }
        if (null != simMemStatus) {
            message = getString(R.string.icc_sms_used) + Integer.toString(simMemStatus.getUsed())
                    + "\n" + getString(R.string.icc_sms_total)
                    + Integer.toString(simMemStatus.getTotal());
            /// M: Add for OP09 @{
            if (MmsConfig.isSupportCTFeature()
                    && mMmsManageSimMessagePlugin.isInternationalCard(mCurrentSlotId)) {
                message = message + "\n"
                        + mStringReplacePlugin.getCTStrings(IStringReplacement.CAPACITY_SIM_CARD);
            }
            /// @}
        } else {
            /// M: For OP09 Feature, replace "SIM" with "UIM". @{
            String ctString = mStringReplacePlugin
                    .getCTStrings(IStringReplacement.GET_CAPACITY_FAILED);
            if (ctString != null && MessageUtils.isUSimType(mCurrentSlotId)) {
                message = ctString;
            } else {
                message = getString(R.string.get_icc_sms_capacity_failed);
            }
            // / @}
        }
        MmsLog.d(TAG, "checkSimCapacity " + message);
        return message;
    }

    /**
     * M: For function: can finish ManageSimMessage activity when querying if user
     * click back key.
     */
    public class QueryProcessDialog extends ProgressDialog {
        public QueryProcessDialog(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if (event.getAction() == KeyEvent.ACTION_DOWN && isQuerying && isShowing()) {
                        Log.d(TAG, "QueryProcessDialog received KEYCODE_BACK so finish activity");
                        dismiss();
                        mQueryCapacityAndMsgTask.cancel(true);
                        ManageSimMessages.this.finish();
                        return true;
                    }
            }
            return super.dispatchKeyEvent(event);
        }
    }

}

