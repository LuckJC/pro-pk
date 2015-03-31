package com.android.exchange.eas;

import android.content.Context;
import android.content.SyncResult;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.Mailbox;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.adapter.GetItemEstimateParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

import org.apache.http.HttpEntity;

import java.io.IOException;

public class EasGetItemEstimate extends EasOperation {
    private String mFilter;
    private Mailbox mMailbox;
    private String mFolderClassName;

    public EasGetItemEstimate(final Context context, final Account account) {
        super(context, account);
    }

    public int getEstimate(String filter, Mailbox mailbox, String folderClassName) {
        mFilter = filter;
        mMailbox = mailbox;
        mFolderClassName = folderClassName;
        return performOperation(null);
    }

    @Override
    protected String getCommand() {
        return "GetItemEstimate";
    }

    @Override
    protected HttpEntity getRequestEntity() throws IOException {
        final Serializer s = new Serializer();
        boolean ex10 = getProtocolVersion() >= Eas.SUPPORTED_PROTOCOL_EX2010_DOUBLE;
        boolean ex03 = getProtocolVersion() < Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE;
        boolean ex07 = !ex10 && !ex03;

        s.start(Tags.GIE_GET_ITEM_ESTIMATE).start(Tags.GIE_COLLECTIONS);
        s.start(Tags.GIE_COLLECTION);
        if (ex07) {
            // Exchange 2007 likes collection id first
            s.data(Tags.GIE_COLLECTION_ID, mMailbox.mServerId);
            s.data(Tags.SYNC_FILTER_TYPE, mFilter);
            s.data(Tags.SYNC_SYNC_KEY, mMailbox.mSyncKey);
        } else if (ex03) {
            // Exchange 2003 needs the "class" element
            s.data(Tags.GIE_CLASS, mFolderClassName);
            s.data(Tags.SYNC_SYNC_KEY, mMailbox.mSyncKey);
            s.data(Tags.GIE_COLLECTION_ID, mMailbox.mServerId);
            s.data(Tags.SYNC_FILTER_TYPE, mFilter);
        } else {
            // Exchange 2010 requires the filter inside an OPTIONS container and sync key first
            s.data(Tags.SYNC_SYNC_KEY, mMailbox.mSyncKey);
            s.data(Tags.GIE_COLLECTION_ID, mMailbox.mServerId);
            s.start(Tags.SYNC_OPTIONS).data(Tags.SYNC_FILTER_TYPE, mFilter).end();
        }
        s.end().end().end().done(); // GIE_COLLECTION, GIE_COLLECTIONS, GIE_GET_ITEM_ESTIMATE
        return makeEntity(s);
    }

    @Override
    protected int handleResponse(EasResponse response, SyncResult syncResult)
            throws IOException {
        if (response.isEmpty()) {
            // TODO this should probably not be an IOException, maybe something more descriptive?
            throw new IOException("Empty ping response");
        }

        // Handle a valid response.
        final GetItemEstimateParser gieParser = new GetItemEstimateParser(response.getInputStream());
        gieParser.parse();
        return gieParser.mEstimate;
    }
}
