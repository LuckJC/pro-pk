
package com.mediatek.encapsulation.android.telephony;

import android.telephony.SmsManager;
import android.app.PendingIntent;

import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.telephony.SmsManagerEx;
import java.util.ArrayList;
import java.util.List;
import android.os.SystemProperties;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;



public class EncapsulatedSmsManager {
    /** M: MTK ADD */
    //private static final String mNowsmsAddress = "http://172.26.89.66:8800/?PhoneNumber=";
    private static final String mNowsmsAddress = "172.26.89.66";
    public static final int RESULT_ERROR_SUCCESS = 0;
    public static final int RESULT_ERROR_SIM_MEM_FULL = 7;

    public static final int VALIDITY_PERIOD_NO_DURATION = EncapsulationConstant.USE_MTK_PLATFORM ?
                        SmsManager.VALIDITY_PERIOD_NO_DURATION : -1;
    // (VP + 1) * 5 = 6 Mins
    public static final int VALIDITY_PERIOD_ONE_HOUR = EncapsulationConstant.USE_MTK_PLATFORM ?
                        SmsManager.VALIDITY_PERIOD_ONE_HOUR : 11;
    // (VP + 1) * 5 = 6 * 60 Mins
    public static final int VALIDITY_PERIOD_SIX_HOURS = EncapsulationConstant.USE_MTK_PLATFORM ?
                        SmsManager.VALIDITY_PERIOD_SIX_HOURS : 71;
    // (VP + 1) * 5 = 12 * 60 Mins
    public static final int VALIDITY_PERIOD_TWELVE_HOURS = EncapsulationConstant.USE_MTK_PLATFORM ?
                        SmsManager.VALIDITY_PERIOD_TWELVE_HOURS : 143;
    // 12 + (VP - 143) * 30 Mins = 24 Hours
    public static final int VALIDITY_PERIOD_ONE_DAY = EncapsulationConstant.USE_MTK_PLATFORM ?
                        SmsManager.VALIDITY_PERIOD_ONE_DAY : 167;
    // (VP - 192) Weeks
    public static final int VALIDITY_PERIOD_MAX_DURATION = EncapsulationConstant.USE_MTK_PLATFORM ?
                        SmsManager.VALIDITY_PERIOD_MAX_DURATION : 255;

    public static final String EXTRA_PARAMS_VALIDITY_PERIOD = "validity_period";

    /** M: MTK ADD */
    public static void setSmsMemoryStatus(boolean status) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            SmsManagerEx.getDefault().setSmsMemoryStatus(status);
        } else {
            /** M: Can not complete for this branch. */
            MmsLog.d("Encapsulation issue", "EncapsulatedSmsManager -- setSmsMemoryStatus()");
        }
    }

    /** M: MTK ADD */
    public static EncapsulatedSmsMemoryStatus getSmsSimMemoryStatus() {
        return EncapsulatedGeminiSmsManager.getSmsSimMemoryStatusGemini(0/* Phone.GEMINI_SIM_1 */);
    }

    /** M: MTK ADD */
    public static int copyTextMessageToIccCard(String scAddress, String address, List<String> text,
            int status, long timestamp) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().copyTextMessageToIccCard(scAddress, address, text,
                    status, timestamp, 0);
        } else {
            return 0;
        }
    }

    /** M: MTK ADD */
    public static ArrayList<String> divideMessage(String text, int encodingType) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().divideMessage(text, encodingType);
        } else {
            //Do not use encodingType to divide message as default implement.
            return SmsManager.getDefault().divideMessage(text);
        }
    }

    public static ArrayList<String> divideMessage(String text) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().divideMessage(text);
        } else {
            return null;
        }
    }

    /** M: MTK ADD */
    public static void sendMultipartTextMessageWithEncodingType(String destAddr, String scAddr,
            ArrayList<String> parts, int encodingType, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents) {
        String enableNowSMS = SystemProperties.get("net.ENABLE_NOWSMS");
        if (enableNowSMS.equals("true")) {
            /// M:add this for simulate sms sending through nowSms @{
            String smsText = "";
            for (int i = 0; i < parts.size(); i++) {
                smsText += parts.get(i);
            }

            String sender = SystemProperties.get("net.EMULATOR_SENDER_" + 0);
            MmsLog.d("EncapsulatedSmsManager", "sendMultipartTextMessageWithEncodingType sender = " + sender);
            MmsLog.d("EncapsulatedSmsManager", "sendMultipartTextMessageWithEncodingType text = " + smsText);
            String smscStr = SystemProperties.get("net.EMULATOR_SMSC_" + 0);
            if (smscStr == null || smscStr.isEmpty()) {
                smscStr = mNowsmsAddress;
            }
            MmsLog.d("EncapsulatedSmsManager", "smsc is " + smscStr);
            HttpGet getMethod = new HttpGet("http://" + smscStr + ":8800/?PhoneNumber=" + destAddr + "&Text=" + smsText + "&Sender=" + sender);
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpResponse response = httpClient.execute(getMethod);
            } catch (Exception e) {}
/// @}
        } else {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                SmsManagerEx.getDefault().sendMultipartTextMessageWithEncodingType(
                        destAddr, scAddr, parts, encodingType, sentIntents, deliveryIntents, 0);
            } else {
            }
        }
    }

    public static boolean queryCellBroadcastSmsActivation(int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().queryCellBroadcastSmsActivation(slotId);
        } else {
            return false;
        }
    }

    public static boolean activateCellBroadcastSms(boolean activate, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().activateCellBroadcastSms(activate, slotId);
        } else {
            return false;
        }
    }

    public static boolean setCellBroadcastSmsConfig(SmsBroadcastConfigInfo[] channels,
            SmsBroadcastConfigInfo[] languages, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().setCellBroadcastSmsConfig(channels, languages, slotId);
        } else {
            return false;
        }
    }

    public static SmsBroadcastConfigInfo[] getCellBroadcastSmsConfig(int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().getCellBroadcastSmsConfig(slotId);
        } else {
            return null;
        }
    }
}
