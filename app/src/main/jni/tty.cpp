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
#include "stream.h"
///#include "com_iwebpp_libuvpp_handles_TTYHandle.h"

static jstring _FILE = NULL;
static jstring _PIPE = NULL;
static jstring _TCP = NULL;
static jstring _TTY = NULL;
static jstring _UDP = NULL;
static jstring _UNKNOWN = NULL;

/*
 * Class:     com_iwebpp_libuvpp_handles_TTYHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_TTYHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  _FILE = env->NewStringUTF("FILE");
  _FILE = (jstring) env->NewGlobalRef(_FILE);

  _PIPE = env->NewStringUTF("PIPE");
  _PIPE = (jstring) env->NewGlobalRef(_PIPE);

  _TCP = env->NewStringUTF("TCP");
  _TCP = (jstring) env->NewGlobalRef(_TCP);

  _TTY = env->NewStringUTF("TTY");
  _TTY = (jstring) env->NewGlobalRef(_TTY);

  _UDP = env->NewStringUTF("UDP");
  _UDP = (jstring) env->NewGlobalRef(_UDP);

  _UNKNOWN = env->NewStringUTF("UNKNOWN");
  _UNKNOWN = (jstring) env->NewGlobalRef(_UNKNOWN);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TTYHandle
 * Method:    _new
 * Signature: (JIZ)J
 */
extern "C" JNIEXPORT  jlong JNICALL Java_com_iwebpp_libuvpp_handles_TTYHandle__1new
  (JNIEnv *env, jclass cls, jlong loop, jint fd, jboolean readable) {

  uv_tty_t* tty = new uv_tty_t();
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  int r = uv_tty_init(lp, tty, fd, readable);
  if (r) {
    ThrowException(env, lp, "uv_tty_init");
    return (jlong) NULL;
  }
  tty->data = new StreamCallbacks();
  return reinterpret_cast<jlong>(tty);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TTYHandle
 * Method:    _set_mode
 * Signature: (JI)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_TTYHandle__1set_1mode
  (JNIEnv *env, jobject that, jlong tty, jint mode) {

  assert(tty);
  uv_tty_t* handle = reinterpret_cast<uv_tty_t*>(tty);
  int r = uv_tty_set_mode(handle, mode);
  if (r) {
    ThrowException(env, handle->loop, "uv_tty_set_mode");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TTYHandle
 * Method:    _reset_mode
 * Signature: (J)
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_TTYHandle__1reset_1mode
  (JNIEnv *env, jobject that, jlong tty) {

  assert(tty);
  uv_tty_reset_mode();
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TTYHandle
 * Method:    _get_window_size
 * Signature: (J)[I
 */
extern "C" JNIEXPORT  jintArray JNICALL Java_com_iwebpp_libuvpp_handles_TTYHandle__1get_1window_1size
  (JNIEnv *env, jobject that, jlong tty) {

  assert(tty);
  uv_tty_t* handle = reinterpret_cast<uv_tty_t*>(tty);
  int width = 0;
  int height = 0;
  int r = uv_tty_get_winsize(handle, &width, &height);
  if (r) {
    ThrowException(env, handle->loop, "uv_tty_set_mode");
    return NULL;
  }
  jintArray size = env->NewIntArray(2);
  OOMN(env, size);
  env->SetIntArrayRegion(size, 0, 1, reinterpret_cast<const jint*>(&width));
  env->SetIntArrayRegion(size, 1, 1, reinterpret_cast<const jint*>(&height));
  return size;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TTYHandle
 * Method:    _is_tty
 * Signature: (I)Z
 */
extern "C" JNIEXPORT  jboolean JNICALL Java_com_iwebpp_libuvpp_handles_TTYHandle__1is_1tty
  (JNIEnv *env, jclass cls, jint fd) {

  return uv_guess_handle(fd) == UV_TTY ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TTYHandle
 * Method:    _guess_handle_type
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT  jstring JNICALL Java_com_iwebpp_libuvpp_handles_TTYHandle__1guess_1handle_1type
  (JNIEnv *env, jclass cls, jint fd) {

  uv_handle_type type = uv_guess_handle(fd);
  switch (type) {
    case UV_TCP:
      return _TCP;

    case UV_TTY:
      return _TTY;

    case UV_UDP:
      return _UDP;

    case UV_NAMED_PIPE:
      return _PIPE;

    case UV_FILE:
      return _FILE;

    case UV_UNKNOWN_HANDLE:
      return _UNKNOWN;

    default:
      assert(0);
  }
  return NULL;
}
