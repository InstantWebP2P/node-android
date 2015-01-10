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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Constants {

    private static final Field[] FIELDS = Constants.class.getDeclaredFields();
    private static final int MASK = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
    private static final int[] FIELD_VALUES = new int[FIELDS.length];

    private static final Map<String, Integer> CONSTANTS;
    private static final Map<Integer, String> CONSTANTS_STRING;

    public static Map<String, Integer> getConstants() {
        return CONSTANTS;
    }

    public static Map<Integer, String> getConstantsString() {
        return CONSTANTS_STRING;
    }

    static {
        Constants._get_field_values(FIELD_VALUES);
    }

    public static final int O_RDONLY        = FIELD_VALUES[0];
    public static final int O_WRONLY        = FIELD_VALUES[1];
    public static final int O_RDWR          = FIELD_VALUES[2];

    public static final int O_APPEND        = FIELD_VALUES[3];
    public static final int O_CREAT         = FIELD_VALUES[4];
    public static final int O_TRUNC         = FIELD_VALUES[5];
    public static final int O_EXCL          = FIELD_VALUES[6];
    public static final int O_SYNC          = FIELD_VALUES[7];
    public static final int O_NOCTTY        = FIELD_VALUES[8];

    public static final int S_IRUSR         = FIELD_VALUES[9];
    public static final int S_IWUSR         = FIELD_VALUES[10];
    public static final int S_IXUSR         = FIELD_VALUES[11];
    public static final int S_IRWXU         = FIELD_VALUES[12];

    public static final int S_IRGRP         = FIELD_VALUES[13];
    public static final int S_IWGRP         = FIELD_VALUES[14];
    public static final int S_IXGRP         = FIELD_VALUES[15];
    public static final int S_IRWXG         = FIELD_VALUES[16];

    public static final int S_IROTH         = FIELD_VALUES[17];
    public static final int S_IWOTH         = FIELD_VALUES[18];
    public static final int S_IXOTH         = FIELD_VALUES[19];
    public static final int S_IRWXO         = FIELD_VALUES[20];

    public static final int S_IFMT          = FIELD_VALUES[21];
    public static final int S_IFIFO         = FIELD_VALUES[22];
    public static final int S_IFCHR         = FIELD_VALUES[23];
    public static final int S_IFDIR         = FIELD_VALUES[24];
    public static final int S_IFBLK         = FIELD_VALUES[25];
    public static final int S_IFREG         = FIELD_VALUES[26];
    public static final int S_IFLNK         = FIELD_VALUES[27];
    public static final int S_IFSOCK        = FIELD_VALUES[28];
    public static final int S_IFWHT         = FIELD_VALUES[29];

    public static final int S_ISUID         = FIELD_VALUES[30];
    public static final int S_ISGID         = FIELD_VALUES[31];
    public static final int S_ISVTX         = FIELD_VALUES[32];

    public static final int SIGHUP          = FIELD_VALUES[33];
    public static final int SIGINT          = FIELD_VALUES[34];
    public static final int SIGQUIT         = FIELD_VALUES[35];
    public static final int SIGILL          = FIELD_VALUES[36];
    public static final int SIGTRAP         = FIELD_VALUES[37];
    public static final int SIGABRT         = FIELD_VALUES[38];
    public static final int SIGIOT          = FIELD_VALUES[39];
    public static final int SIGBUS          = FIELD_VALUES[40];
    public static final int SIGFPE          = FIELD_VALUES[41];
    public static final int SIGKILL         = FIELD_VALUES[42];
    public static final int SIGUSR1         = FIELD_VALUES[43];
    public static final int SIGSEGV         = FIELD_VALUES[44];
    public static final int SIGUSR2         = FIELD_VALUES[45];
    public static final int SIGPIPE         = FIELD_VALUES[46];
    public static final int SIGALRM         = FIELD_VALUES[47];
    public static final int SIGTERM         = FIELD_VALUES[48];
    public static final int SIGSTKFLT       = FIELD_VALUES[49];
    public static final int SIGCHLD         = FIELD_VALUES[50];
    public static final int SIGCONT         = FIELD_VALUES[51];
    public static final int SIGSTOP         = FIELD_VALUES[52];
    public static final int SIGTSTP         = FIELD_VALUES[53];
    public static final int SIGTTIN         = FIELD_VALUES[54];
    public static final int SIGTTOU         = FIELD_VALUES[55];
    public static final int SIGURG          = FIELD_VALUES[56];
    public static final int SIGXCPU         = FIELD_VALUES[57];
    public static final int SIGXFSZ         = FIELD_VALUES[58];
    public static final int SIGVTALRM       = FIELD_VALUES[59];
    public static final int SIGPROF         = FIELD_VALUES[60];
    public static final int SIGWINCH        = FIELD_VALUES[61];
    public static final int SIGIO           = FIELD_VALUES[62];
    public static final int SIGPOLL         = FIELD_VALUES[63];
    public static final int SIGPWR          = FIELD_VALUES[64];
    public static final int SIGSYS          = FIELD_VALUES[65];
    public static final int SIGUNUSED       = FIELD_VALUES[66];

    private static native void _get_field_values(int[] values);

    static {
        final Map<String, Integer> constants = new HashMap<String, Integer>(FIELDS.length);
        final Map<Integer, String> constantsString = new HashMap<Integer, String>(FIELDS.length);
        for (final Field f : FIELDS) {
            if ((f.getModifiers() & MASK) == MASK) {
                try {
                    constants.put(f.getName(), (Integer) f.get(null));
                    constantsString.put((Integer) f.get(null), f.getName());
                } catch (Exception ex) {
                    // Should never happen, ignore with msg
                    ex.printStackTrace();
                }
            }
        }
        CONSTANTS = Collections.unmodifiableMap(constants);
        CONSTANTS_STRING = Collections.unmodifiableMap(constantsString);
    }
}

