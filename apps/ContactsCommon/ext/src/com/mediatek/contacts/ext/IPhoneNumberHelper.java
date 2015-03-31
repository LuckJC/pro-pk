package com.mediatek.contacts.ext;

import android.net.Uri;

public interface IPhoneNumberHelper {

    public boolean canPlaceCallsTo(CharSequence number);
    public boolean canSendSmsTo(CharSequence number);
    public CharSequence getDisplayNumber(CharSequence number, CharSequence formattedNumber);
    public boolean isVoicemailNumber(CharSequence number);
    public boolean isSipNumber(CharSequence number);
    public boolean isEmergencyNumber(CharSequence number, int simId);
    public boolean isEmergencyNumber(CharSequence number);
    public boolean isVoiceMailNumberForMtk(CharSequence number, int simId);
    public Uri getCallUri(String number, int simId);
}
