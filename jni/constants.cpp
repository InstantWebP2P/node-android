/*
 * Copyright (c) 2013] = Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only] = as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful] = but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not] = write to the Free Software Foundation,
 * Inc.] = 51 Franklin St] = Fifth Floor] = Boston] = MA 02110-1301 USA.
 *
 * Please contact Oracle] = 500 Oracle Parkway] = Redwood Shores] = CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <assert.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <signal.h>
#include <jni.h>

///#include "com_iwebpp_libuvpp_Constants.h"

#ifdef _WIN32

#define O_RDONLY    _O_RDONLY
#define O_WRONLY    _O_WRONLY
#define O_RDWR      _O_RDWR
#define O_APPEND    _O_APPEND
#define O_CREAT     _O_CREAT
#define O_TRUNC     _O_TRUNC
#define O_EXCL      _O_EXCL

#define O_SYNC      0
#define O_NOCTTY    0

#define S_IRUSR     0000400
#define S_IWUSR     0000200
#define S_IXUSR     0000100
#define S_IRWXU     S_IRUSR | S_IWUSR | S_IXUSR


#define S_IRGRP     0000040
#define S_IWGRP     0000020
#define S_IXGRP     0000010
#define S_IRWXG      S_IRGRP | S_IWGRP | S_IXGRP

#define S_IROTH     0000004
#define S_IWOTH     0000002
#define S_IXOTH     0000001
#define S_IRWXO     S_IROTH | S_IWOTH | S_IXOTH

#define S_IFMT      _S_IFMT
#define S_IFIFO     _S_IFIFO
#define S_IFCHR     _S_IFCHR
#define S_IFDIR     _S_IFDIR
#define S_IFBLK     0060000
#define S_IFREG     _S_IFREG
#define S_IFLNK     0120000
#define S_IFSOCK    0140000
#define S_IFWHT     0
#define S_ISUID     0004000
#define S_ISGID     0002000
#define S_ISVTX     0001000


#define SIGHUP      1
#define SIGINT      2
#define SIGQUIT     3
#define SIGILL      4
#define SIGTRAP     5
#define SIGABRT     22
#define SIGIOT      SIGABRT
#define SIGBUS      10
#define SIGFPE      8
#define SIGKILL     9
#define SIGUSR1     30
#define SIGSEGV     11
#define SIGUSR2     31
#define SIGPIPE     13
#define SIGALRM     14
#define SIGTERM     15
#define SIGSTKFLT   0
#define SIGCHLD     20
#define SIGCONT     19
#define SIGSTOP     17
#define SIGTSTP     18
#define SIGTTIN     21
#define SIGTTOU     22
#define SIGURG      16
#define SIGXCPU     24
#define SIGXFSZ     25
#define SIGVTALRM   26
#define SIGPROF     27
#define SIGWINCH    28
#define SIGIO       23
#define SIGPOLL     23
#define SIGPWR      29
#define SIGSYS      12
#define SIGUNUSED   31

#endif /* _WIN32 */

#ifdef __MACOS__

#define SIGSTKFLT   0
#define SIGPOLL     23
#define SIGPWR      29
#define SIGUNUSED   31

#endif  /* __MACOS__ */

#ifdef __POSIX__

#ifndef S_IFWHT
#define S_IFWHT 0160000
#endif

#endif  /* __POSIX__ */

/*
 * Class:     com_iwebpp_libuvpp_Constants
 * Method:    _getFieldValues
 * Signature: ([I)V
 */
JNIEXPORT void JNICALL Java_com_iwebpp_libuvpp_Constants__1get_1field_1values
  (JNIEnv *env, jclass cls, jintArray array) {

  jint* values = env->GetIntArrayElements(array, 0);
  assert(values);

  values[0] = O_RDONLY;
  values[1] = O_WRONLY;
  values[2] = O_RDWR;
  values[3] = O_APPEND;
  values[4] = O_CREAT;
  values[5] = O_TRUNC;
  values[6] = O_EXCL;
  values[7] = O_SYNC;
  values[8] = O_NOCTTY;
  values[9] = S_IRUSR;
  values[10] = S_IWUSR;
  values[11] = S_IXUSR;
  values[12] = S_IRWXU;
  values[13] = S_IRGRP;
  values[14] = S_IWGRP;
  values[15] = S_IXGRP;
  values[16] = S_IRWXG;
  values[17] = S_IROTH;
  values[18] = S_IWOTH;
  values[19] = S_IXOTH;
  values[20] = S_IRWXO;
  values[21] = S_IFMT;
  values[22] = S_IFIFO;
  values[23] = S_IFCHR;
  values[24] = S_IFDIR;
  values[25] = S_IFBLK;
  values[26] = S_IFREG;
  values[27] = S_IFLNK;
  values[28] = S_IFSOCK;
  ///values[29] = S_IFWHT;
  values[30] = S_ISUID;
  values[31] = S_ISGID;
  values[32] = S_ISVTX;
  values[33] = SIGHUP;
  values[34] = SIGINT;
  values[35] = SIGQUIT;
  values[36] = SIGILL;
  values[37] = SIGTRAP;
  values[38] = SIGABRT;
  values[39] = SIGIOT;
  values[40] = SIGBUS;
  values[41] = SIGFPE;
  values[42] = SIGKILL;
  values[43] = SIGUSR1;
  values[44] = SIGSEGV;
  values[45] = SIGUSR2;
  values[46] = SIGPIPE;
  values[47] = SIGALRM;
  values[48] = SIGTERM;
#if defined(__sun)
  values[49] = 0; // undefined on solaris
#else
  values[49] = SIGSTKFLT;
#endif
  values[50] = SIGCHLD;
  values[51] = SIGCONT;
  values[52] = SIGSTOP;
  values[53] = SIGTSTP;
  values[54] = SIGTTIN;
  values[55] = SIGTTOU;
  values[56] = SIGURG;
  values[57] = SIGXCPU;
  values[58] = SIGXFSZ;
  values[59] = SIGVTALRM;
  values[60] = SIGPROF;
  values[61] = SIGWINCH;
  values[62] = SIGIO;
  values[63] = SIGPOLL;
  values[64] = SIGPWR;
  values[65] = SIGSYS;
#if defined(__sun)
  values[66] = 0; // undefined on solaris
#else
  values[66] = SIGUNUSED;
#endif

  env->ReleaseIntArrayElements(array, values, 0);
}

