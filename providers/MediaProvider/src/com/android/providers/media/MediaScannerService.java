/* //device/content/providers/media/src/com/android/providers/media/MediaScannerService.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.providers.media;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.IMediaScannerListener;
import android.media.IMediaScannerService;
import android.media.MediaScanner;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class MediaScannerService extends Service implements Runnable
{
    private static final String TAG = "MediaScannerService";
    private static final boolean LOG = true;
    
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private PowerManager.WakeLock mWakeLock;
    private String[] mExternalStoragePaths;
    /// M: MediaScanner Performance turning {@
    /// Add for MediaScanner performance enhance feature.
    private static final int MESSAGE_SCAN = 1;
    private static final int MESSAGE_SHUTDOWN_THREADPOOL = 2;
    private static final int MESSAGE_SCAN_FINISH_WITH_THREADPOOL = 3;
    /// @}
    
    private void openDatabase(String volumeName) {
        try {
            ContentValues values = new ContentValues();
            values.put("name", volumeName);
            getContentResolver().insert(Uri.parse("content://media/"), values);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "failed to open media database");
        }
    }

    private MediaScanner createMediaScanner() {
        MediaScanner scanner = new MediaScanner(this);
        Locale locale = getResources().getConfiguration().locale;
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String localeString = null;
            if (language != null) {
                if (country != null) {
                    scanner.setLocale(language + "_" + country);
                } else {
                    scanner.setLocale(language);
                }
            }
        }
        
        return scanner;
    }

    private void scan(String[] directories, String volumeName) {
        MtkLog.d(TAG, "scan>>>: volumeName = " + volumeName + ", directories = " + Arrays.toString(directories));
        Uri uri = Uri.parse("file://" + directories[0]);
        // don't sleep while scanning
        mWakeLock.acquire();

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MEDIA_SCANNER_VOLUME, volumeName);
            Uri scanUri = getContentResolver().insert(MediaStore.getMediaScannerUri(), values);

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_STARTED, uri));

            try {
                if (volumeName.equals(MediaProvider.EXTERNAL_VOLUME)) {
                    openDatabase(volumeName);
                }

                MediaScanner scanner = createMediaScanner();
                scanner.scanDirectories(directories, volumeName);
            } catch (Exception e) {
                Log.e(TAG, "exception in MediaScanner.scan()", e);
            }

            getContentResolver().delete(scanUri, null, null);

        } catch (Exception ex) {
            Log.e(TAG, "exception in MediaScanner.scan()", ex);
        } finally {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_FINISHED, uri));
            mWakeLock.release();
        }
        MtkLog.d(TAG, "scan<<<");
    }
    
    @Override
    public void onCreate()
    {
        MtkLog.d(TAG, "onCreate: CpuCoreNum = " + getCpuCoreNum() + ", isLowRamDevice = " + isLowRamDevice());
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        StorageManager storageManager = (StorageManager)getSystemService(Context.STORAGE_SERVICE);
        mExternalStoragePaths = storageManager.getVolumePaths();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        Thread thr = new Thread(null, this, "MediaScannerService");
        thr.start();

        /// Register a unmount receiver to make sure pre-scan again when sdcard unmount at scanning.
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        registerReceiver(mUnmountReceiver, filter);

        mIsThreadPoolEnable = getCpuCoreNum() >= 4 && !isLowRamDevice();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        while (mServiceHandler == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    MtkLog.e(TAG, "onStartCommand: InterruptedException!");
                }
            }
        }

        if (intent == null) {
            Log.e(TAG, "Intent is null in onStartCommand: ",
                new NullPointerException());
            return Service.START_NOT_STICKY;
        }

        Message msg = mServiceHandler.obtainMessage(MESSAGE_SCAN);
        msg.arg1 = startId;
        msg.obj = intent.getExtras();
        mServiceHandler.sendMessage(msg);

        // Try again later if we are killed before we can finish scanning.
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy()
    {
        MtkLog.d(TAG, "onDestroy");
        // Make sure thread has started before telling it to quit.
        while (mServiceLooper == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    MtkLog.e(TAG, "onDestroy: InterruptedException!");
                }
            }
        }
        mServiceLooper.quit();

        /// M: MediaScanner Performance turning {@
        /// If service has destroyed, we need release wakelock.
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
            MtkLog.w(TAG, "onDestroy: release wakelock when service destroy");
        }
        /// @}

        /// M: register at onCreate and unregister at onDestory
        unregisterReceiver(mUnmountReceiver);
    }

    public void run()
    {
        // reduce priority below other background threads to avoid interfering
        // with other services at boot time.
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND +
                Process.THREAD_PRIORITY_LESS_FAVORABLE);
        Looper.prepare();

        mServiceLooper = Looper.myLooper();
        mServiceHandler = new ServiceHandler();

        Looper.loop();
    }
   
    private Uri scanFile(String path, String mimeType) {
        String volumeName = MediaProvider.EXTERNAL_VOLUME;
        openDatabase(volumeName);
        MediaScanner scanner = createMediaScanner();
        try {
            // make sure the file path is in canonical form
            String canonicalPath = new File(path).getCanonicalPath();
            return scanner.scanSingleFile(canonicalPath, volumeName, mimeType);
        } catch (Exception e) {
            Log.e(TAG, "bad path " + path + " in scanFile()", e);
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }
    
    private final IMediaScannerService.Stub mBinder = 
            new IMediaScannerService.Stub() {
        public void requestScanFile(String path, String mimeType, IMediaScannerListener listener)
        {
            if (LOG) {
                Log.d(TAG, "IMediaScannerService.scanFile: " + path + " mimeType: " + mimeType);
            }
            Bundle args = new Bundle();
            args.putString("filepath", path);
            args.putString("mimetype", mimeType);
            if (listener != null) {
                args.putIBinder("listener", listener.asBinder());
            }
            startService(new Intent(MediaScannerService.this,
                    MediaScannerService.class).putExtras(args));
        }

        public void scanFile(String path, String mimeType) {
            requestScanFile(path, mimeType, null);
        }
    };

    private final class ServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            /// M: MediaScanner Performance turning {@
            /// Add two message for shutdown threadpool
            /// and handle scan finish request.
            switch (msg.what) {
                case MESSAGE_SCAN:
                    handleScanRequest(msg);
                    break;

                case MESSAGE_SHUTDOWN_THREADPOOL:
                    handleShutdownThreadpool(msg.arg1);
                    break;

                case MESSAGE_SCAN_FINISH_WITH_THREADPOOL:
                    handleScanFinish();
                    break;

                default:
                    Log.w(TAG, "unsupport message " + msg.what);
                    break;
            }
            /// @}
        }
    };

    private void handleScanRequest(Message msg) {
            Bundle arguments = (Bundle) msg.obj;
            String filePath = arguments.getString("filepath");
            MtkLog.d(TAG, "handleMessage: what = " + msg.what + ", startId = " + msg.arg1 + ", arguments = " + arguments.toString());
            try {
                if (filePath != null) {
                    IBinder binder = arguments.getIBinder("listener");
                    IMediaScannerListener listener = 
                            (binder == null ? null : IMediaScannerListener.Stub.asInterface(binder));
                    Uri uri = null;
                    try {
                        uri = scanFile(filePath, arguments.getString("mimetype"));
                        /// M: If file path is a directory we need scan the folder.{@
                        File file = new File(filePath);
                        if (file.isDirectory()) {
                            scan(new String[] {filePath}, MediaProvider.EXTERNAL_VOLUME);
                        }
                        /// @}
                    } catch (Exception e) {
                        Log.e(TAG, "Exception scanning file", e);
                    }
                    if (listener != null) {
                        listener.scanCompleted(filePath, uri);
                    }
                } else {
                    String volume = arguments.getString("volume");
                    String[] directories = null;
                    
                    if (MediaProvider.INTERNAL_VOLUME.equals(volume)) {
                        // scan internal media storage
                        directories = new String[] {
                                Environment.getRootDirectory() + "/media",
                        };
                    }
                    else if (MediaProvider.EXTERNAL_VOLUME.equals(volume)) {
                        // scan external storage volumes
                        directories = mExternalStoragePaths;
                       /// M: if MTK_SUPPORT_OWNER_SDCARD_ONLY_SUPPORT User proccess don't need to scan external 
                       /// sdcard except primary external sdcard @{
                        if(MediaFeatureOption.IS_SUPPORT_OWNER_SDCARD_ONLY_SUPPORT
                           && UserHandle.myUserId() != UserHandle.USER_OWNER) {
                           	directories = new String[] {mExternalStoragePaths[0]};
                        }
                       /// @}
                    /// M: MediaScanner Performance turning {@
                    /// Thread pool enable, use threadpool to scan.
                    if (mIsThreadPoolEnable) {
                        mStartId = msg.arg1;
                        scanWithThreadPool(directories, volume);
                        return;
                    }
                    /// @}
                    }

                    if (directories != null) {
                        if (LOG) Log.d(TAG, "start scanning volume " + volume + ": "
                                + Arrays.toString(directories));
                        scan(directories, volume);
                        if (LOG) Log.d(TAG, "done scanning volume " + volume);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in handleScanRequest", e);
            }

        /// M: MediaScanner Performance turning {@
        /// Only stop service when thread pool terminate
        if (mStartId != -1) {
            stopSelfResult(mStartId);
            mStartId = msg.arg1;
        } else {
            stopSelf(msg.arg1);
        }
        /// @}
    }

    /// M: MediaScanner Performance turning {@
    /// Add for MediaScanner performance enhancement feature, use thread pool to scan folders
    /// with multi-thread. We will parse all sub folder in directories and create a single
    /// new scanner to scan each folder one by one. We will scan these multi media folder(
    /// such as music, DCIM, Photo, Movie and so on) first to let user can see them in apps
    /// as soon as possible. {@

    /// M: Initial thread pool size
    private static final int CORE_POOL_SIZE = 3;
    /// M: Maximum thread pool size
    private static final int MAXIMUM_POOL_SIZE = CORE_POOL_SIZE;
    /// M: Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 2;
    /// M: Sets the Time Unit to seconds
    private final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private final LinkedBlockingQueue<Runnable> mWorkQueue = new LinkedBlockingQueue<Runnable>();
    /// M: Use customers thread factory to rename thread name and modify thread priority.
    private final ThreadFactory mThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "Scan-thread#" + mCount.getAndIncrement());
        }
    };
    /// M: Use to mark special scan path, we will scan these single file in directory and
    /// empty folder one by one with scan single file method in mediascanner.
    private static final String PREFIX_SINGLEFILE_OR_EMPTYFOLDER = "singlefile_or_emtpyfolder_";
    /// M: First scan folders(Music, Photo, Picture, DCIM, Movie, Video)
    private static final String DIRECTORY_MUSIC = Environment.DIRECTORY_MUSIC.toLowerCase(Locale.US);
    private static final String DIRECTORY_PHOTO = "photo";
    private static final String DIRECTORY_PICTURES = Environment.DIRECTORY_PICTURES.toLowerCase(Locale.US);
    private static final String DIRECTORY_DCIM = Environment.DIRECTORY_DCIM;
    private static final String DIRECTORY_MOVIES = Environment.DIRECTORY_MOVIES.toLowerCase(Locale.US);
    private static final String DIRECTORY_VIDEO = "video";

    private MediaScannerThreadPool mMediaScannerThreadPool;
    /// M: This MediaScanner use to do pre-scan before create thread pool and post-scan after
    /// thread pool terminate(finish scan).
    private MediaScanner mPreScanner;
    /// M: Only when device is not low ram device and it's cpu core num big than 4 need enable thread pool to scan.
    private boolean mIsThreadPoolEnable = false;
    /// M: Start mediascanner service id, when scan finish with thread pool we need stop
    /// service with this id.
    private int mStartId = -1;
    /// M: use them to restore scan times.
    private long mScanStartTime;
    private long mPreScanFinishTime;
    private long mScanFinishTime;
    private long mPostScanFinishTime;

    /// M: If sdcard unmount when scanning, we need do pre-scan after scan finish to clear non exist entries.
    private boolean mNeedPreScanAgain = false;
    /// M: Mark need prescan again when user unmount sdcard while scanning to clear not exist files.
    private BroadcastReceiver mUnmountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                mNeedPreScanAgain = true;
                MtkLog.v(TAG, "MediaScanner need prescan again after scan finish.");
            }
        }
    };

    /// M: Every scanner scan finish will return scanned playlist files path and store here, when scan finish
    /// we process them in postScanAll.
    private ArrayList<String> mPlaylistFilePathList = new ArrayList<String>();

    /// M: Store these scan paths which need execute when all task finish. because when new scan request coming,
    /// finished task will be scanned again, and wait scan task need not scan again, only scanning task need to
    /// scan at last to avoid two thread scan same folder and cause hanger competition.
    private ArrayList<String> mLastExecuteTaskList = new ArrayList<String>(CORE_POOL_SIZE);

    /**
     * M: Scan given directories with thread pool.
     * 
     * @param directories need scan directories.
     * @param volume external or internal.
     */
    private void scanWithThreadPool(String[] directories, String volume) {
        MtkLog.v(TAG, "scanWithThreadPool>>> pool size " + MAXIMUM_POOL_SIZE);
        /// don't sleep while scanning
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            MtkLog.v(TAG,"acquire wakelock to avoid sleeping while scanning with threadpool");
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.MEDIA_SCANNER_VOLUME, volume);
        getContentResolver().insert(MediaStore.getMediaScannerUri(), values);

        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_STARTED, Uri.parse("file://" + mExternalStoragePaths[0])));
        openDatabase(volume);

        /// Parse scan paths
        ArrayList<String> scanPaths = parseScanPaths(directories);

        /// If prescanner not init, create a new one.
        if (mPreScanner == null) {
            mPreScanner = createMediaScanner();
        }

        /// If thread pool don't init, create a new one
        if (mMediaScannerThreadPool == null || mMediaScannerThreadPool.isShutdown()) {
            mScanStartTime = System.currentTimeMillis();
            /// Prescan all before scan folders and postscan all when scan finish
            mPreScanner.preScanAll(volume);
            mPreScanFinishTime = System.currentTimeMillis();
            MtkLog.v(TAG, "preScanAll before scan folders with threadpool");
            /// Create thread pool to scan all folders
            mMediaScannerThreadPool = new MediaScannerThreadPool();
        }

        /// Remove duplicate paths whose scan task has existed in thread pool
        removeDuplicatePaths(scanPaths);

        /// Execute scan task for each path
        MtkLog.v(TAG, "scanWithThreadPool: size = " + scanPaths.size() + ", scanPaths = " + scanPaths.toString());
        for (String path : scanPaths) {
            mMediaScannerThreadPool.execute(new ScanTask(path, volume));
        }
        MtkLog.v(TAG, "scanWithThreadPool finished create thread pool to scan");
    }

    private int getCpuCoreNum() {
        return Runtime.getRuntime().availableProcessors();
    }

    private boolean isLowRamDevice() {
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        return am.isLowRamDevice();
    }
    private void removeDuplicatePaths(ArrayList<String> scanPaths) {
        /// Remove duplicate paths which wait to be scanned, so that threadpool will not scan same folder twice.
        scanPaths.removeAll(mMediaScannerThreadPool.mWaitTaskList);
        MtkLog.v(TAG, "scanWithThreadPool: remove duplicate wait to scan paths " + mMediaScannerThreadPool.mWaitTaskList);

        /// Store duplicate paths which has been scanning and scan them again when all task finish so that
        /// threadpool will scan all files(because when user plug out/in sdcard persistently, same scanning
        /// folder may not been scanned finish and need scan again).
        mLastExecuteTaskList.clear();
        mLastExecuteTaskList.addAll(mMediaScannerThreadPool.mWorkTaskList);
        scanPaths.removeAll(mLastExecuteTaskList);
        MtkLog.v(TAG, "scanWithThreadPool: store duplicate work scanning paths " + mLastExecuteTaskList);
    }

    private boolean isWaitScanedPathInThreadPool(String path) {
        boolean isWaitScaned = false;
        for (Runnable run : mMediaScannerThreadPool.getQueue()) {
            String runPath = ((ScanTask) run).getScanPath();
            if (path.startsWith(runPath)) {
                isWaitScaned = true;
                break;
            }
        }
        MtkLog.v(TAG, "isWaitScanedPathInThreadPool for " + path + ": " + isWaitScaned);
        return isWaitScaned;
    }

    /**
     * M: Parse need scan paths from given directories. All sub folders in these directories
     * is a scan task except empty folder. Empty folder and single file will be combine to
     * be one path and scan in one task with special scan method.
     * We will also scan some important multi-media folders first such as Music, Photo, Video
     * DCIM, Pictures, Movies so that user can see them in apps(Music, Galley, VideoPlayer)
     * as soon as possible.
     * 
     * @param directories Given scan directories.
     * @return Need scan paths
     */
    private ArrayList<String> parseScanPaths(String[] directories) {
        long start = System.currentTimeMillis();
        /// Get need scan paths
        ArrayList<String> scanPaths = new ArrayList<String>();
        ArrayList<String> emptyFolderOrSingleFileList = new ArrayList<String>();
        int index = 0;
        for (String directorie : directories) {
            File directorieFile = new File(directorie);
            File[] subFiles = directorieFile.listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    String[] fileList = subFile.list();
                    if (subFile.isDirectory() && fileList != null && fileList.length > 0) {
                        /// Add special path to beginning of list(Music, DCIM, Pictures, Photo, Movies, Video)
                        String path = subFile.getPath();
                        if (path.contains(DIRECTORY_DCIM)
                                || path.toLowerCase(Locale.US).contains(DIRECTORY_PICTURES)
                                || path.toLowerCase(Locale.US).contains(DIRECTORY_MOVIES)
                                || path.toLowerCase(Locale.US).contains(DIRECTORY_MUSIC)
                                || path.toLowerCase(Locale.US).contains(DIRECTORY_PHOTO)
                                || path.toLowerCase(Locale.US).contains(DIRECTORY_VIDEO)) {
                            scanPaths.add(index, path);
                            index++;
                        } else {
                            scanPaths.add(path);
                        }
                    } else {
                        emptyFolderOrSingleFileList.add(subFile.getPath());
                    }
                }
            }
        }
        /// Add empty folder or single file in directory path to scan paths, these paths
        /// will use scan single file method to scan, so we need add a prefix 
        /// PREFIX_SINGLEFILE_OR_EMPTYFOLDER to mark it. Because ArrayList.toString will 
        /// add these object in the list to "[]" and split them with ", ", we need get
        /// substring to add scanPaths to scan.
        String empty = emptyFolderOrSingleFileList.toString();
        empty = PREFIX_SINGLEFILE_OR_EMPTYFOLDER + empty.substring(1, empty.length() - 1);
        scanPaths.add(empty);
        MtkLog.v(TAG, "parseScanPaths cost " + (System.currentTimeMillis() - start) + "ms");
        return scanPaths;
    }

    private void handleShutdownThreadpool(int taskCount) {
        try {
            if (mMediaScannerThreadPool != null) {
                // / Execute some tasks to scan again before shutdown.
                MtkLog.v(TAG, "Before shutdown execute last scan paths " + mLastExecuteTaskList);
                for (String path : mLastExecuteTaskList) {
                    mMediaScannerThreadPool.execute(new ScanTask(path, MediaProvider.EXTERNAL_VOLUME));
                }
                mLastExecuteTaskList.clear();

                // / Shutdown threadpool so that mediascanner threadpool will be terminated after
                // all task finish.
                mMediaScannerThreadPool.shutdown();
                mMediaScannerThreadPool = null;
                MtkLog.v(TAG, "All task complete(" + taskCount + "), shutdown thread pool!");
            }
        } catch (Exception e) {
            MtkLog.e(TAG, "Exception in handleShutdownThreadpool", e);
        }
    }

    private void handleScanFinish() {
        try {
            /// After scan finish we need postscan. 
            mScanFinishTime = System.currentTimeMillis();
            /// If user unmount sdcard while scanning we need to prescan again to clear non-reference entries.
            if (mNeedPreScanAgain) {
                mPreScanner.preScanAll(MediaProvider.EXTERNAL_VOLUME);
                mNeedPreScanAgain = false;
                MtkLog.d(TAG, "preScanAll again because sdcard unmount while scanning.");
            }
            mPreScanner.postScanAll(mPlaylistFilePathList);
            MtkLog.d(TAG, "postScanAll with playlist files list " + mPlaylistFilePathList);
            synchronized (mPlaylistFilePathList) {
                mPlaylistFilePathList.clear();
            }
            mPreScanner = null;
            getContentResolver().delete(MediaStore.getMediaScannerUri(), null, null);
        } catch (Exception e) {
            MtkLog.e(TAG, "Exception in handleScanFinish", e);
        }

        mPostScanFinishTime = System.currentTimeMillis();

        /// When thread pool terminate, we need notify app, release wakelock and stop scan service
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_FINISHED, Uri.parse("file://" + mExternalStoragePaths[0])));
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
         }

        MtkLog.d(TAG, " prescan time: " + (mPreScanFinishTime - mScanStartTime) + "ms\n");
        MtkLog.d(TAG, "    scan time: " + (mScanFinishTime - mPreScanFinishTime) + "ms\n");
        MtkLog.d(TAG, "postscan time: " + (mPostScanFinishTime - mScanFinishTime) + "ms\n");
        MtkLog.d(TAG, "scan exteranl with thread pool cost " + (mPostScanFinishTime - mScanStartTime) + "ms");
        MtkLog.v(TAG, "scanWithThreadPool<<< finish scan so release wakelock and send scan finish intent");
        /// Stop service
        stopSelfResult(mStartId);
        mStartId= -1;
    }
    /**
     * M: Every scan task will scan the given path
     *
     */
    private class ScanTask implements Runnable {
        /// M: scan path
        private final String mPath;
        /// M: external or internal
        private final String mVolume;

        public ScanTask(String scanPath, String volume) {
            mPath = scanPath;
            mVolume = volume;
        }

        public String getScanPath() {
            return mPath;
        }

        @Override
        public void run() {
            MtkLog.v(TAG, "scan  start in " + Thread.currentThread().getName() + ": " + mPath);
            /// M: reduce thread priority below other background threads to avoid interfering
            /// with other services at boot time.
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_LESS_FAVORABLE);
            long startTime = System.currentTimeMillis();
            try {
                MediaScanner scanner = createMediaScanner();
                boolean isSingelFileOrEmptyFolder = false;
                String realPath = mPath;
                if (realPath.startsWith(PREFIX_SINGLEFILE_OR_EMPTYFOLDER)) {
                    realPath = realPath.substring(PREFIX_SINGLEFILE_OR_EMPTYFOLDER.length());
                    isSingelFileOrEmptyFolder = true;
                }
                String[] scanPath = realPath.split(", ");
                ArrayList<String> playlist = scanner.scanFolders(scanPath, mVolume, isSingelFileOrEmptyFolder);
                synchronized (mPlaylistFilePathList) {
                    mPlaylistFilePathList.addAll(playlist);
                }
           } catch (Exception e) {
                Log.e(TAG, "exception in MediaScanner scan " + mPath, e);
           }
            MtkLog.v(TAG, "scan finsih in " + Thread.currentThread().getName() + ": " + mPath
                    + " cost " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    /**
     * M: MediaScanner customize thread pool.
     */
    private class MediaScannerThreadPool extends ThreadPoolExecutor {
        /// Store wait to scan task
        private Vector<String> mWaitTaskList = new Vector<String>();
        /// Store has been working scan task
        private Vector<String> mWorkTaskList = new Vector<String>();

        public MediaScannerThreadPool() {
            super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,
                    mWorkQueue, mThreadFactory);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            /// Move the next scan path from wait task list to work task list
            String nextScanPath = ((ScanTask)r).getScanPath();
            mWaitTaskList.remove(nextScanPath);
            mWorkTaskList.add(nextScanPath);
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            /// When all scan task finish, shutdown thread pool and then it will be terminated
            long complete = getCompletedTaskCount() + 1;
            long total = getTaskCount();
            if (complete == total) {
                Message msg = mServiceHandler.obtainMessage(MESSAGE_SHUTDOWN_THREADPOOL, (int)total, -1);
                mServiceHandler.sendMessage(msg);
            }
            /// Remove from work task list when task finish.
            mWorkTaskList.remove(((ScanTask)r).getScanPath());
            super.afterExecute(r, t);
        }

        @Override
        public void execute(Runnable command) {
            /// Store all execute task in wait task list
            mWaitTaskList.add(((ScanTask)command).getScanPath());
            super.execute(command);
        };

        @Override
        protected void terminated() {
            mServiceHandler.sendEmptyMessage(MESSAGE_SCAN_FINISH_WITH_THREADPOOL);
            super.terminated();
        }
    }
    /// @}
}

