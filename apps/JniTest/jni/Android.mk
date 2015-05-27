LOCAL_PATH:= $(call my-dir)

# 一个完整模块编译
include $(CLEAR_VARS)

LOCAL_SRC_FILES := com_example_jnitest_JniActivity.c

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)

LOCAL_MODULE := libmyjnitest

LOCAL_SHARED_LIBRARIES := \
	libcutils	\
	libutils

LOCAL_PRELINK_MODULE := false

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
