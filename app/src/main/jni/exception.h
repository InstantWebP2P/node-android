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

#include <jni.h>
#include <string>

#ifndef _libuv_java_throw_h_
#define _libuv_java_throw_h_

#ifndef FUNCTION_NAME
  #ifdef _WIN32
    #define FUNCTION_NAME   __FUNCTION__
  #else
    #define FUNCTION_NAME   __func__
  #endif
#endif

#define STRINGIFY(x) #x
#define TOSTRING(x) STRINGIFY(x)

#define OOM(e, p) \
  if ((!p)) { \
    ThrowOutOfMemoryError((e), (FUNCTION_NAME), (__FILE__), (TOSTRING(__LINE__)), (#p)); \
    return; \
  }

#define OOME(e, p) \
  if ((!p)) { \
    ThrowOutOfMemoryError((e), (FUNCTION_NAME), (__FILE__), (TOSTRING(__LINE__)), (#p)); \
    return -1; \
  }

#define OOMN(e, p) \
  if ((!p)) { \
    ThrowOutOfMemoryError((e), (FUNCTION_NAME), (__FILE__), (TOSTRING(__LINE__)), (#p)); \
    return NULL; \
  }

const char* get_uv_errno_string(int errorno);
const char* get_uv_errno_message(int errorno);
jstring utf(JNIEnv* env, const std::string& s);
jthrowable NewException(JNIEnv* env, int errorno, const char *syscall, const char *msg, const char *path);
void ThrowOutOfMemoryError(JNIEnv* env, const char* func, const char* file, const char* line, const char* msg);

inline jthrowable NewException(JNIEnv* env, int errorno) {
  return NewException(env, errorno, NULL, NULL, NULL);
}

inline void ThrowException(JNIEnv* env, int errorno, const char *syscall, const char *msg, const char *path) {
  env->Throw(NewException(env, errorno, syscall, msg, path));
}

inline void ThrowException(JNIEnv* env, int errorno, const char *syscall) {
  ThrowException(env, errorno, syscall, NULL, NULL);
}

inline void ThrowException(JNIEnv* env, int errorno, const char *syscall, const char *msg) {
  ThrowException(env, errorno, syscall, msg, NULL);
}

inline void ThrowException(JNIEnv* env, uv_loop_t* loop, const char *syscall) {
  ThrowException(env, uv_last_error(loop).code, syscall, NULL, NULL);
}

inline void ThrowException(JNIEnv* env, uv_loop_t* loop, const char *syscall, const char *msg) {
  ThrowException(env, uv_last_error(loop).code, syscall, msg, NULL);
}

#endif // _libuv_java_throw_h_
