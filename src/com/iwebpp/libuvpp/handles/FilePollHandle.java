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

import com.iwebpp.libuvpp.Stats;
import com.iwebpp.libuvpp.cb.FilePollCallback;
import com.iwebpp.libuvpp.cb.FilePollStopCallback;

public class FilePollHandle extends Handle {

    private boolean closed;

    private FilePollCallback onPoll = null;
    private FilePollStopCallback onStop = null;
    private String path = null;

    private final Stats previous;
    private final Stats current;

    static {
        _static_initialize();
    }

    public void setFilePollCallback(final FilePollCallback callback) {
        onPoll = callback;
    }

    public void setStopCallback(final FilePollStopCallback callback) {
        onStop = callback;
    }

    protected FilePollHandle(final LoopHandle loop) {
        super(_new(loop.pointer()), loop);
        _initialize(pointer);
        previous = new Stats();
        current = new Stats();
    }

    public int start(final String path, final boolean persistent, final int interval) {
        Objects.requireNonNull(path);
        if (this.path != null) {
            throw new IllegalStateException("Already polling " + this.path);
        }
        this.path = path;
        return _start(pointer, path, persistent, interval, previous, current);
    }

    public int stop() {
        this.path = null;
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

    private void callPoll(final int status) {
        if (onPoll != null) {
            loop.getCallbackHandler().handleFilePollCallback(onPoll, status, previous, current);
        }
    }

    private void callStop() {
        if (onStop != null) {
            loop.getCallbackHandler().handleFilePollStopCallback(onStop);
        }
    }

    private static native long _new(final long loop);

    private static native void _static_initialize();

    private native void _initialize(final long ptr);

    private native int _start(final long ptr, final String path, final boolean persistent, final int interval, final Stats previous, final Stats current);

    private native int _stop(final long ptr);

    private native void _close(final long ptr);

}
