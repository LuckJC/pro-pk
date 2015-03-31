/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.providers.telephony;

import android.app.AppOpsManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.TextBasedSmsColumns;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.provider.Telephony.ThreadsColumns;
import android.provider.Telephony.Threads;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import com.mediatek.encapsulation.MmsLog;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsMessage;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SmsHeader;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSimInfoManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.telephony.SmsManagerEx;
import com.mediatek.telephony.TelephonyManagerEx;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.common.telephony.ITelephonyEx;
import com.mediatek.common.telephony.internationalroaming.InternationalRoamingConstants;

// M: MoMS for controling database access ability @{
import com.mediatek.common.mom.SubPermissions;
// @}

public class SmsProvider extends ContentProvider {
    private static final Uri NOTIFICATION_URI = Uri.parse("content://sms");
    private static final Uri ICC_URI = Uri.parse("content://sms/icc");
    /// M: Code analyze 001, new feature, support for gemini.
    private static final Uri ICC_URI_GEMINI = Uri.parse("content://sms/icc2");
    private static final Uri ICC_URI_THREE = Uri.parse("content://sms/icc3");
    private static final Uri ICC_URI_FOUR = Uri.parse("content://sms/icc4");
    /// M: New Feature The international card.
    private static final Uri ICC_URI_INTERNATIONAL = Uri.parse("content://sms/icc_international");
    private static final Uri ICC_URI_GEMINI_INTERNATIONAL = Uri.parse("content://sms/icc2_international");
    private boolean mIsInternationalCardNotActivate = false;
    static final String TABLE_SMS = "sms";
    private static final String TABLE_RAW = "raw";
    private static final String TABLE_SR_PENDING = "sr_pending";
    private static final String TABLE_WORDS = "words";
    /// M: Code analyze 002, fix bug ALPS00046358, improve multi-delete speed by use batch
    /// processing. reference from page http://www.erpgear.com/show.php?contentid=1111.
    private static final String FOR_MULTIDELETE = "ForMultiDelete";
    private static final Integer ONE = Integer.valueOf(1);
    private static final int PERSON_ID_COLUMN = 0;
    /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
    /// remove useless operation and add transaction while import sms. @{
    private static final int NORMAL_NUMBER_MAX_LENGTH = 15;
    private static final String[] CANONICAL_ADDRESSES_COLUMNS_2 =
    new String[] { CanonicalAddressesColumns._ID, CanonicalAddressesColumns.ADDRESS };
    /// @}
    /// M: Code analyze 006, fix bug ALPS00252799, it cost long time to restore messages.
    /// support batch processing while restore messages. @{
    /**
     * Maximum number of operations allowed in a batch
     */
    private static final int MAX_OPERATIONS_PER_PATCH = 50;
    /// @}
    /**
     * These are the columns that are available when reading SMS
     * messages from the ICC.  Columns whose names begin with "is_"
     * have either "true" or "false" as their values.
     */
    private final static String[] ICC_COLUMNS = new String[] {
        // N.B.: These columns must appear in the same order as the
        // calls to add appear in convertIccToSms.
        "service_center_address",       // getServiceCenterAddress
        "address",                      // getDisplayOriginatingAddress
        "message_class",                // getMessageClass
        "body",                         // getDisplayMessageBody
        "date",                         // getTimestampMillis
        "status",                       // getStatusOnIcc
        "index_on_icc",                 // getIndexOnIcc
        "is_status_report",             // isStatusReportMessage
        "transport_type",               // Always "sms".
        "type",                         // Always MESSAGE_TYPE_ALL.
        "locked",                       // Always 0 (false).
        "error_code",                   // Always 0
        "_id",
        /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
        /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
        /// on left and sent sms on right, display sender information for every sms.
        "sim_id"                        // sim id
    };
    /// M: Code analyze 008, fix bug ALPS00262044, not show out unread message
    /// icon after restore messages. notify mms application about unread messages
    /// number after insert operation.
    private static boolean notify = false;
    @Override
    public boolean onCreate() {
        // M: MoMS for controling database access ability
        setQueryPermission(SubPermissions.QUERY_SMS);

        setAppOps(AppOpsManager.OP_READ_SMS, AppOpsManager.OP_WRITE_SMS);
        mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        MmsLog.d(TAG, "query begin, uri = " + url + ", selection = " + selection);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Generate the body of the query.
        int match = sURLMatcher.match(url);
        switch (match) {
            case SMS_ALL:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_ALL);
                break;

            case SMS_UNDELIVERED:
                constructQueryForUndelivered(qb);
                break;

            case SMS_FAILED:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_FAILED);
                break;

            case SMS_QUEUED:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_QUEUED);
                break;

            case SMS_INBOX:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_INBOX);
                break;

            case SMS_SENT:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_SENT);
                break;

            case SMS_DRAFT:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_DRAFT);
                break;

            case SMS_OUTBOX:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_OUTBOX);
                break;

            case SMS_ALL_ID:
                qb.setTables(TABLE_SMS);
                qb.appendWhere("(_id = " + url.getPathSegments().get(0) + ")");
                break;

            case SMS_INBOX_ID:
            case SMS_FAILED_ID:
            case SMS_SENT_ID:
            case SMS_DRAFT_ID:
            case SMS_OUTBOX_ID:
                qb.setTables(TABLE_SMS);
                qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                break;

            case SMS_CONVERSATIONS_ID:
                int threadID;

                try {
                    threadID = Integer.parseInt(url.getPathSegments().get(1));
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.d(TAG, "query conversations: threadID=" + threadID);
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG,
                          "Bad conversation thread id: "
                          + url.getPathSegments().get(1));
                    return null;
                }

                qb.setTables(TABLE_SMS);
                qb.appendWhere("thread_id = " + threadID);
                break;

            case SMS_CONVERSATIONS:
                qb.setTables("sms, (SELECT thread_id AS group_thread_id, MAX(date)AS group_date,"
                       + "COUNT(*) AS msg_count FROM sms GROUP BY thread_id) AS groups");
                qb.appendWhere("sms.thread_id = groups.group_thread_id AND sms.date ="
                       + "groups.group_date");
                qb.setProjectionMap(sConversationProjectionMap);
                break;

            case SMS_RAW_MESSAGE:
                qb.setTables("raw");
                break;

            case SMS_STATUS_PENDING:
                qb.setTables("sr_pending");
                break;

            case SMS_ATTACHMENT:
                qb.setTables("attachments");
                break;

            case SMS_ATTACHMENT_ID:
                qb.setTables("attachments");
                qb.appendWhere(
                        "(sms_id = " + url.getPathSegments().get(1) + ")");
                break;

            case SMS_QUERY_THREAD_ID:
                qb.setTables("canonical_addresses");
                if (projectionIn == null) {
                    projectionIn = sIDProjection;
                }
                break;

            case SMS_STATUS_ID:
                qb.setTables(TABLE_SMS);
                qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                break;

            case SMS_ALL_ICC:
                /// M: Code analyze 009, new feature, support for gemini and plugin.
                /// use a flag "showInOne" indicate show long sms in one bubble or not. @{
                return getAllMessagesFromIcc(url, PhoneConstants.GEMINI_SIM_1);
                /// @}
            /// M: Code analyze 010, new feature, support for gemini. @{
            case SMS_ICC:
                String messageIndexString = url.getPathSegments().get(1);

                if(EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT == true) {
                    return getSingleMessageFromIcc(messageIndexString, PhoneConstants.GEMINI_SIM_1);
                } else {
                    return getSingleMessageFromIcc(messageIndexString);
                }
            case SMS_ALL_ICC_GEMINI:
                /// M: Code analyze 009, new feature, support for plugin.
                /// use a flag "showInOne" indicate show long sms in one bubble or not.
                return getAllMessagesFromIcc(url, PhoneConstants.GEMINI_SIM_2);
            case SMS_ALL_ICC_THREE:
                return getAllMessagesFromIcc(url, PhoneConstants.GEMINI_SIM_3);
            case SMS_ALL_ICC_FOUR:
                return getAllMessagesFromIcc(url, PhoneConstants.GEMINI_SIM_4);
            /// @}
            /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
            /// use new process of delete. @{
            case SMS_ALL_THREADID:
                /// M: return all the distinct threadid from sms table
                return getAllSmsThreadIds(selection, selectionArgs);
            /// M: New Feature The international card.
            case SMS_ALL_ICC_INTERNATIONAL:
                return getAllMessagesFromIccInternational(url, PhoneConstants.GEMINI_SIM_1);
            /// @}
            default:
                Log.e(TAG, "Invalid request: " + url);
                return null;
        }

        String orderBy = null;

        if (!TextUtils.isEmpty(sort)) {
            orderBy = sort;
        } else if (qb.getTables().equals(TABLE_SMS)) {
            orderBy = Sms.DEFAULT_SORT_ORDER;
        }
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs,
                              null, null, orderBy);
        // TODO: Since the URLs are a mess, always use content://sms
        ret.setNotificationUri(getContext().getContentResolver(),
                NOTIFICATION_URI);
        MmsLog.d(TAG, "query end");
        return ret;
    }
    /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
    /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
    /// on left and sent sms on right, display sender information for every sms. @{
    private Object[] convertIccToSms(SmsMessage message, 
            ArrayList<String> concatSmsIndexAndBody, int id, 
            int simId) {
        // N.B.: These calls must appear in the same order as the
        // columns appear in ICC_COLUMNS.
        Object[] row = new Object[14];
        row[0] = message.getServiceCenterAddress();
        
        // check message status and set address
        if ((message.getStatusOnIcc() == SmsManager.STATUS_ON_ICC_READ) ||
               (message.getStatusOnIcc() == SmsManager.STATUS_ON_ICC_UNREAD)) {
            row[1] = message.getDisplayOriginatingAddress();
        } else {
            row[1] = EncapsulatedSmsMessage.getDestinationAddress(message);
        }

        String concatSmsIndex = null;
        String concatSmsBody = null;
        if (null != concatSmsIndexAndBody) {
            concatSmsIndex = concatSmsIndexAndBody.get(0);
            concatSmsBody = concatSmsIndexAndBody.get(1);
        }
        
        row[2] = String.valueOf(message.getMessageClass());
        row[3] = concatSmsBody == null ? message.getDisplayMessageBody() : concatSmsBody;
        row[4] = message.getTimestampMillis();
        row[5] = message.getStatusOnIcc();
        if (mIsInternationalCardNotActivate) {
            if (concatSmsIndex == null) {
                try {
                    concatSmsIndex = String.valueOf(message.getIndexOnIcc() ^ (0x01 << 10));
                    row[6] = concatSmsIndex;
                } catch (NumberFormatException e) {
                    MmsLog.e(TAG, "concatSmsIndex bad number");
                }
            } else {
                row[6] = concatSmsIndex;
            }
        } else {
            row[6] = concatSmsIndex == null ? message.getIndexOnIcc() : concatSmsIndex;
        }
        MmsLog.d(TAG, "convertIccToSms; contactSmsIndex:" + row[6]);
        row[7] = message.isStatusReportMessage();
        row[8] = "sms";
        row[9] = TextBasedSmsColumns.MESSAGE_TYPE_ALL;
        row[10] = 0;      // locked
        row[11] = 0;      // error_code
        row[12] = id;
        row[13] = simId;
        return row;
    }
    /// @}
    /**
     * Return a Cursor containing just one message from the ICC.
     */
    private Cursor getSingleMessageFromIcc(String messageIndexString) {
        try {
            int messageIndex = Integer.parseInt(messageIndexString);
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<SmsMessage> messages;

            // use phone app permissions to avoid UID mismatch in AppOpsManager.noteOp() call
            long token = Binder.clearCallingIdentity();
            try {
                messages = smsManager.getAllMessagesFromIcc();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
            /// M: Code analyze 012, unknown, check if "messages" is valid. @{
            if (messages == null || messages.isEmpty()) {
                MmsLog.e(TAG, "getSingleMessageFromIcc messages is null");
                return null;
            }
            /// @}
            SmsMessage message = messages.get(messageIndex);
            if (message == null) {
                throw new IllegalArgumentException(
                        "Message not retrieved. ID: " + messageIndexString);
            }
            MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, 1);
            /// M: Code analyze 013, unknown, maybe support for gemini.
            cursor.addRow(convertIccToSms(message, 0, 0));
            return withIccNotificationUri(cursor);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        }
    }

    /**
     * Return a Cursor listing all the messages stored on the ICC.
     */
    /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
    /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
    /// on left and sent sms on right, display sender information for every sms. @{
    private Cursor getAllMessagesFromIcc(Uri url) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<SmsMessage> messages;

        // use phone app permissions to avoid UID mismatch in AppOpsManager.noteOp() call
        long token = Binder.clearCallingIdentity();
        try {
            messages = smsManager.getAllMessagesFromIcc();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        /// M: Code analyze 012, unknown, check if "messages" is valid. @{
        if (messages == null || messages.isEmpty()) {
            MmsLog.e(TAG, "getAllMessagesFromIcc messages is null");
            return null;
        }
        /// @}
        final int count = messages.size();
        MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, count);
        ArrayList<String> concatSmsIndexAndBody = null;
        /// M: Code analyze 009, new feature, support for gemini and plugin.
        /// use a flag "showInOne" indicate show long sms in one bubble or not.
        boolean showInOne = "1".equals(url.getQueryParameter("showInOne"));

        for (int i = 0; i < count; i++) {
            concatSmsIndexAndBody = null;
            SmsMessage message = messages.get(i);
            if (message != null && !message.isStatusReportMessage()) {
                if (showInOne) {
                    SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
                    if (null != smsHeader && null != smsHeader.concatRef) {
                        concatSmsIndexAndBody = getConcatSmsIndexAndBody(messages, i);
                    }
                }
               cursor.addRow(convertIccToSms(message, concatSmsIndexAndBody, i, -1));
            }
        }
        return withIccNotificationUri(cursor);
    }
    /// @}
    private Cursor withIccNotificationUri(Cursor cursor) {
        cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI);
        return cursor;
    }

    private void constructQueryForBox(SQLiteQueryBuilder qb, int type) {
        qb.setTables(TABLE_SMS);

        if (type != Sms.MESSAGE_TYPE_ALL) {
            qb.appendWhere("type=" + type);
        }
    }

    private void constructQueryForUndelivered(SQLiteQueryBuilder qb) {
        qb.setTables(TABLE_SMS);

        qb.appendWhere("(type=" + Sms.MESSAGE_TYPE_OUTBOX +
                       " OR type=" + Sms.MESSAGE_TYPE_FAILED +
                       " OR type=" + Sms.MESSAGE_TYPE_QUEUED + ")");
    }

    @Override
    public String getType(Uri url) {
        switch (url.getPathSegments().size()) {
        case 0:
            return VND_ANDROID_DIR_SMS;
            case 1:
                try {
                    Integer.parseInt(url.getPathSegments().get(0));
                    return VND_ANDROID_SMS;
                } catch (NumberFormatException ex) {
                    return VND_ANDROID_DIR_SMS;
                }
            case 2:
                // TODO: What about "threadID"?
                if (url.getPathSegments().get(0).equals("conversations")) {
                    return VND_ANDROID_SMSCHAT;
                } else {
                    return VND_ANDROID_SMS;
                }
        }
        return null;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        long token = Binder.clearCallingIdentity();
        try {
            return insertInner(url, initialValues);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private Uri insertInner(Uri url, ContentValues initialValues) {
        MmsLog.d(TAG, "insertInner begin, uri = " + url + ", values = " + initialValues);
        ContentValues values;
        long rowID;
        int type = Sms.MESSAGE_TYPE_ALL;
        /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
        /// remove useless operation and add transaction while import sms. @{
        /// M: for import sms only
        boolean importSms = false;
        /// @}
        int match = sURLMatcher.match(url);
        String table = TABLE_SMS;
        /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
        /// icon after restore messages. notify mms application about unread messages
        /// number after insert operation. @{
        /// M: do not notify the launcher to show unread message, if there are lots of operations
        notify = true;
        /// @}
        switch (match) {
            case SMS_ALL:
                Integer typeObj = initialValues.getAsInteger(Sms.TYPE);
                if (typeObj != null) {
                    type = typeObj.intValue();
                } else {
                    // default to inbox
                    type = Sms.MESSAGE_TYPE_INBOX;
                }
                break;

            case SMS_INBOX:
                type = Sms.MESSAGE_TYPE_INBOX;
                break;

            case SMS_FAILED:
                type = Sms.MESSAGE_TYPE_FAILED;
                break;

            case SMS_QUEUED:
                type = Sms.MESSAGE_TYPE_QUEUED;
                break;

            case SMS_SENT:
                type = Sms.MESSAGE_TYPE_SENT;
                break;

            case SMS_DRAFT:
                type = Sms.MESSAGE_TYPE_DRAFT;
                break;

            case SMS_OUTBOX:
                type = Sms.MESSAGE_TYPE_OUTBOX;
                break;

            case SMS_RAW_MESSAGE:
                table = "raw";
                break;

            case SMS_STATUS_PENDING:
                table = "sr_pending";
                break;

            case SMS_ATTACHMENT:
                table = "attachments";
                break;

            case SMS_NEW_THREAD_ID:
                table = "canonical_addresses";
                break;

            default:
                Log.e(TAG, "Invalid request: " + url);
                return null;
        }
        MmsLog.d(TAG, "insertInner match url end"); 
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (table.equals(TABLE_SMS)) {
                boolean addDate = false;
                boolean addType = false;

                // Make sure that the date and type are set
                if (initialValues == null) {
                    values = new ContentValues(1);
                    addDate = true;
                    addType = true;
                } else {
                    values = new ContentValues(initialValues);

                    if (!initialValues.containsKey(Sms.DATE)) {
                        addDate = true;
                    }

                    if (!initialValues.containsKey(Sms.TYPE)) {
                        addType = true;
                    }
                    /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
                    /// remove useless operation and add transaction while import sms. @{
                    if (initialValues.containsKey("import_sms")) {
                        importSms = true;
                        values.remove("import_sms");
                    }
                    /// @}
                }

                if (addDate) {
                    values.put(Sms.DATE, new Long(System.currentTimeMillis()));
                /// M: Code analyze 014, fix bug ALPS00114870, messages' time were abnormal after restored.
                /// set the date as the right value when import.
                } else {
                    Long date = values.getAsLong(Sms.DATE);
                    values.put(Sms.DATE, date);
                    MmsLog.d(TAG, "insert sms date "+ date);
                /// @}
                }

                if (addType && (type != Sms.MESSAGE_TYPE_ALL)) {
                    values.put(Sms.TYPE, Integer.valueOf(type));
                }

                // thread_id
                Long threadId = values.getAsLong(Sms.THREAD_ID);
                String address = values.getAsString(Sms.ADDRESS);

                if (((threadId == null) || (threadId == 0)) && (!TextUtils.isEmpty(address))) {
                    /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
                    /// remove useless operation and add transaction while import sms. @{
                    MmsLog.d(TAG, "insert sms getThreadId start");
                    long id = 0;
//                    if (importSms){
                        id = getThreadIdInternal(address, db);
//                    } else {
//                        id = EncapsulatedTelephony.Threads.getOrCreateThreadIdInternal(getContext(), address);
//                    }
                    values.put(Sms.THREAD_ID, id);
                    MmsLog.d(TAG, "insert getContentResolver getOrCreateThreadId end id = " + id);
                    /// @}
                }

                // If this message is going in as a draft, it should replace any
                // other draft messages in the thread.  Just delete all draft
                // messages with this thread ID.  We could add an OR REPLACE to
                // the insert below, but we'd have to query to find the old _id
                // to produce a conflict anyway.
                if (values.getAsInteger(Sms.TYPE) == Sms.MESSAGE_TYPE_DRAFT) {
                    db.delete(TABLE_SMS, "thread_id=? AND type=?",
                            new String[] { values.getAsString(Sms.THREAD_ID),
                                           Integer.toString(Sms.MESSAGE_TYPE_DRAFT) });
                }
                /// M: Code analyze 006, fix bug ALPS00252799, it cost long time to restore messages.
                /// support batch processing while restore messages. @{
                if (type != Sms.MESSAGE_TYPE_INBOX) {
                    values.put(Sms.READ, ONE);
                }
                if (!values.containsKey(Sms.PERSON)) {
                    values.put(Sms.PERSON, 0);
                }
                /// @}
            } else {
                if (initialValues == null) {
                    values = new ContentValues(1);
                } else {
                    values = initialValues;
                }
            }

            rowID = db.insert(table, "body", values);
            /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
            /// remove useless operation and add transaction while import sms. @{
            MmsLog.d(TAG, "insert table body end");
            // if (!importSms){
                /// M: Code analyze 015, fix bug ALPS00234074, do not delete the thread
                /// when it is in writiing status.
                setThreadStatus(db, values, 0);
            // }
            /// @}
            // Don't use a trigger for updating the words table because of a bug
            // in FTS3.  The bug is such that the call to get the last inserted
            // row is incorrect.
            if (table == TABLE_SMS) {
                // Update the words table with a corresponding row.  The words table
                // allows us to search for words quickly, without scanning the whole
                // table;
                MmsLog.d(TAG, "insert TABLE_WORDS begin");
                ContentValues cv = new ContentValues();
                cv.put(Telephony.MmsSms.WordsTable.ID, rowID);
                cv.put(Telephony.MmsSms.WordsTable.INDEXED_TEXT, values.getAsString("body"));
                cv.put(Telephony.MmsSms.WordsTable.SOURCE_ROW_ID, rowID);
                cv.put(Telephony.MmsSms.WordsTable.TABLE_ID, 1);
                db.insert(TABLE_WORDS, Telephony.MmsSms.WordsTable.INDEXED_TEXT, cv);
                MmsLog.d(TAG, "insert TABLE_WORDS end");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            MmsLog.d(TAG, "insert sms transacton end");
        }
        /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
        /// icon after restore messages. notify mms application about unread messages
        /// number after insert operation. @{
        if (rowID > 0) {
            Uri uri = Uri.parse("content://" + table + "/" + rowID);

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "insertInner " + uri + " succeeded");
            }
            //now notify the launcher to show unread message.
            notify = false;
            /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
            /// remove useless operation and add transaction while import sms. @{
            // if (!importSms){
                notifyChange2(uri);
            // }
            /// @}
            MmsLog.d(TAG, "insertInner succeed, uri = " + uri);
            return uri;
        } else {
            notify = false;
            Log.e(TAG,"insertInner: failed! " + values.toString());
        }
        /// @}
        return null;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        MmsLog.d(TAG, "delete begin, uri = " + url + ", selection = " + where);

        int count = 0;
        int match = sURLMatcher.match(url);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int simId = -1;
        switch (match) {
            case SMS_ALL:
                if (where != null && where.equals(FOR_MULTIDELETE)) {
                    MmsLog.d(TAG, "delete FOR_MULTIDELETE");
                    String selectids = getSmsIdsFromArgs(whereArgs);
                    String threadQuery = String.format("SELECT DISTINCT thread_id FROM sms WHERE _id IN %s", selectids);
                    Cursor cursor = db.rawQuery(threadQuery, null);
                    /// M: fix ALPS01263429, consider cursor as view, we should read cursor before delete related records.
                    long[] deletedThreads = null;
                    try {
                        deletedThreads = new long[cursor.getCount()];
                        int i = 0;
                        while(cursor.moveToNext()) {
                            deletedThreads[i++] = cursor.getLong(0);
                        }
                    } finally {
                        cursor.close();
                    }
                    String finalSelection = String.format(" _id IN %s", selectids);
                    count = deleteMessages(db, finalSelection, null);
                    if (count != 0) {
                        MmsSmsDatabaseHelper.updateMultiThreads(db, deletedThreads);
                    }
                    MmsLog.d(TAG, "delete FOR_MULTIDELETE count = "+ count);
                } else {
                    count = db.delete(TABLE_SMS, where, whereArgs);
                    if (count != 0) {
                        // Don't update threads unless something changed.
                        MmsSmsDatabaseHelper.updateAllThreads(db, where, whereArgs);
                    }
                }
                break;

            case SMS_ALL_ID:
                try {
                    int message_id = Integer.parseInt(url.getPathSegments().get(0));
                    count = MmsSmsDatabaseHelper.deleteOneSms(db, message_id);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Bad message id: " + url.getPathSegments().get(0));
                }
                break;

            case SMS_CONVERSATIONS_ID:
                int threadID;

                try {
                    threadID = Integer.parseInt(url.getPathSegments().get(1));
                } catch (Exception ex) {
                    throw new IllegalArgumentException(
                            "Bad conversation thread id: "
                            + url.getPathSegments().get(1));
                }

                // delete the messages from the sms table
                where = DatabaseUtils.concatenateWhere("thread_id=" + threadID, where);
                count = db.delete(TABLE_SMS, where, whereArgs);
                MmsSmsDatabaseHelper.updateThread(db, threadID);
                break;
            /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
            /// use new process of delete. @{
            case SMS_AUTO_DELETE:

                try {
                    threadID = Integer.parseInt(url.getPathSegments().get(1));
                } catch (Exception ex) {
                    throw new IllegalArgumentException(
                            "Bad conversation thread id: "
                            + url.getPathSegments().get(1));
                }

                where = DatabaseUtils.concatenateWhere("thread_id=" + threadID, where);
                /// M: delete the messages from the sms table
                if (whereArgs != null){
                    String selectids = getSmsIdsFromArgs(whereArgs);
                  //  Log.d(TAG, "selectids = "  + selectids);
                    db.execSQL("delete from words where table_to_use=1 and source_id in " + selectids);
                    MmsLog.d(TAG, "delete words end");
                    for (int i = 0; i < whereArgs.length; ){
                        if (i%100 == 0){
                            MmsLog.d(TAG, "delete sms1 beginTransaction i = " + i);
                          // db.beginTransaction();
                        }
                        where = "_id=" + whereArgs[i];
                        count += db.delete(TABLE_SMS, where, null);
                        i++;
//                        if (i%100 == 0 || i == whereArgs.length){
//                            MmsLog.d(TAG, "delete sms1 endTransaction i = " + i);
//                            db.endTransaction();
//                        }
                    }
                } else {
                    if (where != null) {
                        int id = 0;
                        String[] args = where.split("_id<");
                        if (args.length > 1) {
                            String finalid = args[1].replace(")", "");
                            MmsLog.d(TAG, "SMS_CONVERSATIONS_ID args[1] = " + args[1]);
                            id = Integer.parseInt(finalid);
                            MmsLog.d(TAG, "SMS_CONVERSATIONS_ID id = " + id);

                            for (int i = 1; i < id; i++) {
                                if (i % 30 == 0 || i == id - 1) {
                                    MmsLog.d(TAG, "delete sms2 beginTransaction i = " + i);
                                    where = "locked=0 AND type<>3 AND ipmsg_id<=0 AND _id>" + (i-30) + " AND _id<=" + i;
                                    where = DatabaseUtils.concatenateWhere("thread_id=" + threadID, where);
                                    count += db.delete(TABLE_SMS, where, null);
                                    MmsLog.d(TAG, "delete sms2 endTransaction i = " + i + " count=" + count);
                                }
                            }
                        }
                    }
                }
                MmsSmsDatabaseHelper.updateThread(db, threadID);
                break;
            /// @}
            case SMS_RAW_MESSAGE:
                count = db.delete("raw", where, whereArgs);
                break;

            case SMS_STATUS_PENDING:
                count = db.delete("sr_pending", where, whereArgs);
                break;
            /// M: Code analyze 010, new feature, support for gemini. @{
            // mark for reserve

            case SMS_ICC:
                String messageIndexString = url.getPathSegments().get(1);
                if(EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT == true) {
                    Log.i(TAG, "Delete Sim1 SMS id: " + messageIndexString);
                    return deleteMessageFromIcc(messageIndexString, PhoneConstants.GEMINI_SIM_1);
                } else {
                    return deleteMessageFromIcc(messageIndexString);
                }
            case SMS_ALL_ICC:
                simId = PhoneConstants.GEMINI_SIM_1;
                break;
            case SMS_ALL_ICC_GEMINI:
                simId = PhoneConstants.GEMINI_SIM_2;
                break;
            case SMS_ALL_ICC_THREE:
                simId = PhoneConstants.GEMINI_SIM_3;
                break;
            case SMS_ALL_ICC_FOUR:
                simId = PhoneConstants.GEMINI_SIM_4;
                break;
            default:
                throw new IllegalArgumentException("Unknown URL");
        }

        if (simId >=0) {
            if (where != null && where.equals(FOR_MULTIDELETE)) {
                MmsLog.d(TAG, "delete FOR_MULTIDELETE");
                String message_id = "";
                for (int i = 0; i < whereArgs.length; i++) {
                    if (whereArgs[i] != null) {
                        message_id = whereArgs[i];
                        if (simId == 0 && !EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                            count += deleteMessageFromIcc(message_id);
                        } else {
                            Log.i(TAG, "Delete Sim" + (simId + 1) + " SMS id: " + message_id);
                            count += deleteMessageFromIcc(message_id, simId);
                        }
                    }
                }
            } else {
                count = deleteMessageFromIcc("-1", simId);
            }
        }

        if (count > 0) {
            notifyChange(url);
        }
        MmsLog.d(TAG, "delete end, count = " + count);
        return count;
    }

    protected static String getSmsIdsFromArgs(String[] selectionArgs) {
        StringBuffer content = new StringBuffer("(");
        String res = "";
        if (selectionArgs == null || selectionArgs.length < 1){
            return "()";
        }
        for (int i = 0; i < selectionArgs.length - 1; i++){
            if (selectionArgs[i] == null){
                break;
            }
            content.append(selectionArgs[i]);
            content.append(",");
        }
        if (selectionArgs[selectionArgs.length-1] != null){
           content.append(selectionArgs[selectionArgs.length-1]);
        }
        res = content.toString();
        if (res.endsWith(",")) {
            res = res.substring(0, res.lastIndexOf(","));
        }
        res += ")";
        return res;
    }
    /**
     * Delete the message at index from ICC.  Return true iff
     * successful.
     */
    private int deleteMessageFromIcc(String messageIndexString) {
        SmsManager smsManager = SmsManager.getDefault();

        long token = Binder.clearCallingIdentity();
        try {
            return smsManager.deleteMessageFromIcc(Integer.parseInt(messageIndexString)) ? 1 : 0;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        } finally {
            /// M: no need to notify change
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        MmsLog.d(TAG, "update begin, uri = " + url + ", values = " + values + ", selection = " + where);
        int count = 0;
        String table = TABLE_SMS;
        String extraWhere = null;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (sURLMatcher.match(url)) {
            case SMS_RAW_MESSAGE:
                table = TABLE_RAW;
                break;

            case SMS_STATUS_PENDING:
                table = TABLE_SR_PENDING;
                break;

            case SMS_ALL:
            case SMS_FAILED:
            case SMS_QUEUED:
            case SMS_INBOX:
            case SMS_SENT:
            case SMS_DRAFT:
            case SMS_OUTBOX:
            case SMS_CONVERSATIONS:
                break;

            case SMS_ALL_ID:
                extraWhere = "_id=" + url.getPathSegments().get(0);
                break;

            case SMS_INBOX_ID:
            case SMS_FAILED_ID:
            case SMS_SENT_ID:
            case SMS_DRAFT_ID:
            case SMS_OUTBOX_ID:
                extraWhere = "_id=" + url.getPathSegments().get(1);
                break;

            case SMS_CONVERSATIONS_ID: {
                String threadId = url.getPathSegments().get(1);

                try {
                    Integer.parseInt(threadId);
                } catch (Exception ex) {
                    Log.e(TAG, "Bad conversation thread id: " + threadId);
                    break;
                }

                extraWhere = "thread_id=" + threadId;
                break;
            }

            case SMS_STATUS_ID:
                extraWhere = "_id=" + url.getPathSegments().get(1);
                break;

            default:
                throw new UnsupportedOperationException(
                        "URI " + url + " not supported");
        }

        where = DatabaseUtils.concatenateWhere(where, extraWhere);
        count = db.update(table, values, where, whereArgs);

        if (count > 0) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "update " + url + " succeeded");
            }
            notifyChange(url);
        }
        MmsLog.d(TAG, "update end, affectedRows = " + count);
        return count;
    }

    private void notifyChange(Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null);
        cr.notifyChange(MmsSms.CONTENT_URI, null);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null);
        /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
        /// icon after restore messages. notify mms application about unread messages
        /// number after insert operation.
        MmsLog.d(TAG, "notifyChange, notify unread change");
        MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
    }

    private SQLiteOpenHelper mOpenHelper;

    private final static String TAG = "Mms/Provider/Sms";
    private final static String VND_ANDROID_SMS = "vnd.android.cursor.item/sms";
    private final static String VND_ANDROID_SMSCHAT =
            "vnd.android.cursor.item/sms-chat";
    private final static String VND_ANDROID_DIR_SMS =
            "vnd.android.cursor.dir/sms";

    private static final HashMap<String, String> sConversationProjectionMap =
            new HashMap<String, String>();
    private static final String[] sIDProjection = new String[] { "_id" };

    private static final int SMS_ALL = 0;
    private static final int SMS_ALL_ID = 1;
    private static final int SMS_INBOX = 2;
    private static final int SMS_INBOX_ID = 3;
    private static final int SMS_SENT = 4;
    private static final int SMS_SENT_ID = 5;
    private static final int SMS_DRAFT = 6;
    private static final int SMS_DRAFT_ID = 7;
    private static final int SMS_OUTBOX = 8;
    private static final int SMS_OUTBOX_ID = 9;
    private static final int SMS_CONVERSATIONS = 10;
    private static final int SMS_CONVERSATIONS_ID = 11;
    private static final int SMS_RAW_MESSAGE = 15;
    private static final int SMS_ATTACHMENT = 16;
    private static final int SMS_ATTACHMENT_ID = 17;
    private static final int SMS_NEW_THREAD_ID = 18;
    private static final int SMS_QUERY_THREAD_ID = 19;
    private static final int SMS_STATUS_ID = 20;
    private static final int SMS_STATUS_PENDING = 21;
    private static final int SMS_ALL_ICC = 22;
    private static final int SMS_ICC = 23;
    private static final int SMS_FAILED = 24;
    private static final int SMS_FAILED_ID = 25;
    private static final int SMS_QUEUED = 26;
    private static final int SMS_UNDELIVERED = 27;
    /// M: Code analyze 010, new feature, support for gemini. @{
    private static final int SMS_ALL_ICC_GEMINI = 28;
    private static final int SMS_ALL_ICC_THREE = 29;
    private static final int SMS_ALL_ICC_FOUR = 30;
    /// @}
    /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
    /// use new process of delete. @{
    private static final int SMS_ALL_THREADID = 34;
    private static final int SMS_AUTO_DELETE  = 35;
    /// @}

    /// M: New Feature The international card.
    private static final int SMS_ALL_ICC_INTERNATIONAL = 36;
    private static final int SMS_ICC_INTERNATIONAL = 37;
    private static final int SMS_ALL_ICC_GEMINI_INTERNATIONAL = 38;
    private static final int SMS_ICC_GEMINI_INTERNATIONAL = 39;
    private static final UriMatcher sURLMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI("sms", null, SMS_ALL);
        sURLMatcher.addURI("sms", "#", SMS_ALL_ID);
        sURLMatcher.addURI("sms", "inbox", SMS_INBOX);
        sURLMatcher.addURI("sms", "inbox/#", SMS_INBOX_ID);
        sURLMatcher.addURI("sms", "sent", SMS_SENT);
        sURLMatcher.addURI("sms", "sent/#", SMS_SENT_ID);
        sURLMatcher.addURI("sms", "draft", SMS_DRAFT);
        sURLMatcher.addURI("sms", "draft/#", SMS_DRAFT_ID);
        sURLMatcher.addURI("sms", "outbox", SMS_OUTBOX);
        sURLMatcher.addURI("sms", "outbox/#", SMS_OUTBOX_ID);
        sURLMatcher.addURI("sms", "undelivered", SMS_UNDELIVERED);
        sURLMatcher.addURI("sms", "failed", SMS_FAILED);
        sURLMatcher.addURI("sms", "failed/#", SMS_FAILED_ID);
        sURLMatcher.addURI("sms", "queued", SMS_QUEUED);
        sURLMatcher.addURI("sms", "conversations", SMS_CONVERSATIONS);
        sURLMatcher.addURI("sms", "conversations/*", SMS_CONVERSATIONS_ID);
        sURLMatcher.addURI("sms", "raw", SMS_RAW_MESSAGE);
        sURLMatcher.addURI("sms", "attachments", SMS_ATTACHMENT);
        sURLMatcher.addURI("sms", "attachments/#", SMS_ATTACHMENT_ID);
        sURLMatcher.addURI("sms", "threadID", SMS_NEW_THREAD_ID);
        sURLMatcher.addURI("sms", "threadID/*", SMS_QUERY_THREAD_ID);
        sURLMatcher.addURI("sms", "status/#", SMS_STATUS_ID);
        sURLMatcher.addURI("sms", "sr_pending", SMS_STATUS_PENDING);
        sURLMatcher.addURI("sms", "icc", SMS_ALL_ICC);
        sURLMatcher.addURI("sms", "icc/#", SMS_ICC);
        //we keep these for not breaking old applications
        sURLMatcher.addURI("sms", "sim", SMS_ALL_ICC);
        sURLMatcher.addURI("sms", "sim/#", SMS_ICC);
        /// M: Code analyze 010, new feature, support for gemini. @{
        sURLMatcher.addURI("sms", "icc2", SMS_ALL_ICC_GEMINI);
        /// M: we keep these for not breaking old applications
        sURLMatcher.addURI("sms", "sim2", SMS_ALL_ICC_GEMINI);
        sURLMatcher.addURI("sms", "icc3", SMS_ALL_ICC_THREE);
        sURLMatcher.addURI("sms", "sim3", SMS_ALL_ICC_THREE);
        sURLMatcher.addURI("sms", "icc4", SMS_ALL_ICC_FOUR);
        sURLMatcher.addURI("sms", "sim4", SMS_ALL_ICC_FOUR);
        /// @}
        /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
        /// use new process of delete. @{
        sURLMatcher.addURI("sms", "all_threadid", SMS_ALL_THREADID);
        sURLMatcher.addURI("sms", "auto_delete/#", SMS_AUTO_DELETE);
        /// M: New Feature The international card.
        sURLMatcher.addURI("sms", "icc_international", SMS_ALL_ICC_INTERNATIONAL);
        sURLMatcher.addURI("sms", "icc_international/#", SMS_ICC_INTERNATIONAL);
        sURLMatcher.addURI("sms", "icc2_international", SMS_ALL_ICC_GEMINI_INTERNATIONAL);
        sURLMatcher.addURI("sms", "icc2_international/#", SMS_ICC_GEMINI_INTERNATIONAL);
        /// @}
        sConversationProjectionMap.put(Sms.Conversations.SNIPPET,
            "sms.body AS snippet");
        sConversationProjectionMap.put(Sms.Conversations.THREAD_ID,
            "sms.thread_id AS thread_id");
        sConversationProjectionMap.put(Sms.Conversations.MESSAGE_COUNT,
            "groups.msg_count AS msg_count");
        sConversationProjectionMap.put("delta", null);
    }
    /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
    /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
    /// on left and sent sms on right, display sender information for every sms. @{
    private Object[] convertIccToSms(SmsMessage message,int id, int simId) {
        return convertIccToSms(message, null,id, simId);
    }
    /// @}
    /// M: Code analyze 010, new feature, support for gemini.
    /// fix bug ALPS00042403, show the sender's number in manage SIM card. @{
    /**
     * Return a Cursor containing just one message from the ICC.
     */
    private Cursor getSingleMessageFromIcc(String messageIndexString, int slotId) {
        long token = Binder.clearCallingIdentity();
        try {
            int messageIndex = Integer.parseInt(messageIndexString);
            ArrayList<SmsMessage> messages = EncapsulatedGeminiSmsManager.getAllMessagesFromIccGemini(slotId);
            if (messages == null || messages.isEmpty()) {
                MmsLog.e(TAG, "getSingleMessageFromIcc messages is null");
                return null;
            }
            MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, 1);
            SmsMessage message = messages.get(messageIndex);
            if (message == null) {
                throw new IllegalArgumentException(
                        "Message not retrieved. ID: " + messageIndexString);
            }

            /// M: convert slotId to simId
            EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(getContext(), slotId);
            if (null == si) {
                MmsLog.e(TAG, "getSingleMessageFromIcc:SIMInfo is null for slot " + slotId);
                return null;
            }
            cursor.addRow(convertIccToSms(message, 0, (int)si.getSimId()));
            return withIccNotificationUri(cursor, slotId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
    /// @}
    /// M: Code analyze 010, new feature, support for gemini.
    /// fix bug ALPS00042403, show the sender's number in manage SIM card. @{
    private Cursor getAllMessagesFromIcc(Uri url, int slotId) {
        long token = Binder.clearCallingIdentity();
        try {
            MmsLog.d(TAG, "getAllMessagesFromIcc slotId =" + slotId);
            ArrayList<SmsMessage> messages = EncapsulatedGeminiSmsManager.getAllMessagesFromIccGemini(slotId);
            if (messages == null || messages.isEmpty()) {
                MmsLog.e(TAG, "getAllMessagesFromIcc messages is null");
                return null;
            }
            ArrayList<String> concatSmsIndexAndBody = null;

            /// M: convert slotId to simId
            EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(getContext(), slotId);
            if (null == si) {
                MmsLog.d(TAG, "getSingleMessageFromIcc:SIMInfo is null for slot " + slotId);
                return null;
            }
            int count = messages.size();
            MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, count);
            /// M: Code analyze 009, new feature, support for gemini and plugin.
            /// use a flag "showInOne" indicate show long sms in one bubble or not.
            boolean showInOne = "1".equals(url.getQueryParameter("showInOne"));

            for (int i = 0; i < count; i++) {
                concatSmsIndexAndBody = null;
                SmsMessage message = messages.get(i);
                if (message != null && !message.isStatusReportMessage()) {
                    if (showInOne) {
                        SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
                        if (null != smsHeader && null != smsHeader.concatRef) {
                            concatSmsIndexAndBody = getConcatSmsIndexAndBody(messages, i);
                        }
                    }
                    cursor.addRow(convertIccToSms(message, concatSmsIndexAndBody, i, (int)si.getSimId()));
                }
            }
            return withIccNotificationUri(cursor, slotId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
    /// M: New Feature The international card.
    private Cursor getAllMessagesFromIccInternational(Uri url, int slotId) {
        long token = Binder.clearCallingIdentity();
        try {
            MmsLog.d(TAG, "getAllMessagesFromIccInternational slotId =" + slotId);
            MatrixCursor mc = null;
            SmsManagerEx smsManagerEx = SmsManagerEx.getDefault();
            if (isDual()) {
                int activateMode = TelephonyManagerEx.getDefault().getPhoneType(PhoneConstants.GEMINI_SIM_1);
                ArrayList<SmsMessage> messagesActivate = null;
                ArrayList<SmsMessage> messagesNotActivate = null;
                if (activateMode == PhoneConstants.PHONE_TYPE_GSM) {
                    messagesActivate = smsManagerEx.getAllMessagesFromIcc(slotId, PhoneConstants.PHONE_TYPE_GSM);
                    messagesNotActivate = smsManagerEx.getAllMessagesFromIcc(slotId, PhoneConstants.PHONE_TYPE_CDMA);
                } else {
                    messagesActivate = smsManagerEx.getAllMessagesFromIcc(slotId, PhoneConstants.PHONE_TYPE_CDMA);
                    messagesNotActivate = smsManagerEx.getAllMessagesFromIcc(slotId, PhoneConstants.PHONE_TYPE_GSM);
                }
                mc = getAllMessagesForInternationalCardMatrix(slotId, url, messagesActivate, messagesNotActivate);
                mIsInternationalCardNotActivate = false;
                if (mc != null) {
                    return withIccNotificationUriInternational(mc, slotId);
                }
                return null;
            }
            return null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private Cursor withIccNotificationUriInternational(Cursor cursor, int slotId) {
        if(slotId == PhoneConstants.GEMINI_SIM_1) {
            cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI_INTERNATIONAL);
        }
        else {
            cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI_GEMINI_INTERNATIONAL);
        }
        return cursor;
    }

    private boolean isDual() {
        int cardType = 0;
        //ITelephony mTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        ITelephonyEx mTelephony = ITelephonyEx.Stub.asInterface(ServiceManager.checkService("phoneEx"));
        try {
            if (mTelephony != null) {
                cardType = mTelephony.getInternationalCardType(SystemProperties.getInt("ril.external.md", 0)-1);
                MmsLog.d(TAG, "cardType: " + cardType);
                if (cardType == InternationalRoamingConstants.CARD_TYPE_DUAL_MODE) {
                    return true;
                }
            }
            return false;
        } catch (android.os.RemoteException ex) {
            MmsLog.d(TAG, "get International cardType failed");
            return false;
        }
    }

    private MatrixCursor getAllMessagesForInternationalCardMatrix(int slotId, Uri url,
            ArrayList<SmsMessage> messagesActivate, ArrayList<SmsMessage> messagesNotActivate) {
        if (messagesActivate == null || messagesActivate.isEmpty()) {
            MmsLog.e(TAG, "getAllMessagesFromIccInternational messagesActivate is null");
            if (messagesNotActivate == null || messagesNotActivate.isEmpty()) {
                return null;
            } else {
                mIsInternationalCardNotActivate = true;
                return getAllMessagesForInternationalCard(slotId, url, messagesNotActivate);
            }
        } else {
            if (messagesNotActivate == null || messagesNotActivate.isEmpty()) {
                mIsInternationalCardNotActivate = false;
                return getAllMessagesForInternationalCard(slotId, url, messagesActivate);
            } else {
                ArrayList<String> concatSmsIndexAndBody = null;
                /// M: convert slotId to simId
                EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(getContext(), slotId);
                if (null == si) {
                    MmsLog.d(TAG, "getSingleMessageFromIcc:SIMInfo is null for slot " + slotId);
                    return null;
                }
                int countActivate = messagesActivate.size();
                int countNotActivate = messagesNotActivate.size();
                MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, countActivate + countNotActivate);
                /// M: Code analyze 009, new feature, support for gemini and plugin.
                /// use a flag "showInOne" indicate show long sms in one bubble or not.
                boolean showInOne = "1".equals(url.getQueryParameter("showInOne"));
                mIsInternationalCardNotActivate = true;
                for (int i = 0; i < countNotActivate; i++) {
                    concatSmsIndexAndBody = null;
                    SmsMessage message = messagesNotActivate.get(i);
                    if (message != null) {
                        if (showInOne) {
                            SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
                            if (null != smsHeader && null != smsHeader.concatRef) {
                                concatSmsIndexAndBody = getConcatSmsIndexAndBody(messagesNotActivate, i);
                            }
                        }
                        cursor.addRow(convertIccToSms(message, concatSmsIndexAndBody, i, (int) si.getSimId()));
                    }
                }
                mIsInternationalCardNotActivate = false;
                for (int i = 0; i < countActivate; i++) {
                    concatSmsIndexAndBody = null;
                    SmsMessage message = messagesActivate.get(i);
                    if (message != null) {
                        if (showInOne) {
                            SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
                            if (null != smsHeader && null != smsHeader.concatRef) {
                                concatSmsIndexAndBody = getConcatSmsIndexAndBody(messagesActivate, i);
                            }
                        }
                        cursor.addRow(convertIccToSms(message, concatSmsIndexAndBody, i + countNotActivate, (int) si.getSimId()));
                    }
                }
                return cursor;
            }
        }
    }

    private MatrixCursor getAllMessagesForInternationalCard(int slotId, Uri url, ArrayList<SmsMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            MmsLog.e(TAG, "getAllMessagesFromIccInternational messages is null");
            return null;
        }
        ArrayList<String> concatSmsIndexAndBody = null;
        /// M: convert slotId to simId
        EncapsulatedSimInfoManager si = EncapsulatedSimInfoManager.getSimInfoBySlot(getContext(), slotId);
        if (null == si) {
            MmsLog.d(TAG, "getSingleMessageFromIcc:SIMInfo is null for slot " + slotId);
            return null;
        }
        int count = messages.size();
        MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, count);
        /// M: Code analyze 009, new feature, support for gemini and plugin.
        /// use a flag "showInOne" indicate show long sms in one bubble or not.
        boolean showInOne = "1".equals(url.getQueryParameter("showInOne"));

        for (int i = 0; i < count; i++) {
            concatSmsIndexAndBody = null;
            SmsMessage message = messages.get(i);
            if (message != null) {
                if (showInOne) {
                    SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
                    if (null != smsHeader && null != smsHeader.concatRef) {
                        concatSmsIndexAndBody = getConcatSmsIndexAndBody(messages, i);
                    }
                }
                cursor.addRow(convertIccToSms(message, concatSmsIndexAndBody, i, (int)si.getSimId()));
            }
        }
        return cursor;
    }

    /// @}
    /// M: Code analyze 010, new feature, support for gemini.
    /// fix bug ALPS00042403, show the sender's number in manage SIM card. @{
    private Cursor withIccNotificationUri(Cursor cursor, int slotId) {
        if(slotId == PhoneConstants.GEMINI_SIM_1) {
            cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI);
        }
        else {
            cursor.setNotificationUri(getContext().getContentResolver(), Uri.parse("content://sms/icc" + slotId));
        }
        return cursor;
    }
    /// @}
    /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
    /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
    /// on left and sent sms on right, display sender information for every sms. @{
    private ArrayList<String> getConcatSmsIndexAndBody(ArrayList<SmsMessage> messages, int index) {
        int totalCount = messages.size();
        int refNumber = 0;
        int msgCount = 0;
        ArrayList<String> indexAndBody = new ArrayList<String>();
        StringBuilder smsIndex = new StringBuilder();
        StringBuilder smsBody = new StringBuilder();
        ArrayList<SmsMessage> concatMsg = null;
        SmsMessage message = messages.get(index);
        if (message != null) {
            SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
            if (null != smsHeader && null != smsHeader.concatRef) {
                msgCount = smsHeader.concatRef.msgCount;
                refNumber = smsHeader.concatRef.refNumber;
            }
        }

        concatMsg = new ArrayList<SmsMessage>();
        concatMsg.add(message);

        for (int i = index + 1; i < totalCount; i++) {
            SmsMessage sms = messages.get(i);
            if (sms != null) {
                SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(sms);
                if (null != smsHeader && null != smsHeader.concatRef && refNumber == smsHeader.concatRef.refNumber) {
                    concatMsg.add(sms);
                    messages.set(i, null);
                    if (msgCount == concatMsg.size()) {
                        break;
                    }
                }
            }
        }

        int concatCount = concatMsg.size();
        for (int k = 0; k < msgCount; k++) {
            for (int j = 0; j < concatCount; j++) {
                SmsMessage sms = concatMsg.get(j);
                SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(sms);
                if (k == smsHeader.concatRef.seqNumber -1) {
                    if (mIsInternationalCardNotActivate) {
                        try {
                            smsIndex.append((message.getIndexOnIcc() ^ (0x01 << 10)) + "");
                        } catch (NumberFormatException e) {
                            MmsLog.e(TAG, "concatSmsIndex bad number");
                        }
                    } else {
                        smsIndex.append(sms.getIndexOnIcc());
                    }
                    smsIndex.append(";");
                    smsBody.append(sms.getDisplayMessageBody());
                    break;
                }
            }
        }

        MmsLog.d(TAG, "concatenation sms index:" + smsIndex.toString());
        MmsLog.d(TAG, "concatenation sms body:" + smsBody.toString());
        indexAndBody.add(smsIndex.toString());
        indexAndBody.add(smsBody.toString());

        return indexAndBody;
    }
    /// @}
    /// M: Code analyze 010, new feature, support for gemini.
    /// fix bug ALPS00042403, show the sender's number in manage SIM card. @{
    /**
     * Delete the message at index from ICC.  Return true iff
     * successful.
     */
    private int deleteMessageFromIcc(String messageIndexString, int slotId) {
        long token = Binder.clearCallingIdentity();
        try {
            return EncapsulatedGeminiSmsManager.deleteMessageFromIccGemini(
                    Integer.parseInt(messageIndexString), slotId) ? 1 : 0;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        } finally {
            /// M: no need to notify change
            Binder.restoreCallingIdentity(token);
        }
    }
    /// @}
    /// M: Code analyze 006, fix bug ALPS00252799, it cost long time to restore messages.
    /// support batch processing while restore messages. @{
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        int ypCount = 0;
        int opCount = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        /// M: Fix ALPS00288517, not use transaction again to avoid ANR because of order of locking db
        db.beginTransaction();
        /// @}
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                if (++opCount > MAX_OPERATIONS_PER_PATCH) {
                    throw new OperationApplicationException(
                            "Too many content provider operations between yield points. "
                                    + "The maximum number of operations per yield point is "
                                    + MAX_OPERATIONS_PER_PATCH, ypCount);
                }
                final ContentProviderOperation operation = operations.get(i);
                results[i] = operation.apply(this, results, i);
            }
            /// M: Fix ALPS00288517, not use transaction again to avoid ANR because of order of locking db
            db.setTransactionSuccessful();
            /// @}
            return results;
        } finally {
            /// M: Fix ALPS00288517, not use transaction again to avoid ANR because of order of locking db
            db.endTransaction();
            /// @}
        }
    }
    /// @}
    /// M: Code analyze 015, fix bug ALPS00234074, do not delete the thread
    /// when it is in writiing status. @{
    private void setThreadStatus(SQLiteDatabase db, ContentValues values, int value) {
        ContentValues statusContentValues = new ContentValues(1);
        statusContentValues.put(EncapsulatedTelephony.Threads.STATUS, value);
        db.update("threads", statusContentValues, "_id=" + values.getAsLong(Sms.THREAD_ID), null);
    }
    /// @}
    /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
    /// use new process of delete. @{
    private Cursor getAllSmsThreadIds(String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        return db.query("sms",  new String[] {"distinct thread_id"},
                selection, selectionArgs, null, null, null);
    }
    /// @}
    /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
    /// remove useless operation and add transaction while import sms. @{
    private long getThreadIdInternal(String recipient, SQLiteDatabase db) {
        String THREAD_QUERY;
        if(EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT){
            THREAD_QUERY = "SELECT _id FROM threads " + "WHERE type<>"
                    + EncapsulatedTelephony.Threads.WAPPUSH_THREAD + " AND type<>"
                    + EncapsulatedTelephony.Threads.CELL_BROADCAST_THREAD + " AND recipient_ids=?";
        }else{
            THREAD_QUERY = "SELECT _id FROM threads " + "WHERE type<>"
                    + EncapsulatedTelephony.Threads.CELL_BROADCAST_THREAD + " AND recipient_ids=?";
        }
        long recipientId = getRecipientId(recipient, db);
        MmsLog.d(TAG, "sms insert, getThreadIdInternal, recipientId = " + recipientId);
        String[] selectionArgs = new String[] { String.valueOf(recipientId) };
        Cursor cursor = db.rawQuery(THREAD_QUERY, selectionArgs);
        try {
              if (cursor != null && cursor.getCount() == 0) {
                   Log.d(TAG, "getThreadId: create new thread_id for recipients " + recipient);
                   return insertThread(recipientId, db);
               } else if (cursor.getCount() == 1){
                      if (cursor.moveToFirst()) {
                       return cursor.getLong(0);
                   }
               } else {
                   Log.w(TAG, "getThreadId: why is cursorCount=" + cursor.getCount());
               }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return 0;
    }

    /**
     * Insert a record for a new thread.
     */
    private long insertThread(long recipientIds, SQLiteDatabase db) {
        ContentValues values = new ContentValues(4);

        long date = System.currentTimeMillis();
        values.put(ThreadsColumns.DATE, date - date % 1000);
        values.put(ThreadsColumns.RECIPIENT_IDS, recipientIds);
        values.put(ThreadsColumns.MESSAGE_COUNT, 0);
        return db.insert("threads", null, values);
    }
    private long getRecipientId(String address, SQLiteDatabase db) {
         if (!address.equals(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR)) {
             long id = getSingleAddressId(address, db);
             if (id != -1L) {
                 return id;
             } else {
                 Log.e(TAG, "getAddressIds: address ID not found for " + address);
             }
         }
         return 0;
    }
    /**
     * Return the canonical address ID for this address.
     */
    private long getSingleAddressId(String address, SQLiteDatabase db) {
        long retVal = -1L;
        HashMap<String, Long> addressesMap = new HashMap<String, Long>();
        HashMap<String, ArrayList<String>> addressKeyMap = new HashMap<String, ArrayList<String>>();
        String key = "";
        ArrayList<String> candidates = null;
        Cursor cursor = null;
        try {
            cursor = db.query(
                    "canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_2,
                    null, null, null, null, null);

            if (cursor != null ) {
                long id;
                String number = "";
                while (cursor.moveToNext()) {
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(CanonicalAddressesColumns._ID));
                    number = cursor.getString(cursor.getColumnIndexOrThrow(CanonicalAddressesColumns.ADDRESS));
                    CharBuffer keyBuffer = CharBuffer.allocate(MmsSmsProvider.STATIC_KEY_BUFFER_MAXIMUM_LENGTH);
                    key = MmsSmsProvider.key(number, keyBuffer);
                    candidates = addressKeyMap.get(key);
                    if (candidates == null) {
                        candidates = new ArrayList<String>();
                        addressKeyMap.put(key,candidates);
                    }
                    candidates.add(number);
                    addressesMap.put(number, id);
                }
            }

            boolean isEmail = Mms.isEmailAddress(address);
            boolean isPhoneNumber = Mms.isPhoneNumber(address);
            String refinedAddress = isEmail ? address.toLowerCase() : address;
            CharBuffer keyBuffer = CharBuffer.allocate(MmsSmsProvider.STATIC_KEY_BUFFER_MAXIMUM_LENGTH);
            key = MmsSmsProvider.key(refinedAddress, keyBuffer);
            candidates = addressKeyMap.get(key);
            String addressValue = "";
            if (candidates != null) {
                for (int i = 0; i < candidates.size(); i++) {
                    addressValue = candidates.get(i);
                    if (addressValue.equals(refinedAddress)) {
                        retVal = addressesMap.get(addressValue);
                        break;
                    }
                    if (isPhoneNumber && (refinedAddress != null && refinedAddress.length() <= NORMAL_NUMBER_MAX_LENGTH)
                            && (addressValue != null && addressValue.length() <= NORMAL_NUMBER_MAX_LENGTH)) {
                        boolean useStrictPhoneNumberComparation = getContext().getResources().getBoolean(
                                com.android.internal.R.bool.config_use_strict_phone_number_comparation);

                        if (PhoneNumberUtils.compare(refinedAddress, addressValue,
                                useStrictPhoneNumberComparation)) {
                            retVal = addressesMap.get(addressValue);
                            break;
                        }
                    }
                }
            }

            if (retVal == -1L) {
                retVal = insertCanonicalAddresses(db, refinedAddress);
                MmsLog.d(TAG, "getSingleAddressId: insert new canonical_address for " +
                        /*address*/ "xxxxxx" + ", addressess = " + refinedAddress.toString());
            } else {
                MmsLog.d(TAG, "getSingleAddressId: get exist id=" + retVal + ", refinedAddress="
                        + refinedAddress + ", currentNumber=" + addressValue);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return retVal;
    }

    private long insertCanonicalAddresses(SQLiteDatabase db, String refinedAddress) {
        MmsLog.d(TAG, "sms insert insertCanonicalAddresses for address = " + refinedAddress);
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(CanonicalAddressesColumns.ADDRESS, refinedAddress);
        return db.insert("canonical_addresses", CanonicalAddressesColumns.ADDRESS, contentValues);
    }
    /// @}
    /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
    /// icon after restore messages. notify mms application about unread messages
    /// number after insert operation. @{
    private void notifyChange2(Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null);
        cr.notifyChange(MmsSms.CONTENT_URI, null);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null);
        if (!notify){
            MmsLog.d(TAG, "notifyChange2, notify unread change");
            MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
        }
    }
    /// @}
    /// M: Code analyze 003, fix bug ALPS00239521, ALPS00244682, improve multi-delete speed
    /// in folder mode. @{
    /// M: get the select id from sms to delete words
    private String getWordIds(SQLiteDatabase db, String boxType, String selectionArgs){
        StringBuffer content = new StringBuffer("(");
        String res = "";
        String rawQuery = String.format("select _id from sms where _id NOT IN (select _id from sms where type IN %s AND _id NOT IN %s)", boxType, selectionArgs);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(rawQuery, null);
            if (cursor == null || cursor.getCount() == 0){
                return "()";
            }
            if (cursor.moveToFirst()) {
                do {
                    content.append(cursor.getInt(0));
                    content.append(",");
                } while (cursor.moveToNext());
                res = content.toString();
                if (!TextUtils.isEmpty(content) && res.endsWith(",")) {
                    res = res.substring(0, res.lastIndexOf(","));
                }
                res += ")";
            }
            MmsLog.d(TAG, "getWordIds cursor content = " + res + " COUNT " + cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;
    }
    /// @}

    /// M: because of triggers on sms and pdu, delete a large number of sms/pdu through an
    /// atomic operation will cost too much time. To avoid blocking other database operation,
    /// remove trigger sms_update_thread_on_delete, and set a limit to each delete operation. @{
    private static final int DELETE_LIMIT = 100;

    static int deleteMessages(SQLiteDatabase db,
            String selection, String[] selectionArgs) {
        MmsLog.d(TAG, "deleteMessages, start");
        int deleteCount = DELETE_LIMIT;
        if (TextUtils.isEmpty(selection)) {
            selection = "_id in (select _id from sms limit " + DELETE_LIMIT + ")";
        } else {
            selection = "_id in (select _id from sms where " + selection + " limit " + DELETE_LIMIT + ")";
        }
        int count = 0;
        while (deleteCount > 0) {
            deleteCount = db.delete(TABLE_SMS, selection, selectionArgs);
            count += deleteCount;
            MmsLog.d(TAG, "deleteMessages, delete " + deleteCount + " sms");
        }
        Log.d(TAG, "deleteMessages, delete sms end");
        return count;
    }
    /// @}
}
