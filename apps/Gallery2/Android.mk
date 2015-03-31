LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += com.android.gallery3d.common2 
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.gallery3d.ext
#mark camera
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.camera.ext
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.transcode
LOCAL_STATIC_JAVA_LIBRARIES += xmp_toolkit
LOCAL_STATIC_JAVA_LIBRARIES += mp4parser
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v8-renderscript
LOCAL_SHARED_LIBRARIES += libRSSupport librsjni

LOCAL_JAVA_LIBRARIES += mediatek-framework telephony-common
LOCAL_JAVA_LIBRARIES += com.mediatek.effect
LOCAL_RENDERSCRIPT_FLAGS := -rs-package-name=android.support.v8.renderscript
LOCAL_CERTIFICATE:=platform
# Keep track of previously compiled RS files too (from bundled GalleryGoogle).
prev_compiled_rs_files := $(call all-renderscript-files-under, src)

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-renderscript-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, src_pd)
#mark build camera
LOCAL_SRC_FILES += $(call all-java-files-under, ../Camera/src)

# LCA project will not build emulator
ifeq ($(MTK_EMULATOR_SUPPORT),yes)
LOCAL_RESOURCE_DIR += packages/apps/Camera/res_emulator
endif

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res packages/apps/Camera/res
LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages com.android.camera

LOCAL_PACKAGE_NAME := Gallery2

LOCAL_OVERRIDES_PACKAGES := Gallery Gallery3D GalleryNew3D

#LOCAL_SDK_VERSION := current

# If this is an unbundled build (to install seprately) then include
# the libraries in the APK, otherwise just put them in /system/lib and
# leave them out of the APK
ifneq (,$(TARGET_BUILD_APPS))
  LOCAL_JNI_SHARED_LIBRARIES := libjni_eglfence libjni_filtershow_filters libjni_jpegstream libjni_motion_track
  
  ifeq ($(strip $(MTK_SUBTITLE_SUPPORT)),yes)
      LOCAL_JNI_SHARED_LIBRARIES += libjni_subtitle_bitmap
  endif
else
  LOCAL_REQUIRED_MODULES := libjni_eglfence libjni_filtershow_filters libjni_jpegstream libjni_motion_track
  
  ifeq ($(strip $(MTK_SUBTITLE_SUPPORT)),yes)
      LOCAL_REQUIRED_MODULES += libjni_subtitle_bitmap
  endif
endif

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

include $(call all-makefiles-under, jni)

ifeq ($(strip $(LOCAL_PACKAGE_OVERRIDES)),)
# Use the following include to make gallery test apk.
include $(call all-makefiles-under, $(LOCAL_PATH))

#mark camera
# Use the following include to make camera test apk.
include $(call all-makefiles-under, ../Camera)

endif
