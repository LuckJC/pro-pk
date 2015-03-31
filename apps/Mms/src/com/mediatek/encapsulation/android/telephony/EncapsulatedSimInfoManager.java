package com.mediatek.encapsulation.android.telephony;

import android.os.RemoteException;
import android.provider.BaseColumns;
import android.net.Uri;
import android.content.Context;
import android.database.Cursor;
import android.content.ContentUris;

import com.android.mms.MmsConfig;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.internal.EncapsulatedR;
import com.mediatek.encapsulation.MmsLog;

import java.util.ArrayList;
import java.util.List;

public class EncapsulatedSimInfoManager implements BaseColumns {
        public long mSimInfoId;
        public String mIccId;
        public String mDisplayName = "";
        public int mNameSource;
        public String mNumber = "";
        public int mDispalyNumberFormat = DISLPAY_NUMBER_DEFAULT;
        public int mColor;
        public int mDataRoaming = DATA_ROAMING_DEFAULT;
        public int mSimSlotId = SLOT_NONE;
        public int mSimBackgroundRes = SIMBackgroundRes[COLOR_DEFAULT];
        public String mOperator = "";
        public int mWapPush = -1;
        public int mSimBackgroundDarkRes = SIMBackgroundDarkRes[COLOR_DEFAULT];
        public int mSimBackgroundLightRes = SIMBackgroundLightRes[COLOR_DEFAULT];
        /** M: MTK Add */
        public static final int[] SIMBackgroundRes = new int[] {
            EncapsulatedR.drawable.sim_background_blue,
            EncapsulatedR.drawable.sim_background_orange,
            EncapsulatedR.drawable.sim_background_green,
            EncapsulatedR.drawable.sim_background_purple
        };

        // add by mtk02772 for Consistent UI Design start
        public static final int[] SIMBackgroundDarkRes = new int[] {
            EncapsulatedR.drawable.sim_dark_blue,
            EncapsulatedR.drawable.sim_dark_orange,
            EncapsulatedR.drawable.sim_dark_green,
            EncapsulatedR.drawable.sim_dark_purple
        };

        public static final int[] SIMBackgroundLightRes = new int[] {
            EncapsulatedR.drawable.sim_light_blue,
            EncapsulatedR.drawable.sim_light_orange,
            EncapsulatedR.drawable.sim_light_green,
            EncapsulatedR.drawable.sim_light_purple
        };
        // add by mtk02772 for Consistent UI Design end
        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.CONTENT_URI : Uri.parse("content://telephony/siminfo");

        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String DISPLAY_NAME = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPLAY_NAME : "display_name";

        public static final int DEFAULT_NAME_MIN_INDEX = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DEFAULT_NAME_MIN_INDEX : 01;

        public static final int DEFAULT_NAME_MAX_INDEX = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DEFAULT_NAME_MAX_INDEX : 99;

        public static final int DEFAULT_NAME_RES = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DEFAULT_NAME_RES : EncapsulatedR.string.new_sim;

        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ICC_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.ICC_ID : "icc_id";

        /**
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String NUMBER = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.NUMBER : "number";

        /**
         * 0:none, 1:the first four digits, 2:the last four digits.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DISPLAY_NUMBER_FORMAT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPLAY_NUMBER_FORMAT : "display_number_format";

        public static final int DISPALY_NUMBER_NONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPALY_NUMBER_NONE : 0;

        public static final int DISPLAY_NUMBER_FIRST = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPLAY_NUMBER_FIRST : 1;

        public static final int DISPLAY_NUMBER_LAST = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISPLAY_NUMBER_LAST : 2;

        public static final int DISLPAY_NUMBER_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DISLPAY_NUMBER_DEFAULT : DISPLAY_NUMBER_FIRST;

        /**
         * Eight kinds of colors. 0-3 will represent the eight colors. Default
         * value: any color that is not in-use.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String COLOR = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR : "color";

        public static final int COLOR_1 = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_1 : 0;

        public static final int COLOR_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_2 : 1;

        public static final int COLOR_3 = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_3 : 2;

        public static final int COLOR_4 = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_4 : 3;

        public static final int COLOR_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.COLOR_DEFAULT : COLOR_1;

        /**
         * 0: Don't allow data when roaming, 1:Allow data when roaming
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DATA_ROAMING = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DATA_ROAMING : "data_roaming";

        public static final int DATA_ROAMING_ENABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DATA_ROAMING_ENABLE : 1;

        public static final int DATA_ROAMING_DISABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DATA_ROAMING_DISABLE : 0;

        public static final int DATA_ROAMING_DEFAULT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.DATA_ROAMING_DEFAULT : DATA_ROAMING_DISABLE;

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SLOT = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.SLOT : "slot";

        public static final int SLOT_NONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.SLOT_NONE : -1;

        public static final int ERROR_GENERAL = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.ERROR_GENERAL : -1;

        public static final int ERROR_NAME_EXIST = EncapsulationConstant.USE_MTK_PLATFORM ?
                SimInfoManager.ERROR_NAME_EXIST : -2;

        private SimInfoManager.SimInfoRecord mSIMInfoRecord;

        private EncapsulatedSimInfoManager() {
        }

        public EncapsulatedSimInfoManager(SimInfoManager.SimInfoRecord simInfoRecord) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                if (simInfoRecord != null) {
                    mSIMInfoRecord = simInfoRecord;
                }
            }
        }

        public long getSimId() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mSimInfoId ;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSimId()");
                return mSimInfoId;
            }
        }

        public String getDisplayName() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mDisplayName;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getDisplayName()");
                return mDisplayName;
            }
        }

        public String getNumber() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mNumber;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getNumber()");
                return mNumber;
            }
        }

        public int getDispalyNumberFormat() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mDispalyNumberFormat;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getDispalyNumberFormat()");
                return mDispalyNumberFormat;
            }
        }

        public int getColor() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mColor;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getColor()");
                return mColor;
            }
        }

        public int getSlot() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mSimSlotId;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSlot()");
                return mSimSlotId;
            }
        }

        public int getSimBackgroundRes() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mSimBackgroundRes;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSimBackgroundRes()");
                return android.R.color.background_light;
            }
        }

        public int getSimBackgroundLightRes() {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return mSIMInfoRecord.mSimBackgroundLightRes;
            } else {
                MmsLog.d("Encapsulation issue", "EncapsulatedTelephony.SIMInfo -- getSimBackgroundLightRes()");
                return android.R.color.background_light;
            }
        }

        private static EncapsulatedSimInfoManager fromCursor(Cursor cursor) {
            EncapsulatedSimInfoManager info = new EncapsulatedSimInfoManager();
            info.mSimInfoId  = cursor.getLong(cursor.getColumnIndexOrThrow(EncapsulatedSimInfoManager._ID));
            info.mIccId = cursor.getString(cursor.getColumnIndexOrThrow(EncapsulatedSimInfoManager.ICC_ID));
            info.mDisplayName = cursor
                    .getString(cursor.getColumnIndexOrThrow(EncapsulatedSimInfoManager.DISPLAY_NAME));
            info.mNumber = cursor.getString(cursor.getColumnIndexOrThrow(EncapsulatedSimInfoManager.NUMBER));
            info.mDispalyNumberFormat = cursor.getInt(cursor
                    .getColumnIndexOrThrow(EncapsulatedSimInfoManager.DISPLAY_NUMBER_FORMAT));
            info.mColor = cursor.getInt(cursor.getColumnIndexOrThrow(EncapsulatedSimInfoManager.COLOR));
            info.mDataRoaming = cursor.getInt(cursor.getColumnIndexOrThrow(EncapsulatedSimInfoManager.DATA_ROAMING));
            info.mSimSlotId = cursor.getInt(cursor.getColumnIndexOrThrow(EncapsulatedSimInfoManager.SLOT));
            int size = SIMBackgroundRes.length;
            if (info.mColor >= 0 && info.mColor < size) {
                info.mSimBackgroundRes = SIMBackgroundRes[info.mColor];

                // add by mtk02772 for Consistent UI Design start
                info.mSimBackgroundDarkRes = SIMBackgroundDarkRes[info.mColor];
                info.mSimBackgroundLightRes = SIMBackgroundLightRes[info.mColor];
                // add by mtk02772 for Consistent UI Design end
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
                Cursor cursor = ctx.getContentResolver().query(EncapsulatedSimInfoManager.CONTENT_URI, null,
                        EncapsulatedSimInfoManager.SLOT + "!=" + EncapsulatedSimInfoManager.SLOT_NONE, null, null);
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
                if (SIMId <= 0) {
                    return null;
                }
                Cursor cursor = ctx.getContentResolver().query(
                        ContentUris.withAppendedId(EncapsulatedSimInfoManager.CONTENT_URI, SIMId), null, null, null, null);
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
                Cursor cursor = ctx.getContentResolver().query(EncapsulatedSimInfoManager.CONTENT_URI, null,
                        EncapsulatedSimInfoManager.SLOT + "=?", new String[] {
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
         * @param SIMId
         * @return the slot of the SIM Card, -1 indicate that the SIM card is
         *         missing
         */
        public static int getSlotById(Context ctx, long SIMId) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                SimInfoManager.SimInfoRecord siminfo = SimInfoManager.getSimInfoById(ctx, SIMId);
                if (siminfo == null) {
                    return EncapsulatedSimInfoManager.SLOT_NONE;
                } else {
                    return siminfo.mSimSlotId;
                }
            } else {
                if (SIMId <= 0) {
                    return EncapsulatedSimInfoManager.SLOT_NONE;
                }
                Cursor cursor = ctx.getContentResolver().query(
                        ContentUris.withAppendedId(EncapsulatedSimInfoManager.CONTENT_URI, SIMId), new String[] {
                            EncapsulatedSimInfoManager.SLOT
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
                return EncapsulatedSimInfoManager.SLOT_NONE;
            }
        }

        /**
         * @param ctx
         * @param SIMId
         * @return the id of the SIM Card, 0 indicate that no SIM card is
         *         inserted
         */
        public static long getIdBySlot(Context ctx, int slot) {
            EncapsulatedSimInfoManager simInfo = getSimInfoBySlot(ctx, slot);
            if (simInfo != null) {
                if (EncapsulationConstant.USE_MTK_PLATFORM) {
                    return simInfo.mSIMInfoRecord.mSimInfoId;
                } else {
                    return simInfo.mSimInfoId;
                }
            }
            return 0;
        }

        /**
         * @param ctx
         * @return current SIM Count
         */
        public static int getInsertedSimCount(Context ctx) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return SimInfoManager.getInsertedSimCount(ctx);
            } else {
                Cursor cursor = ctx.getContentResolver().query(EncapsulatedSimInfoManager.CONTENT_URI, null,
                        EncapsulatedSimInfoManager.SLOT + "!=" + EncapsulatedSimInfoManager.SLOT_NONE, null, null);
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
                Cursor cursor = ctx.getContentResolver().query(EncapsulatedSimInfoManager.CONTENT_URI, null, null,
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
         * M: check whether user can view SIM card message.
         * FeatureOption:
         * MTK_FLIGHT_MODE_POWER_OFF_MD: power off modem when flight mode is on.
         * MTK_RADIOOFF_POWER_OFF_MD: power off modem when radio is off.
         *
         * @return true for accessable
         */
        public static boolean isSimMessageAccessable(Context context) {
            // First, forbid to access SIM message if this is not default MMS.
            boolean isSmsEnable = MmsConfig.isSmsEnabled(context);
            if(!isSmsEnable) {
                MmsLog.d("EncapsulatedSimInfoManager", "isSimMessageAccessable Sms not enabled");
                return false;
            }

            // Second, check airplane mode
            boolean airplaneOn = EncapsulatedSettings.System.getInt(context.getContentResolver(),
                    EncapsulatedSettings.System.AIRPLANE_MODE_ON, 0) == 1;
            if (airplaneOn) {
                MmsLog.d("EncapsulatedSimInfoManager", "isSimMessageAccessable airplane is On");
                return false;
            }

            // Third, check whether has inserted SIM
            List<EncapsulatedSimInfoManager> listSimInfo = EncapsulatedSimInfoManager
                    .getInsertedSimInfoList(context);
            if (listSimInfo.size() == 0) {
                MmsLog.d("EncapsulatedSimInfoManager", "isSimMessageAccessable SIM not insert");
                return false;
            }

            // Forth, check radio
            boolean isSimRadioOn = false;
            EncapsulatedTelephonyService iTelephony = EncapsulatedTelephonyService.getInstance();
            if (iTelephony == null) {
                MmsLog.d("EncapsulatedSimInfoManager", "iTelephony == null");
                return false;
            }
            try {
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    for (EncapsulatedSimInfoManager simInfo : listSimInfo) {
                        isSimRadioOn = iTelephony.isRadioOn(simInfo.getSlot());
                        if (isSimRadioOn) {
                            break;
                        }
                    }
                } else {
                    isSimRadioOn = iTelephony.isRadioOn();
                }
            } catch (RemoteException e) {
            }
            MmsLog.d("EncapsulatedSimInfoManager", "isSimMessageAccessable" + isSimRadioOn);
            return isSimRadioOn;
        }

        /**
         * M: check whether user can view one SIM card's message by slotId.
         * FeatureOption:
         * MTK_FLIGHT_MODE_POWER_OFF_MD: power off modem when flight mode is on.
         * MTK_RADIOOFF_POWER_OFF_MD: power off modem when radio is off.
         *
         * @return true for accessable
         */
        public static boolean isSimMessageAccessable(Context context, int slotId) {
            // First, forbid to access SIM message if this is not default MMS.
            boolean isSmsEnable = MmsConfig.isSmsEnabled(context);
            if(!isSmsEnable) {
                MmsLog.d("EncapsulatedSimInfoManager", "isSimMessageAccessable Sms not enabled");
                return false;
            }

            // Second, check airplane mode
            boolean airplaneOn = EncapsulatedSettings.System.getInt(context.getContentResolver(),
                    EncapsulatedSettings.System.AIRPLANE_MODE_ON, 0) == 1;
            if (airplaneOn) {
                MmsLog.d("EncapsulatedSimInfoManager", "isSimMessageAccessable airplane is On");
                return false;
            }

            // Third, check radio.(no need check insert state, because registered a listener
            // of hot_swap)
            EncapsulatedTelephonyService iTelephony = EncapsulatedTelephonyService.getInstance();
            if (iTelephony == null) {
                MmsLog.d("EncapsulatedSimInfoManager", "iTelephony == null");
                return false;
            }
            boolean isSimRadioOn = false;
            try {
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    isSimRadioOn = iTelephony.isRadioOn(slotId);
                } else {
                    isSimRadioOn = iTelephony.isRadioOn();
                }
            } catch (RemoteException e) {
            }
            MmsLog.d("EncapsulatedSimInfoManager", "isSimMessageAccessable" + isSimRadioOn + " slot: "
                    + slotId);
            return isSimRadioOn;
        }
    }