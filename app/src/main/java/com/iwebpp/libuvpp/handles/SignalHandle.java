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

import com.iwebpp.libuvpp.Constants;
import com.iwebpp.libuvpp.LibUVPermission;
import com.iwebpp.libuvpp.cb.SignalCallback;

public class SignalHandle extends Handle {

    private SignalCallback onSignal = null;
    private boolean closed;

    static {
        _static_initialize();
    }

    public void setSignalCallback(final SignalCallback callback) {
        onSignal = callback;
    }

    protected SignalHandle(final LoopHandle loop) {
        super(_new(loop.pointer()), loop);
        _initialize(pointer);
    }

    public void start(final String signal) {
        Objects.requireNonNull(signal);
        start(Constants.getConstants().get(signal));
    }

    public void start(final int signum) {
        LibUVPermission.checkPermission(LibUVPermission.SIGNAL + signum);
        _start(pointer, signum);
    }

    public void stop() {
        _stop(pointer);
    }

    public void close() {
        if (!closed) {
            stop();
        }
        closed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }

    private void callback(final int signum) {
       loop.getCallbackHandler().handleSignalCallback(onSignal, signum);
    }

    private static native long _new(final long loop);

    private static native void _static_initialize();

    private native void _initialize(final long ptr);

    private native int _start(final long ptr, final int signum);

    private native int _stop(final long ptr);

}
