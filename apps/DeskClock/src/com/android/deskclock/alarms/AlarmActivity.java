/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.alarms;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.multiwaveview.GlowPadView;

///M: MTK Voice command support @{
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.common.voicecommand.IVoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandManagerService;
import com.mediatek.common.voicecommand.VoiceCommandListener;
///@}
/**
 * Alarm activity that pops up a visible indicator when the alarm goes off.
 */
public class AlarmActivity extends Activity {
    // AlarmActivity listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    // AlarmActivity listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    // Controller for GlowPadView.
    private class GlowPadController extends Handler implements GlowPadView.OnTriggerListener {
        private static final int PING_MESSAGE_WHAT = 101;
        private static final long PING_AUTO_REPEAT_DELAY_MSEC = 1200;

        public void startPinger() {
            sendEmptyMessage(PING_MESSAGE_WHAT);
        }

        public void stopPinger() {
            removeMessages(PING_MESSAGE_WHAT);
        }

        @Override
        public void handleMessage(Message msg) {
            ping();
            sendEmptyMessageDelayed(PING_MESSAGE_WHAT, PING_AUTO_REPEAT_DELAY_MSEC);
        }

        @Override
        public void onGrabbed(View v, int handle) {
            stopPinger();
        }

        @Override
        public void onReleased(View v, int handle) {
            startPinger();

        }

        @Override
        public void onTrigger(View v, int target) {
            switch (mGlowPadView.getResourceIdForTarget(target)) {
                case R.drawable.ic_alarm_alert_snooze:
                    Log.v("AlarmActivity - GlowPad snooze trigger");
                    snooze();
                    break;

                case R.drawable.ic_alarm_alert_dismiss:
                    Log.v("AlarmActivity - GlowPad dismiss trigger");
                    dismiss();
                    break;
                default:
                    // Code should never reach here.
                    Log.e("Trigger detected on unhandled resource. Skipping.");
            }
        }

        @Override
        public void onGrabbedStateChange(View v, int handle) {
            Log.v("AlarmActivity onGrabbedStateChange");
        }

        @Override
        public void onFinishFinalAnimation() {
            Log.v("AlarmActivity onFinishFinalAnimation");
        }
    }

    private AlarmInstance mInstance;
    private int mVolumeBehavior;
    private GlowPadView mGlowPadView;
    private GlowPadController glowPadController = new GlowPadController();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("AlarmActivity - Broadcast Receiver - " + action);
            if (action.equals(ALARM_SNOOZE_ACTION)) {
                snooze();
            } else if (action.equals(ALARM_DISMISS_ACTION)) {
                dismiss();
            } else if (action.equals(AlarmService.ALARM_DONE_ACTION)) {
                finish();
            } else {
                Log.i("Unknown broadcast in AlarmActivity: " + action);
            }
        }
    };

    private void snooze() {
        AlarmStateManager.setSnoozeState(this, mInstance);
    }

    private void dismiss() {
        ///M: Voice Command support @{
        if (IS_SUPPORT_VOICE_COMMAND_UI) {
            getNotificationManager().cancel("voiceui", 100);
        }
        ///@}
        AlarmStateManager.setDismissState(this, mInstance);
    }

    ///M:VoiceCommand support @{
    private Context mContext;
    private String[] mKeywordArray;
    private IVoiceCommandManagerService mVCmdMgrService;
    private boolean mVoiceUiStartSuccessful = false;
    private boolean mVCmdIsRegistered = false;
    private static final int VOICE_COMMAND_ID_SNOOZE = 5;
    private static final int VOICE_COMMAND_ID_STOP = 6;
    public static final boolean IS_SUPPORT_VOICE_COMMAND_UI = FeatureOption.MTK_VOICE_UI_SUPPORT;
    ///@}

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        long instanceId = AlarmInstance.getId(getIntent().getData());
        mInstance = AlarmInstance.getInstance(this.getContentResolver(), instanceId);
        Log.v("Displaying alarm for instance: " + mInstance);
        if (mInstance == null) {
            // The alarm got deleted before the activity got created, so just finish()
            Log.v("Error displaying alarm for intent: " + getIntent());
            finish();
            return;
        }

        ///M: init voice command manager and listener @{
        Log.v("AlarmActivity IS_SUPPORT_VOICE_COMMAND_UI = " + IS_SUPPORT_VOICE_COMMAND_UI);
        if (IS_SUPPORT_VOICE_COMMAND_UI) {
            mContext = this;
        }
        /// @}

        // Get the volume/camera button behavior setting
        final String vol =
                PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        mVolumeBehavior = Integer.parseInt(vol);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        ///M: Don't show the wallpaper when the alert arrive. @{
        win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        ///@}

        // In order to allow tablets to freely rotate and phones to stick
        // with "nosensor" (use default device orientation) we have to have
        // the manifest start with an orientation of unspecified" and only limit
        // to "nosensor" for phones. Otherwise we get behavior like in b/8728671
        // where tablets start off in their default orientation and then are
        // able to freely rotate.
        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
        updateLayout();

        // Register to get the alarm done/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);
    }

    private void updateTitle() {
        final String titleText = mInstance.getLabelOrDefault(this);
        TextView tv = (TextView)findViewById(R.id.alertTitle);
        tv.setText(titleText);
        super.setTitle(titleText);
    }

    private void updateLayout() {
        final LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.alarm_alert, null);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(view);
        updateTitle();
        Utils.setTimeFormat((TextClock)(view.findViewById(R.id.digitalClock)),
                (int)getResources().getDimension(R.dimen.bottom_text_size));

        // Setup GlowPadController
        mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
        mGlowPadView.setOnTriggerListener(glowPadController);
        glowPadController.startPinger();
    }

    private void ping() {
        mGlowPadView.ping();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
        ///M: show the VoiceCommand UI
        if (mVoiceUiStartSuccessful) {
            displayIndicator();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ///M: register and start voice UI. @{
        if (IS_SUPPORT_VOICE_COMMAND_UI) {
            if (mVCmdMgrService == null) {
                bindVoiceService();
            } else {
                String pkgName = getPackageName();
                registerVoiceCommand(pkgName);
                sendVoiceCommand(pkgName, VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                            VoiceCommandListener.ACTION_VOICE_UI_START, null);
            }
        }
        /// @}
        glowPadController.startPinger();
    }

    @Override
    protected void onPause() {
        ///M: stop voice ui and unregister corresponding listener.@{
        if (IS_SUPPORT_VOICE_COMMAND_UI) {
            Log.v("Deskclock onStop: unregister voice command listener and send stop command");
            if (mVCmdMgrService != null) {
                String pkgName = getPackageName();
                sendVoiceCommand(pkgName, VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                        VoiceCommandListener.ACTION_VOICE_UI_STOP, null);
                unRegisterVoiceCommand(pkgName);
            }
        }
        ///@}
        glowPadController.stopPinger();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mInstance == null) {
            super.onDestroy();
            return;
        }
        unregisterReceiver(mReceiver);
        ///M: Voice command support unbindService @{
        if (IS_SUPPORT_VOICE_COMMAND_UI) {
            Log.v("Unbind voice Service");
            unbindService(mVoiceSerConnection);
            mVCmdIsRegistered = false;
            mVCmdMgrService = null;
        }
        ///@}
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        Log.v("AlarmActivity - dispatchKeyEvent - " + event.getKeyCode());
        switch (event.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    switch (mVolumeBehavior) {
                        case 1:
                            snooze();
                            break;

                        case 2:
                            dismiss();
                            break;

                        default:
                            break;
                    }
                }
                return true;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * M: Voice Command support, bindVoiceService @{
     */
    private void bindVoiceService() {
        Log.v("Bind voice service.");
        Intent mVoiceServiceIntent = new Intent();
        mVoiceServiceIntent.setAction(VoiceCommandListener.VOICE_SERVICE_ACTION);
        mVoiceServiceIntent.addCategory(VoiceCommandListener.VOICE_SERVICE_CATEGORY);
        bindService(mVoiceServiceIntent, mVoiceSerConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * M: Voice Command support, mVoiceSerConnection @{
     */
    private final ServiceConnection mVoiceSerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mVCmdMgrService = IVoiceCommandManagerService.Stub.asInterface(service);
            Log.i("ServiceConnection onServiceConnected.");
            String pkgName = getPackageName();
            registerVoiceCommand(pkgName);
            sendVoiceCommand(pkgName, VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                        VoiceCommandListener.ACTION_VOICE_UI_START, null);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v("Service disconnected");
            mVCmdIsRegistered = false;
            mVCmdMgrService = null;
        }
    };

    /**
     * M: Voice Command support, registerVoiceCommand @{
     */
    private void registerVoiceCommand(String pkgName) {
        if (!mVCmdIsRegistered) {
            try {
                int errorid = mVCmdMgrService.registerListener(pkgName, mCallback);
                Log.v("Register voice Listener pkgName = " + pkgName + ",errorid = " + errorid);
                if (errorid == VoiceCommandListener.VOICE_NO_ERROR) {
                    mVCmdIsRegistered = true;
                    Log.v("App has register voice listener success");
                } else {
                    Log.v("App Register voice Listener failure ");
                }
            } catch (RemoteException e) {
                mVCmdMgrService = null;
                Log.v("Register voice Listener RemoteException = " + e.getMessage());
            }
        }
        Log.v("mVCmdIsRegistered = " + mVCmdIsRegistered + ", Register voice listener end!");
    }

    /**
     * M: Voice command support, unRegisterVoiceCommand @{
     */
    private void unRegisterVoiceCommand(String pkgName) {
        try {
            int errorid = mVCmdMgrService.unregisterListener(pkgName, mCallback);
            Log.v("Unregister voice listener, errorid = " + errorid);
            if (errorid == VoiceCommandListener.VOICE_NO_ERROR) {
                mVCmdIsRegistered = false;
            }
        } catch (RemoteException e) {
            Log.v("Unregister error in handler RemoteException = " + e.getMessage());
            mVCmdIsRegistered = false;
            mVCmdMgrService = null;
        }
        Log.v("UnRegister voice listener end! mVCmdIsRegistered = " + mVCmdIsRegistered);
    }

    /**
     * M: Voice command support, sendVoiceCommand @{
     */
    private void sendVoiceCommand(String pkgName, int mainAction, int subAction, Bundle extraData) {
        if (mVCmdIsRegistered) {
            try {
                Log.v("Send Command " + "pkgName" + pkgName + " mainAction=" + mainAction
                        + " subAction=" + subAction + " extraData=" + extraData);
                int errorid = mVCmdMgrService.sendCommand(pkgName, mainAction, subAction, extraData);
                if (errorid == VoiceCommandListener.VOICE_NO_ERROR) {
                    Log.v("Send Command success");
                } else {
                    Log.v("Send Command failure");
                }
            } catch (RemoteException e) {
                mVCmdIsRegistered = false;
                mVCmdMgrService = null;
                Log.v("sendCommand RemoteException");
            }
        } else {
            Log.v("App has not register listener can not send command");
        }
    }

    /**
     * M: Voice Command support getNtificationManager @{
     */
    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * M: Voice Command support Callback used to notify apps @{
     */
    private final IVoiceCommandListener mCallback = new IVoiceCommandListener.Stub() {

        @Override
        public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData)
                throws RemoteException {
            Log.v("onVoiceCommandNotified --> handleVoiceCommandNotified");
            Message.obtain(mHandler, mainAction, subAction, 0, extraData).sendToTarget();
        }
    };

    /**
     * M: Voice Command support Handler @{
     */
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            handleVoiceCommandNotified(msg.what, msg.arg1, (Bundle) msg.obj);
        }
    };

    /**
     * M: Voice Command support handleVoiceCommandNotified @{
     */

    @SuppressWarnings("PMD")
    private void handleVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) {
        Log.v("AlarmActivity in handleVoiceCommandNotified mainAction = " + mainAction
                + ",subAction = " + subAction + ",extraData = " + extraData);
        switch (mainAction) {
        case VoiceCommandListener.ACTION_MAIN_VOICE_COMMON:
            if (subAction == VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD) {
                Log.v("ACTION_MAIN_VOICE_COMMON");
                int keywordResult = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT);
                if (keywordResult == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                    mKeywordArray = extraData.getStringArray(
                            VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                    if (mKeywordArray != null && mKeywordArray.length >= 2) {
                        displayIndicator();
                        Notification indicatorNotify = new Notification.Builder(mContext)
                            .setContentTitle(mContext.getString(R.string.voice_ui_title))
                            .setContentText(mContext.getString(R.string.voice_ui_content_text))
                            .setSmallIcon(com.mediatek.internal.R.drawable.stat_voice)
                            .build();
                        getNotificationManager().notify("voiceui", 100, indicatorNotify);
                    }
                }
            }
            break;
        case VoiceCommandListener.ACTION_MAIN_VOICE_UI:
            if (subAction == VoiceCommandListener.ACTION_VOICE_UI_START) {
                int startResult = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT);
                if (startResult == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                    // success.
                    Log.v("voice ui starts normally  ACTION_MAIN_VOICE_UI");
                    mVoiceUiStartSuccessful = true;
                    if (mVCmdMgrService != null) {
                        sendVoiceCommand(getPackageName(), VoiceCommandListener.ACTION_MAIN_VOICE_COMMON,
                                VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD, null);
                    }
                    Log.v("voice ui start success. ACTION_VOICE_UI_START");
                } else {
                    // error
                    int errorID = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                    String errorString = extraData
                            .getString(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1);
                    Log.v(" voice ui starts abnormally,with errorID: " + errorID + ",errorString: "
                            + errorString);
                }
            } else if (subAction == VoiceCommandListener.ACTION_VOICE_UI_STOP) {
                int stopResult = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT);
                if (stopResult == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                    // sucess
                    Log.v("voice ui stop success ACTION_VOICE_UI_STOP");
                } else {
                    // ERROR
                    int stopErrorID = extraData
                            .getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                    String stopErrorString = extraData
                            .getString(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1);
                    Log.v("voice ui stop error with errorID:" + stopErrorID + ",errorString:"
                            + stopErrorString);
                }
            } else if (subAction == VoiceCommandListener.ACTION_VOICE_UI_NOTIFY) {
                int notifyResult = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT);
                int id = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                if (notifyResult == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                    // success
                    if (id == VOICE_COMMAND_ID_SNOOZE) {
                        // ringtone disappears gradually.
                        Log.v("snooze is triggered ACTION_VOICE_UI_NOTIFY");
                        snooze();
                        finish();
                    } else if (id == VOICE_COMMAND_ID_STOP) {
                        Log.v("dismissed is triggered ACTION_VOICE_UI_NOTIFY");
                        if (IS_SUPPORT_VOICE_COMMAND_UI) {
                            getNotificationManager().cancel("voiceui", 100);
                        }
                        dismiss();
                        finish();
                    }
                } else {
                    // error
                    String notifyErrorString = extraData.getString(
                        VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1);
                    Log.v("something is wrong when notify,with notifyError id:" + id
                            + ",notifyErrorString:" + notifyErrorString);
                }
            }
            break;
        default:
            break;
        }
    }

    /**
     * M: Voice Command support displayIndicator @{
     */
    private void displayIndicator() {
        Log.v("AlarmFullScreen displayIndicator");
        ImageView icon = (ImageView) findViewById(R.id.indicator_icon);
        TextView ticker = (TextView) findViewById(R.id.indicator_text);
        icon.setImageResource(com.mediatek.internal.R.drawable.stat_voice);
        icon.setVisibility(View.VISIBLE);
        ticker.setVisibility(View.VISIBLE);
        Configuration conf = getResources().getConfiguration();
        if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ticker.setText(mContext.getString(R.string.alarm_command_summary_format_land,
                    mKeywordArray[0], mKeywordArray[1]));
        } else {
            ticker.setText(mContext.getString(R.string.alarm_command_summary_format,
                    mKeywordArray[0], mKeywordArray[1]));
        }
    }
}
