// UDT wrapper tom zhou <iwebpp@gmail.com>

#include <assert.h>

#include "uv.h"
#include "exception.h"
#include "stream.h"
#include "context.h"
///#include "com_iwebpp_libuvpp_handles_UDTHandle.h"

static void _udt_connect_cb(uv_connect_t* req, int status) {
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
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1new__J
  (JNIEnv *env, jclass cls, jlong loop) {

  assert(loop);
  uv_udt_t* udt = new uv_udt_t();
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  int r = uv_udt_init(lp, udt);
  if (r) {
    ThrowException(env, lp, "uv_udt_init");
    return (jlong) NULL;
  }
  assert(udt);
  udt->data = new StreamCallbacks();
  return reinterpret_cast<jlong>(udt);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _new
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1new__JJ
  (JNIEnv *env, jclass cls, jlong loop, jlong socket) {

  assert(loop);
  uv_udt_t* udt = new uv_udt_t();
  uv_loop_t* lp = reinterpret_cast<uv_loop_t*>(loop);
  int r = uv_udt_init(lp, udt);
  if (r) {
    ThrowException(env, lp, "uv_udt_init");
    return (jlong) NULL;
  }
  r = -1;///uv_udt_open(udt, (uv_os_sock_t) socket);
  if (r) {
    ThrowException(env, lp, "uv_udt_open");
    return (jlong) NULL;
  }
  assert(udt);
  udt->data = new StreamCallbacks();
  return reinterpret_cast<jlong>(udt);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _bind
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1bind
  (JNIEnv *env, jobject that, jlong udt, jstring host, jint port) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in addr = uv_ip4_addr(h, port);
  int r = uv_udt_bind(handle, addr);
  if (r) {
    ThrowException(env, handle->loop, "uv_udt_bind", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _bind6
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1bind6
  (JNIEnv *env, jobject that, jlong udt, jstring host, jint port) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in6 addr = uv_ip6_addr(h, port);
  int r = uv_udt_bind6(handle, addr);
  if (r) {
    ThrowException(env, handle->loop, "uv_udt_bind6", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _connect
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1connect
  (JNIEnv *env, jobject that, jlong udt, jstring host, jint port, jobject context) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in addr = uv_ip4_addr(h, port);
  uv_connect_t* req = new uv_connect_t();
  req->handle = reinterpret_cast<uv_stream_t*>(handle);
  ContextHolder* req_data = new ContextHolder(env, context);
  req->data = req_data;
  int r = uv_udt_connect(req, handle, addr, _udt_connect_cb);
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_udt_connect", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _connect6
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1connect6
  (JNIEnv *env, jobject that, jlong udt, jstring host, jint port, jobject context) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);
  const char* h = env->GetStringUTFChars(host, 0);
  sockaddr_in6 addr = uv_ip6_addr(h, port);
  uv_connect_t* req = new uv_connect_t();
  req->handle = reinterpret_cast<uv_stream_t*>(handle);
  ContextHolder* req_data = new ContextHolder(env, context);
  req->data = req_data;
  int r = uv_udt_connect6(req, handle, addr, _udt_connect_cb);
  if (r) {
    delete req_data;
    delete req;
    ThrowException(env, handle->loop, "uv_udt_connect6", h);
  }
  env->ReleaseStringUTFChars(host, h);
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _open
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1open
  (JNIEnv *env, jobject that, jlong udt, jlong socket) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);
  int r = -1;///uv_udt_open(handle, (uv_os_sock_t) socket);
  if (r) {
    ThrowException(env, handle->loop, "uv_udt_open");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _socket_name
 * Signature: (J)Lcom/oracle/libuv/Address;
 */
JNIEXPORT jobject JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1socket_1name
  (JNIEnv *env, jobject that, jlong udt) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);

  struct sockaddr_storage address;
  int addrlen = sizeof(address);
  int r = uv_udt_getsockname(handle,
                             reinterpret_cast<sockaddr*>(&address),
                             &addrlen);
  if (r) {
    ThrowException(env, handle->loop, "uv_udt_getsockname");
    return NULL;
  }
  const sockaddr* addr = reinterpret_cast<const sockaddr*>(&address);
  return StreamCallbacks::_address_to_js(env, addr);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _peer_name
 * Signature: (J)Lcom/oracle/libuv/Address;
 */
JNIEXPORT jobject JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1peer_1name
  (JNIEnv *env, jobject that, jlong udt) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);

  struct sockaddr_storage address;
  int addrlen = sizeof(address);
  int r = uv_udt_getpeername(handle,
                             reinterpret_cast<sockaddr*>(&address),
                             &addrlen);
  if (r) {
    ThrowException(env, handle->loop, "uv_udt_getpeername");
    return NULL;
  }
  const sockaddr* addr = reinterpret_cast<const sockaddr*>(&address);
  return StreamCallbacks::_address_to_js(env, addr);
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _no_delay
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1no_1delay
  (JNIEnv *env, jobject that, jlong udt, jint enable) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);

  int r = uv_udt_nodelay(handle, enable);
  if (r) {
    ThrowException(env, handle->loop, "uv_udt_nodelay");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _keep_alive
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1keep_1alive
  (JNIEnv *env, jobject that, jlong udt, jint enable, jint delay) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);

  int r = uv_udt_keepalive(handle, enable, delay);
  if (r) {
    ThrowException(env, handle->loop, "uv_udt_keepalive");
  }
  return r;
}

/*
 * Class:     com_iwebpp_libuvpp_handles_UDTHandle
 * Method:    _simultaneous_accepts
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_iwebpp_libuvpp_handles_UDTHandle__1simultaneous_1accepts
  (JNIEnv *env, jobject that, jlong udt, jint enable) {

  assert(udt);
  uv_udt_t* handle = reinterpret_cast<uv_udt_t*>(udt);

  int r = -1; ///uv_udt_simultaneous_accepts(handle, enable);
  if (r) {
    // TODO: Node.js as of v0.10.23 ignores the error.
    // ThrowException(env, handle->loop, "uv_udt_simultaneous_accepts");
  }
  return r;
}
