LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 universal-image-loader

LOCAL_PACKAGE_NAME := WatchGallery

include $(BUILD_PACKAGE)
##################################################
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := universal-image-loader:libs/universal-image-loader-1.8.6-with-sources.jar

include $(BUILD_MULTI_PREBUILT)

# Use the folloing include to make our test apk.
#include $(call all-makefiles-under,$(LOCAL_PATH))
