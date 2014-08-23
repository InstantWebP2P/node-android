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
#include "stats.h"
#include "stream.h"
///#include "com_iwebpp_libuvpp_handles_FilePollHandle.h"

class FilePollCallbacks {
private:
  static jclass _file_poll_handle_cid;

  static jmethodID _file_poll_callback_mid;
  static jmethodID _file_poll_stop_callback_mid;

  JNIEnv* _env;
  jobject _instance;
  jobject _previous_stats;
  jobject _current_stats;

public:
  static void static_initialize(JNIEnv* env, jclass cls);

  FilePollCallbacks();
  ~FilePollCallbacks();

  void initialize(JNIEnv* env, jobject instance);
  void on_poll(int status, const uv_statbuf_t* previous, const uv_statbuf_t* current);
  void on_stop();
  void set_stats(jobject previous, jobject current);
};

typedef enum {
  FILE_POLL_CALLBACK = 1,
  FILE_POLL_STOP_CALLBACK
} FilePollHandleCallbackType;

jclass FilePollCallbacks::_file_poll_handle_cid = NULL;

jmethodID FilePollCallbacks::_file_poll_callback_mid = NULL;
jmethodID FilePollCallbacks::_file_poll_stop_callback_mid = NULL;

void FilePollCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _file_poll_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_file_poll_handle_cid);

  _file_poll_callback_mid = env->GetMethodID(_file_poll_handle_cid, "callPoll", "(I)V");
  assert(_file_poll_callback_mid);

  _file_poll_stop_callback_mid = env->GetMethodID(_file_poll_handle_cid, "callStop", "()V");
  assert(_file_poll_stop_callback_mid);
}

FilePollCallbacks::FilePollCallbacks() {
}

FilePollCallbacks::~FilePollCallbacks() {
  _env->DeleteGlobalRef(_instance);

  if (_previous_stats) {
    _env->DeleteGlobalRef(_previous_stats);
  }

  if (_current_stats) {
    _env->DeleteGlobalRef(_current_stats);
  }
}

void FilePollCallbacks::initialize(JNIEnv* env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

void FilePollCallbacks::on_poll(int status, const uv_statbuf_t* previous, const uv_statbuf_t* current) {
  assert(_env);

  Stats::update(_env, _previous_stats, previous);
  Stats::update(_env, _current_stats, current);

  _env->CallVoidMethod(
      _instance,
      _file_poll_callback_mid,
      status);
}

void FilePollCallbacks::on_stop() {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _file_poll_stop_callback_mid);
}

void FilePollCallbacks::set_stats(jobject previous, jobject current) {
  _previous_stats = _env->NewGlobalRef(previous);
  _current_stats = _env->NewGlobalRef(current);
}

static void _poll_cb(uv_fs_poll_t* handle, int status, const uv_statbuf_t* previous, const uv_statbuf_t* current) {
  assert(handle);
  assert(handle->data);
  FilePollCallbacks* cb = reinterpret_cast<FilePollCallbacks*>(handle->data);
  cb->on_poll(status, previous, current);
}

static void _stop_cb(uv_handle_t* handle) {
  assert(handle);
  assert(handle->data);
  FilePollCallbacks* cb = reinterpret_cast<FilePollCallbacks*>(handle->data);
  cb->on_stop();
  delete cb;
  delete handle;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FilePollHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_iwebpp_libuvpp_handles_FilePollHandle__1new
  (JNIEnv *env, jclass cls, jlong loop) {

  assert(loop);
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  uv_fs_poll_t* fs_poll = new uv_fs_poll_t();
  int r = uv_fs_poll_init(lp, fs_poll);
  if (r) {
    ThrowException(env, fs_poll->loop, "uv_fs_poll_init");
  } else {
    fs_poll->data = new FilePollCallbacks();
  }
  return reinterpret_cast<jlong>(fs_poll);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FilePollHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_iwebpp_libuvpp_handles_FilePollHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  FilePollCallbacks::static_initialize(env, cls);
  Stats::static_initialize(env);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FilePollHandle
 * Method:    _initialize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_iwebpp_libuvpp_handles_FilePollHandle__1initialize
  (JNIEnv *env, jobject that, jlong fs_poll_ptr) {

  assert(fs_poll_ptr);
  uv_fs_poll_t* handle = reinterpret_cast<uv_fs_poll_t*>(fs_poll_ptr);
  assert(handle->data);
  FilePollCallbacks* cb = reinterpret_cast<FilePollCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FilePollHandle
 * Method:    _start
 * Signature: (JLjava/lang/String;ZILcom/oracle/libuv/Stats;Lcom/oracle/libuv/Stats;)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_FilePollHandle__1start
  (JNIEnv *env, jobject that, jlong fs_poll_ptr, jstring path, jboolean persistent, jint interval, jobject previous, jobject current) {

  assert(fs_poll_ptr);
  uv_fs_poll_t* handle = reinterpret_cast<uv_fs_poll_t*>(fs_poll_ptr);

  FilePollCallbacks* cb = reinterpret_cast<FilePollCallbacks*>(handle->data);
  cb->set_stats(previous, current);

  const char* cpath = env->GetStringUTFChars(path, 0);
  if (!persistent) {
      uv_unref(reinterpret_cast<uv_handle_t*>(fs_poll_ptr));
  }

  int r = uv_fs_poll_start(handle, _poll_cb, cpath, interval);
  if (r) {
    ThrowException(env, handle->loop, "uv_fs_poll_start");
  }
  env->ReleaseStringUTFChars(path, cpath);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FilePollHandle
 * Method:    _stop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_FilePollHandle__1stop
  (JNIEnv *env, jobject that, jlong fs_poll_ptr) {

  assert(fs_poll_ptr);
  uv_fs_poll_t* fs_poll = reinterpret_cast<uv_fs_poll_t*>(fs_poll_ptr);
  int r = uv_fs_poll_stop(fs_poll);
  if (r) {
    ThrowException(env, fs_poll->loop, "uv_fs_poll_stop");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_FilePollHandle
 * Method:    _close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_iwebpp_libuvpp_handles_FilePollHandle__1close
  (JNIEnv *env, jobject that, jlong fs_poll_ptr) {

  assert(fs_poll_ptr);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(fs_poll_ptr);
  uv_close(handle, _stop_cb);
}
