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

package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.net.WebAddress;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.mediatek.browser.ext.Extensions;
import com.mediatek.browser.ext.IBrowserDownloadEx;
import com.mediatek.xlog.Xlog;

import java.io.File;
import java.net.URI;

/**
 * Handle download requests
 */
public class DownloadHandler {

    private static final boolean LOGD_ENABLED =
            com.android.browser.Browser.LOGD_ENABLED;

    /// M: Operator check storage feature definition. @{
    private static final String EXTRA_STORAGE_DIR = "/storage/sdcard1";
    private static final long LowSpaceThreshold = 10 * 1024 * 1024;
    /// @}
    
    private static final String LOGTAG = "DLHandler";
    private static final String XLOGTAG = "browser/DLHandler";
    private static IBrowserDownloadEx sBrowserDownloadEx = null;
    /**
     * Notify the host application a download should be done, or that
     * the data should be streamed if a streaming viewer is available.
     * @param activity Activity requesting the download.
     * @param url The full url to the content that should be downloaded
     * @param userAgent User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype The mimetype of the content reported by the server
     * @param referer The referer associated with the downloaded url
     * @param privateBrowsing If the request is coming from a private browsing tab.
     */
    public static void onDownloadStart(Activity activity, String url,
            String userAgent, String contentDisposition, String mimetype,
            String referer, boolean privateBrowsing, long  contentLength) {
        onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
                mimetype, referer, privateBrowsing, contentLength);
    }

    // This is to work around the fact that java.net.URI throws Exceptions
    // instead of just encoding URL's properly
    // Helper method for onDownloadStartNoStream
    private static String encodePath(String path) {
        char[] chars = path.toCharArray();

        boolean needed = false;
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                needed = true;
                break;
            }
        }
        if (needed == false) {
            return path;
        }

        StringBuilder sb = new StringBuilder("");
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                sb.append('%');
                sb.append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Notify the host application a download should be done, even if there
     * is a streaming viewer available for thise type.
     * @param activity Activity requesting the download.
     * @param url The full url to the content that should be downloaded
     * @param userAgent User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype The mimetype of the content reported by the server
     * @param referer The referer associated with the downloaded url
     * @param privateBrowsing If the request is coming from a private browsing tab.
     */
    /*package */ public static void onDownloadStartNoStream(Activity activity,
            String url, String userAgent, String contentDisposition,
            String mimetype, String referer, boolean privateBrowsing, long contentLength) {

        String filename = URLUtil.guessFileName(url,
                contentDisposition, mimetype);
        Xlog.d(XLOGTAG, "Guess file name is: " + filename + 
                " mimetype is: " + mimetype);

        // Check to see if we have an SDCard
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            int title;
            String msg;

            // Check to see if the SDCard is busy, same as the music app
            if (status.equals(Environment.MEDIA_SHARED)) {
                msg = activity.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = activity.getString(R.string.download_no_sdcard_dlg_msg, filename);
                title = R.string.download_no_sdcard_dlg_title;
            }

            new AlertDialog.Builder(activity)
                .setTitle(title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(msg)
                .setPositiveButton(R.string.ok, null)
                .show();
            return;
        }

        /// M: Check whether the download path of browser is available before begin to download. @{
        String mDownloadPath = BrowserSettings.getInstance().getDownloadPath();
        if (mDownloadPath.contains("sdcard1")) {
            if (! new File("/storage/sdcard1").canWrite()) {
                int mTitle = R.string.download_path_unavailable_dlg_title;
                String mMsg = activity.getString(R.string.download_path_unavailable_dlg_msg);
                new AlertDialog.Builder(activity)
                    .setTitle(mTitle)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(mMsg)
                    .setPositiveButton(R.string.ok, null)
                    .show();
                return;
            }
        }
        /// @}

        ///M: Operator Feature check storage in download path before download @{
        sBrowserDownloadEx = Extensions.getDownloadPlugin(activity);
        if(sBrowserDownloadEx.shouldCheckStorageBeforeDownload()) {
            if (contentLength > 0) {
                Xlog.i(XLOGTAG, "before checkIfHaveAvailableStoreage(),contentLength: " + contentLength);
                checkIfHaveAvailableStoreage(mDownloadPath, activity, contentLength);
            }
        }
        /// @}

        // java.net.URI is a lot stricter than KURL so we have to encode some
        // extra characters. Fix for b 2538060 and b 1634719
        WebAddress webAddress;
        try {
            webAddress = new WebAddress(url);
            webAddress.setPath(encodePath(webAddress.getPath()));
        } catch (Exception e) {
            // This only happens for very bad urls, we want to chatch the
            // exception here
            Log.e(LOGTAG, "Exception trying to parse url:" + url);
            return;
        }

        String addressString = webAddress.toString();
        Uri uri = Uri.parse(addressString);
        final DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(uri);
        } catch (IllegalArgumentException e) {
            Toast.makeText(activity, R.string.cannot_download, Toast.LENGTH_SHORT).show();
            return;
        }
        request.setMimeType(mimetype);
        /// M: Operator Feature set RequestDestinationDir @{
        sBrowserDownloadEx = Extensions.getDownloadPlugin(activity);
        if (!sBrowserDownloadEx.setRequestDestinationDir(BrowserSettings.getInstance().getDownloadPath(), 
                request, filename, mimetype)) {
        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs depending on mimetype?
            //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            String downloadPath = "file://" + BrowserSettings.getInstance().getDownloadPath()
                                            + File.separator + filename;
            Uri pathUri = Uri.parse(downloadPath);
            request.setDestinationUri(pathUri);
            Xlog.d(XLOGTAG, "request.setDestinationInExternalPublicDir, addressString: " + addressString);
        }
        /// @}
        
        // let this downloaded file be scanned by MediaScanner - so that it can 
        // show up in Gallery app, for example.
        request.allowScanningByMediaScanner();
        request.setDescription(webAddress.getHost());
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        String cookies = CookieManager.getInstance().getCookie(url, privateBrowsing);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.addRequestHeader("Referer", referer);
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setUserAgent(userAgent);
        if (mimetype == null) {
            if (TextUtils.isEmpty(addressString)) {
                return;
            }

            /// M: fix BUG: ALPS00256340 @{
            try {
                URI.create(addressString);
            } catch (IllegalArgumentException e) {
                Toast.makeText(activity, R.string.cannot_download, Toast.LENGTH_SHORT).show();
                return;
            }
            /// @}
            
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            new FetchUrlMimeType(activity, request, addressString, cookies,
                    userAgent).start();
        } else {
            final DownloadManager manager
                    = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            new Thread("Browser download") {
                public void run() {
                    manager.enqueue(request);
                }
            }.start();
        }

        /// M: Operator Feature show ToastWithFileSize @{
        sBrowserDownloadEx = Extensions.getDownloadPlugin(activity);
        if (contentLength > 0 && sBrowserDownloadEx.shouldShowToastWithFileSize()) {
            Toast.makeText(activity, activity.getString(R.string.download_pending_with_file_size) 
                    + Formatter.formatFileSize(activity, contentLength), Toast.LENGTH_SHORT)
                    .show();
        } else {
        /// @}
            Toast.makeText(activity, R.string.download_pending, Toast.LENGTH_SHORT)
                       .show();
        }
        
        /// M: Add to start Download activity. @{
        Intent pageView = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        pageView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(pageView);
        /// @}
    }
    
    /**
     * M: Notify the user download the content or open
     * the content. Add this to support Operator customization
     * @param intent The Intent.ACTION_VIEW intent
     * @param url The full url to the content that should be downloaded
     * @param contentDisposition Content-disposition http header, if
     *                           present.
     * @param mimetype The mimetype of the content reported by the server
     * @param contentLength The file size reported by the server
     */
    public static void showDownloadOrOpenContent(final Activity activity, final Intent intent, 
            final String url, final String userAgent,
            final String contentDisposition, final String mimetype, 
            // final boolean privateBrowsing) {
            final boolean privateBrowsing, final long contentLength) {
        new AlertDialog.Builder(activity)
            .setTitle(R.string.application_name)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage(R.string.download_or_open_content)
            .setPositiveButton(R.string.save_content,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            onDownloadStartNoStream(activity, url, userAgent, 
                                    // contentDisposition, mimetype, privateBrowsing);
                                    contentDisposition, mimetype, null, privateBrowsing, contentLength);
                            Xlog.d(XLOGTAG, "User decide to download the content");
                            return;
                        }
                    })
            .setNegativeButton(R.string.open_content,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            int nFlags = intent.getFlags();
                            nFlags &= (~Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setFlags(nFlags);
                            if (url != null) {
                                String urlCookie = CookieManager.getInstance().getCookie(url);
                                Log.i(XLOGTAG, "url: " + url + " url cookie: " + urlCookie);
                                if (urlCookie != null) {
                                    intent.putExtra("url-cookie", urlCookie);
                                }
                            }
                            activity.startActivity(intent);
                            Xlog.d(XLOGTAG, "User decide to open the content by startActivity");
                            return;
                        }})
            .setOnCancelListener(
                    new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            Xlog.d(XLOGTAG, "User cancel the download action");
                            return;
                        }
                    })
            .show();
    }
    
    /**
     * M: judge if in the condition which have available storage before download
     * @param path             download path
     * @param activity         the current activity context
     * @param contentLength    content-length returned by server
     * @return                 if available storage below 10M, popup alert dialog
     */
    private static void checkIfHaveAvailableStoreage(String path, Activity activity, long contentLength) {
        String downloadPath = Uri.parse(path).getPath();
        String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (downloadPath != null && downloadPath.startsWith(externalPath)) {
            if (availableStorage(externalPath, activity) < contentLength) {
                Xlog.i(XLOGTAG, "external storage is download path, can to download because of low storeage " +
                         "and will popup low storeage dialog");

                new AlertDialog.Builder(activity)
                .setTitle(R.string.low_storage_dialog_title_on_external)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.low_storage_dialog_msg_on_external)
                .setPositiveButton(R.string.ok, null)
                .show();
                return;
            }
        } else if (downloadPath != null && downloadPath.startsWith(EXTRA_STORAGE_DIR)) {
            if (availableStorage(EXTRA_STORAGE_DIR, activity) < contentLength) {
                Xlog.i(XLOGTAG, "extra storage is download path, can to download because of low storeage " +
                        "and will popup low storeage dialog");

                new AlertDialog.Builder(activity)
                .setTitle(R.string.low_storage_dialog_title_on_extra)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.low_storage_dialog_msg_on_extra)
                .setPositiveButton(R.string.ok, null)
                .show();
                return;
            }
        }
    }
    
    /**
     * M: get the available storage which subtract will occupy storage
     *    and low space threshold 
     */
    private static long availableStorage(String path, Activity activity) {
        long availableStorage = getAvailableBytesInFileSystemAtGivenRoot(path) - getDownloadsInProgressWillOccupyBytes(activity) - LowSpaceThreshold;
        Xlog.i(XLOGTAG, "check storage before download, availableStorage : " + availableStorage + ", about" + availableStorage / (1 * 1024 * 1024) + "M");
        return availableStorage;
    }
    
    /** 
     * M: get the available storage in given root path file system 
     * @param path   the root path which will check
     * @return       the available size 
     */
    private static long getAvailableBytesInFileSystemAtGivenRoot(String path) {
        StatFs stat = new StatFs(path);
        long availableBlocks = (long) stat.getAvailableBlocks();
        long size = stat.getBlockSize() * availableBlocks;
        return size;
    }
    
    /**
     * M: get the storage size which downloading files in progress will occupy.
     * @param activity   the current activity context
     * @return           the storage size will use
     */
    private static long getDownloadsInProgressWillOccupyBytes(Activity activity) {
        long downloadsWillOccupyBytes = 0l;
        Cursor cursor = null;
        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        try {
           cursor = manager.query(new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING));
           if (cursor != null) {
               for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                   long downloadID = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                   long totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                   long currentBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                   if (totalBytes > 0 && currentBytes > 0 && totalBytes - currentBytes > 0) {
                       downloadsWillOccupyBytes += totalBytes - currentBytes;
                       Xlog.i(XLOGTAG, "Download id :" + downloadID + " in downloading, totalBytes: " + totalBytes + ",currentBytes: " + currentBytes);
                   }
               }
           }
        } catch (IllegalStateException e) {
            Xlog.i(XLOGTAG, "getDownloadsInProgressWillOccupyBytes: query encounter exception");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Xlog.i(XLOGTAG, "getDownloadsInProgressWillOccupyBytes: return downloadsWillOccupyBytes:" + downloadsWillOccupyBytes);
        return downloadsWillOccupyBytes;
    }

}
