package com.mediatek.email.ui;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.app.ActionBar.OnNavigationListener;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SpinnerAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.android.email.R;
import com.android.emailcommon.service.SearchParams;
import com.android.emailcommon.utility.Utility;
import com.android.mail.ConversationListContext;
import com.android.mail.providers.UIProvider.AccountCapabilities;
import com.android.mail.ui.MailActionBarView;
import com.android.mail.ui.ViewMode;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Throttle;
import com.android.mail.utils.Utils;

/**
 * M: Base on MailActionBarView, add local search related feature.
 */
public class EmailActionBarView extends MailActionBarView {
    //private static final String BUNDLE_KEY_ACTION_BAR_SELECTED_FIELD = "ActionBarController.ACTION_BAR_SELECTED_TAB";
    private TabListener mTabListener = new TabListener();
    private String mSearchField;
    private static final String[] SEARCH_FIELD_LIST = { SearchParams.SEARCH_FIELD_ALL, SearchParams.SEARCH_FIELD_FROM,
            SearchParams.SEARCH_FIELD_TO, SearchParams.SEARCH_FIELD_SUBJECT, SearchParams.SEARCH_FIELD_BODY };
    private static final int INITIAL_FIELD_INDEX = 1;

    private int mLocalSearchResult = 0;
    private TextView mSearchResultCountView;
    private TextView mSearchFiledSpinner;
    private SearchFieldDropdownPopup mSearchFiledDropDown;
    // Indicated user was opening searched conversation, we don't need exit local search mode.
    private boolean mOpeningLocalSearchConversation = false;
    // Indicated user was back from searched conversation, we need restore query text.
    private boolean mBackingLocalSearchConversation = false;

    /// M: expandSearch might be called eariler than onCreateOptionsMenu(),
     // in this case, execute this expanding request after creating the search UI
    private String mPendingQuery;
    ///M: the flag is set to true if the localsearch execute but the search bar has not expand.
    private boolean mHasPendingQuery = false;

    public EmailActionBarView(Context context) {
        this(context, null);
    }

    public EmailActionBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmailActionBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If the mode is valid, then set the initial menu
        if (mMode == ViewMode.UNKNOWN) {
            return false;
        }
        mSearch = menu.findItem(R.id.search);
        if (mSearch != null) {
            mSearch.setActionView(R.layout.local_search_actionbar_view);
            mSearchResultCountView = (TextView) mSearch.getActionView().findViewById(R.id.result_count);
            mSearchResultCountView.setText(String.valueOf(0));
            mSearchFiledSpinner = (TextView) mSearch.getActionView().findViewById(R.id.search_field);
            mSearchWidget = (SearchView) mSearch.getActionView().findViewById(R.id.email_search_view);
            mSearch.setOnActionExpandListener(this);
            mSearch.setOnMenuItemClickListener(new OnSearchItemClickListener());
            if (mSearchWidget != null) {
                mSearchWidget.setOnQueryTextListener(this);
                mSearchWidget.setOnSuggestionListener(this);
                mSearchWidget.setIconifiedByDefault(true);
                mSearchWidget.setQueryHint(getContext().getResources().getString(R.string.search_hint));
            }
        }

        /// M: There's a pending expanding search request
        if (mHasPendingQuery) {
            expandSearch(mPendingQuery, mSearchField);
            mPendingQuery = null;
            mHasPendingQuery = false;
        }

        mHelpItem = menu.findItem(R.id.help_info_menu_item);
        mSendFeedbackItem = menu.findItem(R.id.feedback_menu_item);
        mRefreshItem = menu.findItem(R.id.refresh);
        mFolderSettingsItem = menu.findItem(R.id.folder_options);
        mEmptyTrashItem = menu.findItem(R.id.empty_trash);
        mEmptySpamItem = menu.findItem(R.id.empty_spam);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);
        // always enable search menu for local searching.
        if (!mController.shouldHideMenuItems()) {
            Utils.setMenuItemVisibility(menu, R.id.search, true);
        }
        // M: if has been local searching(resume activity/pull out drawer), init view content.
        // add mSearch null check, sometimes(in localsearch conversationview mode) the mSearch
        // will be null if we change to landscape. @{
        ConversationListContext currentListContext = mController.getCurrentListContext();
        if (mSearch != null && currentListContext != null
                && currentListContext.isLocalSearch()) {
            mSearchResultCountView.setText(String.valueOf(mLocalSearchResult));
            if (useListMode(getContext())) {
                initSpinner();
            }
        }
        /// @}

        /// M: restore local search state, it's a little strange, however, the menu item don't expand
        /// when back form conversation view. @{
        if (mBackingLocalSearchConversation) {
            if (currentListContext != null) {
                //Reset local search status.
                LogUtils.logFeature(LogTag.SEARCH_TAG,
                        "onPrepareOptionsMenu reset localsearch, currentListContext [%s]", currentListContext);
                expandSearch(currentListContext.getSearchQuery(), currentListContext.getSearchField());
            }
            mBackingLocalSearchConversation = false;
        }
        /// @}
        return result;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // if back from conversation view, don't re-query empty term in conversations list.
        if (mBackingLocalSearchConversation
                && TextUtils.isEmpty(newText)) {
            return true;
        }
        /**
         * Not start local search immediately, use Throttle control the query event.
         */
        LogUtils.d(LogTag.SEARCH_TAG, "onQueryTextChange [%s]", newText);
        mLocalSearchThrottle.onEvent();
        return true;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        boolean result = super.onMenuItemActionExpand(item);
        String listContextQuery = null;
        // backup the list context query, cause onActionViewExpanded would clear query text.
        ConversationListContext listContext = mController.getCurrentListContext();
        if (listContext != null && listContext.isLocalSearch()) {
            listContextQuery = listContext.getSearchQuery();
        }
        mSearchWidget.onActionViewExpanded();
        mSearchWidget.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_NONE);
        if (listContextQuery != null) {
            mSearchWidget.setQuery(listContextQuery, false);
        }
        return result;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (!mOpeningLocalSearchConversation) {
            mController.exitLocalSearch();
            // call SearchView's collapsed api to clear focus and query text.
            mSearchWidget.onActionViewCollapsed();
            //M: Manual clear the query text, make sure the query text cleared.
            mSearchWidget.setQuery(null, false);
        }
        removeLocalSearchView();
        return super.onMenuItemActionCollapse(item);
    }

    /*
     * M: Don't exit local search mode, if open message in local search results list.
     * @see com.android.mail.ui.MailActionBarView#onViewModeChanged(int)
     */
    @Override
    public void onViewModeChanged(int newMode) {
        ConversationListContext listContext = mController.getCurrentListContext();
        if (listContext != null && listContext.isLocalSearch()
                && mMode == ViewMode.CONVERSATION_LIST && newMode == ViewMode.CONVERSATION) {
            mOpeningLocalSearchConversation = true;
            mBackingLocalSearchConversation = false;
            clearSearchFocus();
        }
        if (listContext != null && listContext.isLocalSearch()
                && mMode == ViewMode.CONVERSATION && newMode == ViewMode.CONVERSATION_LIST) {
            mOpeningLocalSearchConversation = false;
            mBackingLocalSearchConversation = true;
        }
        super.onViewModeChanged(newMode);
    }

    /**
     * Remove focus from the search field to avoid 1. The keyboard popping in and out. 2. The search suggestions shown
     * up.
     */
    private void clearSearchFocus() {
        // Remove focus from the search action menu in search results mode so
        // the IME and the suggestions don't get in the way.
        mSearchWidget.clearFocus();
    }

    /**
     * Expand the local search UI and query the text
     */
    @Override
    public void expandSearch(String query, String field) {
        // M: support expand search for specific field
        if (field != null) {
            mSearchField = field;
        } else {
            mSearchField = SEARCH_FIELD_LIST[INITIAL_FIELD_INDEX];
        }
        if (mSearch != null) {
            mController.enterLocalSearch(mSearchField);
            initLocalSearchView();
            super.expandSearch();
            mSearchWidget.setQuery(query, false);
        } else {
            mPendingQuery = query;
            ///M: after search actionbar expand, the pending search will be execute
            // and then this flag will reset.
            mHasPendingQuery = true;
        }
    }

    /**
     * M: initialize the tab-styled local search UI
     */
    private void initTabs() {
        Context context = mActivity.getApplicationContext();
        // backup current field, cause addTab would change mSearchField;
        String currentField = mSearchField;
        String[] searchFieldList = context.getResources().getStringArray(R.array.search_field_list);
        for (int i = 0; i < 5; i++) {
            Tab tab = mActionBar.newTab().setText(searchFieldList[i]).setTabListener(mTabListener);
            tab.setTag(SEARCH_FIELD_LIST[i]);
            mActionBar.addTab(tab);
        }

        for (int i = 0; i < mActionBar.getTabCount(); i++) {
            String field = (String) mActionBar.getTabAt(i).getTag();
            if (field != null && field.equals(currentField)) {
                mActionBar.selectTab(mActionBar.getTabAt(i));
                break;
            }
        }
        mSearchField = currentField;
    }

    /**
     * M: initialize the dropdownlist-style local search UI(for tablet)
     */
    private void initSpinner() {
        ArrayList<String> items = new ArrayList<String>();
        Context context = mActivity.getApplicationContext();
        String[] searchFieldList = context.getResources().getStringArray(R.array.search_field_list);
        for (String field : searchFieldList) {
            items.add(field);
        }

        ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.search_fields_spinner, items);

        // field dropdown
        mSearchFiledSpinner.setVisibility(View.VISIBLE);
        mSearchFiledDropDown = new SearchFieldDropdownPopup(getContext(), mSearchFiledSpinner);
        mSearchFiledDropDown.setAdapter(adapter);

        mSearchFiledSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchFiledDropDown.show();
            }
        });
        for (int i = 0; i < SEARCH_FIELD_LIST.length; i++) {
            if (SEARCH_FIELD_LIST[i].equals(mSearchField)) {
                mSearchFiledSpinner.setText(items.get(i));
                break;
            }
        }
    }

    /**
     * M: record current selected tab.
     */
    private class TabListener implements ActionBar.TabListener {
        /* The following are each of the ActionBar.TabListener callbacks */
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mSearchField = (String) tab.getTag();
            mController.enterLocalSearch(mSearchField);
            String query = mSearchWidget.getQuery().toString();
            if (!TextUtils.isEmpty(query)) {
                onQueryTextChange(query);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // User selected the already selected tab. Usually do nothing.
        }
    }

    private boolean onPopupFieldsItemSelected(int itemPosition, View v) {
        String searchField = SEARCH_FIELD_LIST[itemPosition];
        mSearchFiledSpinner.setText(((TextView) v).getText());
        mSearchField = searchField;
        mController.enterLocalSearch(mSearchField);
        onQueryTextChange(mSearchWidget.getQuery().toString());
        return true;
    }

    // Based on Spinner.DropdownPopup
    private class SearchFieldDropdownPopup extends ListPopupWindow {
        public SearchFieldDropdownPopup(Context context, View anchor) {
            super(context);
            setAnchorView(anchor);
            setModal(true);
            setPromptPosition(POSITION_PROMPT_ABOVE);
            setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    onPopupFieldsItemSelected(position, v);
                    dismiss();
                }
            });
        }

        @Override
        public void show() {
            setWidth(getContext().getResources().getDimensionPixelSize(R.dimen.search_fields_popup_width));
            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
            super.show();
            // List view is instantiated in super.show(), so we need to do this after...
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    /**
     * M: call to update search result count.
     */
    public void updateSearchCount(int count) {
        mLocalSearchResult = count;
        mSearchResultCountView.setText(String.valueOf(mLocalSearchResult));
    }

    /**
     * M: get the query term if current search field were "body" or "all", otherwise returns null
     */
    public String getQueryTermIfSearchBody() {
        String selectedTab;
        if (!TextUtils.isEmpty(mSearchField)) {
            selectedTab = mSearchField;
        } else {
            return null;
        }

        return (selectedTab.equalsIgnoreCase(SearchParams.SEARCH_FIELD_BODY) || selectedTab
                .equalsIgnoreCase(SearchParams.SEARCH_FIELD_ALL)) ? mSearchWidget.getQuery().toString() : null;
    }

    private boolean useListMode(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void initLocalSearchView() {
        if (!useListMode(mActivity.getApplicationContext())) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            initTabs();
        } else {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            initSpinner();
        }
    }

    private void removeLocalSearchView() {
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mActionBar.removeAllTabs();
        mSearchFiledSpinner.setVisibility(View.GONE);
    }

    private class OnSearchItemClickListener implements OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            mSearchField = SEARCH_FIELD_LIST[INITIAL_FIELD_INDEX];
            updateSearchCount(0);
            mController.enterLocalSearch(mSearchField);
            initLocalSearchView();
            return true;
        }
    }

    /**
     * Use throttle to avoid throw too many query, when user keep input or delete query.
     * same to a delay when query changed.
     */
    private static final int MIN_QUERY_INTERVAL = 200;
    private static final int MAX_QUERY_INTERVAL = 500;

    private final Throttle mLocalSearchThrottle = new Throttle("EmailActionBarView",
            new Runnable() {
                @Override public void run() {
                    if (null != mController && null != mSearchWidget
                            && null != mSearchWidget.getQuery()) {
                        mController.executeLocalSearch(mSearchWidget.getQuery().toString());
                    }
                }
            }, Utility.getMainThreadHandler(),
            MIN_QUERY_INTERVAL, MAX_QUERY_INTERVAL);
}
