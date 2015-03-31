package com.mediatek.contacts.ext;

public interface IContactPlugin {
    SimPickExtension createSimPickExtension();

    SpeedDialExtension createSpeedDialExtension();

    DialtactsExtension createDialtactsExtension();

    DialPadExtension createDialPadExtension();

    ContactListExtension createContactListExtension();

    ContactDetailExtension createContactDetailExtension();

    ContactAccountExtension createContactAccountExtension();

    CallListExtension createCallListExtension();

    CallDetailExtension createCallDetailExtension();

    QuickContactExtension createQuickContactExtension();
    
    IccCardExtension createIccCardExtension();

    ContactsCallOptionHandlerExtension createContactsCallOptionHandlerExtension();

    ContactsCallOptionHandlerFactoryExtension createContactsCallOptionHandlerFactoryExtension();

    CallLogAdapterExtension createCallLogAdapterExtension();

    CallDetailHistoryAdapterExtension createCallDetailHistoryAdapterExtension();

    DialerSearchAdapterExtension createDialerSearchAdapterExtension();

    CallLogSearchResultActivityExtension createCallLogSearchResultActivityExtension();

    ContactDetailEnhancementExtension createContactDetailEnhancementExtension();

    CallLogSimInfoHelperExtension createCallLogSimInfoHelperExtension();

    SimServiceExtension createSimServiceExtension();
    
    ImportExportEnhancementExtension createImportExportEnhancementExtension();
}
