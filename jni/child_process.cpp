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

#include "uv.h"
#include "exception.h"
///#include "com_oracle_libuv_handles_ProcessHandle.h"

class ProcessCallbacks {
private:
  static jclass _process_handle_cid;

  static jmethodID _process_close_mid;
  static jmethodID _process_exit_mid;

  JNIEnv* _env;
  jobject _instance;

public:
  static void static_initialize(JNIEnv* env, jclass cls);

  ProcessCallbacks();
  ~ProcessCallbacks();

  void initialize(JNIEnv* env, jobject instance);

  void on_exit(int status, int signal);
  void on_exit(int status, int signal, int error_code);
  void on_close();
};

typedef enum {
  PROCESS_CLOSE_CALLBACK = 1,
  PROCESS_EXIT_CALLBACK
} ProcessHandleCallbackType;

jclass ProcessCallbacks::_process_handle_cid = NULL;

jmethodID ProcessCallbacks::_process_close_mid = NULL;
jmethodID ProcessCallbacks::_process_exit_mid = NULL;

void ProcessCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _process_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_process_handle_cid);

  _process_close_mid = env->GetMethodID(_process_handle_cid, "callClose", "()V");
  assert(_process_close_mid);
  _process_exit_mid = env->GetMethodID(_process_handle_cid, "callExit", "(IILjava/lang/Exception;)V");
  assert(_process_exit_mid);
}

void ProcessCallbacks::initialize(JNIEnv* env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

ProcessCallbacks::ProcessCallbacks() {
}

ProcessCallbacks::~ProcessCallbacks() {
  _env->DeleteGlobalRef(_instance);
}

void ProcessCallbacks::on_exit(int status, int signal) {
  assert(_env);

  _env->CallVoidMethod(
      _instance,
      _process_exit_mid,
      status,
      signal,
      NULL);
}

void ProcessCallbacks::on_exit(int status, int signal, int error_code) {
  assert(_env);
  assert(status < 0);

  jthrowable exception = NewException(_env, error_code);
  _env->CallVoidMethod(
      _instance,
      _process_exit_mid,
      status,
      signal,
      exception);
  if (exception) { _env->DeleteLocalRef(exception); }
}

void ProcessCallbacks::on_close() {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _process_close_mid);
}

static void _close_cb(uv_handle_t* handle) {
  assert(handle);
  assert(handle->data);
  ProcessCallbacks* cb = reinterpret_cast<ProcessCallbacks*>(handle->data);
  cb->on_close();
  delete cb;
  delete handle;
}

static void _exit_cb(uv_process_t* process, int exit_status, int term_signal) {
  assert(process);
  uv_process_t* handle = reinterpret_cast<uv_process_t*>(process);
  assert(handle->data);
  ProcessCallbacks* cb = reinterpret_cast<ProcessCallbacks*>(handle->data);
  if (exit_status < 0) {
    int error_code = uv_last_error(handle->loop).code;
    cb->on_exit(exit_status, term_signal, error_code);
  } else {
    cb->on_exit(exit_status, term_signal);
  }
}

/*
 * Class:     com_oracle_libuv_handles_ProcessHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_libuv_handles_ProcessHandle__1new
  (JNIEnv *env, jclass cls, jlong loop) {

  uv_process_t* process = new uv_process_t();
  assert(process);
  process->loop = reinterpret_cast<uv_loop_t*>(loop);
  process->data = new ProcessCallbacks();
  return reinterpret_cast<jlong>(process);
}

/*
 * Class:     com_oracle_libuv_handles_ProcessHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_ProcessHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  ProcessCallbacks::static_initialize(env, cls);
}

/*
 * Class:     com_oracle_libuv_handles_ProcessHandle
 * Method:    _initialize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_ProcessHandle__1initialize
  (JNIEnv *env, jobject that, jlong process) {

  assert(process);
  uv_process_t* handle = reinterpret_cast<uv_process_t*>(process);
  assert(handle->data);
  ProcessCallbacks* cb = reinterpret_cast<ProcessCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_oracle_libuv_handles_ProcessHandle
 * Method:    _close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_ProcessHandle__1close
  (JNIEnv *env, jobject that, jlong process) {

  assert(process);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(process);
  uv_close(handle, _close_cb);
}

/*
 * Class:     com_oracle_libuv_handles_ProcessHandle
 * Method:    _spawn
 * Signature: (JLjava/lang/String;[Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;I[I[J[III)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_ProcessHandle__1spawn
  (JNIEnv *env, jobject that, jlong process, jstring program, jobjectArray args, jobjectArray environ,
    jstring dir, jint process_flags, jintArray stdio_flags, jlongArray streams, jintArray fds, jint uid, jint gid) {

  assert(process);
  uv_process_t* handle = reinterpret_cast<uv_process_t*>(process);
  assert(handle->loop);

  uv_process_options_t options;
  memset(&options, 0, sizeof(uv_process_options_t));

  options.exit_cb = _exit_cb;
  const char* program_chars = env->GetStringUTFChars(program, 0);
  options.file = program_chars;
  const char* dir_chars = dir ? env->GetStringUTFChars(dir, 0) : NULL;
  options.cwd = (char*) dir_chars;
  options.flags = process_flags;

  if (uid != -1) {
    if (uid & ~((uv_uid_t) ~0)) {
        ThrowException(env, handle->loop, "uv_spawn", "uid is out of range");
    } else {
        options.flags |= UV_PROCESS_SETUID;
        options.uid = (uv_uid_t) uid;
    }
  }

  if (gid != -1) {
    if (gid & ~((uv_gid_t) ~0)) {
      ThrowException(env, handle->loop, "uv_spawn", "gid is out of range");
    } else {
      options.flags |= UV_PROCESS_SETGID;
      options.gid = (uv_gid_t) gid;
    }
  }

  jsize stdio_flags_len = 0;
  int* flag = NULL;
  jlong* stream = NULL;
  int* fd = NULL;

  assert(stdio_flags);
  stdio_flags_len = env->GetArrayLength(stdio_flags);
  options.stdio = new uv_stdio_container_t[stdio_flags_len];
  options.stdio_count = stdio_flags_len;
  flag = stdio_flags ? (int*) env->GetIntArrayElements(stdio_flags, 0) : NULL;
  stream = streams ? env->GetLongArrayElements(streams, 0) : NULL;
  fd = fds ? (int*) env->GetIntArrayElements(fds, 0) : NULL;

  for (int i=0; i < stdio_flags_len; i++) {
    if (flag[i] == UV_IGNORE) {
      options.stdio[i].flags = UV_IGNORE;
    } else if (flag[i] == UV_CREATE_PIPE) {
      options.stdio[i].flags = static_cast<uv_stdio_flags>(UV_CREATE_PIPE | UV_READABLE_PIPE | UV_WRITABLE_PIPE);
      options.stdio[i].data.stream = reinterpret_cast<uv_stream_t*>(stream[i]);
    } else if (flag[i] == UV_INHERIT_STREAM) {
      options.stdio[i].flags = UV_INHERIT_STREAM;
      options.stdio[i].data.stream = reinterpret_cast<uv_stream_t*>(stream[i]);
    } else {
      options.stdio[i].flags = UV_INHERIT_FD;
      options.stdio[i].data.fd = fd[i];
    }
  }

  jsize args_len = 0;
  if (args) {
    args_len = env->GetArrayLength(args);
    options.args = new char*[args_len + 1];
    for (int i=0; i < args_len; i++) {
      jstring element = (jstring) env->GetObjectArrayElement(args, i);
      options.args[i] = (char*) env->GetStringUTFChars(element, 0);
    }
    options.args[args_len] = NULL;
  }

  jsize env_len = 0;
  if (environ) {
    env_len = env->GetArrayLength(environ);
    options.env = new char*[env_len + 1];
    for (int i=0; i < env_len; i++) {
      jstring element = (jstring) env->GetObjectArrayElement(environ, i);
      options.env[i] = (char*) env->GetStringUTFChars(element, 0);
    }
    options.env[env_len] = NULL;
  }

  if (process_flags & UV_PROCESS_WINDOWS_VERBATIM_ARGUMENTS) {
    options.flags |= UV_PROCESS_WINDOWS_VERBATIM_ARGUMENTS;
  }

  if (process_flags & UV_PROCESS_DETACHED) {
    options.flags |= UV_PROCESS_DETACHED;
  }

  int r = uv_spawn(handle->loop, handle, options);
  if (r) {
    ThrowException(env, handle->loop, "uv_spawn", program_chars);
  } else {
    r = handle->pid;
  }

  env->ReleaseStringUTFChars(program, program_chars);
  if (dir) {
    env->ReleaseStringUTFChars(dir, dir_chars);
  }

  if (flag) {
    env->ReleaseIntArrayElements(stdio_flags, (jint*) flag, 0);
  }
  if (stream) {
    env->ReleaseLongArrayElements(streams, stream, 0);
  }
  if (fd) {
    env->ReleaseIntArrayElements(fds, (jint*) fd, 0);
  }
  if (options.stdio) {
    delete[] options.stdio;
  }

  for (int i=0; i < args_len; i++) {
    jstring element = (jstring) env->GetObjectArrayElement(args, i);
    env->ReleaseStringUTFChars(element, options.args[i]);
  }
  if (args) {
    delete[] options.args;
  }

  for (int i=0; i < env_len; i++) {
    jstring element = (jstring) env->GetObjectArrayElement(environ, i);
    env->ReleaseStringUTFChars(element, options.env[i]);
  }
  if (environ) {
    delete[] options.env;
  }

  return r;
}

/*
 * Class:     com_oracle_libuv_handles_ProcessHandle
 * Method:    _kill
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_ProcessHandle__1kill
  (JNIEnv *env, jobject that, jlong ptr, jint signal) {

  assert(ptr);
  uv_process_t* handle = reinterpret_cast<uv_process_t*>(ptr);
  int r = uv_process_kill(handle, signal);
  if (r) {
    ThrowException(env, handle->loop, "uv_process_kill", "error killing process");
  }
  return r;
}
