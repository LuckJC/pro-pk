package com.mediatek.vcs;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.content.ContentResolver;

import com.android.contacts.activities.ActionBarAdapter;
import com.android.contacts.activities.ActionBarAdapter.TabState;
import com.mediatek.common.voicecommand.IVoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandManagerService;
import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.ext.ContactAccountExtension.OnGuideFinishListener;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.contacts.util.VCSUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * The Voice Search Manager is used to start or stop voice search listener,
 * which will respond to user request. In a addition, it also query
 * corresponding contacts info for The VCS list UI by voice searched results.
 */
public class VoiceSearchManager {

    public interface Listener {
        void onRegisterResult(boolean success);

        void onCommandSericeConnected();

        // to query contacts info by sync call.
        void onQueryContactsInfo(ArrayList<String> nameList);

        // trigger voice search dialog
        void onTrigVoiceSearchDlg();

        // on contacts found in voice bank
        void onNoContactFoundInVoiceBank();

        void onCommandSericeStop();

        void onCommandSericeStart();
    }

    private static final String TAG = "VoiceSearchManger";

    private Activity mContext;
    private Listener mListener;
    private String mNotificationMessage;

    private boolean mIsRegistered = false;
    private boolean mIsReconEnable = false;
    private ActionBarAdapter mActionBarAdapter;
    private IVoiceCommandListener mCallback;
    public IVoiceCommandManagerService mVCmdMgrService;

    // variables
    private String[] mReturnNameStringArray;
    // TODO:used to avoid multi call show list popup window, depend on name list
    // factually.
    // private static int mVoiceCommandToken = 0;
    // private static int mReturnVoiceToken;

    private static final ArrayList<String> mAudioNameList = new ArrayList<String>();

    // MSG class
    private static final int MSG_UPDATE_INTENSITY = 0;
    private static final int MSG_SEARCH_NOTIFY = 1;
    private static final int MSG_SERVICE_ERROR = 2;
    private static final int MSG_SPEECH_DETECTED = 3;

    public VoiceSearchManager(Activity context, Listener listener) {
        mContext = context;
        mListener = listener;
        LogUtils.i(TAG, "[VoiceSearchManager] [vcs]");
        mCallback = new IVoiceCommandListener.Stub() {
            @Override
            public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) throws RemoteException {
                LogUtils.i(TAG, "[onVoiceCommandNotified] [vcs]mainAction = " + mainAction + ", subAction=" + subAction);
                switch (mainAction) {
                case VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS:
                    LogUtils.i(TAG, "[onVoiceCommandNotified] ACTION_MAIN_VOICE_CONTACTS");
                    handleContactsUIVoiceCommand(subAction, extraData);
                    break;
                default:
                    LogUtils.i(TAG, "[onVoiceCommandNotified] running in default");
                    break;
                }
            }
        };

        LogUtils.i(TAG, "[VoiceSearchManager] [vcs] bindVoiceService");
        bindVoiceService();
    }

    private void bindVoiceService() {
        LogUtils.i(TAG, "[bindVoiceService] [vcs]");
        Intent mVoiceServiceIntent = new Intent();
        mVoiceServiceIntent.setAction(VoiceCommandListener.VOICE_SERVICE_ACTION);
        mVoiceServiceIntent.addCategory(VoiceCommandListener.VOICE_SERVICE_CATEGORY);
        mContext.bindService(mVoiceServiceIntent, mVoiceSerConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mVoiceSerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtils.i(TAG, "[onServiceConnected] [vcs]");
            mVCmdMgrService = IVoiceCommandManagerService.Stub.asInterface(service);

            if (mListener == null) {
                LogUtils.i(TAG, "[onServiceConnected] [vcs] mListener is null. need to unbind voice service");
                unbindVoiceService();
                return;
            }

            mListener.onCommandSericeConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtils.i(TAG, "[onServiceDisconnected] voice Service DisConnected.name" + name);
            mIsRegistered = false;
            mVCmdMgrService = null;
            if (mListener != null) {
                mListener.onCommandSericeStop();
            }
        }
    };

    public void startVoiceSearch(int orientation) {
        if(mContext == null){
            LogUtils.i(TAG, "[startVoiceSearch] [vcs] mContext is null,may be the VoiceSearchManger has been destory.");
            return;
        }
        startVoiceCommand(orientation);
    }

    public void stopVoiceSearch() {
        stopVoiceCommand();
    }

    public void destroyVoiceSearch() {
        LogUtils.i(TAG, "[destroyVoiceSearch] [vcs] unbindVoiceService");
        unbindVoiceService();
        clear();
    }

    /**
     * Used to set screen orientation to Native SWIP Layer.
     * 
     * @param orientation
     */
    public void onSetScreenOrientation(int orientation) {
        if (null == mContext) {
            LogUtils.i(TAG, "[onSetScreenDirection] mContext is null, just return");
            return;
        }

        Bundle extraData = new Bundle();
        extraData.putInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, orientation);
        LogUtils.i(
                TAG,
                "[onSetScreenDirection] [vcs] screen direction:"
                        + extraData.getInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, 1));

        final String pkgName = mContext.getPackageName();
        sendVoiceCommand(pkgName, VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS,
                VoiceCommandListener.ACTION_VOICE_CONTACTS_ORIENTATION, extraData);
    }

    /**
     * Notify the native to study user's selection.
     * 
     * @param extraData
     *            the selected contact
     */
    public void onContactLearning(String contactName) {
        if (null == mContext) {
            LogUtils.i(TAG, "[onContactLearning] mContext is null, just return");
            return;
        }

        Bundle extraData = new Bundle();
        extraData.putString(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, contactName);
        LogUtils.i(
                TAG,
                "[onContactLearning] [vcs] study name:"
                        + extraData.getString(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, "contactAAA"));

        final String pkgName = mContext.getPackageName();
        sendVoiceCommand(pkgName, VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS,
                VoiceCommandListener.ACTION_VOICE_CONTACTS_SELECTED, extraData);
    }

    private void startVoiceCommand(int orientation) {
        if(!isRegistered()){
            LogUtils.i(TAG, "[startVoiceCommand] [vcs] start");
            Bundle extraData = new Bundle();
            extraData.putInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, orientation);

            final String pkgName = mContext.getPackageName();
            registerVoiceCommand(pkgName);
            sendVoiceCommand(pkgName, VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS,
                    VoiceCommandListener.ACTION_VOICE_CONTACTS_START, extraData);
            return;
        }
        if(!mIsReconEnable){
            LogUtils.i(TAG, "[startVoiceCommand] [vcs] setVcsReconEnable");
            setVcsReconEnable(true);
        }
    }

    private void stopVoiceCommand() {
        if(mVCmdMgrService == null){
            LogUtils.i(TAG, "[stopVoiceCommand] mVCmdMgrService is null");
            return;
        }
        Activity activity = (Activity) mContext;
        if(activity.isResumed() && !mIsRegistered){
            return;
        }
        if (activity.isResumed()) {
            setVcsReconEnable(false);
            return;
        }
        stopVoiceCommand(true);
    }

    public void stopVoiceCommand(boolean stopService) {
        if (stopService) {
            LogUtils.i(TAG, "[stopVoiceCommand] [vcs] stop");
            final String pkgName = mContext.getPackageName();
            // really stop the command service
            sendVoiceCommand(pkgName, VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS,
                    VoiceCommandListener.ACTION_VOICE_CONTACTS_STOP, null);
            unRegisterVoiceCommand(pkgName);
            mIsReconEnable = false;
            if (mListener != null) {
                mListener.onCommandSericeStop();
            }
        } else {
            stopVoiceCommand();
        }
    }
    
    private void setVcsReconEnable(boolean enable) {
        final String pkgName = mContext.getPackageName();
        int subAction = enable ? VoiceCommandListener.ACTION_VOICE_CONTACTS_RECOGNITION_ENABLE
                : VoiceCommandListener.ACTION_VOICE_CONTACTS_RECOGNITION_DISABLE;

        sendVoiceCommand(pkgName, VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS, subAction, null);
    }

    private boolean isRegistered() {
        return mIsRegistered;
    }

    private void sendVoiceCommand(String pkgName, int mainAction, int subAction, Bundle extraData) {
        if (mIsRegistered && mVCmdMgrService != null) {
            try {
                LogUtils.i(TAG, "[sendVoiceCommand] [vcs] pkgName = " + pkgName + " mainAction = " + mainAction
                        + " subAction = " + subAction + " extraData = " + extraData);
                int errorid = mVCmdMgrService.sendCommand(pkgName, mainAction, subAction, extraData);
                if (errorid != VoiceCommandListener.VOICE_NO_ERROR) {
                    LogUtils.i(TAG, "[sendVoiceCommand] failure");
                } else {
                    LogUtils.i(TAG, "[sendVoiceCommand] [vcs] success");
                }
            } catch (RemoteException e) {
                mIsRegistered = false;
                mVCmdMgrService = null;
                LogUtils.i(TAG, "[sendVoiceCommand] RemoteException = " + e.getMessage());
            }
        } else {
            LogUtils.i(TAG, "[sendVoiceCommand] [vcs] App has not register listener can not send command");
        }
    }

    private void registerVoiceCommand(String pkgName) {
        if (!mIsRegistered && mVCmdMgrService != null) {
            try {
                int errorid = mVCmdMgrService.registerListener(pkgName, mCallback);
                LogUtils.i(TAG, "[registerVoiceCommand] [vcs] pkgName = " + pkgName + ",errorid = " + errorid);
                if (errorid == VoiceCommandListener.VOICE_NO_ERROR) {
                    mIsRegistered = true;
                } else {
                    LogUtils.i(TAG, "[registerVoiceCommand] [vcs] Register voice Listener failure ");
                }
            } catch (RemoteException e) {
                // TODO: handle exception
                mIsRegistered = false;
                mVCmdMgrService = null;
                LogUtils.i(TAG, "[registerVoiceCommand] Register voice Listener RemoteException = " + e.getMessage());
            }
            mListener.onRegisterResult(mIsRegistered);
        } else {
            LogUtils.i(TAG, "[registerVoiceCommand] [vcs] App has register voice listener success");
        }
        LogUtils.i(TAG, "[registerVoiceCommand] [vcs] Register voice listener end!");
    }

    private void unRegisterVoiceCommand(String pkgName) {
        if (mVCmdMgrService != null) {
            try {
                int errorid = mVCmdMgrService.unregisterListener(pkgName, mCallback);
                LogUtils.i(TAG, "[unRegisterVoiceCommand] [vcs] Unregister voice listener, errorid = " + errorid);
                if (errorid == VoiceCommandListener.VOICE_NO_ERROR) {
                    mIsRegistered = false;
                    // There may exist unexecuted messages when unRegister, so
                    // should remove those messages after unRegister
                    // successfully.
                    // TODO: can not remove those delayed Msgs
                    mHandler.removeMessages(MSG_SEARCH_NOTIFY);
                    mHandler.removeMessages(MSG_UPDATE_INTENSITY);
                    // / @}
                }
            } catch (RemoteException e) {
                // TODO: handle exception
                mIsRegistered = false;
                mVCmdMgrService = null;
                LogUtils.i(TAG,
                        "[unRegisterVoiceCommand] [vcs] Unregister error in handler RemoteException = " + e.getMessage());
            }
        }
        LogUtils.i(TAG, "[unRegisterVoiceCommand] [vcs] UnRegister voice listener end!");
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!mIsRegistered) {
                LogUtils.w(TAG, "[handleMessage] ignore the msg,not registered. msg.what:" + msg.what);
                return;
            }
            if (mListener == null) {
                LogUtils.i(TAG, "[handleMessage] [vcs] mListener is null.");
                return;
            }

            LogUtils.i(TAG, "[handleMessage] [vcs] msg.what = " + msg.what);
            switch (msg.what) {
            case MSG_UPDATE_INTENSITY:
                int voiceIntensity = msg.arg1;
                LogUtils.i(TAG, "[handleMessage] [vcs] intensity:" + voiceIntensity);
                break;
            case MSG_SEARCH_NOTIFY:
                // TODO: clear duplicated results.@{
                // LogUtils.i(TAG, "[handleMessage] [vcs] mVoiceCommandToken:" +
                // mVoiceCommandToken
                // +"mReturnVoiceToke:"+mReturnVoiceToken);
                // if (mReturnVoiceToken != mVoiceCommandToken) {
                // break;
                // }
                // @}
                String[] nameArray = (String[]) msg.obj;
                // add to avoid empty result
                if (nameArray == null || nameArray.length == 0) {
                    LogUtils.i(TAG, "[handleMessage] [vcs] nameArray is empty,nameArray:" + nameArray);
                    nameArray = new String[0];
                }
                // clear it before use
                mAudioNameList.clear();
                // add return results
                for (String nameStr : nameArray) {
                    mAudioNameList.add(nameStr);
                }
                mListener.onQueryContactsInfo(mAudioNameList);
                break;
            case MSG_SERVICE_ERROR:
                break;
            case MSG_SPEECH_DETECTED:
                    mListener.onTrigVoiceSearchDlg();
                break;
            default:
                LogUtils.i(TAG, "[handleMessage] [vcs] running in default");
                break;
            }
        }
    };

    /**
     * Process all Voice messages from Native
     * 
     * @param subAction
     * @param extraData
     */
    private void handleContactsUIVoiceCommand(int subAction, Bundle extraData) {
        if (VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS != extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT)) {
            LogUtils.w(TAG, "[handleContactsUIVoiceCommand] subAction:" + subAction + " failed..");
            return;
        }
        if(mContext == null){
            LogUtils.i(TAG, "[handleContactsUIVoiceCommand] [vcs] mContext is null,may be the VoiceSearchManger has been destory.");
            return;
        }
        LogUtils.i(TAG, "[handleContactsUIVoiceCommand] [vcs] subAction = " + subAction + ", extraData = " + extraData);
        switch (subAction) {
        case VoiceCommandListener.ACTION_VOICE_CONTACTS_RECOGNITION_ENABLE:
            mIsReconEnable = true;
            if (mListener != null) {
                mListener.onCommandSericeStart();
            }
            break;
        case VoiceCommandListener.ACTION_VOICE_CONTACTS_START:
            LogUtils.i(TAG,"[vcs][performance],ACTION_VOICE_CONTACTS_START,time:"+System.currentTimeMillis());
            setVcsReconEnable(true);
            break;

        case VoiceCommandListener.ACTION_VOICE_CONTACTS_RECOGNITION_DISABLE:
            //not break,call onCommandSericeStop same as ACTION_VOICE_CONTACTS_STOP
        case VoiceCommandListener.ACTION_VOICE_CONTACTS_STOP:
            mIsReconEnable = false;
            if (mListener != null) {
                mListener.onCommandSericeStop();
            }
            break;

        // /M: study user's choice section
        case VoiceCommandListener.ACTION_VOICE_CONTACTS_SELECTED:
            if (VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS != extraData
                    .getInt(VoiceCommandListener.ACTION_EXTRA_RESULT)) {
                LogUtils.i(TAG,
                        "[handleContactsUIVoiceCommand] [vcs] ACTION_VOICE_CONTACTS_SELECTED message's extra data is not sent SUCCESS");
                // TODO:may add some error process codes here!
                break;
            }
            break;

        // /M: Voice search result process here!
        case VoiceCommandListener.ACTION_VOICE_CONTACTS_NOTIFY:
            LogUtils.i(TAG,"[vcs][performance],ACTION_VOICE_CONTACTS_NOTIFY,time:"+System.currentTimeMillis());
            //
            if (VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS != extraData
                    .getInt(VoiceCommandListener.ACTION_EXTRA_RESULT)) {
                LogUtils.i(TAG,
                        "[handleContactsUIVoiceCommand] ACTION_VOICE_CONTACTS_NOTIFY message's extra data is not SUCCESS");
                // TODO:may add some error process codes here!
                // String errorMsg =
                // extraData.getString(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1);
                String errorMsg = "error result";
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SERVICE_ERROR, errorMsg));
                break;
            }
            // search result process seciton
            mReturnNameStringArray = extraData.getStringArray(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
            if (mReturnNameStringArray != null) {
                LogUtils.i(TAG, "[handleContactsUIVoiceCommand] [vcs] mReturnNameStringArray.size:"
                        + mReturnNameStringArray.length);
            }
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SEARCH_NOTIFY, mReturnNameStringArray));
            // TODO:back transmit the token
            // mReturnVoiceToken =
            // extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
            // LogUtils.i(TAG,
            // "[handleContactsUIVoiceCommand] [vcs] ACTION_VOICE_CONTACTS_START mReturnVoiceToke:"+mReturnVoiceToken);
            break;

        // /M: Voice volume update section
        case VoiceCommandListener.ACTION_VOICE_CONTACTS_INTENSITY:
            int intensity = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
            mHandler.removeMessages(MSG_UPDATE_INTENSITY);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_INTENSITY, intensity, 0));
            break;

        // be sure to search contacts by voice
        case VoiceCommandListener.ACTION_VOICE_CONTACTS_SPEECHDETECTED:
            LogUtils.i(TAG, "[handleContactsUIVoiceCommand] [vcs] ACTION_VOICE_CONTACTS_SPEECHDETECTED");
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SPEECH_DETECTED));
            break;

        default:
            break;
        }
    }

    private void unbindVoiceService() {
        mContext.unbindService(mVoiceSerConnection);
        mIsRegistered = false;
        mVCmdMgrService = null;
        LogUtils.i(TAG, "[unbindVoiceService] [vcs] unbindVoiceService.");
    }

    private void clear() {
        if (mContext != null) {
            final String pkgName = mContext.getPackageName();
            unRegisterVoiceCommand(pkgName);
        }
        mContext = null;
        mListener = null;
    }

    private boolean isVCmdMgrServiceExists() {
        return mVCmdMgrService != null;
    }

    /**
     * show VCS App Guide if VCS feature is Enable.
     * 
     * @param activity
     */
    public static boolean setVcsAppGuideVisibility(Activity activity, boolean visibility, OnGuideFinishListener listener) {
        if (VCSUtils.isVCSFeatureEnabled()) {
            return ExtensionManager.getInstance().getContactAccountExtension()
                    .setVcsAppGuideVisibility(activity, visibility, listener, ContactPluginDefault.COMMD_FOR_AppGuideExt);
        }
        return false;
    }
}
