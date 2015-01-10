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

package com.iwebpp.libuvpp;

///import java.nio.file.LinkPermission;
import java.security.BasicPermission;

import com.iwebpp.libuvpp.Address;

/**
 * Permissions specific to LibUV.
 * Permission examples:
 * permission net.java.libuv.LibUVPermission "libuv.process.*";
 * permission net.java.libuv.LibUVPermission "libuv.process.chdir";
 * permission net.java.libuv.LibUVPermission "libuv.pipe.*";
 * permission net.java.libuv.LibUVPermission "libuv.signal.9";
 * permission net.java.libuv.LibUVPermission "libuv.handle";
 * permission net.java.libuv.LibUVPermission "libuv.loop.multi";
 * - Child process spawning is authorized thanks to SecurityManager.checkExec.
 * libuv.spawn permission is also required.
 * - TCP/UDP are authorized thanks to calls to SecurityManager.checkConnect/checkListen/checkAccept
 * libuv.udp or libuv.tcp permission are also required.
 */
public final class LibUVPermission extends BasicPermission {

    static final long serialVersionUID = 8529091307897434802L;

    public interface AddressResolver {
        public Address resolve();
    }

    private static final String LIBUV = "libuv";
    private static final String PREFIX = LIBUV + ".";
    // process
    private static final String PROCESS = PREFIX + "process.";
    public static final String PROCESS_CHDIR = PROCESS + "chdir";
    public static final String PROCESS_CWD = PROCESS + "cwd";
    public static final String PROCESS_EXE_PATH = PROCESS + "exePath";
    public static final String PROCESS_GET_TITLE = PROCESS + "getTitle";
    public static final String PROCESS_KILL = PROCESS + "kill";
    public static final String PROCESS_SET_TITLE = PROCESS + "setTitle";
    // pipe
    private static final String PIPE = PREFIX + "pipe.";
    public static final String PIPE_BIND = PIPE + "bind";
    public static final String PIPE_CONNECT = PIPE + "connect";
    public static final String PIPE_OPEN = PIPE + "open";
    public static final String PIPE_ACCEPT = PIPE + "accept";

    // handle
    public static final LibUVPermission HANDLE = new LibUVPermission(PREFIX + "handle");

    // loop
    public static final LibUVPermission MULTI_LOOP = new LibUVPermission(PREFIX + "loop.multi");

    // signal
    public static final String SIGNAL = PREFIX + "signal.";

    public LibUVPermission(final String name) {
        super(name);
    }

    public static void checkPermission(final String name) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (System.getSecurityManager() != null) {
            final LibUVPermission perm = new LibUVPermission(name);
            sm.checkPermission(perm);
        }*/
    }

    public static void checkHandle() {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (System.getSecurityManager() != null) {
            sm.checkPermission(HANDLE);
        }*/
    }

    public static void checkNewLoop(final int count) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (count > 1 && System.getSecurityManager() != null) {
            sm.checkPermission(MULTI_LOOP);
        }*/
    }

    public static void checkSpawn(final String cmd) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (System.getSecurityManager() != null) {
            sm.checkExec(cmd);
        }*/
    }

    public static void checkBind(final String host, final int port) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // Side effect is to check permission to resolve host.
            new InetSocketAddress(host, port);
        }*/
    }

    public static void checkConnect(final String host, final int port) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkConnect(host, port);
        }*/
    }

    public static void checkListen(final int port) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkListen(port);
        }*/
    }

    public static void checkAccept(final AddressResolver resolver) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            final Address addr = resolver.resolve();

            sm.checkAccept(addr.getIp(), addr.getPort());
        }*/
    }

    public static void checkUDPBind(final String host, final int port) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkBind(host, port);
            sm.checkListen(port);
        }*/
    }

    public static void checkUDPSend(final String host, final int port) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                final InetAddress addr = InetAddress.getByName(host);
                if (addr.isMulticastAddress()) {
                    sm.checkMulticast(addr);
                }
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
            sm.checkConnect(host, port);
        }*/
    }

    /*
     * Files
     */
    private static boolean isFlag(final int mask, final int flag) {
        return (mask & flag) == flag;
    }

    public static void checkOpenFile(final String path, final int mask) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // write
            if (isFlag(mask, Constants.O_CREAT) ||
                isFlag(mask, Constants.O_WRONLY)||
                isFlag(mask, Constants.O_RDWR) ||
                isFlag(mask, Constants.O_TRUNC)) {
                sm.checkWrite(path);
            }

            // read
            if (isFlag(mask, Constants.O_RDONLY) ||
                isFlag(mask, Constants.O_RDWR)) {
                sm.checkRead(path);
            }
        }*/
    }

    public static void checkReadFile(final String path) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkRead(path);
        }*/
    }

    public static void checkWriteFile(final String path) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkWrite(path);
        }*/
    }

    public static void checkReadFile(final int fd, final String path) {
    	/*
        // stdin, stdout, and stderr does not need to be checked as they are provided by the underlying platform.
        // Needed to support command line redirection.
        if (fd == 0 || fd == 1 || fd == 2) {
            return;
        }
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkRead(path);
        }*/
    }

    public static void checkWriteFile(final int fd, final String path) {
    	/*
        // stdin, stdout, and stderr does not need to be checked as they are provided by the underlying platform.
        // Needed to support command line redirection.
        if (fd == 0 || fd == 1 || fd == 2) {
            return;
        }
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkWrite(path);
        }*/
    }

    public static void checkDeleteFile(final String path) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkDelete(path);
        }*/
    }

    public static void checkHardLink(final String existing, final String link) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ///sm.checkPermission(new LinkPermission("hard"));
            sm.checkWrite(existing);
            sm.checkWrite(link);
        }*/
    }

    public static void checkSymbolicLink(final String existing, final String link) {
    	/*
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ///sm.checkPermission(new LinkPermission("symbolic"));
            sm.checkWrite(existing);
            sm.checkWrite(link);
        }*/
    }
}
