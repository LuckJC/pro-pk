package com.mediatek.contacts.detail;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.android.contacts.R;

import com.android.contacts.detail.ContactDetailFragment;

public class GroupListDialog extends DialogFragment {
    private static final String TAG = "GroupListDialog";

    private Activity mContext = null;
    private String mAccountName = null;
    private int mSlotId = -1;
    private String [] mTitleArray = null;
    private ContactDetailFragment mFragment;
    
    public GroupListDialog(){

    }

    public GroupListDialog(String[] titleArray) {
        mTitleArray = titleArray;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            mTitleArray = savedState.getStringArray("mTitleArray");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("mAccountName", mAccountName);
        outState.putInt("mSlotId", mSlotId);
        outState.putStringArray("mTitleArray", mTitleArray);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mFragment = (ContactDetailFragment)this.getTargetFragment();
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle(R.string.contact_detail_group_list_title);
        
        if (mTitleArray.length == 0) {
            builder.setMessage(R.string.contact_detail_group_list_nogroups);
        } else {
            builder.setView(mFragment.configDetailGroupList());
        }

        // Reset group list item status
        mFragment.isGroupItemChecked = false;

        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // when there is no group click ok is to create new group
                        if (mTitleArray.length == 0) {
                            mFragment.createNewGroup();
                            return;
                        }
                        // count the all checked groups.
                        if (mContext != null) {
                            mFragment.countSelectedGroupItem();
                            mFragment.updateGroupIdToContact();
                        }
                    }
                });
        builder.setNegativeButton(android.R.string.no, null);

        return builder.create();
    }

}