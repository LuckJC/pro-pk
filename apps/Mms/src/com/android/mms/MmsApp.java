/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms;

import java.util.HashMap;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.location.Country;
import android.location.CountryDetector;
import android.location.CountryListener;
import android.net.Uri;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.Telephony.Sms.Intents;
import android.provider.SearchRecentSuggestions;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import com.android.mms.layout.LayoutManager;
import com.android.mms.PDebug;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.DraftCache;
import com.android.mms.util.MuteCache;
import com.android.mms.util.PduLoaderManager;
import com.android.mms.util.RateController;
import com.android.mms.util.SmileyParser;
import com.android.mms.util.ThumbnailManager;




/// M:
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.widget.Toast;

import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.drm.EncapsulatedDrmManagerClient;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.ipmsg.util.IpMessageUtils;
///M: add for ALPS00444082 @{
import com.android.mms.transaction.MmsSystemEventReceiver;




/// @}
/// M: ALPS00440523, set service to foreground @ {
import android.content.Intent;
import android.database.Cursor;

import com.android.mms.MmsPluginManager;
import com.android.mms.transaction.TransactionService;
import com.mediatek.mms.ext.IMmsTransaction;
/// @}
import com.mediatek.mms.ext.IMmsSettings;
import com.mediatek.mms.ext.IMmsSettingsHost;

public class MmsApp extends Application implements IMmsSettingsHost {
    public static final String LOG_TAG = "Mms";   

    private SearchRecentSuggestions mRecentSuggestions;
    private TelephonyManager mTelephonyManager;
    private CountryDetector mCountryDetector;
    private CountryListener mCountryListener;
    private String mCountryIso;
    private static MmsApp sMmsApp = null;
    private PduLoaderManager mPduLoaderManager;
    private ThumbnailManager mThumbnailManager;
    private EncapsulatedDrmManagerClient mDrmManagerClient;
    /// M: fix bug ALPS00987075, Optimize first launch time @{
    private Context mContext;
    /// @}

    /// M: for toast thread
    public static final String TXN_TAG = "Mms/Txn";
    public static final int MSG_RETRIEVE_FAILURE_DEVICE_MEMORY_FULL = 2;
    public static final int MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION = 4;
    public static final int MSG_MMS_TOO_BIG_TO_DOWNLOAD = 6;
    public static final int MSG_MMS_CAN_NOT_SAVE = 8;
    public static final int MSG_MMS_CAN_NOT_OPEN = 10;
    public static final int MSG_DONE = 12;
    public static final int EVENT_QUIT = 100;
    private static HandlerThread mToastthread = null;
    private static ToastHandler mToastHandler = null;

    @Override
    public void onCreate() {
        super.onCreate();

        PDebug.Start("MmsApp onCreate");
        /// M: ALPS00440523, when run mms ap will scan and restart pending mms @{
        MmsLog.d(MmsApp.TXN_TAG, "MmsApp.onCreate");
        /// @}

        if (Log.isLoggable(LogTag.STRICT_MODE_TAG, Log.DEBUG)) {
            // Log tag for enabling/disabling StrictMode violation log. This will dump a stack
            // in the log that shows the StrictMode violator.
            // To enable: adb shell setprop log.tag.Mms:strictmode DEBUG
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        }

        sMmsApp = this;
        /// M: add for ipmessage
        IpMessageUtils.getIpMessagePlugin(this).getServiceManager(this).startIpService();
        com.android.mms.util.SmileyParser2.init(this);

        // Load the default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);        

        // Figure out the country *before* loading contacts and formatting numbers
        mCountryDetector = (CountryDetector) getSystemService(Context.COUNTRY_DETECTOR);
        mCountryListener = new CountryListener() {
            @Override
            public synchronized void onCountryDetected(Country country) {
                mCountryIso = country.getCountryIso();
            }
        };
        mCountryDetector.addCountryListener(mCountryListener, getMainLooper());
        /// M: fix bug ALPS01017776, Optimize first launch time @{
        mContext = getApplicationContext();
        /// @}
        MmsConfig.init(this);
        MmsPluginManager.initPlugins(this);
        Contact.init(this);
        DraftCache.init(this);
        /// M: comment this
        Conversation.init(this);
        DownloadManager.init(this);
        RateController.init(this);
        LayoutManager.init(this);
        SmileyParser.init(this);
        MessagingNotification.init(this);
        /// M: @{
        InitToastThread();
        /// @}
        if (MmsConfig.getFolderModeEnabled()) {
            MuteCache.init(this);
        }

        /// M: ALPS00440523, when run mms ap will scan and restart pending mms @{
        IMmsTransaction mmsTransactionPlugin = (IMmsTransaction)MmsPluginManager
                                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_TRANSACTION);
        if (mmsTransactionPlugin.isRestartPendingsEnabled()) {
            startService(new Intent(this, TransactionService.class));
        } else {
            ///M: add for ALPS00444082 @{
            new Thread(new Runnable() {
                public void run() {
                    MmsSystemEventReceiver.setPendingMmsFailed(mContext);
                }
            }).start();
            /// @}
        }
        MmsConfig.printMmsMemStat(mContext, "MmsApp.onCreate");
        /// @}
        /// M: new feature for regional phone. @{
        mMmsSettingsPlugin = (IMmsSettings)MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_SETTINGS);
        MmsLog.d(MmsApp.TXN_TAG, "MmsApp#onCreate, mMmsSettingsPlugin=" + mMmsSettingsPlugin);
        mMmsSettingsPlugin.init(this);
        /// @}
        PDebug.End("MmsApp onCreate");
        //add-s by minesea
        setLoadDefaultRing();
        //add-e by minesea
    }

    synchronized public static MmsApp getApplication() {
        return sMmsApp;
    }

    @Override
    public void onTerminate() {
        mCountryDetector.removeCountryListener(mCountryListener);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        /// M: fix bug ALPS01017776, Optimize first launch time @{
        if (mPduLoaderManager != null) {
            mPduLoaderManager.onLowMemory();
        }
        if (mThumbnailManager != null) {
            mThumbnailManager.onLowMemory();
        }
        /// @}
    }

    public PduLoaderManager getPduLoaderManager() {
        /// M: fix bug ALPS01017776, Optimize first launch time @{
        if (mPduLoaderManager == null) {
            mPduLoaderManager = new PduLoaderManager(mContext);
        }
        /// @}
        return mPduLoaderManager;
    }

    public ThumbnailManager getThumbnailManager() {
        /// M: fix bug ALPS01017776, Optimize first launch time @{
        if (mThumbnailManager == null) {
            mThumbnailManager = new ThumbnailManager(mContext);
        }
        /// @}
        return mThumbnailManager;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LayoutManager.getInstance().onConfigurationChanged(newConfig);
    }

    /**
     * @return Returns the TelephonyManager.
     */
    public TelephonyManager getTelephonyManager() {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager)getApplicationContext()
                    .getSystemService(Context.TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }

    /**
     * Returns the content provider wrapper that allows access to recent searches.
     * @return Returns the content provider wrapper that allows access to recent searches.
     */
    public SearchRecentSuggestions getRecentSuggestions() {
        /*
        if (mRecentSuggestions == null) {
            mRecentSuggestions = new SearchRecentSuggestions(this,
                    SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
        }
        */
        return mRecentSuggestions;
    }

    /// Google JB MR1.1 patch. This function CAN return null.
    public String getCurrentCountryIso() {
        if (mCountryIso == null) {
            Country country = mCountryDetector.detectCountry();
            if (country != null) {
                mCountryIso = country.getCountryIso();
            }
        }
        return mCountryIso;
    }

    public EncapsulatedDrmManagerClient getDrmManagerClient() {
        if (mDrmManagerClient == null) {
            mDrmManagerClient = new EncapsulatedDrmManagerClient(getApplicationContext());
        }
        return mDrmManagerClient;
    }

    /// M: a handler belong to UI thread.
    private void InitToastThread() {
        if (null == mToastHandler) {
            mToastHandler = new ToastHandler();
        }
    }

    public static ToastHandler getToastHandler() {
        return mToastHandler;
    }

    public final class ToastHandler extends Handler {
        public ToastHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            MmsLog.d(MmsApp.TXN_TAG, "Toast Handler handleMessage :" + msg);
            
            switch (msg.what) {
                case EVENT_QUIT: {
                    MmsLog.d(MmsApp.TXN_TAG, "EVENT_QUIT");
                    getLooper().quit();
                    return;
                }

                case MSG_RETRIEVE_FAILURE_DEVICE_MEMORY_FULL: {
                    Toast.makeText(sMmsApp, R.string.download_failed_due_to_full_memory, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_SHOW_TRANSIENTLY_FAILED_NOTIFICATION: {
                    Toast.makeText(sMmsApp, R.string.transmission_transiently_failed, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_MMS_TOO_BIG_TO_DOWNLOAD: {
                    Toast.makeText(sMmsApp, R.string.mms_too_big_to_download, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_MMS_CAN_NOT_SAVE: {
                    Toast.makeText(sMmsApp, R.string.cannot_save_message, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_MMS_CAN_NOT_OPEN: {
                    String str = sMmsApp.getResources().getString(R.string.unsupported_media_format, (String)msg.obj);
                    Toast.makeText(sMmsApp, str, Toast.LENGTH_LONG).show();
                    break;
                }

                case MSG_DONE: {
                    Toast.makeText(sMmsApp, R.string.finish, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    /// M: new feature for regional phone.
    /// modify values about creation mode and sms service center. @{
    IMmsSettings mMmsSettingsPlugin = null;
    String mCreationMode = "";
    String mSmsServiceCenter = "";
    public void setSmsValues(HashMap<String, String> values) {
        mCreationMode = values.get("creationmode");
        setCreactionMode(mCreationMode);
        mSmsServiceCenter = values.get("servicecenter");
        setSmsServiceCenter(0, mSmsServiceCenter);
    }

    private void setCreactionMode(String mode) {
        MmsLog.d(MmsApp.TXN_TAG, "setCreactionMode, mode=" + mode);
        Context context = getBaseContext();
        if (!TextUtils.isEmpty(mode)) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString("pref_key_mms_creation_mode", mode);
            editor.commit();
        }
    }

    private void setSmsServiceCenter(final int simId, final String number) {
        MmsLog.d(MmsApp.TXN_TAG, "setSmsCenter,  simId=" + simId + ", number=" + number);
        final EncapsulatedTelephonyService teleService = EncapsulatedTelephonyService.getInstance();
        new Thread(new Runnable() {
            public void run() {
                try {
                    teleService.setScAddressGemini(number, simId);
                } catch (RemoteException e1) {
                    MmsLog.e(MmsApp.TXN_TAG,"setScAddressGemini is failed.\n" + e1.toString());
                } catch (NullPointerException e2) {
                    MmsLog.e(MmsApp.TXN_TAG,"setScAddressGemini is failed.\n" + e2.toString());
                }
            }
        }).start();
    }

    public void registerSmsStateReceiver() {
        IntentFilter intentFilter = new IntentFilter(EncapsulatedTelephony.Sms.Intents.SMS_STATE_CHANGED_ACTION);
        this.registerReceiver(mSmsStateReceiver, intentFilter);
    }

    private BroadcastReceiver mSmsStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(EncapsulatedTelephony.Sms.Intents.SMS_STATE_CHANGED_ACTION)) {
                boolean isReady = intent.getBooleanExtra("ready", false);
                if (isReady) {
                    int simId = intent.getIntExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, -1);
                    if (simId >= 0) {
                        if (mMmsSettingsPlugin != null && TextUtils.isEmpty(mSmsServiceCenter)) {
                            mSmsServiceCenter = mMmsSettingsPlugin.getSmsServiceCenter();
                        }
                        MmsLog.d(MmsApp.TXN_TAG, "mSmsStateReceiver#onReceive, mSmsServiceCenter=" + mSmsServiceCenter);
                        if (!TextUtils.isEmpty(mSmsServiceCenter)) {
                            setSmsServiceCenter(simId, mSmsServiceCenter);
                        }
                    }
                    MmsApp.this.unregisterReceiver(mSmsStateReceiver);
                }
            }
        }
    };
    /// @}
    
    
    public void setLoadDefaultRingDone(){
    	SharedPreferences ringShare = getSharedPreferences("com.android.mms_preferences", Context.MODE_PRIVATE);
    	String defaultRing = ringShare.getString("pref_key_ringtone", "");
    	if(defaultRing == null || defaultRing.equals("")){
    		SharedPreferences.Editor editor = ringShare.edit();
    		if(getRealPath() != null){
    			editor.putString("pref_key_ringtone", getRealPath().toString());
	    	}else{
	    		editor.putString("pref_key_ringtone", "");
	    	}
    		editor.commit();
    	}
    }
    
    public void setLoadDefaultRing(){
    	mHandler.sendEmptyMessageDelayed(MSG_RING_DEFAULT,4000);
    }
    
    Handler mHandler = new RingDefaultHandler();
    
    private final int MSG_RING_DEFAULT = 1;
    class RingDefaultHandler extends Handler {

		public RingDefaultHandler() {

		}

		public RingDefaultHandler(Looper L) {
			super(L);
		}

		@Override
		public void handleMessage(Message paramMessage) {
			switch (paramMessage.what) {
			case MSG_RING_DEFAULT:
				setLoadDefaultRingDone();
				break;
			default:
			}
		}
    }
    public Uri getRealPath() {
		Uri uri = null;		
		StringBuffer buff = new StringBuffer(); 
		buff.append(MediaStore.Audio.Media.DATA).append(" like '").append("%").append("Lemon_SMS.mp3").append("'");
		Cursor cur = getContentResolver().query(
		MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
		new String[] { MediaStore.Audio.Media._ID }, buff.toString(),
		null, null);
		
				
		int index = 0;
		for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
			index = cur.getColumnIndex(Images.ImageColumns._ID);
			// set _id value
			index = cur.getInt(index);
		}
		if (index == 0) {
			// do nothing
		} else {
			Uri uri_temp = Uri.parse("content://media/internal/audio/media/"
					+ index);
			if (uri_temp != null) {
				uri = uri_temp;
			}
		}
		return uri;
	}
}
