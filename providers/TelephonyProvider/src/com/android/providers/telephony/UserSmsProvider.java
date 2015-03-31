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
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.TextBasedSmsColumns;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.provider.Telephony.ThreadsColumns;
import android.provider.Telephony.Threads;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import com.mediatek.encapsulation.MmsLog;

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
//import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;

public class UserSmsProvider extends ContentProvider {
    private static final Uri NOTIFICATION_URI = Uri.parse("content://usersms");
    private static final String TABLE_USERSMS = "usersms";
    private SQLiteOpenHelper mOpenHelper;

    private final static String TAG = "UserSmsProvider";

    private static final HashMap<String, String> sConversationProjectionMap =
            new HashMap<String, String>();
    private static final String[] sIDProjection = new String[] { "_id" };

    private static final int SMS_ALL = 0;
    
    private static final UriMatcher URI_MATCHER =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI("usersms", null, SMS_ALL);
    }
        
    @Override
    public boolean onCreate() {
        mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        MmsLog.d(TAG, "query begin uri = " + url);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Generate the body of the query.
        int match = URI_MATCHER.match(url);
        switch (match) {
            case SMS_ALL:
                qb.setTables(TABLE_USERSMS);
                break;
        }

        String orderBy = null;

        if (!TextUtils.isEmpty(sort)) {
            orderBy = sort;
        } else if (qb.getTables().equals(TABLE_USERSMS)) {
            orderBy = "_id ASC";
        }
        
        MmsLog.d(TAG, "query getReadbleDatabase");
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        MmsLog.d(TAG, "query getReadbleDatabase qb.query begin");
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs,
                              null, null, orderBy);
        MmsLog.d(TAG, "query getReadbleDatabase qb.query end");
        
        ret.setNotificationUri(getContext().getContentResolver(),
                NOTIFICATION_URI);
        return ret;
    }
    
    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        MmsLog.d(TAG, "insert begin");
        ContentValues values;
        int match = URI_MATCHER.match(url);
        String table = TABLE_USERSMS;
        long rowID;
        
        switch (match) {
            case SMS_ALL:
                break;
            default:
                break;
        }
        MmsLog.d(TAG, "insert match url end"); 
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        MmsLog.d(TAG, "insert mOpenHelper.getWritableDatabase end"); 
        
        if (initialValues == null) {
            return null;
        }
        
        values = new ContentValues(initialValues);
        rowID = db.insert(table, "usersms-pdus", values);
        MmsLog.d(TAG, "insert table body end");
        
        if (rowID > 0) {
            Uri uri = Uri.parse("content://" + table + "/" + rowID);

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "insert " + uri + " succeeded");
            }
            return uri;
        } else {
            Log.e(TAG,"insert: failed! " + values.toString());
        }
        
        MmsLog.d(TAG, "insert end");
        return null;
    }
    
    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        MmsLog.d(TAG, "update begin");
        int count = 0;
        String table = TABLE_USERSMS;
        String extraWhere = null;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (URI_MATCHER.match(url)) {
            case SMS_ALL:
                break;
            default:
                throw new UnsupportedOperationException(
                        "URI " + url + " not supported");
        }

        where = DatabaseUtils.concatenateWhere(where, extraWhere);
        count = db.update(table, values, where, whereArgs);

        if (count > 0) {
            Log.d(TAG, "update " + url + " succeeded");
        }
        MmsLog.d(TAG, "update end");
        return count;
    }
    
    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int deletedRows = 0;
        Uri deleteUri = null;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        deleteOnce(url, where, whereArgs);
        return deletedRows;
    }
    
    @Override
    public String getType(Uri url) {
        return null;
    }

    private int deleteOnce(Uri url, String where, String[] whereArgs) {
        int count = 0;
        int match = URI_MATCHER.match(url);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Log.d(TAG, "Delete deleteOnce: " + match);
        switch (match) {
            case SMS_ALL:
                count = db.delete(TABLE_USERSMS, where, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URL");
        }
        return count;
    }
}
