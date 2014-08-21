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
///#include "com_oracle_libuv_handles_PollHandle.h"

class PollCallbacks {
private:
  static jclass _poll_handle_cid;

  static jmethodID _poll_callback_mid;

  JNIEnv* _env;
  jobject _instance;

public:
  static void static_initialize(JNIEnv* env, jclass cls);

  PollCallbacks();
  ~PollCallbacks();

  void initialize(JNIEnv *env, jobject instance);

  void on_poll(int status, int events);
};

jclass PollCallbacks::_poll_handle_cid = NULL;

jmethodID PollCallbacks::_poll_callback_mid = NULL;

void PollCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _poll_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_poll_handle_cid);

  _poll_callback_mid = env->GetMethodID(_poll_handle_cid, "callPoll", "(II)V");
  assert(_poll_callback_mid);
}

void PollCallbacks::initialize(JNIEnv *env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

PollCallbacks::PollCallbacks() {
  _env = NULL;
}

PollCallbacks::~PollCallbacks() {
  _env->DeleteGlobalRef(_instance);
}

void PollCallbacks::on_poll(int status, int events) {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _poll_callback_mid,
      status,
      events);
}

static void _poll_cb(uv_poll_t* handle, int status, int events) {
  assert(handle);
  assert(handle->data);
  PollCallbacks* cb = reinterpret_cast<PollCallbacks*>(handle->data);
  cb->on_poll(status, events);
}

static void _close_cb(uv_handle_t* handle) {
  assert(handle);
  assert(handle->data);
  PollCallbacks* cb = reinterpret_cast<PollCallbacks*>(handle->data);
  delete cb;
  delete handle;
}

/*
 * Class:     com_oracle_libuv_handles_PollHandle
 * Method:    _new
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_libuv_handles_PollHandle__1new__JI
  (JNIEnv *env, jclass cls, jlong loop, jint fd) {

  assert(loop);
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  uv_poll_t* poll = new uv_poll_t();
  int r = uv_poll_init(lp, poll, fd);
  if (r) {
    ThrowException(env, poll->loop, "uv_poll_init");
  } else {
    poll->data = new PollCallbacks();
  }
  return reinterpret_cast<jlong>(poll);
}

/*
 * Class:     com_oracle_libuv_handles_PollHandle
 * Method:    _new
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_libuv_handles_PollHandle__1new__JJ
  (JNIEnv *env, jclass cls, jlong loop, jlong socket) {

  assert(loop);
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  uv_poll_t* poll = new uv_poll_t();
  int r = uv_poll_init_socket(lp, poll, (uv_os_sock_t) socket);
  if (r) {
    ThrowException(env, poll->loop, "uv_poll_init_socket");
  } else {
    poll->data = new PollCallbacks();
  }
  return reinterpret_cast<jlong>(poll);
}

/*
 * Class:     com_oracle_libuv_handles_PollHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_PollHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  PollCallbacks::static_initialize(env, cls);
}

/*
 * Class:     com_oracle_libuv_handles_PollHandle
 * Method:    _initialize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_PollHandle__1initialize
  (JNIEnv *env, jobject that, jlong poll) {

  assert(poll);
  uv_poll_t* handle = reinterpret_cast<uv_poll_t*>(poll);
  assert(handle->data);
  PollCallbacks* cb = reinterpret_cast<PollCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_oracle_libuv_handles_PollHandle
 * Method:    _start
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_PollHandle__1start
  (JNIEnv *env, jobject that, jlong poll, jint events) {

  assert(poll);
  uv_poll_t* handle = reinterpret_cast<uv_poll_t*>(poll);
  int r = uv_poll_start(handle, events, _poll_cb);
  if (r) {
    ThrowException(env, handle->loop, "uv_poll_start");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_PollHandle
 * Method:    _stop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_PollHandle__1stop
  (JNIEnv *env, jobject that, jlong poll) {

  assert(poll);
  uv_poll_t* handle = reinterpret_cast<uv_poll_t*>(poll);
  int r = uv_poll_stop(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_poll_stop");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_PollHandle
 * Method:    _close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_PollHandle__1close
  (JNIEnv *env, jobject that, jlong poll) {

  assert(poll);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(poll);
  uv_close(handle, _close_cb);
}
