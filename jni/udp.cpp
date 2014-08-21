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
#include <stdlib.h>

#include "uv.h"
#include "exception.h"
#include "context.h"
#include "stream.h"
#include "udp.h"
///#include "com_oracle_libuv_handles_UDPHandle.h"

uv_buf_t _alloc_cb(uv_handle_t* handle, size_t suggested_size) {
  // override - 64k buffers are too large for udp
  if (suggested_size >= 64 * 1024) suggested_size = 2 * 1024;
  return uv_buf_init(new char[suggested_size], static_cast<unsigned int>(suggested_size));
}

jclass UDPCallbacks::_udp_handle_cid = NULL;

jmethodID UDPCallbacks::_recv_callback_mid = NULL;
jmethodID UDPCallbacks::_send_callback_mid = NULL;
jmethodID UDPCallbacks::_close_callback_mid = NULL;

void UDPCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _udp_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_udp_handle_cid);

  _recv_callback_mid = env->GetMethodID(_udp_handle_cid, "callRecv", "(ILjava/nio/ByteBuffer;Lcom/oracle/libuv/Address;)V");
  assert(_recv_callback_mid);
  _send_callback_mid = env->GetMethodID(_udp_handle_cid, "callSend", "(ILjava/lang/Exception;Ljava/lang/Object;)V");
  assert(_send_callback_mid);
  _close_callback_mid = env->GetMethodID(_udp_handle_cid, "callClose", "()V");
  assert(_close_callback_mid);

  // ensure JNI ids used by StreamCallbacks::_address_to_js are initialized
  StreamCallbacks::static_initialize_address(env);
}

void UDPCallbacks::initialize(JNIEnv *env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

UDPCallbacks::UDPCallbacks() {
  _env = NULL;
}

UDPCallbacks::~UDPCallbacks() {
  _env->DeleteGlobalRef(_instance);
}

void UDPCallbacks::on_recv(ssize_t nread, uv_buf_t buf, struct sockaddr* addr, unsigned flags) {
  if (nread == 0) return;
  jobject buffer_arg = NULL;
  if (nread > 0) {
    jbyte* data = new jbyte[nread];
    memcpy(data, buf.base, nread);
    buffer_arg = _env->NewDirectByteBuffer(data, nread);
    OOM(_env, buffer_arg);
  }
  jobject rinfo_arg = addr ? StreamCallbacks::_address_to_js(_env, addr) : NULL;
  _env->CallVoidMethod(
      _instance,
      _recv_callback_mid,
      nread,
      buffer_arg,
      rinfo_arg);
  if (buffer_arg) {
    _env->DeleteLocalRef(buffer_arg);
  }
  delete[] buf.base;
}

void UDPCallbacks::on_send(int status, int error_code, jobject buffer, jobject context) {
  assert(_env);

  jthrowable exception = error_code ? NewException(_env, error_code) : NULL;
  _env->CallVoidMethod(
      _instance,
      _send_callback_mid,
      status,
      exception,
      context);
}

void UDPCallbacks::on_close() {
  _env->CallVoidMethod(
      _instance,
      _close_callback_mid);
}

static void _close_cb(uv_handle_t* handle) {
  assert(handle);
  assert(handle->data);
  UDPCallbacks* cb = reinterpret_cast<UDPCallbacks*>(handle->data);
  cb->on_close();
  delete cb;
  delete handle;
}

static void _recv_cb(uv_udp_t* udp, ssize_t nread, uv_buf_t buf, struct sockaddr* addr, unsigned flags) {
  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  assert(handle->data);
  UDPCallbacks* cb = reinterpret_cast<UDPCallbacks*>(handle->data);
  cb->on_recv(nread, buf, addr, flags);
}

static void _send_cb(uv_udp_send_t* req, int status) {
  assert(req->handle);
  assert(req->data);
  assert(req->handle->data);
  UDPCallbacks* cb = reinterpret_cast<UDPCallbacks*>(req->handle->data);
  ContextHolder* req_data = reinterpret_cast<ContextHolder*>(req->data);
  cb->on_send(status, status < 0 ? uv_last_error(req->handle->loop).code : 0, req_data->data(), req_data->context());
  delete req_data;
  delete req;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_libuv_handles_UDPHandle__1new__J
  (JNIEnv *env, jclass cls, jlong loop) {

  assert(loop);
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  uv_udp_t* udp = new uv_udp_t();
  int r = uv_udp_init(lp, udp);
  if (r) {
    ThrowException(env, udp->loop, "uv_udp_init");
    return (jlong) NULL;
  }
  udp->data = new UDPCallbacks();
  return reinterpret_cast<jlong>(udp);
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _new
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_libuv_handles_UDPHandle__1new__JJ
  (JNIEnv *env, jclass cls, jlong loop, jlong socket) {

  assert(loop);
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  uv_udp_t* udp = new uv_udp_t();
  int r = uv_udp_init(lp, udp);
  if (r) {
    ThrowException(env, udp->loop, "uv_udp_init");
    return (jlong) NULL;
  }
  r = -1;///uv_udp_open(udp, (uv_os_sock_t) socket);
  if (r) {
    ThrowException(env, udp->loop, "uv_udp_open");
    delete udp;
    return (jlong) NULL;
  }
  udp->data = new UDPCallbacks();
  return reinterpret_cast<jlong>(udp);
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_UDPHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  UDPCallbacks::static_initialize(env, cls);
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _initialize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_UDPHandle__1initialize
  (JNIEnv *env, jobject that, jlong udp) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  assert(handle->data);
  UDPCallbacks* cb = reinterpret_cast<UDPCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _address
 * Signature: (J)Lcom/oracle/libuv/Address;
 */
JNIEXPORT jobject JNICALL Java_com_oracle_libuv_handles_UDPHandle__1address
  (JNIEnv *env, jobject that, jlong udp) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  struct sockaddr_storage address;
  sockaddr* sock = reinterpret_cast<sockaddr*>(&address);
  int addrlen = sizeof(address);
  int r = uv_udp_getsockname(handle, sock, &addrlen);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_getsockname");
    return NULL;
  }
  const sockaddr* addr = reinterpret_cast<const sockaddr*>(&address);
  return StreamCallbacks::_address_to_js(env, addr);
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _bind
 * Signature: (JILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1bind
  (JNIEnv *env, jobject that, jlong udp, jint port, jstring host) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in addr = uv_ip4_addr(h, port);
  unsigned flags = 0;
  int r = uv_udp_bind(handle, addr, flags);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_bind", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _bind6
 * Signature: (JILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1bind6
  (JNIEnv *env, jobject that, jlong udp, jint port, jstring host) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in6 addr = uv_ip6_addr(h, port);
  unsigned flags = 0;
  int r = uv_udp_bind6(handle, addr, flags);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_bind6", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _send
 * Signature: (JLjava/nio/ByteBuffer;[BIIILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1send
  (JNIEnv *env, jobject that, jlong udp, jobject buffer, jbyteArray data, jint offset, jint length, jint port, jstring host, jobject context) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in addr = uv_ip4_addr(h, port);
  uv_udp_send_t* req = new uv_udp_send_t();
  req->handle = handle;
  ContextHolder* req_data = NULL;
  int r;
  if (data) {
    jbyte* base = (jbyte*) env->GetPrimitiveArrayCritical(data, NULL);
    OOME(env, base);
    uv_buf_t buf;
    buf.base = reinterpret_cast<char*>(base + offset);
    buf.len = length;
    req_data = new ContextHolder(env, context);
    req->data = req_data;
    r = uv_udp_send(req, handle, &buf, 1, addr, _send_cb);
    env->ReleasePrimitiveArrayCritical(data, base, 0);
  } else {
    jbyte* base = (jbyte*) env->GetDirectBufferAddress(buffer);
    uv_buf_t buf;
    buf.base = reinterpret_cast<char*>(base + offset);
    buf.len = length;
    req_data = new ContextHolder(env, buffer, context);
    req->data = req_data;
    r = uv_udp_send(req, handle, &buf, 1, addr, _send_cb);
  }
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_udp_send", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _send6
 * Signature: (JLjava/nio/ByteBuffer;[BIIILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1send6
  (JNIEnv *env, jobject that, jlong udp, jobject buffer, jbyteArray data, jint offset, jint length, jint port, jstring host, jobject context) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in6 addr = uv_ip6_addr(h, port);

  jbyte* base = (jbyte*) env->GetPrimitiveArrayCritical(data, NULL);
  OOME(env, base);
  uv_buf_t buf;
  buf.base = reinterpret_cast<char*>(base + offset);
  buf.len = length;

  uv_udp_send_t* req = new uv_udp_send_t();
  req->handle = handle;
  ContextHolder* req_data = new ContextHolder(env, context);
  req->data = req_data;
  int r = uv_udp_send6(req, handle, &buf, 1, addr, _send_cb);
  env->ReleasePrimitiveArrayCritical(data, base, 0);
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_udp_send6", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _recv_start
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1recv_1start
  (JNIEnv *env, jobject that, jlong udp) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  int r = uv_udp_recv_start(handle, _alloc_cb, _recv_cb);
  // UV_EALREADY means that the socket is already bound but that's okay
  if (r && uv_last_error(handle->loop).code != UV_EALREADY) {
    ThrowException(env, handle->loop, "uv_udp_recv_start");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _recv_stop
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1recv_1stop
  (JNIEnv *env, jobject that, jlong udp) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  int r = uv_udp_recv_stop(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_recv_stop");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _set_ttl
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1set_1ttl
  (JNIEnv *env, jobject that, jlong udp, jint ttl) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  int r = uv_udp_set_ttl(handle, ttl);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_set_ttl");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _set_membership
 * Signature: (JLjava/lang/String;Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1set_1membership
  (JNIEnv *env, jobject that, jlong udp, jstring multicastAddress, jstring interfaceAddress, jint membership) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  const char* maddr = env->GetStringUTFChars(multicastAddress, 0);
  const char* iaddr = env->GetStringUTFChars(interfaceAddress, 0);
  int r = uv_udp_set_membership(handle, maddr, iaddr, static_cast<uv_membership>(membership));
  env->ReleaseStringUTFChars(multicastAddress, maddr);
  env->ReleaseStringUTFChars(interfaceAddress, iaddr);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_set_membership");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _set_multicast_loop
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1set_1multicast_1loop
  (JNIEnv *env, jobject that, jlong udp, jint on) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  int r = uv_udp_set_multicast_loop(handle, on);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_set_multicast_loop");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _set_multicast_ttl
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1set_1multicast_1ttl
  (JNIEnv *env, jobject that, jlong udp, jint ttl) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  int r = uv_udp_set_multicast_ttl(handle, ttl);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_set_multicast_ttl");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _set_broadcast
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_oracle_libuv_handles_UDPHandle__1set_1broadcast
  (JNIEnv *env, jobject that, jlong udp, jint on) {

  assert(udp);
  uv_udp_t* handle = reinterpret_cast<uv_udp_t*>(udp);
  int r = uv_udp_set_broadcast(handle, on);
  if (r) {
    ThrowException(env, handle->loop, "uv_udp_set_broadcast");
  }
  return r;
}

/*
 * Class:     com_oracle_libuv_handles_UDPHandle
 * Method:    _close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_libuv_handles_UDPHandle__1close
  (JNIEnv *env, jobject that, jlong udp) {

  assert(udp);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(udp);
  uv_close(handle, _close_cb);
}
