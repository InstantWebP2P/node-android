/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.iwebpp.libuvpp.Files;

public class DefaultHandleFactory implements HandleFactory {

    private LoopHandle loop;

    public static HandleFactory newFactory() { // generally only for tests
        return new DefaultHandleFactory().initialize(new LoopHandle());
    }

    @Override
    public HandleFactory initialize(final LoopHandle loop) {
        if (this.loop != null) throw new IllegalStateException("already initialized");
        this.loop = loop;
        return this;
    }

    @Override
    public LoopHandle getLoopHandle() {
        return loop;
    }

    @Override
    public AsyncHandle newAsyncHandle() {
        assert loop != null;
        return new AsyncHandle(loop);
    }

    @Override
    public CheckHandle newCheckHandle() {
        assert loop != null;
        return new CheckHandle(loop);
    }

    @Override
    public IdleHandle newIdleHandle() {
        assert loop != null;
        return new IdleHandle(loop);
    }

    @Override
    public PipeHandle newPipeHandle(final boolean ipc) {
        assert loop != null;
        return new PipeHandle(loop, ipc);
    }

    @Override
    public PipeHandle newPipeHandle(final long pointer,
                                    final boolean ipc) {
        assert loop != null;
        return new PipeHandle(loop, pointer, ipc);
    }

    @Override
    public PollHandle newPollHandle(final int fd) {
        assert loop != null;
        return new PollHandle(loop, fd);
    }

    @Override
    public PollHandle newPollHandle(final long socket) {
        assert loop != null;
        return new PollHandle(loop, socket);
    }

    @Override
    public ProcessHandle newProcessHandle() {
        assert loop != null;
        return new ProcessHandle(loop);
    }

    @Override
    public SignalHandle newSignalHandle() {
        assert loop != null;
        return new SignalHandle(loop);
    }

    @Override
    public TCPHandle newTCPHandle() {
        assert loop != null;
        return new TCPHandle(loop);
    }

    @Override
    public TCPHandle newTCPHandle(final long pointer) {
        assert loop != null;
        return new TCPHandle(loop, pointer, true);
    }

    @Override
    public TCPHandle openTCPHandle(final long socket) {
        assert loop != null;
        return new TCPHandle(loop, socket);
    }

    @Override
    public TimerHandle newTimerHandle() {
        assert loop != null;
        return new TimerHandle(loop);
    }

    @Override
    public TTYHandle newTTYHandle(final int fd,
                                  final boolean readable) {
        assert loop != null;
        return new TTYHandle(loop, fd, readable);
    }

    @Override
    public UDPHandle newUDPHandle() {
        assert loop != null;
        return new UDPHandle(loop);
    }

    @Override
    public UDPHandle newUDPHandle(final long pointer) {
        assert loop != null;
        return new UDPHandle(loop, pointer, true);
    }

    @Override
    public UDPHandle openUDPHandle(final long socket) {
        assert loop != null;
        return new UDPHandle(loop, socket);
    }

    @Override
    public FileEventHandle newFileEventHandle() {
        assert loop != null;
        return new FileEventHandle(loop);
    }

    @Override
    public FilePollHandle newFilePollHandle() {
        assert loop != null;
        return new FilePollHandle(loop);
    }

    @Override
    public Files newFiles() {
        assert loop != null;
        return new FilesWrapper(loop);
    }

    private static class FilesWrapper extends Files {
        FilesWrapper(final LoopHandle loop) {
            super(loop);
        }
    }
}
