
package com.mediatek.encapsulation.android.telephony.gemini;

import android.app.ActivityThread;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.os.Bundle;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.mediatek.encapsulation.MmsLog;
import com.android.internal.telephony.ISms;
import com.mediatek.common.telephony.IccSmsStorageStatus;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsMemoryStatus;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.telephony.SmsManagerEx;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import java.util.List;
import java.util.ArrayList;
import android.os.SystemProperties;

/// M: ALPS00510627, SMS Framewrok API refactoring, EncapsulatedGeminiSmsManager -> SmsManagerEx
public class EncapsulatedGeminiSmsManager {

    /** M: MTK ADD */
    //private static final String mNowsmsAddress = "http://172.26.89.66:8800/?PhoneNumber=";
    private static final String mNowsmsAddress = "172.26.89.66";

    public static int copyTextMessageToIccCardGemini(String scAddress, String address,
            List<String> text, int status, long timestamp, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().copyTextMessageToIccCard(scAddress, address, text,
                    status, timestamp, slotId);
        } else {
            return 0;
        }
    }

    /** M: MTK ADD */
    public static EncapsulatedSmsMemoryStatus getSmsSimMemoryStatusGemini(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            IccSmsStorageStatus smsMemoryStatus
                    = SmsManagerEx.getDefault().getIccSmsStorageStatus(simId);
            if (smsMemoryStatus != null) {
                return new EncapsulatedSmsMemoryStatus(smsMemoryStatus);
            } else {
                return null;
            }
        } else {
            /** M: Can not complete for this branch. */
            return null;
        }
    }

    /** M: MTK ADD */
    public static int copyMessageToIccGemini(byte[] smsc, byte[] pdu, int status, int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().copyMessageToIcc(smsc,pdu,status,simId);
        } else {
            /** M: Can not complete for this branch. */
            int result = SmsManager.getDefault().copyMessageToIcc(smsc, pdu, status) ? 0 : -1;
            return result;
        }
    }

    /** M: MTK ADD */
    public static void sendMultipartTextMessageWithExtraParamsGemini(String destAddr,
            String scAddr, ArrayList<String> parts, Bundle extraParams, int slotId,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        String enableNowSMS = SystemProperties.get("net.ENABLE_NOWSMS");
        if (enableNowSMS.equals("true")) {
            /// M:add this for simulate sms sending through nowSms @{
            String smsText = "";
            for (int i = 0; i < parts.size(); i++) {
                smsText += parts.get(i);
            }

            String sender = SystemProperties.get("net.EMULATOR_SENDER_" + slotId);
            MmsLog.d("EncapsulatedGeminiSmsManager", "sendMultipartTextMessageWithExtraParamsGemini slot id = " + slotId);
            MmsLog.d("EncapsulatedGeminiSmsManager", "sendMultipartTextMessageWithExtraParamsGemini sender = " + sender);
            MmsLog.d("EncapsulatedGeminiSmsManager", "sendMultipartTextMessageWithExtraParamsGemini text = " + smsText);
            String smscStr = SystemProperties.get("net.EMULATOR_SMSC_" + slotId);
            MmsLog.d("EncapsulatedGeminiSmsManager", "smsc is " + smscStr);
            if (smscStr == null || smscStr.isEmpty()) {
                smscStr = mNowsmsAddress;
            }
            HttpGet getMethod = new HttpGet("http://" + smscStr + ":8800/?PhoneNumber=" + destAddr + "&Text=" + smsText + "&Sender=" + sender);
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpResponse response = httpClient.execute(getMethod);
            } catch (Exception e) {}
            /// @}
        } else {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                 SmsManagerEx.getDefault().sendMultipartTextMessageWithExtraParams(
                        destAddr, scAddr, parts, extraParams, sentIntents, deliveryIntents, slotId);
            } else {
            }
        }
    }

    /** M: MTK ADD */
    public static void sendMultipartTextMessageWithEncodingTypeGemini(String destAddr,
            String scAddr, ArrayList<String> parts, int encodingType, int slotId,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        String enableNowSMS = SystemProperties.get("net.ENABLE_NOWSMS");
        if (enableNowSMS.equals("true")) {
            /// M:add this for simulate sms sending through nowSms @{
            String smsText = "";
            for (int i = 0; i < parts.size(); i++) {
                smsText += parts.get(i);
            }

            String sender = SystemProperties.get("net.EMULATOR_SENDER_" + slotId);
            MmsLog.d("EncapsulatedGeminiSmsManager", "sendMultipartTextMessageWithEncodingTypeGemini slot id = " + slotId);
            MmsLog.d("EncapsulatedGeminiSmsManager", "sendMultipartTextMessageWithEncodingTypeGemini sender = " + sender);
            MmsLog.d("EncapsulatedGeminiSmsManager", "sendMultipartTextMessageWithEncodingTypeGemini text = " + smsText);

            String smscStr = SystemProperties.get("net.EMULATOR_SMSC_" + slotId);
            MmsLog.d("EncapsulatedGeminiSmsManager", "smsc is " + smscStr);
            if (smscStr == null || smscStr.isEmpty()) {
                smscStr = mNowsmsAddress;
            }
            HttpGet getMethod = new HttpGet("http://" + smscStr + ":8800/?PhoneNumber=" + destAddr + "&Text=" + smsText + "&Sender=" + sender);
            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpResponse response = httpClient.execute(getMethod);
            } catch (Exception e) {}
/// @}
        } else {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                 SmsManagerEx.getDefault().sendMultipartTextMessageWithEncodingType(
                        destAddr, scAddr, parts, encodingType, sentIntents, deliveryIntents, slotId);
            } else {
                if (!isValidParameters(destAddr, parts, sentIntents)) {
                    Log.d(TAG, "invalid parameters for multipart message");
                    return;
                }
                String isms = getSmsServiceName(slotId);
                if (parts.size() > 1) {
                    try {
                        ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
                        if (iccISms != null) {
                            Log.d(TAG, "call ISms.sendMultipartText");
                            iccISms.sendMultipartText(
                                    ActivityThread.currentPackageName(), destAddr, scAddr, parts,
                                    sentIntents, deliveryIntents);
                        }
                    } catch (RemoteException ex) {
                        // ignore it
                    }
                } else {
                    PendingIntent sentIntent = null;
                    PendingIntent deliveryIntent = null;
                    if (sentIntents != null && sentIntents.size() > 0) {
                        sentIntent = sentIntents.get(0);
                    }
                    Log.d(TAG, "get sentIntent: " + sentIntent);
                    if (deliveryIntents != null && deliveryIntents.size() > 0) {
                        deliveryIntent = deliveryIntents.get(0);
                    }
                    Log.d(TAG, "send single message");
                    if (parts != null) {
                        Log.d(TAG, "parts.size = " + parts.size());
                    }
                    String text = (parts == null || parts.size() == 0) ? "" : parts.get(0);
                    Log.d(TAG, "pass encoding type " + encodingType);
                    sendTextMessageWithEncodingType(destAddr, scAddr, text, encodingType,
                            slotId, sentIntent, deliveryIntent);
                }
            }
        }
    }

    public static void setLastIncomingSmsSimId(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            SmsManagerEx.getDefault().setLastIncomingSmsSimId(simId);
        } else {
        }
    }

    public void sendMultipartTextMessage(String destinationAddress, String scAddress,
            ArrayList<String> parts, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, int slotId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            SmsManagerEx.getDefault().sendMultipartTextMessage(
                    destinationAddress, scAddress, parts, sentIntents, deliveryIntents, slotId);
        }
    }

    public static boolean isSmsReady(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return SmsManagerEx.getDefault().isSmsReady(simId);
        } else {
            /** M: Can not complete for this branch. */
            return true;
        }
    }

    private static final String TAG = "EncapsulatedGeminiSmsManager";

    private static String getSmsServiceName(int slotId) {
        if (slotId == EncapsulatedPhone.GEMINI_SIM_1) {
            return "isms";
        } else if (slotId == EncapsulatedPhone.GEMINI_SIM_2) {
            return "isms2";
        } else if (slotId == EncapsulatedPhone.GEMINI_SIM_3) {
            return "isms3";
        } else if (slotId == EncapsulatedPhone.GEMINI_SIM_4) {
            return "isms4";
        } else {
            return null;
        }
    }

    private static boolean isValidParameters(
            String destinationAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents) {
        // impl
        if (parts == null || parts.size() == 0) {
            return true;
        }

        if (!isValidSmsDestinationAddress(destinationAddress)) {
            for (int i = 0; i < sentIntents.size(); i++) {
                PendingIntent sentIntent = sentIntents.get(i);
                if (sentIntent != null) {
                    try {
                        sentIntent.send(SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                    } catch (CanceledException ex) {
                    }
                }
            }

            Log.d(TAG, "Invalid destinationAddress: " + destinationAddress);
            return false;
        }

        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (parts == null || parts.size() < 1) {
            throw new IllegalArgumentException("Invalid message body");
        }
        return true;
    }

    private static boolean isValidSmsDestinationAddress(String da) {
        // impl
        String encodeAddress = PhoneNumberUtils.extractNetworkPortion(da);
        if (encodeAddress == null) {
            return true;
        }

        int spaceCount = 0;
        for (int i = 0; i < da.length(); ++i) {
            if (da.charAt(i) == ' ' || da.charAt(i) == '-') {
                spaceCount++;
            }
        }

        return encodeAddress.length() == (da.length() - spaceCount);
    }

    public static void sendTextMessageWithEncodingType(
            String destAddr,
            String scAddr,
            String text,
            int encodingType,
            int slotId,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        // impl
        Log.d(TAG, "call sendTextMessageWithEncodingType, encoding = " + encodingType);

        if (!isValidParameters(destAddr, text, sentIntent)) {
            Log.d(TAG, "the parameters are invalid");
            return;
        }

        String isms = getSmsServiceName(slotId);
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService(isms));
            if (iccISms != null) {
                Log.d(TAG, "call ISms interface to send text message");
                iccISms.sendText(ActivityThread.currentPackageName(), destAddr,
                    scAddr, text, sentIntent, deliveryIntent);
            } else {
                Log.d(TAG, "iccISms is null");
            }
        } catch (RemoteException ex) {
            // ignore it
            Log.d(TAG, "fail to get ISms");
        }
    }

    private static boolean isValidParameters(
            String destinationAddress, String text, PendingIntent sentIntent) {
        // impl
        ArrayList<PendingIntent> sentIntents =
                new ArrayList<PendingIntent>();
        ArrayList<String> parts =
                new ArrayList<String>();

        sentIntents.add(sentIntent);
        parts.add(text);

        // if (TextUtils.isEmpty(text)) {
        // throw new IllegalArgumentException("Invalid message body");
        // }

        return isValidParameters(destinationAddress, parts, sentIntents);
    }
}