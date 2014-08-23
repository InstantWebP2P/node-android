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

#include <assert.h>

#include "uv.h"
#include "exception.h"
#include "context.h"
#include "stream.h"
///#include "com_iwebpp_libuvpp_handles_PipeHandle.h"

static void _pipe_connect_cb(uv_connect_t* req, int status) {
  assert(req);
  assert(req->data);
  assert(req->handle);
  assert(req->handle->data);
  ContextHolder* req_data = reinterpret_cast<ContextHolder*>(req->data);
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(req->handle->data);
  cb->on_connect(status, status < 0 ? uv_last_error(req->handle->loop).code : 0, req_data->context());
  delete req_data;
  delete req;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_PipeHandle
 * Method:    _new
 * Signature: (JZ)J
 */
JNIEXPORT jlong JNICALL Java_com_iwebpp_libuvpp_handles_PipeHandle__1new
  (JNIEnv *env, jclass cls, jlong loop, jboolean ipc) {

  assert(loop);
  uv_pipe_t* pipe = new uv_pipe_t();
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  int r = uv_pipe_init(lp, pipe, ipc == JNI_FALSE ? 0 : 1);
  if (r) {
    ThrowException(env, lp, "uv_pipe_init");
    return (jlong) NULL;
  }
  assert(pipe);
  pipe->data = new StreamCallbacks();
  return reinterpret_cast<jlong>(pipe);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_PipeHandle
 * Method:    _open
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_PipeHandle__1open
  (JNIEnv *env, jobject that, jlong pipe, jint fd) {

  assert(pipe);
  uv_pipe_t* handle = reinterpret_cast<uv_pipe_t*>(pipe);
  int r = 0;uv_pipe_open(handle, fd);
  if (r) {
    ThrowException(env, handle->loop, "uv_pipe_open");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_PipeHandle
 * Method:    _bind
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_PipeHandle__1bind
  (JNIEnv *env, jobject that, jlong pipe, jstring name) {

  assert(pipe);
  uv_pipe_t* handle = reinterpret_cast<uv_pipe_t*>(pipe);
  const char* n = env->GetStringUTFChars(name, 0);
  int r = uv_pipe_bind(handle, n);
  if (r) {
    ThrowException(env, handle->loop, "uv_pipe_bind", n);
  }
  env->ReleaseStringUTFChars(name, n);
  return r;
 }

/*
 * Class:     com_iwebpp_libuvpp_handles_PipeHandle
 * Method:    _connect
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_iwebpp_libuvpp_handles_PipeHandle__1connect
  (JNIEnv *env, jobject that, jlong pipe, jstring name, jobject context) {

  assert(pipe);
  uv_pipe_t* handle = reinterpret_cast<uv_pipe_t*>(pipe);
  uv_connect_t* connect = new uv_connect_t();
  connect->data = new ContextHolder(env, context);
  connect->handle = reinterpret_cast<uv_stream_t*>(handle);
  const char *pipeName = env->GetStringUTFChars(name, 0);
  uv_pipe_connect(connect, handle, pipeName, _pipe_connect_cb);
  env->ReleaseStringUTFChars(name, pipeName);
}
