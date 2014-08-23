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

import java.util.Objects;

import com.iwebpp.libuvpp.LibUVPermission;

public class PipeHandle extends StreamHandle {

    protected PipeHandle(final LoopHandle loop,
                         final boolean ipc) {
        super(_new(loop.pointer(), ipc), loop);
    }

    protected PipeHandle(final LoopHandle loop,
                         final long pointer,
                         final boolean ipc) {
        super(pointer, loop);
    }

    public int open(final int fd) {
        LibUVPermission.checkPermission(LibUVPermission.PIPE_OPEN);
        return _open(pointer, fd);
    }

    public int bind(final String name) {
        Objects.requireNonNull(name);
        LibUVPermission.checkPermission(LibUVPermission.PIPE_BIND);
        return _bind(pointer, name);
    }

    @Override
    public int accept(final StreamHandle client) {
        LibUVPermission.checkPermission(LibUVPermission.PIPE_ACCEPT);
        return super.accept(client);
    }

    public void connect(final String name) {
        Objects.requireNonNull(name);
        LibUVPermission.checkPermission(LibUVPermission.PIPE_CONNECT);
        _connect(pointer, name, loop.getContext());
    }

    private static native long _new(final long loop, final boolean ipc);

    private native int _open(final long ptr, final int fd);

    private native int _bind(final long ptr, final String name);

    private native void _connect(final long ptr, final String name, final Object context);

}
