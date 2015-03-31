package com.mediatek.vcs;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

import com.android.contacts.common.list.ContactListFilter;
import com.mediatek.contacts.util.LogUtils;

import java.util.ArrayList;

/**
 * The Voice Search Result Loader is used to load contacts who exist in local
 * contacts provider by the results voice bank return, including all
 * corresponding contacts info for The VCS list UI.
 * @deprecated
 */
public class VoiceSearchResultLoader implements LoaderCallbacks<Cursor> {

    public interface Listener {
        // get contacts loader manager
        LoaderManager onGetLoaderManager();
        // to optimize the voice search results for final list
        void onOptimizeResultDone(Cursor optCursor);
        // hint no contacts found in data base
        void onNoSuchContactsInDatabase();
    }

    private static final String TAG = "VoiceSearchResultLoader";

    private Context mContext;
    private Listener mListener;
    private static ArrayList<String> mAudioNameList = new ArrayList<String>();

    public VoiceSearchResultLoader(Context context, Listener listener) {
        mContext = context;
        mListener = listener;
    }

    /// load contacts section
    final String[] CONTACT_PROJECTION = new String[] { Contacts._ID, // 0
            Contacts.DISPLAY_NAME_PRIMARY, // 1
            Contacts.CONTACT_PRESENCE, // 2
            Contacts.CONTACT_STATUS, // 3
            Contacts.PHOTO_ID, // 4
            Contacts.PHOTO_URI, // 5
            Contacts.PHOTO_THUMBNAIL_URI, // 6
            Contacts.LOOKUP_KEY // 7
    };
    private static final String SUB_SELECTION = "display_name like ?";
    private static final String OR_SELECTION = " or ";

    private static final int MAX_NAME_COUNTS_TODISPLAY = 6;
    private static final int COLUMN_COUNTS_OF_CURSOR = 8;

    public void triggerContactsLoader(final ArrayList<String> audioNameArrayList) {
        LogUtils.d(TAG, "[triggerContactsLoader] [vcs] initiate loader");
        if (audioNameArrayList == null || audioNameArrayList.size() == 0) {
            LogUtils.d(TAG, "[triggerContactsLoader] [vcs] audion name list is null or empty");
            return;
        }
        mAudioNameList = audioNameArrayList;
        mListener.onGetLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = createCursorLoader();
        long directoryId = 0;
        LogUtils.d(TAG, "[onCreateLoader] [vcs] onCreateLoader directoryId:" + directoryId);
        configureLoader(loader, directoryId);
        return loader;
    }

    private CursorLoader createCursorLoader() {
        return new CursorLoader(mContext, null, null, null, null, null);
    }

    private void configureLoader(CursorLoader loader, long directoryId) {
        LogUtils.d(TAG, "[configureLoader] [vcs] configureLoader directoryId:" + directoryId);
        ContactListFilter filter = null;
        configureUri(loader, directoryId, filter);
        loader.setProjection(CONTACT_PROJECTION);
        configureSelection(loader, directoryId, mAudioNameList);

        String sortOrder = Contacts.SORT_KEY_PRIMARY;
        loader.setSortOrder(sortOrder);
    }

    private void configureUri(CursorLoader loader, long directoryId, ContactListFilter filter) {
        Uri uri = Uri.parse(ContactsContract.Contacts.CONTENT_URI + "?" + "address_book_index_extras" + "=true&"
                + ContactsContract.DIRECTORY_PARAM_KEY + "=0");
        LogUtils.d(TAG, "[configureUri] [vcs] uri:" + uri);
        loader.setUri(uri);
    }

    private void configureSelection(CursorLoader loader, long directoryId, final ArrayList<String> audioNameList) {
        if (audioNameList == null || audioNameList.size() == 0) {
            LogUtils.i(TAG, "[configureSelection] [vcs] audioNameList is null or empty.");
            return;
        }
        LogUtils.i(TAG, "[configureSelection] [vcs] audioNameList size:" + audioNameList.size());

        int nameListSize = audioNameList.size();
        String selection = new String();
        String subSelection = SUB_SELECTION;
        String[] selectionArgs = new String[nameListSize];
        for (int i = 0; i < nameListSize; i++) {
            selection = selection + subSelection;
            subSelection = OR_SELECTION + SUB_SELECTION;
            selectionArgs[i] = "%" + audioNameList.get(i) + "%";
        }
        LogUtils.i(TAG, "[configureSelection] [vcs] selection:" + selection);

        loader.setSelection(selection);
        loader.setSelectionArgs(selectionArgs);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() == 0) {
            LogUtils.d(TAG, "[onLoadFinished] [vcs] cursor is null");
            mListener.onNoSuchContactsInDatabase();
            return;
        }

        LogUtils.d(TAG, "[onLoadFinished] [vcs] cursor.counts:" + data.getCount());

        // construct a new sorted cursor as mAudioNameList order by data cursor.
        Cursor cursor = makeOrderedCursor(data);

        //call back trigger search done method.
        cursor.moveToPosition(-1);
        mListener.onOptimizeResultDone(cursor);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // TODO Auto-generated method stub
        LogUtils.d(TAG, "[onLoaderReset] [vcs]");

    }

    // construct a new sorted cursor as audioNameList order by originalCursor.
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

        LogUtils.i(TAG, "[makeOrderedCursor] [vcs-test] orderedCursor counts:" + audioOrderedCursor.getCount());

        return audioOrderedCursor;
    }

    private void printOrderedCursor(Cursor cursor) {
        LogUtils.i(TAG, "[printOrderedCursor] [vcs] orderedCursor counts:" + cursor.getCount());
        // check loading result
        while (cursor.moveToNext()) {
            String[] columnValArray = new String[5];
            // TODO: modify this column segments val if need this section.
            columnValArray[0] = String.valueOf(cursor.getLong(cursor.getColumnIndex("_id")));
            LogUtils.d(TAG, "[printOrderedCursor] [vcs] contact_id:" + columnValArray[0]);
            columnValArray[1] = cursor.getString(cursor.getColumnIndex("lookup"));
            LogUtils.d(TAG, "[printOrderedCursor] [vcs] lookup:" + columnValArray[1]);
            columnValArray[2] = cursor.getString(cursor.getColumnIndex("photo_uri"));
            LogUtils.d(TAG, "[printOrderedCursor] [vcs] photo_uri:" + columnValArray[2]);
            columnValArray[3] = cursor.getString(cursor.getColumnIndex("display_name"));
            LogUtils.d(TAG, "[printOrderedCursor] [vcs-test] display_name:" + columnValArray[3]);
        }
    }
}
