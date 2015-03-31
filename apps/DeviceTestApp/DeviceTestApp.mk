ifeq ($(MTK_NFC_SUPPORT), yes)

LOCAL_PATH:= packages/apps/DeviceTestApp

########################################
# MTK NFC DTA Configuration
########################################
#$(info "copy file test start...")

#$(info "LOCAL PATH:" $(LOCAL_PATH))

#PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/nfc_conformance/DTA_Config/AT4/listen_config.txt:data/misc/nfc_conformance/DTA_Config/AT4/listen_config.txt 

#$(info $(foreach f,$(wildcard $(LOCAL_PATH)/nfc_conformance/DTA_Config/AT4/*),$(f):data/misc/nfc_conformance/DTA_Config/AT4/$(notdir $(f))))


PRODUCT_COPY_FILES += \
        $(foreach f,$(wildcard $(LOCAL_PATH)/nfc_conformance/DTA_Config/AT4/*),$(f):data/misc/nfc_conformance/DTA_Config/AT4/$(notdir $(f)))

PRODUCT_COPY_FILES += \
        $(foreach f,$(wildcard $(LOCAL_PATH)/nfc_conformance/DTA_Config/Clear2Pay/*),$(f):data/misc/nfc_conformance/DTA_Config/Clear2Pay/$(notdir $(f)))

PRODUCT_COPY_FILES += \
        $(foreach f,$(wildcard $(LOCAL_PATH)/nfc_conformance/DTA_Config/Comprion/*),$(f):data/misc/nfc_conformance/DTA_Config/Comprion/$(notdir $(f)))

PRODUCT_COPY_FILES += \
        $(foreach f,$(wildcard $(LOCAL_PATH)/nfc_conformance/DTA_Config/Others/*),$(f):data/misc/nfc_conformance/DTA_Config/Others/$(notdir $(f)))

#$(info $(foreach f,$(wildcard $(LOCAL_PATH)/nfc_conformance/DTA_Config/AT4/*),$(f):data/misc/nfc_conformance/DTA_Config/AT4/$(notdir $(f))))

#$(info "copy file test end")
endif

