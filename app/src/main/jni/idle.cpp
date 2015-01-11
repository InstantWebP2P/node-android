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
///#include "com_iwebpp_libuvpp_handles_IdleHandle.h"

class IdleCallbacks {
private:
  static jclass _idle_handle_cid;

  static jmethodID _callback_mid;

  JNIEnv* _env;
  jobject _instance;

public:
  static void static_initialize(JNIEnv* env, jclass cls);

  IdleCallbacks();
  ~IdleCallbacks();

  void initialize(JNIEnv* env, jobject instance);

  void on_idle(int status);
  void on_close();
};

typedef enum {
  IDLE_CALLBACK = 1,
  IDLE_CLOSE_CALLBACK
} IdleHandleCallbackType;

jclass IdleCallbacks::_idle_handle_cid = NULL;

jmethodID IdleCallbacks::_callback_mid = NULL;

void IdleCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _idle_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_idle_handle_cid);

  _callback_mid = env->GetMethodID(_idle_handle_cid, "callback", "(II)V");
  assert(_callback_mid);
}

void IdleCallbacks::initialize(JNIEnv* env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

IdleCallbacks::IdleCallbacks() {
  _env = NULL;
}

IdleCallbacks::~IdleCallbacks() {
  _env->DeleteGlobalRef(_instance);
}

void IdleCallbacks::on_idle(int status) {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _callback_mid,
      IDLE_CALLBACK,
      status);
}

void IdleCallbacks::on_close() {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _callback_mid,
      IDLE_CLOSE_CALLBACK,
      0);
}

static void _idle_cb(uv_idle_t* handle, int status) {
  assert(handle);
  assert(handle->data);
  IdleCallbacks* cb = reinterpret_cast<IdleCallbacks*>(handle->data);
  cb->on_idle(status);
}

static void _close_cb(uv_handle_t* handle) {
  assert(handle);
  assert(handle->data);
  IdleCallbacks* cb = reinterpret_cast<IdleCallbacks*>(handle->data);
  cb->on_close();
  delete cb;
  delete handle;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_IdleHandle
 * Method:    _new
 * Signature: (J)J
 */
extern "C" JNIEXPORT  jlong JNICALL Java_com_iwebpp_libuvpp_handles_IdleHandle__1new
  (JNIEnv *env, jclass cls, jlong loop) {

  assert(loop);
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  uv_idle_t* idle = new uv_idle_t();
  int r = uv_idle_init(lp, idle);
  if (r) {
    ThrowException(env, idle->loop, "uv_idle_init");
  } else {
    idle->data = new IdleCallbacks();
  }
  return reinterpret_cast<jlong>(idle);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_IdleHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_IdleHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  IdleCallbacks::static_initialize(env, cls);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_IdleHandle
 * Method:    _initialize
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_IdleHandle__1initialize
  (JNIEnv *env, jobject that, jlong idle) {

  assert(idle);
  uv_idle_t* handle = reinterpret_cast<uv_idle_t*>(idle);
  assert(handle->data);
  IdleCallbacks* cb = reinterpret_cast<IdleCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_IdleHandle
 * Method:    _start
 * Signature: (J)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_IdleHandle__1start
  (JNIEnv *env, jobject that, jlong idle) {

  assert(idle);
  uv_idle_t* handle = reinterpret_cast<uv_idle_t*>(idle);
  int r = uv_idle_start(handle, _idle_cb);
  if (r) {
    ThrowException(env, handle->loop, "uv_idle_start");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_IdleHandle
 * Method:    _stop
 * Signature: (J)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_IdleHandle__1stop
  (JNIEnv *env, jobject that, jlong idle) {

  assert(idle);
  uv_idle_t* handle = reinterpret_cast<uv_idle_t*>(idle);
  int r = uv_idle_stop(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_idle_stop");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_IdleHandle
 * Method:    _close
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_IdleHandle__1close
  (JNIEnv *env, jobject that, jlong idle) {

  assert(idle);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(idle);
  uv_close(handle, _close_cb);
}
