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

package com.iwebpp.libuvpp;

@SuppressWarnings("serial")
public final class NativeException extends RuntimeException {

    private final int errno;
    private final String errnoString;
    private final String errnoMessage;
    private final String syscall;
    private final String path;

    public NativeException(final int errno,
                           final String errnoString,
                           final String errnoMessage,
                           final String syscall,
                           final String message,
                           final String path) {
        super(message);
        this.errno = errno;
        this.errnoString = errnoString;
        this.errnoMessage = errnoMessage;
        this.syscall = syscall;
        this.path = path;
    }

    public NativeException(final String message) {
        super(message);
        this.errno = 0;
        this.errnoString = null;
        this.errnoMessage = null;
        this.syscall = null;
        this.path = null;
    }
    
    public NativeException(final int errno,
    		               final String syscall,
    		               final String message) {
        super(message);
        this.errno = errno;
        this.errnoString = null;
        this.errnoMessage = null;
        this.syscall = syscall;
        this.path = null;
    }

    public int errno() {
        return errno;
    }

    public String errnoString() {
        return errnoString;
    }

    public String getErrnoMessage() {
        return errnoMessage;
    }

    public String syscall() {
        return syscall;
    }

    public String path() {
        return path;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(4096);
        sb.append("errno: ");
        sb.append(errno);
        sb.append(", errnoString: ");
        sb.append(errnoString);
        sb.append(", errnoMessage: ");
        sb.append(errnoMessage);
        if (syscall != null && syscall.length() > 0) {
            sb.append(", syscall: ");
            sb.append(syscall);
        }
        if (path != null && path.length() > 0) {
            sb.append(", path: ");
            sb.append(path);
        }
        sb.append(", message: ");
        sb.append(super.getMessage());
        return sb.toString();
    }

    public static void static_initialize() {
        _static_initialize();
    }

    private static native void _static_initialize();
}
