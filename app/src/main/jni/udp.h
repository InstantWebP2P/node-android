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

#ifndef _libuv_java_udp_h_
#define _libuv_java_udp_h_

#include <jni.h>

#include "uv.h"

class UDPCallbacks {
private:
  static jclass _udp_handle_cid;

  static jmethodID _recv_callback_mid;
  static jmethodID _send_callback_mid;
  static jmethodID _close_callback_mid;

  JNIEnv* _env;
  jobject _instance;

public:
  static void static_initialize(JNIEnv* env, jclass cls);

  UDPCallbacks();
  ~UDPCallbacks();

  void initialize(JNIEnv *env, jobject instance);

  void on_recv(ssize_t nread, uv_buf_t buf, struct sockaddr* addr, unsigned flags);
  void on_send(int status, int error_code, jobject buffer, jobject domain);
  void on_close();
};

#endif // _libuv_java_udp_h_
