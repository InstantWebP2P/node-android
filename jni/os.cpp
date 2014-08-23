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
#include <string.h>

#ifdef __POSIX__
# include <sys/utsname.h>
#endif

#include "uv.h"
#include "exception.h"
///#include "com_iwebpp_libuvpp_LibUV.h"

/*
 * Class:     com_iwebpp_libuvpp_LibUV
 * Method:    _getUptime
 * Signature: ()D
 */
extern "C" JNIEXPORT  jdouble JNICALL Java_com_iwebpp_libuvpp_LibUV__1getUptime
  (JNIEnv *env, jclass cls) {

  double uptime;

  uv_err_t err = uv_uptime(&uptime);

  if (err.code != UV_OK) {
    return 0;
  }

  return uptime;
}

/*
 * Class:     com_iwebpp_libuvpp_LibUV
 * Method:    _getLoadAvg
 * Signature: ()[D
 */
extern "C" JNIEXPORT  jdoubleArray JNICALL Java_com_iwebpp_libuvpp_LibUV__1getLoadAvg
  (JNIEnv *env, jclass cls) {

  double loadavg[3];

  uv_loadavg(loadavg);
  jdoubleArray array = env->NewDoubleArray(3);
  if (array != NULL) {
    env->SetDoubleArrayRegion(array, 0, 3, loadavg);
  }

  return array;
}

/*
 * Class:     com_iwebpp_libuvpp_LibUV
 * Method:    _getTotalMem
 * Signature: ()D
 */
extern "C" JNIEXPORT  jdouble JNICALL Java_com_iwebpp_libuvpp_LibUV__1getTotalMem
  (JNIEnv *env, jclass cls) {

  return (jdouble) uv_get_total_memory();
}

/*
 * Class:     com_iwebpp_libuvpp_LibUV
 * Method:    _getFreeMem
 * Signature: ()D
 */
extern "C" JNIEXPORT  jdouble JNICALL Java_com_iwebpp_libuvpp_LibUV__1getFreeMem
  (JNIEnv *env, jclass cls) {

  return (jdouble) uv_get_free_memory();
}

/*
 * Class:     com_iwebpp_libuvpp_LibUV
 * Method:    _getCPUs
 * Signature: ()[Ljava/lang/Object;
 */
extern "C" JNIEXPORT  jobjectArray JNICALL Java_com_iwebpp_libuvpp_LibUV__1getCPUs
  (JNIEnv *env, jclass cls) {

  uv_cpu_info_t* cpu_infos;
  int count;

  uv_err_t err = uv_cpu_info(&cpu_infos, &count);
  if (err.code != UV_OK) {
    return NULL;
  }

  jclass objectClassID = env->FindClass("java/lang/Object");
  assert(objectClassID);
  jclass integerClassID = env->FindClass("java/lang/Integer");
  assert(integerClassID);
  jmethodID integerConstructorMID = env->GetMethodID(integerClassID, "<init>", "(I)V");
  assert(integerConstructorMID);

  jobjectArray array = env->NewObjectArray(count * 7, objectClassID, NULL);
  OOMN(env, array);

  int j = 0;
  for (int i = 0; i < count; i++) {
      jstring model = env->NewStringUTF(cpu_infos[i].model);
      OOMN(env, model);
      env->SetObjectArrayElement(array, j++, model);
      env->DeleteLocalRef(model);

      jobject speed = env->NewObject(integerClassID, integerConstructorMID, cpu_infos[i].speed);
      OOMN(env, speed);
      env->SetObjectArrayElement(array, j++, speed);
      env->DeleteLocalRef(speed);

      jobject user = env->NewObject(integerClassID, integerConstructorMID, cpu_infos[i].cpu_times.user);
      OOMN(env, user);
      env->SetObjectArrayElement(array, j++, user);
      env->DeleteLocalRef(user);

      jobject nice = env->NewObject(integerClassID, integerConstructorMID, cpu_infos[i].cpu_times.nice);
      OOMN(env, nice);
      env->SetObjectArrayElement(array, j++, nice);
      env->DeleteLocalRef(nice);

      jobject sys = env->NewObject(integerClassID, integerConstructorMID, cpu_infos[i].cpu_times.sys);
      OOMN(env, sys);
      env->SetObjectArrayElement(array, j++, sys);
      env->DeleteLocalRef(sys);

      jobject idle = env->NewObject(integerClassID, integerConstructorMID, cpu_infos[i].cpu_times.idle);
      OOMN(env, idle);
      env->SetObjectArrayElement(array, j++, idle);
      env->DeleteLocalRef(idle);

      jobject irq = env->NewObject(integerClassID, integerConstructorMID, cpu_infos[i].cpu_times.irq);
      OOMN(env, irq);
      env->SetObjectArrayElement(array, j++, irq);
      env->DeleteLocalRef(irq);
  }
  uv_free_cpu_info(cpu_infos, count);

  return array;
}

/*
 * Class:     com_iwebpp_libuvpp_LibUV
 * Method:    _isIPv6
 * Signature: (Ljava/lang/String;)Z
 */
extern "C" JNIEXPORT  jboolean JNICALL Java_com_iwebpp_libuvpp_LibUV__1isIPv6
  (JNIEnv *env, jclass cls, jstring ip) {

  const char *address = env->GetStringUTFChars(ip, JNI_FALSE);
  char address_buffer[sizeof(struct in6_addr)];
  ///if (uv_inet_pton(AF_INET6, address, &address_buffer).code == UV_OK) {
  if (uv_inet_pton(AF_INET6, address, &address_buffer) == 0) {
    return JNI_TRUE;
  }
  return JNI_FALSE;
}
