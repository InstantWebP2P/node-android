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

package com.iwebpp.libuvpp.tests;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;

import com.iwebpp.libuvpp.cb.ProcessCloseCallback;
import com.iwebpp.libuvpp.cb.ProcessExitCallback;
import com.iwebpp.libuvpp.cb.StreamConnectCallback;
import com.iwebpp.libuvpp.cb.StreamConnectionCallback;
import com.iwebpp.libuvpp.cb.StreamReadCallback;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.PipeHandle;
import com.iwebpp.libuvpp.handles.ProcessHandle;
import com.iwebpp.libuvpp.handles.StdioOptions;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class ProcessHandleTest extends TestBase {
	private static final String TAG = "CheckHandleTest";

    private static final String OS = System.getProperty("os.name");

    ///@Test
    public void testExitCode() throws Throwable {
        final String MESSAGE = "TEST";
        final String PIPE_NAME;
        if (OS.startsWith("Windows")) {
            PIPE_NAME = "\\\\.\\pipe\\libuv-java-process-handle-test-pipe";
        } else {
            PIPE_NAME = "/tmp/libuv-java-process-handle-test-pipe";
            ///Files.deleteIfExists(FileSystems.getDefault().getPath(PIPE_NAME));
        }

        final AtomicBoolean exitCalled = new AtomicBoolean(false);
        final AtomicBoolean closeCalled = new AtomicBoolean(false);
        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final ProcessHandle process = handleFactory.newProcessHandle();
        final PipeHandle parent = handleFactory.newPipeHandle(false);
        final PipeHandle peer = handleFactory.newPipeHandle(false);
        final PipeHandle child = handleFactory.newPipeHandle(false);

        peer.setReadCallback(new StreamReadCallback() {
            @Override
            public void onRead(final ByteBuffer data) throws Exception {
                final byte[] bytes = data.array();
                final String s = new String(bytes, "utf-8");
                ///Assert.assertEquals(s, MESSAGE);
                peer.close();
                process.close();
            }
        });

        parent.setConnectionCallback(new StreamConnectionCallback() {
            @Override
            public void onConnection(int status, Exception error) throws Exception {
                parent.accept(peer);
                peer.readStart();
                parent.close();
            }
        });

        child.setConnectCallback(new StreamConnectCallback() {
            @Override
            public void onConnect(int status, Exception error) throws Exception {
                child.write(MESSAGE);
                child.close();
            }
        });

        process.setExitCallback(new ProcessExitCallback() {
            @Override
            public void onExit(final int status, final int signal, final Exception error) throws Exception {
                System.out.println("status " + status + ", signal " + signal);
                child.connect(PIPE_NAME);
                exitCalled.set(true);
            }
        });

        process.setCloseCallback(new ProcessCloseCallback() {
            @Override
            public void onClose() throws Exception {
                closeCalled.set(true);
            }
        });

        final String[] args = new String[2];
        args[0] = "java";
        args[1] = "-version";

        final EnumSet<ProcessHandle.ProcessFlags> processFlags = EnumSet.noneOf(ProcessHandle.ProcessFlags.class);
        processFlags.add(ProcessHandle.ProcessFlags.NONE);

        final StdioOptions[] stdio = new StdioOptions[3];
        stdio[0] = new StdioOptions(StdioOptions.StdioType.INHERIT_FD, null, 0);
        stdio[1] = new StdioOptions(StdioOptions.StdioType.INHERIT_FD, null, 1);
        stdio[2] = new StdioOptions(StdioOptions.StdioType.INHERIT_FD, null, 2);

        parent.bind(PIPE_NAME);
        parent.listen(0);

        process.spawn(args[0], args, null, ".", processFlags, stdio, -1, -1);

        while (!exitCalled.get() && !closeCalled.get()) {
            loop.run();
        }
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
  		Log.d(TAG, "start test");
  		
        final ProcessHandleTest test = new ProcessHandleTest();
        try {
			test.testExitCode();
		} catch (Throwable e) {
			e.printStackTrace();
		}
    }
}
