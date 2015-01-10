/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <string.h>
#include <assert.h>
#include <stdlib.h>
#include <jni.h>

#include "uv.h"
#include "exception.h"
#include "stream.h"
///#include "com_iwebpp_libuvpp_handles_FileEventHandle.h"

class FileEventCallbacks {
private:
  static jclass _file_event_handle_cid;

  static jmethodID _file_event_callback_mid;

  JNIEnv* _env;
  jobject _instance;

public:
  static void static_initialize(JNIEnv* env, jclass cls);

  FileEventCallbacks();
  ~FileEventCallbacks();

  void initialize(JNIEnv* env, jobject instance);
  void on_event(int status, int event, const char* filename);
  void on_close();
};

typedef enum {
  FILE_EVENT_CALLBACK = 1,
  FILE_EVENT_CLOSE_CALLBACK
} FileEventHandleCallbackType;

jclass FileEventCallbacks::_file_event_handle_cid = NULL;

jmethodID FileEventCallbacks::_file_event_callback_mid = NULL;

void FileEventCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _file_event_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_file_event_handle_cid);

  _file_event_callback_mid = env->GetMethodID(_file_event_handle_cid, "callback", "(IIILjava/lang/String;)V");
  assert(_file_event_callback_mid);
}

void FileEventCallbacks::initialize(JNIEnv* env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

FileEventCallbacks::FileEventCallbacks() {
}

FileEventCallbacks::~FileEventCallbacks() {
  _env->DeleteGlobalRef(_instance);
}

void FileEventCallbacks::on_event(int status, int events, const char* filename) {
  assert(_env);
  jstring f = _env->NewStringUTF(filename);
  _env->CallVoidMethod(
      _instance,
      _file_event_callback_mid,
      FILE_EVENT_CALLBACK,
      status,
      events,
      f);
  if (f) { _env->DeleteLocalRef(f); }
}

void FileEventCallbacks::on_close() {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _file_event_callback_mid,
      FILE_EVENT_CLOSE_CALLBACK,
      0,
      0,
      NULL);
}

static void on_event_cb(uv_fs_event_t* handle, const char* filename, int events, int status) {
  assert(handle);
  assert(handle->data);
  FileEventCallbacks* cb = reinterpret_cast<FileEventCallbacks*>(handle->data);
  cb->on_event(status, events, filename);
}

static void _close_cb(uv_handle_t* handle) {

  assert(handle);
  assert(handle->data);
  FileEventCallbacks* cb = reinterpret_cast<FileEventCallbacks*>(handle->data);
  cb->on_close();
  delete cb;
  delete handle;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FileEventHandle
 * Method:    _new
 * Signature: (V)J
 */
extern "C" JNIEXPORT  jlong JNICALL Java_com_iwebpp_libuvpp_handles_FileEventHandle__1new
  (JNIEnv *env, jclass cls) {

  uv_fs_event_t* fs_event = new uv_fs_event_t();
  fs_event->data = new FileEventCallbacks();
  return reinterpret_cast<jlong>(fs_event);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FileEventHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_FileEventHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  FileEventCallbacks::static_initialize(env, cls);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FileEventHandle
 * Method:    _initialize
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_FileEventHandle__1initialize
  (JNIEnv *env, jobject that, jlong fs_event_ptr) {

  assert(fs_event_ptr);
  uv_fs_event_t* handle = reinterpret_cast<uv_fs_event_t*>(fs_event_ptr);
  assert(handle->data);
  FileEventCallbacks* cb = reinterpret_cast<FileEventCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FileEventHandle
 * Method:    _start
 * Signature: (JJLjava/lang/String;Z)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_FileEventHandle__1start
  (JNIEnv *env, jobject that, jlong loop_ptr, jlong fs_event_ptr, jstring path, jboolean persistent) {

  assert(loop_ptr);
  assert(fs_event_ptr);

  uv_loop_t* loop = reinterpret_cast<uv_loop_t*>(loop_ptr);
  uv_fs_event_t* handle = reinterpret_cast<uv_fs_event_t*>(fs_event_ptr);
  const char* cpath = env->GetStringUTFChars(path, 0);

  int r = uv_fs_event_init(loop, handle, cpath, on_event_cb, 0);
  if (r == 0 && !persistent) {
    uv_unref(reinterpret_cast<uv_handle_t*>(handle));
  }
  if (r) {
    ThrowException(env, loop, "uv_fs_event_init");
  }
  env->ReleaseStringUTFChars(path, cpath);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FileEventHandle
 * Method:    _close
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_FileEventHandle__1close
  (JNIEnv *env, jobject that, jlong fs_event_ptr) {

  assert(fs_event_ptr);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(fs_event_ptr);
  uv_close(handle, _close_cb);
}
