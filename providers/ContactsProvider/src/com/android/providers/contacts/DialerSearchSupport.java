package com.android.providers.contacts;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DialerSearch;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Preferences;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper.Clauses;
import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.GroupsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.MimetypesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.RawContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.SearchIndexColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.StatusUpdatesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.android.providers.contacts.util.LogUtils;
import com.mediatek.providers.contacts.ContactsFeatureConstants;
import com.mediatek.providers.contacts.ContactsFeatureConstants.FeatureOption;

import java.util.ArrayList;
import java.util.HashMap;

public class DialerSearchSupport {

    private static final String TAG = "DialerSearchSupport";
    private static final boolean DS_DBG = ContactsFeatureConstants.DBG_DIALER_SEARCH;
    private static final String DATA_READY_FLAG = "isDataReady";
    private boolean mIsDataInit = false;

    public interface DialerSearchLookupColumns {
        public static final String _ID = BaseColumns._ID;
        public static final String RAW_CONTACT_ID = "raw_contact_id";
        public static final String DATA_ID = "data_id";
        public static final String NORMALIZED_NAME = "normalized_name";
        public static final String NAME_TYPE = "name_type";
        public static final String CALL_LOG_ID = "call_log_id";
        public static final String NUMBER_COUNT = "number_count";
        public static final String SEARCH_DATA_OFFSETS = "search_data_offsets";
        public static final String NORMALIZED_NAME_ALTERNATIVE = "normalized_name_alternative";
        public static final String SEARCH_DATA_OFFSETS_ALTERNATIVE = "search_data_offsets_alternative";
        public static final String IS_VISIABLE = "is_visiable";
        public static final String SORT_KEY = "sort_key";
        public static final String TIMES_USED = "times_used";
    }

    public final static class DialerSearchLookupType {
        public static final int PHONE_EXACT = 8;
        public static final int NO_NAME_CALL_LOG = 8;
        public static final int NAME_EXACT = 11;
    }

    public interface DialerSearchQuery {
        String TABLE = Tables.DIALER_SEARCH;
        String[] COLUMNS = new String[] {
                DialerSearch.NAME_LOOKUP_ID,
                DialerSearch.CONTACT_ID,
                DialerSearchLookupColumns.DATA_ID,
                DialerSearch.CALL_DATE,
                DialerSearch.CALL_LOG_ID,
                DialerSearch.CALL_TYPE,
                DialerSearch.CALL_GEOCODED_LOCATION,
                DialerSearch.SIM_ID,
                DialerSearch.VTCALL,
                DialerSearch.INDICATE_PHONE_SIM,
                DialerSearch.CONTACT_STARRED,
                DialerSearch.PHOTO_ID,
                DialerSearch.SEARCH_PHONE_TYPE,
                DialerSearch.NAME,
                DialerSearch.SEARCH_PHONE_NUMBER,
                DialerSearch.CONTACT_NAME_LOOKUP,
                DialerSearch.IS_SDN_CONTACT, // add by MTK
                DialerSearch.MATCHED_DATA_OFFSETS,
                DialerSearch.MATCHED_NAME_OFFSETS
        };

        public static final int NAME_LOOKUP_ID_INDEX = 0;
        public static final int CONTACT_ID_INDEX = 1;
        public static final int CALL_LOG_DATE_INDEX = 2;
        public static final int CALL_LOG_ID_INDEX = 3;
        public static final int CALL_TYPE_INDEX = 4;
        public static final int CALL_GEOCODED_LOCATION_INDEX = 5;
        public static final int SIM_ID_INDEX = 6;
        public static final int VTCALL = 7;
        public static final int INDICATE_PHONE_SIM_INDEX = 8;
        public static final int CONTACT_STARRED_INDEX = 9;
        public static final int PHOTO_ID_INDEX = 10;
        public static final int SEARCH_PHONE_TYPE_INDEX = 11;
        public static final int NAME_INDEX = 12;
        public static final int SEARCH_PHONE_NUMBER_INDEX = 13;
        public static final int CONTACT_NAME_LOOKUP_INDEX = 14;
        public static final int IS_SDN_CONTACT = 15;
        public static final int DS_MATCHED_DATA_OFFSETS = 16;
        public static final int DS_MATCHED_NAME_OFFSETS = 17;
    }

    // result columns
    private static final String DS_INIT_SEARCH_RESULTS_COLUMNS = DialerSearch.NAME_LOOKUP_ID + ","
                         + DialerSearch.CONTACT_ID + ","
                         + DialerSearch.CALL_DATE + ","
                         + DialerSearch.CALL_LOG_ID + ","
                         + DialerSearch.CALL_TYPE + ","
                         + DialerSearch.CALL_GEOCODED_LOCATION + ","
                         + DialerSearch.SIM_ID + ","
                         + DialerSearch.VTCALL + ","
                         + DialerSearch.INDICATE_PHONE_SIM + ","
                         + DialerSearch.CONTACT_STARRED + ","
                         + DialerSearch.PHOTO_ID + ","
                         + DialerSearch.SEARCH_PHONE_TYPE + ","
                         + DialerSearch.NAME + ","
                         + DialerSearch.SEARCH_PHONE_NUMBER + ","
                         + DialerSearch.CONTACT_NAME_LOOKUP + ","
                         + DialerSearch.IS_SDN_CONTACT; // add by MTK

    // The initial TEMP_DIALER_SEARCH_VIEW without display name column and sort
    // key column
    private static final String DS_INIT_VIEW_COLUMNS = DialerSearch.NAME_LOOKUP_ID + ","
                        + DialerSearch.CONTACT_ID + ","
                        + DialerSearch.RAW_CONTACT_ID + ","
                        + DialerSearch.NAME_ID + ","
                        + DialerSearch.CALL_DATE + ","
                        + DialerSearch.CALL_LOG_ID + ","
                        + DialerSearch.CALL_TYPE + ","
                        + DialerSearch.CALL_GEOCODED_LOCATION + ","
                        + DialerSearch.SIM_ID + ","
                        + DialerSearch.VTCALL + ","
                        + DialerSearch.SEARCH_PHONE_NUMBER + ","
                        + DialerSearch.SEARCH_PHONE_TYPE + ","
                        + DialerSearch.CONTACT_NAME_LOOKUP + ","
                        + DialerSearch.PHOTO_ID + ","
                        + DialerSearch.CONTACT_STARRED + ","
                        + DialerSearch.INDICATE_PHONE_SIM + ","
                        + DialerSearch.IS_SDN_CONTACT; // add by MTK

    private static final String TEMP_DIALER_SEARCH_TABLE = "temp_dialer_search_table";
    private ArrayList<String> mFilterCache = new ArrayList<String>();
    private ArrayList<Object[][]> mResultCache = new ArrayList<Object[][]>();
    
    private ContactsProvider2 mContactsProvider;
    private boolean mUseStrictPhoneNumberComparation;
    private HashMap<Long, ContactData> mContactMap;
    private int mNumberCount = 0;
    private int mDisplayOrder = -1;
    private int mSortOrder = -1;
    private int mPrevSearchNumberLen = 0;

    public DialerSearchSupport(ContactsProvider2 provider) {
        mContactsProvider = provider;
    }

    public static String computeNormalizedNumber(String number) {
        String normalizedNumber = null;
        if (number != null) {
            normalizedNumber = PhoneNumberUtils.getStrippedReversed(number);
        }
        return normalizedNumber;
    }

    public static String stripSpecialCharInNumberForDialerSearch(String number) {
        if (number == null)
            return null;
        int len = number.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = number.charAt(i);
            if (PhoneNumberUtils.isNonSeparator(c)) {
                sb.append(c);
            } else if (c == ' ' || c == '-' || c == '(' || c == ')') {
                // strip blank and hyphen
            } else {
                continue;
            }
        }
        return sb.toString();
    }

    public static void createDialerSearchTable(SQLiteDatabase db) {
        if (FeatureOption.MTK_SEARCH_DB_SUPPORT) {
            db.execSQL("CREATE TABLE " + Tables.DIALER_SEARCH + " ("
                    + DialerSearchLookupColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + DialerSearchLookupColumns.DATA_ID
                        + " INTEGER REFERENCES data(_id) NOT NULL,"
                    + DialerSearchLookupColumns.RAW_CONTACT_ID
                        + " INTEGER REFERENCES raw_contacts(_id) NOT NULL,"
                    + DialerSearchLookupColumns.NAME_TYPE + " INTEGER NOT NULL,"
                    + DialerSearchLookupColumns.CALL_LOG_ID + " INTEGER DEFAULT 0,"
                    + DialerSearchLookupColumns.NUMBER_COUNT + " INTEGER NOT NULL DEFAULT 0, "
                    + DialerSearchLookupColumns.IS_VISIABLE + " INTEGER NOT NULL DEFAULT 1, "
                    + DialerSearchLookupColumns.NORMALIZED_NAME + " VARCHAR DEFAULT NULL,"
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + " VARCHAR DEFAULT NULL,"
                    + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE
                        + " VARCHAR DEFAULT NULL,"
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS_ALTERNATIVE
                        + " VARCHAR DEFAULT NULL " + ");");
            db.execSQL("CREATE INDEX dialer_data_id_index ON "
                    + Tables.DIALER_SEARCH + " ("
                    + DialerSearchLookupColumns.DATA_ID + ");");
            db.execSQL("CREATE INDEX dialer_search_raw_contact_id_index ON "
                    + Tables.DIALER_SEARCH + " ("
                    + DialerSearchLookupColumns.RAW_CONTACT_ID + ","
                    + DialerSearchLookupColumns.NAME_TYPE + ");");
            db.execSQL("CREATE INDEX dialer_search_call_log_id_index ON "
                    + Tables.DIALER_SEARCH + " ("
                    + DialerSearchLookupColumns.CALL_LOG_ID + ");");
        }
    }

    public static void createDialerSearchView(SQLiteDatabase db) {
        if (FeatureOption.MTK_SEARCH_DB_SUPPORT) {
            db.execSQL("DROP VIEW IF EXISTS " + Views.DIALER_SEARCH_VIEW + ";");
            String mDSNameTable = "dialer_search_name";
            String mDSNumberTable = "dialer_search_number";
            String mDSViewSelect = "SELECT "
                + mDSNumberTable + "." + DialerSearchLookupColumns._ID 
                + " AS " + DialerSearch.NAME_LOOKUP_ID + ","
                + Tables.CONTACTS + "." + Contacts._ID 
                + " AS " + DialerSearch.CONTACT_ID + ","
                + mDSNumberTable + "." + DialerSearchLookupColumns.RAW_CONTACT_ID 
                + " AS " + DialerSearch.RAW_CONTACT_ID + ","
                + Tables.RAW_CONTACTS + "." + RawContacts.DISPLAY_NAME_PRIMARY 
                + " AS " + DialerSearch.NAME + ","
                + Tables.RAW_CONTACTS + "." + RawContacts.DISPLAY_NAME_ALTERNATIVE 
                + " AS " + DialerSearch.NAME_ALTERNATIVE + ","
                + Tables.CALLS + "." + Calls.DATE 
                + " AS " + DialerSearch.CALL_DATE + ","
                + mDSNumberTable + "." + DialerSearchLookupColumns.CALL_LOG_ID 
                + " AS " + DialerSearch.CALL_LOG_ID + ","
                + Tables.CALLS + "." + Calls.TYPE + " AS " + DialerSearch.CALL_TYPE + ","
                + Tables.CALLS + "." + Calls.SIM_ID + " AS " + DialerSearch.SIM_ID + ","
                + Tables.CALLS + "." + Calls.VTCALL + " AS " + DialerSearch.VTCALL + ","
                + Tables.CALLS + "." + Calls.GEOCODED_LOCATION 
                + " AS " + DialerSearch.CALL_GEOCODED_LOCATION + ","
//                + " (CASE " 
//                    + " WHEN " + mDSNumberTable + "." + DialerSearchLookupColumns.CALL_LOG_ID + " > 0 "
//                        + " THEN " + Tables.CALLS + "." + Calls.NUMBER
//                    + " ELSE " + mDSNumberTable + "." + DialerSearchLookupColumns.NORMALIZED_NAME
//                + " END) AS " + DialerSearch.SEARCH_PHONE_NUMBER + ","
                + mDSNumberTable + "." + DialerSearchLookupColumns.NORMALIZED_NAME
                + " AS " + DialerSearch.SEARCH_PHONE_NUMBER + ","
                + Tables.DATA + "." + Data.DATA2 + " AS " + DialerSearch.SEARCH_PHONE_TYPE + ","
                + Tables.CONTACTS + "." + Contacts.LOOKUP_KEY 
                + " AS " + DialerSearch.CONTACT_NAME_LOOKUP + ","
                + Tables.CONTACTS + "." + Contacts.PHOTO_ID + " AS " + DialerSearch.PHOTO_ID + ","
                + Tables.CONTACTS + "." + Contacts.STARRED + " AS " + DialerSearch.CONTACT_STARRED + ","
                + Tables.CONTACTS + "." + Contacts.INDICATE_PHONE_SIM 
                + " AS " + DialerSearch.INDICATE_PHONE_SIM + ","
                + Tables.CONTACTS + "." + Contacts.IS_SDN_CONTACT 
                + " AS " + DialerSearch.IS_SDN_CONTACT + ","  // add by MTK
                + Tables.RAW_CONTACTS + "." + RawContacts.SORT_KEY_PRIMARY 
                + " AS " + DialerSearch.SORT_KEY_PRIMARY + ","
                + Tables.RAW_CONTACTS + "." + RawContacts.SORT_KEY_ALTERNATIVE 
                + " AS " + DialerSearch.SORT_KEY_ALTERNATIVE + ","
                + mDSNameTable + "." + DialerSearchLookupColumns._ID + " AS " + DialerSearch.NAME_ID 
                + " FROM (SELECT * FROM " + Tables.DIALER_SEARCH 
                        + " WHERE " + DialerSearchLookupColumns.NAME_TYPE 
                        + " = " + DialerSearchLookupType.PHONE_EXACT + ") AS " + mDSNumberTable
                + " LEFT JOIN " + Tables.RAW_CONTACTS 
                    + " ON " + Tables.RAW_CONTACTS + "." + RawContacts._ID 
                    + " = " + mDSNumberTable + "." + DialerSearchLookupColumns.RAW_CONTACT_ID
                + " LEFT JOIN " + Tables.CONTACTS 
                    + " ON " + Tables.CONTACTS + "." + Contacts._ID 
                    + " = " + Tables.RAW_CONTACTS + "." + RawContacts.CONTACT_ID
                + " LEFT JOIN " + Tables.CALLS 
                    + " ON " + Tables.CALLS + "." + Calls._ID 
                    + " = " + mDSNumberTable + "." + DialerSearchLookupColumns.CALL_LOG_ID
                + " LEFT JOIN " + Tables.DATA 
                    + " ON " + Tables.DATA + "." + Data._ID 
                    + " = " + mDSNumberTable + "." + DialerSearchLookupColumns.DATA_ID
                + " LEFT JOIN " + Tables.DIALER_SEARCH + " AS " + mDSNameTable 
                    + " ON " + mDSNameTable + "." + DialerSearchLookupColumns.RAW_CONTACT_ID 
                    + " = " + mDSNumberTable + "." + DialerSearchLookupColumns.RAW_CONTACT_ID 
                    + " AND " + mDSNameTable + "." + DialerSearchLookupColumns.NAME_TYPE 
                    + " = " + DialerSearchLookupType.NAME_EXACT
                + " WHERE " + Tables.CONTACTS + "." + Contacts._ID + " >0 OR " 
                    + mDSNumberTable + "." + DialerSearchLookupColumns.CALL_LOG_ID + ">0";
            db.execSQL("CREATE VIEW " + Views.DIALER_SEARCH_VIEW + " AS " + mDSViewSelect);
        }
    }

    public static void createContactsTriggersForDialerSearch(SQLiteDatabase db) {
        /*
         * For dialer search, update dialer search table and calls table when 
         * a raw contact is deleted.
         */

        // It used to SYNC dialer_search table and calls table when a contact was deleted.
        db.execSQL("DROP TRIGGER IF EXISTS " + Tables.AGGREGATION_EXCEPTIONS + "_splite_contacts");
        db.execSQL("CREATE TRIGGER " + Tables.AGGREGATION_EXCEPTIONS
                + "_splite_contacts AFTER INSERT ON " + Tables.AGGREGATION_EXCEPTIONS
                + " BEGIN "
                + "   UPDATE " + Tables.DIALER_SEARCH
                + "     SET " + DialerSearchLookupColumns.RAW_CONTACT_ID + "="
                                + "(SELECT " + DialerSearchLookupColumns.RAW_CONTACT_ID
                                + " FROM " + Tables.DATA +
                                " WHERE " + Tables.DATA + "." + Data._ID
                                + "=" + Tables.DIALER_SEARCH
                                    + "." + DialerSearchLookupColumns.DATA_ID + ")"
                + "   WHERE " + DialerSearchLookupColumns.RAW_CONTACT_ID
                                + " IN (" + "NEW." + AggregationExceptions.RAW_CONTACT_ID1
                                    + ",NEW." + AggregationExceptions.RAW_CONTACT_ID2 + ")"
                                + " AND " + DialerSearchLookupColumns.IS_VISIABLE + "=1"
                                + " AND " + DialerSearchLookupColumns.NAME_TYPE
                                    + "=" + DialerSearchLookupType.PHONE_EXACT
                                + " AND " + "NEW." + AggregationExceptions.TYPE + "=2;"
                + "   UPDATE " + Tables.DIALER_SEARCH
                + "     SET " + DialerSearchLookupColumns.IS_VISIABLE + "=1"
                + "   WHERE " + DialerSearchLookupColumns.RAW_CONTACT_ID
                                + " IN (" + "NEW." + AggregationExceptions.RAW_CONTACT_ID1
                                    + ",NEW." + AggregationExceptions.RAW_CONTACT_ID2 + ")"
                                + " AND " + DialerSearchLookupColumns.IS_VISIABLE + "=0"
                                + " AND " + DialerSearchLookupColumns.NAME_TYPE
                                    + "=" + DialerSearchLookupType.NAME_EXACT
                                + " AND " + "NEW." + AggregationExceptions.TYPE + "=2"
                                + ";"
                + " END");
    }

    public static void setNameForDialerSearch(SQLiteStatement dialerSearchNameUpdate,
            SQLiteDatabase db, long rawContactId, String displayNamePrimary,
            String displayNameAlternative) {
        if (dialerSearchNameUpdate == null) {
            dialerSearchNameUpdate = db.compileStatement("UPDATE "
                    + Tables.DIALER_SEARCH + " SET "
                    + DialerSearchLookupColumns.NORMALIZED_NAME + "=?,"
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + "=?,"
                    + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + "=?,"
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS_ALTERNATIVE + "=?"
                    + " WHERE " + DialerSearchLookupColumns.RAW_CONTACT_ID + "=? AND "
                    + DialerSearchLookupColumns.NAME_TYPE + "="
                    + DialerSearchLookupType.NAME_EXACT);
        }

        StringBuilder mSearchNameOffsets = new StringBuilder();
        String mSearchName = HanziToPinyin.getInstance()
                .getTokensForDialerSearch(displayNamePrimary, mSearchNameOffsets);
        StringBuilder mSearchNameOffsetsAlt = new StringBuilder();
        String mSearchNameAlt = HanziToPinyin.getInstance()
                .getTokensForDialerSearch(displayNameAlternative, mSearchNameOffsetsAlt);

        setBind(dialerSearchNameUpdate, mSearchName, 1);
        setBind(dialerSearchNameUpdate, mSearchNameOffsets.toString(), 2);
        setBind(dialerSearchNameUpdate, mSearchNameAlt, 3);
        setBind(dialerSearchNameUpdate, mSearchNameOffsetsAlt.toString(), 4);
        dialerSearchNameUpdate.bindLong(5, rawContactId);
        dialerSearchNameUpdate.execute();
    }

    private static void setBind(SQLiteStatement stmt, String value, int index) {
        if (TextUtils.isEmpty(value)) {
            stmt.bindNull(index);
            return;
        }
        stmt.bindString(index, value);
    }

    private String getDialerSearchViewColumns(int displayOrder, int sortOrder) {

        StringBuilder sb = new StringBuilder(DS_INIT_VIEW_COLUMNS);
        if (displayOrder == ContactsContract.Preferences.DISPLAY_ORDER_ALTERNATIVE) {
            sb.append("," + DialerSearch.NAME_ALTERNATIVE + " AS " + DialerSearch.NAME);
        } else {
            sb.append("," + DialerSearch.NAME);
        }
        if (sortOrder == ContactsContract.Preferences.SORT_ORDER_ALTERNATIVE) {
            sb.append("," + DialerSearch.SORT_KEY_ALTERNATIVE + " AS "
                    + DialerSearch.SORT_KEY_PRIMARY);
        } else {
            sb.append("," + DialerSearch.SORT_KEY_PRIMARY);
        }
        return sb.toString();
    }

    public int updateDialerSearchDataForMultiDelete(SQLiteDatabase db, String selection,
            String[] selectionArgs) {
        /**
         * Original Android code:
         * Cursor cursor = db.rawQuery("SELECT _id FROM raw_contacts WHERE " + selection, selectionArgs);
         * 
         * M: [ALPS00884503]After reboot phone, sim contact will remove then import again.
         * SEARCH_INDEX table not delete old data of sim contact, so the performance will degradation @{
         */
        Cursor cursor = db.rawQuery("SELECT _id, contact_id FROM raw_contacts WHERE " + selection, selectionArgs);
        ArrayList<Long> contactIdArray = new ArrayList<Long>();
        /**
         * [ALPS00884503] @}
         */
        ArrayList<Long> rawIdArray = new ArrayList<Long>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                rawIdArray.add(cursor.getLong(0));
                /**
                 * M: [ALPS00884503]After reboot phone, sim contact will remove then import again.
                 * SEARCH_INDEX table not delete old data of sim contact, so the performance will degradation @{
                 */
                contactIdArray.add(cursor.getLong(1));
                /**
                 * [ALPS00884503] @}
                 */
            }
            cursor.close();
        }
        int count = db.delete(Tables.RAW_CONTACTS, selection, selectionArgs);
        for (long rawContactId : rawIdArray) {
            log("[updateDialerSearchDataForMultiDelete]rawContactId:" + rawContactId);
            updateDialerSearchDataForDelete(db, rawContactId);
        }
        /**
         * M: [ALPS00884503] After reboot phone, sim contact will remove then import again.
         * SEARCH_INDEX table not delete old data of sim contact, so the performance will degradation @{
         */
        for (long contactId: contactIdArray) {
            String contactIdAsString = String.valueOf(contactId);
            db.execSQL("DELETE FROM " + Tables.SEARCH_INDEX + " WHERE " + SearchIndexColumns.CONTACT_ID + "=CAST(? AS int)",
                    new String[] { contactIdAsString });
        }
        /**
         * [ALPS00884503] @}
         */
        return count;
    }

    public void updateDialerSearchDataForDelete(SQLiteDatabase db, long rawContactId) {
        Cursor c = db.rawQuery(
                "SELECT _id,number,data_id FROM calls WHERE raw_contact_id = "
                        + rawContactId + " GROUP BY data_id;", null);
        if (c != null) {
            log("[updateDialerSearchDataForDelete]calls count:" + c.getCount());
            while (c.moveToNext()) {
                long callId = c.getLong(0);
                String number = c.getString(1);
                long dataId = c.getLong(2);
                log("[updateDialerSearchDataForDelete]callId:" + callId
                        + "|number:" + number + "|dataId:" + dataId);
                String UseStrict = mUseStrictPhoneNumberComparation ? "1" : "0";
                Cursor dataCursor = null;
                if (PhoneNumberUtils.isUriNumber(number)) {
                    dataCursor = db.rawQuery(
                                    "SELECT _id,raw_contact_id,contact_id FROM view_data "
                                            + " WHERE data1 =? AND mimetype_id=6 "
                                            + "AND raw_contact_id !=? LIMIT 1",
                                    new String[] {
                                            number, String.valueOf(rawContactId)
                                    });
                } else {
                    dataCursor = db.rawQuery(
                              "SELECT _id,raw_contact_id,contact_id FROM view_data "
                                      + " WHERE PHONE_NUMBERS_EQUAL(data1, '" + number + "' , "
                                      + UseStrict + " )"
                                      + " AND mimetype_id=5 AND raw_contact_id !=? LIMIT 1",
                               new String[] {
                                   String.valueOf(rawContactId)
                               });
                }
                if (dataCursor != null && dataCursor.moveToFirst()) {
                    long newDataId = dataCursor.getLong(0);
                    long newRawId = dataCursor.getLong(1);
                    log("[updateDialerSearchDataForDelete]newDataId:" + newDataId
                                + "|newRawId:" + newRawId);
                    db.execSQL("UPDATE calls SET data_id=?, raw_contact_id=? "
                                        + " WHERE data_id=?", new String[] {
                                        String.valueOf(newDataId),
                                        String.valueOf(newRawId),
                                        String.valueOf(dataId)
                                });
                    db.execSQL("UPDATE dialer_search SET call_log_id=? "
                                + " WHERE data_id=?", new String[] {
                                String.valueOf(callId),
                                String.valueOf(newDataId)
                    });
                } else {
                    log("[updateDialerSearchDataForDelete]update call log null.");
                    db.execSQL("UPDATE calls SET data_id=null, raw_contact_id=null "
                                    + "WHERE data_id=?", new String[] {
                                    String.valueOf(dataId)
                                });
                    db.execSQL("UPDATE dialer_search "
                            + "SET data_id=-call_log_id, "
                            + " raw_contact_id=-call_log_id, "
                            + " normalized_name=?, "
                            + " normalized_name_alternative=? "
                            + " WHERE data_id =?",
                            new String[] {
                                    number, number, String.valueOf(dataId)
                            });

                }
                if (dataCursor != null) {
                    dataCursor.close();
                }
            }
            c.close();
        }
        String delStr = "DELETE FROM dialer_search WHERE raw_contact_id=" + rawContactId;
        log("[updateDialerSearchDataForDelete]delStr:" + delStr);
        db.execSQL(delStr);
    }

    public static Cursor queryPhoneLookupByNumber(SQLiteDatabase db, ContactsDatabaseHelper dbHelper,
            String number, String[] projection, String selection, String[] selectionArgs,
            String groupBy, String having, String sortOrder, String limit) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String numberE164 = PhoneNumberUtils.formatNumberToE164(number,
                dbHelper.getCurrentCountryIso());
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
        dbHelper.buildPhoneLookupAndContactQuery(qb, normalizedNumber, numberE164);
        qb.setStrict(true);
        boolean foundResult = false;
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having,
                sortOrder, limit);
        try {
            if (c.getCount() > 0) {
                foundResult = true;
                return c;
            } else {
                qb = new SQLiteQueryBuilder();
                dbHelper.buildFallbackPhoneLookupAndContactQuery(qb, normalizedNumber);
                qb.setStrict(true);
            }
        } finally {
            if (!foundResult) {
                // We'll be returning a different cursor, so close this one.
                c.close();
            }
        }
        return qb.query(db, projection, selection, selectionArgs, groupBy, having,
                sortOrder, limit);
    }

    /// M: Remove abandoned dialer search entry @{
    /**
    public Cursor handleEmptyQuery(SQLiteDatabase db, Uri uri) {
        return queryDialerSearchInit(db, uri);
    }
    
    public Cursor handleSimpleQuery(SQLiteDatabase db, Uri uri) {
        return queryDialerSearchSimple(db, uri);
    }
    
    public Cursor handleIncrementQuery(SQLiteDatabase db, Uri uri) {
        return queryDialerSearchIncrement(db, uri);
    }
    */
    /// M: @}

    public Cursor handleDialerSearchQuery(SQLiteDatabase db, Uri uri) {
        return queryDialerSearch(db, uri);
    }

    public void handleDialerSearchQueryInit(SQLiteDatabase db, Uri uri) {
        queryDialerSearchInit(db, uri);
    }

    private void queryDialerSearchInit(SQLiteDatabase db, Uri uri) {
        LogUtils.d(TAG, "MTK-DialerSearch, queryDialerSearchInit, begin. uri: " + uri);

        String displayOrder = uri.getQueryParameter(Preferences.DISPLAY_ORDER);
        String sortOrder = uri.getQueryParameter(Preferences.SORT_ORDER);
        LogUtils.d(TAG, "MTK-DialerSearch, queryDialerSearchInit, displayOrder: " + displayOrder + " ,sortOrder: " + sortOrder);

        if (!TextUtils.isEmpty(displayOrder) && !TextUtils.isEmpty(sortOrder)) {
            mDisplayOrder = Integer.parseInt(displayOrder);
            mSortOrder = Integer.parseInt(sortOrder);
            db.execSQL("DROP TABLE IF EXISTS " + TEMP_DIALER_SEARCH_TABLE);
            db.execSQL("CREATE TEMP TABLE  " + TEMP_DIALER_SEARCH_TABLE
                    + " AS SELECT "
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns._ID 
                    + " AS " + DialerSearchLookupColumns._ID + " ,"
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.DATA_ID 
                    + " AS " + DialerSearchLookupColumns.DATA_ID + ","
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.RAW_CONTACT_ID
                    + " AS " + DialerSearchLookupColumns.RAW_CONTACT_ID + ","
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.NAME_TYPE 
                    + " AS " + DialerSearchLookupColumns.NAME_TYPE + ","
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.CALL_LOG_ID 
                    + " AS " + DialerSearchLookupColumns.CALL_LOG_ID + ","
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.NUMBER_COUNT 
                    + " AS " + DialerSearchLookupColumns.NUMBER_COUNT + ","
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.IS_VISIABLE 
                    + " AS " + DialerSearchLookupColumns.IS_VISIABLE + ","
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.NORMALIZED_NAME
                    + " AS " + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                    + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS
                    + " AS " + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + ","
                    + Tables.DIALER_SEARCH + "."
                    + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE 
                    + " AS " + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + ","
                    + Tables.DIALER_SEARCH + "."
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS_ALTERNATIVE 
                    + " AS " + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS_ALTERNATIVE + ","
                    + Tables.RAW_CONTACTS + "." + RawContacts.SORT_KEY_PRIMARY 
                    + " AS " + DialerSearchLookupColumns.SORT_KEY + ","
                    + Tables.RAW_CONTACTS + "." + RawContacts.TIMES_CONTACTED 
                    + " AS " + DialerSearchLookupColumns.TIMES_USED
                    + " FROM " + Tables.DIALER_SEARCH
                    + " LEFT JOIN " + Tables.RAW_CONTACTS
                      + " ON " + RawContactsColumns.CONCRETE_ID 
                      + "=" + Tables.DIALER_SEARCH + "." + DialerSearchLookupColumns.RAW_CONTACT_ID
                    + " WHERE " + DialerSearchLookupColumns.IS_VISIABLE + " = 1");

            String viewColumns = getDialerSearchViewColumns(mDisplayOrder, mSortOrder);
            mContactMap = new HashMap<Long, ContactData>();
            long rawId = 0;
            ContactData contactData;
            Cursor c = db.rawQuery(
                    "SELECT " + viewColumns
                            + " FROM " + Views.DIALER_SEARCH_VIEW + " ORDER BY "
                            + DialerSearch.RAW_CONTACT_ID, null);
            if (c != null) {
                try {
                    mNumberCount = c.getCount();
                    LogUtils.d(TAG, "MTK-DialerSearch, DialerSearch View Count: " + mNumberCount);

                    while (c.moveToNext()) {
                        long tmpRawId = c.getLong(2);
                        if (rawId != tmpRawId) {
                            rawId = tmpRawId;
                            contactData = new ContactData(rawId,
                                    c.getLong(c.getColumnIndex(DialerSearch.CONTACT_ID)),
                                    c.getString(c.getColumnIndex(DialerSearch.NAME)),
                                    c.getInt(c.getColumnIndex(DialerSearch.INDICATE_PHONE_SIM)),
                                    c.getLong(c.getColumnIndex(DialerSearch.PHOTO_ID)),
                                    c.getString(c.getColumnIndex(DialerSearch.CONTACT_NAME_LOOKUP)));
                            mContactMap.put(rawId, contactData);
                        }
                        ContactData refData = mContactMap.get(rawId);
                        if (refData == null) {
                            continue;
                        }
                        long id = c.getLong(0);
                        int label = c.getInt(c.getColumnIndex(DialerSearch.SEARCH_PHONE_TYPE));
                        long callLogId = c.getLong(c.getColumnIndex(DialerSearch.CALL_LOG_ID));
                        String callDate = c.getString(c.getColumnIndex(DialerSearch.CALL_DATE));
                        int callType = c.getInt(c.getColumnIndex(DialerSearch.CALL_TYPE));
                        String geo = c.getString(c
                                .getColumnIndex(DialerSearch.CALL_GEOCODED_LOCATION));
                        int callSimId = c.getInt(c.getColumnIndex(DialerSearch.SIM_ID));
                        int vtCall = c.getInt(c.getColumnIndex(DialerSearch.VTCALL));
                        String number = c.getString(c
                                .getColumnIndex(DialerSearch.SEARCH_PHONE_NUMBER));
                        refData.mNumberMap.add(new PhoneNumber(id, label, callLogId, callDate,
                                callType, geo, callSimId, vtCall, number));
                    }
                } finally {
                    c.close();
                }
            }
        }
        mFilterCache = new ArrayList<String>();
        mPrevSearchNumberLen = 0;
        mIsDataInit = true;
        LogUtils.d(TAG, "MTK-DialerSearch, queryDialerSearchInit, end.");
    }

    private Cursor queryDialerSearch(SQLiteDatabase db, Uri uri) {
        LogUtils.d(TAG, "MTK-DialerSearch, queryDialerSearch, begin. uri: " + uri);

        String isDataReadyParam = uri.getQueryParameter(DATA_READY_FLAG);
        boolean isDataReady = Boolean.parseBoolean(isDataReadyParam);
        String filterParam = uri.getLastPathSegment();
        LogUtils.d(TAG, "MTK-DialerSearch, queryDialerSearch, isDataReady: " + isDataReady + " ,filterParam: " + filterParam);

        if (!isDataReady || !mIsDataInit) {
            queryDialerSearchInit(db, uri);
        }
        Object[][] cursorValues = null;

        cursorValues = queryDialerSearchInternal(db, filterParam, null, null);
        Cursor c = buildCursor(cursorValues);
        LogUtils.d(TAG, "MTK-DialerSearch, queryDialerSearch, end. ResultCount: " + c.getCount());

        return c; 
    }

    /// M: Remove abandoned dialer search entry @{
    /**
    private Cursor queryDialerSearchSimple(SQLiteDatabase db, Uri uri) {
        log("DIALER_SEARCH_SIMPLE begin. uri:" + uri);
        String filterParam = uri.getLastPathSegment();
        if (TextUtils.isEmpty(filterParam)) {
            return null;
        }
        mFilterCache = new ArrayList<String>();
        mPrevSearchNumberLen = 0;

        Object[][] cursorValues = queryDialerSearchInternal(db, filterParam, null, null);
        
        log("DIALER_SEARCH_SIMPLE end.");
        return buildCursor(cursorValues);
    }

    private Cursor queryDialerSearchIncrement(SQLiteDatabase db, Uri uri) {
        log("DIALER_SEARCH_INCREMENT begin. uri:" + uri);
        String filterParam = uri.getLastPathSegment();

        Object[][] cursorValues = null;
        // Check Input OR Delete
        int numberCount = filterParam.length();
        if (mPrevSearchNumberLen > numberCount) {
            // current operation is delete number to search
            mPrevSearchNumberLen = numberCount;
            if (mFilterCache.size() > 0) {
                mFilterCache.remove(mFilterCache.size() - 1);
                mResultCache.remove(mResultCache.size() - 1);
                if (mResultCache.size() > 0) {
                    cursorValues = mResultCache.get(mResultCache.size() - 1);
                }
            }
        } else if (mPrevSearchNumberLen == numberCount) {
            // current operation is delete number to search
            if (mResultCache.size() > 0) {
                cursorValues = mResultCache.get(mResultCache.size() - 1);
            }
        } else {
            mPrevSearchNumberLen = numberCount;
            String selection = mFilterCache.size() == 0 ? null : 
                mFilterCache.get(mFilterCache.size() - 1);
            ResultCallBack result = new ResultCallBack();
            cursorValues = queryDialerSearchInternal(db, filterParam, selection, result);
            mFilterCache.add(result.mFilter);
            mResultCache.add(cursorValues);
        }
        Cursor c = buildCursor(cursorValues);
        log("DIALER_SEARCH_INCREMENT end");
        return c; 
    }
    */
    /// M: @}

    private Object[][] queryDialerSearchInternal(SQLiteDatabase db, String filterParam,
            String selection, ResultCallBack callBack) {
        LogUtils.d(TAG, "MTK-DialerSearch, queryDialerSearchInternal begin. filterParam:" + filterParam 
                   + "|selection:" + selection);

        Object[][] objectMap = null;
        StringBuilder selectedIds = new StringBuilder();
        int cursorPos = 0;
        db.beginTransaction();
        Cursor rawCursor = null;
        try {
            String mTableColumns = getDialerSearchNameTableColumns(mDisplayOrder, filterParam);
            rawCursor = db.rawQuery("SELECT "
                    + mTableColumns
                    + " FROM "
                    + TEMP_DIALER_SEARCH_TABLE
                    + " WHERE "
                    + (TextUtils.isEmpty(selection) ? "" : 
                        (DialerSearchLookupColumns._ID + " IN (" + selection + ") AND "))
                    + " DIALER_SEARCH_MATCH_FILTER("
                    + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + ","
                    + DialerSearchLookupColumns.NAME_TYPE + ",'"
                    + filterParam + "'" + ")"
                    + " ORDER BY " + DialerSearch.MATCHED_DATA_OFFSETS + " COLLATE MATCHTYPE DESC,"
                    + DialerSearchLookupColumns.TIMES_USED + " DESC,"
                    + DialerSearchLookupColumns.SORT_KEY + " COLLATE PHONEBOOK,"
                    + DialerSearchLookupColumns.CALL_LOG_ID + " DESC " + " limit 500", null);
            if (rawCursor == null) {
                LogUtils.e(TAG, "--- rawCursor is null ---");
                return null;
            }
            objectMap = new Object [mNumberCount][];
            ArrayList<Object[]> callLogPartitionList = new ArrayList<Object[]>(256);
            HashMap<Long, Integer> matchPosMap = new HashMap<Long, Integer>();
            LogUtils.d(TAG, "MTK-DialerSearch, Cursor from temp dialer table, Count: " + rawCursor.getCount());

            while (rawCursor.moveToNext()) {
                long searchId = rawCursor.getLong(0);
                selectedIds.append(searchId).append(",");
                int nameType = rawCursor.getInt(2);
                long DataId = rawCursor.getLong(3);
                String matchOffset = rawCursor.getString(4);
                if (TextUtils.isEmpty(matchOffset))
                    break;
                long rawId = rawCursor.getLong(1);
                ContactData contactData = mContactMap.get(rawId);
                // if contactData is null, it mean the contacts data has not been cached,
                // and so the result will not show this persion. However, we do not remove 
                // this result, it will show after the cache finish its update.
                if (contactData == null || contactData.mNumberMap == null) {
                    continue;
                }

                if (nameType == DialerSearchLookupType.NAME_EXACT) {
                    for (PhoneNumber number : contactData.mNumberMap) {
                        if (matchPosMap.containsKey(number.mId)) {
                            int pos = matchPosMap.get(number.mId);
                            objectMap[pos][DialerSearchQuery.DS_MATCHED_NAME_OFFSETS] = matchOffset;
                        } else {
                            matchPosMap.put(number.mId, cursorPos);
                            objectMap[cursorPos++] = buildCursorRecord(number.mId,
                                    contactData.mContactId, contactData.mDataId, null, 0, 0, null, 0, 0,
                                    contactData.mSimIndicate, 0, contactData.mPhotoId,
                                    number.mNumberLabel, contactData.mDisplayName, number.mNumber,
                                    contactData.mLookup, 0, null, matchOffset);
                        }
                    }
                } else if (nameType == DialerSearchLookupType.PHONE_EXACT) {

                    contactData.mDataId = DataId;
                    // two numbers
                    PhoneNumber number = null;
                    for (PhoneNumber n : contactData.mNumberMap) {
                        if (n.mId == searchId) {
                            number = n;
                            break;
                        }
                    }
                    if (number == null) {
                        continue;
                    }
                    if (rawId > 0) {
                        if (matchPosMap.containsKey(number.mId)) {
                            int pos = matchPosMap.get(number.mId);
                            objectMap[pos][DialerSearchQuery.DS_MATCHED_DATA_OFFSETS] = matchOffset;
                        } else {
                            matchPosMap.put(number.mId, cursorPos);
                            objectMap[cursorPos++] = buildCursorRecord(number.mId,
                                    contactData.mContactId, contactData.mDataId, null, 0, 0, null, 0, 0,
                                    contactData.mSimIndicate, 0, contactData.mPhotoId,
                                    number.mNumberLabel, contactData.mDisplayName, number.mNumber,
                                    contactData.mLookup, 0, matchOffset, null);
                        }
                    } else {
                        callLogPartitionList.add(buildCursorRecord(number.mId,
                                contactData.mContactId, contactData.mDataId, number.mCallDate, number.mCallLogId,
                                number.mCallType, number.mGeoLocation, number.mCallSimId,
                                number.mVtCall, -1, 0, 0, 0, null, number.mNumber, null, 0,
                                matchOffset, null));
                    }
                }
            }
            if (selectedIds.length() > 0) {
                selectedIds.deleteCharAt(selectedIds.length() - 1);
            }
            rawCursor.close();
            if (callLogPartitionList != null && callLogPartitionList.size() > 0) {
                for(Object[] item:callLogPartitionList) {
                    objectMap[cursorPos++] = item;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            if (rawCursor != null) {
                rawCursor.close();
                rawCursor = null;
            }
            db.endTransaction();
        }
        if (callBack != null) {
            callBack.mCursorCount = cursorPos + 1;
            callBack.mFilter = selectedIds.toString();
        }
        LogUtils.d(TAG, "MTK-DialerSearch, queryDialerSearchInternal end. objectCount: " + cursorPos);

        return objectMap;
    }

    private String getDialerSearchNameTableColumns(int displayOrder, String filterParam) {
        String columns = DialerSearchLookupColumns._ID + " ,"
                + DialerSearchLookupColumns.RAW_CONTACT_ID + ","
                + DialerSearchLookupColumns.NAME_TYPE + ","
                + DialerSearchLookupColumns.DATA_ID;
        String searchParamList = "";
        if (displayOrder == ContactsContract.Preferences.DISPLAY_ORDER_ALTERNATIVE) {
            searchParamList = DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE
                    + "," + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS_ALTERNATIVE
                    + "," + DialerSearchLookupColumns.NAME_TYPE
                    + ",'" + filterParam + "'";
        } else {
            searchParamList = DialerSearchLookupColumns.NORMALIZED_NAME + ","
                                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + ","
                                    + DialerSearchLookupColumns.NAME_TYPE + ",'"
                                    + filterParam + "'";
        }
        return columns + ","
                + "DIALER_SEARCH_MATCH(" + searchParamList + ") AS "
                + DialerSearch.MATCHED_DATA_OFFSETS;
    }

    private Cursor buildCursor(Object[][] cursorValues) {
        MatrixCursor c = new MatrixCursor(DialerSearchQuery.COLUMNS);
        if (cursorValues != null) {
            for (Object[] record : cursorValues) {
                if (record == null) {
                    break;
                }
                c.addRow(record);
            }
        }
        return c;
    }

    private Object[] buildCursorRecord(long id, long contactId, long dataId, String callData, long callLogId,
            int callType, String geo, int callSimId, int isVtCall, int simIndicator, int starred,
            long photoId, int phoneType, String name, String number, String lookup, int isSdn,
            String phoneOffset, String nameOffset) {
        Object[] record = new Object[] {
                id, contactId, dataId, callData, callLogId,
                callType, geo, callSimId, isVtCall, simIndicator, starred,
                photoId, phoneType, name, number, lookup, isSdn,
                phoneOffset, nameOffset
        };
        return record;
    }

    private void log(String msg) {
        if (DS_DBG) {
            Log.d(TAG, msg);
        }
    }

    public static class PhoneNumber {
        long mId;
        int mNumberLabel;
        long mCallLogId;
        String mCallDate;
        int mCallType;
        String mGeoLocation;
        int mCallSimId;
        int mVtCall;
        String mNumber;

        PhoneNumber(long phoneId, int numberLabel, long callLogId, String callDate, int callType,
                String geo, int callSimId, int vtCall, String phoneNumber) {
            mId = phoneId;
            mNumberLabel = numberLabel;
            mCallLogId = callLogId;
            mCallDate = callDate;
            mCallType = callType;
            mGeoLocation = geo;
            mCallSimId = callSimId;
            mVtCall = vtCall;
            mNumber = phoneNumber;
        }

        @Override
        public String toString() {
            return mId + "," + mNumber;
        }
    }

    public static class ContactData {
        long mRawId;
        long mContactId;
        long mDataId;
        String mDisplayName;
        int mSimIndicate;
        long mPhotoId;
        String mLookup;
        ArrayList<PhoneNumber> mNumberMap;

        ContactData(long rId, long rContactId, String rName, int rIndicate, long rPhotoId,
                String rLookup) {
            mRawId = rId;
            mContactId = rContactId;
            mDataId = -1;
            mDisplayName = rName;
            mSimIndicate = rIndicate;
            mPhotoId = rPhotoId;
            mLookup = rLookup;
            mNumberMap = new ArrayList<PhoneNumber>();
        }

        @Override
        public String toString() {
            return mRawId + "," + mDataId + "," + mContactId + "," + mDisplayName + "||" + mNumberMap.toString();
        }
    }

    class ResultCallBack {
        int mCursorCount;
        String mFilter;
    }
}
