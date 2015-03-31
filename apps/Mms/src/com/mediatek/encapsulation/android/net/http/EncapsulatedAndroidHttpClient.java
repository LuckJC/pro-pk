package com.mediatek.encapsulation.android.net.http;

import android.net.http.AndroidHttpClient;

import com.mediatek.encapsulation.EncapsulationConstant;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

public class EncapsulatedAndroidHttpClient {

    private AndroidHttpClient mHttpClient;

    public EncapsulatedAndroidHttpClient(AndroidHttpClient client) {
        mHttpClient = client;
    }

    /// M: enhance MMS retry in AndroidHttpClient module
    /**
     * Set a handler for determining if an HttpRequest should be retried after a recoverable exception during execution.
     * @param retryHandler used by request executors
     * @hide
     * @internal
     */
    public void setHttpRequestRetryHandler(final DefaultHttpRequestRetryHandler retryHandler) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mHttpClient.setHttpRequestRetryHandler(retryHandler);
        } else {
        }
    }
}