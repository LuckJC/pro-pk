LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional # user

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := JniTest

LOCAL_JNI_SHARED_LIBRARIES := libmyjnitest

include $(BUILD_PACKAGE)

include $(LOCAL_PATH)/jni/Android.mk

# Use the folloing include to make our test apk.  
include $(call all-makefiles-under,$(LOCAL_PATH))
