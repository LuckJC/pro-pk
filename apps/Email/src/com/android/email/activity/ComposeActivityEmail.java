/**
 * Copyright (c) 2013, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.email.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.MultiAutoCompleteTextView;

import com.android.email.R;
import com.android.emailcommon.Configuration;
import com.android.emailcommon.Logging;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.Utility;
import com.android.ex.chips.MTKRecipientEditTextView;
import com.android.ex.chips.MTKRecipientList;
import com.android.mail.analytics.Analytics;
import com.android.mail.compose.AttachmentsView.AttachmentFailureException;
import com.android.mail.compose.ComposeActivity;
import com.android.mail.providers.Attachment;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.collect.Sets;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmUtils;
import com.mediatek.email.attachment.AttachmentHelper;
import com.mediatek.email.attachment.AttachmentTypeSelectorAdapter;
import com.mediatek.mail.ui.utils.ChipsAddressTextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComposeActivityEmail extends ComposeActivity
        implements InsertQuickResponseDialog.Callback {
    static final String INSERTQUICKRESPONE_DIALOG_TAG = "insertQuickResponseDialog";
    /// M: support add attachment @{
    private static final String TAG = "ComposeActivityEmail";
    private AlertDialog.Builder mDialogBuilder = null;
    private AttachmentTypeSelectorAdapter mAttachmentTypeSelectorAdapter;
    /// @}

    //M: add attachment process dialog.@{
    private LoadingAttachProgressDialog mProgressDialog = null;
    private final EmailAsyncTask.Tracker mTaskTracker = new EmailAsyncTask.Tracker();
    /// @}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.email_compose_menu_extras, menu);
        /// M: reset google default menu, hide attach photo, video.@{
        resetMenu(menu);
        /// @}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.insert_quick_response_menu_item) {
            InsertQuickResponseDialog dialog = InsertQuickResponseDialog
                    .newInstance(null, mReplyFromAccount.account);
            dialog.show(getFragmentManager(), INSERTQUICKRESPONE_DIALOG_TAG);
        /// M: support more kinds of attachment.
        } else if (id == R.id.add_attachment) {
            onAddAttachment();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onQuickResponseSelected(CharSequence quickResponse) {
        final int selEnd = mBodyView.getSelectionEnd();
        final int selStart = mBodyView.getSelectionStart();

        if (selEnd >= 0 && selStart >= 0) {
            final SpannableStringBuilder messageBody =
                    new SpannableStringBuilder(mBodyView.getText());
            final int replaceStart = selStart < selEnd ? selStart : selEnd;
            final int replaceEnd = selStart < selEnd ? selEnd : selStart;
            messageBody.replace(replaceStart, replaceEnd, quickResponse);
            mBodyView.setText(messageBody);
            mBodyView.setSelection(replaceStart + quickResponse.length());
        } else {
            mBodyView.append(quickResponse);
            mBodyView.setSelection(mBodyView.getText().length());
        }
    }

    /// M: support add Image/Audio/Video/Contact/Calendar/File attachments. @{

    /**
     * Since have added a more power attachments interface, hide some default entrances.
     */
    public void resetMenu(Menu menu) {
        MenuItem photoAtt = menu.findItem(R.id.add_photo_attachment);
        MenuItem videoAtt = menu.findItem(R.id.add_video_attachment);
        if (photoAtt != null) {
            photoAtt.setVisible(false);
        }
        if (videoAtt != null) {
            videoAtt.setVisible(false);
        }
        // hide some menu, since account not ready.
        MenuItem attachmentMemu = menu
                .findItem(R.id.insert_quick_response_menu_item);
        MenuItem quickResponseMenu = menu.findItem(R.id.add_attachment);
        boolean accountReady = mAccount != null && mAccount.isAccountReady();
        LogUtils.d(TAG, "reset attachment menu and quickResponse Menu, since account is read? %s", accountReady);
        if (attachmentMemu != null) {
            attachmentMemu.setVisible(accountReady);
        }
        if (quickResponseMenu != null) {
            quickResponseMenu.setVisible(accountReady);
        }
    }

    /**
     * Kick off a dialog to choose types of attachments: image, music and video.
     */
    private void onAddAttachment() {
        // check if can add more attachments
        mDialogBuilder = new AlertDialog.Builder(this);
        mDialogBuilder.setIcon(R.drawable.ic_dialog_attach);
        mDialogBuilder.setTitle(R.string.choose_attachment_dialog_title);
        mAttachmentTypeSelectorAdapter = new AttachmentTypeSelectorAdapter(this);
        mDialogBuilder.setAdapter(mAttachmentTypeSelectorAdapter,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAddingAttachment = true;
                        AttachmentHelper.addAttachment(
                                mAttachmentTypeSelectorAdapter
                                        .buttonToCommand(which), ComposeActivityEmail.this);
                        dialog.dismiss();
                    }
                });
        mDialogBuilder.show();
    }

    protected final void onActivityResult(int request, int result, Intent data) {
        // let supper handle it firstly, make sure the request is unique.
        super.onActivityResult(request, result, data);
        LogUtils.e(TAG, "ComposeActivityEmail onActivityResult [%d] data: %s ", request,
                (data != null) ? data.toString() : "NULL");

        if (data == null) {
            return;
        }
        switch (request) {
        case AttachmentHelper.REQUEST_CODE_ATTACH_IMAGE:
            addAttachmentAndUpdateView(data);
            break;
        case AttachmentHelper.REQUEST_CODE_ATTACH_VIDEO:
            addAttachmentAndUpdateView(data.getData());
            break;
        case AttachmentHelper.REQUEST_CODE_ATTACH_SOUND:
            addAttachmentAndUpdateView(data.getData());
            break;
        case AttachmentHelper.REQUEST_CODE_ATTACH_CONTACT:
            Bundle extras = data.getExtras();
            if (extras != null) {
                Uri uri = (Uri) extras.get(AttachmentHelper.ITEXTRA_CONTACTS);
                if (uri != null) {
                    addAttachmentAndUpdateView(uri);
                }
            } else {
                LogUtils.e(TAG,
                        "Can not get extras data from the attaching contact");
            }
            break;
        case AttachmentHelper.REQUEST_CODE_ATTACH_CALENDAR:
            // handle calendar
            addAttachmentAndUpdateView(data.getData());
            break;
        case AttachmentHelper.REQUEST_CODE_ATTACH_FILE:
            addAttachmentAndUpdateView(data.getData());
            break;
        default:
            LogUtils.i(TAG, "Can not handle the requestCode [%s] in onActivityResult method",request);
        }
        mAddingAttachment = false;
    }
    /// Attachment enhancement @}

    /**
     * M: add attachment in background
     */
    public void addAttachmentAndUpdateView(Uri contentUri) {
        LogUtils.e(TAG, "addAttachmentAndUpdateView uri: %s",
                (contentUri != null) ? (contentUri.toSafeString()) : "NULL");
        if (contentUri == null) {
            return;
        }
        ArrayList<Uri> uriArray = new ArrayList<Uri>();
        uriArray.add(contentUri);
        loadAttachmentsInBackground(uriArray);
    }

    /**
     * M: we cancel all the task if activity destroy
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTaskTracker.cancellAllInterrupt();
    }

    /**
     * M: MTK load attachment in background
     */
    protected void initAttachmentsFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            extras = Bundle.EMPTY;
        }
        final String action = intent.getAction();
        ArrayList<Uri> uriArray = new ArrayList<Uri>();
        if (!mAttachmentsChanged) {
            if (extras.containsKey(EXTRA_ATTACHMENTS)) {
                String[] uris = (String[]) extras.getSerializable(EXTRA_ATTACHMENTS);
                for (String uriString : uris) {
                    final Uri uri = Uri.parse(uriString);
                    uriArray.add(uri);
                }
            }
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    ArrayList<Parcelable> uris = extras
                            .getParcelableArrayList(Intent.EXTRA_STREAM);
                    for (Parcelable uri : uris) {
                        Uri newUri = (Uri) uri;
                        uriArray.add(newUri);
                    }
                } else {
                    final Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                    if (uri != null) {
                        uriArray.add(uri);
                    }
                }
            }

            loadAttachmentsInBackground(uriArray);
        }
    }

    /**
     *  M: MTK load attachment in background EmailAsyncTask, and show Load
     *  Attachment ProgressDialog when attachments is loading. avoid user make
     *  UI operation cause ANR.
     */
    public void loadAttachmentsInBackground(final ArrayList<Uri> uris) {
        LogUtils.e(TAG, "loadAttachmentsInBackground: %s", uris.toString());
        final ArrayList<Uri> attachUris = uris;
        if (uris.size() > 0) {
            new EmailAsyncTask<Void, Void, List<Attachment>>(mTaskTracker) {

                private boolean mContainFailedAttachment = false;
                @Override
                protected List<Attachment> doInBackground(Void... params) {
                    mContainFailedAttachment = false;
                    List<Attachment> attachments = new ArrayList<Attachment>();
                    showLoadAttachmentProgressDialog(this);
                    for (Uri uri : attachUris) {
                        if (uri != null) {
                            try {
                                final Attachment attachment = mAttachmentsView.generateLocalAttachment(uri);
                                attachments.add(attachment);

                                Analytics.getInstance().sendEvent("send_intent_attachment",
                                        Utils.normalizeMimeType(attachment.getContentType()), null, attachment.size);

                            } catch (AttachmentFailureException e) {
                                LogUtils.e(TAG, e, "Error adding attachment uri [%s]", uri);
                                mContainFailedAttachment = true;
                            } catch (IllegalStateException e) {
                                /// M: Maybe this Exception happen when the file of the URI doesn't exsit
                                LogUtils.e(TAG, e, "Error adding attachment uri [%s]", uri);
                                mContainFailedAttachment = true;
                            }
                        }
                    }
                    return attachments;
                }

                @Override
                protected void onCancelled(List<Attachment> attachments) {
                    super.onCancelled(attachments);
                    releaseProgressDialog();
                }

                @Override
                protected void onSuccess(List<Attachment> attachments) {
                    super.onSuccess(attachments);
                    releaseProgressDialog();
                    if (mContainFailedAttachment) {
                        showErrorToast(getString(R.string.cannot_add_this_attachment));
                    }
                    if (null == attachments || attachments.size() == 0) {
                        return;
                    }
                    //Add attachments list to UI
                    long addedAttachments = addAttachments(attachments, true);
                    int totalSize = getAttachments().size();
                    if (totalSize > 0) {
                        ///M: if no attachments has been added , keep mAttachmentsChanged from modification
                        if (addedAttachments > 0) {
                            mAttachmentsChanged = true;
                        }
                        updateSaveUi();

                        Analytics.getInstance().sendEvent("send_intent_with_attachments",
                                Integer.toString(totalSize), null, totalSize);
                    }
                }

            }.executeParallel((Void [])null);
        }
    }

    /**
     * M: When have add attachment, show progress dialog.
     */
    private void showLoadAttachmentProgressDialog(final EmailAsyncTask task) {
        runOnUiThread(new Runnable() {
            public void run() {
                FragmentManager fm = getFragmentManager();
                mProgressDialog = LoadingAttachProgressDialog.newInstance(null);
                // Set Loading Task for loading dialog
                mProgressDialog.setLoadingTask(task);
                fm.beginTransaction()
                    .add(mProgressDialog, LoadingAttachProgressDialog.TAG)
                    .commit();
                fm.executePendingTransactions();
                }
            });
    }

    /**
     * M: When finished or canceled add attachment, release progress dialog.
     */
    private void releaseProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = (LoadingAttachProgressDialog)
                    getFragmentManager().findFragmentByTag(LoadingAttachProgressDialog.TAG);
        }
        if (mProgressDialog != null) {
            mProgressDialog.dismissAllowingStateLoss();
            // Reset Loading Task for loading dialog
            mProgressDialog.setLoadingTask(null);
            mProgressDialog = null;
        }
    }

    /**
     * M: Loading attachment Progress dialog
     */
    public static class LoadingAttachProgressDialog extends DialogFragment {
        @SuppressWarnings("hiding")
        public static final String TAG = "LoadingAttachProgressDialog";
        private EmailAsyncTask mLoadingTask = null;
        /**
         * Create a dialog for Loading attachment asynctask.
         */
        public static LoadingAttachProgressDialog newInstance(Fragment parentFragment) {
            LoadingAttachProgressDialog f = new LoadingAttachProgressDialog();
            f.setTargetFragment(parentFragment, 0);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();

            ProgressDialog dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setMessage(getString(R.string.loading_attachment));
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);

            return dialog;
        }

        /**
         * Listen for cancellation, which can happen from places other than the
         * negative button (e.g. touching outside the dialog), and stop the
         * checker
         */
        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            Logging.d(TAG, "LoadingAttachProgressDialog is onCancel "
                    + "and mLoadingTask will be canceled too");
            if (mLoadingTask != null) {
                mLoadingTask.cancel(true);
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mLoadingTask != null) {
                mLoadingTask = null;
            }
        }

        /**
         * M: Set Runnable task for Loading Progress dialog
         * @param task Asynctask of background loading
         */
        public void setLoadingTask(EmailAsyncTask task) {
            mLoadingTask = task;
        }
    }

    /**
     * M: check if the attachment is drm protected.
     */
    private boolean isDrmProtected(Uri uri) {
        /** M: MTK Dependence @{ */
        boolean checkResult = false;
        if (FeatureOption.MTK_DRM_APP) {
            OmaDrmClient drmClient = new OmaDrmClient(this);
            OmaDrmUtils.DrmProfile profile = OmaDrmUtils.getDrmProfile(this, uri, drmClient);
            // Only normal file and SD type drm file can be forwarded
            if (profile.isDrm()
                    && profile.getMethod() != OmaDrmStore.DrmMethod.METHOD_SD
                    && profile.getMethod() != OmaDrmStore.DrmMethod.METHOD_NONE) {
                LogUtils.w(TAG, "Not add attachment [%s], for Drm protected.", uri);
                checkResult = true;
            }
            drmClient.release();
            drmClient = null;
        }
        /** @} */
        return checkResult;
    }

    /**
     * M: Override and filter drm attachment.
     */
    @Override
    public long addAttachments(List<Attachment> attachments, boolean allowDup) {
        List<Attachment> filterAttachments = new ArrayList<Attachment>();
        boolean containDrmProtectedAtt = false;
        for (Attachment attachment : attachments) {
            Uri contentUri = attachment.contentUri;
            // contentUri be null? it is an exception no way to check drm or not.
            // follow default flow.
            if (contentUri == null) {
                filterAttachments.add(attachment);
                continue;
            }
            if (!isDrmProtected(contentUri)) {
                filterAttachments.add(attachment);
            } else {
                // DRM protected file, not add to attachment list.
                containDrmProtectedAtt = true;
            }
        }
        if (containDrmProtectedAtt) {
            showErrorToast(getString(com.mediatek.internal.R.string.drm_can_not_forward));
        }
        return super.addAttachments(filterAttachments, allowDup);
    }

    /**
     * M: Override and filter email address.
     */
    @Override
    protected void setupRecipients(MultiAutoCompleteTextView view) {
        super.setupRecipients(view);
        InputFilter[] recipientFilters = new InputFilter[] { ChipsAddressTextView.RECIPIENT_FILTER };
        view.setFilters(recipientFilters);
        // set filter delayer to avoid too many Filter thread be created.
        ((ChipsAddressTextView)view).setGalSearchDelayer();
    }

    /**
     * M: For reply, replyall and edit draft safely add CcAddresses to cc view at one time.
     * and the limit max number for add in one time is 250 to avoid too many to cause ANR.
     */
    @Override
    protected void addCcAddressesToList(List<Rfc822Token[]> addresses,
            List<Rfc822Token[]> compareToList, MultiAutoCompleteTextView list) {
        Set<String> tokenAddresses = Sets.newHashSet();

        if (compareToList == null) {
            for (Rfc822Token[] tokens : addresses) {
                for (int i = 0; i < tokens.length; i++) {
                    if (!tokenAddresses.contains(tokens[i].toString())) {
                        tokenAddresses.add(tokens[i].toString());
                    }
                }
            }
        } else {
            HashSet<String> compareTo = convertToHashSet(compareToList);
            for (Rfc822Token[] tokens : addresses) {
                for (int i = 0; i < tokens.length; i++) {
                    // Check if this is a duplicate:
                    if (!compareTo.contains(tokens[i].getAddress())
                            && !tokenAddresses.contains(tokens[i].toString())) {
                        tokenAddresses.add(tokens[i].toString());
                    }
                }
            }
        }
        safeAddAddressesToView(tokenAddresses, list);
    }

    /**
     * M: For reply, replyall and edit draft safely add ToAddresses to To view at one time.
     * and the limit max number for add in one time is 250 to avoid too many to cause ANR.
     */
    @Override
    protected void addAddressesToList(Collection<String> addresses, MultiAutoCompleteTextView list) {
        Set<String> tokenAddresses = Sets.newHashSet();
        for (String address : addresses) {
            if (address == null || list == null) {
                return;
            }

            final Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(address);
            for (int i = 0; i < tokens.length; i++) {
                if (!tokenAddresses.contains(tokens[i].toString())) {
                    tokenAddresses.add(tokens[i].toString());
                }
            }
        }
        safeAddAddressesToView(addresses, list);
    }

    /**
     * M: add numbers of address to RecipientView and avoid can not get the add text by appendList.
     */
    private void safeAddAddressesToView(Collection<String> addresses, MultiAutoCompleteTextView list) {
        final MTKRecipientList recipientList = new MTKRecipientList();
        for (String address : addresses) {
            if (recipientList.getRecipientCount() >= Configuration.RECIPIENT_MAX_NUMBER) {
                Utility.showToast(this,
                        getString(R.string.not_add_more_recipients, Configuration.RECIPIENT_MAX_NUMBER));
                LogUtils.d(TAG, "Not add more recipient, added address length is: %d", addresses.size());
                break;
            }
            recipientList.addRecipient("", address);
        }
        ((MTKRecipientEditTextView)list).appendList(recipientList);
    }

    /**
     * M: Init edit view's length filter
     */
    @Override
    protected void initLenghtFilter() {
        super.initLenghtFilter();
        UiUtilities.setupLengthFilter(getSubjectEditText(), this,
                Configuration.EDITVIEW_MAX_LENGTH_1, true);
        UiUtilities.setupLengthFilter(getBodyEditText(), this,
                Configuration.EDITVIEW_MAX_LENGTH_2, true);
    }
}
