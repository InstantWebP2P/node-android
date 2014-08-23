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
#include <string.h>

#include "uv.h"
#include "exception.h"
#include "context.h"
#include "stream.h"
#include "udp.h"
///#include "com_iwebpp_libuvpp_handles_StreamHandle.h"

jstring StreamCallbacks::_IPV4 = NULL;
jstring StreamCallbacks::_IPV6 = NULL;

jclass StreamCallbacks::_address_cid = NULL;
jclass StreamCallbacks::_stream_handle_cid = NULL;

jmethodID StreamCallbacks::_address_init_mid = NULL;
jmethodID StreamCallbacks::_call_read_callback_mid = NULL;
jmethodID StreamCallbacks::_call_read2_callback_mid = NULL;
jmethodID StreamCallbacks::_call_write_callback_mid = NULL;
jmethodID StreamCallbacks::_call_connect_callback_mid = NULL;
jmethodID StreamCallbacks::_call_connection_callback_mid = NULL;
jmethodID StreamCallbacks::_call_close_callback_mid = NULL;
jmethodID StreamCallbacks::_call_shutdown_callback_mid = NULL;

void StreamCallbacks::static_initialize(JNIEnv* env, jclass cls) {
  _IPV4 = env->NewStringUTF("IPv4");
  _IPV4 = (jstring) env->NewGlobalRef(_IPV4);

  _IPV6 = env->NewStringUTF("IPv6");
  _IPV6 = (jstring) env->NewGlobalRef(_IPV6);

  _stream_handle_cid = (jclass) env->NewGlobalRef(cls);
  assert(_stream_handle_cid);

  _call_read_callback_mid = env->GetMethodID(_stream_handle_cid, "callRead", "(Ljava/nio/ByteBuffer;)V");
  assert(_call_read_callback_mid);

  _call_read2_callback_mid = env->GetMethodID(_stream_handle_cid, "callRead2", "(Ljava/nio/ByteBuffer;JI)V");
  assert(_call_read2_callback_mid);

  _call_write_callback_mid = env->GetMethodID(_stream_handle_cid, "callWrite", "(ILjava/lang/Exception;Ljava/lang/Object;)V");
  assert(_call_write_callback_mid);

  _call_connect_callback_mid = env->GetMethodID(_stream_handle_cid, "callConnect", "(ILjava/lang/Exception;Ljava/lang/Object;)V");
  assert(_call_connect_callback_mid);

  _call_connection_callback_mid = env->GetMethodID(_stream_handle_cid, "callConnection", "(ILjava/lang/Exception;)V");
  assert(_call_connection_callback_mid);

  _call_close_callback_mid = env->GetMethodID(_stream_handle_cid, "callClose", "()V");
  assert(_call_close_callback_mid);

  _call_shutdown_callback_mid = env->GetMethodID(_stream_handle_cid, "callShutdown", "(ILjava/lang/Exception;Ljava/lang/Object;)V");
  assert(_call_shutdown_callback_mid);

  static_initialize_address(env);
}

void StreamCallbacks::static_initialize_address(JNIEnv* env) {
  if (!_address_cid) {
      _address_cid = env->FindClass("com/iwebpp/libuvpp/Address");
      assert(_address_cid);
      _address_cid = (jclass) env->NewGlobalRef(_address_cid);
      assert(_address_cid);
  }

  if (!_address_init_mid) {
    _address_init_mid = env->GetMethodID(_address_cid, "<init>", "(Ljava/lang/String;ILjava/lang/String;)V");
    assert(_address_init_mid);
  }
}

void StreamCallbacks::initialize(JNIEnv *env, jobject instance) {
  _env = env;
  assert(_env);
  assert(instance);
  _instance = _env->NewGlobalRef(instance);
}

StreamCallbacks::StreamCallbacks() {
  _env = NULL;
}

StreamCallbacks::~StreamCallbacks() {
  _env->DeleteGlobalRef(_instance);
}

void StreamCallbacks::throw_exception(int code, const char* syscall) {
  assert(_env);
  ThrowException(_env, code, syscall);
}

void StreamCallbacks::on_read(uv_buf_t* buf, jsize nread) {
  assert(_env);
  if (nread < 0) {
    _env->CallVoidMethod(
        _instance,
        _call_read_callback_mid,
        NULL);
  } else if (nread > 0) {
    jbyte* data = new jbyte[nread];
    memcpy(data, buf->base, nread);
    jobject arg = _env->NewDirectByteBuffer(data, nread);
    OOM(_env, arg);
    _env->CallVoidMethod(
        _instance,
        _call_read_callback_mid,
        arg);
    _env->DeleteLocalRef(arg);
  }
  delete[] buf->base;
}

void StreamCallbacks::on_read2(uv_buf_t* buf, jsize nread, jlong ptr, uv_handle_type pending) {
  assert(_env);
  if (nread < 0) {
    _env->CallVoidMethod(
        _instance,
        _call_read2_callback_mid,
        NULL,
        ptr,
        pending);
  } else if (nread > 0) {
    jbyte* data = new jbyte[nread];
    memcpy(data, buf->base, nread);
    jobject array = _env->NewDirectByteBuffer(data, nread);
    OOM(_env, array);
    _env->CallVoidMethod(
        _instance,
        _call_read2_callback_mid,
        array,
        ptr,
        pending);
    _env->DeleteLocalRef(array);
  }
  delete[] buf->base;
}

void StreamCallbacks::on_write(int status, int error_code, jobject buffer, jobject context) {
  assert(_env);
  jthrowable exception = error_code ? NewException(_env, error_code) : NULL;
  _env->CallVoidMethod(
      _instance,
      _call_write_callback_mid,
      status,
      exception,
      context);
  if (exception) { _env->DeleteLocalRef(exception); }
}

void StreamCallbacks::on_connect(int status, int error_code, jobject context) {
  assert(_env);
  jthrowable exception = error_code ? NewException(_env, error_code) : NULL;
  _env->CallVoidMethod(
      _instance,
      _call_connect_callback_mid,
      status,
      exception,
      context);
  if (exception) { _env->DeleteLocalRef(exception); }
}

void StreamCallbacks::on_connection(int status, int error_code) {
  assert(_env);
  jthrowable exception = error_code ? NewException(_env, error_code) : NULL;
  _env->CallVoidMethod(
      _instance,
      _call_connection_callback_mid,
      status,
      exception);
  if (exception) { _env->DeleteLocalRef(exception); }
}

void StreamCallbacks::on_shutdown(int status, int error_code, jobject context) {
  assert(_env);
  jthrowable exception = error_code ? NewException(_env, error_code) : NULL;
  _env->CallVoidMethod(
      _instance,
      _call_shutdown_callback_mid,
      status,
      exception,
      context);
  if (exception) { _env->DeleteLocalRef(exception); }
}

void StreamCallbacks::on_close() {
  assert(_env);
  _env->CallVoidMethod(
      _instance,
      _call_close_callback_mid);
}

// used in tcp.cpp and udp.cpp
jobject StreamCallbacks::_address_to_js(JNIEnv* env, const sockaddr* addr) {
  char ip[INET6_ADDRSTRLEN];
  const sockaddr_in *a4;
  const sockaddr_in6 *a6;
  int port;

  assert(addr);
  switch (addr->sa_family) {
  case AF_INET6:
    a6 = reinterpret_cast<const sockaddr_in6*>(addr);
    ///uv_inet_ntop(AF_INET6, &a6->sin6_addr, ip, sizeof ip);
    uv_inet_ntop(AF_INET6, &a6->sin6_addr, ip, sizeof ip);
    port = ntohs(a6->sin6_port);
    return env->NewObject(_address_cid,
      _address_init_mid,
      env->NewStringUTF(ip),
      port,
      _IPV6);

  case AF_INET:
    a4 = reinterpret_cast<const sockaddr_in*>(addr);
    uv_inet_ntop(AF_INET, &a4->sin_addr, ip, sizeof ip);
    port = ntohs(a4->sin_port);
    return env->NewObject(_address_cid,
      _address_init_mid,
      env->NewStringUTF(ip),
      port,
      _IPV4);
  }
  return NULL;
}

static uv_buf_t _alloc_cb(uv_handle_t* handle, size_t suggested_size) {
  return uv_buf_init(new char[suggested_size], static_cast<unsigned int>(suggested_size));
}

static void _read_cb(uv_stream_t* stream, ssize_t nread, uv_buf_t buf) {
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(stream->data);
  assert(cb);
  jsize size = static_cast<jsize>(nread);
  cb->on_read(&buf, size);
}

static void _shutdown_cb(uv_shutdown_t* req, int status) {
  assert(req);
  assert(req->data);
  assert(req->handle);
  assert(req->handle->data);
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(req->handle->data);
  ContextHolder* req_data = reinterpret_cast<ContextHolder*>(req->data);
  cb->on_shutdown(status, status < 0 ? uv_last_error(req->handle->loop).code : 0, req_data->context());
  delete req_data;
  delete req;
}

static void _close_cb(uv_handle_t* handle) {
  assert(handle);
  assert(handle->data);
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(handle->data);
  cb->on_close();
  delete cb;
  delete handle;
}

static void _read2_cb(uv_pipe_t* pipe, ssize_t nread, uv_buf_t buf, uv_handle_type pending) {
  int r;
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(pipe->data);
  assert(cb);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(pipe);
  jsize size = static_cast<jsize>(nread);
  if (pending == UV_TCP) {
    uv_tcp_t* tcp = new uv_tcp_t();
    r = uv_tcp_init(handle->loop, tcp);
    if (r) {
      cb->throw_exception(r, "read2_cb.uv_tcp_init");
      return;
    }
    tcp->data = new StreamCallbacks();
    r = uv_accept(handle, reinterpret_cast<uv_stream_t*>(tcp));
    if (r) {
      cb->throw_exception(r, "read2_cb.uv_accept(tcp)");
      return;
    }
    cb->on_read2(&buf, size, reinterpret_cast<jlong>(tcp), pending);
  } else if (pending == UV_NAMED_PIPE) {
    uv_pipe_t* p = new uv_pipe_t();
    r = uv_pipe_init(handle->loop, p, 1);
    if (r) {
      cb->throw_exception(r, "read2_cb.uv_pipe_init");
      return;
    }
    p->data = new StreamCallbacks();
    r = uv_accept(handle, reinterpret_cast<uv_stream_t*>(p));
    if (r) {
      cb->throw_exception(r, "read2_cb.uv_accept(pipe)");
      return;
    }
    cb->on_read2(&buf, size, reinterpret_cast<jlong>(p), pending);
  } else if (pending == UV_UDP) {
    uv_udp_t* udp = new uv_udp_t();
    r = uv_udp_init(handle->loop, udp);
    if (r) {
      cb->throw_exception(r, "read2_cb.uv_udp_init");
      return;
    }
    udp->data = new UDPCallbacks();
    r = uv_accept(handle, reinterpret_cast<uv_stream_t*>(udp));
    if (r) {
      cb->throw_exception(r, "read2_cb.uv_accept(udp)");
      return;
    }
    cb->on_read2(&buf, size, reinterpret_cast<jlong>(udp), pending);
  } else {
    assert(pending == UV_UNKNOWN_HANDLE);
    cb->on_read(&buf, size);
  }
}

static void _write_cb(uv_write_t* req, int status) {
  assert(req->handle);
  assert(req->handle->data);
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(req->handle->data);
  ContextHolder* req_data = reinterpret_cast<ContextHolder*>(req->data);
  cb->on_write(status, status < 0 ? uv_last_error(req->handle->loop).code : 0, req_data->data(), req_data->context());
  delete req;
  delete req_data;
}

static void _connection_cb(uv_stream_t* stream, int status) {
  assert(stream);
  assert(stream->data);
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(stream->data);
  cb->on_connection(status, status < 0 ? uv_last_error(stream->loop).code : 0);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _static_initialize
 * Signature: ()V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1static_1initialize
  (JNIEnv *env, jclass cls) {

  StreamCallbacks::static_initialize(env, cls);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _initialize
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1initialize
  (JNIEnv *env, jobject that, jlong stream) {

  assert(stream);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  assert(handle->data);
  StreamCallbacks* cb = reinterpret_cast<StreamCallbacks*>(handle->data);
  cb->initialize(env, that);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _read_start
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1read_1start
  (JNIEnv *env, jobject that, jlong stream) {

  assert(stream);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);

  bool ipc_pipe = handle->type == UV_NAMED_PIPE && ((uv_pipe_t*)handle)->ipc;
  if (!ipc_pipe) {
    int r = uv_read_start(handle, _alloc_cb, _read_cb);
    if (r) {
      ThrowException(env, handle->loop, "uv_read_start");
    }
  } else {
    ///Java_com_iwebpp_libuvpp_handles_StreamHandle__1read2_1start(env, that, stream);
  }
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _read2_start
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1read2_1start
  (JNIEnv *env, jobject that, jlong stream) {

  assert(stream);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  int r = uv_read2_start(handle, _alloc_cb, _read2_cb);
  if (r) {
    ThrowException(env, handle->loop, "uv_read2_start");
  }
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _read_stop
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1read_1stop
  (JNIEnv *env, jobject that, jlong stream) {

  assert(stream);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  int r = uv_read_stop(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_read_stop");
  }
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _readable
 * Signature: (J)Z
 */
extern "C" JNIEXPORT  jboolean JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1readable
  (JNIEnv *env, jobject that, jlong stream) {

  assert(stream);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  int r = uv_is_readable(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_is_readable");
  }
  return r == 0 ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _writable
 * Signature: (J)Z
 */
extern "C" JNIEXPORT  jboolean JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1writable
  (JNIEnv *env, jobject that, jlong stream) {

  assert(stream);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  int r = uv_is_writable(handle);
  if (r) {
    ThrowException(env, handle->loop, "uv_is_writable");
  }
  return r == 0 ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _write
 * Signature: (JLjava/nio/ByteBuffer;[BIILjava/lang/Object;)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1write
  (JNIEnv *env, jobject that, jlong stream, jobject buffer, jbyteArray data, jint offset, jint length, jobject context) {

  assert(stream);

  int r;
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  uv_write_t* req = new uv_write_t();
  req->handle = handle;
  ContextHolder* req_data = NULL;
  if (data) {
    jbyte* base = (jbyte*) env->GetPrimitiveArrayCritical(data, NULL);
    OOME(env, base);
    uv_buf_t buf;
    buf.base = reinterpret_cast<char*>(base + offset);
    buf.len = length;
    req_data = new ContextHolder(env, context);
    req->data = req_data;
    r = uv_write(req, handle, &buf, 1, _write_cb);
    env->ReleasePrimitiveArrayCritical(data, base, 0);
  } else {
    jbyte* base = (jbyte*) env->GetDirectBufferAddress(buffer);
    uv_buf_t buf;
    buf.base = reinterpret_cast<char*>(base + offset);
    buf.len = length;
    req->data = new ContextHolder(env, buffer, context);
    r = uv_write(req, handle, &buf, 1, _write_cb);
  }
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_write");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _writev
 * Signature: (J[[BILjava/lang/Object;)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1writev
  (JNIEnv *env, jobject that, jlong stream, jobjectArray buffers, jint bufcount, jobject context) {

  assert(stream);
  assert(buffers);
  assert(bufcount > 0);
  assert(bufcount == env->GetArrayLength(buffers));

  int r;
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  uv_write_t* req = new uv_write_t();
  req->handle = handle;
  ContextHolder* req_data = NULL;
  jobject* elements = new jobject[bufcount];
  jbyte** bases = new jbyte*[bufcount];
  uv_buf_t* bufs = new uv_buf_t[bufcount];
  for (int i=0; i < bufcount; i++) {
    jbyteArray data = (jbyteArray) env->GetObjectArrayElement(buffers, i);
    jbyte* base = (jbyte*) env->GetByteArrayElements(data, NULL);
    OOME(env, base);
    elements[i] = (jobject) data;
    bases[i] = base;
    bufs[i].base = reinterpret_cast<char*>(base);
    bufs[i].len = env->GetArrayLength(data);
  }
  req_data = new ContextHolder(env, buffers, context);
  req_data->set_elements(buffers, elements, bases, bufcount); // ContextHolder destructor will release array elements
  req->data = req_data;
  r = uv_write(req, handle, bufs, bufcount, _write_cb);
  delete bufs;
  delete bases;
  delete elements;
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_write");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _write2
 * Signature: (JLjava/nio/ByteBuffer;[BIIJ)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1write2
  (JNIEnv *env, jobject that, jlong stream, jobject buffer, jbyteArray data, jint offset, jint length, jlong send_stream, jobject context) {

  assert(stream);
  assert(send_stream);

  int r;
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  uv_write_t* req = new uv_write_t();
  ContextHolder* req_data = NULL;
  req->handle = handle;
  if (data) {
    jbyte* base = (jbyte*) env->GetPrimitiveArrayCritical(data, NULL);
    OOME(env, base);
    uv_buf_t buf;
    buf.base = reinterpret_cast<char*>(base + offset);
    buf.len = length - offset;
    req_data = new ContextHolder(env, context);
    req->data = req_data;
    uv_stream_t* send_handle = reinterpret_cast<uv_stream_t*>(send_stream);
    r = uv_write2(req, handle, &buf, 1, send_handle, _write_cb);
    env->ReleasePrimitiveArrayCritical(data, base, 0);
  } else {
    jbyte* base = (jbyte*) env->GetDirectBufferAddress(buffer);
    OOME(env, base);
    uv_buf_t buf;
    buf.base = reinterpret_cast<char*>(base + offset);
    buf.len = length - offset;
    assert(stream);
    req_data = new ContextHolder(env, buffer, context);
    req->data = req_data;
    uv_stream_t* send_handle = reinterpret_cast<uv_stream_t*>(send_stream);
    r = uv_write2(req, handle, &buf, 1, send_handle, _write_cb);
  }
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_write2");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _write_queue_size
 * Signature: (J)J
 */
extern "C" JNIEXPORT  jlong JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1write_1queue_1size
  (JNIEnv *env, jobject that, jlong stream) {

  assert(stream);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  return handle->write_queue_size;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _close_write
 * Signature: (J)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1close_1write
  (JNIEnv *env, jobject that, jlong stream, jobject context) {

  assert(stream);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(stream);
  uv_shutdown_t* req = new uv_shutdown_t();
  ContextHolder* req_data = new ContextHolder(env, context);
  req->data = req_data;
  req->handle = handle;
  int r = uv_shutdown(req, handle, _shutdown_cb);
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_close_write");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _close
 * Signature: (J)V
 */
extern "C" JNIEXPORT  void JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1close
  (JNIEnv *env, jobject that, jlong stream) {

  assert(stream);
  uv_handle_t* handle = reinterpret_cast<uv_handle_t*>(stream);
  uv_close(handle, _close_cb);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _listen
 * Signature: (JI)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1listen
  (JNIEnv *env, jobject that, jlong ptr, jint backlog) {

  assert(ptr);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(ptr);
  int r = uv_listen(handle, backlog, _connection_cb);
  if (r) {
    ThrowException(env, handle->loop, "uv_listen");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_StreamHandle
 * Method:    _accept
 * Signature: (JJ)I
 */
extern "C" JNIEXPORT  jint JNICALL Java_com_iwebpp_libuvpp_handles_StreamHandle__1accept
  (JNIEnv *env, jobject that, jlong ptr, jlong clientPtr) {

  assert(ptr);
  assert(clientPtr);
  uv_stream_t* handle = reinterpret_cast<uv_stream_t*>(ptr);
  uv_stream_t* client = reinterpret_cast<uv_stream_t*>(clientPtr);
  int r = uv_accept(handle, client);
  if (r) {
    ThrowException(env, handle->loop, "uv_accept");
  }
  return r;
}
