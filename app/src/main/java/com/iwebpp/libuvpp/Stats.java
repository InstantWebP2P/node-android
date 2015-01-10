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

public class Stats {

    private int dev;
    private int ino;
    private int mode;
    private int nlink;
    private int uid;
    private int gid;
    private int rdev;
    private long size;
    private int blksize;
    private long blocks;
    private long atime;
    private long mtime;
    private long ctime;

    public Stats() {
    }

    public Stats(final int dev, final int ino, final int mode,
                 final int nlink, final int uid, final int gid,
                 final int rdev, final long size, final int blksize,
                 final long blocks, final long atime, final long mtime,
                 final long ctime) {
        set(dev, ino, mode, nlink, uid, gid, rdev, size, blksize, blocks, atime, mtime, ctime);
    }

    public void set(final int dev, final int ino, final int mode,
                    final int nlink, final int uid, final int gid,
                    final int rdev, final long size, final int blksize,
                    final long blocks, final long atime, final long mtime,
                    final long ctime) {
        this.dev = dev;
        this.ino = ino;
        this.mode = mode;
        this.nlink = nlink;
        this.uid = uid;
        this.gid = gid;
        this.rdev = rdev;
        this.size = size;
        this.blksize = blksize;
        this.blocks = blocks;
        this.atime = atime;
        this.mtime = mtime;
        this.ctime = ctime;
    }

    public int getDev() {
        return dev;
    }

    public int getIno() {
        return ino;
    }

    public int getMode() {
        return mode;
    }

    public int getNlink() {
        return nlink;
    }

    public int getUid() {
        return uid;
    }

    public int getGid() {
        return gid;
    }

    public int getRdev() {
        return rdev;
    }

    public long getSize() {
        return size;
    }

    public int getBlksize() {
        return blksize;
    }

    public long getBlocks() {
        return blocks;
    }

    public long getAtime() {
        return atime;
    }

    public long getMtime() {
        return mtime;
    }

    public long getCtime() {
        return ctime;
    }

    @Override
    public String toString() {
        return "{ dev: " + dev +
                " ino: " + ino +
                " mode: " + mode +
                " nlink: " + nlink +
                " uid: " + uid +
                " gid: " + gid +
                " rdev: " + rdev +
                " size: " + size +
                " blksize: " + blksize +
                " blocks: " + blocks +
                " atime: " + getAtime() +
                " mtime: " + getMtime() +
                " ctime: " + getCtime() + " }";
    }
}
