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

import com.iwebpp.libuvpp.cb.CheckCallback;

public class CheckHandle extends Handle {

    private boolean closed;

    private CheckCallback onCheck = null;
    private CheckCallback onClose = null;

    static {
        _static_initialize();
    }

    public void setCheckCallback(final CheckCallback callback) {
        onCheck = callback;
    }

    public void setCloseCallback(final CheckCallback callback) {
        onClose = callback;
    }

    protected CheckHandle(final LoopHandle loop) {
        super(_new(loop.pointer()), loop);
        _initialize(pointer);
    }

    public int start() {
        return _start(pointer);
    }

    public int stop() {
        return _stop(pointer);
    }

    public void close() {
        if (!closed) {
            _close(pointer);
        }
        closed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    private void callback(final int type, final int status) {
        switch (type) {
            case 1: if (onCheck != null) {loop.getCallbackHandler().handleCheckCallback(onCheck, status);} break;
            case 2: if (onClose != null) {loop.getCallbackHandler().handleCheckCallback(onClose, status);} break;
            default: assert false : "unsupported callback type " + type;
        }
    }

    private static native long _new(final long loop);

    private static native void _static_initialize();

    private native void _initialize(final long ptr);

    private native int _start(final long ptr);

    private native int _stop(final long ptr);

    private native void _close(final long ptr);

}
