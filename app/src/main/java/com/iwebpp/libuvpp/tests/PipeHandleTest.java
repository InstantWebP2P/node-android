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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.iwebpp.libuvpp.cb.StreamCloseCallback;
import com.iwebpp.libuvpp.cb.StreamConnectCallback;
import com.iwebpp.libuvpp.cb.StreamConnectionCallback;
import com.iwebpp.libuvpp.cb.StreamReadCallback;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.PipeHandle;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class PipeHandleTest extends TestBase {
	private static final String TAG = "PipeHandleTest";

    private static final String OS = System.getProperty("os.name");
    private static final int TIMES = 10;

    ///@Test
    public void testConnection() throws Throwable {
        final String PIPE_NAME;
        if (OS.startsWith("Windows")) {
            PIPE_NAME = "\\\\.\\pipe\\libuv-java-pipe-handle-test-pipe";
        } else {
            PIPE_NAME = "/tmp/libuv-java-pipe-handle-test-pipe";
            ///Files.deleteIfExists(FileSystems.getDefault().getPath(PIPE_NAME));
        }

        final AtomicInteger serverSendCount = new AtomicInteger(0);
        final AtomicInteger clientSendCount = new AtomicInteger(0);

        final AtomicInteger serverRecvCount = new AtomicInteger(0);
        final AtomicInteger clientRecvCount = new AtomicInteger(0);

        final AtomicBoolean serverDone = new AtomicBoolean(false);
        final AtomicBoolean clientDone = new AtomicBoolean(false);

        final Logger serverLoggingCallback = new Logger("S: ");
        final Logger clientLoggingCallback = new Logger("C: ");

        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final PipeHandle server = handleFactory.newPipeHandle(false);
        final PipeHandle peer = handleFactory.newPipeHandle(false);
        final PipeHandle client = handleFactory.newPipeHandle(false);

        peer.setReadCallback(new StreamReadCallback() {
            @Override
            public void onRead(final ByteBuffer data) throws Exception {
                serverRecvCount.incrementAndGet();
                if (data == null) {
                    peer.close();
                } else {
                    final Object[] args = {data};
                    serverLoggingCallback.log(args);
                    if (serverSendCount.get() < TIMES) {
                        peer.write("PING " + serverSendCount.incrementAndGet());
                    } else {
                        peer.close();
                    }
                }
            }
        });

        peer.setCloseCallback(new StreamCloseCallback() {
            @Override
            public void onClose() throws Exception {
                serverDone.set(true);
            }
        });

        server.setConnectionCallback(new StreamConnectionCallback() {
            @Override
            public void onConnection(int status, Exception error) throws Exception {
                serverLoggingCallback.log(status, error);
                server.accept(peer);
                peer.readStart();
                peer.write("INIT " + serverSendCount.incrementAndGet());
                server.close(); // not expecting any more connections
            }
        });

        client.setConnectCallback(new StreamConnectCallback() {
            @Override
            public void onConnect(int status, Exception error) throws Exception {
                clientLoggingCallback.log(status, error);
                client.readStart();
            }
        });

        client.setReadCallback(new StreamReadCallback() {
            @Override
            public void onRead(final ByteBuffer data) throws Exception {
                clientRecvCount.incrementAndGet();
                if (data == null) {
                    client.close();
                } else {
                    final Object[] args = {data};
                    clientLoggingCallback.log(args);
                    if (clientSendCount.incrementAndGet() < TIMES) {
                        client.write("PONG " + clientSendCount.get());
                    } else {
                        client.close();
                    }
                }
            }
        });

        client.setCloseCallback(new StreamCloseCallback() {
            @Override
            public void onClose() throws Exception {
                clientDone.set(true);
            }
        });

        server.bind(PIPE_NAME);
        server.listen(0);

        client.connect(PIPE_NAME);

        while (!serverDone.get() && !clientDone.get()) {
            loop.run();
        }

        client.close();
        server.close();

        ///Assert.assertEquals(serverSendCount.get(), TIMES);
        ///Assert.assertEquals(clientSendCount.get(), TIMES);
        ///Assert.assertEquals(serverRecvCount.get(), TIMES);
        ///Assert.assertEquals(clientRecvCount.get(), TIMES);
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
		Log.d(TAG, "start test");
		
        try {
			new PipeHandleTest().testConnection();
		} catch (Throwable e) {
			e.printStackTrace();
		}
    }

}
