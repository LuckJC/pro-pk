package com.mediatek.encapsulation.android.util;

import android.util.Patterns;
import com.mediatek.encapsulation.EncapsulationConstant;
import java.util.regex.Pattern;

public class EncapsulatedPatterns {

    /**
     * M: MTK Version for ALPS00934864
     * @hide
     */
    public static final Pattern PHONE_PATTERN_MTK
        = EncapsulationConstant.USE_MTK_PLATFORM ? Patterns.PHONE_PATTERN_MTK :
            Pattern.compile(                                                      // sdd = space, dot, or dash
                "(\\+[0-9\\(\\)]+[\\- \\.]*)?"                                    // +<digits or ( or )><sdd>*
                + "(\\([0-9\\(\\)]+\\)[\\- \\.]*)?"                               // (<digits or ( or )>)<sdd>*
                + "([0-9\\(\\)][0-9\\(\\)\\- \\.][0-9\\(\\)\\- \\.]+[0-9\\(\\)])" // <digit or ( or )><digit or ( or )|sdd>+<digit>
                + "(,? *)?");
}
