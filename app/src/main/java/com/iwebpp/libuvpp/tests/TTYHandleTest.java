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

import android.util.Log;

import com.iwebpp.libuvpp.NativeException;
import com.iwebpp.libuvpp.cb.StreamReadCallback;
import com.iwebpp.libuvpp.cb.StreamWriteCallback;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.TTYHandle;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class TTYHandleTest extends TestBase {
	private static final String TAG = "TTYHandleTest";

    private TTYHandle newTTY(final HandleFactory factory, final int fd, final boolean readable) {
        final TTYHandle tty;
        try {
            tty = factory.newTTYHandle(fd, readable);
        } catch (final NativeException nx) {
            if ("EBADF".equals(nx.errnoString())) {
                // running under a test runner or ant
                System.out.println("EBADF error creating tty " + fd);
                return null;
            }
            throw nx; // any other error fails the test
        }
        if (!TTYHandle.isTTY(fd)) {
            System.out.println("not a tty " + fd);
            return null;
        }
        return tty;
    }

   /// @Test
    public void testStdOutErrWindowSize() {
        testStdOutErrWindowSize("stdout", 1);
        testStdOutErrWindowSize("stderr", 2);
    }

    private void testStdOutErrWindowSize(final String name, final int fd) {
        final HandleFactory handleFactory = newFactory();
        final TTYHandle tty = newTTY(handleFactory, fd, false);
        if (tty == null) {
            return;
        }
        testWindowSize(name, tty);
    }

   /// @Test
    public void testStdinWindowSize() {
        final HandleFactory handleFactory = newFactory();
        final TTYHandle tty = newTTY(handleFactory, 0, true);
        if (tty == null) {
            return;
        }
        testWindowSize("stdin", tty);
    }

    private void testWindowSize(final String name, final TTYHandle tty) {
        final int[] windowSize = tty.getWindowSize();
        ///Assert.assertNotNull(windowSize);
        ///Assert.assertEquals(windowSize.length, 2);
        ///Assert.assertNotNull(windowSize[0]);
        ///Assert.assertNotNull(windowSize[1]);
        ///Assert.assertTrue(windowSize[0] > 10);
        ///Assert.assertTrue(windowSize[1] > 10);
        System.out.println("tty " + name + " window size: " + windowSize[0] + " : " + windowSize[1]);
    }

   /// @Test
    public void testWrite() throws Throwable {
        testWrite("stdout", 1);
        testWrite("stderr", 2);
    }

    private void testWrite(final String name, final int fd) throws Throwable {
        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final TTYHandle tty = newTTY(handleFactory, fd, false);
        if (tty == null) {
            return;
        }
        tty.setWriteCallback(new StreamWriteCallback() {
            @Override
            public void onWrite(int status, Exception error) throws Exception {
                System.out.println(status);
                ///Assert.assertEquals(status, 0);
                ///Assert.assertNotNull(error);
            }
        });
        tty.write("written to " + name + "\n");
        loop.run();
    }

   /// @Test
    public void testRead() throws Throwable {
        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final TTYHandle tty = newTTY(handleFactory, 0, true);
        if (tty == null) {
            return;
        }
        final String prompt = "\ntype something (^D to exit) > ";
        tty.setReadCallback(new StreamReadCallback() {
            @Override
            public void onRead(final ByteBuffer data) throws Exception {
                if (data != null) {
                    System.out.print(new String(data.array()));
                    System.out.print(prompt);
                } else {
                    System.out.print("\n");
                }
            }
        });
        System.out.print(prompt);
        tty.readStart();
        loop.run();
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
		Log.d(TAG, "start test");
		
        final TTYHandleTest test = new TTYHandleTest();
        try {
			test.testWrite();
		} catch (Throwable e) {
			e.printStackTrace();
		}
        test.testStdOutErrWindowSize();

        try {
			test.testRead();
		} catch (Throwable e) {
			e.printStackTrace();
		}
        test.testStdinWindowSize();
    }
}
