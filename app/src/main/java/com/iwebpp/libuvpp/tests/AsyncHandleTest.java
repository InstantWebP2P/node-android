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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.iwebpp.libuvpp.cb.AsyncCallback;
import com.iwebpp.libuvpp.handles.AsyncHandle;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.LoopHandle;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class AsyncHandleTest extends TestBase {
	private static final String TAG = "AsyncHandleTest";

    /// @Test
    public void testAsync() throws Throwable {
        final AtomicBoolean gotCallback = new AtomicBoolean(false);
        final AtomicInteger times = new AtomicInteger(0);

        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final AsyncHandle asyncHandle = handleFactory.newAsyncHandle();
        final ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);

        asyncHandle.setAsyncCallback(new AsyncCallback() {
            @Override
            public void onSend(final int status) throws Exception {
                gotCallback.set(true);
                System.out.println("onSend!");
                times.incrementAndGet();
                asyncHandle.close();
            }
        });

        timer.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("calling asyncHandle.send...");
                asyncHandle.send();
            }
        }, 100, TimeUnit.MILLISECONDS);

        loop.run();

        ///Assert.assertTrue(gotCallback.get());
        ///Assert.assertEquals(times.get(), 1);
    }

    static class Holder {
        AsyncHandle asyncHandle;
        volatile boolean initialized = false;
        final int id;
        final AtomicBoolean gotCallback = new AtomicBoolean(false);
        final AtomicInteger times = new AtomicInteger(0);
        final LoopHandle loop = new LoopHandle();
        final Throwable[] errors = new Throwable[1];
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    asyncHandle = new AsyncHandle(loop);
                    asyncHandle.setAsyncCallback(new AsyncCallback() {
                        @Override
                        public void onSend(final int status) throws Exception {
                            gotCallback.set(true);
                            System.out.println(id + " onSend!");
                            times.incrementAndGet();
                            asyncHandle.close();
                        }
                    });
                    synchronized (loop) {
                        initialized = true;
                        loop.notifyAll();
                    }
                    loop.run();
                } catch (Throwable throwable) {
                    errors[0] = throwable;
                }
            }
        };
        Holder(int id) {this.id = id;}
    }

   /// @Test
    public void testAsyncMulti() throws Throwable {
        final int CONCURRENCY = 256;
        final ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);

        final Holder[] holders = new Holder[CONCURRENCY];
        for (int i=0; i < holders.length; i++) {
            final Holder holder = holders[i] = new Holder(i);
            holder.thread.start();
            synchronized (holder.loop) {
                while (!holder.initialized) {
                    holder.loop.wait();
                }
            }
            final int fi = i;
            timer.schedule(new Runnable() {
                @Override
                public void run() {
                    System.out.println(fi + " calling asyncHandle.send...");
                    holder.asyncHandle.send();
                }
            }, (int) (Math.random() * 1000), TimeUnit.MILLISECONDS);
        }

        for (int i=0; i < holders.length; i++) {
            final Holder holder = holders[i];
            holder.thread.join();
            if (holder.errors[0] != null) {
                throw holder.errors[0];
            }

            ///Assert.assertTrue(holder.gotCallback.get());
            ///Assert.assertEquals(holder.times.get(), 1);
        }
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
		Log.d(TAG, "start test");

        final AsyncHandleTest test = new AsyncHandleTest();
        try {
			test.testAsync();
		} catch (Throwable e) {
			e.printStackTrace();
		}
        try {
			test.testAsyncMulti();
		} catch (Throwable e) {
			e.printStackTrace();
		}
    }

}
