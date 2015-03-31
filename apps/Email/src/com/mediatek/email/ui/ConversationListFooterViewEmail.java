package com.mediatek.email.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.android.email.R;
import com.android.mail.ConversationListContext;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.browse.ConversationListFooterView;
import com.android.mail.providers.Account;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AccountCapabilities;
import com.android.mail.providers.UIProvider.FolderCapabilities;
import com.android.mail.ui.ActivityController;
import com.android.mail.ui.ControllableActivity;
import com.android.mail.utils.Utils;
import com.mediatek.mail.utils.Utility;

/**
 * M: Override ConversationListFooterView, add remote search view.
 */
public class ConversationListFooterViewEmail extends ConversationListFooterView {
    /// M: add for local search feature.
    private View mRemoteSearch;

    public ConversationListFooterViewEmail(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRemoteSearch = (View)findViewById(R.id.remote_search);
        mRemoteSearch.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        final int id = v.getId();
        final Folder f = (Folder) v.getTag();
        if (id == R.id.remote_search) {
            mClickListener.onFooterViewRemoteSearchClick(f);
        }
    }

    /**
     * Update the view to reflect the new folder status.
     */
    @Override
    public boolean updateStatus(final ConversationCursor cursor) {
        ControllableActivity activity = (ControllableActivity)mClickListener;
        ActivityController activityController = (ActivityController) activity.getAccountController();
        ConversationListContext listContext = activityController.getCurrentListContext();
        /// M: get the current account.
        Account account = listContext.account;
        /// M: check Connectivity. Adjust mFolder is null to avoid NPE. @{
        if (!Utility.hasConnectivity(getContext()) && mFolder != null
                && !mFolder.isType(UIProvider.FolderType.DRAFT)
                && !mFolder.isType(UIProvider.FolderType.OUTBOX)
                && !mFolder.supportsCapability(FolderCapabilities.IS_VIRTUAL)
                /// M: if it is local/remote search, not show "no connection and retry". @{
                && !(listContext.isLocalSearchExecuted())
                && !(ConversationListContext.isSearchResult(listContext))) {
            mErrorStatus = UIProvider.LastSyncResult.CONNECTION_ERROR;
            mNetworkError.setVisibility(View.VISIBLE);
            mErrorText.setText(Utils.getSyncStatusText(getContext(),
                    mErrorStatus));
            mLoading.setVisibility(View.GONE);
            mLoadMore.setVisibility(View.GONE);
            mErrorActionButton.setVisibility(View.VISIBLE);
            mErrorActionButton.setText(R.string.retry);
            mRemoteSearch.setVisibility(View.GONE);
            return true;
        }
        /// @}
        // check if this folder allow remote search.
        /// M: not show search on server, if query is empty. Adjust mFolder is null to avoid NPE
        if (listContext.isLocalSearchExecuted() && mFolder != null
                && !mFolder.isType(UIProvider.FolderType.DRAFT)
                && !mFolder.isType(UIProvider.FolderType.OUTBOX)
                && !mFolder.supportsCapability(FolderCapabilities.IS_VIRTUAL)) {
            mLoading.setVisibility(View.GONE);
            mNetworkError.setVisibility(View.GONE);
            mLoadMore.setVisibility(View.GONE);
            /// M: pop account do not support remote search. @{
            boolean showRemoteSearch = account != null ? account.supportsCapability(
                    AccountCapabilities.FOLDER_SERVER_SEARCH) : false;
            mRemoteSearch.setVisibility(showRemoteSearch ? View.VISIBLE : View.GONE);
            return showRemoteSearch;
            /// @}
        } else {
            mRemoteSearch.setVisibility(View.GONE);
        }
        return super.updateStatus(cursor);
    }
}
