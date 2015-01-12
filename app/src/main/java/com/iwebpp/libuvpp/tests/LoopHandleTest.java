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

import java.util.HashSet;
import java.util.Set;

import android.util.Log;

import com.iwebpp.libuvpp.handles.AsyncHandle;
import com.iwebpp.libuvpp.handles.CheckHandle;
import com.iwebpp.libuvpp.handles.HandleFactory;
import com.iwebpp.libuvpp.handles.IdleHandle;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.PipeHandle;
import com.iwebpp.libuvpp.handles.TCPHandle;
import com.iwebpp.libuvpp.handles.UDPHandle;
import com.iwebpp.libuvpp.handles.UDTHandle;

///import org.testng.Assert;
///import org.testng.annotations.Test;

import static com.iwebpp.libuvpp.handles.DefaultHandleFactory.newFactory;

public class LoopHandleTest extends TestBase {
	private static final String TAG = "LoopHandleTest";

    private static final String DOT_SPLIT_REGEX = "\\.";

   /// @Test
    public void testList() throws Throwable {
        final HandleFactory handleFactory = newFactory();
        final LoopHandle loop = handleFactory.getLoopHandle();
        final String[] handles = loop.list();
        ///Assert.assertNotNull(handles);
        ///Assert.assertEquals(handles.length, 0);

        final AsyncHandle async= handleFactory.newAsyncHandle();
        final CheckHandle check= handleFactory.newCheckHandle();
        final IdleHandle idle= handleFactory.newIdleHandle();
        ///final SignalHandle signal= handleFactory.newSignalHandle();
        final PipeHandle pipe= handleFactory.newPipeHandle(false);
        final TCPHandle tcp= handleFactory.newTCPHandle();
        final UDPHandle udp= handleFactory.newUDPHandle();
        final UDTHandle udt= handleFactory.newUDTHandle();

        System.out.println(async);
        System.out.println(check);
        System.out.println(idle);
        ///System.out.println(signal);
        System.out.println(pipe);
        System.out.println(tcp);
        System.out.println(udp);
        System.out.println(udt);

        final Set<String> pointers = new HashSet<String>();
        pointers.add(async.toString().split(DOT_SPLIT_REGEX)[1]);
        pointers.add(check.toString().split(DOT_SPLIT_REGEX)[1]);
        pointers.add(idle.toString().split(DOT_SPLIT_REGEX)[1]);
        ///pointers.add(signal.toString().split(DOT_SPLIT_REGEX)[1]);
        pointers.add(pipe.toString().split(DOT_SPLIT_REGEX)[1]);
        pointers.add(tcp.toString().split(DOT_SPLIT_REGEX)[1]);
        pointers.add(udp.toString().split(DOT_SPLIT_REGEX)[1]);
        pointers.add(udt.toString().split(DOT_SPLIT_REGEX)[1]);

        final String[] handles1 = loop.list();
        ///Assert.assertNotNull(handles1);
        ///Assert.assertEquals(handles1.length, 7);
        for (final String handle : handles1) {
            System.out.println(handle);
            ///Assert.assertNotNull(handle);
            final String pointer = handle.toString().split(DOT_SPLIT_REGEX)[1];
            ///Assert.assertTrue(pointers.remove(pointer));
        }
        ///Assert.assertTrue(pointers.isEmpty());
    }

    ///public static void main(final String[] args) throws Throwable {
    public void run() {
		Log.d(TAG, "start test");
		
        final LoopHandleTest test = new LoopHandleTest();
        try {
			test.testList();
		} catch (Throwable e) {
			e.printStackTrace();
		}
    }

}
