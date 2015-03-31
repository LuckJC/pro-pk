package com.android.exchange.adapter;

import com.android.exchange.Eas;
import com.android.mail.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;

public class GetItemEstimateParser extends Parser {
    private static final String TAG = Eas.LOG_TAG;
    public int mEstimate = -1;

    public GetItemEstimateParser(InputStream in) throws IOException {
        super(in);
    }

    @Override
    public boolean parse() throws IOException {
        // Loop here through the remaining xml
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            if (tag == Tags.GIE_GET_ITEM_ESTIMATE) {
                parseGetItemEstimate();
            } else {
                skipTag();
            }
        }
        return true;
    }

    public void parseGetItemEstimate() throws IOException {
        while (nextTag(Tags.GIE_GET_ITEM_ESTIMATE) != END) {
            if (tag == Tags.GIE_RESPONSE) {
                parseResponse();
            } else {
                skipTag();
            }
        }
    }

    public void parseResponse() throws IOException {
        while (nextTag(Tags.GIE_RESPONSE) != END) {
            if (tag == Tags.GIE_STATUS) {
                LogUtils.d(TAG, "GIE status: " + getValue());
            } else if (tag == Tags.GIE_COLLECTION) {
                parseCollection();
            } else {
                skipTag();
            }
        }
    }

    public void parseCollection() throws IOException {
        while (nextTag(Tags.GIE_COLLECTION) != END) {
            if (tag == Tags.GIE_CLASS) {
                LogUtils.d(TAG, "GIE class: " + getValue());
            } else if (tag == Tags.GIE_COLLECTION_ID) {
                LogUtils.d(TAG, "GIE collectionId: " + getValue());
            } else if (tag == Tags.GIE_ESTIMATE) {
                mEstimate = getValueInt();
                LogUtils.d(TAG, "GIE estimate: " + mEstimate);
            } else {
                skipTag();
            }
        }
    }
}
