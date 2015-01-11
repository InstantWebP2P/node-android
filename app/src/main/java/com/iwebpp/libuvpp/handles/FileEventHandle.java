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

import com.iwebpp.libuvpp.cb.FileEventCallback;

public class FileEventHandle extends Handle {

    // must be equal to values in uv.h
    private enum EventType {
        UKNOWN(0, ""),
        RENAME(1, "rename"),
        CHANGE(2, "change");

        final int value;
        final String string;

        private EventType(final int value, final String string) {
            this.value = value;
            this.string = string;
        }
    }
    private boolean closed;

    private FileEventCallback onEvent = null;
    private FileEventCallback onClose = null;

    static {
        _static_initialize();
    }

    public void setFileEventCallback(final FileEventCallback callback) {
        onEvent = callback;
    }

    public void setCloseCallback(final FileEventCallback callback) {
        onClose = callback;
    }

    public FileEventHandle(final LoopHandle loop) {
        super(_new(), loop);
        _initialize(pointer);
    }

    public int start(final String path, final boolean persistent) {
        Objects.requireNonNull(path);
        return _start(loop.pointer(), pointer, path, persistent);
    }

    public void stop() {
        _close(pointer);
        closed = true;
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

    private void callback(final int type, final int status, final int event, final String filename) {
        String eventStr = null;
        if (status != 0) {
            eventStr = EventType.UKNOWN.string;
        } else if ((event & EventType.RENAME.value) != 0) {
            eventStr = EventType.RENAME.string;
        } else if ((event & EventType.CHANGE.value) != 0) {
            eventStr = EventType.CHANGE.string;
        }

        switch (type) {
            case 1: if (onEvent != null) {loop.getCallbackHandler().handleFileEventCallback(onEvent, status, eventStr, filename);} break;
            case 2: if (onClose != null) {loop.getCallbackHandler().handleFileEventCallback(onClose, status, eventStr, filename);} break;
            default: assert false : "unsupported callback type " + type;
        }
    }

    private static native long _new();

    private static native void _static_initialize();

    private native void _initialize(final long ptr);

    private native int _start(final long loopPtr, final long ptr, final String path, final boolean persistent);

    private native void _close(final long ptr);

}
