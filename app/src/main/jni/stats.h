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

#ifndef _libuv_java_stats_h_
#define _libuv_java_stats_h_

#include <jni.h>

#include "uv.h"

class Stats {
private:
  static jclass _stats_cid;
  static jmethodID _stats_init_mid;
  static jmethodID _stats_set_mid;

public:
  static void static_initialize(JNIEnv* env);
  static jobject create(JNIEnv* env, const uv_statbuf_t* ptr);
  static void update(JNIEnv* env, jobject stats, const uv_statbuf_t* ptr);

  Stats();
  ~Stats();
};

#endif // _libuv_java_stats_h_
