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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.iwebpp.libuvpp.cb.TimerCallback;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.TimerHandle;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class TimerHandleTest extends TestBase {
	private static final String TAG = "TimerHandleTest";

    private static final int TIMES = 100;

   /// @Test
    public void testOnce() throws Throwable {
        final AtomicBoolean gotCallback = new AtomicBoolean(false);
        final AtomicBoolean gotClose = new AtomicBoolean(false);

        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final TimerHandle timer = handleFactory.newTimerHandle();

        timer.setCloseCallback(new TimerCallback() {
            @Override
            public void onTimer(final int i) throws Exception {
                System.out.println("timer closed");
                gotClose.set(true);
            }
        });

        timer.setTimerFiredCallback(new TimerCallback() {
            @Override
            public void onTimer(final int status) throws Exception {
                gotCallback.set(true);
                System.out.println("timer fired once");
                timer.close();
            }
        });

        timer.start(100, 0);

        final long start = System.currentTimeMillis();
        while (!gotCallback.get() || !gotClose.get()) {
            if (System.currentTimeMillis() - start > TestBase.TIMEOUT) {
                ///Assert.fail("timeout waiting for timer");
            }
            loop.runNoWait();
        }

        ///Assert.assertTrue(gotCallback.get());
        ///Assert.assertTrue(gotClose.get());
    }

   /// @Test
    public void testRepeat() throws Throwable {
        final AtomicBoolean gotCallback = new AtomicBoolean(false);
        final AtomicBoolean gotClose = new AtomicBoolean(false);
        final AtomicInteger callbackCount = new AtomicInteger(0);

        final LoopHandle loop = new LoopHandle();
        final TimerHandle timer = new TimerHandle(loop);

        timer.setCloseCallback(new TimerCallback() {
            @Override
            public void onTimer(final int i) throws Exception {
                System.out.println("repeat timer closed");
                gotClose.set(true);
            }
        });

        timer.setTimerFiredCallback(new TimerCallback() {
            @Override
            public void onTimer(final int status) throws Exception {
                gotCallback.set(true);
                if (callbackCount.incrementAndGet() == TIMES) {
                    System.out.println("closing repeat timer");
                    gotClose.set(true);
                }
                System.out.println("timer fired " + callbackCount.get());
            }
        });

        timer.start(50, 5);

        final long start = System.currentTimeMillis();
        while (!gotCallback.get() || !gotClose.get()) {
            if (System.currentTimeMillis() - start > TestBase.TIMEOUT) {
                ///Assert.fail("timeout waiting for timer");
            }
            loop.runNoWait();
        }

        ///Assert.assertTrue(gotCallback.get());
        ///Assert.assertTrue(gotClose.get());
        ///Assert.assertTrue(callbackCount.get() == TIMES);
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
		Log.d(TAG, "start test");
		
        final TimerHandleTest test = new TimerHandleTest();
        try {
			test.testOnce();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        /*try {
			test.testRepeat();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
    }

}
