package com.mediatek.email.extension;

import android.content.Context;

import com.android.emailcommon.Configuration;
import com.android.emailcommon.Logging;
import com.mediatek.email.ext.DefaultServerProviderExtension;
import com.mediatek.email.ext.IServerProviderPlugin;
import com.mediatek.email.ext.DefaultSendNotification;
import com.mediatek.email.ext.ISendNotification;
import com.mediatek.pluginmanager.*;

/**
 * Factory for producing each operator extensions
 */
public class OPExtensionFactory {
    private static final String TAG = "OPExtensionFactory";

    private static ISendNotification sSendNotification;
    private static IServerProviderPlugin sProviderExtension;
    private final static String METANAME_SN = "sendnotification";

    /**
     * Clear all plugin objects which instance has already existed.
     * Because the context of plugin objects will updated in some case, such as
     * locale configuration changed,etc.
     */
    public static void resetAllPluginObject(Context context) {
        sSendNotification = null;
        sProviderExtension = null;
        getSendingNotifyExtension(context);
        getProviderExtension(context);
    }

    /**
     * The "Sending Notification" Extension is an Single instance. it would hold the ApplicationContext, and
     * alive within the whole Application.
     * @param context PluginManager use it to retrieve the plug-in object
     * @return the single instance of "Sending Notification" Extension
     */
    public synchronized static ISendNotification getSendingNotifyExtension(Context context) {
        if (Configuration.isTest()) {
            sSendNotification = new DefaultSendNotification();
        } else if (sSendNotification == null) {
            try {
                sSendNotification = (ISendNotification)PluginManager.createPluginObject(
                        context, ISendNotification.class.getName(), METANAME_SN);
                Logging.d(TAG, "use SendNotification plugin");
            } catch (Plugin.ObjectCreationException e) {
                Logging.d(TAG, "get plugin failed, use default");
                sSendNotification = new DefaultSendNotification();
            }
        }
        return sSendNotification;
    }

    /**
     * The Provider Extension is an Single instance. it would hold the ApplicationContext, and
     * alive within the whole Application.
     * @param context PluginManager use it to retrieve the plug-in object
     * @return the single instance of Lunar Extension
     */
    public synchronized static IServerProviderPlugin getProviderExtension(Context context) {
        // if is running test, just return DefaultServerProviderExtension to
        // avoid test fail,
        // because this extension feature will change set up flow of email.
        if (Configuration.isTest()) {
            sProviderExtension = new DefaultServerProviderExtension();
        } else if (sProviderExtension == null) {
            // if is not running test, return extension Plugin object.
            try {
                sProviderExtension = (IServerProviderPlugin) PluginManager.createPluginObject(
                        context, IServerProviderPlugin.class.getName());
                Logging.d(TAG, "use email esp plugin");
            } catch (Plugin.ObjectCreationException e) {
                Logging.d(TAG, "get plugin failed, use default");
                sProviderExtension = new DefaultServerProviderExtension();
            }
        }
        return sProviderExtension;
    }
}
