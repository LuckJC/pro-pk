LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := mediatek-framework \
                        com.android.vcard
LOCAL_JAVA_LIBRARIES += telephony-common

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
                   ../../Dialer/src/com/mediatek/dialer/PhoneCallDetailsEx.java \
                   ../../Dialer/src/com/mediatek/dialer/calllogex/ContactInfoEx.java \
                   ../../Dialer/src/com/mediatek/dialer/calllogex/CallLogQueryEx.java \
                   ../src/com/android/contacts/common/util/UriUtils.java \

LOCAL_MODULE := com.mediatek.contacts.ext
LOCAL_STATIC_JAVA_LIBRARIES := com.android.phone.shared \
                               android-support-v13 \
                               android-support-v4 \
                               CellConnUtil
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_STATIC_JAVA_LIBRARY)

