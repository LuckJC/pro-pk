
package com.mediatek.contacts.simservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.util.SparseArray;

import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.vcard.ProcessorBase;
import com.android.i18n.phonenumbers.AsYouTypeFormatter;
import com.android.i18n.phonenumbers.PhoneNumberUtil;

import com.mediatek.contacts.ext.ContactAccountExtension;
import com.mediatek.contacts.ext.ContactPluginDefault;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.SubContactsUtils.NamePhoneTypePair;
import com.mediatek.contacts.extension.aassne.SneExt;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simservice.SIMProcessorManager.ProcessorCompleteListener;
import com.mediatek.contacts.simservice.SIMServiceUtils;
import com.mediatek.contacts.simservice.SIMServiceUtils.ServiceWorkData;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroup;
import com.mediatek.contacts.util.LogUtils;

import com.mediatek.telephony.SimInfoManager;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;

public class SIMImportProcessor extends SIMProcessorBase {
    private static final String TAG = "SIMImportProcessor";

    private static final String[] COLUMN_NAMES = new String[] {
            "index", "name", "number", "emails", "additionalNumber", "groupIds"
    };

    protected static final int INDEX_COLUMN = 0; // index in SIM
    protected static final int NAME_COLUMN = 1;
    protected static final int NUMBER_COLUMN = 2;
    protected static final int EMAIL_COLUMN = 3;
    protected static final int ADDITIONAL_NUMBER_COLUMN = 4;
    protected static final int GROUP_COLUMN = 5;

    // In order to prevent locking DB too long,
    // set the max operation count 90 in a batch.
    private static final int MAX_OP_COUNT_IN_ONE_BATCH = 90;

    private HashMap<Integer, Integer> mGroupIdMap;

    private int mSlotId;
    private Context mContext;

    public SIMImportProcessor(Context context, int slotId, Intent intent,
            ProcessorCompleteListener listener) {
        super(intent, listener);
        mContext = context;
        mSlotId = slotId;
        mGroupIdMap = new HashMap<Integer, Integer>();
    }

    @Override
    public int getType() {
        return SIMServiceUtils.TYPE_IMPORT;
    }

    @Override
    public void doWork() {
        LogUtils.d(TAG, "[dowork]Processor [slotId=" + mSlotId + "] running...Thread id="
                + Thread.currentThread().getId());
        if (isCancelled()) {
            LogUtils.d(TAG,"[dowork]cancel import work. Thread id=" + Thread.currentThread().getId());
            return;
        }
        SIMServiceUtils.deleteSimContact(mContext, mSlotId);
        if (isCancelled()) {
            LogUtils.i(TAG,"[dowork]cancelimport work after deleteSimContact. Thread id="
                    + Thread.currentThread().getId());
            return;
        }
        boolean simStateReady = checkSIMStateReady();

        int simId = -1;
        if (simStateReady) {
            SimInfoRecord simInfo = SimInfoManager.getSimInfoBySlot(mContext, mSlotId);
            if (simInfo != null) {
                simId = (int) simInfo.mSimInfoId;
            }
        } else {
            //fixed issue that the usim groups will appear again after enable FDN 
            SIMServiceUtils.sendFinishIntent(mContext, mSlotId);
            LogUtils.i(TAG, "[doWork]simStateReady is not ready, return!");
            return;
        }
        
        int simType = SimCardUtils.getSimTypeBySlot(mSlotId);
        final Uri iccUri = SimCardUtils.SimUri.getSimUri(mSlotId);
        Cursor simCursor = querySIMContact(mContext, mSlotId, simType, iccUri, simId);
        importAllSimContacts(mContext, mSlotId, simCursor, simType, simId);
        if (simCursor != null) {
            simCursor.close();
        }
    }

    public void importAllSimContacts(Context context, int slotId, Cursor simCursor, int simType,
            int simId) {
        if (isCancelled()) {
            LogUtils.d(TAG,"[importAllSimContacts]cancel import work,Thread id="
                    + Thread.currentThread().getId());
            return;
        }
        final ContentResolver resolver = context.getContentResolver();
        
        LogUtils.d(TAG,"[importAllSimContacts]insert slot id:" + slotId + ",sim id:" +simId
                + ",sim type:" + simType);
        
        if (simCursor != null) {
            if (simId < 1) {
                SimInfoRecord simInfo = SimInfoManager.getSimInfoBySlot(context, slotId);
                if (simInfo != null) {
                    simId = (int) simInfo.mSimInfoId;
                }
            }
            
            LogUtils.d(TAG,"[importAllSimContacts]sim Id changed as:" + simId);
            if (simId > 0) {
                synchronized (this) {
                    importAllSimContacts(context, simCursor, resolver, slotId, simId,
                            simType, null, false);
                }
            }
            simCursor.close();

            if (/*SIMServiceUtils.checkSimState(resolver, mSlotId)*/SIMServiceUtils.checkPhoneBookState(mSlotId) && simId > 0) {
                Cursor sdnCursor = null;
                final Uri iccSdnUri = SimCardUtils.SimUri.getSimSdnUri(slotId);
                LogUtils.d(TAG,"[importAllSimContacts]iccSdnUri" + iccSdnUri);
                sdnCursor = resolver.query(iccSdnUri, COLUMN_NAMES, null, null, null);
                if (sdnCursor != null) {
                    LogUtils.d(TAG,"[importAllSimContacts]sdnCursor.getCount() = "
                            + sdnCursor.getCount());
                    try {
                        if (sdnCursor.getCount() > 0) {
                            importAllSimContacts(context, sdnCursor, resolver, slotId,
                                    simId, simType, null, true);
                        }
                    } catch (Exception e) {
                        LogUtils.d(TAG,"[importAllSimContacts]exception:" + e.toString());
                    } finally {
                        sdnCursor.close();
                    }
                }
            }
        }
        
        if (isCancelled()) {
            LogUtils.d(TAG,"[ImportAllSimContactsThread] cancel.");
            return;
        }
        SIMServiceUtils.sendFinishIntent(context, slotId);
    }
    
    //////////////////////private function////////////////////////////////////////////////////
    
    private void importAllSimContacts(Context context, final Cursor cursor,
            final ContentResolver resolver, int slotId, long simId, int simType,
            HashSet<Long> insertSimIdSet, boolean importSdnContacts) {

        if (isCancelled()) {
            LogUtils.d(TAG,"[importAllSimContacts]cancel, Thread id=" + Thread.currentThread().getId());
            return;
        }

        AccountTypeManager atm = AccountTypeManager.getInstance(context);
        List<AccountWithDataSet> lac = atm.getAccounts(true);
        boolean isUsim = (simType == SIMServiceUtils.SIM_TYPE_USIM);

        int accountSlot = -1;
        AccountWithDataSetEx account = null;
        for (AccountWithDataSet accountData : lac) {
            if (accountData instanceof AccountWithDataSetEx) {
                AccountWithDataSetEx accountEx = (AccountWithDataSetEx) accountData;
                accountSlot = accountEx.getSlotId();
                if (accountSlot == slotId) {
                    int accountSimType = (accountEx.type.equals(AccountType.ACCOUNT_TYPE_USIM)) ? SIMServiceUtils.SIM_TYPE_USIM
                            : SIMServiceUtils.SIM_TYPE_SIM;
                    // UIM
                    if (accountEx.type.equals(AccountType.ACCOUNT_TYPE_UIM)) {
                        accountSimType = SIMServiceUtils.SIM_TYPE_UIM;
                    }
                    
                    if (accountSimType == simType) {
                        account = accountEx;
                        break;
                    }
                    break;
                }
            }
        }

        if (account == null) {
            // String accountName = isUsim ? "USIM" + slot : "SIM" + slot;
            // String accountType = isUsim ? AccountType.ACCOUNT_TYPE_USIM : AccountType.ACCOUNT_TYPE_SIM;
            // TBD: use default sim name and sim type.
            LogUtils.i(TAG, "[importAllSimContacts]account is null!");
        }

        final ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

        if (cursor != null) {
            cursor.moveToPosition(-1);
            //Bug Fix ALPS00289127:
            String countryCode = ContactsUtils.getCurrentCountryIso(context);
            LogUtils.i(TAG, "[importAllSimContacts] countryCode :" + countryCode);
            int i = 0;
            while (cursor.moveToNext()) {
                long indexInSim = cursor.getLong(INDEX_COLUMN); // index in SIM
                // Do nothing if sim contacts is already inserted into contacts DB.
                if (insertSimIdSet != null && insertSimIdSet.contains(indexInSim)) {
                    LogUtils.d(TAG,"[importAllSimContacts]slot id:" + slotId + "||indexInSim:"
                            + indexInSim + "||isInserted is true,contine to do next.");
                    continue;
                }
                
                i = actuallyImportOneSimContact(context, cursor,resolver, slotId, simId,simType,
                        indexInSim, importSdnContacts,operationList,i, account, 
                        isUsim,accountSlot,countryCode);
                
                if (i > MAX_OP_COUNT_IN_ONE_BATCH) {
                    try {
                        // TBD: The deleting and inserting of SIM contacts will
                        // be controled in the same operation queue in the future.
                        if (!SIMServiceUtils.checkPhoneBookState(slotId)/*!SIMServiceUtils.checkSimState(context.getContentResolver(), slotId)*/) {
                            LogUtils.d(TAG,"[importAllSimContacts]check sim State: false");
                            break;
                        }
                        LogUtils.d(TAG,"[importAllSimContacts]Before applyBatch. ");
                        resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                        LogUtils.d(TAG,"[importAllSimContacts]After applyBatch ");
                    } catch (RemoteException e) {
                        LogUtils.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    } catch (OperationApplicationException e) {
                        LogUtils.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    }
                    i = 0;
                    operationList.clear();
                }
            }

            //fix CR ALPS00754984
            mGroupIdMap.clear();
            if (isCancelled()) {
                LogUtils.d(TAG,"[importAllSimContacts]cancel import work on after while{}. Thread id="
                                + Thread.currentThread().getId());
                return;
            }
            try {
                LogUtils.d(TAG,"[importAllSimContacts]final,Before applyBatch ");
                if (SIMServiceUtils.checkPhoneBookState(slotId)/*SIMServiceUtils.checkSimState(context.getContentResolver(), slotId)*/) {
                    LogUtils.d(TAG,"[importSimContactcheck] sim State: true");
                    if (!operationList.isEmpty()) {
                        resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                    }
                }
                LogUtils.d(TAG,"[importAllSimContacts]final,After applyBatch ");
            } catch (RemoteException e) {
                LogUtils.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            } catch (OperationApplicationException e) {
                LogUtils.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
        }
    }
    
    private int actuallyImportOneSimContact(Context context, final Cursor cursor,
            final ContentResolver resolver, int slotId, long simId, int simType,
            long indexInSim,boolean importSdnContacts,
            ArrayList<ContentProviderOperation> operationList,int loopCheck,
            AccountWithDataSetEx account,boolean isUsim,
            int accountSlot,String countryCode) {
        int i = loopCheck;
        if (isCancelled()) {
            LogUtils.d(TAG,"[actuallyImportOneSimContact]cancel, Thread id="
                            + Thread.currentThread().getId());
            return i;
        }

        final NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(cursor
                .getString(NAME_COLUMN));
        final String name = namePhoneTypePair.name;
        final int phoneType = namePhoneTypePair.phoneType;
        final String phoneTypeSuffix = namePhoneTypePair.phoneTypeSuffix;
        String phoneNumber = cursor.getString(NUMBER_COLUMN);
        LogUtils.d(TAG,"indexInSim = " + indexInSim + ",phoneType = " + phoneType
                + ",phoneTypeSuffix" + phoneTypeSuffix + ",name = " + name
                + ",phoneNumber = " + phoneNumber);

        int j = 0; 
        String additionalNumber = null;
        String accountType = null;
        
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI);
        ContentValues values = new ContentValues();
        
        if (account != null) {
            accountType = account.type;
            values.put(RawContacts.ACCOUNT_NAME, account.name);
            values.put(RawContacts.ACCOUNT_TYPE, account.type);
        }
        values.put(RawContacts.INDICATE_PHONE_SIM, simId);
        values.put(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
        values.put(RawContacts.INDEX_IN_SIM, indexInSim);

        if (importSdnContacts) {
            values.put(RawContacts.IS_SDN_CONTACT, 1);
        }

        builder.withValues(values);
        operationList.add(builder.build());
        j++;

        if (!TextUtils.isEmpty(phoneNumber)) {
            LogUtils.d(TAG, "[actuallyImportOneSimContact] phoneNumber before : "
                    + phoneNumber);
            AsYouTypeFormatter mFormatter = PhoneNumberUtil.getInstance()
                    .getAsYouTypeFormatter(countryCode);
            char[] cha = phoneNumber.toCharArray();
            int ii = cha.length;
            for (int num = 0; num < ii; num++) {
                phoneNumber = mFormatter.inputDigit(cha[num]);
            }
            LogUtils.d(TAG, "[actuallyImportOneSimContact] phoneNumber after : "
                    + phoneNumber);

            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, i);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            // builder.withValue(Phone.TYPE, phoneType);

            // AAS primary number doesn't have type. 
            ExtensionManager.getInstance().getContactAccountExtension()
                    .checkOperationBuilder(accountType, builder, cursor,
                            ContactAccountExtension.TYPE_OPERATION_INSERT,
                            ExtensionManager.COMMD_FOR_AAS);

            phoneNumber = ExtensionManager.getInstance().getContactListExtension()
                    .getReplaceString(phoneNumber, ContactPluginDefault.COMMD_FOR_OP01);
            builder.withValue(Phone.NUMBER, phoneNumber);
            if (!TextUtils.isEmpty(phoneTypeSuffix)) {
                builder.withValue(Data.DATA15, phoneTypeSuffix);
            }
            operationList.add(builder.build());
            j++;
        }

        if (!TextUtils.isEmpty(name)) {
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, i);
            builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(StructuredName.DISPLAY_NAME, name);
            operationList.add(builder.build());
            j++;
        }

        // if USIM
        if (isUsim) {
            j = importUSimPart(cursor,operationList,i,j,countryCode,accountType,accountSlot); 
        }
        
        i = i + j;
        
        return i;
    }
    
    private int importUSimPart(final Cursor cursor, ArrayList<ContentProviderOperation> operationList,
            int loopCheck,int loop,
            String countryCode,String accountType,int accountSlot) {
        int i = loopCheck;
        int j = loop;
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        
        // insert USIM email
        final String emailAddresses = cursor.getString(EMAIL_COLUMN);
        LogUtils.d(TAG,"[importUSimPart]import a USIM contact.emailAddresses:" + emailAddresses);
        if (!TextUtils.isEmpty(emailAddresses)) {
            final String[] emailAddressArray;
            emailAddressArray = emailAddresses.split(",");
            for (String emailAddress : emailAddressArray) {
                LogUtils.d(TAG,"[actuallyImportOneSimContact]emailAddress IS " + emailAddress);
                if (!TextUtils.isEmpty(emailAddress) && !emailAddress.equals("null")) {
                    builder.withValueBackReference(Email.RAW_CONTACT_ID, i);
                    builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                    builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
                    builder.withValue(Email.DATA, emailAddress);
                    operationList.add(builder.build());
                    j++;
                }
            }
        }

        // insert USIM additional number
        String additionalNumber = cursor.getString(ADDITIONAL_NUMBER_COLUMN);
        LogUtils.d(TAG,"[importUSimPart]additionalNumber:" + additionalNumber);
        if (!TextUtils.isEmpty(additionalNumber)) {
            LogUtils.i(TAG, "[importUSimPart] additionalNumber before : " + additionalNumber);
            AsYouTypeFormatter mFormatter = PhoneNumberUtil.getInstance()
                    .getAsYouTypeFormatter(countryCode);
            char[] cha = additionalNumber.toCharArray();
            int ii = cha.length;
            for (int num = 0; num < ii; num++) {
                additionalNumber = mFormatter.inputDigit(cha[num]);
            }
            LogUtils.i(TAG, "[importUSimPart] additionalNumber after : " + additionalNumber);
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, i);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            // builder.withValue(Phone.TYPE, phoneType);

            /**
             *For AAS , original code:
             * builder.withValue(Data.DATA2, 7); 
             * TBD, Need to do refactoring, seperate from host to plug-in fully
             */
            if (!ExtensionManager.getInstance().getContactAccountExtension()
                    .checkOperationBuilder(accountType, builder, cursor,
                            ContactAccountExtension.TYPE_OPERATION_AAS,
                            ExtensionManager.COMMD_FOR_AAS)) {
                builder.withValue(Data.DATA2, 7);
            }
            additionalNumber = ExtensionManager.getInstance().getContactListExtension()
                    .getReplaceString(additionalNumber,
                            ContactPluginDefault.COMMD_FOR_OP01);
            builder.withValue(Phone.NUMBER, additionalNumber);
            builder.withValue(Data.IS_ADDITIONAL_NUMBER, 1);
            operationList.add(builder.build());
            j++;
        }

        //SNE,TBD, Need to do refactoring, seperate from host to plug-in fully
        if (SneExt.hasSne(accountSlot)
                && ExtensionManager.getInstance().getContactAccountExtension()
                        .buildOperationFromCursor(accountType, operationList, cursor,
                                i, ExtensionManager.COMMD_FOR_SNE)) {
            j++;
        }

        //  USIM group
        final String ugrpStr = cursor.getString(GROUP_COLUMN);
        LogUtils.d(TAG,"[importUSimPart]sim group id string: " + ugrpStr);
        if (!TextUtils.isEmpty(ugrpStr)) {
            String[] ugrpIdArray = null;
            if (!TextUtils.isEmpty(ugrpStr)) {
                ugrpIdArray = ugrpStr.split(",");
            }
            for (String ugrpIdStr : ugrpIdArray) {
                int ugrpId = -1;
                try {
                    if (!TextUtils.isEmpty(ugrpIdStr)) {
                        ugrpId = Integer.parseInt(ugrpIdStr);
                    }
                } catch (Exception e) {
                    LogUtils.d(TAG,"[importUSimPart] catched exception");
                    e.printStackTrace();
                    continue;
                }
                LogUtils.d(TAG,"[importUSimPart] sim group id ugrpId: " + ugrpId);
                if (ugrpId > 0) {
                    // / M: fix CR ALPS00754984
                    Integer grpId = mGroupIdMap.get(ugrpId);
                    LogUtils.d(TAG,"[importUSimPart]simgroup mapping group grpId: " + grpId);
                    if (grpId == null) {
                        LogUtils.e(TAG, "[USIM Group] Error. Catch unhandled "
                                + "SIM group error. ugrp: " + ugrpId);
                        continue;
                    }
                    builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                    builder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
                    builder.withValue(GroupMembership.GROUP_ROW_ID, grpId);
                    builder.withValueBackReference(Phone.RAW_CONTACT_ID, i);
                    operationList.add(builder.build());
                    j++;
                }
            }
        }
        
        return j;
    }
    
    private boolean checkSIMStateReady() {
        boolean simStateReady = /*SIMServiceUtils.checkSimState(mContext.getContentResolver(),
                mSlotId)
                && */SIMServiceUtils.checkPhoneBookState(mSlotId);
        int i = 10;
        while (i > 0) {
            if (!simStateReady) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    LogUtils.w(TAG, "[checkSIMStateReady]excepiotn:" + e.toString());
                }
                simStateReady = /*SIMServiceUtils.checkSimState(mContext.getContentResolver(),
                        mSlotId)
                        && */SIMServiceUtils.checkPhoneBookState(mSlotId);
            } else {
                break;
            }
            i--;
        }
        return simStateReady;
    }

    private Cursor querySIMContact(Context context, int slotId, int simType, Uri iccUri, int simId) {
        if (isCancelled()) {
            LogUtils.d(TAG, "[querySIMContact]canceled,return.");
            return null;
        }
        LogUtils.d(TAG, "[querySIMContact]slotId:" + slotId + "|simId:" + simId + "|simType:"
                + simType);
        
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(iccUri, COLUMN_NAMES, null, null, null);
        } catch (java.lang.NullPointerException e) {
            LogUtils.d(TAG, "[querySIMContact]exception:" + e.toString());
            return null;
        }
        if (cursor != null) {
            int count = cursor.getCount();
            LogUtils.d(TAG, "[querySIMContact]count:" + count);
        }
        
        if (simType == SIMServiceUtils.SIM_TYPE_USIM) {
            mGroupIdMap.clear();
            ServiceWorkData workData = new ServiceWorkData(slotId, simId, simType, cursor);
            USIMGroup.syncUSIMGroupContactsGroup(context, workData, mGroupIdMap);
        } else {
            USIMGroup.deleteUSIMGroupOnPhone(context, slotId);
        }
        // workData.mSimCursor = cursor;
        return cursor;
    }
}
