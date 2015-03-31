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
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
/// M: New plugin API @{
import java.util.ArrayList;
/// @}

public interface IMmsCompose {
    int CAPTURE_PIC_NORMAL = 0;

    int CAPTURE_PIC_AT_TEMP_FILE = 1;

    String USING_COLON = "USE_COLON";

    String SELECTION_CONTACT_RESULT = "contactId";

    String NUMBERS_SEPARATOR_COLON = ":";

    String NUMBERS_SEPARATOR_SIMCOLON = ";";

    String NUMBERS_SEPARATOR_COMMA = ",";
    /**
     * @param host the reference to IMmsComposeHost
     */
    void init(IMmsComposeHost host);

    /**
     * @param subjectEditor the control of subject editor
     */
    void configSubjectEditor(EditText subjectEditor);

    /**
     * @param menu the context menu of MmsCompose
     */
    void addContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, CharSequence text);

    /**
     * @param menu the context menu of MmsCompose
     */
    void addContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo);

    /**
     * M: For OP09; Add Context Menu for split message apart
     * @param menu
     * @param v
     * @param menuInfo
     * @param context
     * @param messageGroupId
     */
    void addSplitMessageContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo, Activity activity,
            long messageGroupId, int messagesCount);

    /**
     * M: For OP09; Add option menu for split thread apart
     * @param menu
     * @param activity
     * @param threadId
     */
    void addSplitThreadOptionMenu(Menu menu, Activity activity, long threadId);

    /**
     * M:For OP09;
     * @param item
     * @return
     */
    boolean onOptionsItemSelected(MenuItem item);

    /**
     * Returns whether append original sender while forwarding a sms.
     * 
     * @return whether append original sender while forwarding a sms.
     */
    boolean isAppendSender();

    /**
     * Returns how to capture a picture for inserting mms attachment.
     * 
     * @return how to capture a picture for inserting mms attachment.
     */
    int getCapturePicMode();

    /**
     * Returns whether add mms uri to bookmark if Mms'body has uri.
     * 
     * @return whether add mms uri to bookmark when Mms'body has uri.
     */
    /// M:Code analyze 01,For new feature CMCC_MMS in ALPS00325381 , MMS easy
    /// porting check in. @{
    boolean isAddMmsUrlToBookMark();
    /// @}

    boolean isMultiCompose();

    /**
     * get the location of number
     * @param context
     * @param number
     * @return
     */
    String getNumberLocation(Context context, String number);

    /**
     * M:
     * @param uriSrc
     * @param threadId
     * @return
     */
    Uri getConverationUri(Uri uriSrc, long threadId);

    /**
     * M: delete mass text msg
     * @param msgId
     * @param timeStamp
     * @return
     */
    boolean deleteMassTextMsg(long msgId, long timeStamp);

    /**
     * M: lock mass text msg.
     * @param context
     * @param msgId
     * @param timeStamp
     * @param lock
     * @return true: compose need continue to lock msg with the old lock process.
     */
    boolean lockMassTextMsg(Context context, long msgId, long timeStamp, boolean lock);

    /**
     * M: show mass text msg details
     * @param context
     * @param timeStamp
     * @return
     */
    boolean showMassTextMsgDetail(Context context, long timeStamp);

    /**
     * M: add sendButton to linear layout
     * @param btnPanel
     * @return
     */
    boolean addSendButtonLayout(LinearLayout btnPanel);

    /**
     * M: add attachment view a send button;
     * @param btnPanel
     * @return
     */
    boolean addAttachmentViewSendButton(LinearLayout btnPanel);

    /**
     * M: init OP09 send buttons.
     * @return
     */
    boolean initCTSendButton();

    boolean setCTSendButtonType();

    boolean hideCTButtonPanel();

    boolean showCTButtonPanel();

    boolean updateCTSendButtonStatue(boolean enable, boolean isMms);

    boolean updateCTTextCounter(int remainingInCurrentMessage, int msgCount);

    String getNumbersFromIntent(Intent intent);

    int getSendParameter(Message msg, int sendSimId);

    boolean hideCtSendPanel();

    boolean isDualSendButtonEnable();

    void showDisableDRDialog(final Activity activity, final int simId);

    void enableDRWarningDialog(Context context, boolean isEnable, int simId);

    boolean isMassTextEnable();

    /// M: New plugin API @{
    /**
     * Add menu to add uri bookmark for Mms
     * @param context    Context use to call browser
     * @param menu       Menu object
     * @param menuId    Menu item id
     * @param urls          Urls to add
     * @param titleString Title of dialog
     * @param icon         Icon of dialog
     * @return void
     */
    void addMmsUrlToBookMark(Context context, ContextMenu menu,
                                    int menuId, ArrayList<String> urls,
                                    int titleString, int icon);
    /// @}

    /**
     * M:confirm whether need to change the MMS to SMS .
     * @return
     */
    boolean needConfirmMmsToSms();

    /**
     * Set the sign for message
     * @param needConfirm
     */
    void setConfirmMmsToSms(boolean needConfirm);

    /**
     *
     * @return
     */
    boolean isEnableLengthRequiredMmsToSms();
}

