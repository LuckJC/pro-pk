package com.mediatek.contacts.ext;

public class ContactPluginDefault implements IContactPlugin {
    public static final String COMMD_FOR_OP01 = "ExtensionForOP01";
    public static final String COMMD_FOR_OP09 = "ExtensionForOP09";
    public static final String COMMD_FOR_AppGuideExt = "ExtensionForAppGuideExt";
    public static final String COMMD_FOR_RCS = "ExtenstionForRCS";
    public static final String COMMD_FOR_AAS = "ExtensionForAAS";
    public static final String COMMD_FOR_SNE = "ExtensionForSNE";
    public static final String COMMD_FOR_SNS = "ExtensionForSNS";

    public CallDetailExtension createCallDetailExtension() {
        return new CallDetailExtension();
    }

    public CallListExtension createCallListExtension() {
        return new CallListExtension();
    }

    public ContactAccountExtension createContactAccountExtension() {
        return new ContactAccountExtension();
    }

    public ContactDetailExtension createContactDetailExtension() {
        return new ContactDetailExtension();
    }

    public ContactListExtension createContactListExtension() {
        return new ContactListExtension();
    }

    public DialPadExtension createDialPadExtension() {
        return new DialPadExtension();
    }

    public DialtactsExtension createDialtactsExtension() {
        return new DialtactsExtension();
    }

    public SimPickExtension createSimPickExtension() {
        return new SimPickExtension();
    }

    public SpeedDialExtension createSpeedDialExtension() {
        return new SpeedDialExtension();
    }

    public QuickContactExtension createQuickContactExtension() {
        return new QuickContactExtension();
    }
    
    public ContactDetailEnhancementExtension createContactDetailEnhancementExtension() {
        return new ContactDetailEnhancementExtension();
    }

    @Override
    public ContactsCallOptionHandlerExtension createContactsCallOptionHandlerExtension() {
        return new ContactsCallOptionHandlerExtension();
    }

    @Override
    public ContactsCallOptionHandlerFactoryExtension createContactsCallOptionHandlerFactoryExtension() {
        return new ContactsCallOptionHandlerFactoryExtension();
    }

    @Override
    public CallLogAdapterExtension createCallLogAdapterExtension() {
        return new CallLogAdapterExtension();
    }

    @Override
    public CallDetailHistoryAdapterExtension createCallDetailHistoryAdapterExtension() {
        return new CallDetailHistoryAdapterExtension();
    }

    @Override
    public CallLogSearchResultActivityExtension createCallLogSearchResultActivityExtension() {
        return new CallLogSearchResultActivityExtension();
    }

    @Override
    public DialerSearchAdapterExtension createDialerSearchAdapterExtension() {
        return new DialerSearchAdapterExtension();
    }

    @Override
    public CallLogSimInfoHelperExtension createCallLogSimInfoHelperExtension() {
        return new CallLogSimInfoHelperExtension();
    }

    @Override
    public SimServiceExtension createSimServiceExtension() {
        return new SimServiceExtension();
    }
    
    @Override
    public ImportExportEnhancementExtension createImportExportEnhancementExtension() {
        return new ImportExportEnhancementExtension();
    }

    @Override
    public IccCardExtension createIccCardExtension() {
        return new IccCardExtension();
    }
}
