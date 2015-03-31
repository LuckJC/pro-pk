/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.contacts.common;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;

import com.android.contacts.common.util.Constants;
import com.android.phone.common.PhoneConstants;

/**
 * Utilities related to calls.
 */
public class CallUtil {

    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_IMTO = "imto";
    public static final String SCHEME_SIP = "sip";

    public static final ComponentName CALL_INTENT_DESTINATION = new ComponentName(
            "com.android.phone", "com.android.phone.PrivilegedOutgoingCallBroadcaster");

    /**
     * Copied from PhoneApp. See comments in Phone app for more detail.
     */
    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";

    /**
     * Return an Intent for making a phone call. Scheme (e.g. tel, sip) will be determined
     * automatically.
     */
    public static Intent getCallIntent(String number) {
        return getCallIntent(number, null);
    }

    /**
     * Return an Intent for making a phone call. A given Uri will be used as is (without any
     * sanity check).
     */
    public static Intent getCallIntent(Uri uri) {
        return getCallIntent(uri, null);
    }

    /**
     * A variant of {@link #getCallIntent(String)} but also accept a call origin. For more
     * information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(String number, String callOrigin) {
        //return getCallIntent(getCallUri(number), callOrigin);
        return getCallIntent(number, callOrigin, Constants.DIAL_NUMBER_INTENT_NORMAL);
    }

    public static Intent getCallIntent(String number, String callOrigin, int type) {
        return getCallIntent(getCallUri(number), callOrigin, type);
    }

    /**
     * A variant of {@link #getCallIntent(Uri)} but also accept a call origin. For more
     * information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(Uri uri, String callOrigin) {
        /*final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (callOrigin != null) {
            intent.putExtra(DialtactsActivity.EXTRA_CALL_ORIGIN, callOrigin);
        }
        return intent;*/
        return getCallIntent(uri, callOrigin, Constants.DIAL_NUMBER_INTENT_NORMAL);
    }

    public static Intent getCallIntent(Uri uri, String callOrigin, int type) {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (callOrigin != null) {
            intent.putExtra(EXTRA_CALL_ORIGIN, callOrigin);
        }
        if ((type & Constants.DIAL_NUMBER_INTENT_IP) != 0) {
            intent.putExtra(Constants.EXTRA_IS_IP_DIAL, true);
        }

        if ((type & Constants.DIAL_NUMBER_INTENT_VIDEO) != 0) {
            intent.putExtra(Constants.EXTRA_IS_VIDEO_CALL, true);
        }
        return intent;
    }

    /**
     * Return Uri with an appropriate scheme, accepting Voicemail, SIP, and usual phone call
     * numbers.
     */
    public static Uri getCallUri(String number) {
        if (PhoneNumberUtils.isUriNumber(number)) {
             return Uri.fromParts(SCHEME_SIP, number, null);
        }
        return Uri.fromParts(SCHEME_TEL, number, null);
     }
}
