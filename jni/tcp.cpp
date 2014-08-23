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
#include "context.h"
///#include "com_iwebpp_libuvpp_handles_TCPHandle.h"

static void _tcp_connect_cb(uv_connect_t* req, int status) {
  assert(req);
  assert(req->data);
  assert(req->handle);
  assert(req->handle->data);
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(req->handle->data);
  ContextHolder* req_data = reinterpret_cast<ContextHolder*>(req->data);
  cb->on_connect(status, status < 0 ? uv_last_error(req->handle->loop).code : 0, req_data->context());
  delete req;
  delete req_data;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1new__J
  (JNIEnv *env, jclass cls, jlong loop) {

  assert(loop);
  uv_tcp_t* tcp = new uv_tcp_t();
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  int r = uv_tcp_init(lp, tcp);
  if (r) {
    ThrowException(env, lp, "uv_tcp_init");
    return (jlong) NULL;
  }
  assert(tcp);
  tcp->data = new StreamCallbacks();
  return reinterpret_cast<jlong>(tcp);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1new__JJ
  (JNIEnv *env, jclass cls, jlong loop, jlong socket) {

  assert(loop);
  uv_tcp_t* tcp = new uv_tcp_t();
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  int r = uv_tcp_init(lp, tcp);
  if (r) {
    ThrowException(env, lp, "uv_tcp_init");
    return (jlong) NULL;
  }
  r = -1;///uv_tcp_open(tcp, (uv_os_sock_t) socket);
  if (r) {
    ThrowException(env, lp, "uv_tcp_open");
    return (jlong) NULL;
  }
  assert(tcp);
  tcp->data = new StreamCallbacks();
  return reinterpret_cast<jlong>(tcp);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _bind
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1bind
  (JNIEnv *env, jobject that, jlong tcp, jstring host, jint port) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in addr = uv_ip4_addr(h, port);
  int r = uv_tcp_bind(handle, addr);
  if (r) {
    ThrowException(env, handle->loop, "uv_tcp_bind", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _bind6
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1bind6
  (JNIEnv *env, jobject that, jlong tcp, jstring host, jint port) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in6 addr = uv_ip6_addr(h, port);
  int r = uv_tcp_bind6(handle, addr);
  if (r) {
    ThrowException(env, handle->loop, "uv_tcp_bind6", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _connect
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1connect
  (JNIEnv *env, jobject that, jlong tcp, jstring host, jint port, jobject context) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in addr = uv_ip4_addr(h, port);
  uv_connect_t* req = new uv_connect_t();
  req->handle = reinterpret_cast<uv_stream_t*>(handle);
  ContextHolder* req_data = new ContextHolder(env, context);
  req->data = req_data;
  int r = uv_tcp_connect(req, handle, addr, _tcp_connect_cb);
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_tcp_connect", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _connect6
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1connect6
  (JNIEnv *env, jobject that, jlong tcp, jstring host, jint port, jobject context) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in6 addr = uv_ip6_addr(h, port);
  uv_connect_t* req = new uv_connect_t();
  req->handle = reinterpret_cast<uv_stream_t*>(handle);
  ContextHolder* req_data = new ContextHolder(env, context);
  req->data = req_data;
  int r = uv_tcp_connect6(req, handle, addr, _tcp_connect_cb);
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_tcp_connect6", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _open
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1open
  (JNIEnv *env, jobject that, jlong tcp, jlong socket) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);
  int r = -1;///uv_tcp_open(handle, (uv_os_sock_t) socket);
  if (r) {
    ThrowException(env, handle->loop, "uv_tcp_open");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _socket_name
 * Signature: (J)Lcom/oracle/libuv/Address;
 */
JNIEXPORT jobject JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1socket_1name
  (JNIEnv *env, jobject that, jlong tcp) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);

  struct sockaddr_storage address;
  int addrlen = sizeof(address);
  int r = uv_tcp_getsockname(handle,
                             reinterpret_cast<sockaddr*>(&address),
                             &addrlen);
  if (r) {
    ThrowException(env, handle->loop, "uv_tcp_getsockname");
    return NULL;
  }
  const sockaddr* addr = reinterpret_cast<const sockaddr*>(&address);
  return StreamCallbacks::_address_to_js(env, addr);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _peer_name
 * Signature: (J)Lcom/oracle/libuv/Address;
 */
JNIEXPORT jobject JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1peer_1name
  (JNIEnv *env, jobject that, jlong tcp) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);

  struct sockaddr_storage address;
  int addrlen = sizeof(address);
  int r = uv_tcp_getpeername(handle,
                             reinterpret_cast<sockaddr*>(&address),
                             &addrlen);
  if (r) {
    ThrowException(env, handle->loop, "uv_tcp_getpeername");
    return NULL;
  }
  const sockaddr* addr = reinterpret_cast<const sockaddr*>(&address);
  return StreamCallbacks::_address_to_js(env, addr);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _no_delay
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1no_1delay
  (JNIEnv *env, jobject that, jlong tcp, jint enable) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);

  int r = uv_tcp_nodelay(handle, enable);
  if (r) {
    ThrowException(env, handle->loop, "uv_tcp_nodelay");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _keep_alive
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1keep_1alive
  (JNIEnv *env, jobject that, jlong tcp, jint enable, jint delay) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);

  int r = uv_tcp_keepalive(handle, enable, delay);
  if (r) {
    ThrowException(env, handle->loop, "uv_tcp_keepalive");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_TCPHandle
 * Method:    _simultaneous_accepts
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_TCPHandle__1simultaneous_1accepts
  (JNIEnv *env, jobject that, jlong tcp, jint enable) {

  assert(tcp);
  uv_tcp_t* handle = reinterpret_cast<uv_tcp_t*>(tcp);

  int r = uv_tcp_simultaneous_accepts(handle, enable);
  if (r) {
    // TODO: Node.js as of v0.10.23 ignores the error.
    // ThrowException(env, handle->loop, "uv_tcp_simultaneous_accepts");
  }
  return r;
}
