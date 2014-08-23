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

import com.iwebpp.libuvpp.cb.TimerCallback;

public class TimerHandle extends Handle {

    private boolean closed;

    private TimerCallback onTimerFired = null;
    private TimerCallback onClose = null;

    static {
        _static_initialize();
    }

    public void setTimerFiredCallback(final TimerCallback callback) {
        onTimerFired = callback;
    }

    public void setCloseCallback(final TimerCallback callback) {
        onClose = callback;
    }

    protected TimerHandle(final LoopHandle loop) {
        super(_new(loop.pointer()), loop);
        _initialize(pointer);
    }

    public int start(final long timeout, final long repeat) {
        return _start(pointer, timeout, repeat);
    }

    public int again() {
        return _again(pointer);
    }

    public long getRepeat() {
        return _get_repeat(pointer);
    }

    public void setRepeat(final long repeat) {
        _set_repeat(pointer, repeat);
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
            case 1: if (onTimerFired != null) {loop.getCallbackHandler().handleTimerCallback(onTimerFired, status);} break;
            case 2: if (onClose != null) {loop.getCallbackHandler().handleTimerCallback(onClose, status);} break;
            default: assert false : "unsupported callback type " + type;
        }
    }

    private static native long _new(final long loop);

    private static native void _static_initialize();

    private native void _initialize(final long ptr);

    private native int _start(final long ptr, final long timeout, final long repeat);

    private native int _again(final long ptr);

    private native long _get_repeat(final long ptr);

    private native void _set_repeat(final long ptr, final long repeat);

    private native int _stop(final long ptr);

    private native void _close(final long ptr);

}
