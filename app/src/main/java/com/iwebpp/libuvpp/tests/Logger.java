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

public final class Logger {

	private final String prefix;

    public Logger() {
        this.prefix = null;
    }

    public Logger(final String prefix) {
        this.prefix = prefix;
    }

    public byte[] array(final ByteBuffer byteBuffer) {
        if (byteBuffer.hasArray()) {
            return byteBuffer.array();
        } else {
            final ByteBuffer dup = byteBuffer.duplicate();
            final byte[] data = new byte[dup.capacity()];
            dup.clear();
            dup.get(data);
            return data;
        }
    }

    public void log(final Object... args) throws Exception {
        System.out.print(prefix == null ? "" : prefix);
        if (args != null) {
            for (final Object arg : args) {
                if (arg instanceof ByteBuffer) {
                    final byte[] bytes = array(((ByteBuffer) arg));
                    System.out.print(new String(bytes, "utf-8"));
                } else {
                    System.out.print(arg);
                }
                System.out.print(" ");
            }
        }
        System.out.println();
    }

}
