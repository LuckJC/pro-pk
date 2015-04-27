LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
# Module name should match apk name to be installed
LOCAL_MODULE := iFlyTekPlus
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED

PRODUCT_COPY_FILES += $(LOCAL_PATH)/libbdpush_V2_2.so:system/lib/libbdpush_V2_2.so
PRODUCT_COPY_FILES += $(LOCAL_PATH)/libttsplusmsc.so:system/lib/libttsplusmsc.so

include $(BUILD_PREBUILT)