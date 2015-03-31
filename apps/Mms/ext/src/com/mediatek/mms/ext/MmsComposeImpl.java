/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.ext;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.mediatek.encapsulation.MmsLog;

/// M: New plugin API @{
import java.util.ArrayList;
/// @}

public class MmsComposeImpl extends ContextWrapper implements IMmsCompose {
    private static final String TAG = "Mms/MmsComposeImpl";
    private IMmsComposeHost mHost = null;

    public MmsComposeImpl(Context context) {
        super(context);
    }

    public void init(IMmsComposeHost host) {
        mHost = host;
        return;
    }
    
    public void addContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, CharSequence text) {
        // do nothing
        return;
    }

    public void addContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        // do nothing
        return;
    }

    public void addSplitMessageContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, Activity activity,
            long messageGroupId, int messagesCount) {
    }

    public void addSplitThreadOptionMenu(Menu menu, Activity activity, long threadId) {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public int getCapturePicMode() {
        return IMmsCompose.CAPTURE_PIC_AT_TEMP_FILE;
    }

    public boolean isAppendSender() {
        return false;
    }

    public void configSubjectEditor(EditText subjectEditor) {
        return;
    }

    protected IMmsComposeHost getHost() {
        return mHost;
    }

    /// M:Code analyze 01,For new feature CMCC_Mms in ALPS00325381, MMS easy
    /// porting check in JB @
    public boolean isAddMmsUrlToBookMark() {
        return false;
    }
    /// @}

    public boolean isMultiCompose() {
        return false;
    }

    public String getNumberLocation(Context context, String number) {
        return number;
    }

    public Uri getConverationUri(Uri uriSrc, long threadId) {
        return uriSrc;
    }

    public boolean deleteMassTextMsg(long msgId, long timeStamp) {
        return false;
    }

    public boolean lockMassTextMsg(Context context, long msgId, long timeStamp, boolean lock) {
        return false;
    }

    public boolean showMassTextMsgDetail(Context context, long timeStamp) {
        return false;
    }

    public boolean addSendButtonLayout(LinearLayout btnPanel) {
        return false;
    }

    public boolean addAttachmentViewSendButton(LinearLayout btnPanel) {
        return false;
    }

    public boolean initCTSendButton() {
        return false;
    }

    public boolean setCTSendButtonType() {
        return false;
    }

    public boolean hideCTButtonPanel() {
        return false;
    }

    public boolean showCTButtonPanel() {
        return false;
    }

    public boolean updateCTSendButtonStatue(boolean enable, boolean isMms) {
        return false;
    }

    public boolean updateCTTextCounter(int remainingInCurrentMessage, int msgCount) {
        return false;
    }

    public String getNumbersFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        boolean usingColon = intent.getBooleanExtra(USING_COLON, false);
        String selectContactsNumbers = intent.getStringExtra(SELECTION_CONTACT_RESULT);
        if (usingColon) {
            if (selectContactsNumbers == null || selectContactsNumbers.length() < 1) {
                return null;
            }
            String[] numberArray = selectContactsNumbers.split(NUMBERS_SEPARATOR_COLON);
            String numberTempl = "";
            int simcolonIndex = -1;
            int colonIndex = -1;
            int separatorIndex = -1;
            for (int index = 0; index < numberArray.length; index++) {
                numberTempl = numberArray[index];
                simcolonIndex = numberTempl.indexOf(NUMBERS_SEPARATOR_SIMCOLON);
                colonIndex = numberTempl.indexOf(NUMBERS_SEPARATOR_COMMA);
                if (simcolonIndex > 0) {
                    if (colonIndex < 0) {
                        separatorIndex = simcolonIndex;
                    } else if (simcolonIndex < colonIndex) {
                        separatorIndex = simcolonIndex;
                    } else if (colonIndex > 0) {
                        separatorIndex = colonIndex;
                    }
                } else {
                    if (colonIndex > 0) {
                        separatorIndex = colonIndex;
                    }
                }
                if (separatorIndex > 0) {
                    numberArray[index] = numberTempl.substring(0, separatorIndex);
                }
                simcolonIndex = -1;
                colonIndex = -1;
                separatorIndex = -1;
            }
            return TextUtils.join(NUMBERS_SEPARATOR_SIMCOLON, numberArray);
        }
        return selectContactsNumbers;
    }

    public int getSendParameter(Message msg, int sendSimId) {
        return sendSimId;
    }

    public boolean hideCtSendPanel() {
        return false;
    }

    public boolean isDualSendButtonEnable() {
        return false;
    }

    public void showDisableDRDialog(final Activity activity, final int simId) {
        return;
    }

    public void enableDRWarningDialog(Context context, boolean isEnable, int simId) {
        return;
    }

    public boolean isMassTextEnable() {
        MmsLog.d(TAG, "isMassTextEnable: false");
        return false;
    }

    @Override
    public boolean needConfirmMmsToSms() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setConfirmMmsToSms(boolean needConfirm) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isEnableLengthRequiredMmsToSms() {
        return false;
    }

    /// M: New plugin API @{
    public void addMmsUrlToBookMark(Context context, ContextMenu menu, int menuId, ArrayList<String> urls, int titleString, int icon) {
        MmsLog.d(TAG, "addMmsUrlToBookMark");
        return;
    }
    /// @}
}
