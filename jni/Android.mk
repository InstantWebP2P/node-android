# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

### prebuilt libuvpp.a
include $(CLEAR_VARS)
LOCAL_MODULE := uvpp-prebuilt
LOCAL_SRC_FILES := libuvpp/libuvpp.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/libuvpp
include $(PREBUILT_STATIC_LIBRARY)

### libuvpp-jni.so
include $(CLEAR_VARS)
LOCAL_MODULE           := uvpp-jni

LOCAL_SRC_FILES        := \
                        hello-jni.c \
                        async.cpp \
                        check.cpp \
                        child_process.cpp \
                        constants.cpp \
                        context.cpp \
                        exception.cpp \
                        file.cpp \
                        file_event.cpp \
                        file_poll.cpp \
                        handle.cpp \
                        idle.cpp \
                        loop.cpp \
                        misc.cpp \
                        os.cpp \
                        pipe.cpp \
                        poll.cpp \
                        process.cpp \
                        stats.cpp \
                        stream.cpp \
                        timer.cpp \
                        tcp.cpp \
                        tty.cpp \
                        udp.cpp \
                        udt.cpp 

LOCAL_STATIC_LIBRARIES := uvpp-prebuilt
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
