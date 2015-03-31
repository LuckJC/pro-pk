/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.Display;
import android.view.KeyEvent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.print.PrintHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.widget.ActivityChooserView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.ui.Log;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.PanoramaViewHelper;
import android.os.Environment;
import android.provider.Settings;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.WifiDisplayStatus;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplay;
import android.net.Uri;
import android.view.Menu;

import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.panorama.PanoramaHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;
///M: import for HDMI.
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.hdmi.IHDMINative;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
import com.mediatek.hotknot.*;
 
import java.io.FileNotFoundException;
public class AbstractGalleryActivity extends Activity implements GalleryContext {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/AbstractGalleryActivity";
    private GLRootView mGLRootView;
    private StateManager mStateManager;
    private GalleryActionBar mActionBar;
    private OrientationManager mOrientationManager;
    private TransitionStore mTransitionStore = new TransitionStore();
    private boolean mDisableToggleStatusBar;
    private PanoramaViewHelper mPanoramaViewHelper;

    private AlertDialog mAlertDialog = null;
    /// M: sign gallery status.
    private volatile boolean hasPausedActivity;

    ///M: added for WFD. @{
    //M:define for WFD(wifydiaplay) connected about DRM file play. @{
    public static final String WFD_CONNECTED_ACTION = "com.mediatek.wfd.connection";
    public static final int WFD_CONNECTED_FLAG = 1;
    public static final int WFD_DISCONNECTED_FLAG = 0;
    private boolean mIsWFDconnect = false;
    private WfdReceiver WfdReceiver = new WfdReceiver();
    /// @}
    /// M: define for HEMI about Image path @{
    public static String KEY_HDMI_ENABLE_STATUS = "hdmi_enable_status";
    private HdmiReceiver hdmiReceiver = new HdmiReceiver();
    private IHDMINative mHdmiNative = null;
    private int hdmiCableState = 0;
    private boolean mIsHDMIconnect = false;
    /// @}
    private boolean mIsSMBconnect = false;
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if (getExternalCacheDir() != null) onStorageReady();
            // we don't care about SD card content;
            // As long as the card is mounted, dismiss the dialog
            onStorageReady();
        }
    };
    private IntentFilter mMountFilter = null;
    ///M:
    private static final boolean mIsMavSupported = 
        MediatekFeature.isMAVSupported();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MtkUtils.traceStart("AbstractGalleryActivity:onCreate");
        MtkUtils.traceStart("Activity:onCreate");
        super.onCreate(savedInstanceState);
        MtkUtils.traceEnd("Activity:onCreate");
        Log.d(TAG, "onCreate");

        mOrientationManager = new OrientationManager(this);
        toggleStatusBarByOrientation();
        getWindow().setBackgroundDrawable(null);
        mHdmiNative = MediatekClassFactory.createInstance(IHDMINative.class);
        if (mHdmiNative == null) {
            MtkLog.e(TAG, "Native is not created");
        }
        mPanoramaViewHelper = new PanoramaViewHelper(this);
        mPanoramaViewHelper.onCreate();
        doBindBatchService();
        ///M: add for MAV @{
        if(mIsMavSupported) {
            initGyroSensor();
        }
        ///@}
        /// M: added for HotKnot
        hotKnotInit(this);

        MtkUtils.traceEnd("AbstractGalleryActivity:onCreate");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(outState);
            getStateManager().saveState(outState);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mStateManager.onConfigurationChange(config);
        getGalleryActionBar().onConfigurationChanged();
        invalidateOptionsMenu();
        toggleStatusBarByOrientation();
        ///M: the picture show abnormal after rotate device
        ///to landscape mode,lock device, rotate device to portrait
        ///mode, unlock device.
        ///to resolve this problem, let it show dark color
        if(hasPausedActivity) {
            mGLRootView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }

    @Override
    public DataManager getDataManager() {
        return ((GalleryApp) getApplication()).getDataManager();
    }

    @Override
    public ThreadPool getThreadPool() {
        return ((GalleryApp) getApplication()).getThreadPool();
    }

    public synchronized StateManager getStateManager() {
        if (mStateManager == null) {
            mStateManager = new StateManager(this);
        }
        return mStateManager;
    }

    public GLRoot getGLRoot() {
        return mGLRootView;
    }

    public OrientationManager getOrientationManager() {
        return mOrientationManager;
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
    }

    protected void onStorageReady() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
            unregisterReceiver(mMountReceiver);
        }
    }
    
    /// M: For SmartBook @{
    private final BroadcastReceiver mSmartBookReceiver = new SmartBookBroadcastReceiver();

    private void registerSmartBookReceiver() {
        IntentFilter mSmartBookIntentFilter = new IntentFilter();
        mSmartBookIntentFilter.addAction(Intent.ACTION_SMARTBOOK_PLUG);
        registerReceiver(mSmartBookReceiver, mSmartBookIntentFilter);
    }
    
    private class SmartBookBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean connected = intent.getBooleanExtra(Intent.EXTRA_SMARTBOOK_PLUG_STATE, false);
            MtkLog.i(TAG, "************Rceive Smartbook action = " + action + "|| Isconnected = " + connected);
            
            if (Intent.ACTION_SMARTBOOK_PLUG.equals(action)) {
                if (connected) {
                    mIsSMBconnect = true;
                } else {
                    mIsSMBconnect = false;
                }
                isReplaceDRMImage();
            }
        }
    }
    
    private void isReplaceDRMImage() {
        if (mIsSMBconnect) {
            DrmHelper.drmSetShowState(DrmHelper.DRM_SHOW_STATE_SMB);
        }else if(mIsWFDconnect){
            DrmHelper.drmSetShowState(DrmHelper.DRM_SHOW_STATE_WFD);
        }else if(mIsHDMIconnect){
            DrmHelper.drmSetShowState(DrmHelper.DRM_SHOW_STATE_HDMI);
        }else{
            DrmHelper.drmSetShowState(DrmHelper.DRM_SHOW_STATE_NORMAL);
        }
        mGLRootView.requestRender();
    }
    
    /// M:end
    @Override
    protected void onStart() {
        super.onStart();
        ///M: added for WFD. @{
        // register WFD broadcast Receiver. @{
        IntentFilter filter = new IntentFilter();
        filter.addAction(WFD_CONNECTED_ACTION);
        getApplication().registerReceiver(WfdReceiver, filter);
        MtkLog.i(TAG, "register wfdReceiver");
        /// @}
        // register HDMI broadcast Receiver. @{
        IntentFilter hfilter = new IntentFilter();
        hfilter.addAction(Intent.ACTION_HDMI_PLUG);
        getApplication().registerReceiver(hdmiReceiver, hfilter);
        MtkLog.i(TAG, "register HdmiReceiver");
        /// @}
        
        registerSmartBookReceiver();
        // M: if we're viewing a non-local file/uri, do NOT check storage 
        // or pop up "No storage" dialog
        Log.d(TAG, "onStart: should check storage=" + mShouldCheckStorageState);
        if (!mShouldCheckStorageState) {
            return;
        }
        
        // M: we only care about not mounted condition,
        // SD card full/error state does not affect normal usage of Gallery2
        if ((MtkUtils.getMTKExternalCacheDir(this) == null) && (!isDefaultStorageMounted())) {
            OnCancelListener onCancel = new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            };
            OnClickListener onClick = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_external_storage_title)
                    .setMessage(R.string.no_external_storage)
                    .setNegativeButton(android.R.string.cancel, onClick)
                    .setOnCancelListener(onCancel);
            if (ApiHelper.HAS_SET_ICON_ATTRIBUTE) {
                setAlertDialogIconAttribute(builder);
            } else {
                builder.setIcon(android.R.drawable.ic_dialog_alert);
            }
            mAlertDialog = builder.show();
            if (mMountFilter == null) {
                mMountFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
                mMountFilter.addDataScheme("file");
            }
            registerReceiver(mMountReceiver, mMountFilter);
        }
        mPanoramaViewHelper.onStart();
    }

    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    private static void setAlertDialogIconAttribute(
            AlertDialog.Builder builder) {
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
    }

    @Override
    protected void onStop() {
        super.onStop();
        /// M : unregister the wifydiaplayReceiver and HDMI Broadcast Receiver when leave Gallery app.
        unregisterReceiver(mSmartBookReceiver);
        getApplication().unregisterReceiver(WfdReceiver);
        getApplication().unregisterReceiver(hdmiReceiver);
        /// @}
        if (mAlertDialog != null) {
            unregisterReceiver(mMountReceiver);
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        mPanoramaViewHelper.onStop();
        ///M:
        mGLRootView.setVisibility(View.GONE);
    }

    
    // /M: added for HDMI. @{

    public class HdmiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            hdmiCableState = intent.getIntExtra("state", 0);
            /// M: Mark for 4.3 migration
            //MtkLog.i(TAG, "mHdmiNative.needSwDrmProtect() == " + mHdmiNative.needSwDrmProtect() +  "HDMI action=" + action + "|| hdmiCableState=" + hdmiCableState);
            if (Intent.ACTION_HDMI_PLUG.equals(action)
                    //&& mHdmiNative.needSwDrmProtect() /// M: Mark for 4.3 migration
                    && (hdmiCableState == 1)
                    && (Settings.System.getInt(getContentResolver(), KEY_HDMI_ENABLE_STATUS, 1) == 1)) {
                mIsHDMIconnect = true;
            } else {
                    mIsHDMIconnect = false;
                }
            isReplaceDRMImage();
            }
        }

    
    /// @}
    public class WfdReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action == WFD_CONNECTED_ACTION) {
                int ExtraResult = intent.getIntExtra("connected", 0);
                int Secure = intent.getIntExtra("secure", 0);
                MtkLog.i(TAG, "WfdReceiver action:" + action + "connected = " + ExtraResult + " Secure:"+Secure);
                if (ExtraResult == WFD_CONNECTED_FLAG && Secure == 0) {
                    mIsWFDconnect = true;
                } else {
                    mIsWFDconnect = false;
                }
                isReplaceDRMImage();
            }
        }
    };
    /// @}

    @Override
    protected void onResume() {
        MtkUtils.traceStart("AbstractGalleryActivity:onResume");
        MtkUtils.traceStart("Activity:onResume");
        super.onResume();
        MtkUtils.traceEnd("Activity:onResume");
        /// M: add for 2k video feature @{
        String action = getIntent().getAction();
        Log.d(TAG, "getAction:" + action);
        if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)
                || Intent.ACTION_PICK.equalsIgnoreCase(action)
                || "com.mediatek.action.PICK_VIDEO_FOLDER".equalsIgnoreCase(action)) {
            MediatekFeature.hide2kVideo(true);
            getDataManager().forceRefreshAll();
        } else {
            MediatekFeature.hide2kVideo(false);
        }
        /// @}
        mGLRootView.lockRenderThread();
        /// M: when default storage has been changed, we should refresh bucked id, 
        /// or else the icon showing on the album set slot can not update
        MediaSetUtils.refreshBucketId();

        try {
            getStateManager().resume();
            getDataManager().resume();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        mGLRootView.onResume();
        mOrientationManager.resume();
        /// M: save activity status.
        hasPausedActivity = false;
        ///M:
        mGLRootView.setVisibility(View.VISIBLE);
        MtkUtils.traceEnd("AbstractGalleryActivity:onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationManager.pause();
        mGLRootView.onPause();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().pause();
            getDataManager().pause();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        GalleryBitmapPool.getInstance().clear();
        MediaItem.getBytesBufferPool().clear();
        /// M: save activity status.
        removeGyroPositionListener(null);
        hasPausedActivity = true;
        /// M: added for HotKnot
        hotKnotDismissDialog();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().destroy();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        doUnbindBatchService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGLRootView.lockRenderThread();
        try {
            getStateManager().notifyActivityResult(
                    requestCode, resultCode, data);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    public GalleryActionBar getGalleryActionBar() {
        if (mActionBar == null) {
            mActionBar = new GalleryActionBar(this);
        }
        return mActionBar;
    }

    public boolean hasPausedActivity () {
        return hasPausedActivity;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }

    protected void disableToggleStatusBar() {
        mDisableToggleStatusBar = true;
    }

    private boolean isSmallLCM() {
    	boolean smallLCM = false;
    	Point outSize = new Point();
    	getWindowManager().getDefaultDisplay().getRealSize(outSize);
    	if (outSize.x == 320 && outSize.y == 320) {
    		smallLCM = true;
    	}
    	return smallLCM;
    }
    
    // Shows status bar in portrait view, hide in landscape view
    private void toggleStatusBarByOrientation() {
        if (mDisableToggleStatusBar) return;

        Window win = getWindow();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        	//add by lihj: full screen if in small lcm
        	if (!isSmallLCM()) {        		
        		win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	}
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public TransitionStore getTransitionStore() {
        return mTransitionStore;
    }

    public PanoramaViewHelper getPanoramaViewHelper() {
        return mPanoramaViewHelper;
    }

    protected boolean isFullscreen() {
        return (getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }
    ///M: added for smartbook. @{
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        mGLRootView.dispatchKeyEventView(event);
        return super.dispatchKeyEvent(event);
    }
    /// }@
    
    // M: added for multi-storage support
    private boolean isDefaultStorageMounted() {
        String defaultStorageState = MtkUtils.getMtkDefaultStorageState(this);
        if (defaultStorageState == null) {
            defaultStorageState = Environment.getExternalStorageState();
        }
        return Environment.MEDIA_MOUNTED.equalsIgnoreCase(defaultStorageState);
    }
    
    // M: added for SD hot-plug
    public boolean mShouldCheckStorageState = true;
    private BatchService mBatchService;
    private boolean mBatchServiceIsBound = false;
    private ServiceConnection mBatchServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBatchService = ((BatchService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBatchService = null;
        }
    };

    private void doBindBatchService() {
        bindService(new Intent(this, BatchService.class), mBatchServiceConnection, Context.BIND_AUTO_CREATE);
        mBatchServiceIsBound = true;
    }

    private void doUnbindBatchService() {
        if (mBatchServiceIsBound) {
            // Detach our existing connection.
            unbindService(mBatchServiceConnection);
            mBatchServiceIsBound = false;
        }
    }

    public ThreadPool getBatchServiceThreadPoolIfAvailable() {
        if (mBatchServiceIsBound && mBatchService != null) {
            return mBatchService.getThreadPool();
        } else {
            throw new RuntimeException("Batch service unavailable");
        }
    }

    public void printSelectedImage(Uri uri) {
        if (uri == null) {
            return;
        }
        String path = ImageLoader.getLocalPathFromUri(this, uri);
        if (path != null) {
            Uri localUri = Uri.parse(path);
            path = localUri.getLastPathSegment();
        } else {
            path = uri.getLastPathSegment();
        }

        PrintHelper printer = new PrintHelper(this);
        try {
            printer.printBitmap(path, uri);
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "Error printing an image", fnfe);
        }
    }

    ///M: add for MAV @{
    protected SensorManager mSensorManager;
    protected Sensor mGyroSensor;
    protected boolean mHasGyroSensor;
    protected Display mDisplay;
    private final Object mSyncObj = new Object();
    public static final float UNUSABLE_ANGLE_VALUE = -1;
    private void initGyroSensor() {
        mSensorManager = (SensorManager)this.getAndroidContext()
                                .getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mHasGyroSensor = (mGyroSensor != null);
        if (!mHasGyroSensor) {
            // TODO 
            // show MavSeekBar if has gyro sensor
            // hide MavSeekBar if no gyro sensor,
            // maybe this action should be do when onProgressChanged()
            MtkLog.d(TAG, "not has gyro sensor");
        }
        
        mDisplay = ((Activity)this).getWindowManager().getDefaultDisplay();
    }
    
    private void registerGyroSensorListener() {
        if (mHasGyroSensor) {
            MtkLog.d(TAG, "register gyro sensor listener");
            mSensorManager.registerListener(mPositionListener, mGyroSensor,
                SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void unregisterGyroSensorListener() {
        if (mHasGyroSensor) {
            MtkLog.d(TAG, "unregister gyro listener");
            mSensorManager.unregisterListener(mPositionListener);
        }
    }
    private PositionListener mPositionListener = new PositionListener();

    public class PositionListener implements SensorEventListener {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {    
            onGyroSensorChanged(event);
            /*switch (event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE: {
                    onGyroscopeChanged(
                            event.values[0], event.values[1], event.values[2]);
                    break;
                }
                case Sensor.TYPE_ACCELEROMETER: {
                    onAccelerometerChanged(
                            event.values[0], event.values[1], event.values[2]);
                }
            }*/
        }
    }

    public void onGyroSensorChanged(SensorEvent event) {
        synchronized(mSyncObj) {
            if (mListener != null) {
            float angle = mListener.onCalculateAngle(event);
            if (angle != UNUSABLE_ANGLE_VALUE) {
                mListener.onGyroPositionChanged(angle);
                }
            }
        }
    }
 
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    private GyroPositionListener mListener;

    public interface GyroPositionListener {
        public float onCalculateAngle(SensorEvent event);

        public void onGyroPositionChanged(float angle);
    }
    
    public void setGyroPositionListener(GyroPositionListener gyroPositionListener) {
        synchronized(mSyncObj) {
            registerGyroSensorListener();
            mListener = gyroPositionListener;
            }
    }

    public void removeGyroPositionListener(GyroPositionListener gyroPositionListener) {
        synchronized(mSyncObj) {
            if (gyroPositionListener == null ||(mListener != null && mListener == gyroPositionListener)) {
                unregisterGyroSensorListener();
                mListener = null;
                }
            }
    }

    public boolean hasGyroSensor() {
        return mHasGyroSensor;
    }

    public Display getDisplay() {
        if (mDisplay == null) {
            mDisplay = ((Activity)this).getWindowManager().getDefaultDisplay();
        } 
        return mDisplay;
    }
    ///@}
    // / M: added for HotKnot @{
    private HotKnotAdapter mHotKnotAdapter = null;
    private Uri[] mHotKnotUris = null;
    private Activity mContext = null;
    private boolean mHotKnotEnable = false;
    private AlertDialog mHotKnotDialog = null;
    private Toast mHotKnotToast = null;
    MenuItem mHotKnotItem = null;
    private HotknotCompleteListener mHotKnotListener = null;
    private boolean mHotKnotWaitSend = false;
    
    private void hotKnotInit(Activity activity) {
        Log.d(TAG, "hotKnotInit");
        mContext = activity;
        mHotKnotAdapter = HotKnotAdapter.getDefaultAdapter(mContext);
        if (mHotKnotAdapter == null) {
            mHotKnotEnable = false;
            Log.d(TAG, "hotKnotInit, mHotKnotAdapter is null, disable hotKnot feature");
            return;
        }
        mHotKnotEnable = true;
        mHotKnotAdapter.setOnHotKnotCompleteCallback(
                new HotKnotAdapter.OnHotKnotCompleteCallback() {
                    public void onHotKnotComplete(int reason) {
                        Log.d(TAG, "onHotKnotComplete reason:" + reason);
                        mHotKnotAdapter.setHotKnotBeamUris(null, mContext);
                        if (mHotKnotListener != null) mHotKnotListener.onHotKnotSendComplete();
                    }
                }, mContext);
        OnClickListener onClick = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (DialogInterface.BUTTON_POSITIVE == which) {
                    Log.d(TAG, "hotKnot start setting");
                    Intent intent = new Intent(
                            "mediatek.settings.HOTKNOT_SETTINGS");
                    startActivity(intent);
                    dialog.cancel();
                } else {
                    Log.d(TAG, "onClick cancel dialog");
                    dialog.cancel();
                }
            }
        };
        mHotKnotDialog = new AlertDialog.Builder(mContext)
                .setMessage(R.string.turn_on_hotknot)
                .setNegativeButton(android.R.string.cancel, onClick)
                .setPositiveButton(android.R.string.ok, onClick).create();
    }

    public boolean hotKnotIsEnable() {
        if (MediatekFeature.isHotKnotSupported() && mHotKnotEnable) {
            return true;
        } else {
            return false;
        }
    }

    public void hotKnotSetUris(Uri[] uris) {
        Log.d(TAG, "hotKnotSetUris");
        if (uris != null) {
            for (Uri uri : uris) {
                Log.d(TAG, "HotKnot uri:" + uri);
            }
        }
        mHotKnotUris = uris;
    }

    public void hotKnotStart() {
        Log.d(TAG, "hotKnotStart");

        if (mHotKnotAdapter.isEnabled()) {
            Log.d(TAG, "hotKnotAdapter is Enable");
            mHotKnotAdapter.setHotKnotBeamUris(mHotKnotUris, mContext);
            if (mHotKnotToast == null) 
                mHotKnotToast = Toast.makeText(mContext, R.string.hotknot_toast, Toast.LENGTH_SHORT);
            if (mHotKnotUris == null) {
                mHotKnotToast.cancel();
            } else {
                mHotKnotToast.show();
            }
            return;
        }

        if (mHotKnotDialog != null) {
            Log.d(TAG, "hotKnotStart show dialog");
            mHotKnotDialog.show();
        }
    }

    public void hotKnotSend(Uri uri) {
        Log.d(TAG, "hotKnotSend:" + uri);
        if (uri == null) {
            hotKnotSetUris(null);
            hotKnotStart();
        } else {
            Uri uris[] = new Uri[1];
            uris[0] = uri;
            hotKnotSetUris(uris);
            hotKnotStart();
        }
    }

    public void hotKnotUpdateMenu(Menu menu, int shareAction, int hotKnotAction) {
        if (menu == null) {
            Log.d(TAG, "hotKnotUpdateMenu: menu is null");
            return;
        }
        mHotKnotItem = menu.findItem(hotKnotAction);
        MenuItem shareItem = menu.findItem(shareAction);
        boolean enable = hotKnotIsEnable();
        Log.d(TAG, "hotKnotUpdateMenu, Enable:" + enable);

        if (mHotKnotItem != null && shareItem != null) {
            mHotKnotItem.setVisible(enable);
            ((ActivityChooserView) shareItem.getActionView())
                    .setRecentButtonEnabled(!enable);
            Log.d(TAG, "hotKnotUpdateMenu, success");
        }
    }

    public void hotKnotShowIcon(boolean enable) {
        if (mHotKnotItem != null && hotKnotIsEnable()) {
            mHotKnotItem.setEnabled(enable);
            mHotKnotItem.setVisible(enable);
            Log.d(TAG, "hotKnotShowIcon:" + enable);
        }
    }
    
    public boolean hotKnotAdapterIsEnable() {
        if(mHotKnotAdapter != null){
            return mHotKnotAdapter.isEnabled();
        }
        return false;
    }
    
    public void hotKnotDismissDialog() {
        if (mHotKnotDialog != null) {
            mHotKnotDialog.dismiss();
        }
    }

    public static interface HotknotCompleteListener {
        public void onHotKnotSendComplete();
    }
    
    public void setHotknotCompleteListener(HotknotCompleteListener listener) {
        mHotKnotListener = listener;
        Log.d(TAG, "setHotknotCompleteListener:" + mHotKnotListener);
    }
}
