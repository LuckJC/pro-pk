/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.email.activity.setup;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.provider.EmailProvider;
import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.utility.EmailAsyncTask;
import com.android.emailcommon.utility.Utility;
import com.android.mail.preferences.MailPrefs;
import com.android.mail.ui.settings.ClearPictureApprovalsDialogFragment;
import com.android.mail.utils.Utils;

import com.mediatek.email.attachment.AttachmentAutoClearController;
import com.mediatek.mail.vip.VipMember;
import com.mediatek.mail.vip.VipPreferences;
import com.mediatek.mail.vip.activity.VipListActivity;

public class GeneralPreferences extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREFERENCE_KEY_AUTO_ADVANCE = "auto_advance";
    private static final String PREFERENCE_KEY_TEXT_ZOOM = "text_zoom";
    private static final String PREFERENCE_KEY_CONFIRM_DELETE = "confirm_delete";
    private static final String PREFERENCE_KEY_CONFIRM_SEND = "confirm_send";
    private static final String PREFERENCE_KEY_CONV_LIST_ICON = "conversation_list_icon";
    /// M: Support for auto-clear internal attachments @{
    private static final String PERFERENCE_KEY_AUTO_CLEAR_ATT = "auto_clear_internal_attachments";
    /// @}

    private MailPrefs mMailPrefs;
    private Preferences mPreferences;
    private ListPreference mAutoAdvance;
    /**
     * TODO: remove this when we've decided for certain that an app setting is unnecessary
     * (b/5287963)
     */
    @Deprecated
    private ListPreference mTextZoom;
    private CheckBoxPreference mConfirmDelete;
    private CheckBoxPreference mConfirmSend;
    //private CheckBoxPreference mConvListAttachmentPreviews;
    private CheckBoxPreference mSwipeDelete;
    ///M: add bcc myself
    private CheckBoxPreference mAutoBcc;
    /// M: Support for auto-clear internal attachments @{
    private CheckBoxPreference mAutoClearAttachments;
    /// @}

    private boolean mSettingsChanged = false;

    CharSequence[] mSizeSummaries;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mMailPrefs = MailPrefs.get(getActivity());
        getPreferenceManager().setSharedPreferencesName(Preferences.PREFERENCES_FILE);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.general_preferences);

        final PreferenceScreen ps = getPreferenceScreen();
        // Merely hide app pref for font size until we're sure it's unnecessary (b/5287963)
        ps.removePreference(findPreference(PREFERENCE_KEY_TEXT_ZOOM));
    }

    @Override
    public void onResume() {
        loadSettings();
        mSettingsChanged = false;
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSettingsChanged) {
            // Notify the account list that we have changes
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.notifyChange(EmailProvider.UIPROVIDER_ALL_ACCOUNTS_NOTIFIER, null);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        // Indicate we need to send notifications to UI
        mSettingsChanged = true;
        if (PREFERENCE_KEY_AUTO_ADVANCE.equals(key)) {
            mPreferences.setAutoAdvanceDirection(mAutoAdvance.findIndexOfValue((String) newValue));
            return true;
        } else if (PREFERENCE_KEY_TEXT_ZOOM.equals(key)) {
            mPreferences.setTextZoom(mTextZoom.findIndexOfValue((String) newValue));
            reloadDynamicSummaries();
            return true;
        } else if (MailPrefs.PreferenceKeys.DEFAULT_REPLY_ALL.equals(key)) {
            mMailPrefs.setDefaultReplyAll((Boolean) newValue);
            return true;
        } else if (PREFERENCE_KEY_CONV_LIST_ICON.equals(key)) {
            mMailPrefs.setShowSenderImages((Boolean) newValue);
            return true;
        /** M: Auto clear @{*/
        } else if (PERFERENCE_KEY_AUTO_CLEAR_ATT.equals(key)) {
            if ((Boolean) newValue) {
                mPreferences.setAutoClearAtt(true);
                AttachmentAutoClearController.actionAutoClear(this.getActivity());
            } else {
                mPreferences.setAutoClearAtt(false);
                AttachmentAutoClearController.actionCancelAutoClear(this.getActivity());
            }
            return true;
        }
        /** M: Check is the VIP preference changed @{ */
        if (onVipPreferenceChange(preference, newValue)) {
            return true;
        }
        /** @} */
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (getActivity() == null) {
            // Guard against monkeys.
            return false;
        }
        mSettingsChanged = true;
        String key = preference.getKey();
        if (PREFERENCE_KEY_CONFIRM_DELETE.equals(key)) {
            mPreferences.setConfirmDelete(mConfirmDelete.isChecked());
            return true;
        } else if (PREFERENCE_KEY_CONFIRM_SEND.equals(key)) {
            mPreferences.setConfirmSend(mConfirmSend.isChecked());
            return true;
        } else if (MailPrefs.PreferenceKeys.CONVERSATION_LIST_SWIPE.equals(key)) {
            mMailPrefs.setConversationListSwipeEnabled(mSwipeDelete.isChecked());
            return true;
        } else if (MailPrefs.PreferenceKeys.BCC_MYSELF_KEY.equals(key)) {
            mMailPrefs.setAutoBccMyself(mAutoBcc.isChecked());
            return true;
        /**M: Support for VIP settings. Open VipListActivity @{*/
        } else if (PERFERENCE_KEY_VIP_MEMBERS.equals(key)) {
            final Intent vipActivityIntent = VipListActivity.createIntent(getActivity(),
                    Account.ACCOUNT_ID_COMBINED_VIEW);
            getActivity().startActivity(vipActivityIntent);
            return true;
        }
        /** @} */
        return false;
    }

    private void loadSettings() {
        mPreferences = Preferences.getPreferences(getActivity());
        mAutoAdvance = (ListPreference) findPreference(PREFERENCE_KEY_AUTO_ADVANCE);
        mAutoAdvance.setValueIndex(mPreferences.getAutoAdvanceDirection());
        mAutoAdvance.setOnPreferenceChangeListener(this);

        mTextZoom = (ListPreference) findPreference(PREFERENCE_KEY_TEXT_ZOOM);
        if (mTextZoom != null) {
            mTextZoom.setValueIndex(mPreferences.getTextZoom());
            mTextZoom.setOnPreferenceChangeListener(this);
        }

        final CheckBoxPreference convListIcon =
                (CheckBoxPreference) findPreference(PREFERENCE_KEY_CONV_LIST_ICON);
        if (convListIcon != null) {
            final boolean showSenderImage = mMailPrefs.getShowSenderImages();
            convListIcon.setChecked(showSenderImage);
            convListIcon.setOnPreferenceChangeListener(this);
        }

        mConfirmDelete = (CheckBoxPreference) findPreference(PREFERENCE_KEY_CONFIRM_DELETE);
        mConfirmSend = (CheckBoxPreference) findPreference(PREFERENCE_KEY_CONFIRM_SEND);
        mSwipeDelete = (CheckBoxPreference)
                findPreference(MailPrefs.PreferenceKeys.CONVERSATION_LIST_SWIPE);
        mSwipeDelete.setChecked(mMailPrefs.getIsConversationListSwipeEnabled());
        /**M: add auto bcc myself @{*/
        mAutoBcc = (CheckBoxPreference) findPreference(MailPrefs.PreferenceKeys.BCC_MYSELF_KEY);
        mAutoBcc.setChecked(mMailPrefs.getIsAutoBccMyselfEnabled());
        /**@}*/

        final CheckBoxPreference replyAllPreference =
                (CheckBoxPreference) findPreference(MailPrefs.PreferenceKeys.DEFAULT_REPLY_ALL);
        replyAllPreference.setChecked(mMailPrefs.getDefaultReplyAll());
        replyAllPreference.setOnPreferenceChangeListener(this);

        /** M: Support for auto-clear cache @{ */
        mAutoClearAttachments = (CheckBoxPreference) findPreference(PERFERENCE_KEY_AUTO_CLEAR_ATT);
        mAutoClearAttachments.setChecked(mPreferences.getAutoClearAtt());
        mAutoClearAttachments.setOnPreferenceChangeListener(this);
        /** @} */

        reloadDynamicSummaries();
        /// Load VIP settings
        loadVipData();
    }

    /**
     * Reload any preference summaries that are updated dynamically
     */
    private void reloadDynamicSummaries() {
        if (mTextZoom != null) {
            int textZoomIndex = mPreferences.getTextZoom();
            // Update summary - but only load the array once
            if (mSizeSummaries == null) {
                mSizeSummaries = getActivity().getResources()
                        .getTextArray(R.array.general_preference_text_zoom_summary_array);
            }
            CharSequence summary = null;
            if (textZoomIndex >= 0 && textZoomIndex < mSizeSummaries.length) {
                summary = mSizeSummaries[textZoomIndex];
            }
            mTextZoom.setSummary(summary);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.general_prefs_fragment_menu, menu);
    }

    /**
     * M: Don't show feedback menu if no feedback Uri is set
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem feedBackMenuItem = menu.findItem(R.id.feedback_menu_item);
        if (feedBackMenuItem != null) {
            Uri feedBackUri = Utils.getValidUri(getString(R.string.email_feedback_uri));
            // We only want to enable the feedback menu item, if there is a valid feedback uri
            feedBackMenuItem.setVisible(!Uri.EMPTY.equals(feedBackUri));
        }
    }

    //CHECKSTYLE:OFF
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_picture_approvals_menu_item:
                clearDisplayImages();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //CHECKSTYLE:ON 

    private void clearDisplayImages() {
        final ClearPictureApprovalsDialogFragment fragment =
                ClearPictureApprovalsDialogFragment.newInstance();
        fragment.show(getActivity().getFragmentManager(),
                ClearPictureApprovalsDialogFragment.FRAGMENT_TAG);
    }

    /**M: Support for VIP settings @{*/
    private static final String PERFERENCE_KEY_VIPSETTINGS = "vip_settings";
    private static final String PERFERENCE_KEY_VIP_MEMBERS = "vip_members";

    private VipMemberPreference mVipMembers;
    private VipMemberCountObserver mCountObserver;
    private int mMemberCount;
    private CheckBoxPreference mVipNotification;
    private RingtonePreference mVipRingTone;
    private CheckBoxPreference mVipVibrate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        PreferenceCategory vipCategory = (PreferenceCategory)findPreference(PERFERENCE_KEY_VIPSETTINGS);
        mVipMembers = new VipMemberPreference(getActivity());
        mVipMembers.setOrder(0);
        vipCategory.addPreference(mVipMembers);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStop() {
        unregisterVipCountObserver();
        super.onStop();
    }

    /**
     * M: Load the VIP Settings to UI
     */
    private void loadVipData() {
        VipPreferences vipPreferences = VipPreferences.get(getActivity());
        mVipNotification = (CheckBoxPreference) findPreference(VipPreferences.VIP_NOTIFICATION);
        mVipNotification.setChecked(vipPreferences.getVipNotification());
        mVipNotification.setOnPreferenceChangeListener(this);
        mVipRingTone = (RingtonePreference) findPreference(VipPreferences.VIP_RINGTONE);
        SharedPreferences prefs = mVipRingTone.getPreferenceManager().getSharedPreferences();
        String ringtone = vipPreferences.getVipRingtone();
        prefs.edit().putString(VipPreferences.VIP_RINGTONE, ringtone).apply();
        setRingtoneSummary(getActivity(), ringtone);
        mVipRingTone.setOnPreferenceChangeListener(this);
        mVipVibrate = (CheckBoxPreference) findPreference(VipPreferences.VIP_VIBRATE);
        mVipVibrate.setChecked(vipPreferences.getVipVebarate());
        mVipVibrate.setOnPreferenceChangeListener(this);
        registerVipCountObserver();
    }

    /// M: Deal the the VIP preference change event
    private boolean onVipPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (VipPreferences.VIP_NOTIFICATION.equals(key)) {
            VipPreferences.get(getActivity()).setVipNotification(
                    (Boolean) newValue);
            return true;
        } else if (VipPreferences.VIP_RINGTONE.equals(key)) {
            VipPreferences.get(getActivity()).setVipRingtone((String) newValue);
            setRingtoneSummary(getActivity(), (String) newValue);
            return true;
        } else if (VipPreferences.VIP_VIBRATE.equals(key)) {
            VipPreferences.get(getActivity())
                    .setVipVebarate((Boolean) newValue);
            return true;
        }
        return false;
    }

    /**
     * Sets the current ringtone summary.
     */
    private void setRingtoneSummary(Context context, String ringtoneUri) {
        Ringtone ringtone = null;
        if (!TextUtils.isEmpty(ringtoneUri)) {
            ringtone = RingtoneManager.getRingtone(getActivity(), Uri.parse(ringtoneUri));
        }
        final String summary = ringtone != null ? ringtone.getTitle(context)
                : context.getString(R.string.silent_ringtone);
        mVipRingTone.setSummary(summary);
    }

    /**
     * M: Register the VIP member count observer if it was not registered
     */
    private void registerVipCountObserver() {
        Context context = getActivity();
        if (context != null && mCountObserver == null) {
            mCountObserver = new VipMemberCountObserver(Utility.getMainThreadHandler());
            context.getContentResolver().registerContentObserver(VipMember.NOTIFIER_URI, true,
                    mCountObserver);
            updateVipMemberCount();
        }
    }

    /**
     * M: Unregister the VIP member count observer if it was not unregistered
     */
    private void unregisterVipCountObserver() {
        Context context = getActivity();
        if (context != null && mCountObserver != null) {
            context.getContentResolver().unregisterContentObserver(mCountObserver);
            mCountObserver = null;
        }
    }

    private void updateVipMemberCount() {
        new EmailAsyncTask<Void, Void, Integer>(null) {
            private static final int ERROR_RESULT = -1;
            @Override
            protected Integer doInBackground(Void... params) {
                Context context = getActivity();
                if (context == null) {
                    return ERROR_RESULT;
                }
                return VipMember.countVipMembersWithAccountId(context,
                        Account.ACCOUNT_ID_COMBINED_VIEW);
            }

            @Override
            protected void onSuccess(Integer result) {
                if (result != ERROR_RESULT) {
                    mMemberCount = result;
                    mVipMembers.setCount(result);
                } else {
                    Logging.e("Failed to get the count of the VIP member");
                }
            }
        }.executeParallel();
    }

    private class VipMemberCountObserver extends ContentObserver {

        public VipMemberCountObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateVipMemberCount();
        }
    }

    private class VipMemberPreference extends Preference {
        private TextView mCountView;

        public VipMemberPreference(Context context) {
            super(context);
            setKey(PERFERENCE_KEY_VIP_MEMBERS);
            setTitle(R.string.vip_members);
            setWidgetLayoutResource(R.layout.vip_preference_widget_count);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            // Get the widget view of the member preference
            ViewGroup widgetFrame = (ViewGroup)view.findViewById(com.android.internal.R.id.widget_frame);
            mCountView = (TextView)widgetFrame.findViewById(R.id.vip_count);
            setCount(mMemberCount);
        }

        // Set the count of the VIP member
        public void setCount(int count) {
            if (mCountView != null) {
                mCountView.setText(getContext().getResources().getString(
                        R.string.vip_settings_member_count, count));
            }
        }
    }
    /** @{ */

}
