package com.example.xuntongwatch.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.widget.EditText;
//import com.android.internal.telephony.TelephonyIntents;

import com.example.xuntongwatch.R;

public class SpecialCharSequenceMgr {
	private static final String TAG = "SpecialCharSequenceMgr";

	private static final String MMI_IMEI_DISPLAY = "*#06#";

	// The following constant is copied from package
	// com.android.internal.telephony.TelephonyIntents.
	/**
	 * Broadcast Action: A "secret code" has been entered in the dialer. Secret
	 * codes are of the form *#*#
	 * <code>#*#*. The intent will have the data URI:</p>
	 * 
	 * <p><code>android_secret_code://&lt;code&gt;</code></p>
	 */
	public static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";

	public static boolean handleChars(Context context, String input) {

		// get rid of the separators so that the string gets parsed correctly
		String dialString = PhoneNumberUtils.stripSeparators(input);

		if (handleIMEIDisplay(context, dialString)
				|| handleCITCode(context, dialString)
				|| handleSecretCode(context, dialString)) {
			return true;
		}

		return false;
	}

	private static boolean handleIMEIDisplay(Context context, String input) {
		if (input.equals(MMI_IMEI_DISPLAY)) {
			TelephonyManager telephonyManager = ((TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE));
			String imeiStr = telephonyManager.getDeviceId();

			AlertDialog alert = new AlertDialog.Builder(context)
					.setTitle(R.string.imei).setMessage(imeiStr)
					.setPositiveButton(android.R.string.ok, null)
					.setCancelable(false).show();

			return true;
		}

		return false;
	}

	public static boolean handleCITCode(Context context, String input) {
		if (input.toString().equals("*#63863555#")
				|| input.toString().equals("*#66#")) {
			Intent intent = new Intent("sim.android.cit",
					Uri.parse("cit_secret_code://63863555"));
			context.sendBroadcast(intent);
			return true;
		}
		return false;
	}

	public static boolean handleSecretCode(Context context, String input) {
		int len = input.length();
		if (input.toString().equals("*#3646633#")) {
			Intent intent = new Intent(SECRET_CODE_ACTION,
					Uri.parse("android_secret_code://" + input.substring(2, len - 1)));
			context.sendBroadcast(intent);
			return true;
		}
		return false;
	}
}
