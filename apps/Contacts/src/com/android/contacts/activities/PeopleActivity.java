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

package com.android.contacts.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.ProviderStatus;
import android.provider.ContactsContract.QuickContact;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.activities.ActionBarAdapter.TabState;
import com.android.contacts.detail.ContactDetailFragment;
import com.android.contacts.detail.ContactDetailLayoutController;
import com.android.contacts.detail.ContactDetailUpdatesFragment;
import com.android.contacts.detail.ContactLoaderFragment;
import com.android.contacts.detail.ContactLoaderFragment.ContactLoaderFragmentListener;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.dialog.ClearFrequentsDialog;
import com.android.contacts.group.GroupBrowseListFragment;
import com.android.contacts.group.GroupBrowseListFragment.OnGroupBrowserActionListener;
import com.android.contacts.group.GroupDetailFragment;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.common.interactions.ImportExportDialogFragment;
import com.android.contacts.list.ContactBrowseListFragment;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactTileAdapter.DisplayType;
import com.android.contacts.list.ContactTileFrequentFragment;
import com.android.contacts.list.ContactTileListFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ContactsUnavailableFragment;
import com.android.contacts.list.DefaultContactBrowseListFragment;
import com.android.contacts.common.list.DirectoryListLoader;
import com.android.contacts.list.OnContactBrowserActionListener;
import com.android.contacts.list.OnContactsUnavailableActionListener;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.list.ProviderStatusWatcher.ProviderStatusListener;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.preference.ContactsPreferenceActivity;
import com.android.contacts.preference.DisplayOptionsPreferenceFragment;
import com.android.contacts.common.util.AccountFilterUtil;
import com.android.contacts.util.AccountPromptUtils;
import com.android.contacts.common.util.Constants;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.HelpUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.common.util.UriUtils;
import com.android.contacts.widget.TransitionAnimationView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
/** M: New Feature xxx @{ */
import com.android.contacts.common.vcard.VCardService;
import com.mediatek.xlog.Xlog;
import com.mediatek.contacts.ExtensionManager;
import com.android.contacts.common.model.AccountTypeManager;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.util.PDebug;
import com.mediatek.contacts.util.SetIndicatorUtils;
import com.mediatek.contacts.activities.ContactImportExportActivity;
import com.mediatek.contacts.ext.ContactPluginDefault;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;
import com.mediatek.contacts.ext.ContactAccountExtension.OnGuideFinishListener;

import android.os.Parcel;
import java.util.List;

import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.StatusBarManager;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.android.internal.telephony.ITelephony;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.app.AlertDialog;
import android.os.AsyncTask;


/////******for vcs
import android.R.bool;
import android.app.Dialog;
import android.R.integer;
import android.app.LoaderManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.content.ContentResolver;
import android.provider.ContactsContract.RawContacts;
import android.content.Loader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Message;
import com.mediatek.vcs.VoiceSearchDialogFragment;
import com.mediatek.vcs.VoiceSearchIndicator;
import com.mediatek.vcs.VoiceSearchResultLoader;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.view.MotionEvent;
import android.view.Gravity;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import android.content.res.Configuration;
import com.mediatek.contacts.util.TimingStatistics;
import com.android.internal.view.menu.*;
/// M: VCS
import com.mediatek.vcs.VoiceSearchManager;
import com.mediatek.vcs.VoiceSearchResultLoader.Listener;
import com.mediatek.contacts.util.VCSUtils;
import com.android.contacts.list.DefaultContactBrowseListFragment.onTouchedScreenListener;
//vcs

import javax.security.auth.PrivateCredentialPermission;
//////*****

/*
 * New Feature by Mediatek End.
 */
/**
 * Displays a list to browse contacts. For xlarge screens, this also displays a detail-pane on
 * the right.
 */
public class PeopleActivity extends ContactsActivity
        implements View.OnCreateContextMenuListener, ActionBarAdapter.Listener,
        DialogManager.DialogShowingViewActivity,VoiceSearchManager.Listener,
        ContactListFilterController.ContactListFilterListener, ProviderStatusListener 
        ,onTouchedScreenListener, VoiceSearchResultLoader.Listener,
        VoiceSearchDialogFragment.ContactsRowOnClickListener {

    private static final String TAG = "PeopleActivity";

    private static final int TAB_FADE_IN_DURATION = 500;

    private static final String ENABLE_DEBUG_OPTIONS_HIDDEN_CODE = "debug debug!";

    // These values needs to start at 2. See {@link ContactEntryListFragment}.
    private static final int SUBACTIVITY_NEW_CONTACT = 2;
    private static final int SUBACTIVITY_EDIT_CONTACT = 3;
    private static final int SUBACTIVITY_NEW_GROUP = 4;
    private static final int SUBACTIVITY_EDIT_GROUP = 5;
    private static final int SUBACTIVITY_ACCOUNT_FILTER = 6;

    private final DialogManager mDialogManager = new DialogManager(this);

    private ContactsIntentResolver mIntentResolver;
    private ContactsRequest mRequest;

    private ActionBarAdapter mActionBarAdapter;

    private ContactDetailFragment mContactDetailFragment;

    private ContactLoaderFragment mContactDetailLoaderFragment;
    private final ContactDetailLoaderFragmentListener mContactDetailLoaderFragmentListener =
            new ContactDetailLoaderFragmentListener();

    private GroupDetailFragment mGroupDetailFragment;
    private final GroupDetailFragmentListener mGroupDetailFragmentListener =
            new GroupDetailFragmentListener();

    private ContactTileListFragment.Listener mFavoritesFragmentListener =
            new StrequentContactListFragmentListener();

    private ContactListFilterController mContactListFilterController;

    private ContactsUnavailableFragment mContactsUnavailableFragment;
    private ProviderStatusWatcher mProviderStatusWatcher;
    private ProviderStatusWatcher.Status mProviderStatus;

    private boolean mOptionsMenuContactsAvailable;

    /**
     * Showing a list of Contacts. Also used for showing search results in search mode.
     */
    private DefaultContactBrowseListFragment mAllFragment;
    private ContactTileListFragment mFavoritesFragment;
    private ContactTileFrequentFragment mFrequentFragment;
    private GroupBrowseListFragment mGroupsFragment;

    private View mFavoritesView;
    private View mBrowserView;
    private TransitionAnimationView mPeopleActivityView;
    private TransitionAnimationView mContactDetailsView;
    private TransitionAnimationView mGroupDetailsView;

    /** ViewPager for swipe, used only on the phone (i.e. one-pane mode) */
    private ViewPager mTabPager;
    private TabPagerAdapter mTabPagerAdapter;
    private final TabPagerListener mTabPagerListener = new TabPagerListener();

    private ContactDetailLayoutController mContactDetailLayoutController;

    private boolean mEnableDebugMenuOptions;

    private final Handler mHandler = new Handler();

    /**
     * True if this activity instance is a re-created one.  i.e. set true after orientation change.
     * This is set in {@link #onCreate} for later use in {@link #onStart}.
     */
    private boolean mIsRecreatedInstance;

    /**
     * If {@link #configureFragments(boolean)} is already called.  Used to avoid calling it twice
     * in {@link #onStart}.
     * (This initialization only needs to be done once in onStart() when the Activity was just
     * created from scratch -- i.e. onCreate() was just called)
     */
    private boolean mFragmentInitialized;

    /**
     * Whether or not the current contact filter is valid or not. We need to do a check on
     * start of the app to verify that the user is not in single contact mode. If so, we should
     * dynamically change the filter, unless the incoming intent specifically requested a contact
     * that should be displayed in that mode.
     */
    private boolean mCurrentFilterIsValid;

    /**
     * This is to disable {@link #onOptionsItemSelected} when we trying to stop the activity.
     */
    private boolean mDisableOptionItemSelected;

    /** Sequential ID assigned to each instance; used for logging */
    private final int mInstanceId;
    private static final AtomicInteger sNextInstanceId = new AtomicInteger();

    public PeopleActivity() {
        mInstanceId = sNextInstanceId.getAndIncrement();
        mIntentResolver = new ContactsIntentResolver(this);
        /** M: Bug Fix for ALPS00407311 @{ */
        /*
         * original Code:mProviderStatusWatcher = ProviderStatusWatcher.getInstance(this);
         */
        mProviderStatusWatcher = ProviderStatusWatcher.getInstance(ContactsApplication
                .getInstance());
        /** @} */
    }

    @Override
    public String toString() {
        // Shown on logcat
        return String.format("%s@%d", getClass().getSimpleName(), mInstanceId);
    }

    public boolean areContactsAvailable() {
        return (mProviderStatus != null)
                && mProviderStatus.status == ProviderStatus.STATUS_NORMAL;
    }

    private boolean areContactWritableAccountsAvailable() {
        return ContactsUtils.areContactWritableAccountsAvailable(this);
    }

    private boolean areGroupWritableAccountsAvailable() {
        return ContactsUtils.areGroupWritableAccountsAvailable(this);
    }

    /**
     * Initialize fragments that are (or may not be) in the layout.
     *
     * For the fragments that are in the layout, we initialize them in
     * {@link #createViewsAndFragments(Bundle)} after inflating the layout.
     *
     * However, there are special fragments which may not be in the layout, so we have to do the
     * initialization here.
     * The target fragments are:
     * - {@link ContactDetailFragment} and {@link ContactDetailUpdatesFragment}:  They may not be
     *   in the layout depending on the configuration.  (i.e. portrait)
     * - {@link ContactsUnavailableFragment}: We always create it at runtime.
     */
    @Override
    public void onAttachFragment(Fragment fragment) {
        PDebug.Start("Contacts.onAttachFragment");
        if (fragment instanceof ContactDetailFragment) {
            mContactDetailFragment = (ContactDetailFragment) fragment;
        } else if (fragment instanceof ContactsUnavailableFragment) {
            mContactsUnavailableFragment = (ContactsUnavailableFragment)fragment;
            mContactsUnavailableFragment.setOnContactsUnavailableActionListener(
                    new ContactsUnavailableFragmentListener());
        }
        PDebug.End("Contacts.onAttachFragment");
    }

    @Override
    protected void onCreate(Bundle savedState) {
        PDebug.Start("Contacts.onCreate");
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate start");
        }
        super.onCreate(savedState);

        // /M: VCS featrue. @{
        if (VCSUtils.isVCSFeatureEnabled()) {
            LogUtils.i(TAG, "[onCreate] [vcs] new VoiceSearchManager/VoiceSearchDialogFragment/VoiceSearchResultLoader instances");
            mVoiceSearchMgr = new VoiceSearchManager(this, this);
            mVoiceSearchDialogFragment = new VoiceSearchDialogFragment(this);
            mVoiceSearchResultLoader = new VoiceSearchResultLoader(getApplicationContext(), this);
        }
        // / @}

        /** M: Bug Fix for ALPS00393950 @{ */
        SetIndicatorUtils.getInstance().registerReceiver(this);
        /** @} */

        if (!processIntent(false)) {
            finish();
            return;
        }

        Xlog.i(TAG, "[Performance test][Contacts] loading data start time: ["
                + System.currentTimeMillis() + "]");

        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.checkFilterValidity(false);
        mContactListFilterController.addListener(this);

        mProviderStatusWatcher.addListener(this);

        mIsRecreatedInstance = (savedState != null);
        
        PDebug.Start("createViewsAndFragments");
        createViewsAndFragments(savedState);
        PDebug.End("createViewsAndFragments");
        
        getWindow().setBackgroundDrawableResource(R.color.background_primary);
        
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate finish");
        }

        /**
         * M: For plug-in
         * register context to plug-in, so that the plug-in can use
         * host context to show dialog @{
         */
        PDebug.Start("init plugin");
        ExtensionManager.getInstance().getContactListExtension()
                .registerHostContext(this, null, ContactPluginDefault.COMMD_FOR_OP01);
        ExtensionManager.getInstance().getContactListExtension()
                .registerHostContext(this, null, ContactPluginDefault.COMMD_FOR_OP09);
        /** @} */
        PDebug.End("init plugin");
        
        PDebug.End("Contacts.onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        PDebug.Start("onNewIntent");
        setIntent(intent);
        if (!processIntent(true)) {
            finish();
            return;
        }
        mActionBarAdapter.initialize(null, mRequest);

        mContactListFilterController.checkFilterValidity(false);
        mCurrentFilterIsValid = true;

        // Re-configure fragments.
        configureFragments(true /* from request */);
        invalidateOptionsMenuIfNeeded();
        PDebug.End("onNewIntent");
    }

    /**
     * Resolve the intent and initialize {@link #mRequest}, and launch another activity if redirect
     * is needed.
     *
     * @param forNewIntent set true if it's called from {@link #onNewIntent(Intent)}.
     * @return {@code true} if {@link PeopleActivity} should continue running.  {@code false}
     *         if it shouldn't, in which case the caller should finish() itself and shouldn't do
     *         farther initialization.
     */
    private boolean processIntent(boolean forNewIntent) {
        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, this + " processIntent: forNewIntent=" + forNewIntent
                    + " intent=" + getIntent() + " request=" + mRequest);
        }
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            return false;
        }

        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            return false;
        }

        if (mRequest.getActionCode() == ContactsRequest.ACTION_VIEW_CONTACT
                && !PhoneCapabilityTester.isUsingTwoPanes(this)) {
            redirect = new Intent(this, ContactDetailActivity.class);
            redirect.setAction(Intent.ACTION_VIEW);
            redirect.setData(mRequest.getContactUri());
            startActivity(redirect);
            return false;
        }
        return true;
    }

    private void createViewsAndFragments(Bundle savedState) {
        setContentView(R.layout.people_activity);

        final FragmentManager fragmentManager = getFragmentManager();

        // Hide all tabs (the current tab will later be reshown once a tab is selected)
        final FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Prepare the fragments which are used both on 1-pane and on 2-pane.
        final boolean isUsingTwoPanes = PhoneCapabilityTester.isUsingTwoPanes(this);
        Log.d(TAG, "isUsingTwoPanes: " + isUsingTwoPanes);
        if (isUsingTwoPanes) {
            mFavoritesFragment = getFragment(R.id.favorites_fragment);
            mAllFragment = getFragment(R.id.all_fragment);
            mGroupsFragment = getFragment(R.id.groups_fragment);
        } else {
            PDebug.Start("createViewsAndFragments, prepare fragments");
            mTabPager = getView(R.id.tab_pager);
            mTabPagerAdapter = new TabPagerAdapter();
            mTabPager.setAdapter(mTabPagerAdapter);
            mTabPager.setOnPageChangeListener(mTabPagerListener);

            final String FAVORITE_TAG = "tab-pager-favorite";
            final String ALL_TAG = "tab-pager-all";
            final String GROUPS_TAG = "tab-pager-groups";

            // Create the fragments and add as children of the view pager.
            // The pager adapter will only change the visibility; it'll never create/destroy
            // fragments.
            // However, if it's after screen rotation, the fragments have been re-created by
            // the fragment manager, so first see if there're already the target fragments
            // existing.
            mFavoritesFragment = (ContactTileListFragment)
                    fragmentManager.findFragmentByTag(FAVORITE_TAG);
            mAllFragment = (DefaultContactBrowseListFragment)
                    fragmentManager.findFragmentByTag(ALL_TAG);
            mGroupsFragment = (GroupBrowseListFragment)
                    fragmentManager.findFragmentByTag(GROUPS_TAG);

            if (mFavoritesFragment == null) {
                mFavoritesFragment = new ContactTileListFragment();
                mAllFragment = new DefaultContactBrowseListFragment();
                mGroupsFragment = new GroupBrowseListFragment();

                transaction.add(R.id.tab_pager, mFavoritesFragment, FAVORITE_TAG);
                transaction.add(R.id.tab_pager, mAllFragment, ALL_TAG);
                transaction.add(R.id.tab_pager, mGroupsFragment, GROUPS_TAG);
            }
            PDebug.End("createViewsAndFragments, prepare fragments");
        }

        mFavoritesFragment.setListener(mFavoritesFragmentListener);

        mAllFragment.setOnContactListActionListener(new ContactBrowserActionListener());

        mGroupsFragment.setListener(new GroupBrowserActionListener());

        // Hide all fragments for now.  We adjust visibility when we get onSelectedTabChanged()
        // from ActionBarAdapter.
        transaction.hide(mFavoritesFragment);
        transaction.hide(mAllFragment);
        transaction.hide(mGroupsFragment);

        if (isUsingTwoPanes) {
            // Prepare 2-pane only fragments/views...

            // Container views for fragments
            mPeopleActivityView = getView(R.id.people_view);
            mFavoritesView = getView(R.id.favorites_view);
            mContactDetailsView = getView(R.id.contact_details_view);
            mGroupDetailsView = getView(R.id.group_details_view);
            mBrowserView = getView(R.id.browse_view);

            // Only favorites tab with two panes has a separate frequent fragment
            if (PhoneCapabilityTester.isUsingTwoPanesInFavorites(this)) {
                mFrequentFragment = getFragment(R.id.frequent_fragment);
                mFrequentFragment.setListener(mFavoritesFragmentListener);
                mFrequentFragment.setDisplayType(DisplayType.FREQUENT_ONLY);
                mFrequentFragment.enableQuickContact(true);
            }

            mContactDetailLoaderFragment = getFragment(R.id.contact_detail_loader_fragment);
            mContactDetailLoaderFragment.setListener(mContactDetailLoaderFragmentListener);

            mGroupDetailFragment = getFragment(R.id.group_detail_fragment);
            mGroupDetailFragment.setListener(mGroupDetailFragmentListener);
            mGroupDetailFragment.setQuickContact(true);

            if (mContactDetailFragment != null) {
                transaction.hide(mContactDetailFragment);
            }
            transaction.hide(mGroupDetailFragment);

            // Configure contact details
            mContactDetailLayoutController = new ContactDetailLayoutController(this, savedState,
                    getFragmentManager(), mContactDetailsView,
                    findViewById(R.id.contact_detail_container),
                    new ContactDetailFragmentListener());
        }
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();

        // Setting Properties after fragment is created
        if (PhoneCapabilityTester.isUsingTwoPanesInFavorites(this)) {
            mFavoritesFragment.enableQuickContact(true);
            mFavoritesFragment.setDisplayType(DisplayType.STARRED_ONLY);
        } else {
            // For 2-pane in All and Groups but not in Favorites fragment, show the chevron
            // for quick contact popup
            mFavoritesFragment.enableQuickContact(isUsingTwoPanes);
            mFavoritesFragment.setDisplayType(DisplayType.STREQUENT);
        }

        // Configure action bar
        PDebug.Start("createViewsAndFragments, Configure action bar");
        mActionBarAdapter = new ActionBarAdapter(this, this, getActionBar(), isUsingTwoPanes);
        mActionBarAdapter.initialize(savedState, mRequest);
        PDebug.End("createViewsAndFragments, Configure action bar");

        invalidateOptionsMenuIfNeeded();
    }

    @Override
    protected void onStart() {
        PDebug.Start("Contacts.onStart");
        if (!mFragmentInitialized) {
            mFragmentInitialized = true;
            /* Configure fragments if we haven't.
             *
             * Note it's a one-shot initialization, so we want to do this in {@link #onCreate}.
             *
             * However, because this method may indirectly touch views in fragments but fragments
             * created in {@link #configureContentView} using a {@link FragmentTransaction} will NOT
             * have views until {@link Activity#onCreate} finishes (they would if they were inflated
             * from a layout), we need to do it here in {@link #onStart()}.
             *
             * (When {@link Fragment#onCreateView} is called is different in the former case and
             * in the latter case, unfortunately.)
             *
             * Also, we skip most of the work in it if the activity is a re-created one.
             * (so the argument.)
             */
            configureFragments(!mIsRecreatedInstance);
        } else if (PhoneCapabilityTester.isUsingTwoPanes(this) && !mCurrentFilterIsValid) {
            // We only want to do the filter check in onStart for wide screen devices where it
            // is often possible to get into single contact mode. Only do this check if
            // the filter hasn't already been set properly (i.e. onCreate or onActivityResult).

            // Since there is only one {@link ContactListFilterController} across multiple
            // activity instances, make sure the filter controller is in sync withthe current
            // contact list fragment filter.
            // TODO: Clean this up. Perhaps change {@link ContactListFilterController} to not be a
            // singleton?
            mContactListFilterController.setContactListFilter(mAllFragment.getFilter(), true);
            mContactListFilterController.checkFilterValidity(true);
            mCurrentFilterIsValid = true;
        }
        super.onStart();
        PDebug.End("Contacts.onStart");
    }

    @Override
    protected void onPause() {
        mOptionsMenuContactsAvailable = false;
        mProviderStatusWatcher.stop();
        /*
         * New Feature by Mediatek Begin.
         *   Original Android's code:
         *     null
         *   CR ID: ALPS00112598
         *   Descriptions: 
         */
        SetIndicatorUtils.getInstance().showIndicator(false, this);
        /*
         * New Feature by Mediatek End.
         */

        /// M: VCS Feature.@{
        if (VCSUtils.isVCSFeatureEnabled()) {
            LogUtils.i(TAG, "[onPause] [vcs] call stopVoiceSearch");
            // remove or dismiss no contacts dialog fragment when "home"
            mVoiceSearchMgr.stopVoiceCommand(true);
            FragmentManager fManager = this.getFragmentManager();
            dismissDlgFrag(fManager);
        }
        /// @}

        super.onPause();
    }

    @Override
    protected void onResume() {
        PDebug.Start("Contacts.onResume");
        super.onResume();

        mProviderStatusWatcher.start();
        Log.i(TAG,"call showContactsUnavailableFragmentIfNecessary in onresume");
        updateViewConfiguration(true);

        // Re-register the listener, which may have been cleared when onSaveInstanceState was
        // called.  See also: onSaveInstanceState
        mActionBarAdapter.setListener(this);
        mDisableOptionItemSelected = false;
        if (mTabPager != null) {
            mTabPager.setOnPageChangeListener(mTabPagerListener);
        }
        // Current tab may have changed since the last onSaveInstanceState().  Make sure
        // the actual contents match the tab.
        updateFragmentsVisibility();

        /*
         * New Feature by Mediatek Begin.
         *   Original Android's code:
         *     null
         *   CR ID: ALPS00112598
         *   Descriptions: 
         */
        SetIndicatorUtils.getInstance().showIndicator(true, this);
        //added by mediatek 4.05
        mActionBarAdapter.setQueryString(mActionBarAdapter.getQueryString());
        Xlog.i(TAG, "[Performance test][Contacts] loading data end time: ["
                + System.currentTimeMillis() + "]");
        /*
         * New Feature by Mediatek End.
         */
        
        // /M: VCS feature @{
        if (VCSUtils.isVCSFeatureEnabled()) {
                // /M:show vcs app guide when first launch contacts
             mIsShowingGuide = VoiceSearchManager.setVcsAppGuideVisibility(this, true,mGuideFinishListener);
             startVoiceService();
        }
        // / @}
        PDebug.End("Contacts.onResume");
    }

    @Override
    protected void onStop() {
        PDebug.Start("onStop");
    	if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
			mActionBarAdapter.setSearchMode(false);
			invalidateOptionsMenu();
    	}
		
        super.onStop();
        mCurrentFilterIsValid = false;
        PDebug.End("onStop");
    }

    @Override
    protected void onDestroy() {
        PDebug.Start("onDestroy");
        mProviderStatusWatcher.removeListener(this);

        // Some of variables will be null if this Activity redirects Intent.
        // See also onCreate() or other methods called during the Activity's initialization.
        if (mActionBarAdapter != null) {
            mActionBarAdapter.setListener(null);
        }
        if (mContactListFilterController != null) {
            mContactListFilterController.removeListener(this);
        }

        /*
         * New Feature by Mediatek Begin.
         *   Original Android's code:
         *     null
         *   CR ID: ALPS00112598
         *   Descriptions: 
         */
        SetIndicatorUtils.getInstance().unregisterReceiver(this);
        /*
         * New Feature by Mediatek End.
         */

        // / M: VCS feature.@{
        if (VCSUtils.isVCSFeatureEnabled()) {
            //destroy the last loader with id:0
            getLoaderManager().destroyLoader(0);
            mVoiceSearchMgr.destroyVoiceSearch();
            mVoiceSearchMgr.setVcsAppGuideVisibility(this,false,mGuideFinishListener);
            if(mVoiceIndicator != null){
                mVoiceIndicator.updateIndicator(false);
                mVoiceIndicator = null;
            }
        }
        // / @}
        
        super.onDestroy();
        PDebug.End("onDestroy");
    }

    private void configureFragments(boolean fromRequest) {
        if (fromRequest) {
            ContactListFilter filter = null;
            int actionCode = mRequest.getActionCode();
            boolean searchMode = mRequest.isSearchMode();
            final int tabToOpen;
            switch (actionCode) {
                case ContactsRequest.ACTION_ALL_CONTACTS:
                    filter = ContactListFilter.createFilterWithType(
                            ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS);
                    tabToOpen = TabState.ALL;
                    break;
                case ContactsRequest.ACTION_CONTACTS_WITH_PHONES:
                    filter = ContactListFilter.createFilterWithType(
                            ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY);
                    tabToOpen = TabState.ALL;
                    break;

                case ContactsRequest.ACTION_FREQUENT:
                case ContactsRequest.ACTION_STREQUENT:
                case ContactsRequest.ACTION_STARRED:
                    tabToOpen = TabState.FAVORITES;
                    break;
                case ContactsRequest.ACTION_VIEW_CONTACT:
                    // We redirect this intent to the detail activity on 1-pane, so we don't get
                    // here.  It's only for 2-pane.
                    Uri currentlyLoadedContactUri = mContactDetailFragment.getUri();

                    /** M: Bug Fix for CR ALPS01210883 @{ */
                    // if (currentlyLoadedContactUri != null
                    //  && !mRequest.getContactUri().equals(currentlyLoadedContactUri))
                    // {
                    if (currentlyLoadedContactUri != null && mRequest.getContactUri() != null) {
                        String currentlyContactId = currentlyLoadedContactUri.getLastPathSegment();
                        String requestContactId = mRequest.getContactUri().getLastPathSegment();
                        if (currentlyContactId != null
                                && !currentlyContactId.equals(requestContactId)) {
                            mContactDetailsView.setMaskVisibility(true);
                        }
                    }
                    /** @} */
                    tabToOpen = TabState.ALL;
                    break;
                case ContactsRequest.ACTION_GROUP:
                    tabToOpen = TabState.GROUPS;
                    break;
                default:
                    tabToOpen = -1;
                    break;
            }
            if (tabToOpen != -1) {
                mActionBarAdapter.setCurrentTab(tabToOpen);
            }

            if (filter != null) {
                mContactListFilterController.setContactListFilter(filter, false);
                searchMode = false;
            }

            if (mRequest.getContactUri() != null) {
                searchMode = false;
            }

            mActionBarAdapter.setSearchMode(searchMode);
            configureContactListFragmentForRequest();
        }

        configureContactListFragment();
        configureGroupListFragment();

        invalidateOptionsMenuIfNeeded();
    }

    @Override
    public void onContactListFilterChanged() {
        if (mAllFragment == null || !mAllFragment.isAdded()) {
            return;
        }

        mAllFragment.setFilter(mContactListFilterController.getFilter());

        invalidateOptionsMenuIfNeeded();
    }

    private void setupContactDetailFragment(final Uri contactLookupUri) {
        mContactDetailLoaderFragment.loadUri(contactLookupUri);
        invalidateOptionsMenuIfNeeded();
    }

    private void setupGroupDetailFragment(Uri groupUri) {
        // If we are switching from one group to another, do a cross-fade
        if (mGroupDetailFragment != null && mGroupDetailFragment.getGroupUri() != null &&
                !UriUtils.areEqual(mGroupDetailFragment.getGroupUri(), groupUri)) {
            mGroupDetailsView.startMaskTransition(false, -1);
        }
        mGroupDetailFragment.loadGroup(groupUri);
        invalidateOptionsMenuIfNeeded();
    }

    /**
     * Handler for action bar actions.
     */
    @Override
    public void onAction(int action) {
        LogUtils.i(TAG, "[onAction] [vcs] action:" + action);
        switch (action) {
        case ActionBarAdapter.Listener.Action.START_SEARCH_MODE:
            // M: add for VCS-need to stop voice search contacts.@{
            if (VCSUtils.isVCSFeatureEnabled()) {
                stopVoiceService();
            }
            // @}
            // Tell the fragments that we're in the search mode
            configureFragments(false /* from request */);
            updateFragmentsVisibility();
            invalidateOptionsMenu();
            break;
        case ActionBarAdapter.Listener.Action.STOP_SEARCH_MODE:
            // M: add for VCS-need to restart voice search contacts.@{
            if (VCSUtils.isVCSFeatureEnabled()) {
                //isNeedActivate = true;
                startVoiceService();
            }
            // @}
            setQueryTextToFragment("");
            updateFragmentsVisibility();
            invalidateOptionsMenu();
            break;
        case ActionBarAdapter.Listener.Action.CHANGE_SEARCH_QUERY:
            setQueryTextToFragment(mActionBarAdapter.getQueryString());
            break;
        default:
            throw new IllegalStateException("Unkonwn ActionBarAdapter action: " + action);
        }
    }

    @Override
    public void onSelectedTabChanged() {
        // / M: VCS Feature.@{
        if (VCSUtils.isVCSFeatureEnabled()) {
            LogUtils.i(TAG, "[onSelectedTabChanged] [vcs] onVoiceSearchProcess");
            updateVoiceSearStatus();
        }
        // / @}
        updateFragmentsVisibility();
    }

    /**
     * Updates the fragment/view visibility according to the current mode, such as
     * {@link ActionBarAdapter#isSearchMode()} and {@link ActionBarAdapter#getCurrentTab()}.
     */
    private void updateFragmentsVisibility() {
        int tab = mActionBarAdapter.getCurrentTab();

        // We use ViewPager on 1-pane.
        if (!PhoneCapabilityTester.isUsingTwoPanes(this)) {
            if (mActionBarAdapter.isSearchMode()) {
                mTabPagerAdapter.setSearchMode(true);
            } else {
                // No smooth scrolling if quitting from the search mode.
                final boolean wasSearchMode = mTabPagerAdapter.isSearchMode();
                mTabPagerAdapter.setSearchMode(false);
                if (mTabPager.getCurrentItem() != tab) {
                    mTabPager.setCurrentItem(tab, !wasSearchMode);
                }
            }
            invalidateOptionsMenu();
            showEmptyStateForTab(tab);
            if (tab == TabState.GROUPS) {
                mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
            }
            return;
        }

        // for the tablet...

        // If in search mode, we use the all list + contact details to show the result.
        if (mActionBarAdapter.isSearchMode()) {
            tab = TabState.ALL;
        }

        switch (tab) {
            case TabState.FAVORITES:
                mFavoritesView.setVisibility(View.VISIBLE);
                mBrowserView.setVisibility(View.GONE);
                mGroupDetailsView.setVisibility(View.GONE);
                mContactDetailsView.setVisibility(View.GONE);
                break;
            case TabState.GROUPS:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.VISIBLE);
                mGroupDetailsView.setVisibility(View.VISIBLE);
                mContactDetailsView.setVisibility(View.GONE);
                mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
                break;
            case TabState.ALL:
                mFavoritesView.setVisibility(View.GONE);
                mBrowserView.setVisibility(View.VISIBLE);
                mContactDetailsView.setVisibility(View.VISIBLE);
                mGroupDetailsView.setVisibility(View.GONE);
                break;
        }
        mPeopleActivityView.startMaskTransition(false, TAB_FADE_IN_DURATION);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        // Note mContactDetailLoaderFragment is an invisible fragment, but we still have to show/
        // hide it so its options menu will be shown/hidden.
        switch (tab) {
            case TabState.FAVORITES:
                showFragment(ft, mFavoritesFragment);
                showFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                break;
            case TabState.ALL:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                showFragment(ft, mAllFragment);
                showFragment(ft, mContactDetailLoaderFragment);
                showFragment(ft, mContactDetailFragment);
                hideFragment(ft, mGroupsFragment);
                hideFragment(ft, mGroupDetailFragment);
                break;
            case TabState.GROUPS:
                hideFragment(ft, mFavoritesFragment);
                hideFragment(ft, mFrequentFragment);
                hideFragment(ft, mAllFragment);
                hideFragment(ft, mContactDetailLoaderFragment);
                hideFragment(ft, mContactDetailFragment);
                showFragment(ft, mGroupsFragment);
                showFragment(ft, mGroupDetailFragment);
                break;
        }
        if (!ft.isEmpty()) {
            ft.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
            // When switching tabs, we need to invalidate options menu, but executing a
            // fragment transaction does it implicitly.  We don't have to call invalidateOptionsMenu
            // manually.
        }
        showEmptyStateForTab(tab);
    }

    private void showEmptyStateForTab(int tab) {
        if (mContactsUnavailableFragment != null) {
            switch (tab) {
                case TabState.FAVORITES:
                    mContactsUnavailableFragment.setMessageText(
                            R.string.listTotalAllContactsZeroStarred, -1);
                    break;
                case TabState.GROUPS:
                    mContactsUnavailableFragment.setMessageText(R.string.noGroups,
                            areGroupWritableAccountsAvailable() ? -1 : R.string.noAccounts);
                    break;
                case TabState.ALL:
                    mContactsUnavailableFragment.setMessageText(R.string.noContacts, -1);
                    break;
            }
        }
    }

    private class TabPagerListener implements ViewPager.OnPageChangeListener {

        // This package-protected constructor is here because of a possible compiler bug.
        // PeopleActivity$1.class should be generated due to the private outer/inner class access
        // needed here.  But for some reason, PeopleActivity$1.class is missing.
        // Since $1 class is needed as a jvm work around to get access to the inner class,
        // changing the constructor to package-protected or public will solve the problem.
        // To verify whether $1 class is needed, javap PeopleActivity$TabPagerListener and look for
        // references to PeopleActivity$1.
        //
        // When the constructor is private and PeopleActivity$1.class is missing, proguard will
        // correctly catch this and throw warnings and error out the build on user/userdebug builds.
        //
        // All private inner classes below also need this fix.
        TabPagerListener() {}

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Make sure not in the search mode, in which case position != TabState.ordinal().
            if (!mTabPagerAdapter.isSearchMode()) {
                mActionBarAdapter.setCurrentTab(position, false);
                showEmptyStateForTab(position);
                if (position == TabState.GROUPS) {
                    mGroupsFragment.setAddAccountsVisibility(!areGroupWritableAccountsAvailable());
                }
                // / M: VCS Feature. @{
                if (VCSUtils.isVCSFeatureEnabled() && mActionBarAdapter.getCurrentTab() == TabState.ALL) {
                    LogUtils.i(TAG, "[onPageSelected] [vcs] onVoiceSearchProcess.");
                   updateVoiceSearStatus();
                }
                // / @}
                invalidateOptionsMenu();
            }
        }
    }

    /**
     * Adapter for the {@link ViewPager}.  Unlike {@link FragmentPagerAdapter},
     * {@link #instantiateItem} returns existing fragments, and {@link #instantiateItem}/
     * {@link #destroyItem} show/hide fragments instead of attaching/detaching.
     *
     * In search mode, we always show the "all" fragment, and disable the swipe.  We change the
     * number of items to 1 to disable the swipe.
     *
     * TODO figure out a more straight way to disable swipe.
     */
    private class TabPagerAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;

        private boolean mTabPagerAdapterSearchMode;

        private Fragment mCurrentPrimaryItem;

        public TabPagerAdapter() {
            mFragmentManager = getFragmentManager();
        }

        public boolean isSearchMode() {
            return mTabPagerAdapterSearchMode;
        }

        public void setSearchMode(boolean searchMode) {
            if (searchMode == mTabPagerAdapterSearchMode) {
                return;
            }
            mTabPagerAdapterSearchMode = searchMode;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabPagerAdapterSearchMode ? 1 : TabState.COUNT;
        }

        /** Gets called when the number of items changes. */
        @Override
        public int getItemPosition(Object object) {
            if (mTabPagerAdapterSearchMode) {
                if (object == mAllFragment) {
                    return 0; // Only 1 page in search mode
                }
            } else {
                if (object == mFavoritesFragment) {
                    return TabState.FAVORITES;
                }
                if (object == mAllFragment) {
                    return TabState.ALL;
                }
                if (object == mGroupsFragment) {
                    return TabState.GROUPS;
                }
            }
            return POSITION_NONE;
        }

        @Override
        public void startUpdate(ViewGroup container) {
        }

        private Fragment getFragment(int position) {
            if (mTabPagerAdapterSearchMode) {
                if (position != 0) {
                    // This has only been observed in monkey tests.
                    // Let's log this issue, but not crash
                    Log.w(TAG, "Request fragment at position=" + position + ", eventhough we " +
                            "are in search mode");
                }
                return mAllFragment;
            } else {
                if (position == TabState.FAVORITES) {
                    return mFavoritesFragment;
                } else if (position == TabState.ALL) {
                    return mAllFragment;
                } else if (position == TabState.GROUPS) {
                    return mGroupsFragment;
                }
            }
            throw new IllegalArgumentException("position: " + position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            mCurTransaction.show(f);

            // Non primary pages are not visible.
            f.setUserVisibleHint(f == mCurrentPrimaryItem);
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.hide((Fragment) object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (mCurTransaction != null) {
                mCurTransaction.commitAllowingStateLoss();
                mCurTransaction = null;
                mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (mCurrentPrimaryItem != fragment) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);
                }
                mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }

    private void setQueryTextToFragment(String query) {
        mAllFragment.setQueryString(query, true);
        mAllFragment.setVisibleScrollbarEnabled(!mAllFragment.isSearchMode());
    }

    private void configureContactListFragmentForRequest() {
        Uri contactUri = mRequest.getContactUri();
        if (contactUri != null) {
            // For an incoming request, explicitly require a selection if we are on 2-pane UI,
            // (i.e. even if we view the same selected contact, the contact may no longer be
            // in the list, so we must refresh the list).
            if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
                mAllFragment.setSelectionRequired(true);
            }
            mAllFragment.setSelectedContactUri(contactUri);
        }

        mAllFragment.setFilter(mContactListFilterController.getFilter());
        setQueryTextToFragment(mActionBarAdapter.getQueryString());

        if (mRequest.isDirectorySearchEnabled()) {
            mAllFragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DEFAULT);
        } else {
            mAllFragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
        }
    }

    private void configureContactListFragment() {
        // Filter may be changed when this Activity is in background.
        mAllFragment.setFilter(mContactListFilterController.getFilter());

        final boolean useTwoPane = PhoneCapabilityTester.isUsingTwoPanes(this);

        mAllFragment.setVerticalScrollbarPosition(getScrollBarPosition(useTwoPane));
        mAllFragment.setSelectionVisible(useTwoPane);
        mAllFragment.setQuickContactEnabled(!useTwoPane);
    }

    private int getScrollBarPosition(boolean useTwoPane) {
        final boolean isLayoutRtl = isRTL();
        final int position;
        if (useTwoPane) {
            position = isLayoutRtl ? View.SCROLLBAR_POSITION_RIGHT : View.SCROLLBAR_POSITION_LEFT;
        } else {
            position = isLayoutRtl ? View.SCROLLBAR_POSITION_LEFT : View.SCROLLBAR_POSITION_RIGHT;
        }
        return position;
    }

    private boolean isRTL() {
        final Locale locale = Locale.getDefault();
        return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL;
    }

    private void configureGroupListFragment() {
        final boolean useTwoPane = PhoneCapabilityTester.isUsingTwoPanes(this);
        mGroupsFragment.setVerticalScrollbarPosition(getScrollBarPosition(useTwoPane));
        mGroupsFragment.setSelectionVisible(useTwoPane);
    }

    @Override
    public void onProviderStatusChange() {
        updateViewConfiguration(false);
    }

    private void updateViewConfiguration(boolean forceUpdate) {
        ProviderStatusWatcher.Status providerStatus = mProviderStatusWatcher.getProviderStatus();
        if (!forceUpdate && (mProviderStatus != null)
                && (providerStatus.status == mProviderStatus.status)) return;
        mProviderStatus = providerStatus;

        View contactsUnavailableView = findViewById(R.id.contacts_unavailable_view);
        View mainView = findViewById(R.id.main_view);
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *    
         *   CR ID: ALPS00113819
         *   Descriptions: remove ContactUnavaliableFragment 
         */
        boolean isUsingTwoPanes = PhoneCapabilityTester.isUsingTwoPanes(this);
        /*
         * Bug Fix by Mediatek End.
         */
        
        if (mProviderStatus.status == ProviderStatus.STATUS_NORMAL) {
            // Ensure that the mTabPager is visible; we may have made it invisible below.
            contactsUnavailableView.setVisibility(View.GONE);
            if (mTabPager != null) {
                mTabPager.setVisibility(View.VISIBLE);
            }

            if (mainView != null) {
                mainView.setVisibility(View.VISIBLE);
            }
            if (mAllFragment != null) {
                mAllFragment.setEnabled(true);
            }
        } else {
            // If there are no accounts on the device and we should show the "no account" prompt
            // (based on {@link SharedPreferences}), then launch the account setup activity so the
            // user can sign-in or create an account.
            //
            // Also check for ability to modify accounts.  In limited user mode, you can't modify
            // accounts so there is no point sending users to account setup activity.
            final UserManager userManager = UserManager.get(this);
            final boolean disallowModifyAccounts = userManager.getUserRestrictions().getBoolean(
                    UserManager.DISALLOW_MODIFY_ACCOUNTS);
            if (!disallowModifyAccounts && !areContactWritableAccountsAvailable() &&
                    AccountPromptUtils.shouldShowAccountPrompt(this)) {
                AccountPromptUtils.launchAccountPrompt(this);
                return;
            }

            // Otherwise, continue setting up the page so that the user can still use the app
            // without an account.
            if (mAllFragment != null) {
                mAllFragment.setEnabled(false);
            }
            if (mContactsUnavailableFragment == null) {
                mContactsUnavailableFragment = new ContactsUnavailableFragment();
                mContactsUnavailableFragment.setOnContactsUnavailableActionListener(
                        new ContactsUnavailableFragmentListener());
                getFragmentManager().beginTransaction()
                        .replace(R.id.contacts_unavailable_container, mContactsUnavailableFragment)
                        .commitAllowingStateLoss();
            }
            mContactsUnavailableFragment.updateStatus(mProviderStatus);

            // Show the contactsUnavailableView, and hide the mTabPager so that we don't
            // see it sliding in underneath the contactsUnavailableView at the edges.
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *    contactsUnavailableView.setVisibility(View.VISIBLE);
         *   CR ID: ALPS00113819
         *   Descriptions: remove ContactUnavaliableFragment 
         */
            boolean mDestroyed = mContactsUnavailableFragment.mDestroyed;
            boolean isNeedShow = false;
            Log.i(TAG, " mContactsUnavailableFragment.mDestroyed : " + mDestroyed
                    + " | mProviderStatus.status : " + mProviderStatus.status
                    + " | ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS : "
                    + ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS);
            if (mProviderStatus.status == ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS
                    || mDestroyed) {
                contactsUnavailableView.setVisibility(View.GONE);
                isNeedShow = true;
                if (mDestroyed) {
                    mContactsUnavailableFragment.mDestroyed = false;
                }
            } else {
                contactsUnavailableView.setVisibility(View.VISIBLE);
            }
        /*
         * Bug Fix by Mediatek End.
         */
           
            /** M: Fix wait cursor keeps showing while no contacts issue @{ */
            // mTabPager only exists while 1-pane thus the code should be modified for 2-panes
            if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
                if (isNeedShow) {
                    if (null != mAllFragment) {
                        Log.i(TAG, "close wait cursor");
                        mAllFragment.setEnabled(true);
                        mAllFragment.closeWaitCursor();
                        mAllFragment.setProfileHeader();
                    } else {
                        Log.e(TAG, "mAllFragment is null");
                    }
                    isNeedShow = false;
                }
            } else {
            /** @} */
                if (mTabPager != null) {
                    /** M: New Feature @{ */
                    // Original Code :mTabPager.setVisibility(View.GONE);
                    if (isNeedShow) {
                        mTabPager.setVisibility(View.VISIBLE);
                        if (null != mAllFragment) {
                            Log.i(TAG, "close wait cursor");
                            mAllFragment.setEnabled(true);
                            mAllFragment.closeWaitCursor();
                            //mAllFragment.setProfileHeader();
                        } else {
                            Log.e(TAG, "mAllFragment is null");
                        }
                        isNeedShow = false;
                    } else {
                        mTabPager.setVisibility(View.GONE);
                    }
                    /** @} */
                }
            /** M: Fix wait cursor keep showing while no contacts issue @{ */
            // mTabPager only exists while 1-pane thus the code should be modified for 2-panes
            }
            /** @} */


            if (mainView != null) {
                /*
                 * Bug Fix by Mediatek Begin. Original Android's code: CR ID:
                 * ALPS00248405
                 */
                if (!isUsingTwoPanes) {

                    mainView.setVisibility(View.INVISIBLE);
                }
                /*
                 * Bug Fix by Mediatek End.
                 */
            }

            showEmptyStateForTab(mActionBarAdapter.getCurrentTab());
        }

        invalidateOptionsMenuIfNeeded();
    }

    private final class ContactBrowserActionListener implements OnContactBrowserActionListener {
        ContactBrowserActionListener() {}

        @Override
        public void onSelectionChange() {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                setupContactDetailFragment(mAllFragment.getSelectedContactUri());
            }
        }

        @Override
        public void onViewContactAction(Uri contactLookupUri) {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                setupContactDetailFragment(contactLookupUri);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, contactLookupUri);
                /** M: New Feature @{ */
                // In search mode, the "up" affordance in the contact detail page should return the
                // user to the search results, so finish the activity when that button is selected.
//                if (mActionBarAdapter.isSearchMode()) {
//                    intent.putExtra(
//                            ContactDetailActivity.INTENT_KEY_FINISH_ACTIVITY_ON_UP_SELECTED, true);
                    /** @} */
//                }
                startActivity(intent);
            }
        }

        @Override
        public void onCreateNewContactAction() {
            Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                intent.putExtras(extras);
            }
            startActivity(intent);
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                intent.putExtras(extras);
            }
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            startActivityForResult(intent, SUBACTIVITY_EDIT_CONTACT);
        }

        @Override
        public void onAddToFavoritesAction(Uri contactUri) {
            ContentValues values = new ContentValues(1);
            values.put(Contacts.STARRED, 1);
            getContentResolver().update(contactUri, values, null, null);
        }

        @Override
        public void onRemoveFromFavoritesAction(Uri contactUri) {
            ContentValues values = new ContentValues(1);
            values.put(Contacts.STARRED, 0);
            getContentResolver().update(contactUri, values, null, null);
        }

        @Override
        public void onDeleteContactAction(Uri contactUri) {
            ContactDeletionInteraction.start(PeopleActivity.this, contactUri, false);
        }

        @Override
        public void onFinishAction() {
            onBackPressed();
        }

        @Override
        public void onInvalidSelection() {
            ContactListFilter filter;
            ContactListFilter currentFilter = mAllFragment.getFilter();
            if (currentFilter != null
                    && currentFilter.filterType == ContactListFilter.FILTER_TYPE_SINGLE_CONTACT) {
                filter = ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS);
                mAllFragment.setFilter(filter);
            } else {
                filter = ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_SINGLE_CONTACT);
                mAllFragment.setFilter(filter, false);
            }
            mContactListFilterController.setContactListFilter(filter, true);
        }
    }

    private class ContactDetailLoaderFragmentListener implements ContactLoaderFragmentListener {
        ContactDetailLoaderFragmentListener() {}

        @Override
        public void onContactNotFound() {
            // Nothing needs to be done here
        }

        @Override
        public void onDetailsLoaded(final Contact result) {
            if (result == null) {
                // Nothing is loaded. Show empty state.
                mContactDetailLayoutController.showEmptyState();
                return;
            }
            // Since {@link FragmentTransaction}s cannot be done in the onLoadFinished() of the
            // {@link LoaderCallbacks}, then post this {@link Runnable} to the {@link Handler}
            // on the main thread to execute later.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // If the activity is destroyed (or will be destroyed soon), don't update the UI
                    if (isFinishing()) {
                        return;
                    }
                    /** M: New Feature xxx @{ */
                    if (result != null) {
                        mContactData = result;
                    }
                    /** @} */
                    mContactDetailLayoutController.setContactData(result);
                }
            });
        }

        @Override
        public void onEditRequested(Uri contactLookupUri) {
            Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
            startActivityForResult(intent, SUBACTIVITY_EDIT_CONTACT);
        }

        @Override
        public void onDeleteRequested(Uri contactUri) {
            /** M: New Feature xxx @{ */
            /*
             * orignal code
             * ContactDeletionInteraction.start(PeopleActivity.this, contactUri,
             * false);
             */
            if (mContactData.getIndicate() < 0) {
                /// M: change for SIM Service refactoring
                /*
                ContactDeletionInteraction
                        .start(PeopleActivity.this, contactUri, false, null, null);
                        */
                ContactDeletionInteraction.start(PeopleActivity.this, contactUri, false);
            } else {
                int simIndex = mContactData.getSimIndex();
                ///M:fix CR ALPS01065879,sim Info Manager API Remove
                SimInfoRecord simInfo = SimInfoManager.getSimInfoById(PeopleActivity.this, mContactData.getIndicate());
                int slotId;
                if (simInfo == null) {
                    slotId = -1;
                } else {
                    slotId = simInfo.mSimSlotId;
                }
                Uri simUri = SimCardUtils.SimUri.getSimUri(slotId);
                Log.d(TAG, "onDeleteRequested contact indicate = " + mContactData.getIndicate());
                Log.d(TAG, "onDeleteRequested slot id = " + slotId);
                Log.d(TAG, "onDeleteRequested simUri = " + simUri);
                ContactDeletionInteraction.start(PeopleActivity.this, contactUri, false, simUri,
                        ("index = " + simIndex), slotId);
            }
            /** @} */

        }
    }

    public class ContactDetailFragmentListener implements ContactDetailFragment.Listener {
        @Override
        public void onItemClicked(Intent intent) {
            if (intent == null) {
                return;
            }
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found for intent: " + intent);
            }
        }

        @Override
        public void onCreateRawContactRequested(ArrayList<ContentValues> values,
                AccountWithDataSet account) {
            Toast.makeText(PeopleActivity.this, R.string.toast_making_personal_copy,
                    Toast.LENGTH_LONG).show();
            Intent serviceIntent = ContactSaveService.createNewRawContactIntent(
                    PeopleActivity.this, values, account,
                    PeopleActivity.class, Intent.ACTION_VIEW);
            startService(serviceIntent);
        }
    }

    private class ContactsUnavailableFragmentListener
            implements OnContactsUnavailableActionListener {
        ContactsUnavailableFragmentListener() {}

        @Override
        public void onCreateNewContactAction() {
            startActivity(new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
        }

        @Override
        public void onAddAccountAction() {
            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Settings.EXTRA_AUTHORITIES,
                    new String[] { ContactsContract.AUTHORITY });
            startActivity(intent);
        }

        @Override
        public void onImportContactsFromFileAction() {
            /** M: New Feature xxx @{ */
            /*
             * orignal code
             * ImportExportDialogFragment.show(getFragmentManager(),
             * areContactsAvailable());
             */
            // ImportExportDialogFragment.show(getFragmentManager());
            final Intent intent = new Intent(PeopleActivity.this, ContactImportExportActivity.class);
            startActivity(intent);
            /** @} */

        }

        @Override
        public void onFreeInternalStorageAction() {
            startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        }
    }

    private final class StrequentContactListFragmentListener
            implements ContactTileListFragment.Listener {
        StrequentContactListFragmentListener() {}

        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                QuickContact.showQuickContact(PeopleActivity.this, targetRect, contactUri, 0, null);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, contactUri));
            }
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            // No need to call phone number directly from People app.
            Log.w(TAG, "unexpected invocation of onCallNumberDirectly()");
        }
    }

    private final class GroupBrowserActionListener implements OnGroupBrowserActionListener {

        GroupBrowserActionListener() {}

        @Override
        public void onViewGroupAction(Uri groupUri) {
            if (PhoneCapabilityTester.isUsingTwoPanes(PeopleActivity.this)) {
                /*
                 * Bug Fix by Mediatek Begin Original Android's code:
                 * setupGroupDetailFragment(groupUri); CR ID :ALPS000231724
                 * Descriptions:
                 */

                List uriList = groupUri.getPathSegments();
                Uri newGroupUri = Uri.parse("content://com.android.contacts").buildUpon()
                        .appendPath(uriList.get(0).toString())
                        .appendPath(uriList.get(1).toString()).build();

                if (uriList.size() > 2) {
                    int slotId = Integer.parseInt(uriList.get(2).toString());
                    Log.i(TAG, "people slotId-----------" + slotId);
                    setupGroupDetailFragment(newGroupUri, slotId);
                    return;
                }

                setupGroupDetailFragment(newGroupUri);
                /*
                 * Bug Fix by Mediatek End
                 */
            } else {
                /*
                 * New feature by Mediatek Begin Original Android code: Intent
                 * intent = new Intent(PeopleActivity.this,
                 * GroupDetailActivity.class); intent.setData(groupUri);
                 * startActivity(intent);
                 */
                int simId = -1;
                int slotId = -1;
                String accountType = "";
                String accountName = "";
                Log.i(TAG, "groupUri" + groupUri.toString());
                List uriList = groupUri.getPathSegments();
                Uri newGroupUri = Uri.parse("content://com.android.contacts").buildUpon()
                        .appendPath(uriList.get(0).toString())
                        .appendPath(uriList.get(1).toString()).build();
                if (uriList.size() > 2) {
                    slotId = Integer.parseInt(uriList.get(2).toString());
                    Log.i(TAG, "people slotId-----------" + slotId);
                }
                if (uriList.size() > 3) {
                    accountType = uriList.get(3).toString();
                }
                if (uriList.size() > 4) {
                    accountName = uriList.get(4).toString();
                }
                Log.i(TAG, "newUri-----------" + newGroupUri);
                Log.i(TAG, "accountType-----------" + accountType);
                Log.i(TAG, "accountName-----------" + accountName);
                if (slotId >= 0) {
                    simId = SIMInfoWrapper.getDefault().getSimIdBySlotId(slotId);
                }
                Intent intent = new Intent(PeopleActivity.this, GroupDetailActivity.class);
                intent.setData(newGroupUri);
                intent.putExtra("AccountCategory", new AccountCategoryInfo(accountType, slotId,
                        simId, accountName));
                // startActivity(intent);

                /** M: Bug Fix for CR ALPS00463033 @{ */
                startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
                /** @} */
                /*
                 * New feature by Mediatek End
                 */
            }
        }
    }

    private class GroupDetailFragmentListener implements GroupDetailFragment.Listener {

        GroupDetailFragmentListener() {}

        @Override
        public void onGroupSizeUpdated(String size) {
            // Nothing needs to be done here because the size will be displayed in the detail
            // fragment
        }
        
        @Override
        public void onGroupNotFound() {
            // Nothing needs to be done here because the details page will be finished
        }

        @Override
        public void onGroupTitleUpdated(String title) {
            // Nothing needs to be done here because the title will be displayed in the detail
            // fragment
        }

        @Override
        public void onAccountTypeUpdated(String accountTypeString, String dataSet) {
            // Nothing needs to be done here because the group source will be displayed in the
            // detail fragment
        }

        @Override
        public void onEditRequested(Uri groupUri) {
            final Intent intent = new Intent(PeopleActivity.this, GroupEditorActivity.class);
            /*
            modify for ALPS246046, ALPS242371 on tablet 
            because of GroupDetail fragment used instead of GroupDetail activity
            original code:
            intent.setData(groupUri);
            intent.setAction(Intent.ACTION_EDIT);
            startActivityForResult(intent, SUBACTIVITY_EDIT_GROUP);
            */
            int simId = -1;
            int slotId = Integer.parseInt(groupUri.getLastPathSegment().toString());
            String grpId = groupUri.getPathSegments().get(1).toString();
            Log.i(TAG, grpId + "--------grpId");
            Uri uri = Uri.parse("content://com.android.contacts/groups").buildUpon().appendPath(
                    grpId).build();
            Log.i(TAG, uri.toString() + "--------groupUri.getPath();");
            intent.setData(uri);
            intent.setAction(Intent.ACTION_EDIT);
            intent.putExtra("SLOT_ID", slotId);
            if (slotId >= 0) {
                simId = SIMInfoWrapper.getDefault().getSimIdBySlotId(slotId);
            }
            intent.putExtra("SIM_ID", simId);
            startActivity(intent);
        }

        @Override
        public void onContactSelected(Uri contactUri) {
            // Nothing needs to be done here because either quickcontact will be displayed
            // or activity will take care of selection
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        PDebug.Start("onCreateOptionsMenu");
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *    
         *   CR ID: ALPS00113819
         *   Descriptions: always show actionbar
         */
        
//        if (!areContactsAvailable()) {
//            // If contacts aren't available, hide all menu items.
//            return false;
//        }
        /*
         * Bug Fix by Mediatek End.
         */
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.people_options, menu);

        /**
         * M: For plug-in
         * extend the default menu items
         * @{
         */
        ExtensionManager.getInstance().getContactListExtension()
                .addOptionsMenu(menu, null, ContactPluginDefault.COMMD_FOR_OP01);
        ExtensionManager.getInstance().getContactListExtension()
                .addOptionsMenu(menu, null, ContactPluginDefault.COMMD_FOR_OP09);
        /** @} */

        /// M: for VCS new feature @{
        if (VCSUtils.isVCSFeatureEnabled()) {
            mVCSItem = menu.findItem(R.id.menu_vcs);
            // set item not clickable if need
            if (mVCSItem != null) {
                if (mVoiceIndicator != null) {
                    mVoiceIndicator.updateIndicator(false);
                    mVoiceIndicator = null;
                }
                mVoiceIndicator = new VoiceSearchIndicator(mVCSItem);
                startVoiceService();
            }
        }
        /// @}
        PDebug.End("onCreateOptionsMenu");
        return true;
    }

    private void invalidateOptionsMenuIfNeeded() {
        if (isOptionsMenuChanged()) {
            invalidateOptionsMenu();
        }
    }

    public boolean isOptionsMenuChanged() {
        if (mOptionsMenuContactsAvailable != areContactsAvailable()) {
            return true;
        }

        if (mAllFragment != null && mAllFragment.isOptionsMenuChanged()) {
            return true;
        }

        if (mContactDetailLoaderFragment != null &&
                mContactDetailLoaderFragment.isOptionsMenuChanged()) {
            return true;
        }

        if (mGroupDetailFragment != null && mGroupDetailFragment.isOptionsMenuChanged()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        PDebug.Start("onPrepareOptionsMenu");
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *    
         *   CR ID: ALPS00113819
         *   Descriptions: always show actionbar
         */
//        mOptionsMenuContactsAvailable = areContactsAvailable();
//        if (!mOptionsMenuContactsAvailable) {
//            return false;
//        }
        /*
         * Bug Fix by Mediatek End.
         */
        final MenuItem addContactMenu = menu.findItem(R.id.menu_add_contact);
        final MenuItem contactsFilterMenu = menu.findItem(R.id.menu_contacts_filter);
        /** M: New Feature xxx @{ */
        final MenuItem deleteContactMenu = menu.findItem(R.id.menu_delete_contact);
        /** @} */
        MenuItem addGroupMenu = menu.findItem(R.id.menu_add_group);

        final MenuItem clearFrequentsMenu = menu.findItem(R.id.menu_clear_frequents);
        final MenuItem helpMenu = menu.findItem(R.id.menu_help);

        final boolean isSearchMode = mActionBarAdapter.isSearchMode();
        if (isSearchMode) {
            addContactMenu.setVisible(false);
            addGroupMenu.setVisible(false);
            contactsFilterMenu.setVisible(false);
            clearFrequentsMenu.setVisible(false);
            helpMenu.setVisible(false);
            /** M: New Feature xxx @{ */
            deleteContactMenu.setVisible(false);
            /** @} */
        } else {
            switch (mActionBarAdapter.getCurrentTab()) {
                case TabState.FAVORITES:
                    addContactMenu.setVisible(true);
                    addGroupMenu.setVisible(false);
                    contactsFilterMenu.setVisible(false);
                    clearFrequentsMenu.setVisible(hasFrequents());
                    /** M: New Feature xxx @{ */
                    deleteContactMenu.setVisible(false);
                    /** @} */
                    break;
                case TabState.ALL:
                    addContactMenu.setVisible(true);
                    addGroupMenu.setVisible(false);
                    contactsFilterMenu.setVisible(true);
                    clearFrequentsMenu.setVisible(false);
                    break;
                case TabState.GROUPS:
                    // Do not display the "new group" button if no accounts are available
                    if (areGroupWritableAccountsAvailable()) {
                        addGroupMenu.setVisible(true);
                    } else {
                        addGroupMenu.setVisible(false);
                    }
                    addContactMenu.setVisible(false);
                    contactsFilterMenu.setVisible(false);
                    clearFrequentsMenu.setVisible(false);
                    /** M: New Feature xxx @{ */
                    deleteContactMenu.setVisible(false);
                    /** @} */
                    break;
            }
            HelpUtils.prepareHelpMenuItem(this, helpMenu, R.string.help_url_people_main);
        }
        final boolean showMiscOptions = !isSearchMode;
        makeMenuItemVisible(menu, R.id.menu_search, showMiscOptions);
        // / M:for VCS new feature @{
        boolean showVcsItem = VCSUtils.isVCSFeatureEnabled() && (mActionBarAdapter.getCurrentTab() == TabState.ALL)
                && (mActionBarAdapter.isSearchMode() == false);
        makeMenuItemVisible(menu, R.id.menu_vcs, showVcsItem);
        if (VCSUtils.isVCSFeatureEnabled() && !showVcsItem) {
            //if current not show vcs item,stop voice service.
            stopVoiceService();
        }
        if (VCSUtils.isVCSFeatureEnabled() && !VCSUtils.isVcsEnableByUser(this) && showVcsItem) {
            //if current will show vcs item,and the vcs if disable by user.show disable icon.
            //make sure the vcs be stop.
            stopVoiceService();
            stopVoiceIndicator();
        }
        // / @}
        makeMenuItemVisible(menu, R.id.menu_import_export, showMiscOptions && showImportExportMenu());
        makeMenuItemVisible(menu, R.id.menu_accounts, showMiscOptions);
        makeMenuItemVisible(menu, R.id.menu_settings,
                showMiscOptions && !ContactsPreferenceActivity.isEmpty(this));
        /** M: New Feature xxx @{ */
        makeMenuItemVisible(menu, R.id.menu_share_visible_contacts, showMiscOptions);
        /** @} */
        PDebug.End("onPrepareOptionsMenu");
        return true;
    }

    /**
     * Returns whether there are any frequently contacted people being displayed
     * @return
     */
    private boolean hasFrequents() {
        if (PhoneCapabilityTester.isUsingTwoPanesInFavorites(this)) {
            return mFrequentFragment.hasFrequents();
        } else {
            return mFavoritesFragment.hasFrequents();
        }
    }

    private void makeMenuItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDisableOptionItemSelected) {
            return false;
        }

        switch (item.getItemId()) {
            case android.R.id.home: {
                // The home icon on the action bar is pressed
                if (mActionBarAdapter.isUpShowing()) {
                    // "UP" icon press -- should be treated as "back".
                    onBackPressed();
                }
                return true;
            }
            case R.id.menu_settings: {
                final Intent intent = new Intent(this, ContactsPreferenceActivity.class);
                // as there is only one section right now, make sure it is selected
                // on small screens, this also hides the section selector
                // Due to b/5045558, this code unfortunately only works properly on phones
                boolean settingsAreMultiPane = getResources().getBoolean(
                        com.android.internal.R.bool.preferences_prefer_dual_pane);
                if (!settingsAreMultiPane) {
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                            DisplayOptionsPreferenceFragment.class.getName());
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                            R.string.activity_title_settings);
                }
                startActivity(intent);
                return true;
            }
            case R.id.menu_contacts_filter: {
                AccountFilterUtil.startAccountFilterActivityForResult(
                        this, SUBACTIVITY_ACCOUNT_FILTER,
                        mContactListFilterController.getFilter());
                return true;
            }
            case R.id.menu_search: {
                onSearchRequested();
                return true;
            }
            case R.id.menu_add_contact: {
                final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                // On 2-pane UI, we can let the editor activity finish itself and return
                // to this activity to display the new contact.
                if (PhoneCapabilityTester.isUsingTwoPanes(this)) {
                    intent.putExtra(
                            ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED,
                            true);
                    startActivityForResult(intent, SUBACTIVITY_NEW_CONTACT);
                } else {
                    // Otherwise, on 1-pane UI, we need the editor to launch the view contact
                    // intent itself.
                    startActivity(intent);
                }
                return true;
            }
            case R.id.menu_add_group: {
            // / M: judge contactsapplication is busy or not? fixed cr ALPS00567939 & ALPS00542175 @{
            if (ContactsApplication.isContactsApplicationBusy()) {
                LogUtils.w(TAG, "[onOptionsItemSelected]contacts busy doing something");
                MtkToast.toast(PeopleActivity.this, R.string.phone_book_busy);
                return false;
            }
            // / @}
                createNewGroup();
                return true;
            }
            case R.id.menu_import_export: {
                /**
                 * Change Feature by Mediatek Begin. Original Android's Code:
                 * ImportExportDialogFragment.show(getFragmentManager());
                 */
                if (MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)) {
                    Toast.makeText(PeopleActivity.this, R.string.contact_delete_all_tips,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final Intent intent = new Intent(this, ContactImportExportActivity.class);
                startActivity(intent);
                /**
                 * Change Feature by Mediatek End.
                 */
                return true;
            }
            case R.id.menu_clear_frequents: {
                ClearFrequentsDialog.show(getFragmentManager());
                return true;
            }
            case R.id.menu_accounts: {
                final Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
                intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {
                    ContactsContract.AUTHORITY
                });
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                return true;
            }

                /**
                 * New Feature by Mediatek Inc Begin. Description: Add Multiple
                 * contacts to delete function
                 */
            case R.id.menu_delete_contact: {
                if (MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)) {
                    Toast.makeText(PeopleActivity.this, R.string.contact_delete_all_tips,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                startActivity(new Intent().setClassName(getApplicationContext(),
                        "com.mediatek.contacts.list.ContactListMultiChoiceActivity").setAction(
                        com.mediatek.contacts.util.ContactsIntent.LIST.ACTION_DELETE_MULTICONTACTS));
                return true;
            }
                /**
                 * New Feature by Mediatek Inc End.
                 */

                /**
                 * New Feature by Mediatek Inc Begin. Description: Add Multiple
                 * contacts to delete function
                 */
            case R.id.menu_share_visible_contacts: {
                startActivity(new Intent().setClassName(getApplicationContext(),
                        "com.mediatek.contacts.list.ContactListMultiChoiceActivity").setAction(
                        com.mediatek.contacts.util.ContactsIntent.LIST.ACTION_SHARE_MULTICONTACTS));
                return true;
            }
            
            case R.id.menu_vcs:
            boolean enableByUser = VCSUtils.isVcsEnableByUser(this);
            boolean enableByUserCurrent = !enableByUser;
            VCSUtils.setVcsEnableByUser(enableByUserCurrent, this);
            if (enableByUserCurrent) {
                startVoiceService();
            } else {
                stopVoiceService();
            }
                return true;
                /**
                 * New Feature by Mediatek Inc End.
                 */
        }
        return false;
    }

    private void createNewGroup() {
        final Intent intent = new Intent(this, GroupEditorActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
    }

    @Override
    public boolean onSearchRequested() { // Search key pressed.
        mActionBarAdapter.setSearchMode(true);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SUBACTIVITY_ACCOUNT_FILTER: {
                AccountFilterUtil.handleAccountFilterResult(
                        mContactListFilterController, resultCode, data);
                break;
            }

            case SUBACTIVITY_NEW_CONTACT:
            case SUBACTIVITY_EDIT_CONTACT: {
                if (resultCode == RESULT_OK && PhoneCapabilityTester.isUsingTwoPanes(this)) {
                    mRequest.setActionCode(ContactsRequest.ACTION_VIEW_CONTACT);
                    mAllFragment.setSelectionRequired(true);
                    mAllFragment.setSelectedContactUri(data.getData());
                    // Suppress IME if in search mode
                    if (mActionBarAdapter != null) {
                        mActionBarAdapter.clearFocusOnSearchView();
                    }
                    // No need to change the contact filter
                    mCurrentFilterIsValid = true;
                }
                break;
            }

            case SUBACTIVITY_NEW_GROUP:
            case SUBACTIVITY_EDIT_GROUP: {
                if (resultCode == RESULT_OK && PhoneCapabilityTester.isUsingTwoPanes(this)) {
                    mRequest.setActionCode(ContactsRequest.ACTION_GROUP);
                    mGroupsFragment.setSelectedUri(data.getData());
                }
                break;
            }

            // TODO: Using the new startActivityWithResultFromFragment API this should not be needed
            // anymore
            case ContactEntryListFragment.ACTIVITY_REQUEST_CODE_PICKER:
                if (resultCode == RESULT_OK) {
                    mAllFragment.onPickerResult(data);
                }

// TODO fix or remove multipicker code
//                else if (resultCode == RESULT_CANCELED && mMode == MODE_PICK_MULTIPLE_PHONES) {
//                    // Finish the activity if the sub activity was canceled as back key is used
//                    // to confirm user selection in MODE_PICK_MULTIPLE_PHONES.
//                    finish();
//                }
//                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO move to the fragment
        switch (keyCode) {
//            case KeyEvent.KEYCODE_CALL: {
//                if (callSelection()) {
//                    return true;
//                }
//                break;
//            }

            case KeyEvent.KEYCODE_DEL: {
                if (deleteSelection()) {
                    return true;
                }
                break;
            }
            default: {
                // Bring up the search UI if the user starts typing
                final int unicodeChar = event.getUnicodeChar();
                if ((unicodeChar != 0)
                        // If COMBINING_ACCENT is set, it's not a unicode character.
                        && ((unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0)
                        && !Character.isWhitespace(unicodeChar)) {
                    String query = new String(new int[]{ unicodeChar }, 0, 1);
                    if (!mActionBarAdapter.isSearchMode()) {
                        mActionBarAdapter.setQueryString(query);
                        mActionBarAdapter.setSearchMode(true);
                        return true;
                    }
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (mActionBarAdapter.isSearchMode()) {
            mActionBarAdapter.setSearchMode(false);
            /** M: New Feature xxx @{ */
        } else if (isTaskRoot()) {
            // Instead of stopping, simply push this to the back of the stack.
            // This is only done when running at the top of the stack;
            // otherwise, we have been launched by someone else so need to
            // allow the user to go back to the caller.
            moveTaskToBack(false);
            /** @} */
        } else {
            super.onBackPressed();
        }
    }

    private boolean deleteSelection() {
        // TODO move to the fragment
//        if (mActionCode == ContactsRequest.ACTION_DEFAULT) {
//            final int position = mListView.getSelectedItemPosition();
//            if (position != ListView.INVALID_POSITION) {
//                Uri contactUri = getContactUri(position);
//                if (contactUri != null) {
//                    doContactDelete(contactUri);
//                    return true;
//                }
//            }
//        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mActionBarAdapter.onSaveInstanceState(outState);
        if (mContactDetailLayoutController != null) {
            mContactDetailLayoutController.onSaveInstanceState(outState);
        }

        // Clear the listener to make sure we don't get callbacks after onSaveInstanceState,
        // in order to avoid doing fragment transactions after it.
        // TODO Figure out a better way to deal with the issue.
        mDisableOptionItemSelected = true;
        mActionBarAdapter.setListener(null);
        if (mTabPager != null) {
            mTabPager.setOnPageChangeListener(null);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // In our own lifecycle, the focus is saved and restore but later taken
        // away by the
        // ViewPager. As a hack, we force focus on the SearchView if we know
        // that we are searching.
        // This fixes the keyboard going away on screen rotation
        if (mActionBarAdapter.isSearchMode()) {
            mActionBarAdapter.setFocusOnSearchView();
        }
    }

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    // Visible for testing
    public ContactBrowseListFragment getListFragment() {
        return mAllFragment;
    }

    // Visible for testing
    public ContactDetailFragment getDetailFragment() {
        return mContactDetailFragment;
    }
    

    // The following lines are provided and maintained by Mediatek Inc.

    // New feature for SimIndicator begin


    private boolean mShowSimIndicator = false;

    private Contact mContactData;



    // New feature for SimIndicator end

    public static class AccountCategoryInfo implements Parcelable {

        public String mAccountCategory;
        public int mSlotId;
        public int mSimId;
        public String mSimName;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(mAccountCategory);
            out.writeInt(mSlotId);
            out.writeInt(mSimId);
            out.writeString(mSimName);
        }

        public static final Parcelable.Creator<AccountCategoryInfo> CREATOR = new Parcelable.Creator<AccountCategoryInfo>() {
            public AccountCategoryInfo createFromParcel(Parcel in) {
                return new AccountCategoryInfo(in);
            }

            public AccountCategoryInfo[] newArray(int size) {
                return new AccountCategoryInfo[size];
            }
        };

        private AccountCategoryInfo(Parcel in) {
            mAccountCategory = in.readString();
            mSlotId = in.readInt();
            mSimId = in.readInt();
            mSimName = in.readString();
        }

        public AccountCategoryInfo(String accountCategory, int slot, int simId, String simName) {
            mAccountCategory = accountCategory;
            mSlotId = slot;
            mSimId = simId;
            mSimName = simName;
        }
    }

    private void setupGroupDetailFragment(Uri groupUri, int slotId) {
        /** M: Bug Fix for ALPS00871315 @{ */
        // Tablet delete usim group fail. send simId to groupDetail for fix it.
        if (PhoneCapabilityTester.isUsingTwoPanesInFavorites(this)) {
            int simId = -1;
            if (slotId >= 0) {
                simId = SIMInfoWrapper.getDefault().getSimIdBySlotId(slotId);
            }
            mGroupDetailFragment.loadExtrasEx(simId);
        }
        /** @} */

        mGroupDetailFragment.loadExtras(slotId);
        mGroupDetailFragment.loadGroup(groupUri);
        invalidateOptionsMenuIfNeeded();
    }

    /* M: unused function
    private void createNewGroupWithAccountDisambiguation() {
        // final List<AccountWithDataSet> accounts =
        // AccountTypeManager.getInstance(this).getAccounts(true);
        // if (accounts.size() <= 1 || mAddGroupImageView == null) {
        // No account to choose or no control to anchor the popup-menu to
        // ==> just go straight to the editor which will disambig if necessary
        final Intent intent = new Intent(this, GroupEditorActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
        // return;
        // }

        // final ListPopupWindow popup = new ListPopupWindow(this, null);
        // popup.setWidth(getResources().getDimensionPixelSize(R.dimen.account_selector_popup_width));
        // popup.setAnchorView(mAddGroupImageView);
        // // Create a list adapter with all writeable accounts (assume that the
        // writeable accounts all
        // // allow group creation).
        // final AccountsListAdapter adapter = new AccountsListAdapter(this,
        // AccountListFilter.ACCOUNTS_GROUP_WRITABLE);
        // popup.setAdapter(adapter);
        // popup.setOnItemClickListener(new OnItemClickListener() {
        // @Override
        // public void onItemClick(AdapterView<?> parent, View view, int
        // position, long id) {
        // popup.dismiss();
        // int slotId = -1;
        // AccountWithDataSet account = (AccountWithDataSet)
        // adapter.getItem(position);
        // final Intent intent = new Intent(PeopleActivity.this,
        // GroupEditorActivity.class);
        // intent.setAction(Intent.ACTION_INSERT);
        // intent.putExtra(Intents.Insert.ACCOUNT, account);
        // intent.putExtra(Intents.Insert.DATA_SET, account.dataSet);
        //                
        // String accountType = account.type;
        // String accountName = account.name;
        // if(account instanceof AccountWithDataSetEx){
        // slotId = ((AccountWithDataSetEx) account).getSlotId();
        // Log.i(TAG, "[peopleActivity] slotId  +++++++"+slotId);
        // }
        // int simId = SIMInfoWrapper.getDefault().getSimIdBySlotId(slotId);
        // intent.putExtra("AccountCategory", new AccountCategoryInfo(
        // accountType, slotId, simId, accountName));
        // startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
        // }
        // });
        // popup.setModal(true);
        // popup.show();
    }
    */

    private int getAvailableStorageCount() {
        int storageCount = 0;
        final StorageManager sm = (StorageManager) getApplicationContext().getSystemService(
                STORAGE_SERVICE);
        if (null == sm) {
            return 0;
        }

        final IMountService mountService = IMountService.Stub.asInterface(ServiceManager
                .getService("mount"));

        try {
            StorageVolume volumes[] = sm.getVolumeList();
            if (volumes != null) {
                Log.d(TAG, "volumes are " + volumes);
                for (StorageVolume volume : volumes) {
                    Log.d(TAG, "volume is " + volume);
                    if (volume.getPath().startsWith(Environment.DIRECTORY_USBOTG)
                            || !Environment.MEDIA_MOUNTED.equals(mountService.getVolumeState(volume
                                    .getPath()))) {
                        continue;
                    }
                    storageCount++;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return storageCount;
    }

    private boolean showImportExportMenu() {
        return !((getAvailableStorageCount() == 0) && (AccountTypeManager.getInstance(this)
                .getAccounts(false).size() <= 1));
    }
    // The previous lines are provided and maintained by Mediatek Inc.

    //----------Voice Search Contacts Feature---------Begin------------------------//
    // some VCS class instances
    private VoiceSearchManager mVoiceSearchMgr;
    private VoiceSearchDialogFragment mVoiceSearchDialogFragment;
    private VoiceSearchResultLoader mVoiceSearchResultLoader;

    // constrain continuous trigger wave dialog
    private boolean isWaveListDlgDone = false;
    private boolean isWaveDialogExist = false;
    private int mContactsCount = 0;
    private Cursor mOptCursor;
    // used for contacts query
    private static final int MAX_NAME_COUNTS_TODISPLAY = 6;
    private static final int COLUMN_COUNTS_OF_CURSOR = 8;
    private static ArrayList<String> mAudioNameList = new ArrayList<String>();
    private static final String SUB_SELECTION = "display_name like ?";
    private static final String OR_SELECTION = " or ";
    // vcs icon section
    private MenuItem mVCSItem = null;
    private static final String KEY_DISABLE_VCS_BY_USER = "disable_vcs_by_user";
    private boolean mIsShowingGuide = false;
    // vcs msg seciton
    private static final int MSG_UPDATE_VCS_ICON = 100;
    private static final int MSG_UPDATE_VCS_STATUS = 200;
    private static final int MSG_START_VOICE_INDICATOR = 201;
    private static final int MSG_STOP_VOICE_INDICATOR = 202;
    private static final int TIME_CIRCLE_SHOULD_SHOW = VCSUtils.isAnimatorEnable() ? 350 : 0;

    private VoiceSearchIndicator mVoiceIndicator = null;

    //vcs methods
    public void onRegisterResult(boolean isRistered) {
        LogUtils.i(TAG, "[onRegisterResult] Register value:" + isRistered);
    }

    public void onCommandSericeConnected(){
        startVoiceService();
    }

    /**
     * M: get the current screen orientation.
     * @return
     */
    private int getScreenOrientation() {
        LogUtils.i(TAG, "[getScreenOrientation] [vcs]");
        return getApplicationContext().getResources().getConfiguration().orientation;
    }

    /**
     * M: process whether start Voice search or stop voice search.
     */
    private void updateVoiceSearStatus() {
        int tab = mActionBarAdapter.getCurrentTab();
        if (tab == TabState.ALL) {
            startVoiceService();
        } else {
            stopVoiceService();
        }
    }

    /**
     * try to start the voice search.If the conditions if not fill,it will stop.
     * @return true if start the voice search,false else.
     */
    private boolean startVoiceService() {
        if (mVCSItem == null) {
            LogUtils.w(TAG, "[startVoiceService] the mVCSItem is null..");
            return false;
        }

        //do some check when start Voice Search
        if(!checkStartVcsCondition()){
            LogUtils.w(TAG, "[startVoiceService] current can't start vcs.");
            stopVoiceService();
            return false;
        }

        if(mVoiceIndicator.isIndicatorEnable()){
            LogUtils.i(TAG, "[startVoiceSearch] The Voice Search Has been started,Ignore..");
            return true;
        }
        LogUtils.i(TAG, "[startVoiceService] [vcs]");
        // activate voice icon
        int orientation = getScreenOrientation();
        mVoiceSearchMgr.startVoiceSearch(orientation);
        return true;
    }
    
    private boolean startVoiceIndicator() {
        // do some check when start Voice Search
        if (!checkStartVcsCondition()) {
            LogUtils.w(TAG, "[startVoiceIndicator]current can't show vcs...");
            stopVoiceService();
            return false;
        }

        if (mVCSItem == null) {
            LogUtils.w(TAG, "[startVoiceIndicator] the mVCSItem is null..");
        }

        if (mVoiceIndicator.isIndicatorEnable() && mVCSItem.isVisible()) {
            LogUtils.i(TAG, "[startVoiceIndicator] The Voice Search Has been started,Ignore..");
            return true;
        }
        if(isWaveDlgExist()){
            mVoiceIndicator.updateIndicator(false);
            return true;
        }
        mVoiceIndicator.updateIndicator(true);
        return true;
    }

    /**
     * 
     * @return true if can start vcs,false else;
     */
    private boolean checkStartVcsCondition() {
        int tab = mActionBarAdapter == null ? TabState.DEFAULT : mActionBarAdapter.getCurrentTab();
        boolean isSearchMode = mActionBarAdapter == null ? false : mActionBarAdapter.isSearchMode();

        if ((!mIsShowingGuide) && isResumed() && (!isSearchMode) && (tab == TabState.ALL) && mContactsCount > 0 && VCSUtils.isVcsEnableByUser(this)) {
            return true;
        }
        return false;
    }

    private void stopVoiceService() {
        if (mVCSItem == null) {
            LogUtils.w(TAG, "[stopVoiceService] the mVCSItem is null..");
        }
        mVCSHandler.removeMessages(MSG_UPDATE_VCS_STATUS);
        int orientation = getScreenOrientation();
        mVoiceSearchMgr.onSetScreenOrientation(orientation);
        mVoiceSearchMgr.stopVoiceSearch();
    }

    private void stopVoiceIndicator() {
        if (mVCSItem == null) {
            LogUtils.w(TAG, "[stopVoiceIndicator] the mVCSItem is null..");
        }
        if (mVoiceIndicator == null) {
            LogUtils.w(TAG, "[stopVoiceIndicator] the mVoiceIndicator is null..");
            return;
        }
        mVoiceIndicator.updateIndicator(false);
    }

    /**
     * M:trigger the voice search wave dialog
     */
    public void onTrigVoiceSearchDlg() {
        LogUtils.d(TAG, "[onTrigVoiceSearchDlg][vcs]--------------------------------------------");
        if (!isResumed()) {
            LogUtils.w(TAG, "[onTrigVoiceSearchDlg] Activity is not in Resumed,ignore... ");
            return;
        }

        if (!isWaveDlgExist() && (mContactsCount > 0) && (mActionBarAdapter.getCurrentTab() == TabState.ALL)) {
            LogUtils.d(TAG, "[onTrigVoiceSearchDlg][vcs] need show wave dialog");
            // remove or dismiss dialog fragment ahead of time
            FragmentManager fManager = this.getFragmentManager();
            dismissDlgFrag(fManager);
            // show dialog
            setWaveDlgFlag();
            resetFreshDone();
            mVoiceSearchDialogFragment.show(fManager, "VoiceSearchDialog");
        } else {
            LogUtils.w(TAG, "[onTrigVoiceSearchDlg] not show Dialog,mContactsCount:" + mContactsCount + ",tab:"
                    + mActionBarAdapter.getCurrentTab() + ",isWaveDlgExist:" + isWaveDlgExist());
        }
    }

    private void setWaveDlgFlag() {
        isWaveDialogExist = true;
    }

    private void resetWaveDlgFlag() {
        isWaveDialogExist = false;
    }

    private boolean isWaveDlgExist() {
        return isWaveDialogExist;
    }

    /**
     * M: Used to dismiss the dialog floating on.
     * @param v
     */
    @SuppressWarnings( { "UnusedDeclaration" })
    public void onClickDialog(View v) {
        LogUtils.d(TAG, "[onClickDialog][vcs] view id:" + v.getId());
        FragmentManager fManager = this.getFragmentManager();
        dismissDlgFrag(fManager);
    }

    /**
     * M: to dismiss the voice search process or results list dialog
     * 
     * @param fManager
     */
    public void dismissDlgFrag(FragmentManager fManager) {
        LogUtils.d(TAG, "[dismissDlgFrag][vcs]");
        if (mVoiceSearchDialogFragment.isAdded()) {
            resetWaveDlgFlag();
            mVoiceSearchDialogFragment.dismiss();
            Message msg = Message.obtain();
            msg.what = MSG_UPDATE_VCS_STATUS;
            mVCSHandler.sendMessage(msg);
        }
    }

    /// M: for VCS new feature, VCS icon management seciton. @{
    public Handler mVCSHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

            case MSG_UPDATE_VCS_STATUS:
                if (VCSUtils.isVCSFeatureEnabled()) {
                    updateVoiceSearStatus();
                }
                break;
                
            case MSG_START_VOICE_INDICATOR:
                if (VCSUtils.isVCSFeatureEnabled()) {
                    startVoiceIndicator();
                    if(mVoiceSearchDialogFragment != null){
                        mVoiceSearchDialogFragment.updateVcsIndicator(true);
                    }
                }
                break;
                
            case MSG_STOP_VOICE_INDICATOR:
                if (VCSUtils.isVCSFeatureEnabled()) {
                    stopVoiceIndicator();
                    if(mVoiceSearchDialogFragment != null){
                        mVoiceSearchDialogFragment.updateVcsIndicator(false);
                    }
                }
                break;

            default:
                LogUtils.i(TAG, "[handleMessage] [vcs] default message.");
                break;
            }
        }
    };
    /// @}

    /**
     * M: default contacts list load finished, not to activate VCS if no contacts in database.
     */
    public void onShowContactsCount(final int count) {
        if (!VCSUtils.isVCSFeatureEnabled()) {
            LogUtils.i(TAG, "[onLoadFinished] [vcs] not support VCS.");
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mContactsCount = count;
                if (count <= 0) {
                    dismissDlgFrag(getFragmentManager());
                    stopVoiceService();
                } else if (!isWaveDlgExist()) {
                    startVoiceService();
                }
            }
        });
        return;
    }

    // /M: get loader mgr for voice search manager. @{
    public LoaderManager onGetLoaderManager() {
        return getLoaderManager();
    }

    @Override
    public LoaderManager getLoaderManager() {
        return super.getLoaderManager();
    }
    // / @}

    public void onNoContactFoundInVoiceBank() {
        LogUtils.d(TAG, "[onNoContactFound][vcs]");
        if(isFreshDone()){
            LogUtils.w(TAG, "[onNoContactFoundInVoiceBank] Result List still show,don't refresh..");
            return;
        }
        showNoContactDlg();
    }

    private void showNoContactDlg() {
        LogUtils.d(TAG, "[showNoContactDlg][vcs]");
        // trigger no contacts found dialog
        MtkToast.toast(getApplicationContext(), "There is no contacts who you want!");
    }

    public void onNoSuchContactsInDatabase() {
        LogUtils.d(TAG, "[onNoSuchContactsInDatabase][vcs]");
        showNoContactDlg();
    }

    /**
     * M:optimize query result and set query ordered results to dialog
     * @deprecated
     */
    public void onOptimizeResultDone(Cursor optCursor) {
        LogUtils.d(TAG, "[onOptimizeResultDone][vcs]");
        mOptCursor = optCursor;
        if (isFreshDone()) {
            resetFreshDone();
            refreshDialogList();
        }
    }

    /**
     * M: To refresh contacts list for Voice search dialog by optimized contact cursor.
     * @param optCursor
     *
     */
    public void onRefreshContactsList(Cursor optCursor) {
        LogUtils.d(TAG, "[onRefreshContactsList][vcs]");
        mOptCursor = optCursor;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshDialogDone();
            }
        }, TIME_CIRCLE_SHOULD_SHOW);
    }

    private void refreshDialogDone() {
        if (mOptCursor == null) {
            LogUtils.w(TAG, "[refreshDialogDone][vcs] mOptCursor cursor is null");
        }
        int count = mOptCursor == null ? -1 : mOptCursor.getCount();
        LogUtils.d(TAG, "[refreshDialogDone][vcs] cursor counts:" + count);
        
        Dialog dialog = mVoiceSearchDialogFragment.getDialog();
        if (dialog != null && dialog.isShowing()) {
            mVoiceSearchDialogFragment.searchDone(mOptCursor);
        } else {
            LogUtils.w(TAG, "[refreshDialogDone] Dialog is not showing..dialog:" + dialog);
        }
        
    }

    /**
     * @deprecated
     */
    public void refreshDialogList() {
        if (mOptCursor == null) {
            LogUtils.d(TAG, "[refreshDialogList][vcs] static cursor is null");
            return;
        }
        LogUtils.d(TAG, "[refreshDialogList][vcs] cursor counts:" + mOptCursor.getCount());
        mVoiceSearchDialogFragment.refreshContactsList(mOptCursor);
    }

    private void setFreshDone() {
        LogUtils.d(TAG, "[setFreshDone][vcs]");
        isWaveListDlgDone = true;
    }

    private boolean isFreshDone() {
        LogUtils.d(TAG, "[isFreshDone][vcs] isRefreshDone" + isWaveListDlgDone);
        return isWaveListDlgDone;
    }

    private void resetFreshDone() {
        LogUtils.d(TAG, "[resetFreshDone][vcs]");
        isWaveListDlgDone = false;
    }

    /**
     * call back from VoiceSearchDialogFragment to setFreshDone flag.
     */
    public void refreshDone() {
        LogUtils.d(TAG, "[refreshDone][vcs]");
        setFreshDone();
    }

    /**
     * call back from VoiceSearchDialogFragment to setWaveDlg flag when back press the wave dialog.
     */
    public void onCancel() {
        LogUtils.d(TAG, "[onCancel][vcs]");
        resetWaveDlgFlag();
        startVoiceService();
    }

    /**
     * M: callback to contact detail by its Uri
     */
    public void contactsRowOnClick(Uri uri, String contactName) {
        LogUtils.d(TAG, "[contactsRowOnClick][vcs] uri:"+uri);
        //dismiss dialog
        FragmentManager fManager = this.getFragmentManager();
        dismissDlgFrag(fManager);
        //show contact detail info when clicking the item
        mAllFragment.viewContact(uri);
        // to learn user selected result
        mVoiceSearchMgr.onContactLearning(contactName);
    }

    /**
     * M: used to query these contacts info by voice bank returned contacts list
     */
    public void onQueryContactsInfo(final ArrayList<String> audioNameList) {
        final CursorLoader loader = new CursorLoader(getApplicationContext());
        new AsyncTask<ArrayList<String>, Void, Cursor>() {
            // query projection
            final String[] CONTACT_PROJECTION = new String[] { Contacts._ID, // 0
                    Contacts.DISPLAY_NAME_PRIMARY, // 1
                    Contacts.CONTACT_PRESENCE, // 2
                    Contacts.CONTACT_STATUS, // 3
                    Contacts.PHOTO_ID, // 4
                    Contacts.PHOTO_URI, // 5
                    Contacts.PHOTO_THUMBNAIL_URI, // 6
                    Contacts.LOOKUP_KEY // 7
            };

            @Override
            public void onPreExecute() {
                stopVoiceService();
                LogUtils.i(TAG,"[vcs][performance],onQueryContactsInfo start,time:"+System.currentTimeMillis());
            }

            @Override
            public Cursor doInBackground(ArrayList<String>... names) {
                if (names[0] == null || names[0].size() <= 0) {
                    Log.w(TAG, "doInBackground,audioNameList is empty:" + audioNameList);
                    return null;
                }
                LogUtils.i(TAG, "[onQueryContactsInfo] [vcs] audioNameList size:" + names[0].size());
                mAudioNameList = names[0];
                int nameListSize = mAudioNameList.size();
                StringBuffer sbToLog = new StringBuffer();

                // 1.make name filter selection
                StringBuffer selection = new StringBuffer();
                ArrayList<String> selectionArgs = new ArrayList<String>();
                selection.append("(");
                for (int i = 0; i < nameListSize; i++) {
                    selection.append("display_name like ? or ");
                    selectionArgs.add("%" + audioNameList.get(i) + "%");
                    sbToLog.append(audioNameList.get(i)+",");
                }
                //1==1 to handle nameListSize is null or empty
                selection.append("1=1) ");

                // 2.make account filter selection
                String accountFilter = "1=1";
                if (mAllFragment.getAdapter() == null) {
                    Log.w(TAG, "doInBackground,adapter is null");
                    return null;
                }
                if (mAllFragment.getAdapter() instanceof com.android.contacts.common.list.DefaultContactListAdapter) {
                    mAllFragment.getAdapter().configureLoader(loader, Directory.DEFAULT);
                    accountFilter = loader.getSelection();
                }
                selection.append("and (" + accountFilter + ")");

                // 3.make selection args
                final ContentResolver resolver = getContentResolver();
                Uri uri = loader.getUri();
                String[] args = loader.getSelectionArgs();
                if (args != null) {
                    for (String s : args) {
                        selectionArgs.add(s);
                        sbToLog.append(s+",");
                    }
                }
                LogUtils.d(TAG, "[onQueryContactsInfo] uri:" + uri + ",selects:" + selection + ":args:" + sbToLog.toString());

                // 4.query contacts DB
                LogUtils.i(TAG, "[vcs][performance],start query ContactsProvider,time:" + System.currentTimeMillis());
                Cursor originalCursor = resolver.query(uri, CONTACT_PROJECTION, selection.toString(),
                        selectionArgs.toArray(new String[0]), "sort_key");
                LogUtils.i(TAG, "[vcs][performance],end query ContactsProvider,time:" + System.currentTimeMillis());

                // 5.order the result
                if (originalCursor == null) {
                    LogUtils.w(TAG, "[onQueryContactsInfo] cusur is null.");
                    return null;
                }
                LogUtils.i(TAG, "[onQueryContactsInfo] [vcs] originalCursor counts:" + originalCursor.getCount());
                Cursor cursor = makeOrderedCursor(originalCursor);
                return cursor;
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                LogUtils.i(TAG,"[vcs][performance],onQueryContactsInfo end,time:"+System.currentTimeMillis());
                // call back trigger search done method.
                onRefreshContactsList(cursor);
            }

        }.execute(audioNameList);
    }

    /**
     * M: construct a new sorted cursor as audioNameList order by originalCursor.
     */
    private Cursor makeOrderedCursor(Cursor originalCursor) {
        LogUtils.i(TAG, "[makeOrderedCursor] [vcs] order the cursor,mAudioNameList.size:"+mAudioNameList.size());
        String preAudioItemName = new String();
        String currAudioItemName = new String();
        String cursorItemName = new String();
        int itemCounts = 0;
        MatrixCursor audioOrderedCursor = new MatrixCursor(originalCursor.getColumnNames());
        for (int i = 0; i < mAudioNameList.size(); i++) {
            currAudioItemName = mAudioNameList.get(i);
            if (currAudioItemName.equals(preAudioItemName)) {
                LogUtils.i(TAG,
                        "[makeOrderedCursor] [vcs] currAudioItemName is equal to preAudioItemName, skip to next item");
                continue;
            }

            while (originalCursor.moveToNext()) {
                cursorItemName = originalCursor.getString(originalCursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY));
                if (currAudioItemName.equals(cursorItemName)) {
                    String[] columnValArray = new String[COLUMN_COUNTS_OF_CURSOR];
                    columnValArray[0] = String.valueOf(originalCursor.getLong(originalCursor
                            .getColumnIndex(Contacts._ID)));
                    columnValArray[1] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY));
                    columnValArray[2] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.CONTACT_PRESENCE));
                    columnValArray[3] = originalCursor
                            .getString(originalCursor.getColumnIndex(Contacts.CONTACT_STATUS));
                    columnValArray[4] = originalCursor.getString(originalCursor.getColumnIndex(Contacts.PHOTO_ID));
                    columnValArray[5] = originalCursor.getString(originalCursor.getColumnIndex(Contacts.PHOTO_URI));
                    columnValArray[6] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI));
                    columnValArray[7] = originalCursor.getString(originalCursor.getColumnIndex(Contacts.LOOKUP_KEY));
                    try {
                        itemCounts++;
                        if (itemCounts > MAX_NAME_COUNTS_TODISPLAY) {
                            LogUtils.i(TAG, "[makeOrderedCursor] [vcs] mounts to max list counts!");
                            break;
                        }
                        audioOrderedCursor.addRow(columnValArray);
                        // backup for debug
                        /*
                         * LogUtils.i(TAG,
                         * "[makeOrderedCursor] [vcs] ordered cursor. id:" +
                         * columnValArray[0] + " display_name:" +
                         * columnValArray[1] + " contact_presence:" +
                         * columnValArray[2] + " contact_status:" +
                         * columnValArray[3] + " photo_id:" + columnValArray[4]
                         * + " photo_uri:" + columnValArray[5] +
                         * " photo_thumb_uri:" + columnValArray[6] + " lookup:"
                         * + columnValArray[7]);
                         */
                    } catch (Exception e) {
                        // TODO: handle exception
                        LogUtils.i(TAG, "[makeOrderedCursor] [vcs] columnValStrings.length!=columnnames.length");
                    }
                }
            }
            // back to original position for ordering next time
            originalCursor.moveToPosition(-1);
            // set previous audio name item
            preAudioItemName = currAudioItemName;
        }
        //close the cursor
        if (originalCursor != null) {
            originalCursor.close();
            originalCursor = null;
        }

        LogUtils.i(TAG, "[makeOrderedCursor] [vcs] orderedCursor counts:" + audioOrderedCursor.getCount());

        audioOrderedCursor.moveToPosition(-1);
        return audioOrderedCursor;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (VCSUtils.isVCSFeatureEnabled()) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_CANCEL) {
                if (mActionBarAdapter.getCurrentTab() == TabState.ALL) {
                    mVCSHandler.removeMessages(MSG_UPDATE_VCS_STATUS);
                    stopVoiceService();
                }
            } else if (action == MotionEvent.ACTION_UP && !isWaveDlgExist()) {
                if (mActionBarAdapter.getCurrentTab() == TabState.ALL) {
                    Message msg = Message.obtain();
                    msg.what = MSG_UPDATE_VCS_STATUS;
                    mVCSHandler.sendMessageDelayed(msg, 400);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }


    OnGuideFinishListener mGuideFinishListener = new OnGuideFinishListener() {
        @Override
        public void onGuideFinish() {
            if(VCSUtils.isVCSFeatureEnabled()){
                mIsShowingGuide = false;
                startVoiceService();
            }
        }
    };

    @Override
    public void onCommandSericeStop() {
        Message msg = Message.obtain();
        msg.what = MSG_STOP_VOICE_INDICATOR;
        mVCSHandler.sendMessage(msg);
    }

    @Override
    public void onCommandSericeStart() {
        Message msg = Message.obtain();
        msg.what = MSG_START_VOICE_INDICATOR;
        mVCSHandler.sendMessage(msg);
    }
    
    @Override
    public boolean onSearchPanelClick() {
        startVoiceService();
        return true;
    }
  //----------Voice Search Contacts Feature---------End------------------------//
}
