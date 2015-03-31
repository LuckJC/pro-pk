package com.android.mms;

import android.content.Context;
import android.util.AndroidException;

import com.mediatek.encapsulation.MmsLog;
import com.mediatek.mms.ext.IMmsCompose;
import com.mediatek.mms.ext.MmsComposeImpl;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
import com.mediatek.mms.ext.DefaultAppGuideExt;
import com.mediatek.mms.ext.IAppGuideExt;
import com.mediatek.mms.ext.IMmsAskIfDownload;
import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.IMmsCancelDownload;
import com.mediatek.mms.ext.IMmsDialogNotify;
import com.mediatek.mms.ext.IMmsFailedNotify;
import com.mediatek.mms.ext.IMmsSettings;
import com.mediatek.mms.ext.IMmsTextSizeAdjust;
import com.mediatek.mms.ext.MmsSettingsImpl;
import com.mediatek.mms.ext.IMmsTextSizeAdjustHost;
import com.mediatek.mms.ext.IMmsUtils;
import com.mediatek.mms.ext.ISmsReceiver;
import com.mediatek.mms.ext.IStringReplacement;
import com.mediatek.mms.ext.IUnreadMessageNumber;
import com.mediatek.mms.ext.MmsAskIfDownloadImpl;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;
import com.mediatek.mms.ext.MmsCancelDownloadImpl;
import com.mediatek.mms.ext.MmsDialogNotifyImpl;
import com.mediatek.mms.ext.MmsFailedNotifyImpl;
import com.mediatek.mms.ext.MmsTextSizeAdjustImpl;
import com.mediatek.mms.ext.MmsUtilsImpl;
import com.mediatek.mms.ext.SmsReceiverImpl;
import com.mediatek.mms.ext.StringReplacementImpl;
import com.mediatek.mms.ext.UnreadMessageNumberImpl;

///M: add for Mms transaction plugin
import com.mediatek.mms.ext.IMmsTransaction;
import com.mediatek.mms.ext.MmsTransactionImpl;

import com.mediatek.mms.ext.IMmsSlideshowEditor;
import com.mediatek.mms.ext.MmsSlideshowEditorImpl;

///M: add for SMS\MMS Conversation Split plugin @{
import com.mediatek.mms.ext.IMmsSmsMmsConversationSplit;
import com.mediatek.mms.ext.MmsSmsMmsConversationSplitImpl;
///@}
import com.mediatek.mms.ext.IMmsConversation;
import com.mediatek.mms.ext.MmsConversationImpl;

public class MmsPluginManager {
    
    private static String TAG = "MmsPluginManager";

    public static final int MMS_PLUGIN_TYPE_DIALOG_NOTIFY = 0X0001;
    public static final int MMS_PLUGIN_TYPE_TEXT_SIZE_ADJUST = 0X0002;
    // M: fix bug ALPS00352897
    public static final int MMS_PLUGIN_TYPE_SMS_RECEIVER = 0X0003;
    
    public static final int MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE = 0X0005;
    ///M: add for Mms transaction plugin
    public static final int MMS_PLUGIN_TYPE_MMS_TRANSACTION = 0X0007;


    public static final int MMS_PLUGIN_TYPE_MMS_COMPOSE = 0X0008;

    public static final int MMS_PLUGIN_TYPE_MMS_SETTINGS = 0X0009;

    ///M: add for OP09 @{
    public static final int MMS_PLUGIN_TYPE_FAILED_NOTIFY = 0X000b;
    public static final int MMS_PLUGIN_TYPE_CANCEL_DOWNLOAD = 0X0000c;
    public static final int MMS_PLUGIN_TYPE_MESSAGE_UTILS = 0X000d;
    public static final int MMS_PLUGIN_TYPE_ASK_IF_DOWNLOAD = 0X0000e;
    public static final int MMS_PLUGIN_TYPE_STRING_REPLACEMENT = 0X000f;
    public static final int MMS_PLUGIN_TYPE_UNREAD_MESSAGE = 0X0010;
    /// @}

    ///M: add for SlideshowEditor plugin
    public static final int MMS_PLUGIN_TYPE_MMS_SLIDESHOW_EDITOR = 0X0011;

    ///M: add for SMS\MMS Conversation Split plugin @{
    public static final int MMS_PLUGIN_TYPE_SMS_MMS_CONVERSATION_SPLIT = 0X0012;
    ///@}
    public static final int MMS_PLUGIN_TYPE_MMS_CONV = 0X0013;

    private static IMmsTextSizeAdjust mMmsTextSizeAdjustPlugin = null;
    private static IMmsDialogNotify mMmsDialogNotifyPlugin = null;
    // M: fix bug ALPS00352897
    private static ISmsReceiver mSmsReceiverPlugin = null;
    private static IAppGuideExt mAppGuideExt = null;
    public static final int MMS_PLUGIN_TYPE_APPLICATION_GUIDE = 0X0004;
    private static IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = null;
    ///M: add for Mms transaction plugin
    private static IMmsTransaction mMmsTransactionPlugin = null;
    private static IMmsCompose mMmsComposePlugin = null;

    private static IMmsSettings mMmsSettingsPlugin = null;

    /// M: New member for OP09 plug-in @{
    private static IMmsFailedNotify sMmsFailedNotifyPlugin = null;
    private static IMmsCancelDownload sCancelDownloadPlugin = null;
    private static IMmsAskIfDownload sAskIfDownloadPlugin = null;
    private static IMmsUtils sMmsUtilsPlugin = null;
    private static IStringReplacement sStringReplacementPlugin = null;
    private static IUnreadMessageNumber sUnreadMessagePlugin = null;
    /// @}

    ///M: add for SlideshowEditor plugin
    private static IMmsSlideshowEditor mMmsSlideshowEditorPlugin = null;

    ///M: add for SMS\MMS Conversation Split plugin @{
    private static IMmsSmsMmsConversationSplit mSmsMmsConversationSplitPlugin = null;
    ///@}
    private static IMmsConversation mMmsConversationPlugin = null;

    public static void initPlugins(Context context){

        //Dialog Notify
        try {
            mMmsDialogNotifyPlugin = (IMmsDialogNotify)EncapsulatedPluginManager.createPluginObject(context, IMmsDialogNotify.class.getName());
            MmsLog.d(TAG, "operator mMmsDialogNotifyPlugin = " + mMmsDialogNotifyPlugin);
        } catch (AndroidException e) {
            mMmsDialogNotifyPlugin = new MmsDialogNotifyImpl(context);
            MmsLog.d(TAG, "default mMmsDialogNotifyPlugin = " + mMmsDialogNotifyPlugin);
        }
        
        //TextSizeAdjust plugin
        try{
            mMmsTextSizeAdjustPlugin = (IMmsTextSizeAdjust)EncapsulatedPluginManager.createPluginObject(context, IMmsTextSizeAdjust.class.getName());
            MmsLog.d(TAG, "operator mMmsTextSizeAdjustPlugin = " + mMmsTextSizeAdjustPlugin);
        }catch(AndroidException e) {
            mMmsTextSizeAdjustPlugin = new MmsTextSizeAdjustImpl(context);
            MmsLog.d(TAG, "default mMmsTextSizeAdjustPlugin = " + mMmsTextSizeAdjustPlugin);
        }

        // M: fix bug ALPS00352897
        //SmsReceiver plugin
        try{
            mSmsReceiverPlugin = (ISmsReceiver)EncapsulatedPluginManager.createPluginObject(context, ISmsReceiver.class.getName());
            MmsLog.d(TAG, "operator mSmsReceiverPlugin = " + mSmsReceiverPlugin);
        }catch(AndroidException e) {
            mSmsReceiverPlugin = new SmsReceiverImpl(context);
            MmsLog.d(TAG, "default mSmsReceiverPlugin = " + mSmsReceiverPlugin);
        }
        /// M: add for application guide. @{
        try {
            mAppGuideExt = (IAppGuideExt)EncapsulatedPluginManager.createPluginObject(context,
                    IAppGuideExt.class.getName());
        } catch (AndroidException e) {
            mAppGuideExt = new DefaultAppGuideExt();
            MmsLog.d(TAG,"default mAppGuideExt = " + mAppGuideExt);
        }
        /// @}
        //Mms attachment enhance plugin
        try {
            mMmsAttachmentEnhancePlugin =
            (IMmsAttachmentEnhance)EncapsulatedPluginManager.createPluginObject(context, IMmsAttachmentEnhance.class.getName());
            MmsLog.d(TAG, "operator mMmsAttachmentEnhancePlugin = " + mMmsAttachmentEnhancePlugin);
        } catch (AndroidException e) {
            mMmsAttachmentEnhancePlugin = new MmsAttachmentEnhanceImpl(context);
            MmsLog.d(TAG, "default mMmsAttachmentEnhancePlugin = " + mMmsAttachmentEnhancePlugin);
        }
        ///M: add for Mms transaction plugin
        try{
            mMmsTransactionPlugin = (IMmsTransaction)EncapsulatedPluginManager.createPluginObject(context, IMmsTransaction.class.getName());
            MmsLog.d(TAG, "operator mMmsTransactionPlugin = " + mMmsTransactionPlugin);
        } catch(AndroidException e) {
            mMmsTransactionPlugin = new MmsTransactionImpl(context);
            MmsLog.d(TAG, "default mMmsTransactionPlugin = " + mMmsTransactionPlugin);
        }
        ///@}
        /// M: add for Mms Compose plugin
        try {
            mMmsComposePlugin = (IMmsCompose)EncapsulatedPluginManager.createPluginObject(context,
                                 IMmsCompose.class.getName());
            MmsLog.d(TAG, "operator mMmsComposePlugin = " + mMmsComposePlugin);
        } catch (AndroidException e) {
            mMmsComposePlugin = new MmsComposeImpl(context);
        }
        MmsLog.d(TAG, "default mMmsComposePlugin = " + mMmsComposePlugin);
        ///M: add for regional phone plugin @{
        try{
            mMmsSettingsPlugin = (IMmsSettings)EncapsulatedPluginManager.createPluginObject(context, IMmsSettings.class.getName());
            MmsLog.d(TAG, "operator mMmsSettingsPlugin = " + mMmsSettingsPlugin);
        } catch(AndroidException e) {
            mMmsSettingsPlugin = new MmsSettingsImpl(context);
            MmsLog.d(TAG, "default mMmsSettingsPlugin = " + mMmsSettingsPlugin);
        }
        ///@}

        /// M: add for OP09 feature @{
        try {
            sMmsFailedNotifyPlugin = (IMmsFailedNotify) EncapsulatedPluginManager.createPluginObject(context,
                    IMmsFailedNotify.class.getName());
            MmsLog.d(TAG, "operator sMmsFailedNotifyPlugin = " + sMmsFailedNotifyPlugin);
        } catch (AndroidException e) {
            sMmsFailedNotifyPlugin = new MmsFailedNotifyImpl(context);
            MmsLog.d(TAG, "default sMmsFailedNotifyPlugin = " + sMmsFailedNotifyPlugin);
        }

        try {
            sCancelDownloadPlugin = (IMmsCancelDownload)EncapsulatedPluginManager.createPluginObject( context,
                    IMmsCancelDownload.class.getName());
            MmsLog.d(TAG, "operator sCancelDownloadPlugin = " + sCancelDownloadPlugin);
        } catch (AndroidException e) {
            sCancelDownloadPlugin = new MmsCancelDownloadImpl(context);
            MmsLog.d(TAG, "default sCancelDownloadPlugin = " + sCancelDownloadPlugin);
        }

        try {
            sAskIfDownloadPlugin = (IMmsAskIfDownload)EncapsulatedPluginManager.createPluginObject( context,
                    IMmsAskIfDownload.class.getName());
            MmsLog.d(TAG, "operator sAskIfDownloadPlugin = " + sAskIfDownloadPlugin);
        } catch (AndroidException e) {
            sAskIfDownloadPlugin = new MmsAskIfDownloadImpl(context);
            MmsLog.d(TAG, "default sAskIfDownloadPlugin = " + sAskIfDownloadPlugin);
        }

        try {
            sMmsUtilsPlugin = (IMmsUtils)EncapsulatedPluginManager.createPluginObject( context,
                IMmsUtils.class.getName());
            MmsLog.d(TAG, "operator sMmsUtilsPlugin = " + sMmsUtilsPlugin);
        } catch (AndroidException e) {
            sMmsUtilsPlugin = new MmsUtilsImpl(context);
            MmsLog.d(TAG, "default sMmsUtilsPlugin = " + sMmsUtilsPlugin);
        }

        try {
            sStringReplacementPlugin = (IStringReplacement)EncapsulatedPluginManager.createPluginObject( context,
                    IStringReplacement.class.getName());
            MmsLog.d(TAG, "operator sStringReplacementPlugin = " + sStringReplacementPlugin);
        } catch (AndroidException e) {
            sStringReplacementPlugin = new StringReplacementImpl(context);
            MmsLog.d(TAG, "default sStringReplacementPlugin = " + sStringReplacementPlugin);
        }

        try {
            sUnreadMessagePlugin = (IUnreadMessageNumber)EncapsulatedPluginManager.createPluginObject(
                    context.getApplicationContext(), IUnreadMessageNumber.class.getName());
            MmsLog.d(TAG, "operator sUnreadMessagePlugin = " + sUnreadMessagePlugin);
        } catch (AndroidException e) {
            sUnreadMessagePlugin = new UnreadMessageNumberImpl(context.getApplicationContext());
            MmsLog.d(TAG, "default sUnreadMessagePlugin = " + sUnreadMessagePlugin);
        }
        ///@}

        ///M: add SlideshowEditor plugin
        try{
            mMmsSlideshowEditorPlugin =
                (IMmsSlideshowEditor)EncapsulatedPluginManager.createPluginObject(context, IMmsSlideshowEditor.class.getName());
            MmsLog.d(TAG, "operator mMmsSlideshowEditorPlugin = " + mMmsSlideshowEditorPlugin);
        } catch(AndroidException e) {
            mMmsSlideshowEditorPlugin = new MmsSlideshowEditorImpl(context);
            MmsLog.d(TAG, "default mMmsSlideshowEditorPlugin = " + mMmsSlideshowEditorPlugin);
        }

        ///M: add for SMS\MMS Conversation Split plugin @{
        try{
            mSmsMmsConversationSplitPlugin =
                (IMmsSmsMmsConversationSplit)EncapsulatedPluginManager.createPluginObject(context, IMmsSmsMmsConversationSplit.class.getName());
            MmsLog.d(TAG, "operator mSmsMmsConversationSplitPlugin = " + mSmsMmsConversationSplitPlugin);
        } catch(AndroidException e) {
            mSmsMmsConversationSplitPlugin = new MmsSmsMmsConversationSplitImpl(context);
            MmsLog.d(TAG, "default mSmsMmsConversationSplitPlugin = " + mSmsMmsConversationSplitPlugin);
        }
        ///@}
        try {
            mMmsConversationPlugin =
                    (IMmsConversation)EncapsulatedPluginManager.createPluginObject(context, IMmsConversation.class.getName());
            MmsLog.d(TAG, "operator mMmsConversationPlugin = " + mMmsConversationPlugin);
        } catch (AndroidException e) {
            mMmsConversationPlugin = new MmsConversationImpl(context);
            MmsLog.d(TAG, "default mMmsConversationPlugin = " + mMmsConversationPlugin);
        }
    }


    public static Object getMmsPluginObject(int type){
        Object obj = null;
        MmsLog.d(TAG,"getMmsPlugin, type = " + type);
        switch(type){
            
            case MMS_PLUGIN_TYPE_DIALOG_NOTIFY:
                obj = mMmsDialogNotifyPlugin;
                break;

            case MMS_PLUGIN_TYPE_TEXT_SIZE_ADJUST:
                obj = mMmsTextSizeAdjustPlugin;
                break;

            // M: fix bug ALPS00352897
            case MMS_PLUGIN_TYPE_SMS_RECEIVER:
                obj = mSmsReceiverPlugin;
                break;
            case MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE:
                obj = mMmsAttachmentEnhancePlugin;
                break;

            case MMS_PLUGIN_TYPE_APPLICATION_GUIDE:
                obj = mAppGuideExt;
                break;

            ///M: add for Mms transaction plugin
            case MMS_PLUGIN_TYPE_MMS_TRANSACTION:
                obj = mMmsTransactionPlugin;
                break;
            ///@}
            case MMS_PLUGIN_TYPE_MMS_COMPOSE:
                obj = mMmsComposePlugin;
                break;
            case MMS_PLUGIN_TYPE_MMS_SETTINGS:
                obj = mMmsSettingsPlugin;
                break;

             /// M: add for OP09 feature. @{
             case MMS_PLUGIN_TYPE_FAILED_NOTIFY:
                 obj = sMmsFailedNotifyPlugin;
                 break;

             case MMS_PLUGIN_TYPE_CANCEL_DOWNLOAD:
                 obj = sCancelDownloadPlugin;
                 break;

             case MMS_PLUGIN_TYPE_MESSAGE_UTILS:
                 obj = sMmsUtilsPlugin;
                 break;

             case MMS_PLUGIN_TYPE_ASK_IF_DOWNLOAD:
                 obj = sAskIfDownloadPlugin;
                 break;

             case MMS_PLUGIN_TYPE_STRING_REPLACEMENT:
                 obj = sStringReplacementPlugin;
                 break;

             case MMS_PLUGIN_TYPE_UNREAD_MESSAGE:
                 obj = sUnreadMessagePlugin;
                 break;
            /// @}

            ///M: add for SlideshowEditor plugin @{
            case MMS_PLUGIN_TYPE_MMS_SLIDESHOW_EDITOR:
                obj = mMmsSlideshowEditorPlugin;
                break;
            ///@}

            ///M: add for SMS MMS conversation Split plugin @{
            case MMS_PLUGIN_TYPE_SMS_MMS_CONVERSATION_SPLIT:
                obj = mSmsMmsConversationSplitPlugin;
                break;
            ///@}

            ///M: add for MMS conversation plugin @{
            case MMS_PLUGIN_TYPE_MMS_CONV:
                obj = mMmsConversationPlugin;
                break;
            ///@}
            default:
                MmsLog.e(TAG, "getMmsPlugin, type = " + type + " don't exist");
                break;
        }
        return obj;
            
    }
}
