/*
 * Copyright (C) 2008 Esmertec AG.
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

package com.android.mms.ui;

import com.android.mms.R;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.util.ItemLoadedCallback;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
/// M:
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.AndroidException;

import com.android.mms.ExceedMessageSizeException;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.MmsPluginManager;
import com.android.mms.model.FileAttachmentModel;
import com.android.mms.model.VCardModel;
import com.mediatek.encapsulation.android.provider.EncapsulatedSettings;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.com.mediatek.pluginmanager.EncapsulatedPluginManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSimInfoManager;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.drm.EncapsulatedDrmManagerClient;
import com.mediatek.encapsulation.com.mediatek.internal.EncapsulatedR;
import com.mediatek.mms.ext.IMmsCompose;
import com.mediatek.mms.ext.IMmsComposeHost;
import com.mediatek.mms.ext.IMmsUtils;
import com.mediatek.mms.ext.MmsComposeImpl;

//add for attachment enhance by feng
//import packages
import com.android.mms.MmsPluginManager;
import com.android.mms.model.FileModel;
import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;
import com.mediatek.encapsulation.com.google.android.mms.EncapsulatedContentType;
import com.mediatek.encapsulation.android.drm.EncapsulatedDrmUiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an embedded editor/view to add photos and sound/video clips
 * into a multimedia message.
 */
public class AttachmentEditor extends LinearLayout implements IMmsComposeHost{
    private static final String TAG = "AttachmentEditor";

    static final int MSG_EDIT_SLIDESHOW   = 1;
    static final int MSG_SEND_SLIDESHOW   = 2;
    static final int MSG_PLAY_SLIDESHOW   = 3;
    static final int MSG_REPLACE_IMAGE    = 4;
    static final int MSG_REPLACE_VIDEO    = 5;
    static final int MSG_REPLACE_AUDIO    = 6;
    static final int MSG_PLAY_VIDEO       = 7;
    static final int MSG_PLAY_AUDIO       = 8;
    static final int MSG_VIEW_IMAGE       = 9;
    static final int MSG_REMOVE_ATTACHMENT = 10;
    /// M: add for attachment enhance
    static final int MSG_REMOVE_EXTERNAL_ATTACHMENT = 11;
    static final int MSG_REMOVE_SLIDES_ATTACHMENT = 12;

    private final Context mContext;
    private Handler mHandler;

    private SlideViewInterface mView;
    private SlideshowModel mSlideshow;
    private Presenter mPresenter;
    private boolean mCanSend;
    private Button mSendButton;

    /// M: add for vCard
    private View mFileAttachmentView;
    /// M: Compose Plugin for OP09@{
    private IMmsCompose mMmsComposePlugin = null;
    private IMmsUtils mMmsUtilsPlugin = null;

    private View mBtnPanel;
    private ImageButton mButtonBig;
    private ImageButton mButtonSmall;
    /// @}

    public AttachmentEditor(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;
        initPlugin(context);
    }

    /**
     * Returns true if the attachment editor has an attachment to show.
     */
    public boolean update(WorkingMessage msg) {
        hideView();
        mView = null;
        /// M: add for vcard @{
        mFileAttachmentView = null;
        mWorkingMessage = msg;
        /// @}
        // If there's no attachment, we have nothing to do.
        if (!msg.hasAttachment()) {
            return false;
        }

        // Get the slideshow from the message.
        mSlideshow = msg.getSlideshow();
        try {
            /// M: fix bug ALPS00947784, check and remove FileAttachment
            IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)
                    MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
            if (mMmsAttachmentEnhancePlugin == null
                    || !mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance()) {
                checkFileAttacment(msg);
            }
            /// M: for vcard: file attachment view and other views are exclusive to each other
            if (mSlideshow.sizeOfFilesAttach() > 0) {
                mFileAttachmentView = createFileAttachmentView(msg);
                if (mFileAttachmentView != null) {
                    mFileAttachmentView.setVisibility(View.VISIBLE);
                }
            }
            //add for attachment enhance
            if (mSlideshow.size() == 0) {
                //It only has attachment but not slide
                return true;
            }
            /// M: fix bug ALPS01238218
            if (mSlideshow.size() > 1 && !msg.getIsUpdateAttachEditor()) {
                MmsLog.d(TAG, "AttachmentEditor update, IsUpdateAttachEditor == false");
                return true;
            }
            mView = createView(msg);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if ((mPresenter == null) || !mSlideshow.equals(mPresenter.getModel())) {
            mPresenter = PresenterFactory.getPresenter(
                    "MmsThumbnailPresenter", mContext, mView, mSlideshow);
        } else {
            mPresenter.setView(mView);
        }

        if ((mPresenter != null) && mSlideshow.size() > 1) {
            mPresenter.present(null);
        } else if (mSlideshow.size() == 1) {
            SlideModel sm = mSlideshow.get(0);
            if ((mPresenter != null) && (sm != null) && (sm.hasAudio() || sm.hasImage() || sm.hasVideo())) {
                mPresenter.present(null);
            }
        }
        return true;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setCanSend(boolean enable) {
        if (mCanSend != enable) {
            mCanSend = enable;
            updateSendButton();
        }
        mMmsComposePlugin.setCTSendButtonType();
    }

    private void updateSendButton() {
        if (null != mSendButton) {
            if (mCanSend && MmsConfig.isSmsEnabled(mContext)) {
                mSendButton.setEnabled(true);
                mSendButton.setFocusable(true);
            } else {
                mSendButton.setEnabled(false);
                mSendButton.setFocusable(false);
            }
        }
        mMmsComposePlugin.updateCTSendButtonStatue(mCanSend, true);
    }

    public void hideView() {
        if (mView != null) {
            ((View)mView).setVisibility(View.GONE);
        }
        /// M: add for vcard
        if (mFileAttachmentView != null) {
            mFileAttachmentView.setVisibility(View.GONE);
        }
    }

    private View getStubView(int stubId, int viewId) {
        View view = findViewById(viewId);
        if (view == null) {
            ViewStub stub = (ViewStub) findViewById(stubId);
            view = stub.inflate();
        }
        return view;
    }

    private class MessageOnClick implements OnClickListener {
        private int mWhat;

        public MessageOnClick(int what) {
            mWhat = what;
        }

        public void onClick(View v) {
            MmsLog.d(TAG, "AttachmentEditor onclick: mWhat = " + mWhat);
            Message msg = Message.obtain(mHandler, mWhat);
            msg.sendToTarget();
        }
    }
    /// m: @{
    //    private SlideViewInterface createView() {
    private SlideViewInterface createView(WorkingMessage msg) {
    /// @}
        boolean inPortrait = inPortraitMode();

        if (mSlideshow.size() > 1) {
            return createSlideshowView(inPortrait, msg);
        }

        /// M: @{

        //add for attachment enhance

        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE); 

        final int NOT_OP01 = 0;
        final int IS_OP01 = 1;
        int flag = NOT_OP01; // 0 means not OP01, 1 means OP01

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                flag = IS_OP01;
            }
        }

         ///@}
        SlideModel slide = mSlideshow.get(0);
        /// M: before using SlideModel's function,we should make sure it is
        // null or not
        if (null == slide) {
            throw new IllegalArgumentException();
        }
        if (slide.hasImage()) {
            if (flag == NOT_OP01) {
            return createMediaView(
                    R.id.image_attachment_view_stub,
                    R.id.image_attachment_view,
                    R.id.view_image_button, R.id.replace_image_button, R.id.remove_image_button,
                /// m: @{
                // MSG_VIEW_IMAGE, MSG_REPLACE_IMAGE, MSG_REMOVE_ATTACHMENT);
                    R.id.media_size_info, msg.getCurrentMessageSize(),
                        MSG_VIEW_IMAGE, MSG_REPLACE_IMAGE, MSG_REMOVE_ATTACHMENT, msg);
                /// @}
            }else {
                //OP01
                return createMediaView(
                            R.id.image_attachment_view_stub,
                            R.id.image_attachment_view,
                            R.id.view_image_button, R.id.replace_image_button, R.id.remove_image_button,
                            R.id.media_size_info, msg.getCurrentMessageSize(),
                            MSG_VIEW_IMAGE, MSG_REPLACE_IMAGE, MSG_REMOVE_SLIDES_ATTACHMENT, msg);
            } 
        } else if (slide.hasVideo()) {
            if (flag == NOT_OP01) {
            return createMediaView(
                    R.id.video_attachment_view_stub,
                    R.id.video_attachment_view,
                    R.id.view_video_button, R.id.replace_video_button, R.id.remove_video_button,
                /// M: @{
                // MSG_PLAY_VIDEO, MSG_REPLACE_VIDEO, MSG_REMOVE_ATTACHMENT);
                    R.id.media_size_info, msg.getCurrentMessageSize(),
                        MSG_PLAY_VIDEO, MSG_REPLACE_VIDEO, MSG_REMOVE_ATTACHMENT, msg);
                /// @}
            }else  {
                //OP01
                return createMediaView(
                            R.id.video_attachment_view_stub,
                            R.id.video_attachment_view,
                            R.id.view_video_button, R.id.replace_video_button, R.id.remove_video_button,
                            R.id.media_size_info, msg.getCurrentMessageSize(),
                            MSG_PLAY_VIDEO, MSG_REPLACE_VIDEO, MSG_REMOVE_SLIDES_ATTACHMENT, msg);	
            }
        } else if (slide.hasAudio()) {
            if (flag == NOT_OP01) {
            return createMediaView(
                    R.id.audio_attachment_view_stub,
                    R.id.audio_attachment_view,
                    R.id.play_audio_button, R.id.replace_audio_button, R.id.remove_audio_button,
                /// M: @{
                // MSG_PLAY_AUDIO, MSG_REPLACE_AUDIO, MSG_REMOVE_ATTACHMENT);
                    R.id.media_size_info, msg.getCurrentMessageSize(),
                        MSG_PLAY_AUDIO, MSG_REPLACE_AUDIO, MSG_REMOVE_ATTACHMENT, msg);
                /// @}
        } else {
                //OP01
                return createMediaView(
                            R.id.audio_attachment_view_stub,
                            R.id.audio_attachment_view,
                            R.id.play_audio_button, R.id.replace_audio_button, R.id.remove_audio_button,
                            R.id.media_size_info, msg.getCurrentMessageSize(),
                            MSG_PLAY_AUDIO, MSG_REPLACE_AUDIO, MSG_REMOVE_SLIDES_ATTACHMENT, msg);	
            }
        } else {
            throw new IllegalArgumentException();
        }
    }


    /**
     * What is the current orientation?
     */
    private boolean inPortraitMode() {
        final Configuration configuration = mContext.getResources().getConfiguration();
        return configuration.orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private SlideViewInterface createMediaView(
            int stub_view_id, int real_view_id,
            int view_button_id, int replace_button_id, int remove_button_id,
            /// M: @{
            // int viewMessage, int replaceMessage, int removeMessage) {
            int sizeViewId, int msgSize,
            int viewMessage, int replaceMessage, int removeMessage, WorkingMessage msg) {
            /// @}
        LinearLayout view = (LinearLayout)getStubView(stub_view_id, real_view_id);
        view.setVisibility(View.VISIBLE);

        Button viewButton = (Button) view.findViewById(view_button_id);
        Button replaceButton = (Button) view.findViewById(replace_button_id);
        Button removeButton = (Button) view.findViewById(remove_button_id);
        /// M: disable when non-default sms
        boolean smsEnable = MmsConfig.isSmsEnabled(mContext);
        replaceButton.setEnabled(smsEnable);
        removeButton.setEnabled(smsEnable);

        /// M: @{
        /// M: show Mms Size  
        mMediaSize = (TextView) view.findViewById(sizeViewId); 
        int sizeShow = (msgSize - 1) / 1024 + 1;
        String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        mMediaSize.setText(info); 
        /// @}

        viewButton.setOnClickListener(new MessageOnClick(viewMessage));
        replaceButton.setOnClickListener(new MessageOnClick(replaceMessage));
        removeButton.setOnClickListener(new MessageOnClick(removeMessage));

        /// M: @{
        if (mFlagMini) {
            replaceButton.setVisibility(View.GONE);
        }
        /// @}
        return (SlideViewInterface) view;
    }

    /// M: @{
    // private SlideViewInterface createSlideshowView(boolean inPortrait) {
    private SlideViewInterface createSlideshowView(boolean inPortrait, WorkingMessage msg) {
    /// @}
        LinearLayout view =(LinearLayout) getStubView(
                R.id.slideshow_attachment_view_stub,
                R.id.slideshow_attachment_view);
        view.setVisibility(View.VISIBLE);

        Button editBtn = (Button) view.findViewById(R.id.edit_slideshow_button);
        mSendButton = (Button) view.findViewById(R.id.send_slideshow_button);
       /// M: @{
        mSendButton.setOnClickListener(new MessageOnClick(MSG_SEND_SLIDESHOW));
        /// @}

        updateSendButton();
        final ImageButton playBtn = (ImageButton) view.findViewById(
                R.id.play_slideshow_button);
        /// M: @{
        if (EncapsulatedFeatureOption.MTK_DRM_APP && msg.mHasDrmPart) {
            MmsLog.i(TAG, "mHasDrmPart");
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.mms_play_btn);
            Drawable front = mContext.getResources().getDrawable(
                    EncapsulatedR.drawable.drm_red_lock);
            EncapsulatedDrmManagerClient drmManager = MmsApp.getApplication().getDrmManagerClient();
            Bitmap drmBitmap = EncapsulatedDrmUiUtils.overlayBitmap(drmManager, bitmap, front);
            playBtn.setImageBitmap(drmBitmap);
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        } else {
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.mms_play_btn);
            playBtn.setImageBitmap(bitmap);
        }

        /// M: show Mms Size  
        mMediaSize = (TextView) view.findViewById(R.id.media_size_info); 
               int sizeShow = (msg.getCurrentMessageSize() - 1) / 1024 + 1;
        String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        
        mMediaSize.setText(info);
        /// @}

        editBtn.setEnabled(true);
        editBtn.setOnClickListener(new MessageOnClick(MSG_EDIT_SLIDESHOW));
        mSendButton.setOnClickListener(new MessageOnClick(MSG_SEND_SLIDESHOW));
        playBtn.setOnClickListener(new MessageOnClick(MSG_PLAY_SLIDESHOW));

        Button removeButton = (Button) view.findViewById(R.id.remove_slideshow_button);
        //add for attachment enhance

        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                //OP01
                removeButton.setOnClickListener(new MessageOnClick(MSG_REMOVE_SLIDES_ATTACHMENT));
            } else{
                //not OP01
                removeButton.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
            }
        }else {
            //common
        removeButton.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
        }
        /// M: For OP09;
        mMmsComposePlugin.initCTSendButton();
        /// M: disable when non-default sms
        boolean smsEnable = MmsConfig.isSmsEnabled(mContext);
        editBtn.setEnabled(smsEnable);
        removeButton.setEnabled(smsEnable);
        return (SlideViewInterface) view;
    }

    /// M: 
    private WorkingMessage mWorkingMessage;
    private TextView mMediaSize;
    private TextView mFileAttachSize;
    private boolean mFlagMini = false;

    public void update(WorkingMessage msg, boolean isMini) {
        mFlagMini = isMini;
        update(msg);
    }

    public void onTextChangeForOneSlide() throws ExceedMessageSizeException {
        if (mWorkingMessage == null || mWorkingMessage.hasSlideshow()) {
            return;
        } else {
            /// M: fix bug ALPS01270248, update FileAttachment Size
            if (mFileAttachSize != null && mWorkingMessage.hasAttachedFiles() && mSlideshow != null) {
                IMmsAttachmentEnhance mmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)
                        MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
                if (mmsAttachmentEnhancePlugin == null
                        || !mmsAttachmentEnhancePlugin.isSupportAttachmentEnhance()) {
                    List<FileAttachmentModel> attachFiles = mSlideshow.getAttachFiles();
                    int attachSize = 0;
                    if (attachFiles != null && attachFiles.size() == 1) {
                        attachSize = attachFiles.get(0).getAttachSize();
                    }

                    int textSize = 0;
                    if (mSlideshow.get(0) != null && mSlideshow.get(0).hasText()) {
                        textSize = mSlideshow.get(0).getText().getMediaPackagedSize();
                    }
                    int totalSize = attachSize + textSize + SlideshowModel.mReserveSize;
                    String info = MessageUtils.getHumanReadableSize(totalSize)
                            + "/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
                    mFileAttachSize.setText(info);
                }
            }
        }

        if (mMediaSize == null) {
            return;
        }
        /// M: borrow this method to get the encoding type
        /// int[] params = SmsMessage.calculateLength(s, false);
        int totalSize = 0;
        if (mWorkingMessage.hasAttachment()) {
            totalSize = mWorkingMessage.getCurrentMessageSize();
        }
        /// M: show
        int sizeShow = (totalSize - 1) / 1024 + 1;
        String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        mMediaSize.setText(info);
    }
    /// @}

    /// M: add for vcard
    private View createFileAttachmentView(WorkingMessage msg) {
        /// M: for OP09;
        boolean isCtFeature = MmsConfig.isSupportVCardPreview();
        List<FileAttachmentModel> attachFiles = mSlideshow.getAttachFiles();
/// M: @{
        //add for attachment enhance

        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE); 

        if (attachFiles == null) {
            Log.e(TAG, "createFileAttachmentView, oops no attach files found.");
            return null;
        } else {
            if (mMmsAttachmentEnhancePlugin != null) {
                if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == false) {
                    //NOT for OP01
                    if (attachFiles.size() != 1) {
                        return null;
                    }
                }
            } else {
                if (attachFiles.size() != 1) {
                    return null;
        }
            }
        }
/// @}
        FileAttachmentModel attach = attachFiles.get(0);
        Log.i(TAG, "createFileAttachmentView, attach " + attach.toString());
        final View view = getStubView(R.id.file_attachment_view_stub, R.id.file_attachment_view);
        view.setVisibility(View.VISIBLE);
        /// M: For OP09 @{
        ImageView thumb = (ImageView) view.findViewById(R.id.file_attachment_thumbnail);
        if (isCtFeature) {
            thumb.setVisibility(View.GONE);
            thumb = (ImageView) view.findViewById(R.id.file_attachment_thumbnail2);
            thumb.setVisibility(View.VISIBLE);
        }
        TextView name = (TextView) view.findViewById(R.id.file_attachment_name_info);
        TextView name2 = (TextView) view.findViewById(R.id.file_attachment_name_info2);
        /// @}
        String nameText = null;
        int thumbResId = -1;

        int attachSize = 0;
        //get external attachment size
        for (int i = 0; i < attachFiles.size(); i++) {
            attachSize += attachFiles.get(i).getAttachSize();
        }

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                //Op01 plugin
    /// M: @{
                //add for attachment enhance
               if (attachFiles.size() > 1) {
                    //multi attachments files
                    MmsLog.i(TAG, "createFileAttachmentView, attachFiles.size() > 1");
                    nameText = mContext.getString(R.string.file_attachment_common_name, String.valueOf(attachFiles.size()));
                    thumbResId = R.drawable.multi_files;
                } else if (attachFiles.size() == 1){
                    //single attachment(file)
                    if (attach.isVCard()) {
                        // vCard
                        nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
                        thumbResId = R.drawable.ic_vcard_attach;
                    } else if (attach.isVCalendar()) {
                        // VCalender
                        nameText = mContext.getString(R.string.file_attachment_vcalendar_name, attach.getSrc());
                        thumbResId = R.drawable.ic_vcalendar_attach;
                    } else {
                        // other attachment
                        nameText = attach.getSrc();
                        thumbResId = R.drawable.unsupported_file;
                    }
                }
    /// @}
            }else {
            //common
        if (attach.isVCard()) {
            /// M: modify For OP09 {
                if (isCtFeature) {
                    nameText = ((VCardModel) attach).getDisplayName();
                    if (TextUtils.isEmpty(nameText)) {
                        nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
                    }
                    thumbResId = R.drawable.ipmsg_chat_contact_vcard;

                    if (isCtFeature && name2 != null) {
                        if (((VCardModel) attach).getContactCount() > 1) {
                            name2.setText(" +" + (((VCardModel) attach).getContactCount() - 1));
                            name2.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
                    thumbResId = R.drawable.ic_vcard_attach;
                }
        } else if (attach.isVCalendar()) {
            nameText = mContext.getString(R.string.file_attachment_vcalendar_name, attach.getSrc());
                if (isCtFeature) {
                    thumbResId = R.drawable.ipmsg_chat_contact_calendar;
                } else {
                    thumbResId = R.drawable.ic_vcalendar_attach;
                }
        }/// @}
            }
        }else {
            //common
        if (attach.isVCard()) {
            nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcard_attach;
        } else if (attach.isVCalendar()) {
            nameText = mContext.getString(R.string.file_attachment_vcalendar_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcalendar_attach;
        }
        }
        name.setText(nameText);
        /// M: Add for OP09@{
        if ((!isCtFeature || !attach.isVCard() || ((VCardModel) attach).getContactCount() <= 1)
                && name2 != null) {
            name2.setText("");
            name2.setVisibility(View.GONE);
        }
        /// @}
        thumb.setImageResource(thumbResId);
        final TextView size = (TextView) view.findViewById(R.id.file_attachment_size_info);
        mFileAttachSize = size;
        if (mMmsAttachmentEnhancePlugin != null &&
            mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
            //OP01
            size.setText(MessageUtils.getHumanReadableSize(attachSize));
        } else {
            //Not OP01
            /// M: fix bug ALPS01270248, update FileAttachment Size
            int textSize = 0;
            if (mSlideshow.get(0) != null && mSlideshow.get(0).hasText()) {
                textSize = mSlideshow.get(0).getText().getMediaPackagedSize();
            }
            int totalSize = attach.getAttachSize() + textSize + SlideshowModel.mReserveSize;
            size.setText(MessageUtils.getHumanReadableSize(totalSize)
                + "/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K");
        }

        final ImageView remove = (ImageView) view.findViewById(R.id.file_attachment_button_remove);
        final ImageView divider = (ImageView) view.findViewById(R.id.file_attachment_divider);
        divider.setVisibility(View.VISIBLE);
        remove.setVisibility(View.VISIBLE);
        //add for attachment enhance
        //IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE); 

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                //OP01
                remove.setOnClickListener(new MessageOnClick(MSG_REMOVE_EXTERNAL_ATTACHMENT));
            }else {
                //not OP01
                remove.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
            }
        }else {
            //not OP01
            remove.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
        }
        /// M: disable when non-default sms
        boolean smsEnable = MmsConfig.isSmsEnabled(mContext);
        remove.setEnabled(smsEnable);
        return view;
    }

   /**
    * M: init plugin for OP09
    * @param context
    */
    private void initPlugin(Context context){
        try {
            mMmsComposePlugin = (IMmsCompose)EncapsulatedPluginManager.createPluginObject(context,
                                 IMmsCompose.class.getName());
            MmsLog.d(TAG, "operator mMmsComposePlugin = " + mMmsComposePlugin);
        } catch (AndroidException e) {
            mMmsComposePlugin = new MmsComposeImpl(context);
            MmsLog.d(TAG, "default mMmsComposePlugin = " + mMmsComposePlugin);
        }
        mMmsComposePlugin.init(this);
        mMmsUtilsPlugin = (IMmsUtils)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MESSAGE_UTILS);
    }
    /**
     * M:
     * @return
     */
    public boolean initCTSendButton() {
        if (mSendButton != null) {
            mSendButton.setVisibility(View.GONE);
        }
        getSimInfoListInHost();
        mBtnPanel = findViewById(R.id.ct_button_slideshow_panel);
        mBtnPanel.setVisibility(View.VISIBLE);
        mButtonBig = (ImageButton) findViewById(R.id.send_slideshow_button_big);
        mButtonBig.setVisibility(View.VISIBLE);
        mButtonSmall = (ImageButton) findViewById(R.id.send_slideshow_button_small);
        if (mSimCount == 2) {
            mButtonSmall.setVisibility(View.VISIBLE);
        } else {
            mButtonSmall.setVisibility(View.GONE);
            android.view.ViewGroup.LayoutParams  lp = mButtonBig.getLayoutParams();
            lp.width = this.getResources().getDimensionPixelOffset(R.dimen.attchment_view_send_button_length);
            mButtonBig.setLayoutParams(lp);
        }
        mMmsComposePlugin.setCTSendButtonType();
        mMmsComposePlugin.hideCtSendPanel();
        return true;
    }

    public boolean setCTSendButtonType() {
        Drawable bigImageId = null;
        Drawable smallImageId = null;
        Object[][] resIds = null;
        mButtonDrawable = new Drawable[2];
        int slotId = 0;
        if (mSimCount == 0) {
            return true;
        } else if (mSimCount == 1) {
            slotId = mSimInfoList.get(0).getSlot();
        } else if (mSimCount == 2) {
            int defaultSimId = (int) EncapsulatedSettings.System.getLong(mContext.getContentResolver(),
                EncapsulatedSettings.System.SMS_SIM_SETTING, EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET);
            if (defaultSimId == EncapsulatedSettings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
                slotId = 0;
            } else if (defaultSimId == EncapsulatedSettings.System.DEFAULT_SIM_NOT_SET) {
                slotId = 0;
            } else {
                slotId = EncapsulatedSimInfoManager.getSlotById(mContext, defaultSimId);
            }
        }

        IMmsUtils mmsUtilsPlugin = (IMmsUtils) MmsPluginManager
                .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MESSAGE_UTILS);
        resIds = mmsUtilsPlugin.getSendButtonResourceIdBySlotId(mContext, slotId, false);
        if (resIds != null) {
            mButtonDrawable[0] = (Drawable) resIds[0][0];
            mButtonDrawable[1] = (Drawable) resIds[1][0];
        }
        if (mButtonBig.isEnabled()) {
            resIds = mmsUtilsPlugin.getSendButtonResourceIdBySlotId(mContext, slotId, true);
        }
        bigImageId = (Drawable) resIds[0][0];
        smallImageId = (Drawable) resIds[1][0];

        mButtonSlotIds = new int[2];
        mButtonColors = new int[2];

        mButtonSlotIds[0] = (Integer) resIds[0][1];
        mButtonSlotIds[1] = (Integer) resIds[1][1];
        mButtonColors[0] = (Integer) resIds[0][2];
        mButtonColors[1] = (Integer) resIds[1][2];
        if (mButtonBig != null) {
            mButtonBig.setImageDrawable(bigImageId);
            mButtonBig.setOnClickListener(mCtButtonClickListener);
        }
        if (mButtonSmall != null) {
            mButtonSmall.setImageDrawable(smallImageId);
            mButtonSmall.setOnClickListener(mCtButtonClickListener);
        }
        return true;
    }

    private List<EncapsulatedSimInfoManager> mSimInfoList;
    private int mSimCount;//The count of current sim cards.  0/1/2
    private int[] mButtonSlotIds;
    private int[] mButtonColors;
    private Drawable[] mButtonDrawable;
    private int send_sim_id = -1;
    private void getSimInfoListInHost() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            //mSimInfoList = SIMInfo.getInsertedSIMList(this);
            mSimInfoList = new ArrayList<EncapsulatedSimInfoManager>();
            EncapsulatedSimInfoManager sim1Info = EncapsulatedSimInfoManager.getSimInfoBySlot(mContext, EncapsulatedPhone.GEMINI_SIM_1);
            EncapsulatedSimInfoManager sim2Info = EncapsulatedSimInfoManager.getSimInfoBySlot(mContext, EncapsulatedPhone.GEMINI_SIM_2);
            if (sim1Info != null) {
                mSimInfoList.add(sim1Info);
            }
            if (sim2Info != null) {
                mSimInfoList.add(sim2Info);
            }
            mSimCount = mSimInfoList.isEmpty()? 0: mSimInfoList.size();
            MmsLog.v(TAG, "ComposeMessageActivity.getSimInfoList(): mSimCount = " + mSimCount);
        } else { // single SIM
            /** M: MTK Encapsulation ITelephony */
            // ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            EncapsulatedTelephonyService phone = EncapsulatedTelephonyService.getInstance();
            if (phone != null) {
                try {
                    mSimCount = phone.isSimInsert(0) ? 1: 0;
                } catch (RemoteException e) {
                    MmsLog.e(MmsApp.TXN_TAG, "check sim insert status failed");
                    mSimCount = 0;
                }
            }
        }
    }

    View.OnClickListener mCtButtonClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v == mButtonBig) {
                EncapsulatedSimInfoManager sm = EncapsulatedSimInfoManager.getSimInfoBySlot(mContext,
                    mButtonSlotIds[0]);
                send_sim_id = (int) sm.getSimId();
            } else if (v == mButtonSmall) {
                EncapsulatedSimInfoManager sm = EncapsulatedSimInfoManager.getSimInfoBySlot(mContext,
                    mButtonSlotIds[1]);
                send_sim_id = (int) sm.getSimId();
            }
            Message msg = Message.obtain(mHandler, MSG_SEND_SLIDESHOW);
            msg.arg1 = send_sim_id;
            msg.sendToTarget();
        }
    };

    /**
     * M:
     * @param enable
     * @param isMms
     * @return
     */
    public boolean updateCTSendButtonStatue(boolean enable, boolean isMms) {
        if (mSendButton != null) {
            mSendButton.setVisibility(View.GONE);
        }
        if (mButtonBig == null) {
            return true;
        }
        // 1. If there is no sim card，the both will be disabled.
        // 2. If there is only one sim card, the big button will be enabled, and disable small button
        // 3. there are two sim cards. so, will enable all buttons.
        boolean bigEnable = false;
        boolean smallEnable = false;
        /// M: correct Button's status picture @{
        boolean bigBtnEnable = mButtonBig.isEnabled();
        boolean smallBtnEnable = mButtonSmall.isEnabled();

        if (bigBtnEnable != enable && mButtonSlotIds != null && mButtonColors != null) {
            Drawable drawable = null;
            if (enable) {
                drawable = mMmsUtilsPlugin.getActivatedButtonIconBySlotId(mButtonSlotIds[0], false, mButtonColors[0]);
            } else {
                if (mButtonDrawable != null) {
                    drawable = mButtonDrawable[0];
                }
            }
            if (drawable != null) {
                mButtonBig.setImageDrawable(drawable);
            }
        }
        if (smallBtnEnable != enable && mButtonSlotIds != null && mButtonColors != null) {
            Drawable drawable = null;
            if (enable) {
                drawable = mMmsUtilsPlugin.getActivatedButtonIconBySlotId(mButtonSlotIds[1], true, mButtonColors[1]);
            } else {
                if (mButtonDrawable != null) {
                    drawable = mButtonDrawable[1];
                }
            }
            if (drawable != null) {
                mButtonSmall.setImageDrawable(drawable);
            }
        }
        /// @}
        if (mSimCount == 0) {
            bigEnable = false;
            smallEnable = false;
        } else if (mSimCount == 1) {
            bigEnable = enable;
            smallEnable = false;
            mButtonSmall.setVisibility(View.GONE);
        } else if (mSimCount == 2) {
            bigEnable = enable;
            smallEnable = enable;
        }
        if (mButtonBig != null) {
            mButtonBig.setEnabled(bigEnable);
            mButtonBig.setFocusable(bigEnable);
        }
        if (mButtonSmall != null) {
            mButtonSmall.setEnabled(smallEnable);
        }
        return true;
    }

    public boolean hideCTButtonPanel() {
        return false;
    }

    public boolean showCTButtonPanel() {
        return false;
    }

    public boolean updateCTTextCounter(int remainingInCurrentMessage, int msgCount) {
        return false;
    }

    public void deleteMassTextMsg(long msgId, long timeStamp){

    }

    public void showMassTextMsgDetails(String[] items, DialogInterface.OnClickListener clickListener, String btnStr, boolean showButton) {
    }

    public boolean hideCtSendPanel() {
        if (mSimCount > 0) {
            return true;
        }

        View ctView = this.findViewById(R.id.ct_button_slideshow_panel);
        ctView.setVisibility(View.GONE);
        if (mSendButton != null) {
            mSendButton.setVisibility(View.VISIBLE);
        }
        return true;
    }

    /// M: fix bug ALPS00947784, check and remove FileAttachment
    private void checkFileAttacment(WorkingMessage msg) {
        if (msg.getSlideshow().sizeOfFilesAttach() > 0 && msg.hasMediaAttachments()) {
            msg.removeAllFileAttaches();
        }
    }
}
