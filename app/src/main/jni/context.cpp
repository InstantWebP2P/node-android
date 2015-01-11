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
#include <stdlib.h>
#include <jni.h>

#include "context.h"

ContextHolder::ContextHolder(JNIEnv* env, jobject data, jobject context) {
  _data = data ? (jobject) env->NewGlobalRef(data) : NULL;
  _env = env;
  _context = context ? (jobject) env->NewGlobalRef(context) : NULL;
  _buffers = NULL;
  _elements = NULL;
  _bases = NULL;
  _element_count = 0;
}

ContextHolder::ContextHolder(JNIEnv* env, jobject context) {
  _data = NULL;
  _env = env;
  _context = context ? (jobject) env->NewGlobalRef(context) : NULL;
  _buffers = NULL;
  _elements = NULL;
  _bases = NULL;
  _element_count = 0;
}

void ContextHolder::set_elements(jobjectArray buffers, jobject* elements, jbyte** bases, int count) {
  assert(buffers);
  assert(elements);
  assert(bases);
  assert(count > 0);

  // set_elements can only be called once
  assert(!_buffers);
  assert(!_elements);
  assert(!_bases);
  assert(_element_count == 0);

  _buffers = (jobjectArray) _env->NewGlobalRef(buffers);
  _element_count = count;
  _elements = new jobject[count];
  _bases = new jbyte*[count];
  for (int i=0; i < count; i++) {
    _elements[i] = (jobject) _env->NewGlobalRef(elements[i]);
    _bases[i] = bases[i];
  }
}

ContextHolder::~ContextHolder() {
  if (_context) {
    _env->DeleteGlobalRef(_context);
  }
  if (_data) {
    _env->DeleteGlobalRef(_data);
  }
  if (_buffers && _elements && _bases && _element_count > 0) {
    for (int i=0; i < _element_count; i++) {
      _env->DeleteGlobalRef(_buffers);
      _env->ReleaseByteArrayElements((jbyteArray) _elements[i], (jbyte*) _bases[i], JNI_ABORT);
      _env->DeleteGlobalRef(_elements[i]);
    }
    delete _bases;
    delete _elements;
  }
}
