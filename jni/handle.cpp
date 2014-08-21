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
#include <string.h>

#include "uv.h"
#include "exception.h"
#include "handle.h"
///#include "com_oracle_libuv_handles_Handle.h"

const char* handle_typeof(const uv_handle_t* handle) {
    switch (handle->type) {
        case UV_ASYNC: return "ASYNC";
        case UV_CHECK: return "CHECK";
        case UV_FILE: return "FILE";
        case UV_FS_EVENT: return "FS_EVENT";
        case UV_FS_POLL: return "FS_POLL";
        ///case UV_HANDLE: return "HANDLE";
        case UV_IDLE: return "IDLE";
        case UV_NAMED_PIPE: return "PIPE";
        case UV_POLL: return "POLL";
        case UV_PREPARE: return "PREPARE";
        case UV_PROCESS: return "PROCESS";
        ///case UV_STREAM: return "STREAM";
        case UV_TCP: return "TCP";
        case UV_TIMER: return "TIMER";
        case UV_TTY: return "TTY";
        case UV_UDP: return "UDP";
        ///case UV_SIGNAL: return "SIGNAL";
        case UV_UNKNOWN_HANDLE: return "UNKNOWN";
        case UV_HANDLE_TYPE_MAX: assert(0);
    }
    return "<?>";
}

const char* handle_to_string(const uv_handle_t* handle) {
    const char* type = handle_typeof(handle);
    const size_t size = strlen(type) + 16;
    char* buffer = new char[size];
    memset(buffer, 0, size);
    sprintf(buffer, "%s.%lx", type, reinterpret_cast<unsigned long>(handle));
    return buffer;
}

/*
 * Class:     com_oracle_libuv_handles_Handle
 * Method:    _ref
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_Handle__1ref
  (JNIEnv *env, jobject that, jlong ptr) {

  assert(ptr);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(ptr);
  uv_ref(handle);
}

/*
 * Class:     com_oracle_libuv_handles_Handle
 * Method:    _unref
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_Handle__1unref
  (JNIEnv *env, jobject that, jlong ptr) {

  assert(ptr);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(ptr);
  uv_unref(handle);
}

/*
 * Class:     com_oracle_libuv_handles_Handle
 * Method:    _closing
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_oracle_libuv_handles_Handle__1closing
  (JNIEnv *env, jobject that, jlong ptr) {

  assert(ptr);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(ptr);
  int r = uv_is_closing(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_is_closing");
  }
  return r == 0 ? JNI_TRUE : JNI_FALSE;
}

