
package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.R;

import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactListFilter;

public class MultiPhoneAndEmailsPickerFragment extends DataKindPickerBaseFragment {
    //Message only support no more than 100
    private int mNumberBalance = 100;
    private static final String RESULTINTENTEXTRANAME = "com.mediatek.contacts.list.pickdataresult";
    
    public void setNumberBalance(int numberBalance) {
        this.mNumberBalance = numberBalance;
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        // TODO Auto-generated method stub
        super.onItemClick(parent, view, position, id);
        final Button selectItems = (Button) getActivity().getActionBar()
                .getCustomView().findViewById(R.id.select_items);
        if (isSelectedNone()) {
            selectItems.setText(R.string.contactsAllLabel);
        }
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        MultiPhoneAndEmailsPickerAdapter adapter = new MultiPhoneAndEmailsPickerAdapter(getActivity(),
                getListView());
        adapter.setFilter(ContactListFilter
                .createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        return adapter;
    }

    @Override
    public void onOptionAction() {

        final long[] idArray = getCheckedItemIds();
        if (idArray == null) {
            return;
        }

        final Activity activity = getActivity();
        final Intent retIntent = new Intent();
        if (null == retIntent) {
            activity.setResult(Activity.RESULT_CANCELED, null);
            activity.finish();
            return;
        }
        if (idArray.length > mNumberBalance) {
            String limitString = getResources().getString(
                    R.string.contact_recent_number_limit,
                    String.valueOf(mNumberBalance));
            Toast.makeText(getActivity().getApplicationContext(), limitString,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        retIntent.putExtra(RESULTINTENTEXTRANAME, idArray);
        activity.setResult(Activity.RESULT_OK, retIntent);
        activity.finish();
    }

}
