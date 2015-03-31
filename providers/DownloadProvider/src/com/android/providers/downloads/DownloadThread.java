/*
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

package com.android.providers.downloads;

import static android.provider.Downloads.Impl.STATUS_BAD_REQUEST;
import static android.provider.Downloads.Impl.STATUS_CANNOT_RESUME;
import static android.provider.Downloads.Impl.STATUS_FILE_ERROR;
import static android.provider.Downloads.Impl.STATUS_HTTP_DATA_ERROR;
import static android.provider.Downloads.Impl.STATUS_SUCCESS;
import static android.provider.Downloads.Impl.STATUS_TOO_MANY_REDIRECTS;
import static android.provider.Downloads.Impl.STATUS_WAITING_FOR_NETWORK;
import static android.provider.Downloads.Impl.STATUS_WAITING_TO_RETRY;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.android.providers.downloads.Constants.TAG;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

//import android.drm.DrmManagerClient;
import android.drm.DrmRights;
//import android.drm.DrmUtils;
import com.mediatek.drm.OmaDrmClient;
import android.media.MediaScannerConnection;

import android.drm.DrmManagerClient;
import android.drm.DrmOutputStream;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyListener;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.net.TrafficStats;
import android.net.http.HttpAuthHeader;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.os.WorkSource;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import android.util.Slog;

import com.mediatek.downloadmanager.ext.Extensions;
import com.mediatek.downloadmanager.ext.IDownloadProviderFeatureEx;
import com.mediatek.xlog.Xlog;

import com.android.providers.downloads.DownloadInfo.NetworkState;

import libcore.io.IoUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.Header;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Locale;
import org.apache.http.Header;

/**
 * Task which executes a given {@link DownloadInfo}: making network requests,
 * persisting data to disk, and updating {@link DownloadProvider}.
 */
public class DownloadThread implements Runnable {

    // TODO: bind each download to a specific network interface to avoid state
    // checking races once we have ConnectivityManager API
    private IDownloadProviderFeatureEx mDownloadProviderFeatureEx;

    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private static final int HTTP_TEMP_REDIRECT = 307;

    private static final int DEFAULT_TIMEOUT = (int) (60 * SECOND_IN_MILLIS);

    private final Context mContext;
    private final DownloadInfo mInfo;
    private final SystemFacade mSystemFacade;
    private final StorageManager mStorageManager;
    private final DownloadNotifier mNotifier;

    private volatile boolean mPolicyDirty;

    /// M: Add for fix GMS low memory issue 332710. @{
    private static final String PLAY_STORE_RECEIVER = "com.google.android.finsky."
            + "download.DownloadBroadcastReceiver";
    private static final String PLAY_STORE_CLASS = "com.android.vending";
    /// @}
    
    public DownloadThread(Context context, SystemFacade systemFacade, DownloadInfo info,
            StorageManager storageManager, DownloadNotifier notifier) {
        mContext = context;
        mSystemFacade = systemFacade;
        mInfo = info;
        mStorageManager = storageManager;
        mNotifier = notifier;
    }

    /**
     * Returns the user agent provided by the initiating app, or use the default one
     */
    private String userAgent() {
        String userAgent = mInfo.mUserAgent;
        if (userAgent == null) {
            userAgent = Constants.DEFAULT_USER_AGENT;
        }
        return userAgent;
    }

    /**
     * State for the entire run() method.
     */
    static class State {
        public String mFilename;
        public String mMimeType;
        public int mRetryAfter = 0;
        public boolean mGotData = false;
        public String mRequestUri;
        public long mTotalBytes = -1;
        public long mCurrentBytes = 0;
        public String mHeaderETag;
        public boolean mContinuingDownload = false;
        public long mBytesNotified = 0;
        public long mTimeLastNotification = 0;
        public int mNetworkType = ConnectivityManager.TYPE_NONE;

        /** Historical bytes/second speed of this download. */
        public long mSpeed;
        /** Time when current sample started. */
        public long mSpeedSampleStart;
        /** Bytes transferred since current sample started. */
        public long mSpeedSampleBytes;

        public long mContentLength = -1;
        public String mContentDisposition;
        public String mContentLocation;

        public int mRedirectionCount;
        public URL mUrl;

        // M: Add to support OMA download
        public int mOmaDownload;
        public int mOmaDownloadStatus;
        public String mOmaDownloadInsNotifyUrl;

        // M: Add to support DRM
        public long mTotalWriteBytes = 0;
        
        public State(DownloadInfo info) {
            mMimeType = Intent.normalizeMimeType(info.mMimeType);
            mRequestUri = info.mUri;
            mFilename = info.mFileName;
            mTotalBytes = info.mTotalBytes;
            mCurrentBytes = info.mCurrentBytes;
            
            // Add to support OMA download
            mOmaDownload = info.mOmaDownload;
            mOmaDownloadStatus = info.mOmaDownloadStatus;
            mOmaDownloadInsNotifyUrl = info.mOmaDownloadInsNotifyUrl;
        }

        public void resetBeforeExecute() {
            // Reset any state from previous execution
            mContentLength = -1;
            mContentDisposition = null;
            mContentLocation = null;
            mRedirectionCount = 0;
        }
    }

    /// M: Add to support Authenticate download. @{
    private static class InnerState {
        public int mAuthScheme = HttpAuthHeader.UNKNOWN;
        public HttpAuthHeader mAuthHeader = null;
        public String mHost = null;
        public boolean mIsAuthNeeded = false;
        // M: As description on HttpHost, -1 means default port
        public int mPort = -1;
        public String mScheme = null;
    }
    /// @}
    
    @Override
    public void run() {
    	Log.i(TAG, "start run download thread");
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        try {
            runInternal();
        } finally {
            mNotifier.notifyDownloadSpeed(mInfo.mId, 0);
        }
    }

    private void runInternal() {
        // Skip when download already marked as finished; this download was
        // probably started again while racing with UpdateThread.
        if (DownloadInfo.queryDownloadStatus(mContext.getContentResolver(), mInfo.mId)
                == Downloads.Impl.STATUS_SUCCESS) {
            Log.d(TAG, "Download " + mInfo.mId + " already finished; skipping");
            return;
        }

        State state = new State(mInfo);
        PowerManager.WakeLock wakeLock = null;
        int finalStatus = Downloads.Impl.STATUS_UNKNOWN_ERROR;
        int numFailed = mInfo.mNumFailed;
        String errorMsg = null;

        final NetworkPolicyManager netPolicy = NetworkPolicyManager.from(mContext);
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        try {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.TAG);
            wakeLock.setWorkSource(new WorkSource(mInfo.mUid));
            wakeLock.acquire();

            // while performing download, register for rules updates
            netPolicy.registerListener(mPolicyListener);

            Log.i(Constants.TAG, "Download " + mInfo.mId + " starting ,currentThread id: " + 
            		Thread.currentThread().getId());

            // Remember which network this download started on; used to
            // determine if errors were due to network changes.
            final NetworkInfo info = mSystemFacade.getActiveNetworkInfo(mInfo.mUid);
            if (info != null) {
                state.mNetworkType = info.getType();
            }

            // Network traffic on this thread should be counted against the
            // requesting UID, and is tagged with well-known value.
            TrafficStats.setThreadStatsTag(TrafficStats.TAG_SYSTEM_DOWNLOAD);
            TrafficStats.setThreadStatsUid(mInfo.mUid);

            try {
                // TODO: migrate URL sanity checking into client side of API
                state.mUrl = new URL(state.mRequestUri);
            } catch (MalformedURLException e) {
                throw new StopRequestException(STATUS_BAD_REQUEST, e);
            }

            executeDownload(state);

            // M: Add this to support OMA DL. 
            // Need to before Handle DRM. Because if install notify failed,
            // the Media object will be discard
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT) {
                handleOmaDownloadMediaObject(state);
            }
            
            finalizeDestinationFile(state);
            
            /// M: Add this to support OMA DL.
            /// Deal with .dd file
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT) {
                handleOmaDownloadDescriptorFile(state);
            }
            
            finalStatus = Downloads.Impl.STATUS_SUCCESS;
            Xlog.i(Constants.DL_ENHANCE, "Download success" + mInfo.mUri + ",mInfo.mId: " 
                    + mInfo.mId + ",currentThread id: " + Thread.currentThread().getId());
        } catch (StopRequestException error) {
            // remove the cause before printing, in case it contains PII
            errorMsg = error.getMessage();
            String msg = "Aborting request for download " + mInfo.mId + ": " + 
            				",currentThread id: " + Thread.currentThread().getId() + errorMsg;
            Log.w(Constants.TAG, msg);
            if (Constants.LOGV) {
                Log.w(Constants.TAG, msg, error);
            }
            finalStatus = error.getFinalStatus();

            /// M: add cu feature. @{
            mDownloadProviderFeatureEx = Extensions.getDefault(mContext);
            if (mDownloadProviderFeatureEx.shouldNotifyFileAlreadyExist()) {
                if (finalStatus == Downloads.Impl.STATUS_FILE_ALREADY_EXISTS_ERROR) {
                    mInfo.notifyFileAlreadyExist(errorMsg);
                }
            }
            /// @}
            // Nobody below our level should request retries, since we handle
            // failure counts at this level.
            if (finalStatus == STATUS_WAITING_TO_RETRY) {
                throw new IllegalStateException("Execution should always throw final error codes");
            }

            // Some errors should be retryable, unless we fail too many times.
            if (isStatusRetryable(finalStatus)) {
                if (state.mGotData) {
                    numFailed = 1;
                } else {
                    numFailed += 1;
                }

                if (numFailed < Constants.MAX_RETRIES) {
                    final NetworkInfo info = mSystemFacade.getActiveNetworkInfo(mInfo.mUid);
                    if (info != null && info.getType() == state.mNetworkType
                            && info.isConnected()) {
                        // Underlying network is still intact, use normal backoff
                        finalStatus = STATUS_WAITING_TO_RETRY;
                    } else {
                        // Network changed, retry on any next available
                        finalStatus = STATUS_WAITING_FOR_NETWORK;
                    }
                }
            }

            /// M: Add for support OMA Download
            /// Notify to web server if failed @{
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT
                    && ((errorMsg != null && errorMsg.equals(Downloads.Impl.OMADL_OCCUR_ERROR_NEED_NOTIFY))
                            || Downloads.Impl.isStatusError(finalStatus))
                    && state.mOmaDownload == 1 && state.mOmaDownloadInsNotifyUrl != null) {
                ///M: add to fix 1259679, do not to notify server. @{
                if (finalStatus == Downloads.Impl.STATUS_FILE_ALREADY_EXISTS_ERROR) {
                    return;
                }
                /// @}
                int notifyCode = OmaStatusHandler.SUCCESS;
                URL notifyUrl = null;
                try {
                    notifyUrl = new URL(state.mOmaDownloadInsNotifyUrl);
                } catch (MalformedURLException e) {
                    // TODO:need error handling
                    // There will update OMA_Download_Status, or the query will reuse
                    Xlog.e(Constants.LOG_OMA_DL, "DownloadThread: New notify url failed" + state.mOmaDownloadInsNotifyUrl);
                }
                switch (state.mOmaDownloadStatus) {
                    case Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DESCRIPTOR:
                        notifyCode = OmaStatusHandler.INVALID_DESCRIPTOR;
                        break;
                    case Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH:
                        notifyCode = OmaStatusHandler.ATTRIBUTE_MISMATCH;
                        break;
                    case Downloads.Impl.OMADL_STATUS_ERROR_INSUFFICIENT_MEMORY:
                        notifyCode = OmaStatusHandler.INSUFFICIENT_MEMORY;
                        break;
                    case Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DDVERSION:
                        notifyCode = OmaStatusHandler.INVALID_DDVERSION;
                        break;
                    default:
                        //notifyCode = OmaStatusHandler.DEVICE_ABORTED;
                        notifyCode = OmaStatusHandler.LOADER_ERROR;
                        break;
                }
                
                notifyOMADownloadWebServerErrorStatus(notifyUrl, notifyCode);
            }
            /// @}
            
            // fall through to finally block
        } catch (Throwable ex) {
            errorMsg = ex.getMessage();
            String msg = "Exception for id " + mInfo.mId + ": " + errorMsg;
            Log.w(Constants.TAG, msg, ex);
            /// M: falls through to the code that reports an error @{
            if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT && state.mOmaDownload == 1 
                    && state.mOmaDownloadInsNotifyUrl != null) {
                URL notifyUrl = null;
                try {
                    notifyUrl = new URL(state.mOmaDownloadInsNotifyUrl);
                } catch (MalformedURLException e) {
                    // TODO:need error handling
                    // There will update OMA_Download_Status, or the query will reuse
                    Xlog.e(Constants.LOG_OMA_DL, "DownloadThread: New notify url failed" + state.mOmaDownloadInsNotifyUrl);
                }
                notifyOMADownloadWebServerErrorStatus(notifyUrl, OmaStatusHandler.LOADER_ERROR);
            }
            /// @}
            finalStatus = Downloads.Impl.STATUS_UNKNOWN_ERROR;
            // falls through to the code that reports an error
        } finally {
            if (finalStatus == STATUS_SUCCESS) {
                TrafficStats.incrementOperationCount(1);
            }

            TrafficStats.clearThreadStatsTag();
            TrafficStats.clearThreadStatsUid();

            Xlog.d(Constants.DL_ENHANCE, "before cleanupDestination(), finalStauts : " + finalStatus);
            cleanupDestination(state, finalStatus);
            notifyDownloadCompleted(state, finalStatus, errorMsg, numFailed);

            /// M: Add for fix GMS low memory issue 332710. @{
            Xlog.d(Constants.DL_ENHANCE, "after notifyDownloadCompleted"
                    + " mInfo.mClass is: " + mInfo.mClass + " mInfo.mPackage "
                    + mInfo.mPackage + ",after cleanupDestination(), finalStatus: " + finalStatus
                    + " ,now state.mFilename = " + state.mFilename);
            if (finalStatus == Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR
                    && mInfo.mClass != null && mInfo.mPackage != null
                    && mInfo.mClass.equalsIgnoreCase(PLAY_STORE_RECEIVER)
                    && mInfo.mPackage.equalsIgnoreCase(PLAY_STORE_CLASS)) {
                mInfo.sendIntentIfRequested();
            }
            /// @}
            
            Log.i(Constants.TAG, "Download " + mInfo.mId + " finished with status "
                    + Downloads.Impl.statusToString(finalStatus));

            netPolicy.unregisterListener(mPolicyListener);

            if (wakeLock != null) {
                wakeLock.release();
                wakeLock = null;
            }
        }
        mStorageManager.incrementNumDownloadsSoFar();
    }

    /**
     * Fully execute a single download request. Setup and send the request,
     * handle the response, and transfer the data to the destination file.
     */
    private void executeDownload(State state) throws StopRequestException {
        InnerState innerState = new InnerState();
        state.resetBeforeExecute();
        setupDestinationFile(state);

        // skip when already finished; remove after fixing race in 5217390
        if (state.mCurrentBytes == state.mTotalBytes) {
            Log.i(Constants.TAG, "Skipping initiating request for download " +
                  mInfo.mId + "; already completed");
            return;
        }

        while (state.mRedirectionCount++ < Constants.MAX_REDIRECTS) {
            // Open connection and follow any redirects until we have a useful
            // response with body.
            HttpURLConnection conn = null;
            try {
                checkConnectivity();
                conn = (HttpURLConnection) state.mUrl.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);

                addRequestHeaders(state, conn);

                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                    case HTTP_OK:
                        if (state.mContinuingDownload) {
                        	/// M: add to retry url when got 200 code. @{
                            throw new StopRequestException(
                            		STATUS_HTTP_DATA_ERROR, "Expected partial, but received OK");
                            /// @}
                        }
                        processResponseHeaders(state, conn);
                        transferData(state, conn);
                        return;

                    case HTTP_PARTIAL:
                        if (!state.mContinuingDownload) {
                            throw new StopRequestException(
                                    STATUS_CANNOT_RESUME, "Expected OK, but received partial");
                        }
                        transferData(state, conn);
                        return;

                    case HTTP_MOVED_PERM:
                    case HTTP_MOVED_TEMP:
                    case HTTP_SEE_OTHER:
                    case HTTP_TEMP_REDIRECT:
                        final String location = conn.getHeaderField("Location");
                        state.mUrl = new URL(state.mUrl, location);
                        if (responseCode == HTTP_MOVED_PERM) {
                            // Push updated URL back to database
                            state.mRequestUri = state.mUrl.toString();
                        }
                        continue;

                    case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                        throw new StopRequestException(
                                STATUS_CANNOT_RESUME, "Requested range not satisfiable");

                    case HTTP_UNAVAILABLE:
                        parseRetryAfterHeaders(state, conn);
                        throw new StopRequestException(
                                HTTP_UNAVAILABLE, conn.getResponseMessage());

                    case HTTP_INTERNAL_ERROR:
                        throw new StopRequestException(
                                HTTP_INTERNAL_ERROR, conn.getResponseMessage());
                    
                    /// M: Add this to support Authenticate Download @{
                    case HTTP_UNAUTHORIZED:
                        if ((mInfo.mUsername != null || mInfo.mPassword != null) 
                                && innerState.mAuthScheme == HttpAuthHeader.UNKNOWN 
                                && innerState.mAuthHeader == null) {
                        	String headerAuthString = conn.getHeaderField("WWW-Authenticate");
                            if (headerAuthString != null) {
                                Xlog.d(Constants.DL_ENHANCE, "response.getFirstHeader WWW-Authenticate is: " 
                                        + headerAuthString);
                                //Using HttpAuthHeader parse Basic Auth.
                                //Only use first Auth header tag.
                                innerState.mAuthHeader = new HttpAuthHeader(headerAuthString);
                                
                                if (innerState.mAuthHeader != null) {
                                   if (innerState.mAuthHeader.getScheme() == HttpAuthHeader.BASIC)
                                   {
                                       innerState.mAuthScheme = HttpAuthHeader.BASIC;
                                   } else if (innerState.mAuthHeader.getScheme() ==  HttpAuthHeader.DIGEST) {
                                       innerState.mAuthScheme = HttpAuthHeader.DIGEST;
                                   }
                                   Xlog.d(Constants.DL_ENHANCE, "Auth scheme and mAuthHeader.scheme is  " 
                                           + innerState.mAuthScheme);
                                   innerState.mIsAuthNeeded = true;
                                   return;
                                }   
                            } 
                            
                        } else {
                            Xlog.w(Constants.DL_ENHANCE, "DownloadThread: handleExceptionalStatus:" +
                                    " 401, need Authenticate ");
                            throw new StopRequestException(Downloads.Impl.STATUS_NEED_HTTP_AUTH, "http error " + HTTP_UNAUTHORIZED);
                        }
                        //handleAuthenticate(state, response, statusCode);
                    /// @}    
                        
                    default:
                        StopRequestException.throwUnhandledHttpError(
                                responseCode, conn.getResponseMessage());
                }
            } catch (IOException e) {
                // Trouble with low-level sockets
                throw new StopRequestException(STATUS_HTTP_DATA_ERROR, e);

            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        throw new StopRequestException(STATUS_TOO_MANY_REDIRECTS, "Too many redirects");
    }

    /**
     * Transfer data from the given connection to the destination file.
     */
    private void transferData(State state, HttpURLConnection conn) throws StopRequestException {
        DrmManagerClient drmClient = null;
        InputStream in = null;
        OutputStream out = null;
        FileDescriptor outFd = null;
        try {
            try {
                in = conn.getInputStream();
            } catch (IOException e) {
                throw new StopRequestException(STATUS_HTTP_DATA_ERROR, e);
            }

            try {
                if (DownloadDrmHelper.isDrmConvertNeeded(state.mMimeType)) {
                    drmClient = new DrmManagerClient(mContext);
                    final RandomAccessFile file = new RandomAccessFile(
                            new File(state.mFilename), "rw");
                    out = new DrmOutputStream(drmClient, file, state.mMimeType);
                    outFd = file.getFD();
                } else {
                    out = new FileOutputStream(state.mFilename, true);
                    outFd = ((FileOutputStream) out).getFD();
                }
            } catch (IOException e) {
                throw new StopRequestException(STATUS_FILE_ERROR, e);
            }

            // Start streaming data, periodically watch for pause/cancel
            // commands and checking disk space as needed.
            transferData(state, in, out);

            try {
                if (out instanceof DrmOutputStream) {
                    ((DrmOutputStream) out).finish();
                }
            } catch (IOException e) {
                throw new StopRequestException(STATUS_FILE_ERROR, e);
            }

        } finally {
            if (drmClient != null) {
                drmClient.release();
            }

            IoUtils.closeQuietly(in);

            try {
                if (out != null) out.flush();
                if (outFd != null) outFd.sync();
            } catch (IOException e) {
            } finally {
                IoUtils.closeQuietly(out);
            }
        }
    }

    /**
     * Check if current connectivity is valid for this request.
     */
    private void checkConnectivity() throws StopRequestException {
        // checking connectivity will apply current policy
        mPolicyDirty = false;

        final NetworkState networkUsable = mInfo.checkCanUseNetwork();
        if (networkUsable != NetworkState.OK) {
            int status = Downloads.Impl.STATUS_WAITING_FOR_NETWORK;
            if (networkUsable == NetworkState.UNUSABLE_DUE_TO_SIZE) {
                status = Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                mInfo.notifyPauseDueToSize(true);
            } else if (networkUsable == NetworkState.RECOMMENDED_UNUSABLE_DUE_TO_SIZE) {
                status = Downloads.Impl.STATUS_QUEUED_FOR_WIFI;
                mInfo.notifyPauseDueToSize(false);
            }
            throw new StopRequestException(status, networkUsable.name());
        }
    }

    /**
     * Transfer as much data as possible from the HTTP response to the
     * destination file.
     */
    private void transferData(State state, InputStream in, OutputStream out)
            throws StopRequestException {
        final byte data[] = new byte[Constants.BUFFER_SIZE];
        for (;;) {
            int bytesRead = readFromResponse(state, data, in);
            if (bytesRead == -1) { // success, end of stream already reached
                handleEndOfStream(state);
                /// M: Add to support DRM
                state.mTotalWriteBytes = state.mCurrentBytes;
                return;
            }

            state.mGotData = true;
            writeDataToDestination(state, data, bytesRead, out);
            state.mCurrentBytes += bytesRead;
            reportProgress(state);

            if (Constants.LOGVV) {
                Log.v(Constants.TAG, "downloaded " + state.mCurrentBytes + " for "
                      + mInfo.mUri);
            }

            checkPausedOrCanceled(state);
        }
    }

    /**
     * Called after a successful completion to take any necessary action on the downloaded file.
     */
    private void finalizeDestinationFile(State state) {
        if (state.mFilename != null) {
            // make sure the file is readable
            FileUtils.setPermissions(state.mFilename, 0644, -1, -1);

            File file = new File(state.mFilename);
            Xlog.d(Constants.DL_DRM, "finalizeDestinationFile:MimeType is: "  + state.mMimeType +
                    "Total Write Bytes is: " + state.mTotalWriteBytes + "file length is: " + file.length()
                     + ", file.exists(): " + file.exists() + ",file location: " + state.mFilename);
            /// M: support MTK DRM @{
            if (file.length() == state.mTotalWriteBytes) {
                ContentValues values = new ContentValues();
                // If written bytes is not equal to file.length(), don't install DRM file
                if ((Constants.MTK_DRM_ENABLED)
                        && Helpers.isMtkDRMFile(state.mMimeType)) {
                    //DrmManagerClient drmClient = new DrmManagerClient(this.mContext);
                    OmaDrmClient drmClient = new OmaDrmClient(this.mContext);
                    if (Helpers.isMtkDRMFLOrCDFile(state.mMimeType)) {
                        int result = drmClient.installDrmMsg(state.mFilename);
                        Xlog.d(Constants.DL_DRM, "install FLCD result is"  + result + 
                                ",alfter install DRM Msg, new File(state.mFilename).exists(): " + 
                                new File(state.mFilename).exists() + "new File(state.mFilename).length()" +
                                new File(state.mFilename).length());
                        String[] paths = {state.mFilename};
                        String[] mimeTypes = {state.mMimeType};
                        MediaScannerConnection.scanFile(mContext, paths, mimeTypes, null);
                    } else if (Helpers.isMtkDRMRightFile(state.mMimeType)) {
                        try {
                            DrmRights rights = new DrmRights(state.mFilename, state.mMimeType);
                            int result = drmClient.saveRights(rights, null, null);
                            if (result == OmaDrmClient.ERROR_NONE) {
                                /*
                                String strCID = drmClient.getContentIdFromRights(rights);
                                Xlog.d(Constants.DL_DRM, "finalizeDestinationFile:saverights return CID:"
                                        + strCID);
                                DrmUtils.rescanDrmMediaFiles(mContext, strCID, null);
                                */
                                drmClient.rescanDrmMediaFiles(mContext, rights, null);
                            }

                            // Mark for delete for DRM right file
                            values = new ContentValues();
                            values.put(Downloads.Impl.COLUMN_DELETED, 1);
                            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                            Xlog.e(Constants.DL_DRM, "Mark for delete DRM rights file");

                        } catch (IOException e) {
                            Xlog.e(Constants.DL_DRM, "save rights " + state.mFilename + " exception");
                        }
                    }
                    /// M : when file length change after install drm msg, update state.mCurrentBytes. @{
                    if (new File(state.mFilename).length() != state.mTotalWriteBytes) {
                        state.mCurrentBytes = new File(state.mFilename).length();
                        state.mTotalWriteBytes = state.mCurrentBytes;
                    }
                    /// @}
                }
                values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, state.mCurrentBytes);
                mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                Xlog.d(Constants.DL_ENHANCE, "finalizeDestinationFile: " +
                        " Update Total Bytes:"  + state.mCurrentBytes);
            }
            /// @}
        }
    }

    /**
     * Called just before the thread finishes, regardless of status, to take any necessary action on
     * the downloaded file.
     */
    private void cleanupDestination(State state, int finalStatus) {
        if (state.mFilename != null && Downloads.Impl.isStatusError(finalStatus)) {
            if (true || Constants.LOGVV) {
                Log.d(TAG, "cleanupDestination() deleting " + state.mFilename);
            }
            new File(state.mFilename).delete();
            state.mFilename = null;
        }
    }

    /**
     * Check if the download has been paused or canceled, stopping the request appropriately if it
     * has been.
     */
    private void checkPausedOrCanceled(State state) throws StopRequestException {
        synchronized (mInfo) {
            if (mInfo.mControl == Downloads.Impl.CONTROL_PAUSED) {
                throw new StopRequestException(
                        Downloads.Impl.STATUS_PAUSED_BY_APP, "download paused by owner");
            }
            if (mInfo.mStatus == Downloads.Impl.STATUS_CANCELED || mInfo.mDeleted) {
                throw new StopRequestException(Downloads.Impl.STATUS_CANCELED, "download canceled");
            }
        }

        // if policy has been changed, trigger connectivity check
        if (mPolicyDirty) {
            checkConnectivity();
        }
    }

    /**
     * Report download progress through the database if necessary.
     */
    private void reportProgress(State state) {
        final long now = SystemClock.elapsedRealtime();

        final long sampleDelta = now - state.mSpeedSampleStart;
        if (sampleDelta > 500) {
            final long sampleSpeed = ((state.mCurrentBytes - state.mSpeedSampleBytes) * 1000)
                    / sampleDelta;

            if (state.mSpeed == 0) {
                state.mSpeed = sampleSpeed;
            } else {
                state.mSpeed = ((state.mSpeed * 3) + sampleSpeed) / 4;
            }

            // Only notify once we have a full sample window
            if (state.mSpeedSampleStart != 0) {
                mNotifier.notifyDownloadSpeed(mInfo.mId, state.mSpeed);
            }

            state.mSpeedSampleStart = now;
            state.mSpeedSampleBytes = state.mCurrentBytes;
        }

        if (state.mCurrentBytes - state.mBytesNotified > Constants.MIN_PROGRESS_STEP &&
            now - state.mTimeLastNotification > Constants.MIN_PROGRESS_TIME) {
            ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
            state.mBytesNotified = state.mCurrentBytes;
            state.mTimeLastNotification = now;
        }
    }

    /**
     * Write a data buffer to the destination file.
     * @param data buffer containing the data to write
     * @param bytesRead how many bytes to write from the buffer
     */
    private void writeDataToDestination(State state, byte[] data, int bytesRead, OutputStream out)
            throws StopRequestException {
        mStorageManager.verifySpaceBeforeWritingToFile(
                mInfo.mDestination, state.mFilename, bytesRead);

        boolean forceVerified = false;
        while (true) {
            try {
                out.write(data, 0, bytesRead);
                return;
            } catch (IOException ex) {
                // TODO: better differentiate between DRM and disk failures
                if (!forceVerified) {
                    // couldn't write to file. are we out of space? check.
                    mStorageManager.verifySpace(mInfo.mDestination, state.mFilename, bytesRead);
                    forceVerified = true;
                } else {
                    throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                            "Failed to write data: " + ex);
                }
            }
        }
    }

    /**
     * Called when we've reached the end of the HTTP response stream, to update the database and
     * check for consistency.
     */
    private void handleEndOfStream(State state) throws StopRequestException {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
        
        Xlog.d(Constants.DL_ENHANCE, "handleEndOfStream: " +
                "state.mContentLength: " +  state.mContentLength + 
                " state.mCurrentBytes: " + state.mCurrentBytes + 
                " mInfo.mTotalBytes: " + mInfo.mTotalBytes + 
                " state.mTotalBytes: " + state.mTotalBytes);
        
        if (state.mContentLength == -1) {
            values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, state.mCurrentBytes);
        }
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);

        final boolean lengthMismatched = (state.mContentLength != -1)
                && (state.mCurrentBytes != state.mContentLength);
        if (lengthMismatched) {
            if (cannotResume(state)) {
                throw new StopRequestException(STATUS_CANNOT_RESUME,
                        "mismatched content length; unable to resume");
            } else {
                throw new StopRequestException(STATUS_HTTP_DATA_ERROR,
                        "closed socket before end of file");
            }
        }
    }

    private boolean cannotResume(State state) {
    	/// Extend the resume condition, exclude the Etag influence
    	/*
        return (state.mCurrentBytes > 0 && !mInfo.mNoIntegrity && state.mHeaderETag == null)
                || DownloadDrmHelper.isDrmConvertNeeded(state.mMimeType); */
        Xlog.d(Constants.DL_ENHANCE, "innerState.mBytesSoFar is: " + 
                state.mCurrentBytes);
        return state.mCurrentBytes < 0;
    }

    /**
     * Read some data from the HTTP response stream, handling I/O errors.
     * @param data buffer to use to read data
     * @param entityStream stream for reading the HTTP response entity
     * @return the number of bytes actually read or -1 if the end of the stream has been reached
     */
    private int readFromResponse(State state, byte[] data, InputStream entityStream)
            throws StopRequestException {
        try {
            return entityStream.read(data);
        } catch (IOException ex) {
            // TODO: handle stream errors the same as other retries
            if ("unexpected end of stream".equals(ex.getMessage())) {
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
            if (cannotResume(state)) {
                throw new StopRequestException(STATUS_CANNOT_RESUME,
                        "Failed reading response: " + ex + "; unable to resume", ex);
            } else {
                throw new StopRequestException(STATUS_HTTP_DATA_ERROR,
                        "Failed reading response: " + ex, ex);
            }
        }
    }

    /**
     * Prepare target file based on given network response. Derives filename and
     * target size as needed.
     */
    private void processResponseHeaders(State state, HttpURLConnection conn)
            throws StopRequestException {
        // TODO: fallocate the entire file if header gave us specific length

        readResponseHeaders(state, conn);

        state.mFilename = Helpers.generateSaveFile(
                mContext,
                mInfo.mUri,
                mInfo.mHint,
                state.mContentDisposition,
                state.mContentLocation,
                state.mMimeType,
                mInfo.mDestination,
                state.mContentLength,
                /// M: Modify to support CU customization @{
                mInfo.mIsPublicApi, mStorageManager, 
                mInfo.mContinueDownload,
                mInfo.mPackage, 
                mInfo.mDownloadPath);
                /// @}
        
        Xlog.d(Constants.TAG, "writing " + mInfo.mUri + " to " + state.mFilename);        
        updateDatabaseFromHeaders(state);
        // check connectivity again now that we know the total size
        checkConnectivity();
    }

    /**
     * Update necessary database fields based on values of HTTP response headers that have been
     * read.
     */
    private void updateDatabaseFromHeaders(State state) {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl._DATA, state.mFilename);
        if (state.mHeaderETag != null) {
            values.put(Constants.ETAG, state.mHeaderETag);
        }
        if (state.mMimeType != null) {
            values.put(Downloads.Impl.COLUMN_MIME_TYPE, state.mMimeType);
        }
        values.put(Downloads.Impl.COLUMN_TOTAL_BYTES, mInfo.mTotalBytes);
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
    }

    /**
     * Read headers from the HTTP response and store them into local state.
     */
    private void readResponseHeaders(State state, HttpURLConnection conn)
            throws StopRequestException {
        state.mContentDisposition = conn.getHeaderField("Content-Disposition");
        state.mContentLocation = conn.getHeaderField("Content-Location");

        if (state.mMimeType == null) {
            state.mMimeType = Intent.normalizeMimeType(conn.getContentType());
        }

        state.mHeaderETag = conn.getHeaderField("ETag");

        final String transferEncoding = conn.getHeaderField("Transfer-Encoding");
        if (transferEncoding == null) {
            state.mContentLength = getHeaderFieldLong(conn, "Content-Length", -1);
        } else {
            Log.i(TAG, "Ignoring Content-Length since Transfer-Encoding is also defined");
            state.mContentLength = -1;
        }

        state.mTotalBytes = state.mContentLength;
        mInfo.mTotalBytes = state.mContentLength;

        Xlog.d(Constants.TAG, "Content-Disposition: " +
        		state.mContentDisposition);
        Xlog.d(Constants.TAG, "Content-Length: " + state.mContentLength);
        Xlog.d(Constants.TAG, "Content-Location: " + state.mContentLocation);
        Xlog.d(Constants.TAG, "Content-Type: " + state.mMimeType);
        Xlog.d(Constants.TAG, "ETag: " + state.mHeaderETag);
        Xlog.d(Constants.TAG, "Transfer-Encoding: " + transferEncoding);
        
        final boolean noSizeInfo = state.mContentLength == -1
                && (transferEncoding == null || !transferEncoding.equalsIgnoreCase("chunked"));
        if (!mInfo.mNoIntegrity && noSizeInfo) {
            throw new StopRequestException(STATUS_CANNOT_RESUME,
                    "can't know size of download, giving up");
        }
        
        /// M: Add this for OMA_DL
        /// OMA_DL HLD: 4.4 Installation Failure: in the case of retrieval errors
        /// If MimeType is not same with .dd file description, throw exception ATTRIBUTE_MISMATCH exception
        /// && !state.mMimeType.equals("audio/mp3") @{
        if (Downloads.Impl.MTK_OMA_DOWNLOAD_SUPPORT && state.mOmaDownload == 1 && 
                !state.mMimeType.equalsIgnoreCase("application/vnd.oma.dd+xml")) {
        	String mimeType = Intent.normalizeMimeType(conn.getContentType());
            if (mimeType != null) {
                Xlog.d(Constants.LOG_OMA_DL, "DownloadThread:readResponseHeader():" +
                        " header mimeType is:" + mimeType 
                        + "state.mMimeType is :" + state.mMimeType);
                
                
                if (Helpers.isMtkDRMFile(mimeType)) {
                    state.mMimeType = mimeType;
                    return;
                }
                
                if (((state.mMimeType.equals("audio/mp3") || state.mMimeType.equals("audio/mpeg")) &&
                        (mimeType.equals("audio/mp3") || mimeType.equals("audio/mpeg")))) {
                    return;
                } 
                
                // This means ATTRIBUTE_MISMATCH
                if (!mimeType.equals(state.mMimeType)) {              
                    ContentValues values = new ContentValues();
                    values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS,
                            Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH);
                    mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                    
                    state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH;
                    throw new StopRequestException(Downloads.Impl.OMADL_STATUS_ERROR_ATTRIBUTE_MISMATCH,
                            Downloads.Impl.OMADL_OCCUR_ERROR_NEED_NOTIFY);
                }
            } 
        }
        /// @}
    }

    private void parseRetryAfterHeaders(State state, HttpURLConnection conn) {
        state.mRetryAfter = conn.getHeaderFieldInt("Retry-After", -1);
        if (state.mRetryAfter < 0) {
            state.mRetryAfter = 0;
        } else {
            if (state.mRetryAfter < Constants.MIN_RETRY_AFTER) {
                state.mRetryAfter = Constants.MIN_RETRY_AFTER;
            } else if (state.mRetryAfter > Constants.MAX_RETRY_AFTER) {
                state.mRetryAfter = Constants.MAX_RETRY_AFTER;
            }
            state.mRetryAfter += Helpers.sRandom.nextInt(Constants.MIN_RETRY_AFTER + 1);
            state.mRetryAfter *= 1000;
        }
    }

    /**
     * Prepare the destination file to receive data.  If the file already exists, we'll set up
     * appropriately for resumption.
     */
    private void setupDestinationFile(State state) throws StopRequestException {
        Xlog.d(Constants.TAG, "setupDestinationFile(): state.mFilename :" + state.mFilename); 
        if (!TextUtils.isEmpty(state.mFilename)) { // only true if we've already run a thread for this download
            if (Constants.LOGV) {
                Log.i(Constants.TAG, "have run thread before for id: " + mInfo.mId +
                        ", and state.mFilename: " + state.mFilename);
            }
            if (!Helpers.isFilenameValid(state.mFilename,
                    mStorageManager.getDownloadDataDirectory())) {
                // this should never happen
                throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                        "found invalid internal destination filename");
            }
            
            Xlog.d(Constants.TAG, "new File(state.mFilename).exists: " + new File(state.mFilename).exists() + "," +
            		"length: " + new File(state.mFilename).length());
            
            // We're resuming a download that got interrupted
            File f = new File(state.mFilename);
            if (f.exists()) {
                if (Constants.LOGV) {
                    Log.i(Constants.TAG, "resuming download for id: " + mInfo.mId +
                            ", and state.mFilename: " + state.mFilename);
                }
                long fileLength = f.length();
                /// M: modify to fix tablet cts testDwonloadManagerDestination case fail [ALPS00357624] @{
                // Because CTS test file is 0 bytes, the file will be deleted in this.
                // So add "state.mCurrentBytes != state.mTotalBytes". If they equal, it means
                // the predownload is complete.
                Xlog.i(Constants.DL_ENHANCE, "setupDestinationFile: file " + state.mFilename +
                        " exsit. File length is: " + fileLength +
                        "state.mCurrentBytes: " + state.mCurrentBytes +
                        "state.mTotalBytes: " + state.mTotalBytes);
                
                if (fileLength == 0 && (state.mCurrentBytes != state.mTotalBytes)) {
                /// @}
                    // The download hadn't actually started, we can restart from scratch
                    if (Constants.LOGVV) {
                        Log.d(TAG, "setupDestinationFile() found fileLength=0, deleting "
                                + state.mFilename);
                    }
                    f.delete();
                    state.mFilename = null;
                    if (Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming download for id: " + mInfo.mId +
                                ", BUT starting from scratch again: ");
                    }
                /*    
                } else if (mInfo.mETag == null && !mInfo.mNoIntegrity) {
                    // This should've been caught upon failure
                    if (Constants.LOGVV) {
                        Log.d(TAG, "setupDestinationFile() unable to resume download, deleting "
                                + state.mFilename);
                    }
                    f.delete();
                    throw new StopRequestException(Downloads.Impl.STATUS_CANNOT_RESUME,
                            "Trying to resume a download that can't be resumed");
                */            
                } else {
                    // All right, we'll be able to resume this download
                    if (true || Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming download for id: " + mInfo.mId +
                                ", and starting with file of length: " + fileLength);
                    }
                    state.mCurrentBytes = (int) fileLength;
                    if (mInfo.mTotalBytes != -1) {
                        state.mContentLength = mInfo.mTotalBytes;
                    }
                    state.mHeaderETag = mInfo.mETag;
                    state.mContinuingDownload = true;
                    if (Constants.LOGV) {
                        Log.i(Constants.TAG, "resuming download for id: " + mInfo.mId +
                                ", state.mCurrentBytes: " + state.mCurrentBytes +
                                ", and setting mContinuingDownload to true: ");
                    }
                }
            } else {
                /// M: Add to to fix [ALPS00423697]. @{
                state.mCurrentBytes = 0;
                ContentValues values = new ContentValues();
                values.put(Downloads.Impl.COLUMN_CURRENT_BYTES, state.mCurrentBytes);
                mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
               /// @}
            }
            Xlog.d(Constants.TAG, "state.mCurrentBytes : " + state.mCurrentBytes);
        }
    }

    /**
     * Add custom headers for this download to the HTTP request.
     */
    private void addRequestHeaders(State state, HttpURLConnection conn) {
        for (Pair<String, String> header : mInfo.getHeaders()) {
            /// M : add to fix 1257388. remove null referfer. @{
            if (header.first.equalsIgnoreCase("Referer") &&
                    ((header.second == null) || header.second.equals(""))) {
                 Xlog.d(Constants.TAG, "header.first: referer "  +
                            ", header.second: null , remove null referer");
                 continue;
            }
            /// @}
            conn.addRequestProperty(header.first, header.second);
        }

        // Only splice in user agent when not already defined
        if (conn.getRequestProperty("User-Agent") == null) {
            conn.addRequestProperty("User-Agent", userAgent());
        }

        // Defeat transparent gzip compression, since it doesn't allow us to
        // easily resume partial downloads.
        conn.setRequestProperty("Accept-Encoding", "identity");

        if (state.mContinuingDownload) {
        	/*
            if (state.mHeaderETag != null) {
                conn.addRequestProperty("If-Match", state.mHeaderETag);
            }*/
            conn.addRequestProperty("Range", "bytes=" + state.mCurrentBytes + "-");
            Xlog.d(Constants.TAG, "Adding Range header: " +
                    "bytes=" + state.mCurrentBytes + "-");
        }
    }

    /**
     * Stores information about the completed download, and notifies the initiating application.
     */
    private void notifyDownloadCompleted(
            State state, int finalStatus, String errorMsg, int numFailed) {
        notifyThroughDatabase(state, finalStatus, errorMsg, numFailed);
        if (Downloads.Impl.isStatusCompleted(finalStatus)) {
            mInfo.sendIntentIfRequested();
        }
    }

    private void notifyThroughDatabase(
            State state, int finalStatus, String errorMsg, int numFailed) {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_STATUS, finalStatus);
        values.put(Downloads.Impl._DATA, state.mFilename);
        values.put(Downloads.Impl.COLUMN_MIME_TYPE, state.mMimeType);
        values.put(Downloads.Impl.COLUMN_LAST_MODIFICATION, mSystemFacade.currentTimeMillis());
        values.put(Downloads.Impl.COLUMN_FAILED_CONNECTIONS, numFailed);
        values.put(Constants.RETRY_AFTER_X_REDIRECT_COUNT, state.mRetryAfter);

        if (!TextUtils.equals(mInfo.mUri, state.mRequestUri)) {
            values.put(Downloads.Impl.COLUMN_URI, state.mRequestUri);
        }

        // save the error message. could be useful to developers.
        if (!TextUtils.isEmpty(errorMsg)) {
            values.put(Downloads.Impl.COLUMN_ERROR_MSG, errorMsg);
        }
        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
    }

    private INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        @Override
        public void onUidRulesChanged(int uid, int uidRules) {
            // caller is NPMS, since we only register with them
            if (uid == mInfo.mUid) {
                mPolicyDirty = true;
            }
        }

        @Override
        public void onMeteredIfacesChanged(String[] meteredIfaces) {
            // caller is NPMS, since we only register with them
            mPolicyDirty = true;
        }

        @Override
        public void onRestrictBackgroundChanged(boolean restrictBackground) {
            // caller is NPMS, since we only register with them
            mPolicyDirty = true;
        }
    };

    public static long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            return Long.parseLong(conn.getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Return if given status is eligible to be treated as
     * {@link android.provider.Downloads.Impl#STATUS_WAITING_TO_RETRY}.
     */
    public static boolean isStatusRetryable(int status) {
        switch (status) {
            case STATUS_HTTP_DATA_ERROR:
            case HTTP_UNAVAILABLE:
            case HTTP_INTERNAL_ERROR:
                return true;
            default:
                return false;
        }
    }

    /**
     * M: This function is used to notify webserver 
     * oma download error status 
     */
    private void notifyOMADownloadWebServerErrorStatus(URL notifyUrl, int notifyCode) {
        if (notifyUrl != null) {
            Xlog.i(Constants.LOG_OMA_DL, "DownloadThread: catch StopRequest and need to notify web server: " + 
                    notifyUrl.toString() + " and Notify code is:" + notifyCode);
            OmaDescription omaDescription = new OmaDescription();
            omaDescription.setInstallNotifyUrl(notifyUrl);
            omaDescription.setStatusCode(notifyCode);
            if (OmaDownload.installNotify(omaDescription, null) != OmaStatusHandler.READY) {
                Xlog.d(Constants.LOG_OMA_DL, "DownloadThread: catch StopRequest but notify URL : " +
                        "" + notifyUrl + " failed");
            } else {
                Xlog.d(Constants.LOG_OMA_DL, "DownloadThread: catch StopRequest and notify URL OK");
            }
        }
    }
    
    /**
     * M: After download complete, Check whether OMA DL or not.
     * Deal with OMA DL file (install Notify and next url)
     * 
     */
    private void handleOmaDownloadMediaObject(State state) throws StopRequestException {    
        if (state.mOmaDownload != 1 || state.mMimeType.equalsIgnoreCase("application/vnd.oma.dd+xml")) {
            return;
        }
        state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_DOWNLOAD_COMPLETELY;
        ContentValues values = new ContentValues(); 

        if (state.mOmaDownloadInsNotifyUrl != null) {
            Xlog.i(Constants.LOG_OMA_DL, "Handle Media object, notify URL is: " +
                    state.mOmaDownloadInsNotifyUrl);
            URL notifyUrl = null;
            try {
                notifyUrl = new URL(state.mOmaDownloadInsNotifyUrl);
            } catch (MalformedURLException e) {     
                values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                        Downloads.Impl.OMADL_STATUS_ERROR_INSTALL_FAILED);
                mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                
                state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_INSTALL_FAILED;
                
                // There will update OMA_Download_Status, or the query will reuse
                Xlog.e(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDownloadMediaObject(): " +
                        "New url failed" + state.mOmaDownloadInsNotifyUrl);
                throw new StopRequestException(Downloads.Impl.STATUS_UNKNOWN_ERROR, 
                        "OMA Download Installation Media Object Failure");
            }
            
            OmaDescription omaDescription = new OmaDescription();
            omaDescription.setInstallNotifyUrl(notifyUrl);
            omaDescription.setStatusCode(OmaStatusHandler.SUCCESS);
            if (OmaDownload.installNotify(omaDescription, null) != OmaStatusHandler.READY) {
                values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                        Downloads.Impl.OMADL_STATUS_ERROR_INSTALL_FAILED);
                mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                
                state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_INSTALL_FAILED;
                throw new StopRequestException(Downloads.Impl.STATUS_UNKNOWN_ERROR, 
                        "OMA Download Installation Media Object Failure");
            }
            Xlog.i(Constants.LOG_OMA_DL, "Handle Media object, after notify URL");
        }

        if (mInfo.mOmaDownloadNextUrl != null) {
            Xlog.d(Constants.LOG_OMA_DL, "DownloadThread:handleOmaDownloadMediaObject(): " +
                    "next url is: " + mInfo.mOmaDownloadNextUrl);
            values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_SUCCESS);
            //mInfo.notifyOmaDownloadNextUrl(mInfo.mOmaDownloadNextUrl);
            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG, 1);
            // Download the Media Object success and install notify success and need to show user next URL
            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                    Downloads.Impl.OMADL_STATUS_HAS_NEXT_URL);
            mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
        }
        /// M: add to send oma dl intent in kk. @{
        Intent intent = new Intent(Constants.ACTION_OMA_DL_DIALOG);
        //Intent intent = new Intent();
        //intent.setClassName(OmaDownloadActivity.class.getPackage().getName(),
        //               OmaDownloadActivity.class.getName());

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        mContext.startActivity(intent);
        Xlog.w(Constants.LOG_OMA_DL, "send oma dl dialog intent");
        /// @}
    }
    
    /**
     * M: Add this function to support OMA DL
     * This function is used to handle .dd file
     */
    private void handleOmaDownloadDescriptorFile (State state) throws StopRequestException {
        // Handle .dd file
        if (state.mMimeType != null) {
            if (state.mMimeType.equals("application/vnd.oma.dd+xml")) {             
                state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_DOWNLOAD_COMPLETELY;
                File ddFile = new File(state.mFilename);
                URL ddUrl = null;
                try {
                    ddUrl = new URL(state.mRequestUri);
                } catch (MalformedURLException e) {
                    // TODO:need error handling and update UI
                    // There will update OMA_Download_Status, or the query will reuse
                    Xlog.e(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDescriptorFile():" +
                            "New url failed" + state.mRequestUri);
                }
                Xlog.i(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDescriptorFile(): "
                        + "URL is " + ddUrl + "file path is " + ddFile);
                
                if (ddFile != null && ddUrl != null) {
                    OmaDescription omaDescription = new OmaDescription();
                    int parseStatus = OmaDownload.parseXml(ddUrl, ddFile, omaDescription);
                    
                    ContentValues values = new ContentValues(); 
                    if (omaDescription != null && parseStatus == OmaStatusHandler.SUCCESS) {
                        // Update downloads.db
                        // Show this is OMA DL
                        values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_SUCCESS);
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG, 1);
                        // Update the parse status to success
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, 
                                Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS);
                        // Update the info. This info will show to user
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_NAME, 
                                omaDescription.getName());
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_VENDOR, 
                                omaDescription.getVendor());
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_SIZE, 
                                omaDescription.getSize());
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_TYPE,
                                omaDescription.getType().get(0));
                        Xlog.d(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDownloadDescriptorFile(): " +
                                "dd file's mimtType is :" + omaDescription.getType().get(0));
                        
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_DD_FILE_INFO_DESCRIPTION, 
                                omaDescription.getDescription());
  
                        if (omaDescription.getObjectUrl() != null) {
                            Xlog.d(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDownloadDescriptorFile(): " +
                                    "dd file's object url :" + omaDescription.getObjectUrl().toString());
                            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_OBJECT_URL,
                                    omaDescription.getObjectUrl().toString());
                        }
                        if (omaDescription.getNextUrl() != null) {
                            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_NEXT_URL, 
                                    omaDescription.getNextUrl().toString());
                            mInfo.mOmaDownloadNextUrl = omaDescription.getNextUrl().toString();
                        }
                        if (omaDescription.getInstallNotifyUrl() != null) {
                            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_INSTALL_NOTIFY_URL, 
                                    omaDescription.getInstallNotifyUrl().toString());
                            state.mOmaDownloadInsNotifyUrl = omaDescription.getInstallNotifyUrl().toString();
                        }
                        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                        
                        // Note: these class members maybe change to DownloadThread class's local variable.
                        // So, the values can not be modified by DownloadService's updateDownload function.
                        state.mOmaDownload = 1;
                        state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS;
                        
                    } else {
                        Xlog.w(Constants.LOG_OMA_DL, "DownloadThread: handleOmaDownloadDescriptorFile(): " +
                                "parse .dd file failed, error is: " + parseStatus);
                        
                        // Update database
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_FLAG, 1);
                        values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_STATUS, parseStatus);
                        if (omaDescription.getInstallNotifyUrl() != null) {
                            values.put(Downloads.Impl.COLUMN_OMA_DOWNLOAD_INSTALL_NOTIFY_URL, 
                                    omaDescription.getInstallNotifyUrl().toString());
                        }
                        mContext.getContentResolver().update(mInfo.getAllDownloadsUri(), values, null, null);
                        
                        //Need to install notify
                        state.mOmaDownload = 1;
                        if (omaDescription.getInstallNotifyUrl() != null) {
                            state.mOmaDownloadInsNotifyUrl = omaDescription.getInstallNotifyUrl().toString();
                        }
                        if (parseStatus == OmaStatusHandler.INVALID_DDVERSION) {
                            state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DDVERSION;
                            throw new StopRequestException(Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DDVERSION, 
                                    Downloads.Impl.OMADL_OCCUR_ERROR_NEED_NOTIFY);
                        } else {
                            state.mOmaDownloadStatus = Downloads.Impl.OMADL_STATUS_ERROR_INVALID_DESCRIPTOR;
                            throw new StopRequestException(Downloads.Impl.STATUS_BAD_REQUEST, 
                                    Downloads.Impl.OMADL_OCCUR_ERROR_NEED_NOTIFY);
                        }    
                    }
                }
                
                /// M: add to send oma dl intent in kk. @{
                Intent intent = new Intent(Constants.ACTION_OMA_DL_DIALOG);
                //Intent intent = new Intent();
                //intent.setClassName(OmaDownloadActivity.class.getPackage().getName(),
                //		OmaDownloadActivity.class.getName());
                
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                mContext.startActivity(intent);
                Xlog.w(Constants.LOG_OMA_DL, "send oma dl dialog intent");
                /// @}
            }
        }
        
    }
}
