package com.mediatek.encapsulation.android.telephony;
import android.provider.BaseColumns;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.content.Intent;
import android.content.ContentUris;
import android.database.DatabaseUtils;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.util.Log;
import android.provider.Telephony;
import android.database.sqlite.SqliteWrapper;
import android.telephony.SmsCbMessage;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
    

	/** M: MTK Add */
public class EncapsulatedSimInfoManager implements BaseColumns{
        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.CONTENT_URI : Uri.parse("content://telephony/siminfo");

        public static final String DEFAULT_SORT_ORDER = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DEFAULT_SORT_ORDER :"name ASC";

        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ICC_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.ICC_ID :"icc_id";
        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String DISPLAY_NAME = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPLAY_NAME :"display_name";

        public static final int DEFAULT_NAME_MIN_INDEX = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DEFAULT_NAME_MIN_INDEX :01;

        public static final int DEFAULT_NAME_MAX_INDEX = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DEFAULT_NAME_MAX_INDEX :99;

        public static final int DEFAULT_NAME_RES = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DEFAULT_NAME_RES :com.mediatek.internal.R.string.new_sim;

        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String NUMBER = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.NUMBER :"number";

        /**
         * 0:none, 1:the first four digits, 2:the last four digits.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DISPLAY_NUMBER_FORMAT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPLAY_NUMBER_FORMAT :"display_number_format";

        public static final int DISPALY_NUMBER_NONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPALY_NUMBER_NONE :0;

        public static final int DISPLAY_NUMBER_FIRST = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPLAY_NUMBER_FIRST :1;

        public static final int DISPLAY_NUMBER_LAST = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPLAY_NUMBER_LAST :2;

        public static final int DISLPAY_NUMBER_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISLPAY_NUMBER_DEFAULT :DISPLAY_NUMBER_FIRST;

        /**
         * Eight kinds of colors. 0-3 will represent the eight colors. Default
         * value: any color that is not in-use.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String COLOR = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR :"color";

        public static final int COLOR_1 = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_1 :0;

        public static final int COLOR_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_2 :1;

        public static final int COLOR_3 = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_3 :2;

        public static final int COLOR_4 = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_4 :3;

        public static final int COLOR_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_DEFAULT :COLOR_1;

        /**
         * 0: Don't allow data when roaming, 1:Allow data when roaming
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DATA_ROAMING = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DATA_ROAMING :"data_roaming";

        public static final int DATA_ROAMING_ENABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DATA_ROAMING_ENABLE :1;

        public static final int DATA_ROAMING_DISABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DATA_ROAMING_DISABLE :0;

        public static final int DATA_ROAMING_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DATA_ROAMING_DEFAULT :DATA_ROAMING_DISABLE;

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SLOT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.SLOT :"slot";

        public static final int SLOT_NONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.SLOT_NONE :-1;

        public static final int ERROR_GENERAL = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.ERROR_GENERAL :-1;

        public static final int ERROR_NAME_EXIST = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.ERROR_NAME_EXIST :-2;
            /** M: MTK Add */
        public static final int[] SIMBackgroundRes = new int[] {
                com.mediatek.internal.R.drawable.sim_background_blue,
                com.mediatek.internal.R.drawable.sim_background_orange,
                com.mediatek.internal.R.drawable.sim_background_green,
                com.mediatek.internal.R.drawable.sim_background_purple
        };
        public long mSimInfoId;
        public String mIccId;

        private String mDisplayName = "";

        private String mNumber = "";

        private int mDispalyNumberFormat = DISLPAY_NUMBER_DEFAULT;

        private int mColor;

        public int mSimSlotId = SLOT_NONE;
        private int mDataRoaming = DATA_ROAMING_DEFAULT;

        private int mSlot = SLOT_NONE;

        private int mSimBackgroundRes = SIMBackgroundRes[COLOR_DEFAULT];

        private SimInfoManager.SimInfoRecord mSIMInfoRecord;

        private EncapsulatedSimInfoManager() {
        }

        public EncapsulatedSimInfoManager(SimInfoManager.SimInfoRecord simInfoRecord) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                if (simInfoRecord != null) {
                    mSIMInfoRecord = simInfoRecord;
                }
            } else {
            }
        }

        public static class ErrorCode {
            public static final int ERROR_GENERAL = EncapsulationConstant.USE_MTK_PLATFORM ?
                    SimInfoManager.ErrorCode.ERROR_GENERAL: -1;

            public static final int ERROR_NAME_EXIST = EncapsulationConstant.USE_MTK_PLATFORM ?
                    SimInfoManager.ErrorCode.ERROR_NAME_EXIST: -2;
        }

        public long getSimId() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mSimInfoId ;
            } else {
                return 0;
            }
        }

        public String getICCId() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mIccId;
            } else {
                return new String();
            }
        }

        public String getDisplayName() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mDisplayName;
            } else {
                return new String();
            }
        }

        public String getNumber() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mNumber;
            } else {
                return new String();
            }
        }

        public int getDispalyNumberFormat() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mDispalyNumberFormat;
            } else {
                return DISLPAY_NUMBER_DEFAULT;
            }
        }

        public int getColor() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mColor;
            } else {
                return 0;
            }
        }

        public int getDataRoaming() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mDataRoaming;
            } else {
                return DATA_ROAMING_DEFAULT;
            }
        }

        public int getSlot() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mSimSlotId;
            } else {
                return SLOT_NONE;
            }
        }

        public int getSimBackgroundRes() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mSimBackgroundRes;
            } else {
                return SIMBackgroundRes[COLOR_DEFAULT];
            }
        }

        private static EncapsulatedSimInfoManager fromCursor(Cursor cursor) {
            EncapsulatedSimInfoManager info = new EncapsulatedSimInfoManager();
            info.mSimInfoId  = cursor.getLong(cursor.getColumnIndexOrThrow(SimInfoManager._ID));
            info.mIccId = cursor.getString(cursor.getColumnIndexOrThrow(SimInfoManager.ICC_ID));
            info.mDisplayName = cursor
                    .getString(cursor.getColumnIndexOrThrow(SimInfoManager.DISPLAY_NAME));
            info.mNumber = cursor.getString(cursor.getColumnIndexOrThrow(SimInfoManager.NUMBER));
            info.mDispalyNumberFormat = cursor.getInt(cursor
                    .getColumnIndexOrThrow(SimInfoManager.DISPLAY_NUMBER_FORMAT));
            info.mColor = cursor.getInt(cursor.getColumnIndexOrThrow(SimInfoManager.COLOR));
            info.mDataRoaming = cursor.getInt(cursor.getColumnIndexOrThrow(SimInfoManager.DATA_ROAMING));
            info.mSimSlotId = cursor.getInt(cursor.getColumnIndexOrThrow(SimInfoManager.SLOT));
            int size = SIMBackgroundRes.length;
            if (info.mColor >= 0 && info.mColor < size) {
                info.mSimBackgroundRes = SIMBackgroundRes[info.mColor];
            }
            return info;
        }

        /**
         * @param ctx
         * @return the array list of Current SIM Info
         */
        public static List<EncapsulatedSimInfoManager> getInsertedSimInfoList(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                List<SimInfoManager.SimInfoRecord> oldSimList = SimInfoManager.getInsertedSimInfoList(ctx);
                ArrayList<EncapsulatedSimInfoManager> newSimList = new ArrayList<EncapsulatedSimInfoManager>();
                for (int i = 0; i < oldSimList.size(); i++) {
                    EncapsulatedSimInfoManager mSimInfo = new EncapsulatedSimInfoManager(oldSimList.get(i));
                    newSimList.add(mSimInfo);
                }
                return newSimList;
            } else {
                ArrayList<EncapsulatedSimInfoManager> simList = new ArrayList<EncapsulatedSimInfoManager>();
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, null,
                        SimInfoManager.SLOT + "!=" + SimInfoManager.SLOT_NONE, null, null);
                try {
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            simList.add(EncapsulatedSimInfoManager.fromCursor(cursor));
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return simList;
            }
        }

        /**
         * @param ctx
         * @return array list of all the SIM Info include what were used before
         */
        public static List<EncapsulatedSimInfoManager> getAllSIMList(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                List<SimInfoManager.SimInfoRecord> oldSimList = SimInfoManager.getAllSimInfoList(ctx);
                ArrayList<EncapsulatedSimInfoManager> newSimList = new ArrayList<EncapsulatedSimInfoManager>();
                for (int i = 0; i < oldSimList.size(); i++) {
                    EncapsulatedSimInfoManager mSimInfo = new EncapsulatedSimInfoManager(oldSimList.get(i));
                    newSimList.add(mSimInfo);
                }
                return newSimList;
            } else {
                ArrayList<EncapsulatedSimInfoManager> simList = new ArrayList<EncapsulatedSimInfoManager>();
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, null, null,
                        null, null);
                try {
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            simList.add(EncapsulatedSimInfoManager.fromCursor(cursor));
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return simList;
            }
        }

        /**
         * @param ctx
         * @param SIMId the unique SIM id
         * @return SIM-Info, maybe null
         */
        public static EncapsulatedSimInfoManager getSIMInfoById(Context ctx, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                SimInfoManager.SimInfoRecord siminfo = SimInfoManager.getSimInfoById(ctx, SIMId);
                if (siminfo == null) {
                    return null;
                } else {
                    EncapsulatedSimInfoManager mSimInfo = new EncapsulatedSimInfoManager(siminfo);
                    return mSimInfo;
                }
            } else {
                if (SIMId <= 0)
                    return null;
                Cursor cursor = ctx.getContentResolver().query(
                        ContentUris.withAppendedId(SimInfoManager.CONTENT_URI, SIMId), null, null, null, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return EncapsulatedSimInfoManager.fromCursor(cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }
        }

        /**
         * @param ctx
         * @param SIMName the Name of the SIM Card
         * @return SIM-Info, maybe null
         */
        public static EncapsulatedSimInfoManager getSIMInfoByName(Context ctx, String SIMName) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                SimInfoManager.SimInfoRecord siminfo = SimInfoManager.getSimInfoByName(ctx, SIMName);
                if (siminfo == null) {
                    return null;
                } else {
                    EncapsulatedSimInfoManager mSimInfo = new EncapsulatedSimInfoManager(siminfo);
                    return mSimInfo;
                }
            } else {
                if (SIMName == null)
                    return null;
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, null,
                        SimInfoManager.DISPLAY_NAME + "=?", new String[] {
                            SIMName
                        }, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return EncapsulatedSimInfoManager.fromCursor(cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }
        }

        /**
         * @param ctx
         * @param cardSlot
         * @return The SIM-Info, maybe null
         */
        public static EncapsulatedSimInfoManager getSimInfoBySlot(Context ctx, int cardSlot) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                SimInfoManager.SimInfoRecord siminfo = SimInfoManager.getSimInfoBySlot(ctx, cardSlot);
                if (siminfo == null) {
                    return null;
                } else {
                    EncapsulatedSimInfoManager mSimInfo = new EncapsulatedSimInfoManager(siminfo);
                    return mSimInfo;
                }
            } else {
                if (cardSlot < 0)
                    return null;
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, null,
                        SimInfoManager.SLOT + "=?", new String[] {
                            String.valueOf(cardSlot)
                        }, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return EncapsulatedSimInfoManager.fromCursor(cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }
        }

        /**
         * @param ctx
         * @param iccid
         * @return The SIM-Info, maybe null
         */
        public static EncapsulatedSimInfoManager getSIMInfoByICCId(Context ctx, String iccid) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                SimInfoManager.SimInfoRecord siminfo =SimInfoManager.getSimInfoByIccId(ctx, iccid);
                if (siminfo == null) {
                    return null;
                } else {
                    EncapsulatedSimInfoManager mSimInfo = new EncapsulatedSimInfoManager(siminfo);
                    return mSimInfo;
                }
            } else {
                if (iccid == null)
                    return null;
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, null,
                        SimInfoManager.ICC_ID + "=?", new String[] {
                            iccid
                        }, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return EncapsulatedSimInfoManager.fromCursor(cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }
        }

        /**
         * @param ctx
         * @param SIMId
         * @return the slot of the SIM Card, -1 indicate that the SIM card is
         *         missing
         */
        public static int getSlotById(Context ctx, long SIMId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            int simNo = -1;
            SimInfoRecord simInfo = SimInfoManager.getSimInfoById(ctx, SIMId);
            if (simInfo != null) {
                simNo = simInfo.mSimSlotId;
            }
            return simNo;
        } else {
                if (SIMId <= 0)
                    return SimInfoManager.SLOT_NONE;
                Cursor cursor = ctx.getContentResolver().query(
                        ContentUris.withAppendedId(SimInfoManager.CONTENT_URI, SIMId), new String[] {
                            SimInfoManager.SLOT
                        }, null, null, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return cursor.getInt(0);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return SimInfoManager.SLOT_NONE;
            }
        }

        /**
         * @param ctx
         * @param SIMId
         * @return the id of the SIM Card, 0 indicate that no SIM card is
         *         inserted
         */
        public static long getIdBySlot(Context ctx, int slot) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                long simNo = -1;
                SimInfoRecord simInfo = SimInfoManager.getSimInfoBySlot(ctx, slot);
                if (simInfo != null) {
                    simNo = simInfo.mSimInfoId;
                }
                return simNo;
            } else {
                EncapsulatedSimInfoManager simInfo = getSimInfoBySlot(ctx, slot);
                if (simInfo != null)
                    return simInfo.mSimInfoId ;
                return 0;
            }
        }

        /**
         * @param ctx
         * @param SIMName
         * @return the slot of the SIM Card, -1 indicate that the SIM card is
         *         missing
         */
        public static int getSlotByName(Context ctx, String SIMName) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                int simNo = -1;
                SimInfoRecord simInfo = SimInfoManager.getSimInfoByName(ctx, SIMName);
                if (simInfo != null) {
                    simNo = simInfo.mSimSlotId;
                }
                return simNo;
            } else {
                if (SIMName == null)
                    return SimInfoManager.SLOT_NONE;
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, new String[] {
                    SimInfoManager.SLOT
                }, SimInfoManager.DISPLAY_NAME + "=?", new String[] {
                    SIMName
                }, null);
                try {
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            return cursor.getInt(0);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return SimInfoManager.SLOT_NONE;
            }
        }

        /**
         * @param ctx
         * @return current SIM Count
         */
        public static int getInsertedSIMCount(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.getInsertedSimCount(ctx);
            } else {
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, null,
                        SimInfoManager.SLOT + "!=" + SimInfoManager.SLOT_NONE, null, null);
                try {
                    if (cursor != null) {
                        return cursor.getCount();
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return 0;
            }
        }

        /**
         * @param ctx
         * @return the count of all the SIM Card include what was used before
         */
        public static int getAllSIMCount(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.getAllSimCount(ctx);
            } else {
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, null, null,
                        null, null);
                try {
                    if (cursor != null) {
                        return cursor.getCount();
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return 0;
            }
        }

        /**
         * set display name by SIM ID
         * 
         * @param ctx
         * @param displayName
         * @param SIMId
         * @return -1 means general error, -2 means the name is exist. >0 means
         *         success
         */
        public static int setDisplayName(Context ctx, String displayName, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.setDisplayName(ctx, displayName, SIMId);
            } else {
                if (displayName == null || SIMId <= 0)
                    return ErrorCode.ERROR_GENERAL;
                Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, new String[] {
                    SimInfoManager._ID
                }, SimInfoManager.DISPLAY_NAME + "=?", new String[] {
                    displayName
                }, null);
                try {
                    if (cursor != null) {
                        if (cursor.getCount() > 0) {
                            return ErrorCode.ERROR_NAME_EXIST;
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                ContentValues value = new ContentValues(1);
                value.put(SimInfoManager.DISPLAY_NAME, displayName);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfoManager.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * @param ctx
         * @param number
         * @param SIMId
         * @return >0 means success
         */
        public static int setNumber(Context ctx, String number, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.setNumber(ctx, number, SIMId);
            } else {
                if (number == null || SIMId <= 0)
                    return -1;
                ContentValues value = new ContentValues(1);
                value.put(SimInfoManager.NUMBER, number);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfoManager.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * @param ctx
         * @param color
         * @param SIMId
         * @return >0 means success
         */
        public static int setColor(Context ctx, int color, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.setColor(ctx, color, SIMId);
            } else {
                int size = SIMBackgroundRes.length;
                if (color < 0 || SIMId <= 0 || color >= size)
                    return -1;
                ContentValues value = new ContentValues(1);
                value.put(SimInfoManager.COLOR, color);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfoManager.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * set the format.0: none, 1: the first four digits, 2: the last four
         * digits.
         * 
         * @param ctx
         * @param format
         * @param SIMId
         * @return >0 means success
         */
        public static int setDispalyNumberFormat(Context ctx, int format, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.setDispalyNumberFormat(ctx, format, SIMId);
            } else {
                if (format < 0 || SIMId <= 0)
                    return -1;
                ContentValues value = new ContentValues(1);
                value.put(SimInfoManager.DISPLAY_NUMBER_FORMAT, format);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfoManager.CONTENT_URI, SIMId), value, null, null);
            }
        }

        /**
         * set data roaming.0:Don't allow data when roaming, 1:Allow data when
         * roaming
         * 
         * @param ctx
         * @param roaming
         * @param SIMId
         * @return >0 means success
         */
        public static int setDataRoaming(Context ctx, int roaming, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.setDataRoaming(ctx, roaming, SIMId);
            } else {
                if (roaming < 0 || SIMId <= 0)
                    return -1;
                ContentValues value = new ContentValues(1);
                value.put(SimInfoManager.DATA_ROAMING, roaming);
                return ctx.getContentResolver().update(
                        ContentUris.withAppendedId(SimInfoManager.CONTENT_URI, SIMId), value, null, null);
            }
        }

        public static int setDefaultName(Context ctx, long simId, String name) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.setDefaultName(ctx, simId, name);
            } else {
                if (simId <= 0)
                    return ErrorCode.ERROR_GENERAL;
                String default_name = ctx.getString(SimInfoManager.DEFAULT_NAME_RES);
                ContentResolver resolver = ctx.getContentResolver();
                Uri uri = ContentUris.withAppendedId(SimInfoManager.CONTENT_URI, simId);
                if (name != null) {
                    int result = setDisplayName(ctx, name, simId);
                    if (result > 0) {
                        return result;
                    }
                }
                int index = getAppropriateIndex(ctx, simId, name);
                String suffix = getSuffixFromIndex(index);
                ContentValues value = new ContentValues(1);
                String display_name = (name == null ? default_name + " " + suffix : name + " "
                        + suffix);
                value.put(SimInfoManager.DISPLAY_NAME, display_name);
                return ctx.getContentResolver().update(uri, value, null, null);
            }
        }

        private static String getSuffixFromIndex(int index) {
            if (index < 10) {
                return "0" + index;
            } else {
                return String.valueOf(index);
            }
        }

        private static int getAppropriateIndex(Context ctx, long simId, String name) {
            String default_name = ctx.getString(SimInfoManager.DEFAULT_NAME_RES);
            StringBuilder sb = new StringBuilder(SimInfoManager.DISPLAY_NAME + " LIKE ");
            if (name == null) {
                DatabaseUtils.appendEscapedSQLString(sb, default_name + '%');
            } else {
                DatabaseUtils.appendEscapedSQLString(sb, name + '%');
            }
            sb.append(" AND (");
            sb.append(SimInfoManager._ID + "!=" + simId);
            sb.append(")");

            Cursor cursor = ctx.getContentResolver().query(SimInfoManager.CONTENT_URI, new String[] {
                    SimInfoManager._ID, SimInfoManager.DISPLAY_NAME
            }, sb.toString(), null, SimInfoManager.DISPLAY_NAME);
            ArrayList<Long> array = new ArrayList<Long>();
            int index = SimInfoManager.DEFAULT_NAME_MIN_INDEX;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String display_name = cursor.getString(1);

                    if (display_name != null) {
                        int length = display_name.length();
                        if (length >= 2) {
                            String sub = display_name.substring(length - 2);
                            if (TextUtils.isDigitsOnly(sub)) {
                                long value = Long.valueOf(sub);
                                array.add(value);
                            }
                        }
                    }
                }
                cursor.close();
            }
            for (int i = SimInfoManager.DEFAULT_NAME_MIN_INDEX; i <= SimInfoManager.DEFAULT_NAME_MAX_INDEX; i++) {
                if (array.contains((long) i)) {
                    continue;
                } else {
                    index = i;
                    break;
                }
            }
            return index;
        }
    }
