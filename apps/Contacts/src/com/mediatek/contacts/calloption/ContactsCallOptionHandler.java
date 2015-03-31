package com.mediatek.contacts.calloption;

import android.content.Context;
import android.content.Intent;
import android.os.ServiceManager;
import android.util.Log;

import com.android.contacts.ContactsApplication;
import com.android.internal.telephony.ITelephony;
import com.android.phone.Constants;
import com.mediatek.calloption.CallOptionBaseHandler;
import com.mediatek.calloption.CallOptionHandler;
import com.mediatek.calloption.CallOptionHandlerFactory;
import com.mediatek.calloption.SimAssociateHandler;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.contacts.simcontact.SlotUtils;

public class ContactsCallOptionHandler extends CallOptionHandler
                                       implements CallOptionBaseHandler.ICallOptionResultHandle {
    private static final String TAG = "ContactsCallOptionHandler";

    private Context mActivityContext = null;

    public ContactsCallOptionHandler(Context activityContext, CallOptionHandlerFactory callOptionHandlerFactory) {
        super(callOptionHandlerFactory);
        mActivityContext = activityContext;
    }
    /**
     * The entry for making an call
     * @param intent the call intent
     */
    public void doCallOptionHandle(Intent intent) {
        final ITelephony telephony =
            ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (null == telephony) {
            LogUtils.w(TAG,"[doCallOptionHandle]Can not get telephony service object");
            return;
        }
        
        SimAssociateHandler.getInstance(ContactsApplication.getInstance()).load();
        if (ExtensionManager.getInstance().getContactsCallOptionHandlerExtension().doCallOptionHandle(
                mCallOptionHandlerList, mActivityContext, ContactsApplication.getInstance(),
                intent, this, ContactsApplication.getInstance().cellConnMgr,
                telephony, FeatureOption.MTK_GEMINI_SUPPORT,
                FeatureOption.MTK_GEMINI_3G_SWITCH)) {
            LogUtils.w(TAG,"[doCallOptionHandle] doCallOptionHandle,plug-in return true,return.");
            return;
        }
        
        super.doCallOptionHandle(mActivityContext, ContactsApplication.getInstance(), intent,
                                 this, ContactsApplication.getInstance().cellConnMgr,
                                 telephony, SlotUtils.isGeminiEnabled(),
                                 FeatureOption.MTK_GEMINI_3G_SWITCH);
    }

    public void onHandlingFinish() {
    }

    public void onContinueCallProcess(Intent intent) {
        dismissDialogs();/// Ensure the Dialogs be dismissed before launch a new "call"
        intent.setAction(Constants.OUTGOING_CALL_RECEIVER);
        intent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_RECEIVER);
        ContactsApplication.getInstance().sendBroadcast(intent);
    }

    public void onPlaceCallDirectly(Intent intent) {
        intent.setAction(Constants.OUTGOING_CALL_RECEIVER);
        intent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_RECEIVER);
        ContactsApplication.getInstance().sendBroadcast(intent);
    }
}
