package com.mediatek.email.extension;

import android.content.Context;
import android.util.Log;

import com.mediatek.email.ext.ISendNotification;

/**
 * Proxy responsible for showing/cancelling/suspending the sending notification
 */
public class SendNotificationProxy {
    private static final SendNotificationProxy sSendNotificationProxy = new SendNotificationProxy();
    private static Context sContext;

    public static SendNotificationProxy getInstance(Context context) {
        sContext = context;
        return sSendNotificationProxy;
    }

    public void showSendingNotification(long accountId, int eventType,
            int messageCount) {
        ISendNotification sendNotifer = OPExtensionFactory.getSendingNotifyExtension(sContext);
        if (sendNotifer != null) {
            sendNotifer.showSendingNotification(sContext, accountId, eventType, messageCount);
        }
    }

    public void suspendSendFailedNotification(long accountId) {
        ISendNotification sendNotifer = OPExtensionFactory.getSendingNotifyExtension(sContext);
        if (sendNotifer != null) {
            sendNotifer.suspendSendFailedNotification(accountId);
        }
    }

    public void cancelSendingNotification() {
        ISendNotification sendNotifer = OPExtensionFactory.getSendingNotifyExtension(sContext);
        if (sendNotifer != null) {
            sendNotifer.cancelSendingNotification();
        }
    }
}
