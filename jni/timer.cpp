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
///#include "com_oracle_libuv_handles_TimerHandle.h"

class TimerCallbacks {
private:
  static jclass _timer_handle_cid;

  static jmethodID _callback_mid;

  JNIEnv* _env;
  jobject _instance;

public:
  static void static_initialize(JNIEnv* env, jclass cls);

  TimerCallbacks();
  ~TimerCallbacks();

  void initialize(JNIEnv *env, jobject instance);

  void on_timer(int status);
  void on_close();
};

typedef enum {
  TIMER_FIRED_CALLBACK = 1,
  TIMER_CLOSE_CALLBACK
} TimerHandleCallbackType;

jclass TimerCallbacks::_timer_handle_cid = NULL;

jmethodID TimerCallbacks::_callback_mid = NULL;

void TimerCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _timer_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_timer_handle_cid);

  _callback_mid = env->GetMethodID(_timer_handle_cid, "callback", "(II)V");
  assert(_callback_mid);
}

void TimerCallbacks::initialize(JNIEnv *env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

TimerCallbacks::TimerCallbacks() {
  _env = NULL;
}

TimerCallbacks::~TimerCallbacks() {
  _env->DeleteGlobalRef(_instance);
}

void TimerCallbacks::on_timer(int status) {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _callback_mid,
      TIMER_FIRED_CALLBACK,
      status);
}

void TimerCallbacks::on_close() {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _callback_mid,
      TIMER_CLOSE_CALLBACK,
      0);
}

static void _timer_cb(uv_timer_t* handle, int status) {
  assert(handle);
  assert(handle->data);
  TimerCallbacks* cb = reinterpret_cast<TimerCallbacks*>(handle->data);
  cb->on_timer(status);
}

static void _close_cb(uv_handle_t* handle) {
  assert(handle);
  assert(handle->data);
  TimerCallbacks* cb = reinterpret_cast<TimerCallbacks*>(handle->data);
  cb->on_close();
  delete cb;
  delete handle;
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_libuv_handles_TimerHandle__1new
  (JNIEnv *env, jclass cls, jlong loop) {

  assert(loop);
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  uv_timer_t* timer = new uv_timer_t();
  int r = uv_timer_init(lp, timer);
  if (r) {
    ThrowException(env, timer->loop, "uv_timer_init");
  } else {
    timer->data = new TimerCallbacks();
  }
  return reinterpret_cast<jlong>(timer);
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_TimerHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  TimerCallbacks::static_initialize(env, cls);
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _initialize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_TimerHandle__1initialize
  (JNIEnv *env, jobject that, jlong timer) {

  assert(timer);
  uv_timer_t* handle = reinterpret_cast<uv_timer_t*>(timer);
  assert(handle->data);
  TimerCallbacks* cb = reinterpret_cast<TimerCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _start
 * Signature: (JJJ)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_TimerHandle__1start
  (JNIEnv *env, jobject that, jlong timer, jlong timeout, jlong repeat) {

  assert(timer);
  uv_timer_t* handle = reinterpret_cast<uv_timer_t*>(timer);
  int r = uv_timer_start(handle, _timer_cb, static_cast<uint64_t>(timeout), static_cast<uint64_t>(repeat));
  if (r) {
    ThrowException(env, handle->loop, "uv_timer_start");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _again
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_TimerHandle__1again
  (JNIEnv *env, jobject that, jlong timer) {

  assert(timer);
  uv_timer_t* handle = reinterpret_cast<uv_timer_t*>(timer);
  int r = uv_timer_again(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_timer_again");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _get_repeat
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_libuv_handles_TimerHandle__1get_1repeat
  (JNIEnv *env, jobject that, jlong timer) {

  assert(timer);
  uv_timer_t* handle = reinterpret_cast<uv_timer_t*>(timer);
  return uv_timer_get_repeat(handle);
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _set_repeat
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_TimerHandle__1set_1repeat
  (JNIEnv *env, jobject that, jlong timer, jlong repeat) {

  assert(timer);
  uv_timer_t* handle = reinterpret_cast<uv_timer_t*>(timer);
  uv_timer_set_repeat(handle, static_cast<uint64_t>(repeat));
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _stop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_TimerHandle__1stop
  (JNIEnv *env, jobject that, jlong timer) {

  assert(timer);
  uv_timer_t* handle = reinterpret_cast<uv_timer_t*>(timer);
  int r = uv_timer_stop(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_timer_stop");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_TimerHandle
 * Method:    _close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_TimerHandle__1close
  (JNIEnv *env, jobject that, jlong timer) {

  assert(timer);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(timer);
  uv_close(handle, _close_cb);
}
