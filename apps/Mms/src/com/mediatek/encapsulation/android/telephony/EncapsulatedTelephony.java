package com.mediatek.encapsulation.android.telephony;

import android.provider.BaseColumns;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.provider.Telephony;

import com.mediatek.encapsulation.EncapsulationConstant;

/// M: ALPS00510627, SMS Framewrok API refactoring
public class EncapsulatedTelephony {
    private static final String TAG = "EncapsulatedTelephony";

    /** M: MTK Add */
    public interface TextBasedSmsCbColumns {

        /**
         * The SIM ID which indicated which SIM the SMSCb comes from Reference
         * to Telephony.SIMx
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.SIM_ID : "sim_id";

        /**
         * The channel ID of the message which is the message identifier defined
         * in the Spec. 3GPP TS 23.041
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String CHANNEL_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.CHANNEL_ID : "channel_id";

        /**
         * The date the message was sent
         * <P>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String DATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.DATE : "date";

        /**
         * Has the message been read
         * <P>
         * Type: INTEGER (boolean)
         * </P>
         */
        public static final String READ = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.READ : "read";

        /**
         * The body of the message
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String BODY = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.BODY : "body";

        /**
         * The thread id of the message
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String THREAD_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.THREAD_ID : "thread_id";

        /**
         * Indicates whether this message has been seen by the user. The "seen"
         * flag will be used to figure out whether we need to throw up a
         * statusbar notification or not.
         */
        public static final String SEEN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.SEEN : "seen";

        /**
         * Has the message been locked?
         * <P>
         * Type: INTEGER (boolean)
         * </P>
         */
        public static final String LOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsCbColumns.LOCKED : "locked";
    }

    /** M: MTK Add */
    public static final class SmsCb implements BaseColumns, TextBasedSmsCbColumns {

        public static final Cursor query(ContentResolver cr, String[] projection) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SmsCb.query(cr, projection);
            } else {
                return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
            }
        }

        public static final Cursor query(ContentResolver cr, String[] projection, String where,
                String orderBy) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SmsCb.query(cr, projection, where, orderBy);
            } else {
                return cr.query(CONTENT_URI, projection, where, null,
                        orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
            }

        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SmsCb.CONTENT_URI : Uri.parse("content://cb/messages");

        /**
         * The content:// style URL for "canonical_addresses" table
         */
        public static final Uri ADDRESS_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SmsCb.ADDRESS_URI : Uri.parse("content://cb/addresses");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.SmsCb.DEFAULT_SORT_ORDER : "date DESC";

        /**
         * Add an SMS to the given URI with thread_id specified.
         * 
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param sim_id the id of the SIM card
         * @param channel_id the message identifier of the CB message
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param body the body of the message
         * @return the URI for the new message
         */
        public static Uri addMessageToUri(ContentResolver resolver, Uri uri, int sim_id,
                int channel_id, long date, boolean read, String body) {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                return Telephony.SmsCb.addMessageToUri(resolver, uri, sim_id, channel_id, date, read, body);
            } else {
                ContentValues values = new ContentValues(5);

                values.put(SIM_ID, Integer.valueOf(sim_id));
                values.put(DATE, Long.valueOf(date));
                values.put(READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
                values.put(BODY, body);
                values.put(CHANNEL_ID, Integer.valueOf(channel_id));

                return resolver.insert(uri, values);
            }
        }

        /**
         * Contains all received SMSCb messages in the SMS app's.
         */
        public static final class Conversations implements BaseColumns, TextBasedSmsCbColumns {
            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.CONTENT_URI : Uri.parse("content://cb/threads");

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.DEFAULT_SORT_ORDER : "date DESC";

            /**
             * The first 45 characters of the body of the message
             * <P>
             * Type: TEXT
             * </P>
             */
            public static final String SNIPPET = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.SNIPPET : "snippet";

            /**
             * The number of messages in the conversation
             * <P>
             * Type: INTEGER
             * </P>
             */
            public static final String MESSAGE_COUNT = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.MESSAGE_COUNT : "msg_count";

            /**
             * The _id of address table in the conversation
             * <P>
             * Type: INTEGER
             * </P>
             */
            public static final String ADDRESS_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.Conversations.ADDRESS_ID : "address_id";
        }

        /**
         * Columns for the "canonical_addresses" table used by CB-SMS
         */
        public interface CanonicalAddressesColumns extends BaseColumns {
            /**
             * An address used in CB-SMS. Just a channel number
             * <P>
             * Type: TEXT
             * </P>
             */
            public static final String ADDRESS = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CanonicalAddressesColumns.ADDRESS : "address";
        }

        /**
         * Columns for the "canonical_addresses" table used by CB-SMS
         */
        public static final class CbChannel implements BaseColumns {
            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CbChannel.CONTENT_URI : Uri.parse("content://cb/channel");

            public static final String NAME = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CbChannel.NAME : "name";

            public static final String NUMBER = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CbChannel.NUMBER : "number";

            public static final String ENABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.SmsCb.CbChannel.ENABLE : "enable";

        }

    }

    public interface BaseMmsColumns {
        /** M: MTK Add */
        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.BaseMmsColumns.SIM_ID : "sim_id";

        /** M: MTK Add */
        public static final String SERVICE_CENTER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.BaseMmsColumns.SERVICE_CENTER : "service_center";
    }

    public static final class Mms implements BaseMmsColumns {

        /** M: MTK Add */
        public static final class ScrapSpace {
            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Mms.ScrapSpace.CONTENT_URI : Uri.parse("content://mms/scrapSpace");

            /**
             * This is the scrap file we use to store the media attachment when
             * the user chooses to capture a photo to be attached . We pass
             * {#link@Uri} to the Camera app, which streams the captured image
             * to the uri. Internally we write the media content to this file.
             * It's named '.temp.jpg' so Gallery won't pick it up.
             */
            public static final String SCRAP_FILE_PATH = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Mms.ScrapSpace.SCRAP_FILE_PATH : "/sdcard/mms/scrapSpace/.temp.jpg";
        }
    }

    public static final class MmsSms {

        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.MmsSms.CONTENT_URI : Uri.parse("content://mms-sms/");

        /** M: MTK Add */
        public static final Uri CONTENT_URI_QUICKTEXT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.MmsSms.CONTENT_URI_QUICKTEXT : Uri.parse("content://mms-sms/quicktext");

        /** M: MTK Add */
        public static final class PendingMessages {
            public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.MmsSms.PendingMessages.SIM_ID : "pending_sim_id";

        }
    }

    /**
     * Base columns for tables that contain text based SMSs.
     */
    public interface TextBasedSmsColumns {
        /** M: MTK Add */
        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.TextBasedSmsColumns.SIM_ID : "sim_id";
    }

    /**
     * Contains all text based SMS messages.
     */
    public static final class Sms implements BaseColumns, TextBasedSmsColumns {

        /**
         * Contains info about SMS related Intents that are broadcast.
         */
        public static final class Intents {
            //MTK-START [mtk04070][111121][ALPS00093395]MTK added
            /**
             * Broadcast Action: The SMS sub-system in the modem is ready.
             * The intent is sent to inform the APP if the SMS sub-system
             * is ready or not. The intent will have the following extra value:</p>
             *
             * <ul>
             *   <li><em>ready</em> - An boolean result code, true for ready</li>
             * </ul>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String SMS_STATE_CHANGED_ACTION = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Sms.Intents.SMS_STATE_CHANGED_ACTION : "android.provider.Telephony.SMS_STATE_CHANGED";
        }

        public static final int STATUS_REPLACED_BY_SC = EncapsulationConstant.USE_MTK_PLATFORM ?
                                        Telephony.Sms.STATUS_REPLACED_BY_SC : 2;

        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                                        Telephony.Sms.SIM_ID : "sim_id";

        /// M: add for ip message
        public static final String IPMSG_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                                        Telephony.Sms.IPMSG_ID : "ipmsg_id";

        /** M: MTK Add */
        /**
         * Add an SMS to the given URI with thread_id specified.
         * 
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param address the address of the sender
         * @param body the body of the message
         * @param subject the psuedo-subject of the message
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param deliveryReport true if a delivery report was requested, false
         *            if not
         * @param threadId the thread_id of the message
         * @param simId the sim_id of the message
         * @return the URI for the new message
         */
        public static Uri addMessageToUri(ContentResolver resolver, Uri uri, String address,
                String body, String subject, Long date, boolean read, boolean deliveryReport,
                long threadId, int simId) {
            return addMessageToUri(resolver, uri, address, body, subject, null, date, read,
                    deliveryReport, threadId, simId);
        }

        /** M: MTK Add */
        /**
         * Add an SMS to the given URI with thread_id specified.
         * 
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param address the address of the sender
         * @param body the body of the message
         * @param subject the psuedo-subject of the message
         * @param sc the service center of the message
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param deliveryReport true if a delivery report was requested, false
         *            if not
         * @param threadId the thread_id of the message
         * @param simId the sim_id of the message
         * @return the URI for the new message
         */
        public static Uri addMessageToUri(ContentResolver resolver, Uri uri, String address,
                String body, String subject, String sc, Long date, boolean read,
                boolean deliveryReport, long threadId, int simId) {
            ContentValues values = new ContentValues(8);

            values.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
            if (date != null) {
                values.put(Telephony.TextBasedSmsColumns.DATE, date);
            }
            if (sc != null) {
                values.put(Telephony.TextBasedSmsColumns.SERVICE_CENTER, sc);
            }
            values.put(Telephony.TextBasedSmsColumns.READ, read ? Integer.valueOf(1) : Integer
                    .valueOf(0));
            values.put(Telephony.TextBasedSmsColumns.SUBJECT, subject);
            values.put(Telephony.TextBasedSmsColumns.BODY, body);
            values.put(Telephony.TextBasedSmsColumns.SEEN, read ? Integer.valueOf(1) : Integer
                    .valueOf(0));
            if (deliveryReport) {
                values.put(Telephony.TextBasedSmsColumns.STATUS,
                        Telephony.TextBasedSmsColumns.STATUS_PENDING);
            }
            if (threadId != -1L) {
                values.put(Telephony.TextBasedSmsColumns.THREAD_ID, threadId);
            }

            if (simId != -1) {
                values.put(SIM_ID, simId);
            }

            return resolver.insert(uri, values);
        }

        public static final class Inbox implements BaseColumns, TextBasedSmsColumns {
            /** M: MTK Add */
            public static Uri addMessage(ContentResolver resolver, String address, String body,
                    String subject, String sc, Long date, boolean read, int simId) {
                return addMessageToUri(resolver, Telephony.Sms.Inbox.CONTENT_URI, address, body,
                        subject, sc, date, read, false, -1L, simId);
            }
        }

        public static final class Sent implements BaseColumns, TextBasedSmsColumns {
            /** M: MTK Add */
            public static Uri addMessage(ContentResolver resolver, String address, String body,
                    String subject, String sc, Long date, int simId) {
                return addMessageToUri(resolver, Telephony.Sms.Sent.CONTENT_URI, address, body,
                        subject, sc, date, true, false, -1L, simId);
            }
        }

    }

    public interface ThreadsColumns extends BaseColumns {

        /** M: MTK Add */
        /**
         * The read message count of the thread.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String READCOUNT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadsColumns.READCOUNT : "readcount";
    }

    public static final class Threads implements ThreadsColumns {

        private static final String[] ID_PROJECTION = { BaseColumns._ID };
        private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");

        /** M: MTK Add */
        public static final int WAPPUSH_THREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.WAPPUSH_THREAD : 2;

        /** M: MTK Add */
        public static final int CELL_BROADCAST_THREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.CELL_BROADCAST_THREAD : 3;

        /** M: MTK Add */
        public static final int IP_MESSAGE_GUIDE_THREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.IP_MESSAGE_GUIDE_THREAD : 10;

        /** M: MTK Add */
        public static final String LATEST_IMPORTANT_SNIPPET_CHARSET = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.LATEST_IMPORTANT_SNIPPET_CHARSET : "li_snippet_cs";

        public static final String LATEST_IMPORTANT_DATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.LATEST_IMPORTANT_DATE : "li_date";

        public static final String LATEST_IMPORTANT_SNIPPET = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.LATEST_IMPORTANT_SNIPPET : "li_snippet";

        /** M: MTK Add */
        /**
         * Whether a thread is being writen or not 0: normal 1: being writen
         * <P>
         * Type: INTEGER (boolean)
         * </P>
         */
        public static final String STATUS = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.STATUS : "status";

        public static final String DATE_SENT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Threads.DATE_SENT : "date_sent";
    }

    /** M: MTK Add */
    public static final class WapPush implements BaseColumns {

        // public static final Uri CONTENT_URI =
        public static final String DEFAULT_SORT_ORDER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.DEFAULT_SORT_ORDER : "date ASC";

        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CONTENT_URI : Uri.parse("content://wappush");

        public static final Uri CONTENT_URI_SI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CONTENT_URI_SI : Uri.parse("content://wappush/si");

        public static final Uri CONTENT_URI_SL = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CONTENT_URI_SL : Uri.parse("content://wappush/sl");

        public static final Uri CONTENT_URI_THREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CONTENT_URI_THREAD : Uri.parse("content://wappush/thread_id");

        // Database Columns
        public static final String THREAD_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.THREAD_ID : "thread_id";

        public static final String ADDR = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.ADDR : "address";

        public static final String SERVICE_ADDR = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.SERVICE_ADDR : "service_center";

        public static final String READ = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.READ : "read";

        public static final String SEEN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.SEEN : "seen";

        public static final String LOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.LOCKED : "locked";

        public static final String ERROR = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.ERROR : "error";

        public static final String DATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.DATE : "date";

        public static final String TYPE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.TYPE : "type";

        public static final String SIID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.SIID : "siid";

        public static final String URL = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.URL : "url";

        public static final String CREATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.CREATE : "created";

        public static final String EXPIRATION = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.EXPIRATION : "expiration";

        public static final String ACTION = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.ACTION : "action";

        public static final String TEXT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.TEXT : "text";

        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.SIM_ID : "sim_id";

        //

        public static final int TYPE_SI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.TYPE_SI : 0;

        public static final int TYPE_SL = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.TYPE_SL : 1;

        public static final int STATUS_SEEN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_SEEN : 1;

        public static final int STATUS_UNSEEN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_UNSEEN : 0;

        public static final int STATUS_READ = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_READ : 1;

        public static final int STATUS_UNREAD = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_UNREAD : 0;

        public static final int STATUS_LOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_LOCKED : 1;

        public static final int STATUS_UNLOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.WapPush.STATUS_UNLOCKED : 0;
    }

    public static final class Carriers implements BaseColumns {

        public static final Uri CONTENT_URI_DM = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.CONTENT_URI_DM : Uri.parse("content://telephony/carriers_dm");

        public static final Uri CONTENT_URI_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.CONTENT_URI_2 : Uri.parse("content://telephony/carriers2");

        public static final String OMACPID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.OMACPID : "omacpid";

        public static final String NAPID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.NAPID : "napid";

        public static final String PROXYID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.PROXYID : "proxyid";

        public static final String SOURCE_TYPE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.SOURCE_TYPE : "sourcetype";

        public static final String CSD_NUM = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.CSD_NUM : "csdnum";

        public static final String SPN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.SPN : "spn";

        public static final String IMSI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.Carriers.IMSI : "imsi";

        public static final class GeminiCarriers {
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Carriers.GeminiCarriers.CONTENT_URI :
                           Uri.parse("content://telephony/carriers_gemini");

            public static final Uri CONTENT_URI_DM = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Carriers.GeminiCarriers.CONTENT_URI_DM :
                           Uri.parse("content://telephony/carriers_dm_gemini");
        }

        public static final class SIM1Carriers {
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Carriers.SIM1Carriers.CONTENT_URI :
                           Uri.parse("content://telephony/carriers_sim1");
        }

        public static final class SIM2Carriers {
            public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                    Telephony.Carriers.SIM2Carriers.CONTENT_URI :
                           Uri.parse("content://telephony/carriers_sim2");
        }

    }

    /** M: MTK Add */
    public static final class GprsInfo implements BaseColumns {
        public static final Uri CONTENT_URI = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.GprsInfo.CONTENT_URI : Uri.parse("content://telephony/gprsinfo");

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.GprsInfo.SIM_ID : "sim_id";

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String GPRS_IN = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.GprsInfo.GPRS_IN : "gprs_in";

        /**
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String GPRS_OUT = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.GprsInfo.GPRS_OUT : "gprs_out";
    }

    public static final class ThreadSettings implements BaseColumns {

        public static void restoreDefaultSettings(Context context, Uri threadSettingUri) {
            ContentValues values = new ContentValues(5);
            values.put(WALLPAPER, "");
            values.put(MUTE, 0);
            values.put(NOTIFICATION_ENABLE,1);
            values.put(RINGTONE, "");
            values.put(VIBRATE, true);
            context.getContentResolver().update(threadSettingUri, values, null, null);
        }

        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                MmsSms.CONTENT_URI, "thread_settings");

        /**
         * Whether a thread is set notification enabled
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String NOTIFICATION_ENABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadSettings.NOTIFICATION_ENABLE : "notification_enable";

        /**
         * Which thread does this settings belongs to
         * <P>Type: INTEGER </P>
         */
        public static final String THREAD_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadSettings.THREAD_ID : "thread_id";

        /**
         * Whether a thread is set spam
         * 0: normal 1: spam
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String SPAM = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadSettings.SPAM : "spam";

        /**
         * Whether a thread is set mute
         * 0: normal >1: mute duration
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String MUTE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadSettings.MUTE : "mute";

        /**
         * when does a thread be set mute
         * 0: normal >1: mute start time
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String MUTE_START = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadSettings.MUTE_START : "mute_start";

        /**
         * Whether a thread is set vibrate
         * 0: normal 1: vibrate
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String VIBRATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadSettings.VIBRATE : "vibrate";

        /**
         * Ringtone for a thread
         * <P>Type: STRING</P>
         */
        public static final String RINGTONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadSettings.RINGTONE : "ringtone";

        /**
         * Wallpaper for a thread
         * <P>Type: STRING</P>
         */
        public static final String WALLPAPER = EncapsulationConstant.USE_MTK_PLATFORM ?
                Telephony.ThreadSettings.WALLPAPER : "_data";
    }
}
