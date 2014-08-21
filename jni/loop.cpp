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

#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <vector>

#include "uv.h"
#include "exception.h"
#include "handle.h"
///#include "com_oracle_libuv_handles_LoopHandle.h"

static jclass _string_cid = NULL;

static void _close_cb(uv_handle_t* handle) {
}

static void _close_all_cb(uv_handle_t* handle, void* arg) {
  if (!uv_is_closing(handle)) {
    uv_close(handle, _close_cb);
  }
}

static void _list_cb(uv_handle_t* handle, void* arg) {
    const char* s = handle_to_string(handle);
    std::vector<const char*>* bag = static_cast<std::vector<const char*>*>(arg);
    bag->push_back(s);
}

/*
 * Class:     com_oracle_libuv_handles_LoopHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_LoopHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  _string_cid = env->FindClass("java/lang/String");
  assert(_string_cid);
  _string_cid = (jclass) env->NewGlobalRef(_string_cid);
  assert(_string_cid);
}

/*
 * Class:     com_oracle_libuv_handles_LoopHandle
 * Method:    _new
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_libuv_handles_LoopHandle__1new
  (JNIEnv *env, jclass cls) {

  uv_loop_t* ptr = uv_loop_new();
  assert(ptr);
  return reinterpret_cast<jlong>(ptr);
}

/*
 * Class:     com_oracle_libuv_handles_LoopHandle
 * Method:    _run
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_LoopHandle__1run
  (JNIEnv *env, jobject that, jlong ptr, jint mode) {

  assert(ptr);
  ///return uv_run(reinterpret_cast<uv_loop_t*>(ptr), (uv_run_mode) mode);
  return uv_run(reinterpret_cast<uv_loop_t*>(ptr));
}

/*
 * Class:     com_oracle_libuv_handles_LoopHandle
 * Method:    _stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_LoopHandle__1stop
  (JNIEnv *env, jobject that, jlong ptr) {

  assert(ptr);
  ///uv_stop(reinterpret_cast<uv_loop_t*>(ptr));
}

/*
 * Class:     com_oracle_libuv_handles_LoopHandle
 * Method:    _destroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_LoopHandle__1destroy
  (JNIEnv *env, jobject that, jlong ptr) {

  assert(ptr);
  uv_loop_t* handle = reinterpret_cast<uv_loop_t*>(ptr);
  uv_loop_delete(handle);
}

/*
 * Class:     com_oracle_libuv_handles_LoopHandle
 * Method:    _close_all
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_LoopHandle__1close_1all
  (JNIEnv *env, jobject that, jlong ptr) {

  assert(ptr);
  uv_loop_t* loop = reinterpret_cast<uv_loop_t*>(ptr);
  uv_walk(loop, _close_all_cb, NULL);
}

/*
 * Class:     com_oracle_libuv_handles_LoopHandle
 * Method:    _list
 * Signature: (J)[Ljava/lang/String
 */
JNIEXPORT jobjectArray JNICALL Java_com_oracle_libuv_handles_LoopHandle__1list
  (JNIEnv *env, jobject that, jlong ptr) {

  assert(ptr);
  assert(_string_cid);
  uv_loop_t* loop = reinterpret_cast<uv_loop_t*>(ptr);
  std::vector<const char*> bag;
  uv_walk(loop, _list_cb, &bag);
  jsize size = static_cast<jsize>(bag.size());
  jobjectArray handles = env->NewObjectArray(size, _string_cid, 0);
  OOMN(env, handles);
  for (int i=0; i < size; i++) {
    jstring s = env->NewStringUTF(bag[i]);
    OOMN(env, s);
    env->SetObjectArrayElement(handles, i, s);
    env->DeleteLocalRef(s);
  }
  return handles;
}

/*
 * Class:     com_oracle_libuv_handles_LoopHandle
 * Method:    _get_last_error
 * Signature: (J)Lcom/oracle/libuv/NativeException;
 */
JNIEXPORT jthrowable JNICALL Java_com_oracle_libuv_handles_LoopHandle__1get_1last_1error
  (JNIEnv *env, jobject that, jlong ptr) {

  assert(ptr);
  uv_loop_t* loop = reinterpret_cast<uv_loop_t*>(ptr);
  int code = uv_last_error(loop).code;

  return NewException(env, code);
}
