/**
 * Copyright (c) 2012, Google Inc.
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

package com.android.mail.providers;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;

import com.android.emailcommon.internet.MimeMessage;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.mail.Part;
import com.android.emailcommon.utility.ConversionUtilities;
import com.android.emailcommon.utility.StringCompressor;
import com.android.mail.providers.UIProvider.MessageColumns;
import com.android.mail.ui.HtmlMessage;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;


public class Message implements Parcelable, HtmlMessage {
    /**
     * Regex pattern used to look for any inline images in message bodies, including Gmail-hosted
     * relative-URL images, Gmail emoticons, and any external inline images (although we usually
     * count on the server to detect external images).
     */
    private static Pattern INLINE_IMAGE_PATTERN = Pattern.compile("<img\\s+[^>]*src=",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /// M: Add log Tag for Message
    private static final String TAG = LogTag.getLogTag();

    /**
     * @see BaseColumns#_ID
     */
    public long id;
    /**
     * @see UIProvider.MessageColumns#SERVER_ID
     */
    public String serverId;
    /**
     * @see UIProvider.MessageColumns#URI
     */
    public Uri uri;
    /**
     * @see UIProvider.MessageColumns#CONVERSATION_ID
     */
    public Uri conversationUri;
    /**
     * @see UIProvider.MessageColumns#SUBJECT
     */
    public String subject;
    /**
     * @see UIProvider.MessageColumns#SNIPPET
     */
    public String snippet;
    /**
     * @see UIProvider.MessageColumns#FROM
     */
    private String mFrom;
    /**
     * @see UIProvider.MessageColumns#TO
     */
    private String mTo;
    /**
     * @see UIProvider.MessageColumns#CC
     */
    private String mCc;
    /**
     * @see UIProvider.MessageColumns#BCC
     */
    private String mBcc;
    /**
     * @see UIProvider.MessageColumns#REPLY_TO
     */
    private String mReplyTo;
    /**
     * @see UIProvider.MessageColumns#DATE_RECEIVED_MS
     */
    public long dateReceivedMs;
    /**
     * @see UIProvider.MessageColumns#BODY_HTML
     */
    public String bodyHtml;
    /**
     * @see UIProvider.MessageColumns#BODY_TEXT
     */
    public String bodyText;
    /**
     * @see UIProvider.MessageColumns#EMBEDS_EXTERNAL_RESOURCES
     */
    public boolean embedsExternalResources;
    /**
     * @see UIProvider.MessageColumns#REF_MESSAGE_ID
     */
    public Uri refMessageUri;
    /**
     * @see UIProvider.MessageColumns#DRAFT_TYPE
     */
    public int draftType;
    /**
     * @see UIProvider.MessageColumns#APPEND_REF_MESSAGE_CONTENT
     */
    public boolean appendRefMessageContent;
    /**
     * @see UIProvider.MessageColumns#HAS_ATTACHMENTS
     */
    public boolean hasAttachments;
    /**
     * @see UIProvider.MessageColumns#ATTACHMENT_LIST_URI
     */
    public Uri attachmentListUri;
    /**
     * @see UIProvider.MessageColumns#MESSAGE_FLAGS
     */
    public long messageFlags;
    /**
     * @see UIProvider.MessageColumns#ALWAYS_SHOW_IMAGES
     */
    public boolean alwaysShowImages;
    /**
     * @see UIProvider.MessageColumns#READ
     */
    public boolean read;
    /**
     * @see UIProvider.MessageColumns#SEEN
     */
    public boolean seen;
    /**
     * @see UIProvider.MessageColumns#STARRED
     */
    public boolean starred;
    /**
     * @see UIProvider.MessageColumns#QUOTE_START_POS
     */
    public int quotedTextOffset;
    /**
     * @see UIProvider.MessageColumns#ATTACHMENTS
     *<p>
     * N.B. this value is NOT immutable and may change during conversation view render.
     */
    public String attachmentsJson;
    /**
     * @see UIProvider.MessageColumns#MESSAGE_ACCOUNT_URI
     */
    public Uri accountUri;
    /**
     * @see UIProvider.MessageColumns#EVENT_INTENT_URI
     */
    public Uri eventIntentUri;
    /**
     * @see UIProvider.MessageColumns#SPAM_WARNING_STRING
     */
    public String spamWarningString;
    /**
     * @see UIProvider.MessageColumns#SPAM_WARNING_LEVEL
     */
    public int spamWarningLevel;
    /**
     * @see UIProvider.MessageColumns#SPAM_WARNING_LINK_TYPE
     */
    public int spamLinkType;
    /**
     * @see UIProvider.MessageColumns#VIA_DOMAIN
     */
    public String viaDomain;
    /**
     * @see UIProvider.MessageColumns#IS_SENDING
     */
    public boolean isSending;
    /**
     * @see UIProvider.MessageColumns#FLAG_LOADED
     */
    public int flagLoaded;
    /**
     * @see UIProvider.MessageColumns#STATE
     */
    public int state;

    /**
     * M: @see UIProvider.MessageColumns#FLAG_EXTEND
     */
    public long extendFlags;

    private transient String[] mFromAddresses = null;
    private transient String[] mToAddresses = null;
    private transient String[] mCcAddresses = null;
    private transient String[] mBccAddresses = null;
    private transient String[] mReplyToAddresses = null;

    private transient List<Attachment> mAttachments = null;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o != null && o instanceof Message
                && Objects.equal(uri, ((Message) o).uri));
    }

    @Override
    public int hashCode() {
        return uri == null ? 0 : uri.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(serverId);
        dest.writeParcelable(uri, 0);
        dest.writeParcelable(conversationUri, 0);
        dest.writeString(subject);
        dest.writeString(snippet);
        dest.writeString(mFrom);
        dest.writeString(mTo);
        dest.writeString(mCc);
        dest.writeString(mBcc);
        dest.writeString(mReplyTo);
        dest.writeLong(dateReceivedMs);
        /** M: Deal with compressed body @{ */
        dest.writeLong(extendFlags);
        if (isFlaggedBodyCompressed()) {
            LogUtils.d(TAG, "Message.writeToParcel compress body of message: %d", id);
            byte[] bytes = StringCompressor.compressToBytes(bodyHtml);
            dest.writeByteArray(bytes);
            bytes = StringCompressor.compressToBytes(bodyText);
            dest.writeByteArray(bytes);
        } else {
            dest.writeString(bodyHtml);
            dest.writeString(bodyText);
        }
        /** @} */
        dest.writeInt(embedsExternalResources ? 1 : 0);
        dest.writeParcelable(refMessageUri, 0);
        dest.writeInt(draftType);
        dest.writeInt(appendRefMessageContent ? 1 : 0);
        dest.writeInt(hasAttachments ? 1 : 0);
        dest.writeParcelable(attachmentListUri, 0);
        dest.writeLong(messageFlags);
        dest.writeInt(alwaysShowImages ? 1 : 0);
        dest.writeInt(quotedTextOffset);
        dest.writeString(attachmentsJson);
        dest.writeParcelable(accountUri, 0);
        dest.writeParcelable(eventIntentUri, 0);
        dest.writeString(spamWarningString);
        dest.writeInt(spamWarningLevel);
        dest.writeInt(spamLinkType);
        dest.writeString(viaDomain);
        dest.writeInt(isSending ? 1 : 0);
        dest.writeInt(flagLoaded);
        dest.writeInt(state);
    }

    private Message(Parcel in) {
        id = in.readLong();
        serverId = in.readString();
        uri = in.readParcelable(null);
        conversationUri = in.readParcelable(null);
        subject = in.readString();
        snippet = in.readString();
        mFrom = in.readString();
        mTo = in.readString();
        mCc = in.readString();
        mBcc = in.readString();
        mReplyTo = in.readString();
        dateReceivedMs = in.readLong();
        /** M: Deal with compressed body @{ */
        extendFlags = in.readLong();
        if (isFlaggedBodyCompressed()) {
            LogUtils.d(TAG, "Message(Parcel in) decompress body of message: %d", id);
            bodyHtml = StringCompressor.decompressFromBytes(in.createByteArray());
            bodyText = StringCompressor.decompressFromBytes(in.createByteArray());
        } else {
            bodyHtml = in.readString();
            bodyText = in.readString();
        }
        /** @} */
        embedsExternalResources = in.readInt() != 0;
        refMessageUri = in.readParcelable(null);
        draftType = in.readInt();
        appendRefMessageContent = in.readInt() != 0;
        hasAttachments = in.readInt() != 0;
        attachmentListUri = in.readParcelable(null);
        messageFlags = in.readLong();
        alwaysShowImages = in.readInt() != 0;
        quotedTextOffset = in.readInt();
        attachmentsJson = in.readString();
        accountUri = in.readParcelable(null);
        eventIntentUri = in.readParcelable(null);
        spamWarningString = in.readString();
        spamWarningLevel = in.readInt();
        spamLinkType = in.readInt();
        viaDomain = in.readString();
        isSending = in.readInt() != 0;
        flagLoaded = in.readInt();
        state = in.readInt();
    }

    public Message() {

    }

    @Override
    public String toString() {
        return "[message id=" + id + "] + [ flagLoaded=" + flagLoaded + "]";
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {

        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }

    };

    public Message(Cursor cursor) {
        if (cursor != null) {
            id = cursor.getLong(UIProvider.MESSAGE_ID_COLUMN);
            serverId = cursor.getString(UIProvider.MESSAGE_SERVER_ID_COLUMN);
            final String messageUriStr = cursor.getString(UIProvider.MESSAGE_URI_COLUMN);
            uri = !TextUtils.isEmpty(messageUriStr) ? Uri.parse(messageUriStr) : null;
            final String convUriStr = cursor.getString(UIProvider.MESSAGE_CONVERSATION_URI_COLUMN);
            conversationUri = !TextUtils.isEmpty(convUriStr) ? Uri.parse(convUriStr) : null;
            subject = cursor.getString(UIProvider.MESSAGE_SUBJECT_COLUMN);
            snippet = cursor.getString(UIProvider.MESSAGE_SNIPPET_COLUMN);
            mFrom = cursor.getString(UIProvider.MESSAGE_FROM_COLUMN);
            mTo = cursor.getString(UIProvider.MESSAGE_TO_COLUMN);
            mCc = cursor.getString(UIProvider.MESSAGE_CC_COLUMN);
            mBcc = cursor.getString(UIProvider.MESSAGE_BCC_COLUMN);
            mReplyTo = cursor.getString(UIProvider.MESSAGE_REPLY_TO_COLUMN);
            dateReceivedMs = cursor.getLong(UIProvider.MESSAGE_DATE_RECEIVED_MS_COLUMN);
            /** M: Deal with compressed body @{ */
            extendFlags = cursor.getLong(UIProvider.MESSAGE_FLAG_EXTEND_COLUMN);
            bodyHtml = cursor.getString(UIProvider.MESSAGE_BODY_HTML_COLUMN);
            bodyText = cursor.getString(UIProvider.MESSAGE_BODY_TEXT_COLUMN);
            if (isFlaggedBodyCompressed()) {
                LogUtils.d(TAG, "Message(Cursor cursor) decompress body of message: %d", id);
                bodyHtml = StringCompressor.decompressFromString(bodyHtml);
                bodyText = StringCompressor.decompressFromString(bodyText);
            }
            /** @} */
            embedsExternalResources = cursor
                    .getInt(UIProvider.MESSAGE_EMBEDS_EXTERNAL_RESOURCES_COLUMN) != 0;
            final String refMessageUriStr =
                    cursor.getString(UIProvider.MESSAGE_REF_MESSAGE_URI_COLUMN);
            refMessageUri = !TextUtils.isEmpty(refMessageUriStr) ?
                    Uri.parse(refMessageUriStr) : null;
            draftType = cursor.getInt(UIProvider.MESSAGE_DRAFT_TYPE_COLUMN);
            appendRefMessageContent = cursor
                    .getInt(UIProvider.MESSAGE_APPEND_REF_MESSAGE_CONTENT_COLUMN) != 0;
            hasAttachments = cursor.getInt(UIProvider.MESSAGE_HAS_ATTACHMENTS_COLUMN) != 0;
            final String attachmentsUri = cursor
                    .getString(UIProvider.MESSAGE_ATTACHMENT_LIST_URI_COLUMN);
            attachmentListUri = hasAttachments && !TextUtils.isEmpty(attachmentsUri) ? Uri
                    .parse(attachmentsUri) : null;
            messageFlags = cursor.getLong(UIProvider.MESSAGE_FLAGS_COLUMN);
            alwaysShowImages = cursor.getInt(UIProvider.MESSAGE_ALWAYS_SHOW_IMAGES_COLUMN) != 0;
            read = cursor.getInt(UIProvider.MESSAGE_READ_COLUMN) != 0;
            seen = cursor.getInt(UIProvider.MESSAGE_SEEN_COLUMN) != 0;
            starred = cursor.getInt(UIProvider.MESSAGE_STARRED_COLUMN) != 0;
            quotedTextOffset = cursor.getInt(UIProvider.QUOTED_TEXT_OFFSET_COLUMN);
            attachmentsJson = cursor.getString(UIProvider.MESSAGE_ATTACHMENTS_COLUMN);
            String accountUriString = cursor.getString(UIProvider.MESSAGE_ACCOUNT_URI_COLUMN);
            accountUri = !TextUtils.isEmpty(accountUriString) ? Uri.parse(accountUriString) : null;
            eventIntentUri =
                    Utils.getValidUri(cursor.getString(UIProvider.MESSAGE_EVENT_INTENT_COLUMN));
            spamWarningString =
                    cursor.getString(UIProvider.MESSAGE_SPAM_WARNING_STRING_ID_COLUMN);
            spamWarningLevel = cursor.getInt(UIProvider.MESSAGE_SPAM_WARNING_LEVEL_COLUMN);
            spamLinkType = cursor.getInt(UIProvider.MESSAGE_SPAM_WARNING_LINK_TYPE_COLUMN);
            viaDomain = cursor.getString(UIProvider.MESSAGE_VIA_DOMAIN_COLUMN);
            isSending = cursor.getInt(UIProvider.MESSAGE_IS_SENDING_COLUMN) != 0;
            flagLoaded = cursor.getInt(UIProvider.MESSAGE_FLAG_LOADED_COLUMN);
            state = cursor.getInt(UIProvider.MESSAGE_STATE_COLUMN);
        }
    }

    public Message(Context context, MimeMessage mimeMessage, Uri emlFileUri)
            throws MessagingException {
        // Set message header values.
        setFrom(com.android.emailcommon.mail.Address.pack(mimeMessage.getFrom()));
        setTo(com.android.emailcommon.mail.Address.pack(mimeMessage.getRecipients(
                com.android.emailcommon.mail.Message.RecipientType.TO)));
        setCc(com.android.emailcommon.mail.Address.pack(mimeMessage.getRecipients(
                com.android.emailcommon.mail.Message.RecipientType.CC)));
        setBcc(com.android.emailcommon.mail.Address.pack(mimeMessage.getRecipients(
                com.android.emailcommon.mail.Message.RecipientType.BCC)));
        setReplyTo(com.android.emailcommon.mail.Address.pack(mimeMessage.getReplyTo()));
        subject = mimeMessage.getSubject();
        /// M: process the case that sent date null. @{
        Date date = mimeMessage.getSentDate();
        if (date == null) {
            date = mimeMessage.getReceivedDate();
        }
        if (date != null) {
            dateReceivedMs = date.getTime();
        }
        /// @}

        // for now, always set defaults
        alwaysShowImages = false;
        viaDomain = null;
        draftType = UIProvider.DraftType.NOT_A_DRAFT;
        isSending = false;
        starred = false;
        spamWarningString = null;
        messageFlags = 0;
        hasAttachments = false;
        /// M: Add for partial download
        // TODO: Need to check the init value
        flagLoaded = UIProvider.MessageFlagLoaded.FLAG_LOADED_COMPLETE;
        /// M: Set initial value of partial download state
        state = UIProvider.MessageState.NONE;

        // body values (snippet/bodyText/bodyHtml)
        // Now process body parts & attachments
        ArrayList<Part> viewables = new ArrayList<Part>();
        ArrayList<Part> attachments = new ArrayList<Part>();
        MimeUtility.collectParts(mimeMessage, viewables, attachments);

        ConversionUtilities.BodyFieldData data =
                ConversionUtilities.parseBodyFields(viewables);

        snippet = data.snippet;
        bodyText = data.textContent;
        bodyHtml = data.htmlContent;

        // populate mAttachments
        mAttachments = Lists.newArrayList();

        int partId = 0;
        final String messageId = mimeMessage.getMessageId();
        for (final Part attachmentPart : attachments) {
            mAttachments.add(new Attachment(context, attachmentPart,
                    emlFileUri, messageId, Integer.toString(partId++)));
        }

        hasAttachments = !mAttachments.isEmpty();

        attachmentListUri =  hasAttachments ?
                EmlAttachmentProvider.getAttachmentsListUri(emlFileUri, messageId) : null;
    }

    public boolean isFlaggedReplied() {
        return (messageFlags & UIProvider.MessageFlags.REPLIED) ==
                UIProvider.MessageFlags.REPLIED;
    }

    public boolean isFlaggedForwarded() {
        return (messageFlags & UIProvider.MessageFlags.FORWARDED) ==
                UIProvider.MessageFlags.FORWARDED;
    }

    public boolean isFlaggedCalendarInvite() {
        return (messageFlags & UIProvider.MessageFlags.CALENDAR_INVITE) ==
                UIProvider.MessageFlags.CALENDAR_INVITE;
    }

    /**
     * M: Is the message body too large
     */
    public boolean isFlaggedBodyTooLarge() {
        return (extendFlags & UIProvider.MessageExtendFlags.BODY_TOO_LARGE) ==
                UIProvider.MessageExtendFlags.BODY_TOO_LARGE;
    }

    /**
     * M: Is the message body compressed
     */
    public boolean isFlaggedBodyCompressed() {
        return (extendFlags & UIProvider.MessageExtendFlags.BODY_COMPRESSED) ==
                UIProvider.MessageExtendFlags.BODY_COMPRESSED;
    }

    public String getFrom() {
        return mFrom;
    }

    public synchronized void setFrom(final String from) {
        mFrom = from;
        mFromAddresses = null;
    }

    public String getTo() {
        return mTo;
    }

    public synchronized void setTo(final String to) {
        mTo = to;
        mToAddresses = null;
    }

    public String getCc() {
        return mCc;
    }

    public synchronized void setCc(final String cc) {
        mCc = cc;
        mCcAddresses = null;
    }

    public String getBcc() {
        return mBcc;
    }

    public synchronized void setBcc(final String bcc) {
        mBcc = bcc;
        mBccAddresses = null;
    }

    public String getReplyTo() {
        return mReplyTo;
    }

    public synchronized void setReplyTo(final String replyTo) {
        mReplyTo = replyTo;
        mReplyToAddresses = null;
    }

    public synchronized String[] getFromAddresses() {
        if (mFromAddresses == null) {
            mFromAddresses = tokenizeAddresses(mFrom);
        }
        return mFromAddresses;
    }

    public String[] getFromAddressesUnescaped() {
        return unescapeAddresses(getFromAddresses());
    }

    public synchronized String[] getToAddresses() {
        if (mToAddresses == null) {
            mToAddresses = tokenizeAddresses(mTo);
        }
        return mToAddresses;
    }

    public String[] getToAddressesUnescaped() {
        return unescapeAddresses(getToAddresses());
    }

    public synchronized String[] getCcAddresses() {
        if (mCcAddresses == null) {
            mCcAddresses = tokenizeAddresses(mCc);
        }
        return mCcAddresses;
    }

    public String[] getCcAddressesUnescaped() {
        return unescapeAddresses(getCcAddresses());
    }

    public synchronized String[] getBccAddresses() {
        if (mBccAddresses == null) {
            mBccAddresses = tokenizeAddresses(mBcc);
        }
        return mBccAddresses;
    }

    public String[] getBccAddressesUnescaped() {
        return unescapeAddresses(getBccAddresses());
    }

    public synchronized String[] getReplyToAddresses() {
        if (mReplyToAddresses == null) {
            mReplyToAddresses = tokenizeAddresses(mReplyTo);
        }
        return mReplyToAddresses;
    }

    public String[] getReplyToAddressesUnescaped() {
        return unescapeAddresses(getReplyToAddresses());
    }

    private static String[] unescapeAddresses(String[] escaped) {
        final String[] unescaped = new String[escaped.length];
        for (int i = 0; i < escaped.length; i++) {
            final String escapeMore = escaped[i].replace("<", "&lt;").replace(">", "&gt;");
            unescaped[i] = Html.fromHtml(escapeMore).toString();
        }
        return unescaped;
    }

    public static String[] tokenizeAddresses(String addresses) {
        if (TextUtils.isEmpty(addresses)) {
            return new String[0];
        }

        LogUtils.d(TAG, "Message.tokenizeAddresses address: %s", addresses);
        /** M: Email addresses are stored as decoded packed strings like
         * Joe@xxx.com\2Joe Smith (John), Rfc822Tokennizer need strings as RFC822 address
         * format like: Joe Smith (John) <Joe@xxx.com>, "Kelly Bob" <kb@xxx.com>.
         * Meanwhile, it's better to unpackToString addresses from packed format to RFC822
         * before feed to Rfc822Tokenizer
         */
        Address[] packAddresses = Address.unpack(addresses);
        if (packAddresses.length > 0) {
            String[] result = new String[packAddresses.length];
            int i = 0;
            for (Address address : packAddresses) {
                result[i] = address.toString();
                i++;
            }
            return result;
        } else {
            return new String[0];
        }
    }

    public List<Attachment> getAttachments() {
        if (mAttachments == null) {
            if (attachmentsJson != null) {
                mAttachments = Attachment.fromJSONArray(attachmentsJson);
            } else {
                mAttachments = Collections.emptyList();
            }
        }
        return mAttachments;
    }

    /**
     * Returns whether a "Show Pictures" button should initially appear for this message. If the
     * button is shown, the message must also block all non-local images in the body. Inversely, if
     * the button is not shown, the message must show all images within (or else the user would be
     * stuck with no images and no way to reveal them).
     *
     * @return true if a "Show Pictures" button should appear.
     */
    public boolean shouldShowImagePrompt() {
        return !alwaysShowImages && (embedsExternalResources ||
                (!TextUtils.isEmpty(bodyHtml) && INLINE_IMAGE_PATTERN.matcher(bodyHtml).find()));
    }

    @Override
    public boolean embedsExternalResources() {
        return embedsExternalResources;
    }

    /**
     * Helper method to command a provider to mark all messages from this sender with the
     * {@link MessageColumns#ALWAYS_SHOW_IMAGES} flag set.
     *
     * @param handler a caller-provided handler to run the query on
     * @param token (optional) token to identify the command to the handler
     * @param cookie (optional) cookie to pass to the handler
     */
    public void markAlwaysShowImages(AsyncQueryHandler handler, int token, Object cookie) {
        alwaysShowImages = true;

        final ContentValues values = new ContentValues(1);
        values.put(UIProvider.MessageColumns.ALWAYS_SHOW_IMAGES, 1);

        /** M: Check is the uri null. @{ */
        if (uri == null) {
            LogUtils.w(TAG, "Message markAlwaysShowImages but uri is null !!!");
            return;
        }
        /** @} */
        handler.startUpdate(token, cookie, uri, values, null, null);
    }

    @Override
    public String getBodyAsHtml() {
        String body = "";
        if (!TextUtils.isEmpty(bodyHtml)) {
            body = bodyHtml;
        } else if (!TextUtils.isEmpty(bodyText)) {
            final SpannableString spannable = new SpannableString(bodyText);
            Linkify.addLinks(spannable, Linkify.EMAIL_ADDRESSES);
            body = Html.toHtml(spannable);
        }
        return body;
    }

    @Override
    public long getId() {
        return id;
    }
}
