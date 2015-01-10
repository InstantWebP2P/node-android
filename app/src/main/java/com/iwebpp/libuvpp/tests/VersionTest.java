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

import com.iwebpp.libuvpp.LibUV;
import android.util.Log;

public class VersionTest extends TestBase {

	private static final String TAG = "VersionTest";

    ///@Test
    public void testVersion() {
        final String version = LibUV.version();
        Log.d(TAG, "libuv version is " + version);
        
        System.out.println("libuv version is " + version);
        ///junit.framework.///Assert.assertEquals(version, "0.10.27");
    }

    ///@Test
    public void testExePath() {
        final String exe = LibUV.exePath();
        Log.d(TAG, "exe is " + exe);

        System.out.println("exe is " + exe);
        ///junit.framework.///Assert.assertNotNull(exe);
    }

    public void run() {
		Log.d(TAG, "start test");

		testVersion();
		testExePath();
    }
    
}
