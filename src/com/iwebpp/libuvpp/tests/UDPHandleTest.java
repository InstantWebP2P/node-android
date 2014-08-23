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

import com.iwebpp.libuvpp.Address;
import com.iwebpp.libuvpp.cb.UDPRecvCallback;
import com.iwebpp.libuvpp.cb.UDPSendCallback;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.UDPHandle;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class UDPHandleTest extends TestBase {
	private static final String TAG = "UDPHandleTest";

    private static final String HOST = "127.0.0.1";
    private static final String HOST6 = "::1";
    private static final int PORT = 34567;
    private static final int PORT6 = 45678;
    private static final int TIMES = 10;

    ///@Test
    public void testConnection() throws Throwable {
        final AtomicInteger clientSendCount = new AtomicInteger(0);
        final AtomicInteger serverRecvCount = new AtomicInteger(0);

        final AtomicBoolean serverDone = new AtomicBoolean(false);
        final AtomicBoolean clientDone = new AtomicBoolean(false);

        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final UDPHandle server = handleFactory.newUDPHandle();
        final UDPHandle client = handleFactory.newUDPHandle();

        server.setRecvCallback(new UDPRecvCallback() {
            @Override
            public void onRecv(int nread, ByteBuffer data, Address address) throws Exception {
                if (serverRecvCount.incrementAndGet() < TIMES) {
                } else {
                    server.close();
                    serverDone.set(true);
                }
            }
        });

        client.setSendCallback(new UDPSendCallback() {
            @Override
            public void onSend(int status, Exception error) throws Exception {
                if (clientSendCount.incrementAndGet() < TIMES) {
                } else {
                    client.close();
                    clientDone.set(true);
                }
            }
        });

        server.bind(PORT, HOST);
        server.recvStart();

        for (int i=0; i < TIMES; i++) {
            client.send("PING." + i, PORT, HOST);
        }

        final long start = System.currentTimeMillis();
        while (!serverDone.get()) {
            if (System.currentTimeMillis() - start > TestBase.TIMEOUT) {
                ///Assert.fail("timeout");
            }
            loop.runNoWait();
        }

        ///Assert.assertEquals(clientSendCount.get(), TIMES);
        ///Assert.assertEquals(serverRecvCount.get(), TIMES);
    }

    ///@Test
    public void testConnection6() throws Throwable {
        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        if (!isIPv6Enabled(loop)) {
            return;
        }
        final AtomicInteger clientSendCount = new AtomicInteger(0);
        final AtomicInteger serverRecvCount = new AtomicInteger(0);

        final AtomicBoolean serverDone = new AtomicBoolean(false);
        final AtomicBoolean clientDone = new AtomicBoolean(false);

        final UDPHandle server = handleFactory.newUDPHandle();
        final UDPHandle client = handleFactory.newUDPHandle();

        server.setRecvCallback(new UDPRecvCallback() {
            @Override
            public void onRecv(int nread, ByteBuffer data, Address address) throws Exception {
                if (serverRecvCount.incrementAndGet() < TIMES) {
                    System.out.printf(TAG+",server6.onRecv nread: %d, data: %s, addr: %s\n", nread, new String(data.array()), address);
                } else {
                    server.close();
                    serverDone.set(true);
                }
            }
        });

        client.setSendCallback(new UDPSendCallback() {
            @Override
            public void onSend(int status, Exception error) throws Exception {
                if (clientSendCount.incrementAndGet() < TIMES) {
                } else {
                    client.close();
                    clientDone.set(true);
                }
            }
        });

        server.bind6(PORT6, HOST6);
        server.recvStart();

        for (int i=0; i < TIMES; i++) {
            client.send6("PING." + i, PORT6, HOST6);
        }

        final long start = System.currentTimeMillis();
        while (!serverDone.get()) {
            if (System.currentTimeMillis() - start > TestBase.TIMEOUT) {
                ///Assert.fail("timeout");
            }
            loop.runNoWait();
        }

        ///Assert.assertEquals(clientSendCount.get(), TIMES);
        ///Assert.assertEquals(serverRecvCount.get(), TIMES);
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
        final UDPHandleTest test = new UDPHandleTest();
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

    public static boolean isIPv6Enabled(final LoopHandle loop) {
        UDPHandle socket = null;
        try {
            socket = new UDPHandle(loop);
            socket.recvStart();
            socket.send6("", PORT6, HOST6);
        } catch(final com.iwebpp.libuvpp.NativeException e) {
            if ("EAFNOSUPPORT".equals(e.errnoString())) {
                return false;
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        return true;
    }
}
