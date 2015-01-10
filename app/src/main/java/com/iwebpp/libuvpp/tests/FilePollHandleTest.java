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

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.iwebpp.libuvpp.Constants;
import com.iwebpp.libuvpp.Files;
import com.iwebpp.libuvpp.Stats;
import com.iwebpp.libuvpp.cb.FileOpenCallback;
import com.iwebpp.libuvpp.cb.FilePollCallback;
import com.iwebpp.libuvpp.cb.FilePollStopCallback;
import com.iwebpp.libuvpp.handles.FilePollHandle;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class FilePollHandleTest extends TestBase {
	
	private static final String TAG = "FilePollHandleTest";

    private String testName;

    ///@BeforeMethod
    public void startSession(final Method method) throws Exception {
        testName = (TestBase.TMPDIR.endsWith(File.separator) ? TestBase.TMPDIR : TestBase.TMPDIR + File.separator) + method.getName();
    }

   /// @Test
    public void testFilePoll() throws Throwable {
        final AtomicBoolean gotCallback = new AtomicBoolean(false);
        final AtomicBoolean gotStop = new AtomicBoolean(false);
        final AtomicInteger times = new AtomicInteger(0);

        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final Files handle = handleFactory.newFiles();
        final FilePollHandle pollHandle = new FilePollHandle(loop);

        handle.setOpenCallback(new FileOpenCallback() {
            @Override
            public void onOpen(final Object context, final int fd, final Exception error) throws Exception {
                handle.ftruncate(fd, 1000);
                handle.close(fd);
            }
        });

        pollHandle.setStopCallback(new FilePollStopCallback() {
            @Override
            public void onStop() throws Exception {
                System.out.println("poll stop");
                handle.unlink(testName);
                gotStop.set(true);
            }
        });

        pollHandle.setFilePollCallback(new FilePollCallback() {
            @Override
            public void onPoll(int status, Stats prev, Stats curr) throws Exception {
                ///Assert.assertEquals(status, 0);
                ///Assert.assertEquals(prev.getSize(), 0);
                ///Assert.assertEquals(curr.getSize(), 1000);
                gotCallback.set(true);
                System.out.println("poll");
                times.incrementAndGet();
                pollHandle.close();
            }
        });

        final int fd = handle.open(testName, Constants.O_WRONLY | Constants.O_CREAT, Constants.S_IRWXU);
        handle.close(fd);

        pollHandle.start(testName, true, 1);

        handle.open(testName, Constants.O_WRONLY | Constants.O_CREAT, Constants.S_IRWXU, this);

        final long start = System.currentTimeMillis();
        while (!gotCallback.get() || !gotStop.get()) {
            if (System.currentTimeMillis() - start > TIMEOUT) {
                ///Assert.fail("timeout waiting for file poll");
            }
            loop.runNoWait();
        }

        ///Assert.assertTrue(gotCallback.get());
        ///Assert.assertTrue(gotStop.get());
        ///Assert.assertEquals(times.get(), 1);
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
		Log.d(TAG, "start test");
		
        final FilePollHandleTest test = new FilePollHandleTest();
        try {
			test.testFilePoll();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}

