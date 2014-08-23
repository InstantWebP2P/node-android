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
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.iwebpp.libuvpp.cb.StreamCloseCallback;
import com.iwebpp.libuvpp.cb.StreamConnectCallback;
import com.iwebpp.libuvpp.cb.StreamConnectionCallback;
import com.iwebpp.libuvpp.cb.StreamReadCallback;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.TCPHandle;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class TCPHandleTest extends TestBase {
	private static final String TAG = "TCPHandleTest";
	
    private static final String ADDRESS = "127.0.0.1";
    private static final String ADDRESS6 = "::1";
    private static final int PORT = 23456;
    private static final int PORT6 = 34567;
    private static final int TIMES = 10;

    public void testConnection() throws Throwable {
        final AtomicInteger serverSendCount = new AtomicInteger(0);
        final AtomicInteger clientSendCount = new AtomicInteger(0);

        final AtomicInteger serverRecvCount = new AtomicInteger(0);
        final AtomicInteger clientRecvCount = new AtomicInteger(0);

        final AtomicBoolean serverDone = new AtomicBoolean(false);
        final AtomicBoolean clientDone = new AtomicBoolean(false);

        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final TCPHandle server = handleFactory.newTCPHandle();
        final TCPHandle peer = handleFactory.newTCPHandle();
        final TCPHandle client = handleFactory.newTCPHandle();

        final Logger serverLoggingCallback = new Logger("s: ");
        final Logger clientLoggingCallback = new Logger("c: ");

        final Random random = new Random();

        server.setConnectionCallback(new StreamConnectionCallback() {
            @Override
            public void onConnection(int status, Exception error) throws Exception {
                serverLoggingCallback.log(status, error);
                server.accept(peer);
                peer.readStart();
                Log.d(TAG, "s: " + server.getSocketName() + " connected to " + peer.getPeerName());
                peer.write("message " + serverSendCount.getAndIncrement() + " from server");
                server.close(); // not expecting any more connections
            }
        });

        peer.setReadCallback(new StreamReadCallback() {
            @Override
            public void onRead(final ByteBuffer data) throws Exception {
                serverRecvCount.incrementAndGet();
                if (data == null) {
                    peer.close();
                } else {
                    final Object[] args = {data};
                    serverLoggingCallback.log(args);
                    if (serverRecvCount.get() == TIMES) {
                        peer.close();
                    } else {
                        peer.write("message " + serverSendCount.getAndIncrement() + " from server");
                    }
                }
            }
        });

        peer.setCloseCallback(new StreamCloseCallback() {
            @Override
            public void onClose() throws Exception { // close
                serverDone.set(true);
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
                    if (clientRecvCount.get() == TIMES) {
                        client.close();
                    } else {
                        client.write("message " + clientSendCount.getAndIncrement() + " from client");
                    }
                }
            }
        });

        client.setConnectCallback(new StreamConnectCallback() {
            @Override
            public void onConnect(int status, Exception error) throws Exception { // connect
                clientLoggingCallback.log(status, error);
                Log.d(TAG, "c: " + client.getSocketName() + " connected to " + client.getPeerName());
                client.readStart();
                client.write("message " + clientSendCount.getAndIncrement() + " from client");
            }
        });

        client.setCloseCallback(new StreamCloseCallback() {
            @Override
            public void onClose() throws Exception { // close
                clientDone.set(true);
            }
        });

        server.bind(ADDRESS, PORT);
        server.listen(1);

        Thread.sleep((long) (random.nextDouble() * 100));
        client.connect(ADDRESS, PORT);

        while (!serverDone.get() || !clientDone.get()) {
            loop.run();
        }

        ///Assert.assertEquals(serverSendCount.get(), TIMES);
        ///Assert.assertEquals(clientSendCount.get(), TIMES);
        ///Assert.assertEquals(serverRecvCount.get(), TIMES);
        ///Assert.assertEquals(clientRecvCount.get(), TIMES);
    }

    public void testConnection6() throws Throwable {
        final LoopHandle loop = new LoopHandle();
        if (!UDPHandleTest.isIPv6Enabled(loop)) {
            return;
        }

        final AtomicInteger serverSendCount = new AtomicInteger(0);
        final AtomicInteger clientSendCount = new AtomicInteger(0);

        final AtomicInteger serverRecvCount = new AtomicInteger(0);
        final AtomicInteger clientRecvCount = new AtomicInteger(0);

        final AtomicBoolean serverDone = new AtomicBoolean(false);
        final AtomicBoolean clientDone = new AtomicBoolean(false);

        final TCPHandle server = new TCPHandle(loop);
        final TCPHandle peer = new TCPHandle(loop);
        final TCPHandle client = new TCPHandle(loop);

        final Logger serverLoggingCallback = new Logger("s: ");
        final Logger clientLoggingCallback = new Logger("c: ");

        final Random random = new Random();

        server.setConnectionCallback(new StreamConnectionCallback() {
            @Override
            public void onConnection(int status, Exception error) throws Exception {
                serverLoggingCallback.log(status, error);
                server.accept(peer);
                peer.readStart();
                Log.d(TAG, "s: " + server.getSocketName() + " connected to " + peer.getPeerName());
                peer.write("message " + serverSendCount.getAndIncrement() + " from server");
                server.close(); // not expecting any more connections
            }
        });

        peer.setReadCallback(new StreamReadCallback() {
            @Override
            public void onRead(final ByteBuffer data) throws Exception {
                serverRecvCount.incrementAndGet();
                if (data == null) {
                    peer.close();
                } else {
                    final Object[] args = {data};
                    serverLoggingCallback.log(args);
                    if (serverRecvCount.get() == TIMES) {
                        peer.close();
                    } else {
                        peer.write("message " + serverSendCount.getAndIncrement() + " from server");
                    }
                }
            }
        });

        peer.setCloseCallback(new StreamCloseCallback() {
            @Override
            public void onClose() throws Exception { // close
                serverDone.set(true);
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
                    if (clientRecvCount.get() == TIMES) {
                        client.close();
                    } else {
                        client.write("message " + clientSendCount.getAndIncrement() + " from client");
                    }
                }
            }
        });

        client.setConnectCallback(new StreamConnectCallback() {
            @Override
            public void onConnect(int status, Exception error) throws Exception {
                clientLoggingCallback.log(status, error);
                Log.d(TAG, "c: " + client.getSocketName() + " connected to " + client.getPeerName());
                client.readStart();
                client.write("message " + clientSendCount.getAndIncrement() + " from client");
            }
        });

        client.setCloseCallback(new StreamCloseCallback() {
            @Override
            public void onClose() throws Exception { // close
                clientDone.set(true);
            }
        });

        server.bind6(ADDRESS6, PORT6);
        server.listen(1);

        Thread.sleep((long) (random.nextDouble() * 100));
        client.connect6(ADDRESS6, PORT6);

        while (!serverDone.get() || !clientDone.get()) {
            loop.run();
        }

        ///Assert.assertEquals(serverSendCount.get(), TIMES);
        ///Assert.assertEquals(clientSendCount.get(), TIMES);
        ///Assert.assertEquals(serverRecvCount.get(), TIMES);
        ///Assert.assertEquals(clientRecvCount.get(), TIMES);
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
		Log.d(TAG, "start test");

        final TCPHandleTest test = new TCPHandleTest();
        try {
			test.testConnection();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			test.testConnection6();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}
