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

package com.iwebpp.libuvpp.handles;

public final class StdioOptions {

    public enum StdioType {
        IGNORE(0x00),
        CREATE_PIPE(0x01),
        INHERIT_FD(0x02),
        INHERIT_STREAM(0x04);

        final int value;

        StdioType(final int value) {
            this.value = value;
        }
    }

    private final StdioType type;
    private final StreamHandle stream;
    private final int fd;

    public StdioOptions(final StdioType type, final StreamHandle stream, final int fd) {
        this.type = type;
        this.stream = stream;
        this.fd = fd;
    }

    public int type() {
        if (this.type != null) {
            return this.type.value;
        }
        return StdioType.IGNORE.value;
    }

    public long stream() {
        if (this.stream != null) {
            return this.stream.pointer;
        }
        return 0;
    }

    public int fd() {
        return this.fd;
    }
}

