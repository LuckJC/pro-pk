# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

# MediaTek Inc. (C) 2010. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.


#
# motion_jni
#

#ifneq ($(filter MT6592, $(MTK_PLATFORM)),)
ifeq ($(MTK_MOTION_TRACK_SUPPORT), yes)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

#LOCAL_PREBUILT_LIBS += libmfbmm.a
#include $(BUILD_MULTI_PREBUILT)

# clear variables again to avoid definition of LOCAL_BUILD_MODULE
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_ARM_MODE := arm
#LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon
LOCAL_CFLAGS += -g -fno-omit-frame-pointer -fno-inline -fno-peephole



LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
    $(MTK_PATH_PLATFORM)/hardware/camera/inc/algorithm/libutility \
    $(MTK_PATH_PLATFORM)/hardware/camera/inc/algorithm/libmfbmm \
    external/skia/include/core  \
	external/skia/include/images \
	$(MTK_PATH_SOURCE)/external/mhal/inc \
	external/stlport/stlport \
	bionic \
	bionic/libstdc++/include \
	$(MTK_PATH_PLATFORM)/hardware/mtkcam/inc/algorithm/libmfbmm \
	$(MTK_PATH_PLATFORM)/hardware/include/mtkcam/algorithm/libmfbmm \
	$(MTK_PATH_PLATFORM)/custom/common/hal/inc \

LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/m4u	
LOCAL_C_INCLUDES += $(MTK_PATH_PLATFORM)/hardware/jpeg/inc
LOCAL_C_INCLUDES += $(TOP)/$(MTK_PATH_PLATFORM)/hardware/camera/inc
	
# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
  jni_motion_track.cpp \
  motion_track.cpp \
  
LOCAL_STATIC_LIBRARIES := \

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libcutils \
	libcamalgo \
	libskia \
	libstlport \
	libJpgEncPipe

  LOCAL_SHARED_LIBRARIES += libmhalImageCodec
  LOCAL_SHARED_LIBRARIES += libdpframework
  LOCAL_SHARED_LIBRARIES += libcamalgo
  	
# This is the target being built.
LOCAL_MODULE:= libjni_motion_track

include $(BUILD_SHARED_LIBRARY)

endif

