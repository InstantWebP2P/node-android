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

package com.iwebpp.libuvpp.handles;

public class TTYHandle extends StreamHandle {

    private final int fd;

    static {
        _static_initialize();
    }

    public enum Mode {

        // must be equal to values in uv.h
        NORMAL(0),
        RAW(1);

        final int value;

        Mode(final int value) {
            this.value = value;
        }
    }

    protected TTYHandle(final LoopHandle loop,
                        final int fd,
                        final boolean readable) {
        super(_new(loop.pointer(), fd, readable), loop);
        this.fd = fd;
    }

    public int getFd() {
        return fd;
    }

    public int setMode(final Mode mode) {
        return _set_mode(pointer, mode.value);
    }

    public void resetMode() {
        _reset_mode(pointer);
    }

    public int[] getWindowSize() {
        return _get_window_size(pointer);
    }

    public static boolean isTTY(final int fd) {
        return _is_tty(fd);
    }

    public static String guessHandleType(final int fd) {
        return _guess_handle_type(fd);
    }

    private static native void _static_initialize();

    private static native long _new(final long loop,
                                    final int fd,
                                    final boolean readable);

    private native int _set_mode(final long pointer,
                                 final int mode);

    private native void _reset_mode(final long pointer);

    private native int[] _get_window_size(final long pointer);

    private static native boolean _is_tty(final int fd);

    private static native String _guess_handle_type(final int fd);

}
