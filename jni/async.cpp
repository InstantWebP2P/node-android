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
///#include "com_iwebpp_libuvpp_handles_AsyncHandle.h"

class AsyncCallbacks {
private:
  static jclass _async_handle_cid;

  static jmethodID _send_callback_mid;

  JNIEnv* _env;
  jobject _instance;

public:
  static void static_initialize(JNIEnv* env, jclass cls);

  AsyncCallbacks();
  ~AsyncCallbacks();

  void initialize(JNIEnv *env, jobject instance);

  void on_send(int status);
};

jclass AsyncCallbacks::_async_handle_cid = NULL;

jmethodID AsyncCallbacks::_send_callback_mid = NULL;

void AsyncCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _async_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_async_handle_cid);

  _send_callback_mid = env->GetMethodID(_async_handle_cid, "callSend", "(I)V");
  assert(_send_callback_mid);
}

void AsyncCallbacks::initialize(JNIEnv *env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

AsyncCallbacks::AsyncCallbacks() {
  _env = NULL;
}

AsyncCallbacks::~AsyncCallbacks() {
  _env->DeleteGlobalRef(_instance);
}

void AsyncCallbacks::on_send(int status) {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _send_callback_mid,
      status);
}

static void _send_cb(uv_async_t* handle, int status) {
  assert(handle);
  assert(handle->data);
  AsyncCallbacks* cb = reinterpret_cast<AsyncCallbacks*>(handle->data);
  cb->on_send(status);
}

static void _close_cb(uv_handle_t* handle) {
  assert(handle);
  assert(handle->data);
  AsyncCallbacks* cb = reinterpret_cast<AsyncCallbacks*>(handle->data);
  delete cb;
  delete handle;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_AsyncHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_iwebpp_libuvpp_handles_AsyncHandle__1new
  (JNIEnv *env, jclass cls, jlong loop) {

  assert(loop);
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  uv_async_t* async = new uv_async_t();
  int r = uv_async_init(lp, async, _send_cb);
  if (r) {
    ThrowException(env, async->loop, "uv_async_init");
  } else {
    async->data = new AsyncCallbacks();
  }
  return reinterpret_cast<jlong>(async);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_AsyncHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_iwebpp_libuvpp_handles_AsyncHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  AsyncCallbacks::static_initialize(env, cls);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_AsyncHandle
 * Method:    _initialize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_iwebpp_libuvpp_handles_AsyncHandle__1initialize
  (JNIEnv *env, jobject that, jlong async) {

  assert(async);
  uv_async_t* handle = reinterpret_cast<uv_async_t*>(async);
  assert(handle->data);
  AsyncCallbacks* cb = reinterpret_cast<AsyncCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_AsyncHandle
 * Method:    _send
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_AsyncHandle__1send
  (JNIEnv *env, jobject that, jlong async) {

  assert(async);
  uv_async_t* handle = reinterpret_cast<uv_async_t*>(async);
  int r = uv_async_send(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_async_send");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_AsyncHandle
 * Method:    _close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_iwebpp_libuvpp_handles_AsyncHandle__1close
  (JNIEnv *env, jobject that, jlong async) {

  assert(async);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(async);
  uv_close(handle, _close_cb);
}
