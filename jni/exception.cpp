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
#include <stdlib.h>
#include <errno.h>
#include <assert.h>
#include <jni.h>
#include <string>

#include "uv.h"
///#include "com_oracle_libuv_NativeException.h"

const char* get_uv_errno_string(int errorno) {
  uv_err_t err;
  memset(&err, 0, sizeof err);
  err.code = (uv_err_code)errorno;
  return uv_err_name(err);
}

const char* get_uv_errno_message(int errorno) {
  uv_err_t err;
  memset(&err, 0, sizeof err);
  err.code = (uv_err_code)errorno;
  return uv_strerror(err);
}

static inline jstring utf(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.data());
}

jthrowable NewException(JNIEnv* env, int errorno, const char *syscall, const char *msg, const char *path) {
  assert(env);

  jobject syscall_arg = syscall ? env->NewStringUTF(syscall) : NULL;

  std::string errno_message = get_uv_errno_message(errorno);
  if (!msg || !msg[0]) {
    msg = errno_message.data();
  }

  std::string errno_string = get_uv_errno_string(errorno);
  std::string message = msg;
  std::string cons1 = errno_string + ", ";
  std::string cons2 = cons1 + message;

  jclass nativeExceptionClassID = env->FindClass("com/oracle/libuv/NativeException");
  assert(nativeExceptionClassID);
  jmethodID nativeExceptionConstructorMID = env->GetMethodID(
      nativeExceptionClassID,
      "<init>",
      "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
  assert(nativeExceptionConstructorMID);

  jthrowable e;
  std::string path_str;

  jstring err_str = utf(env, errno_string);
  jstring err_msg = utf(env, errno_message);

  if (path) {
#ifdef _WIN32
    if (strncmp(path, "\\\\?\\UNC\\", 8) == 0) {
      path_str = "\\\\" + std::string(path + 8);
    } else if (strncmp(path, "\\\\?\\", 4) == 0) {
      path_str = std::string(path + 4);
    } else {
      path_str = std::string(path);
    }
#else
    path_str = std::string(path);
#endif

    std::string cons3 = cons2 + " '";
    std::string cons4 = cons3 + path_str;
    std::string cons5 = cons4 + "'";

    jstring err_cons5 = utf(env, cons5);
    jstring err_path = utf(env, path_str);

    e = (jthrowable) env->NewObject(nativeExceptionClassID, nativeExceptionConstructorMID,
        errorno, err_str, err_msg, syscall_arg, err_cons5, err_path);

    env->DeleteLocalRef(err_cons5);
    env->DeleteLocalRef(err_path);
  } else {
    jstring err_cons2 = utf(env, cons2);

    e = (jthrowable) env->NewObject(nativeExceptionClassID, nativeExceptionConstructorMID,
        errorno, err_str, err_msg, syscall_arg, err_cons2, NULL);

    env->DeleteLocalRef(err_cons2);
  }

  if (syscall_arg != NULL) {
    env->DeleteLocalRef(syscall_arg);
  }
  env->DeleteLocalRef(err_str);
  env->DeleteLocalRef(err_msg);

  return e;
}

#define MESSAGE_SIZE 4096

static char _message[MESSAGE_SIZE];
static jclass _oom_cid = NULL;

void ThrowOutOfMemoryError(JNIEnv* env, const char* func, const char* file, const char* line, const char* msg) {
#ifdef _WIN32
  const char* filename = strrchr(file, '\\');
  _snprintf_s(_message, MESSAGE_SIZE, _TRUNCATE, "%s:%s %s.%s", filename ? filename + 1 : file, line, func, msg);
#else
  const char* filename = strrchr(file, '/');
  snprintf(_message, MESSAGE_SIZE, "%s:%s %s.%s", filename ? filename + 1 : file, line, func, msg);
#endif

  assert(_oom_cid);
  env->ThrowNew(_oom_cid, _message);
}

/*
 * Class:     com_oracle_libuv_NativeException
 * Method:    _static_initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_NativeException__1static_1initialize
  (JNIEnv* env, jclass cls) {

  assert(!_oom_cid);

  _oom_cid = env->FindClass("java/lang/OutOfMemoryError");
  assert(_oom_cid);
  _oom_cid = (jclass) env->NewGlobalRef(_oom_cid);
  assert(_oom_cid);
}
